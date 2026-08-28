package it.fast4x.riplay.extensions.experimental.webdavlibrary

import android.content.Context
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.appContext
import timber.log.Timber
import java.io.File

class WebDavDatabaseSyncBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        return try {
            val appSettingsManager = (appContext() as MainApplication).appSettingsManager
            val appSettings = appSettingsManager.activeSettings.value

            // 1. Quale account fa da backup?
            val backupAccountId = appSettings.backupWebDavAccountId ?: return Result.success()

            // 2. Recupera l'account dal DB
            val backupAccount = Database.webDavAccountDao().getById(backupAccountId)
                ?: return Result.failure() // L'utente ha cancellato l'account ma non cambiato le impostazioni

            // 3. Decripta e crea l'oggetto di config al volo
            val webDavConfig = WebDavConfig(
                baseUrl = backupAccount.baseUrl,
                username = backupAccount.username,
                password = CryptoManager.decrypt(backupAccount.encryptedPassword)
            )
            if (webDavConfig.baseUrl.isEmpty()) {
                return Result.success() // Nessun config salvato, niente da fare
            }

            val selectedFolderUri = context.cacheDir

            val backupManager = DatabaseBackupManager(context, Database)
            val dbFile = File(selectedFolderUri, "riplay_sync.db")

            backupManager.backupDatabase(dbFile.toUri())
            Timber.e("WebDavDatabaseSyncBackupWorker: Backup database completed")

            if (!dbFile.exists()) {
                return Result.success()
            }

            // 4. Esegui l'upload atomico
            val repository = WebDavLibraryRepository()
            repository.uploadFileAtomically(
                config = webDavConfig,
                remoteFolder = "RiPlaySync",
                localFile = dbFile
            )

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "WebDavDatabaseSyncBackupWorker fallito")
            // Se la rete è caduta, diciamo a WorkManager di riprovare dopo un po'
            Result.retry()
        }
    }
}