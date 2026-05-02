package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import it.fast4x.riplay.data.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DatabaseBackupManager(
    private val context: Context,
    private val database: Database
) {

    suspend fun backupDatabase(backupUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                        FileInputStream( database.path() ).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            } catch (e: IOException) {
                throw e
            }
        }
    }

    // classic restore
    suspend fun restoreDatabase(restoreUri: Uri) {
        withContext(Dispatchers.IO) {
            Timber.d("DatabaseBackupManager restoreDatabase close db")
            try {
                database.checkpoint()
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
    fun smartRestoredatabase(restoreUri: Uri) {
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

        db.execSQL("ATTACH DATABASE '$safePath' AS backup_db;")

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
                importTableData(db, tableName)
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

            internalBackupFile.delete()
        }
    }

    private fun importTableData(db: SupportSQLiteDatabase, tableName: String) {
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

        db.delete(tableName, null, null)

        val insertSql = "INSERT INTO $tableName ($columnsStr) SELECT $columnsStr FROM backup_db.$tableName"

        try {
            db.execSQL(insertSql)
            Timber.d("DatabaseBackupManager importTableData Imported data table $tableName (columns: $columnsStr)")
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

}