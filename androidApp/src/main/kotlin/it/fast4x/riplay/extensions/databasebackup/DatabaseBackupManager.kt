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
import androidx.room.withTransaction
import it.fast4x.riplay.utils.isAtLeastAndroid11

class DatabaseBackupManager(
    private val context: Context,
    private val database: Database
) {
    suspend fun backupDatabase(backupUri: Uri) = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "riplay-backup-${System.currentTimeMillis()}.db")
        try {
            if (isAtLeastAndroid11) {
                // Una istruzione al posto di: checkpoint + vacuum + close + copy + reload
                DatabaseInitializer.Instance.openHelper.writableDatabase
                    .execSQL("VACUUM INTO ?", arrayOf(temp.absolutePath))

                context.contentResolver.openOutputStream(backupUri)?.use { out ->
                    temp.inputStream().use { it.copyTo(out) }
                } ?: error("Impossibile aprire la destinazione")
            } else {
                val db = DatabaseInitializer.Instance.openHelper.writableDatabase
                db.beginTransaction()          // legge snapshot dal file congelato
                try {
                    db.query("SELECT COUNT(*) FROM Song").use { it.moveToFirst() }  // avvia lo snapshot
                    context.contentResolver.openOutputStream(backupUri)?.use { out ->
                        temp.inputStream().use { it.copyTo(out) }
                    } ?: error("Impossibile aprire la destinazione")          // copia il db
                } finally {
                    db.endTransaction()
                }
            }

        } finally {
            temp.delete()   // VACUUM INTO fallisce se il file esiste già: temp sempre fresca
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

    suspend fun smartRestoreDatabase(restoreUri: Uri, mode: RestoreMode) {
        withContext(Dispatchers.IO) {
            // 1. Copia il backup in cache — il DB resta VIVO e operativo
            val internalBackupFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.db")
            try {
                context.contentResolver.openInputStream(restoreUri)?.use { input ->
                    FileOutputStream(internalBackupFile).use { input.copyTo(it) }
                } ?: run {
                    Timber.e("smartRestore: impossibile aprire il backup: $restoreUri")
                    internalBackupFile.delete()
                    return@withContext
                }
            } catch (e: Exception) {
                Timber.e("smartRestore: copia fallita: ${e.message}")
                internalBackupFile.delete()
                return@withContext
            }

            val db = DatabaseInitializer.Instance.openHelper.writableDatabase

            try {
                // 2. ATTACH sulla connessione di Room (no close!)
                db.execSQL("ATTACH DATABASE ? AS backup_db", arrayOf(internalBackupFile.absolutePath))

                // 3. Integrity check
                val ok = db.query("PRAGMA backup_db.integrity_check").use { c ->
                    c.moveToFirst() && c.getString(0) == "ok"
                }
                if (!ok) throw IllegalStateException("Backup corrotto")

                // 4. Travaso in transazione — withTransaction di room-ktx (suspend)
                DatabaseInitializer.Instance.withTransaction {
                    val tables = db.query(
                        "SELECT name FROM backup_db.sqlite_master " +
                                "WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                                "AND name != 'room_master_table'"
                    ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

                    for (table in tables) importTableData(db, table, mode)
                }
                Timber.d("smartRestore: travaso completato")
            } catch (e: Exception) {
                Timber.e("smartRestore fallito (rollback automatico): ${e.message}")
                throw e    // propagate alla UI per il messaggio all'utente
            } finally {
                runCatching { db.execSQL("DETACH DATABASE backup_db") }
                internalBackupFile.delete()
            }

        }
    }

    private fun importTableData(db: SupportSQLiteDatabase, tableName: String, mode: RestoreMode) {
        if (!tableName.matches(Regex("[A-Za-z0-9_]+"))) return    // whitelist

        val exists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            .use { it.moveToFirst() }
        if (!exists) return

        if (mode == RestoreMode.REPLACE) {
            try {
                db.execSQL("DELETE FROM `$tableName`")
                runCatching { db.execSQL("DELETE FROM sqlite_sequence WHERE name='$tableName'") }
                Timber.d("REPLACE: svuotata $tableName")
            } catch (e: Exception) {
                throw IllegalStateException("Impossibile svuotare $tableName", e)
            }
        }

        val currentColumns = getColumnNames(db, "main.`$tableName`")
        val backupColumns = getColumnNames(db, "backup_db.`$tableName`")
        val commonColumns = currentColumns.intersect(backupColumns).toList()

        if (commonColumns.isEmpty()) {
            Timber.d("Nessuna colonna in comune per $tableName, skip")
            return
        }

        val columnsStr = commonColumns.joinToString(",") { "`$it`" }
        val insertSql = "INSERT INTO `$tableName` ($columnsStr) SELECT $columnsStr FROM backup_db.`$tableName`"

        db.execSQL(insertSql)

        val count = db.query("SELECT COUNT(*) FROM `$tableName`").use { c ->
            c.moveToFirst(); c.getInt(0)
        }
        Timber.d("Import OK [$tableName] -> $count record totali")
    }

    private fun getColumnNames(db: SupportSQLiteDatabase, qualifiedTable: String): List<String> =
        runCatching {
            db.query("SELECT * FROM $qualifiedTable LIMIT 0").use { it.columnNames.toList() }
        }.getOrElse { emptyList() }

}