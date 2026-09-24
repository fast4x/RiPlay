package it.fast4x.riplay.extensions.webdavlibrary

import android.content.Context
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavBackupInfo
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.JsonManager
import it.fast4x.riplay.utils.ZipManager
import it.fast4x.riplay.utils.capitalized
import it.fast4x.riplay.utils.getDeviceInfo
import timber.log.Timber
import java.io.File

class WebDavDatabaseSyncBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = WebDavLibraryRepository()

        return try {

            // Recupera l'account dal DB
            val backupAccount = Database.webDavAccountDao().getBackupAccount()
                ?: return Result.failure() // L'utente ha cancellato l'account ma non cambiato le impostazioni

            val remoteFolder = if (backupAccount.remoteFolder != "/") backupAccount.remoteFolder else  WEBDAV_DEFAULT_BACKUP_FOLDER

            // Decripta e crea l'oggetto di config al volo
            val webDavConfig = WebDavConfig(
                baseUrl = backupAccount.baseUrl,
                username = backupAccount.username,
                password = CryptoManager.decrypt(backupAccount.encryptedPassword)
            )
            if (webDavConfig.baseUrl.isEmpty()) {
                return Result.success() // Nessun config salvato, niente da fare
            }

            // Esegue l'upload atomico del file informazioni o metadata
            val metadataFile = File(context.cacheDir, WEBDAV_DEFAULT_BACKUP_METADATA_FILE)
            val deviceInfo = getDeviceInfo()
            //Timber.d("WebDavDatabaseSyncBackupWorker deviceInfo = $deviceInfo")
            val info = WebDavBackupInfo(
                timestamp = System.currentTimeMillis(),
                deviceName = "${deviceInfo?.deviceBrand?.capitalized()} ${deviceInfo?.deviceModel}".ifEmpty { "Unknown device" }
            )
            val metadata = try {
                JsonManager.encodeToString(info)
            } catch (e: Exception) {
                Timber.e(e, "WebDavDatabaseSyncBackupWorker fallito ${e.message}")
                return Result.success()
            }
            metadataFile.writeText(metadata)

            repository.uploadFileDirect(
                config = webDavConfig,
                remoteFolder = remoteFolder,
                localFile = metadataFile
            )

            val selectedFolderUri = context.cacheDir

            val backupManager = DatabaseBackupManager(context, Database)
            val dbFile = File(selectedFolderUri, WEBDAV_DEFAULT_BACKUP_DB_FILE)


            backupManager.backupDatabase(dbFile.toUri())
            Timber.e("WebDavDatabaseSyncBackupWorker: backupDatabase completed")

            if (!dbFile.exists()) {
                Timber.d("WebDavDatabaseSyncBackupWorker: dbFile does not exist")
                return Result.failure()
            }

            // Crea il file zip del db in modo da ridurne la dimensione e velocizzare l'upload sul server
            val zippedDbFile = File(context.cacheDir, WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE)
            ZipManager.zip(dbFile, zippedDbFile)

            Timber.d("WebDavDatabaseSyncBackupWorker: DB compresso da ${dbFile.length()} a ${zippedDbFile.length()} bytes")

            // Puliamo il file .db non compresso per non sporcare la cache
            dbFile.delete()

            // Esegui l'upload del file zippato
            repository.uploadFileDirect(
                config = webDavConfig,
                remoteFolder = remoteFolder,
                localFile = zippedDbFile
            )

            // Pulizia finale
            zippedDbFile.delete()

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "WebDavDatabaseSyncBackupWorker fallito")
            // Se la rete è caduta, diciamo a WorkManager di riprovare dopo un po'
            Result.failure()
        }
    }
}