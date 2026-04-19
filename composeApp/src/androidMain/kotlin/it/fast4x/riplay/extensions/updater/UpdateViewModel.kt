package it.fast4x.riplay.extensions.updater

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

data class UpdateState(
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val versionName: String = "",
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState

    private val versionTxtUrl =
        "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"

    private val apkUrlPattern =
        "https://github.com/fast4x/RiPlay/releases/download/v{updatedVersionName}/RiPlay-${BuildConfig.FLAVOR}-release-{updatedVersionName}.apk"

    private var cachedApkUrl: String = ""

    fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = _updateState.value.copy(isChecking = true, errorMessage = null)
            try {

                val url = URL("$versionTxtUrl?t=${System.currentTimeMillis()}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                        .trim().split("-")
                    val latestVersionCode = content.firstOrNull()?.toInt() ?: 0
                    val currentVersionCode = getCurrentVersionCode()
                    val updatedVersionName = content.getOrNull(1) ?: ""

                    if (isNewerVersion(latestVersionCode, currentVersionCode)) {
                        cachedApkUrl = apkUrlPattern.replace("{updatedVersionName}", updatedVersionName)
                        _updateState.value = _updateState.value.copy(
                            isChecking = false,
                            updateAvailable = true,
                            versionName = updatedVersionName
                        )
                    } else {
                        _updateState.value = _updateState.value.copy(isChecking = false)
                    }
                } else {
                    _updateState.value = _updateState.value.copy(isChecking = false, errorMessage = "UpdateViewModel: Impossibile raggiungere il server di aggiornamento.")
                }
                connection.disconnect()
            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(isChecking = false, errorMessage = "UpdateViewModel: Errore di connessione: ${e.message}")
            }
        }
    }

    fun downloadAndInstall() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = _updateState.value.copy(isDownloading = true, updateAvailable = false)
            try {
                val url = URL(cachedApkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                // Gestione del reindirizzamento di GitHub (spesso GitHub porta a un CDN esterno)
                var finalUrl: URL = url
                if (connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM || connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    finalUrl = URL(connection.getHeaderField("Location"))
                    connection.disconnect()
                    val redirectConnection = finalUrl.openConnection() as HttpURLConnection
                    redirectConnection.connect()
                    downloadApkFromConnection(redirectConnection)
                    redirectConnection.disconnect()
                } else {
                    downloadApkFromConnection(connection)
                    connection.disconnect()
                }

            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(isDownloading = false, errorMessage = "Errore durante il download: ${e.message}")
            }
        }
    }

    private fun downloadApkFromConnection(connection: HttpURLConnection) {
        val fileLength = connection.contentLength
        val fileName = "app-update.apk"
        val outputFile = File(getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        connection.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val progress = (totalBytesRead * 100 / fileLength).toFloat() / 100f
                        _updateState.value = _updateState.value.copy(downloadProgress = progress)
                    }
                }
            }
        }

        installApk(outputFile)
        _updateState.value = _updateState.value.copy(isDownloading = false, downloadProgress = 0f)
    }

    private fun installApk(file: File) {
        val context = getApplication<Application>()

        /*
        // 1. Controlla se l'app ha il permesso per installare da fonti sconosciute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                // Manca il permesso! Apro le impostazioni di Android per farglielo abilitare
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                // Torna indietro. L'utente dovrà ripremere "Scarica e Installa" dopo aver abilitato il toggle.
                return
            }
        }
        */

        // 2. Se arriviamo qui, abbiamo il permesso. Procediamo con l'installazione.
        if (!file.exists()) {
            _updateState.value = _updateState.value.copy(errorMessage = "UpdateViewModel: File APK non trovato dopo il download.")
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Fondamentale quando si parte da un ViewModel
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(errorMessage = "UpdateViewModel: Impossibile aprire l'installer: ${e.message}")
        }
    }

    private fun getCurrentVersionCode(): Int {
        return BuildConfig.VERSION_CODE
//        return getApplication<Application>().packageManager
//            .getPackageInfo(getApplication<Application>().packageName, 0).versionName
    }

    private fun isNewerVersion(latest: Int, current: Int): Boolean {
        return latest > current
    }
}