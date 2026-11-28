package it.fast4x.riplay.musicvault

import android.content.Context
import android.os.Environment
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.MusicVaultState
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Servizio Music Vault - comunicazione con eventuale plugin esterno installato.
 */
object MusicVaultService {

    fun enqueue(context: Context, song: Song) {
        // Stato ottimista: il plugin lo porta a DOWNLOADING con STARTED
        Database.asyncTransaction {
            updateMusicVaultState(song.id, MusicVaultState.QUEUED)
        }

        when (val result = VaultPluginClient.dispatchDownload(context, song)) {
            is VaultPluginClient.DispatchResult.Sent -> {
                Timber.d("MusicVaultService enqueue ${song.id} -> ${result.plugin.packageName}")
            }
            is VaultPluginClient.DispatchResult.NoPlugin -> {
                Timber.e("MusicVaultService enqueue ${song.id}: ${result.reason}")
                Database.asyncTransaction {
                    updateMusicVaultState(song.id, MusicVaultState.NONE)
                }
                SmartMessage(
                    message = result.reason,
                    type = PopupType.Error,
                    durationLong = true,
                    context = context
                )
            }
            is VaultPluginClient.DispatchResult.Error -> {
                Timber.e("MusicVaultService enqueue ${song.id} error: ${result.reason}")
                Database.asyncTransaction {
                    updateMusicVaultState(song.id, MusicVaultState.FAILED)
                }
                SmartMessage(
                    message = result.reason,
                    type = PopupType.Error,
                    durationLong = true,
                    context = context
                )
            }
        }
    }

    fun cancel(context: Context, songId: String) {
        // Il plugin rimuove il job dalla coda / ignora l'esito del lavoro in corso
        VaultPluginClient.dispatchCancel(context, songId)

        CoroutineScope(Dispatchers.IO).launch {
            // Pulizia locale immediata: come il vecchio flusso WorkManager
            // post-cancellazione (file + stato + evento Removed)
            val song = Database.song(songId).firstOrNull() ?: return@launch
            MusicVaultRepository.deleteFiles(song)
            Database.updateMusicVaultState(songId, MusicVaultState.NONE)
            Database.updateMusicVaultCompleted(
                id                = songId,
                fileName          = "",
                thumbnailFileName = ""
            )
            MusicVaultEvents.emit(
                MusicVaultEvent.Removed(
                    songId = songId
                )
            )
        }
    }

    fun resolveAudioFile(context: Context, song: Song): File? {
        val fileName = song.musicVaultFileName ?: return null
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            fileName
        )
        return if (file.exists()) file else null
    }

    fun resolveThumbnail(context: Context, song: Song): Any? {
        song.musicVaultThumbnailFileName?.let { fileName ->
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                fileName
            )
            if (file.exists()) return file
        }
        return song.thumbnailUrl
    }

    fun deleteSong(context: Context, song: Song) {
        CoroutineScope(Dispatchers.IO).launch {
            MusicVaultRepository.deleteFiles(song)
            try {
                Database.updateMusicVaultCompleted(
                    state = MusicVaultState.NONE,
                    id                = song.id,
                    fileName          = "",
                    thumbnailFileName = ""
                )
            } catch (e: Exception) {
              Timber.e("MusicVaultService deleteSong error ${e.message}")
            }
        }
    }

}
