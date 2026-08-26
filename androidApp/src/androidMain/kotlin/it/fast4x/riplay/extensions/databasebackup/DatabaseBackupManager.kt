package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.DatabaseInitializer
import it.fast4x.riplay.enums.RestoreMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.database.sqlite.transaction

class DatabaseBackupManager(
    private val context: Context,
    private val database: Database
) {

    suspend fun backupDatabase(backupUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Svuota il WAL nel file principale (usiamo TRUNCATE per sicurezza massima)
                database.checkpoint()

                // 2. CHIUDI IL DATABASE (Fondamentale per evitare scritture concorrenti)
                database.close()

                // 3. COPIA A FREDDO: Ora nessuno sta scrivendo sul file
                try {
                    val dbPath = database.path()
                    context.applicationContext.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                        FileInputStream(dbPath).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } finally {
                    // 4. RIAPRI IL DATABASE: Qualsiasi cosa accada, l'app deve poter funzionare
                    DatabaseInitializer.reload()
                }

            } catch (e: IOException) {
                // Assicuriamoci di riaprire il DB anche se la copia fallisce
                DatabaseInitializer.reload()
                throw e
            }
        }
    }

    // classic restore
    suspend fun restoreDatabase(restoreUri: Uri) {
        withContext(Dispatchers.IO) {
            Timber.d("DatabaseBackupManager restoreDatabase close db")
            try {
                database.openHelper().writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE);")
                database.close()

                context.applicationContext.contentResolver.openInputStream(restoreUri)
                    ?.use { inputStream ->
                        FileOutputStream( database.path() ).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            } catch (e: IOException) {
                throw e
            }
        }
    }

    // smart restore
    suspend fun smartRestoredatabase(restoreUri: Uri, mode: RestoreMode) {
        withContext(Dispatchers.IO) {
            // 1. Estrai il percorso del database principale PRIMA di chiuderlo
            val mainDbPath = database.path() ?: return@withContext

            // 2. Chiudi l'istanza globale di Room per evitare lock concorrenti
            database.close()

            val internalBackupFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.db")

            try {
                context.contentResolver.openInputStream(restoreUri)?.use { inStream ->
                    FileOutputStream(internalBackupFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: run {
                    Timber.e("DatabaseBackupManager performSmartRestore Impossible to open backup file: $restoreUri")
                }
            } catch (e: Exception) {
                Timber.e("DatabaseBackupManager performSmartRestore Error during file copy: ${e.message}")
            }

            // 3. APRI IL DB ORIGINALE TRAMITE SQLITE NATIVO
            val db: SQLiteDatabase = try {
                SQLiteDatabase.openDatabase(
                    mainDbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
            } catch (e: Exception) {
                Timber.e("Impossibile aprire il DB principale a freddo: ${e.message}")
                DatabaseInitializer.reload()
                internalBackupFile.delete()
                return@withContext
            }

            db.execSQL("PRAGMA foreign_keys = OFF;")
            val safePath = internalBackupFile.absolutePath.replace("'", "''")

            // TENTIAMO L'ATTACH (Deve stare FUORI dalla transazione)
            try {
                db.execSQL("ATTACH DATABASE '$safePath' AS backup_db;")
            } catch (e: Exception) {
                Timber.e("DatabaseBackupManager Il file di backup è corrotto o non è un DB valido: ${e.message}")
                db.execSQL("PRAGMA foreign_keys = ON;")
                db.close()
                DatabaseInitializer.reload()
                internalBackupFile.delete()
                return@withContext
            }

            // CHECK DI INTEGRITÀ
            try {
                val cursor = db.rawQuery("PRAGMA backup_db.integrity_check;", null)
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    cursor.close()
                    if (result != "ok") {
                        throw Exception("DatabaseBackupManager Integrity check failed: $result")
                    }
                } else {
                    cursor.close()
                }
            } catch (e: Exception) {
                Timber.e("DatabaseBackupManager Integrity check fallito, DB corrotto: ${e.message}")
                try { db.execSQL("DETACH DATABASE backup_db;") } catch (_: Exception) {}
                db.execSQL("PRAGMA foreign_keys = ON;")
                db.close()
                DatabaseInitializer.reload()
                internalBackupFile.delete()
                return@withContext
            }

            // 4. TRANSAZIONE SOLO PER TRAVASO DATI
            // db.transaction gestisce in automatico beginTransaction, setTransactionSuccessful ed endTransaction
            try {
                db.transaction {
                    // All'interno di questa lambda "this" è l'oggetto SQLiteDatabase
                    val cursor = rawQuery(
                        "SELECT name FROM backup_db.sqlite_master " +
                                "WHERE type='table' " +
                                "AND name NOT LIKE 'sqlite_%' " +
                                "AND name != 'room_master_table'",
                        null
                    )

                    val tablesInBackup = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        tablesInBackup.add(cursor.getString(0))
                    }
                    cursor.close()

                    for (tableName in tablesInBackup) {
                        importTableDataNative(this, tableName, mode)
                    }

                    Timber.d("DatabaseBackupManager performSmartRestore operazioni di travaso completate nella transazione.")
                } // <--- QUI la transazione si chiude e fa il COMMIT definitivo sul disco!

                Timber.d("DatabaseBackupManager Transazione committata con successo.")
            } catch (e: Exception) {
                Timber.e("DatabaseBackupManager Errore durante la transazione di restore smart (Eseguito Rollback): ${e.message}")
            } finally {
                // 5. PULIZIA POST-TRANSAZIONE (Operazioni da fare tassativamente a transazione chiusa)
                try {
                    db.execSQL("DETACH DATABASE backup_db;")
                } catch (e: Exception) {
                    Timber.e("Errore durante il DETACH: ${e.message}")
                }

                try {
                    db.execSQL("PRAGMA foreign_keys = ON;")
                } catch (_: Exception) {}

                // Ora puoi chiudere in sicurezza il DB nativo
                db.close()
                internalBackupFile.delete()
            }

            // 6. Invalida e ricarica Room
            DatabaseInitializer.reload()
        }
    }


    private fun importTableDataNative(db: SQLiteDatabase, tableName: String, mode: RestoreMode) {
        val currentCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'", null)
        if (!currentCursor.moveToFirst()) {
            currentCursor.close()
            return
        }
        currentCursor.close()

        // 1. GESTIONE MODALITÀ REPLACE: Svuota la tabella prima dell'import
        if (mode == RestoreMode.REPLACE) {
            try {
                // Proteggiamo il nome tabella con i backtick
                db.execSQL("DELETE FROM `$tableName`;")
                try {
                    db.execSQL("DELETE FROM sqlite_sequence WHERE name='$tableName';")
                } catch (_: Exception) {}
                Timber.d("DatabaseBackupManager [REPLACE] Tabella svuotata: $tableName")
            } catch (e: Exception) {
                Timber.e("DatabaseBackupManager Errore svuotamento $tableName: ${e.message}")
            }
        }

        // 2. Calcolo colonne con backtick per i path qualificati
        val currentColumns = getColumnNamesNative(db, "main.`$tableName`")
        val backupColumns = getColumnNamesNative(db, "backup_db.`$tableName`")
        val commonColumns = currentColumns.intersect(backupColumns)

        if (commonColumns.isEmpty()) {
            Timber.d("DatabaseBackupManager No columns in common for $tableName. Skipped.")
            return
        }

        // FONDAMENTALE: Avvolge ogni singola colonna tra backtick per evitare conflitti con parole come 'order'
        val columnsStr = commonColumns.joinToString(",") { "`$it`" }

        // Proteggiamo sia i campi che i nomi delle tabelle nel comando finale
        val insertSql = "INSERT INTO `$tableName` ($columnsStr) SELECT $columnsStr FROM backup_db.`$tableName`"

        // 3. Esecuzione dell'inserimento blindato
        try {
            db.execSQL(insertSql)

            val recordsAfter = db.rawQuery("SELECT COUNT(*) FROM `$tableName`", null)
            var countAfter = 0
            if (recordsAfter.moveToFirst()) {
                countAfter = recordsAfter.getInt(0)
            }
            recordsAfter.close()

            Timber.d("DatabaseBackupManager importTableData Success per $tableName. Record totali: $countAfter")
        } catch (e: Exception) {
            // Se una tabella specifica fallisce, adesso non corrompe l'intera transazione degli altri cicli
            Timber.e("DatabaseBackupManager Errore inserimento dati nella tabella $tableName: ${e.message}")
        }
    }



    private fun getColumnNamesNative(db: SQLiteDatabase, qualifiedTableName: String): List<String> {
        val columns = mutableListOf<String>()
        val query = "SELECT * FROM $qualifiedTableName LIMIT 0"

        try {
            val cursor = db.rawQuery(query, null)
            columns.addAll(cursor.columnNames.toList())
            cursor.close()
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager getColumnNames Error for $qualifiedTableName: ${e.message}")
        }

        return columns
    }


    /*
    fun smartRestoredatabase(restoreUri: Uri, mode: RestoreMode) {
        database.close()

        val internalBackupFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.db")

        try {
            context.contentResolver.openInputStream(restoreUri)?.use { inStream ->
                FileOutputStream(internalBackupFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            } ?: run {
                Timber.e("DatabaseBackupManager performSmartRestore Impossible to open backup file: $restoreUri")
            }
            Timber.d("DatabaseBackupManager performSmartRestore File copied from Uri to cache: ${internalBackupFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e( "DatabaseBackupManager performSmartRestore Error during file copy from Uri to cache ${e.message}")
        }

        val db = database.openHelper().writableDatabase

        db.execSQL("PRAGMA foreign_keys = OFF;")

        val safePath = internalBackupFile.absolutePath.replace("'", "''")

        //db.execSQL("ATTACH DATABASE '$safePath' AS backup_db;")

        // TENTIAMO L'ATTACH
        try {
            db.execSQL("ATTACH DATABASE '$safePath' AS backup_db;")
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager Il file di backup è corrotto o non è un DB valido: ${e.message}")
            db.execSQL("PRAGMA foreign_keys = ON;")
            db.close()
            DatabaseInitializer.reload()
            internalBackupFile.delete()
            return // USCITA IMMEDIATA
        }

        // CHECK DI INTEGRITÀ
        try {
            val cursor = db.query("PRAGMA backup_db.integrity_check;")
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                cursor.close()
                if (result != "ok") {
                    throw Exception("DatabaseBackupManager Integrity check failed: $result")
                }
            } else {
                cursor.close()
            }
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager Integrity check fallito, DB corrotto: ${e.message}")
            try { db.execSQL("DETACH DATABASE backup_db;") } catch (_: Exception) {}
            db.execSQL("PRAGMA foreign_keys = ON;")
            db.close()
            DatabaseInitializer.reload()
            internalBackupFile.delete()
            return // USCITA IMMEDIATA
        }

        db.beginTransaction()

        try {

            val cursor = db.query(
                "SELECT name FROM backup_db.sqlite_master " +
                        "WHERE type='table' " +
                        "AND name NOT LIKE 'sqlite_%' " +
                        "AND name != 'room_master_table'"
            )

            val tablesInBackup = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tablesInBackup.add(cursor.getString(0))
            }
            cursor.close()

            for (tableName in tablesInBackup) {
                importTableData(db, tableName, mode)
            }

            db.setTransactionSuccessful()
            Timber.d("DatabaseBackupManager performSmartRestore smart completed")
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager performSmartRestore Error during restore smart ${e.message}")
        } finally {
            db.endTransaction()
            try {
                db.execSQL("DETACH DATABASE backup_db;")
            } catch (e: Exception) {}
            db.execSQL("PRAGMA foreign_keys = ON;")

            db.close()

            internalBackupFile.delete()
        }

        DatabaseInitializer.reload()
    }



    private fun importTableData(db: SupportSQLiteDatabase, tableName: String, mode: RestoreMode) {
        val currentCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
        if (!currentCursor.moveToFirst()) {
            currentCursor.close()
            return
        }
        currentCursor.close()

        val currentColumns = getColumnNames(db, "main.$tableName")

        val backupColumns = getColumnNames(db, "backup_db.$tableName")

        val commonColumns = currentColumns.intersect(backupColumns)

        if (commonColumns.isEmpty()) {
            Timber.d("DatabaseBackupManager importTableData No columns into table  $tableName. Skipped.")
            return
        }

        val columnsStr = commonColumns.joinToString(",")

        //db.delete(tableName, null, null)



        val insertSql = "INSERT INTO $tableName ($columnsStr) SELECT $columnsStr FROM backup_db.$tableName"

        try {
            if (mode == RestoreMode.REPLACE)
                db.execSQL("DELETE FROM $tableName")
            //val recordsBefore = db.query("SELECT * FROM $tableName")
            //val countBefore = recordsBefore.count
            db.execSQL(insertSql)
            val recordsAfter = db.query("SELECT * FROM $tableName")
            val countAfter = recordsAfter.count
            Timber.d("DatabaseBackupManager importTableData Imported records $countAfter data table $tableName (columns: $columnsStr)")
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager importTableData Error data table $tableName (columns: $columnsStr): ${e.message}")
        }

    }

    private fun getColumnNames(db: SupportSQLiteDatabase, qualifiedTableName: String): List<String> {
        val columns = mutableListOf<String>()

        val query = "SELECT * FROM $qualifiedTableName LIMIT 0"

        try {
            val cursor = db.query(query)
            columns.addAll(cursor.columnNames.toList())
            cursor.close()
        } catch (e: Exception) {
            Timber.e("DatabaseBackupManager getColumnNames Error during read columns in table $qualifiedTableName ${e.message}")
        }

        return columns
    }

     */

}