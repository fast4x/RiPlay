package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.net.Uri
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_BLOB
import android.database.Cursor.FIELD_TYPE_FLOAT
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.Cursor.FIELD_TYPE_NULL
import android.database.Cursor.FIELD_TYPE_STRING
import android.database.sqlite.SQLiteDatabase
import androidx.room.Transactor
import androidx.room.immediateTransaction
import androidx.room.execSQL
import androidx.room.useWriterConnection
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.DatabaseInitializer
import it.fast4x.riplay.data.getDatabaseInitializer
import it.fast4x.riplay.enums.RestoreMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Backup/restore del database riscritto con la driver API KMP di Room 2.8
 * (SQLiteConnection/SQLiteStatement/usePrepared) al posto di SupportSQLiteDatabase,
 * che non è più disponibile nel target KMP.
 */
class DatabaseBackupManager(
    private val context: Context,
    private val database: Database
) {

    suspend fun backupDatabase(backupUri: Uri) = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "riplay-backup-${System.currentTimeMillis()}.db")
        temp.delete()   // VACUUM INTO fallisce se il file di destinazione esiste già: temp sempre fresca

        try {
            // 1. Checkpoint WAL best-effort (mai bloccante): libera il WAL prima della copia.
            //    Non serve per la correttezza: VACUUM INTO legge uno snapshot coerente
            //    che include anche il contenuto del WAL.
            runCatching {
                getDatabaseInitializer().useWriterConnection { conn ->
                    conn.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { it.step() }
                }
            }.onFailure {
                Timber.w("backupDatabase: wal_checkpoint saltato: ${it.message}")
            }

            // 2. VACUUM INTO su connessione DEDICATA (mai sulla writer di Room):
            val dbFile = context.getDatabasePath(DatabaseInitializer.DB_NAME)
            val source = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            try {
                source.rawQuery("PRAGMA busy_timeout = 5000", null).use { it.moveToFirst() }
                source.execSQL("VACUUM INTO ?", arrayOf(temp.absolutePath))
            } finally {
                source.close()
            }

            // 3. Copia il file verso la destinazione scelta dall'utente (SAF)
            context.contentResolver.openOutputStream(backupUri)?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: error("Impossibile aprire la destinazione")

        } finally {
            temp.delete()
        }
    }

    // classic restore
    suspend fun restoreDatabase(restoreUri: Uri) {
        withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath(DatabaseInitializer.DB_NAME)
            // 1. Checkpoint e chiusura controllata del DB prima della sovrascrittura
            runCatching {
                getDatabaseInitializer().useWriterConnection { conn ->
                    conn.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { it.step() }
                }
            }.onFailure {
                Timber.w("restoreDatabase: wal_checkpoint saltato: ${it.message}")
            }
            getDatabaseInitializer().close()

            // 2. Copia il backup sul file del database
            context.applicationContext.contentResolver.openInputStream(restoreUri)
                ?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: error("Impossibile leggere il file di backup")

            // 3. Pulizia dei file ausiliari del vecchio database.
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-journal").delete()
        }
    }

    suspend fun smartRestoreDatabase(restoreUri: Uri, mode: RestoreMode) {
        withContext(Dispatchers.IO) {
            // 1. Copia il backup in cache — il DB resta VIVO e operativo
            val internalBackupFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.db")
            try {
                context.contentResolver.openInputStream(restoreUri)?.use { input ->
                    FileOutputStream(internalBackupFile).use { input.copyTo(it) }
                } ?: error("Impossibile leggere il file di backup: $restoreUri")
            } catch (e: Exception) {
                Timber.e("smartRestore: copia fallita: ${e.message}")
                internalBackupFile.delete()
                // Propaga l'errore invece di tornare in silenzio: altrimenti il chiamante
                // vede un restore "riuscito" e riavvia l'app senza aver ripristinato nulla.
                throw IllegalStateException("smartRestore: copia del backup non riuscita", e)
            }

            // 1b. Guardia anti file vuoto/troncato: un file da 0 byte supererebbe
            //     l'integrity check (SQLite lo tratta come DB valido senza tabelle)
            //     e il restore terminerebbe "con successo" senza importare nulla.
            if (internalBackupFile.length() == 0L) {
                internalBackupFile.delete()
                throw IllegalStateException("Il file di backup è vuoto o la copia non è completa")
            }

            var backupDb: SQLiteDatabase? = null
            try {
                // 2. Apre il backup con una connessione DEDICATA read-only.
                backupDb = SQLiteDatabase.openDatabase(
                    internalBackupFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val backup = checkNotNull(backupDb)

                // 3. Integrity check sulla connessione dedicata
                val ok = backup.readStringOrNull("PRAGMA integrity_check") == "ok"
                if (!ok) throw IllegalStateException("Backup corrotto")

                // 4. Elenco tabelle dal backup
                val tables = backup.readStringList(
                    "SELECT name FROM sqlite_master WHERE type='table' " +
                            "AND name NOT LIKE 'sqlite_%' " +
                            "AND name != 'room_master_table'"
                )

                // 5. Travaso in transazione: lettura righe dal backup
                getDatabaseInitializer().useWriterConnection { db ->
                    db.execSQL("PRAGMA foreign_keys = OFF")
                    try {
                        db.immediateTransaction {
                            for (table in tables) importTableData(db, backup, table, mode)
                        }
                    } finally {
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                }
                Timber.d("smartRestore: travaso completato")
            } catch (e: Exception) {
                Timber.e("smartRestore fallito (rollback automatico): ${e.message}")
                throw e
            } finally {
                backupDb?.close()
                internalBackupFile.delete()
            }

        }
    }

    private suspend fun importTableData(
        db: Transactor,
        backupDb: SQLiteDatabase,
        tableName: String,
        mode: RestoreMode
    ) {
        if (!tableName.matches(Regex("[A-Za-z0-9_]+"))) return    // whitelist

        val exists = db.usePrepared("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'") {
            it.step()
        }
        if (!exists) return

        if (mode == RestoreMode.REPLACE) {
            try {
                db.usePrepared("DELETE FROM `$tableName`") { it.step() }
                runCatching { db.usePrepared("DELETE FROM sqlite_sequence WHERE name='$tableName'") { it.step() } }
                Timber.d("REPLACE: svuotata $tableName")
            } catch (e: Exception) {
                throw IllegalStateException("Impossibile svuotare $tableName", e)
            }
        }


        val currentColumns = getColumnNames(db, tableName)
        val backupColumns = getBackupColumnNames(backupDb, tableName)
        val commonColumns = currentColumns.intersect(backupColumns).toList()

        if (currentColumns.isEmpty() || backupColumns.isEmpty()) {
            Timber.w("Schema non leggibile per $tableName (main=${currentColumns.size}, backup=${backupColumns.size}), skip")
            return
        }

        if (commonColumns.isEmpty()) {
            Timber.d("Nessuna colonna in comune per $tableName, skip")
            return
        }

        val columnsStr = commonColumns.joinToString(",") { "`$it`" }
        val placeholders = commonColumns.joinToString(",") { "?" }
        val insertSql = "INSERT INTO `$tableName` ($columnsStr) VALUES ($placeholders)"

        var imported = 0
        backupDb.rawQuery("SELECT $columnsStr FROM `$tableName`", null).use { cursor ->
            db.usePrepared(insertSql) { stmt ->
                while (cursor.moveToNext()) {
                    for (i in commonColumns.indices) {
                        when (cursor.getType(i)) {
                            FIELD_TYPE_NULL -> stmt.bindNull(i + 1)
                            FIELD_TYPE_INTEGER -> stmt.bindLong(i + 1, cursor.getLong(i))
                            FIELD_TYPE_FLOAT -> stmt.bindDouble(i + 1, cursor.getDouble(i))
                            FIELD_TYPE_STRING -> stmt.bindText(i + 1, cursor.getString(i))
                            FIELD_TYPE_BLOB -> stmt.bindBlob(i + 1, cursor.getBlob(i))
                            else -> stmt.bindNull(i + 1)
                        }
                    }
                    stmt.step()
                    stmt.reset()
                    stmt.clearBindings()
                    imported++
                }
            }
        }
        Timber.d("Import OK [$tableName] -> $imported righe importate")
    }

    private suspend fun getColumnNames(db: Transactor, tableName: String): List<String> =
        runCatching {
            db.usePrepared("PRAGMA table_info(`$tableName`)") { stmt ->
                buildList {
                    while (stmt.step()) add(stmt.getText(1))
                }
            }
        }.getOrElse { emptyList() }

    private fun getBackupColumnNames(backupDb: SQLiteDatabase, tableName: String): List<String> =
        runCatching {
            backupDb.rawQuery("PRAGMA table_info(`$tableName`)", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(c.getString(1))
                }
            }
        }.getOrElse { emptyList() }

    /** Prima riga, prima colonna (es. "ok" per PRAGMA integrity_check). */
    private fun SQLiteDatabase.readStringOrNull(sql: String): String? =
        rawQuery(sql, null).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    /** Prima colonna di tutte le righe (es. elenco tabelle da sqlite_master). */
    private fun SQLiteDatabase.readStringList(sql: String): List<String> =
        rawQuery(sql, null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

}
