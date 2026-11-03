package it.fast4x.riplay.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


object DesktopUpdateChecker {

    const val VER_URL =
        "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
    const val RELEASES_URL = "https://github.com/fast4x/RiPlay/releases/latest"

    /** esito del controllo (la UI lo mostra in About e all'avvio) */
    sealed interface UpdateCheckResult {
        data object Checking : UpdateCheckResult
        data object UpToDate : UpdateCheckResult
        data class Available(
            val versionCode: Int,
            val versionName: String,
            val productName: String
        ) : UpdateCheckResult
        data object Failed : UpdateCheckResult
    }

    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    /**
     * Scarica e interpreta il file .ver (stesso parsing di Android:
     * split sul "-"). Nessuna eccezione verso l'esterno: fallimento = Failed.
     */
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder(URI.create(VER_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val content = response.body().trim().split("-")

            val remoteCode = content.firstOrNull()?.toIntOrNull() ?: 0
            val remoteName = content.getOrNull(1) ?: ""
            val remoteProduct = content.getOrNull(2) ?: ""

            println("[RiPlay-Update] remota: code=$remoteCode name='$remoteName' product='$remoteProduct' | locale: ${DesktopAppInfo.VERSION}")

            if (remoteName.isBlank() || remoteCode <= 0) return@runCatching UpdateCheckResult.Failed

            if (isNameNewer(remoteName, DesktopAppInfo.VERSION))
                UpdateCheckResult.Available(remoteCode, remoteName, remoteProduct)
            else
                UpdateCheckResult.UpToDate
        }.getOrDefault(UpdateCheckResult.Failed)
    }

    /**
     * URL DIRETTO dell'installer per il sistema operativo corrente, costruito
     * dalla versionName letta nel file .ver (STEP 6c). Restituisce null se
     * l'OS non e' tra quelli con asset noto. Su Linux sceglie .deb o .rpm
     * guardando i file distintivi della distribuzione (default .deb).
     */
    fun installerDownloadUrl(versionName: String): String? {
        val v = versionName.trim().removePrefix("v").takeIf { it.isNotBlank() } ?: return null
        val os = System.getProperty("os.name", "").lowercase()
        val asset = when {
            os.contains("win") -> "RiPlay-v$v-Windows.msi"
            os.contains("mac") || os.contains("darwin") -> {
                val arch = System.getProperty("os.arch", "").lowercase()
                if (arch.contains("aarch64") || arch.contains("arm")) "RiPlay-v$v-macOS-arm64.dmg"
                else "RiPlay-v$v-macOS-x86_64.dmg"
            }
            os.contains("nix") || os.contains("nux") || os.contains("linux") -> {
                val isRpm = runCatching {
                    File("/etc/redhat-release").isFile || File("/etc/fedora-release").isFile ||
                            File("/etc/SuSE-release").isFile || File("/etc/centos-release").isFile
                }.getOrDefault(false)
                if (isRpm) "RiPlay-v$v-Linux.rpm" else "RiPlay-v$v-Linux.deb"
            }
            else -> null
        } ?: return null
        return "https://github.com/fast4x/RiPlay/releases/download/v$v/$asset"
    }

    /** TRUE se l'URL risponde 2xx (HEAD con redirect): evita di aprire un link 404 */
    private fun urlExists(url: String): Boolean = runCatching {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()
        client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() in 200..299
    }.getOrDefault(false)

    /** apre la pagina releases nel browser di sistema (best-effort) */
    fun openReleasesPage() = openInBrowser(RELEASES_URL)

    fun openInstallerDownload(versionName: String) {
        Thread {
            val direct = runCatching { installerDownloadUrl(versionName) }.getOrNull()
            val url = if (direct != null && urlExists(direct)) direct else RELEASES_URL
            println("[RiPlay-Update] download diretto ($versionName): $url")
            openInBrowser(url)
        }.apply {
            name = "riplay-update-download"
            isDaemon = true
            start()
        }
    }

    /** apertura nel browser di sistema (Desktop API + fallback per OS) */
    fun openInBrowser(url: String) {
        val opened = runCatching {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.isSupported(java.awt.Desktop.Action.BROWSE) &&
                    runCatching { desktop.browse(URI.create(url)) }.isSuccess
        }.getOrDefault(false)
        if (opened) return
        // fallback: comandi nativi (alcuni ambienti Linux non espongono BROWSE)
        runCatching {
            val os = System.getProperty("os.name", "").lowercase()
            val cmd = when {
                os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
                os.contains("mac") || os.contains("darwin") -> listOf("open", url)
                else -> listOf("xdg-open", url)
            }
            ProcessBuilder(cmd).start()
        }.onFailure { it.printStackTrace() }
    }

    /**
     * confronto semantico "a.b.c" (piu' componenti = ok, non numerici =
     * lessicografico come fallback). TRUE se [remote] e' piu' recente di [local].
     */
    internal fun isNameNewer(remote: String, local: String): Boolean {
        val r = remote.trim().removePrefix("v").split(".")
        val l = local.trim().removePrefix("v").split(".")
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val rv = r.getOrNull(i)?.toLongOrNull()
            val lv = l.getOrNull(i)?.toLongOrNull()
            when {
                rv != null && lv != null -> {
                    if (rv != lv) return rv > lv
                }
                // un componente numerico vs non numerico: il numerico vince
                rv != null && lv == null -> return true
                rv == null && lv != null -> return false
                else -> {
                    val rs = r.getOrNull(i) ?: ""
                    val ls = l.getOrNull(i) ?: ""
                    if (rs != ls) return rs > ls
                }
            }
        }
        return false
    }
}
