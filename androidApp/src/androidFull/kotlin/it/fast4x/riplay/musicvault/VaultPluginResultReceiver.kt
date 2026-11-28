package it.fast4x.riplay.musicvault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getSafeDefaultDir
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.enums.MusicVaultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Receiver dei risultati del plugin esterno "MusicVault" (protocollo v1).
 *
 * Registrato nel manifest del flavor full (androidFull/AndroidManifest.xml),
 * exported: il plugin invia broadcast ESPLICITI verso EXTRA_CALLBACK_PACKAGE.
 *
 * Mappa gli eventi del protocollo sugli stati Music Vault:
 *   STARTED   -> DOWNLOADING
 *   PROGRESS  -> (solo log: lo stato resta DOWNLOADING, niente churn sul DB)
 *   COMPLETED -> eventuale import dei file (modalita' handoff), updateMusicVaultCompleted
 *   FAILED    -> FAILED
 */

const val DEFAULT_MUSICVAULT_DIRECTORY = "MusicVault"

class VaultPluginResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context, intent)
            } catch (e: Exception) {
                Timber.e("VaultPluginResultReceiver handle error: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val songId = intent.getStringExtra(VaultPluginContract.EXTRA_SONG_ID) ?: return
        val requestId = intent.getStringExtra(VaultPluginContract.EXTRA_REQUEST_ID).orEmpty()
        val protocolVersion = intent.getIntExtra(VaultPluginContract.EXTRA_PROTOCOL_VERSION, 0)

        if (protocolVersion < VaultPluginContract.MIN_PROTOCOL_VERSION) {
            Timber.e("VaultPluginResultReceiver: protocollo non supportato v$protocolVersion (${intent.action})")
            return
        }

        when (intent.action) {
            VaultPluginContract.ACTION_RESULT_STARTED -> onStarted(songId, requestId)
            VaultPluginContract.ACTION_RESULT_PROGRESS -> onProgress(songId, intent)
            VaultPluginContract.ACTION_RESULT_COMPLETED -> onCompleted(context, songId, requestId, intent)
            VaultPluginContract.ACTION_RESULT_FAILED -> onFailed(songId, requestId, intent)
            else -> Timber.d("VaultPluginResultReceiver: azione ignota ${intent.action}")
        }
    }

    private suspend fun currentState(songId: String): MusicVaultState =
        Database.song(songId).firstOrNull()?.musicVaultState ?: MusicVaultState.NONE

    private suspend fun onStarted(songId: String, requestId: String) {
        val state = currentState(songId)
        if (state != MusicVaultState.QUEUED && state != MusicVaultState.RUNNING) {
            Timber.d("VaultPluginResultReceiver STARTED ignorato ($songId stato=$state)")
            return
        }
        Timber.d("VaultPluginResultReceiver STARTED $songId requestId=$requestId")
        Database.updateMusicVaultState(songId, MusicVaultState.RUNNING)
    }

    private fun onProgress(songId: String, intent: Intent) {
        val progress = intent.getIntExtra(VaultPluginContract.EXTRA_PROGRESS, -1)
        Timber.d("VaultPluginResultReceiver PROGRESS $songId $progress%")
        // Nessuna persistenza: il progresso resta responsabilita' della notifica del plugin.
    }

    private suspend fun onCompleted(
        context: Context,
        songId: String,
        requestId: String,
        intent: Intent
    ) {
        val state = currentState(songId)
        if (state != MusicVaultState.QUEUED && state != MusicVaultState.RUNNING) {
            Timber.d("VaultPluginResultReceiver COMPLETED ignorato ($songId stato=$state)")
            return
        }

        val resultTitle = intent.getStringExtra(VaultPluginContract.EXTRA_RESULT_TITLE).orEmpty()
        val resultArtist = intent.getStringExtra(VaultPluginContract.EXTRA_RESULT_ARTIST).orEmpty()
        val durationSeconds = intent.getIntExtra(VaultPluginContract.EXTRA_DURATION_SECONDS, 0)

        val remoteFileName = intent.getStringExtra(VaultPluginContract.EXTRA_FILE_NAME).orEmpty()
        val remoteThumbnailName = intent.getStringExtra(VaultPluginContract.EXTRA_THUMBNAIL_FILE_NAME).orEmpty()
        val remoteFileUri = intent.getStringExtra(VaultPluginContract.EXTRA_FILE_URI).orEmpty()
        val remoteThumbnailUri = intent.getStringExtra(VaultPluginContract.EXTRA_THUMBNAIL_URI).orEmpty()

        // Modalita' handoff: import dei file dal FileProvider del plugin nella
        // cartella Vault di RiPlay. In modalita' direct i nomi arrivano gia' pronti.
        val finalFileName = if (remoteFileUri.isNotBlank())
            importIntoVault(context, remoteFileUri.toUri(), remoteFileName, isThumbnail = false)
        else remoteFileName

        val finalThumbnailName = if (remoteThumbnailUri.isNotBlank())
            importIntoVault(context, remoteThumbnailUri.toUri(), remoteThumbnailName, isThumbnail = true)
        else remoteThumbnailName

        if (finalFileName.isNullOrBlank()) {
            Timber.e("VaultPluginResultReceiver COMPLETED senza file per $songId: segno FAILED")
            Database.updateMusicVaultState(songId, MusicVaultState.FAILED)
            return
        }

        // Stessa operazione che svolgeva MusicVaultWorker: inserisce/aggiorna il
        // brano con i metadati reali e marca il download completato.
        val song = Song(
            id = songId,
            title = resultTitle.ifBlank { songId },
            durationText = if (durationSeconds > 0) formatAsDuration(durationSeconds.toLong()) else null,
            thumbnailUrl = null,
            artistsText = resultArtist
        )
        Database.asyncTransaction {
            insert(song) // E' gia' nel db? Forzo..
            updateMusicVaultCompleted(
                id = songId,
                fileName = finalFileName,
                thumbnailFileName = finalThumbnailName.orEmpty()
            )
        }

        Timber.i("VaultPluginResultReceiver COMPLETED $songId fileName=$finalFileName requestId=$requestId")
        MusicVaultEvents.emit(
            MusicVaultEvent.Completed(
                songId = songId,
                fileName = finalFileName,
                thumbnailFileName = finalThumbnailName.orEmpty()
            )
        )
    }

    private suspend fun onFailed(songId: String, requestId: String, intent: Intent) {
        val state = currentState(songId)
        if (state != MusicVaultState.QUEUED && state != MusicVaultState.RUNNING) {
            Timber.d("VaultPluginResultReceiver FAILED ignorato ($songId stato=$state)")
            return
        }
        val message = intent.getStringExtra(VaultPluginContract.EXTRA_ERROR_MESSAGE).orEmpty()
        Timber.e("VaultPluginResultReceiver FAILED $songId requestId=$requestId message=$message")
        Database.updateMusicVaultState(songId, MusicVaultState.FAILED)
    }

    /**
     * Copia un file consegnato dal plugin (uri content:// del suo FileProvider)
     * nella cartella Music Vault configurata, replicando la logica di
     * MusicVaultWorker.moveToUserFolder:
     *  - cartella SAF (content://): crea il documento e copia, ritorna l'uri stringa;
     *  - cartella file: copia nel directory sicuro e ritorna il solo nome file.
     */
    private fun importIntoVault(
        context: Context,
        sourceUri: Uri,
        suggestedName: String,
        isThumbnail: Boolean
    ): String? {
        if (suggestedName.isBlank()) {
            Timber.e("VaultPluginResultReceiver importIntoVault: nome file mancante")
            return null
        }

        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: run {
                    Timber.e("VaultPluginResultReceiver importIntoVault: openInputStream null ($sourceUri)")
                    return null
                }

            val savedPath = runCatching {
                (context.applicationContext as MainApplication)
                    .appSettingsManager.activeSettings.value.musicVaultPath
            }.getOrNull().orEmpty()

            if (savedPath.startsWith("content://")) {
                // CASO SAF: cartella scelta dall'utente
                val destDir = DocumentFile.fromTreeUri(context, savedPath.toUri())
                if (destDir == null || !destDir.exists() || !destDir.canWrite()) {
                    Timber.e("VaultPluginResultReceiver importIntoVault: cartella SAF non accessibile")
                    input.close()
                    return null
                }

                val mimeType = mimeTypeFor(suggestedName, isThumbnail)
                val baseName = suggestedName.substringBeforeLast('.')
                val extension = suggestedName.substringAfterLast('.')
                val destFile = destDir.createFile(mimeType, "$baseName.$extension")
                if (destFile == null) {
                    Timber.e("VaultPluginResultReceiver importIntoVault: createFile null")
                    input.close()
                    return null
                }

                context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                    input.use { it.copyTo(output) }
                } ?: run {
                    Timber.e("VaultPluginResultReceiver importIntoVault: openOutputStream null")
                    input.close()
                    return null
                }
                input.close()
                Timber.d("VaultPluginResultReceiver importIntoVault: copiato (SAF) -> ${destFile.uri}")
                return destFile.uri.toString()
            } else {
                // CASO file: cartella di default (privata) di RiPlay
                val destDir = getSafeDefaultDir(context, DEFAULT_MUSICVAULT_DIRECTORY)
                val destFile = File(destDir, suggestedName)
                destFile.outputStream().use { output ->
                    input.use { it.copyTo(output) }
                }
                Timber.d("VaultPluginResultReceiver importIntoVault: copiato -> ${destFile.absolutePath}")
                return suggestedName
            }
        } catch (e: Exception) {
            Timber.e("VaultPluginResultReceiver importIntoVault error: ${e.message}")
            return null
        }
    }

    private fun mimeTypeFor(fileName: String, isThumbnail: Boolean): String {
        if (isThumbnail || fileName.endsWith(".jpg", true)) return "image/jpeg"
        return when {
            fileName.endsWith(".webm", true) -> "audio/webm"
            fileName.endsWith(".m4a", true)  -> "audio/mp4"
            fileName.endsWith(".mp3", true)  -> "audio/mpeg"
            fileName.endsWith(".opus", true) -> "audio/opus"
            fileName.endsWith(".mp4", true)  -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
