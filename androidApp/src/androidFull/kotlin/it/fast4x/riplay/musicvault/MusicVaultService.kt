package it.fast4x.riplay.musicvault

import android.content.Context
import android.os.Environment
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

object MusicVaultService {

    fun enqueue(context: Context, song: Song) {
        val request = OneTimeWorkRequestBuilder<MusicVaultWorker>()
            .setInputData(workDataOf(
                "song_id" to song.id,
                "url"     to "https://youtube.com/watch?v=${song.id}"
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("musicvault_${song.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "musicvault_${song.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context, songId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("musicvault_${songId}")

        // Aspetta che il Worker si fermi completamente prima di pulire
        CoroutineScope(Dispatchers.IO).launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow("musicvault_${songId}")
                .filter { workInfos ->
                    val state = workInfos.firstOrNull()?.state
                    state == WorkInfo.State.CANCELLED ||
                            state == WorkInfo.State.SUCCEEDED ||
                            state == WorkInfo.State.FAILED
                }
                .take(1)  // si prende solo il primo evento terminale
                .collect {
                    // Ora il Worker è fermo, si può pulire in sicurezza
                    val song = Database.song(songId).firstOrNull() ?: return@collect
                    MusicVaultRepository.deleteFiles(song)
                    Database.updateMusicVaultState(songId, MusicVaultState.NONE)
                    Database.updateMusicVaultCompleted(
                        id                = songId,
                        fileName          = "",
                        thumbnailFileName = ""
                    )
                    MusicVaultEvents.emit(
                        MusicVaultEvent.DownloadRemoved(
                            songId = songId
                        )
                    )
                }
        }
    }

    fun observeState(context: Context, songId: String): Flow<MusicVaultState> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("musicvault_${songId}")
            .map { workInfos ->
                val info = workInfos.firstOrNull()
                when {
                    info == null -> MusicVaultState.NONE
                    info.state == WorkInfo.State.RUNNING   -> MusicVaultState.DOWNLOADING
                    info.state == WorkInfo.State.ENQUEUED  -> MusicVaultState.QUEUED  // usato anche per i retry
                    info.state == WorkInfo.State.SUCCEEDED -> MusicVaultState.COMPLETED
                    info.state == WorkInfo.State.FAILED    -> MusicVaultState.FAILED
                    info.state == WorkInfo.State.CANCELLED -> MusicVaultState.NONE
                    else -> MusicVaultState.NONE
                }
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

/* How to
// Avvia il download
MusicVaultService.enqueue(context, song)

// Cancella il download
MusicVaultService.cancel(context, song.id)

// Osserva lo stato (in un ViewModel o Composable)
MusicVaultService.observeState(context, song.id)
    .collect { state ->
        when (state) {
            MusicVaultState.DOWNLOADING -> // mostra progress
            MusicVaultState.COMPLETED   -> // mostra icona completato
            MusicVaultState.FAILED      -> // mostra errore
            else -> {}
        }
    }

// Riproduci con ExoPlayer
val file = MusicVaultService.resolveAudioFile(context, song)
if (file != null) {
    // file.absolutePath a ExoPlayer
} else {
    // file non trovato, riscaricare?
}

 */