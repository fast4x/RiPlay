package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.net.Uri
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
        try {
            getDatabaseInitializer().useWriterConnection { conn ->
                // VACUUM INTO con parametro: usePrepared + bindText (KMP driver API)
                conn.usePrepared("VACUUM INTO ?") { stmt ->
                    stmt.bindText(1, temp.absolutePath)
                    stmt.step()
                }
            }

            context.contentResolver.openOutputStream(backupUri)?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: error("Impossibile aprire la destinazione")

        } finally {
            temp.delete()   // VACUUM INTO fallisce se il file esiste già: temp sempre fresca
        }
    }

    // classic restore
    suspend fun restoreDatabase(restoreUri: Uri) {
        withContext(Dispatchers.IO) {
            Timber.d("DatabaseBackupManager restoreDatabase close db")
            try {
                getDatabaseInitializer().useWriterConnection { conn ->
                    conn.usePrepared("PRAGMA wal_checkpoint(TRUNCATE);") { it.step() }
                }
                getDatabaseInitializer().close()

                context.applicationContext.contentResolver.openInputStream(restoreUri)
                    ?.use { inputStream ->
                        FileOutputStream(
                            context.getDatabasePath(DatabaseInitializer.DB_NAME).absolutePath
                        ).use { outputStream ->
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

            try {
                getDatabaseInitializer().useWriterConnection { db ->
                    // 2. ATTACH sulla connessione di Room (no close!)
                    db.usePrepared("ATTACH DATABASE ? AS backup_db") { stmt ->
                        stmt.bindText(1, internalBackupFile.absolutePath)
                        stmt.step()
                    }

                    // 3. Integrity check
                    val ok = db.usePrepared("PRAGMA backup_db.integrity_check") { stmt ->
                        stmt.step() && stmt.getText(0) == "ok"
                    }
                    if (!ok) throw IllegalStateException("Backup corrotto")

                    // 4. Travaso in transazione
                    db.immediateTransaction {
                        val tables = db.usePrepared(
                            "SELECT name FROM backup_db.sqlite_master " +
                                    "WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                                    "AND name != 'room_master_table'"
                        ) { stmt ->
                            buildList { while (stmt.step()) add(stmt.getText(0)) }
                        }

                        for (table in tables) importTableData(db, table, mode)
                    }
                    Timber.d("smartRestore: travaso completato")
                }
            } catch (e: Exception) {
                Timber.e("smartRestore fallito (rollback automatico): ${e.message}")
                throw e    // propagate alla UI per il messaggio all'utente
            } finally {
                runCatching {
                    getDatabaseInitializer().useWriterConnection { conn ->
                        conn.usePrepared("DETACH DATABASE backup_db") { it.step() }
                    }
                }
                internalBackupFile.delete()
            }

        }
    }

    private suspend fun importTableData(db: Transactor, tableName: String, mode: RestoreMode) {
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

        val currentColumns = getColumnNames(db, "main.`$tableName`")
        val backupColumns = getColumnNames(db, "backup_db.`$tableName`")
        val commonColumns = currentColumns.intersect(backupColumns).toList()

        if (commonColumns.isEmpty()) {
            Timber.d("Nessuna colonna in comune per $tableName, skip")
            return
        }

        val columnsStr = commonColumns.joinToString(",") { "`$it`" }
        val insertSql = "INSERT INTO `$tableName` ($columnsStr) SELECT $columnsStr FROM backup_db.`$tableName`"

        db.usePrepared(insertSql) { it.step() }

        val count = db.usePrepared("SELECT COUNT(*) FROM `$tableName`") { stmt ->
            stmt.step()
            stmt.getLong(0).toInt()
        }
        Timber.d("Import OK [$tableName] -> $count record totali")
    }

    private suspend fun getColumnNames(db: Transactor, qualifiedTable: String): List<String> =
        runCatching {
            db.usePrepared("PRAGMA table_info($qualifiedTable)") { stmt ->
                buildList {
                    while (stmt.step()) add(stmt.getText(2))   // campo "name" in PRAGMA table_info
                }
            }
        }.getOrElse { emptyList() }

}
