package it.fast4x.riplay.musicvault

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getSafeDefaultDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class MusicVaultWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val songId = inputData.getString("song_id") ?: return Result.failure()
        val url    = inputData.getString("url")     ?: return Result.failure()

        // Scarica sempre nella cartella privata
        val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: return Result.failure()

        return try {
            Database.asyncTransaction {
                updateMusicVaultState(songId, MusicVaultState.DOWNLOADING)
            }

            val result = engine.executeScript(url, privateDir.absolutePath)
            val fileName = result.fileName
            val path = result.path
            val title = result.title
            val thumbnailFileName = result.thumbnailFileName
            val duration = result.duration
            val authors = result.artist

            // Sposta nella cartella scelta dall'utente (se diversa)
            val finalFileName          = moveToUserFolder(context, fileName, privateDir)
            val finalThumbnailFileName = moveToUserFolder(context, thumbnailFileName, privateDir)
            Timber.d("MusicVaultWorker finalFileName=$finalFileName finalThumbnailFileName=$finalThumbnailFileName")


            val song = Song(
                id = songId,
                title = title,
                durationText = formatAsDuration(duration.toLong()),
                thumbnailUrl = null,
                artistsText = authors
            )

            Database.asyncTransaction {
                insert(song) // E' già nel db? Forzo..

                CoroutineScope(Dispatchers.IO).launch {
                    updateMusicVaultCompleted(
                        id = songId,
                        fileName = finalFileName,
                        thumbnailFileName = finalThumbnailFileName
                    )
                }
            }


            MusicVaultEvents.emit(
                MusicVaultEvent.DownloadCompleted(
                    songId            = songId,
                    fileName          = finalFileName,
                    thumbnailFileName = finalThumbnailFileName
                )
            )


            Result.success(workDataOf("file_name" to finalFileName))

        } catch (e: Exception) {
            Timber.e("MusicVaultWorker doWork() exception: ${e.message}")
            Database.updateMusicVaultState(songId, MusicVaultState.FAILED)
            Result.failure()
            //if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun moveToUserFolder(
        context: Context,
        fileName: String,
        privateDir: File
    ): String {
        val userUri = MusicVaultRepository.getOutputUri()
            ?: run {
                Timber.d("MusicVault moveToUserFolder: nessun URI salvato, skip")
                return fileName
            }

        val sourceFile = File(privateDir, fileName)
        if (!sourceFile.exists()) {
            Timber.d("MusicVault moveToUserFolder: file sorgente non trovato: ${sourceFile.absolutePath}")
            return fileName
        }

        return try {
            if (userUri.scheme == "file") {
                // CASO 1: Cartella di default dell'app

                val destDir = getSafeDefaultDir(context, DEFAULT_MUSICVAULT_DIRECTORY)
                val destFile = File(destDir, fileName)

                // Tentiamo prima lo spostamento istantaneo
                val renamed = sourceFile.renameTo(destFile)
                if (renamed) {
                    Timber.d("MusicVault moveToUserFolder: file spostato con successo → ${destFile.absolutePath}")
                    return fileName // <--- RITORNA SOLO IL NOME FILE (es. "song.mp3")
                } else {
                    // Se fallisce, copiamo
                    sourceFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    sourceFile.delete()
                    Timber.d("MusicVault moveToUserFolder: file copiato con successo → ${destFile.absolutePath}")
                    return fileName // <--- RITORNA SOLO IL NOME FILE (es. "song.mp3")
                }
            } else if (userUri.scheme == "content") {
                // =================================================================
                // CASO 2: URI content:// (Cartella scelta dall'utente via SAF)
                // Usiamo DocumentFile e ContentResolver
                // =================================================================

                val destDir = DocumentFile.fromTreeUri(context, userUri)
                Timber.d("MusicVault moveToUserFolder: destDir=$destDir exists=${destDir?.exists()} canWrite=${destDir?.canWrite()}")

                if (destDir == null || !destDir.exists() || !destDir.canWrite()) {
                    Timber.e("MusicVault moveToUserFolder: cartella destinazione non accessibile")
                    return fileName
                }

                // Nota: ho corretto il mimeType di m4a in audio/mp4 che è lo standard corretto per SAF
                val mimeType = when {
                    fileName.endsWith(".webm") -> "audio/webm"
                    fileName.endsWith(".m4a")  -> "audio/mp4"
                    fileName.endsWith(".mp3")  -> "audio/mpeg"
                    fileName.endsWith(".opus") -> "audio/opus"
                    else                       -> "audio/*"
                }
                val extension = fileName.substringAfterLast(".")
                val baseName  = fileName.substringBeforeLast(".")

                val destFile = destDir.createFile(mimeType, "$baseName.$extension")

                Timber.d("MusicVault moveToUserFolder: destFile=$destFile uri=${destFile?.uri}")

                if (destFile == null) {
                    Timber.e("MusicVault moveToUserFolder: impossibile creare file nella destinazione")
                    return fileName
                }

                context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        val bytes = input.copyTo(output)
                        Timber.d("MusicVault moveToUserFolder: copiati $bytes bytes")
                    }
                } ?: run {
                    Timber.e("MusicVault moveToUserFolder: openOutputStream ha restituito null")
                    return fileName
                }

                sourceFile.delete()
                Timber.d("MusicVault moveToUserFolder: file spostato con successo → ${destFile.uri}")

                return destFile.uri.toString() // Ritorniamo l'URI content://
            } else {
                Timber.e("MusicVault moveToUserFolder: Schema URI non supportato: ${userUri.scheme}")
                return fileName
            }

        } catch (e: Exception) {
            Timber.e("MusicVault moveToUserFolder: eccezione → ${e.message}")
            fileName
        }
    }
}