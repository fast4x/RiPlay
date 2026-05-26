package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chaquo.python.Python
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
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
            Database.updateMusicVaultState(songId, MusicVaultState.DOWNLOADING)

            val py = Python.getInstance()
            val result = py.getModule("MusicVault")
                .callAttr("download_audio", url, privateDir.absolutePath)

            val path              = result.callAttr("get", "path").toString()
            val fileName          = result.callAttr("get", "filename").toString()
            val thumbnailFileName = result.callAttr("get", "thumbnail_filename").toString()
            val title             = result.callAttr("get", "title").toString()
            val duration          = result.callAttr("get", "duration").toInt()
            val artist            = result.callAttr("get", "artist").toString()

            // Tutto quello che ritorna Python
            Timber.d("MusicVault result keys: $result")
            Timber.d("MusicVault filename: $fileName")
            Timber.d("MusicVault path: $path")
            Timber.d("MusicVault title: $title")

            // Sposta nella cartella scelta dall'utente (se diversa)
            val finalFileName          = moveToUserFolder(context, fileName, privateDir)
            val finalThumbnailFileName = moveToUserFolder(context, thumbnailFileName, privateDir)

            Database.updateMusicVaultCompleted(
                id                = songId,
                fileName          = finalFileName,
                thumbnailFileName = finalThumbnailFileName
            )

            MusicVaultEvents.emit(
                MusicVaultEvent.DownloadCompleted(
                    songId            = songId,
                    fileName          = finalFileName,
                    thumbnailFileName = finalThumbnailFileName
                )
            )


            Result.success(workDataOf("file_name" to finalFileName))

        } catch (e: Exception) {
            Database.updateMusicVaultState(songId, MusicVaultState.FAILED)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
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
            val destDir = DocumentFile.fromTreeUri(context, userUri)
            Timber.d("MusicVault moveToUserFolder: destDir=$destDir exists=${destDir?.exists()} canWrite=${destDir?.canWrite()}")

            if (destDir == null || !destDir.exists() || !destDir.canWrite()) {
                Timber.e("MusicVault moveToUserFolder: cartella destinazione non accessibile")
                return fileName
            }

            val mimeType = when {
                fileName.endsWith(".webm") -> "audio/webm"
                fileName.endsWith(".m4a")  -> "audio/m4a"
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

            destFile.uri.toString()

        } catch (e: Exception) {
            Timber.e("MusicVault moveToUserFolder: eccezione → ${e.message}")
            fileName
        }
    }
}