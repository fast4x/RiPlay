package it.fast4x.riplay.musicvault

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import androidx.core.net.toUri
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.utils.getSafeDefaultDir
import timber.log.Timber

object MusicVaultRepository {

    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    private fun getSavedPath(): String? {
        val path = appSettingsManager.activeSettings.value.musicVaultPath //appContext().preferences.getString(MUSIC_VAULT_PATH.key, "")
        return path.takeIf { it?.isNotBlank() == true }
    }

    fun getOutputDir(): String {
        val savedPath = getSavedPath()

        return if (savedPath != null && savedPath.startsWith("content://")) {
            // L'utente ha scelto una cartella esterna: converto per yt-dlp
            DocumentFile.fromTreeUri(appContext(), savedPath.toUri())
                ?.let { getRealPathFromUri(appContext(), it.uri) }
                ?: getSafeDefaultDir(appContext(), DEFAULT_MUSICVAULT_DIRECTORY).absolutePath
        } else {
            // L'utente non ha scelto niente (""), o ha resettato: uso la cartella sicura dell'app
            getSafeDefaultDir(appContext(), DEFAULT_MUSICVAULT_DIRECTORY).absolutePath
        }
    }

    fun getOutputUri(): Uri? {
        val savedPath = getSavedPath()

        return if (savedPath != null && savedPath.startsWith("content://")) {
            Timber.d("MusicVaultRepository getOutputUri: savedPath $savedPath")
            // Cartella esterna: ritorno l'URI content://
            savedPath.toUri()
        } else {
            // Cartella di default: ritorno l'URI file:// della cartella dell'app
            val uri = Uri.fromFile(getSafeDefaultDir(appContext(), DEFAULT_MUSICVAULT_DIRECTORY))
            Timber.d("MusicVaultRepository getOutputUri: uri $uri")
            uri
        }
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

        return when {
            fileName.startsWith("content://") -> {
                // È un URI SAF esatto: lo controlliamo direttamente (velocissimo)
                try {
                    DocumentFile.fromSingleUri(context, fileName.toUri())?.exists() ?: false
                } catch (e: Exception) {
                    false // Il permesso SAF potrebbe essere stato revocato dall'utente
                }
            }
            fileName.startsWith("/") || fileName.startsWith("file://") -> {
                // Percorso assoluto (per retrocompatibilità con vecchie versioni dell'app)
                val file = if (fileName.startsWith("file://")) File(fileName.toUri().path!!) else File(fileName)
                file.exists()
            }
            else -> {
                // È un nome file relativo (es. "song.mp3"): è per forza nella cartella di default
                File(getSafeDefaultDir(context, DEFAULT_MUSICVAULT_DIRECTORY), fileName).exists()
            }
        }
    }

    fun deleteFiles(song: Song) {
        deleteFile(song.musicVaultFileName)
        deleteFile(song.musicVaultThumbnailFileName)
    }

    fun resolveAudioFile(song: Song): Any? {
        val fileName = song.musicVaultFileName ?: return null

        return when {
            fileName.startsWith("content://") -> {
                // URI SAF esatto
                try {
                    if (DocumentFile.fromSingleUri(appContext(), fileName.toUri())?.exists() == true)
                        fileName.toUri()
                    else null
                } catch(e: Exception) { null }
            }
            fileName.startsWith("/") || fileName.startsWith("file://") -> {
                // Path assoluto
                val file = if (fileName.startsWith("file://")) File(fileName.toUri().path!!) else File(fileName)
                if (file.exists()) file else null
            }
            else -> {
                // Nome file relativo -> Cartella di default
                val file = File(getSafeDefaultDir(appContext(), DEFAULT_MUSICVAULT_DIRECTORY), fileName)
                if (file.exists()) file else null
            }
        }
    }

    fun deleteFile(fileName: String?) {
        if (fileName.isNullOrEmpty()) return

        when {
            fileName.startsWith("content://") -> {
                // Cancella tramite SAF
                try {
                    DocumentFile.fromSingleUri(appContext(), fileName.toUri())?.delete()
                } catch (e: Exception) {
                    Timber.e("MusicVault deleteFile SAF error: ${e.message}")
                }
            }
            fileName.startsWith("/") || fileName.startsWith("file://") -> {
                // Cancella path assoluto
                val file = if (fileName.startsWith("file://")) File(fileName.toUri().path!!) else File(fileName)
                file.delete()
            }
            else -> {
                // Cancella dalla cartella di default
                File(getSafeDefaultDir(appContext(), DEFAULT_MUSICVAULT_DIRECTORY), fileName).delete()
            }
        }
    }
}