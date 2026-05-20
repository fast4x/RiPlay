package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.preferences.musicVaultPathKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import androidx.core.net.toUri
import timber.log.Timber

object MusicVaultRepository {

    fun getOutputDir(): String {
        val savedUri = appContext().preferences.getString(musicVaultPathKey, null)
        return if (savedUri != null) {
            // Conversione URI in path per yt-dlp (Python non capisce content://)
            DocumentFile.fromTreeUri(appContext(), savedUri.toUri())
                ?.let { getRealPathFromUri(appContext(), it.uri) }
                ?: appContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath
        } else {
            appContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath
        }
    }

    fun getOutputUri(): Uri? {
        val savedUri = appContext().preferences.getString(musicVaultPathKey, null)
        val uri = savedUri?.toUri()
        Timber.d("MusicVaultRepository getOutputUri uri $uri")
        return uri
    }

    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        return DocumentFile.fromTreeUri(context, uri)?.uri?.let { treeUri ->
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val split = docId.split(":")
            val type = split[0]
            if (type.equals("primary", ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory()}/${split.getOrNull(1) ?: ""}"
            } else {
                // SD card o percorso secondario
                "/storage/$type/${split.getOrNull(1) ?: ""}"
            }
        }
    }

    // Usare questo metodo per caricare le canzoni nel vault
    // val songs by MusicVaultRepository.getSongs()
    //    .collectAsState(initial = emptyList())

    fun getSongs(): Flow<List<Song>> =
        Database.musicVaultSongs()
            .map { songs ->
                songs.filter { song ->
                    song.musicVaultFileName?.let { fileName ->
                        File(
                            getOutputDir(),
                            fileName
                        ).exists()
                    } ?: false
                }
            }

    fun resolveAudioFile(song: Song): Any? {
        val fileName = song.musicVaultFileName ?: return null

        return if (fileName.startsWith("content://")) {
            // File nella cartella scelta dall'utente → URI diretto
            fileName.toUri()
        } else {
            // File nella cartella privata → File classico
            val file = File(getOutputDir(), fileName)
            if (file.exists()) file else null
        }
    }

    fun resolveThumbnail(song: Song): Any? {
        song.musicVaultThumbnailFileName?.let { fileName ->
            if (fileName.startsWith("content://")) {
                return fileName.toUri()
            }
            val file = File(getOutputDir(), fileName)
            if (file.exists()) return file
        }
        return song.thumbnailUrl
    }

    fun delete(song: Song) {
        // Cancella i file fisici
        song.musicVaultFileName?.let { fileName ->
            File(
                getOutputDir(),
                fileName
            ).delete()
        }
        song.musicVaultThumbnailFileName?.let { fileName ->
            File(
                getOutputDir(),
                fileName
            ).delete()
        }
    }

    fun fileExists(context: Context, song: Song): Boolean {
        val fileName = song.musicVaultFileName ?: return false
        return if (fileName.startsWith("content://")) {
            // Verifica URI SAF
            try {
                val uri = fileName.toUri()
                DocumentFile.fromSingleUri(context, uri)?.exists() ?: false
            } catch (e: Exception) {
                false
            }
        } else {
            // Verifica file privato
            File(getOutputDir(), fileName).exists()
        }
    }

    fun deleteFiles(song: Song) {
        deleteFile(song.musicVaultFileName)
        deleteFile(song.musicVaultThumbnailFileName)
    }

    private fun deleteFile(fileName: String?) {
        if (fileName.isNullOrEmpty()) return

        if (fileName.startsWith("content://")) {
            try {
                val uri = fileName.toUri()

                // Verifica permessi attivi
                val permissions = appContext().contentResolver.persistedUriPermissions
                Timber.d("MusicVaultRepository Permessi attivi: ${permissions.map { it.uri }}")
                Timber.d("MusicVaultRepository canWrite su uri: ${permissions.any {
                    it.uri.toString() in uri.toString() && it.isWritePermission
                }}")

                val docFile = DocumentFile.fromSingleUri(appContext(), uri)
                Timber.d("MusicVaultRepository docFile exists: ${docFile?.exists()}")
                Timber.d("MusicVaultRepository docFile canWrite: ${docFile?.canWrite()}")
                val deleted = docFile?.delete()
                Timber.d("MusicVaultRepository deleted: $deleted")

            } catch (e: Exception) {
                Timber.e("MusicVaultRepository deleteFile error: ${e.message}")
            }
        } else {
            File(getOutputDir(), fileName).delete()
        }
    }
}