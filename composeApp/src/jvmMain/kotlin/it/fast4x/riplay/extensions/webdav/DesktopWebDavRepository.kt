package it.fast4x.riplay.extensions.webdav

import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.utils.DesktopCryptoManager
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * PORTING DESKTOP di WebDavLibraryRepository.kt (androidApp): client WebDAV
 * con OkHttp per la libreria cloud (account, test connessione, backup e
 * restore del database).
 *
 * Stesso protocollo dell'originale:
 *  - PROPFIND Depth 1 per elencare una cartella (XML multistatus);
 *  - MKCOL per creare la cartella di backup (tolera 405 = gia' esistente);
 *  - upload ATOMICO: PUT su <nome>.tmp -> DELETE del vecchio -> MOVE;
 *  - upload DIRETTO con verifica post-timeout (HEAD, dimensione attesa);
 *  - download via GET;
 *  - le credenziali viaggiano in Basic Auth decriptando la password al volo
 *    (DesktopCryptoManager, AES/GCM compatibile con Android).
 *
 * Le differenze JVM sono soltanto:
 *  - parsing XML con javax.xml.parsers (DOM namespace-aware) invece di
 *    XmlPullParser (android-specific), con XXE hardening;
 *  - no MediaMetadataRetriever (Android-only): la metadata dei brani
 *    scansionati resta quella ricavata dal nome file + durata stimata
 *    (la FASE 1 del flusso Android, sufficiente finche' i tag veri non
 *    arrivano dal player).
 */
class DesktopWebDavRepository(
    /** Client condiviso; il manager puo' passare uno gia' configurato. */
    client: OkHttpClient? = null
) {

    val client: OkHttpClient = client ?: OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val propfindBody = """
        <?xml version="1.0" encoding="utf-8" ?>
        <D:propfind xmlns:D="DAV:">
            <D:prop>
                <D:resourcetype/>
                <D:getcontentlength/>
                <D:getcontenttype/>
                <D:getlastmodified/>
            </D:prop>
        </D:propfind>
    """.trimIndent()

    // ── ELENCO CARTELLA (PROPFIND) ──────────────────────────────────────────

    suspend fun listDirectory(account: WebDavAccount, folder: String? = null): List<WebDavItem> {
        val rawPassword = DesktopCryptoManager.decrypt(account.encryptedPassword)
        val authHeader = Credentials.basic(account.username, rawPassword)
        val targetUrl = resolveUrl(account.baseUrl, folder ?: account.remoteFolder)

        val request = Request.Builder()
            .url(targetUrl)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .header("Depth", "1")
            .header("Authorization", authHeader)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("PROPFIND failed: ${response.code} ${response.message}")
                }
                val body = response.body?.byteStream()
                    ?: throw IOException("Empty response body from WebDAV")
                parseWebDavMultistatus(body)
            }
        }
    }

    /** Elenco ricorsivo (BFS, come listMusicDirectoryRecursive Android). */
    suspend fun listDirectoryRecursive(account: WebDavAccount): List<WebDavItem> {
        val allItems = mutableListOf<WebDavItem>()
        val queue = ArrayDeque<String>()
        queue.add(account.remoteFolder)

        val basePath = try {
            account.baseUrl.toHttpUrl().encodedPath.trimEnd('/')
        } catch (e: Exception) {
            ""
        }

        while (queue.isNotEmpty()) {
            val currentPath = queue.removeFirst()
            val items = try {
                listDirectory(account.copy(remoteFolder = currentPath)).drop(1)
            } catch (e: Exception) {
                println("[RiPlay-WebDAV] errore listando $currentPath: ${e.message}")
                emptyList()
            }

            for (item in items) {
                if (item.isDirectory) {
                    var dirPath = item.href.trimEnd('/')
                    if (basePath.isNotEmpty() && dirPath.startsWith(basePath)) {
                        dirPath = dirPath.removePrefix(basePath)
                    }
                    if (!dirPath.endsWith("/")) dirPath += "/"
                    queue.add(dirPath)
                } else {
                    allItems.add(item)
                }
            }
        }
        return allItems
    }

    // ── CARTELLA REMOTA ──────────────────────────────────────────────────────

    /** Crea una cartella remota. Non fallisce se la cartella esiste gia' (405). */
    suspend fun ensureRemoteFolderExists(config: WebDavConfig, folderPath: String) {
        val targetUrl = resolveUrl(config.baseUrl, folderPath)
        val request = Request.Builder()
            .url(targetUrl)
            .method("MKCOL", "".toRequestBody("application/xml".toMediaType()))
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                // 201 = creata, 405 = gia' esiste (Method Not Allowed)
                if (response.code != 200 && response.code != 201 && response.code != 405) {
                    throw IOException("Impossibile creare la cartella remota: ${response.code}")
                }
            }
        }
    }

    // ── UPLOAD ──────────────────────────────────────────────────────────────

    /** Upload ATOMICO (PUT .tmp -> DELETE vecchio -> MOVE), come Android. */
    suspend fun uploadFileAtomically(config: WebDavConfig, remoteFolder: String, localFile: File) {
        ensureRemoteFolderExists(config, remoteFolder)

        val baseUrl = config.baseUrl.trimEnd('/')
        val folderPath = remoteFolder.trim('/')
        val finalUrlStr = "$baseUrl/$folderPath/${localFile.name}"
        val tempUrlStr = "$finalUrlStr.tmp"
        val finalUrl = finalUrlStr.toHttpUrl()
        val tempUrl = tempUrlStr.toHttpUrl()

        val requestBody = localFile.asRequestBody("application/octet-stream".toMediaType())
        val putRequest = Request.Builder()
            .url(tempUrl)
            .put(requestBody)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        // Client paziente per operazioni critiche (db potenzialmente grande)
        val patientClient = client.newBuilder()
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .build()

        withContext(Dispatchers.IO) {
            // FASE 1: upload del .tmp
            try {
                client.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Upload fallito: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                println("[RiPlay-WebDAV] errore PUT ${localFile.name}: ${e.message}")
                runCatching { deleteFile(config, tempUrlStr) }
                throw e
            }

            // FASE 2: eliminazione preventiva del vecchio (404 bene: primo backup)
            try {
                val deleteRequest = Request.Builder()
                    .url(finalUrl)
                    .delete()
                    .header("Authorization", Credentials.basic(config.username, config.password))
                    .build()
                patientClient.newCall(deleteRequest).execute().use { }
            } catch (e: Exception) {
                println("[RiPlay-WebDAV] eliminazione preventiva fallita (probabilmente non esisteva): ${e.message}")
            }

            // FASE 3: MOVE atomico
            val moveRequest = Request.Builder()
                .url(tempUrl)
                .method("MOVE", "".toRequestBody("application/xml".toMediaType()))
                .header("Destination", finalUrl.toString())
                .header("Authorization", Credentials.basic(config.username, config.password))
                .build()
            try {
                patientClient.newCall(moveRequest).execute().use { response ->
                    if (!response.isSuccessful && response.code != 204 && response.code != 201) {
                        throw IOException("MOVE fallito: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                println("[RiPlay-WebDAV] errore MOVE ${localFile.name}: ${e.message}")
                throw e
            }
        }
    }

    /** Upload diretto con verifica post-timeout (HEAD, come uploadFileDirect Android). */
    suspend fun uploadFileDirect(config: WebDavConfig, remoteFolder: String, localFile: File) {
        val baseUrl = config.baseUrl.trimEnd('/')
        val folderPath = remoteFolder.trim('/')
        val finalUrlStr = "$baseUrl/$folderPath/${localFile.name}"
        val finalUrl = finalUrlStr.toHttpUrl()

        val requestBody = localFile.asRequestBody("application/octet-stream".toMediaType())
        val putRequest = Request.Builder()
            .url(finalUrl)
            .put(requestBody)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .header("Connection", "close")
            .build()

        val patientClient = client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .build()

        withContext(Dispatchers.IO) {
            println("[RiPlay-WebDAV] upload diretto ${localFile.name} (${localFile.length()} bytes)...")
            try {
                patientClient.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Upload fallito: ${response.code}")
                    }
                    println("[RiPlay-WebDAV] upload completato per ${localFile.name}")
                }
            } catch (e: Exception) {
                val isTimeoutError = e is java.net.SocketTimeoutException || e is java.net.SocketException ||
                        (e.cause != null && e.cause is java.net.SocketException)
                if (isTimeoutError) {
                    println("[RiPlay-WebDAV] timeout durante l'upload: verifico se il server ha salvato il file...")
                    Thread.sleep(3000)
                    val fileExists = checkRemoteFileExists(patientClient, config, finalUrlStr, localFile.length())
                    if (fileExists) {
                        println("[RiPlay-WebDAV] verifica riuscita: il file ${localFile.name} e' sul server, upload valido")
                    } else {
                        println("[RiPlay-WebDAV] verifica fallita: cleanup e errore")
                        runCatching { deleteFile(config, finalUrlStr) }
                        throw e
                    }
                } else {
                    runCatching { deleteFile(config, finalUrlStr) }
                    throw e
                }
            }
        }
    }

    private suspend fun checkRemoteFileExists(
        patientClient: OkHttpClient,
        config: WebDavConfig,
        fileUrlStr: String,
        expectedSize: Long
    ): Boolean {
        val request = Request.Builder()
            .url(fileUrlStr.toHttpUrl())
            .head()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()
        return withContext(Dispatchers.IO) {
            try {
                patientClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val contentLength = response.header("Content-Length")?.toLongOrNull()
                        if (contentLength != null && contentLength != expectedSize) return@use false
                        return@use true
                    }
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun deleteFile(config: WebDavConfig, fileUrlStr: String) {
        val request = Request.Builder()
            .url(fileUrlStr.toHttpUrl())
            .delete()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().close()
        }
    }

    // ── DOWNLOAD ────────────────────────────────────────────────────────────

    suspend fun downloadFile(config: WebDavConfig, remoteFilePath: String, localTempFile: File) {
        val targetUrl = resolveUrl(config.baseUrl, remoteFilePath)
        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download fallito: ${response.code}")
                response.body?.byteStream()?.use { input ->
                    localTempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Body vuoto nel download")
            }
        }
    }

    // ── INFO BACKUP ─────────────────────────────────────────────────────────

    /**
     * Info del backup remoto (riplay_meta.json): timestamp + nome dispositivo.
     * Legge dalla cartella RiPlayBackup, come fetchBackupInfo Android.
     */
    suspend fun fetchBackupInfo(config: WebDavConfig): WebDavBackupInfo? {
        return try {
            val tempMetaFile = File.createTempFile("riplay_meta", ".json")
            downloadFile(config, "$WEBDAV_DEFAULT_BACKUP_FOLDER/$WEBDAV_DEFAULT_BACKUP_METADATA_FILE", tempMetaFile)
            val json = tempMetaFile.readText()
            tempMetaFile.delete()
            DesktopWebDavJson.decodeFromString<WebDavBackupInfo>(json)
        } catch (e: Exception) {
            null // Il backup non esiste ancora
        }
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────

    private fun resolveUrl(base: String, path: String): HttpUrl {
        val fullUrl = base.trimEnd('/') + "/" + path.trimStart('/')
        return fullUrl.toHttpUrl()
    }
}

/** Json condiviso per la metadata di backup (come JsonManager Android). */
val DesktopWebDavJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

// ── COSTANTI DI PROTOCOLLO (come WebDavLibraryViewModel Android) ─────────────
const val WEBDAV_DEFAULT_BACKUP_FOLDER = "RiPlayBackup"
const val WEBDAV_DEFAULT_BACKUP_DB_FILE = "riplay_sync.db"
const val WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE = "riplay_sync.db.zip"
const val WEBDAV_DEFAULT_BACKUP_METADATA_FILE = "riplay_meta.json"

// ── MODELLI (porting di extensions/webdavlibrary/models/WebDav.kt) ───────────

data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: String
)

data class WebDavItem(
    val href: String,
    val isDirectory: Boolean,
    val contentLength: Long,
    val lastModified: String?,
    val contentType: String?
)

@kotlinx.serialization.Serializable
data class WebDavBackupInfo(
    val timestamp: Long,
    val deviceName: String?
)

// ── PARSING XML (substituto namespace-aware di parseWebDavResponse) ──────────

/**
 * Parsing del multistatus PROPFIND con DOM javax.xml (namespace-aware, XXE
 * hardening). Estrae href/resourcetype/getcontentlength/getlastmodified/
 * getcontenttype da OGNI <response> (D:response, d:response, response...).
 */
fun parseWebDavMultistatus(inputStream: java.io.InputStream): List<WebDavItem> {
    val items = mutableListOf<WebDavItem>()
    val factory = DocumentBuilderFactory.newInstance()
    // XXE hardening: nessuna DTD/entita' esterna (risposte WebDAV fidate solo XML)
    try {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    } catch (t: Throwable) { /* parser senza questo feature: seguono i set generic */ }
    factory.isNamespaceAware = true
    factory.isExpandEntityReferences = false

    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(inputStream)

    fun localName(node: Node): String = node.localName ?: node.nodeName.substringAfterLast(':', node.nodeName)

    fun directChildByName(parent: Element, name: String): Element? {
        var child: Node? = parent.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE && localName(child).equals(name, ignoreCase = true)) {
                return child as Element
            }
            child = child.nextSibling
        }
        return null
    }

    fun hasCollectionChild(parent: Element): Boolean {
        var child: Node? = parent.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE &&
                localName(child).equals("collection", ignoreCase = true)
            ) return true
            child = child.nextSibling
        }
        return false
    }

    val responseNodes = doc.getElementsByTagName("*")
    val responses = (0 until responseNodes.length)
        .map { responseNodes.item(it) as Element }
        .filter { localName(it).equals("response", ignoreCase = true) }

    for (response in responses) {
        val hrefEl = directChildByName(response, "href") ?: continue
        val href = hrefEl.textContent?.trim() ?: continue

        val propEl = directChildByName(response, "propstat")
            ?.let { directChildByName(it, "prop") }
        var isDirectory = false
        var contentLength = 0L
        var lastModified: String? = null
        var contentType: String? = null

        if (propEl != null) {
            val rt = directChildByName(propEl, "resourcetype")
            if (rt != null) isDirectory = hasCollectionChild(rt)
            directChildByName(propEl, "getcontentlength")?.textContent?.trim()
                ?.let { contentLength = it.toLongOrNull() ?: 0L }
            directChildByName(propEl, "getlastmodified")?.textContent?.trim()
                ?.let { lastModified = it }
            directChildByName(propEl, "getcontenttype")?.textContent?.trim()
                ?.let { contentType = it }
        }

        items.add(
            WebDavItem(
                href = href,
                isDirectory = isDirectory,
                contentLength = contentLength,
                lastModified = lastModified,
                contentType = contentType
            )
        )
    }
    return items
}

// ── MAPPATURA ITEM -> SONG (come List<WebDavItem>.toSongs Android) ───────────

/** Durata stimata dal peso del file (bitrate medio per formato), come Utils.estimateDurationMillis. */
fun estimateDurationMillis(contentLength: Long, extension: String): Long {
    if (contentLength <= 0) return 0L
    val assumedBitrateKbps = when (extension.lowercase()) {
        "mp3", "m4a", "aac" -> 190
        "opus", "ogg" -> 128
        "flac", "wav" -> 1000
        else -> 128
    }
    val estimatedSeconds = (contentLength * 8) / (assumedBitrateKbps * 1000)
    return estimatedSeconds * 1000
}

/** Durata formattata (h:mm:ss o m:ss), come Utils.formatAsDuration. */
fun formatAsDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

private val supportedExtensions = listOf("mp3", "flac", "ogg", "m4a", "wav", "opus", "webm")

/** Converte gli item WebDAV in Song (id "webdav:<url>"), come toSongs Android. */
fun List<WebDavItem>.toSongs(baseUrl: String): List<it.fast4x.riplay.data.models.Song> {
    val baseHttpUrl = baseUrl.toHttpUrl()
    return mapNotNull { item ->
        if (item.isDirectory) return@mapNotNull null

        val extension = item.href.substringAfterLast(".", "").lowercase()
        if (extension !in supportedExtensions) return@mapNotNull null

        val fileNameEncoded = item.href.substringAfterLast("/").substringBeforeLast(".")

        val absoluteUrl = when {
            item.href.startsWith("http") -> item.href.toHttpUrl()
            item.href.startsWith("/") ->
                baseHttpUrl.newBuilder().encodedPath(item.href).build()
            else -> baseHttpUrl.resolve(item.href)
        } ?: return@mapNotNull null

        val id = "$WEBDAV_KEY_PREFIX$absoluteUrl"
        val title = try {
            URLDecoder.decode(fileNameEncoded, "UTF-8")
        } catch (e: Exception) {
            fileNameEncoded
        }
        val mediaId = title.substringAfterLast('[', "")
            .substringBeforeLast(']', "").takeIf { !it.contains(" ") }

        val duration = formatAsDuration(estimateDurationMillis(item.contentLength, extension))

        it.fast4x.riplay.data.models.Song(
            id = id,
            title = title,
            mediaId = mediaId,
            durationText = duration,
            thumbnailUrl = null
        )
    }
}
