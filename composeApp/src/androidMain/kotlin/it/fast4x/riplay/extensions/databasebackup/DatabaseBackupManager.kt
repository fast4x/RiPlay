package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.net.Uri
import it.fast4x.riplay.data.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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

}