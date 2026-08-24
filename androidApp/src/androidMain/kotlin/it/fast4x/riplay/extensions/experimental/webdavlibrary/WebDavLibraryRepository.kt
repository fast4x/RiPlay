package it.fast4x.riplay.extensions.experimental.webdavlibrary

import android.media.MediaMetadataRetriever
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavItem
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavSongMetadata
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.estimateDurationMillis
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.utils.saveByteArrayToFilesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.net.URLDecoder

class WebDavLibraryRepository() {
    val client = CustomHttpClient.okHttpClient

    // Il body XML per richiedere le proprietà di base
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

    suspend fun listDirectory(config: WebDavConfig, folderPath: String): List<WebDavItem> {
        val cleanBaseUrl = config.baseUrl.trimEnd('/') + "/"
        val baseHttpUrl = cleanBaseUrl.toHttpUrl()

        val cleanFolderPath = folderPath.trim()

        val targetUrl = when {
            cleanFolderPath.isEmpty() -> baseHttpUrl // Listiamo la root
            cleanFolderPath.startsWith("/") -> {
                val absolutePathWithSlash = if (cleanFolderPath.endsWith("/")) cleanFolderPath else "$cleanFolderPath/"
                baseHttpUrl.resolve(absolutePathWithSlash)
            }
            else -> {
                val relativePathWithSlash = if (cleanFolderPath.endsWith("/")) cleanFolderPath else "$cleanFolderPath/"
                baseHttpUrl.resolve(relativePathWithSlash)
            }
        } ?: throw IllegalArgumentException("WebDavLibraryRepository listDirectory URL non valido: $folderPath")

        Timber.d("WebDavLibraryRepository listDirectory targetUrl: $targetUrl")

        val request = Request.Builder()
            .url(targetUrl)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .header("Depth", "1")
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("WebDavLibraryRepository listDirectory WebDAV PROPFIND failed: ${response.code} ${response.message}")
                }
                val xmlInputStream = response.body?.byteStream()
                    ?: throw IOException("WebDavLibraryRepository listDirectory Empty response body from WebDAV")

                parseWebDavResponse(xmlInputStream)
            }
        }
    }

    // Se l'utente vuole scansionare in modo ricorsivo (utile per indicizzare tutta la musica)
    suspend fun listDirectoryRecursive(config: WebDavConfig, folderPath: String): List<WebDavItem> {
        val allItems = mutableListOf<WebDavItem>()

        val queue = ArrayDeque<String>()
        queue.add(folderPath)

        while (queue.isNotEmpty()) {
            val currentPath = queue.removeFirst()
            Timber.d("WebDavLibraryRepository listDirectoryRecursive > listDirectory called with folderPath: $currentPath")

            val items = try {
                listDirectory(config, currentPath)
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository listDirectoryRecursive Errore listando la cartella: $currentPath")
                emptyList() // Se fallisce, passiamo alla prossima
            }

            for (item in items) {

                val isSelf = currentPath.trimEnd('/') == item.href.trimEnd('/')

                if (isSelf) continue

                if (item.isDirectory) {
                    val dirPath = if (item.href.endsWith("/")) item.href else "$item.href/"
                    queue.add(dirPath)
                } else {
                    allItems.add(item)
                }
            }
        }
        return allItems
    }

    /**
     * Crea una cartella remota. Non fallisce se la cartella esiste già.
     */
    private suspend fun ensureRemoteFolderExists(config: WebDavConfig, folderPath: String) {
        val targetUrl = resolveUrl(config.baseUrl, folderPath)
        val request = Request.Builder()
            .url(targetUrl)
            .method("MKCOL", "".toRequestBody("application/xml".toMediaType()))
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                // 201 = Creata, 405 = Già esiste (Method Not Allowed).
                if (response.code != 201 && response.code != 405) {
                    throw IOException("Impossibile creare la cartella remota: ${response.code}")
                }
            }
        }
    }

    /**
     * Esegue il backup di un file locale in modo atomico (Upload .tmp -> MOVE).
     */
    suspend fun uploadFileAtomically(config: WebDavConfig, remoteFolder: String, localFile: File) {
        // 1. Assicurati che la cartella di backup esista
        ensureRemoteFolderExists(config, remoteFolder)

        val finalUrl = resolveUrl(config.baseUrl, "$remoteFolder/${localFile.name}")
        val tempUrl = resolveUrl(config.baseUrl, "$remoteFolder/${localFile.name}.tmp")

        // 2. Upload del file come .tmp
        val requestBody = localFile.asRequestBody("application/octet-stream".toMediaType())
        val putRequest = Request.Builder()
            .url(tempUrl)
            .put(requestBody)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(putRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Upload fallito: ${response.code}")
                }
            }

            // 3. MOVE atomica da .tmp al nome finale
            val moveRequest = Request.Builder()
                .url(tempUrl)
                .method("MOVE", "".toRequestBody("application/xml".toMediaType()))
                .header("Destination", finalUrl.toString())
                .header("Overwrite", "T") // T = True, sovrascrivi il vecchio backup
                .header("Authorization", Credentials.basic(config.username, config.password))
                .build()

            client.newCall(moveRequest).execute().use { response ->
                // 204 No Content è il successo standard per MOVE
                if (!response.isSuccessful && response.code != 204) {
                    throw IOException("Impossibile finalizzare il backup (MOVE fallito): ${response.code}")
                }
            }
        }
    }

    /**
     * Scarica il database remoto.
     */
    suspend fun downloadBackupFile(config: WebDavConfig, remoteFolder: String, localFile: File) {
        val targetUrl = resolveUrl(config.baseUrl, "$remoteFolder/${localFile.name}")
        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download fallito: ${response.code}")

                response.body?.byteStream()?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Body vuoto nel download")
            }
        }
    }

    // Helper per risolvere gli URL (semplificato, OkHttp è rigoroso sugli slash)
    private fun resolveUrl(base: String, path: String): okhttp3.HttpUrl {
        val fullUrl = base.trimEnd('/') + "/" + path.trimStart('/')
        return fullUrl.toHttpUrl()
    }

    suspend fun fetchMetadataFromRemoteFile(webDavConfig: WebDavConfig, remoteUrl: String): WebDavSongMetadata? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("Range", "bytes=0-524287") // Primi 512KB
                .header("Authorization", Credentials.basic(webDavConfig.username, webDavConfig.password))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                // Salva i byte scaricati in un file temporaneo
                val tempFile = File.createTempFile("webdav_meta", ".tmp")
                response.body?.byteStream()?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                // Usa MediaMetadataRetriever sul file temporaneo
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(tempFile.absolutePath)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

                    // Se non c'è il titolo, consideriamo il file "senza tag" e scartiamo
                    if (title.isNullOrBlank() && artist.isNullOrBlank()) {
                        return@withContext null
                    }

                    val thumbnailUrl = saveByteArrayToFilesDir(appContext(), retriever.embeddedPicture)
                    WebDavSongMetadata(
                        title = title.toString(),
                        artist = artist.toString(),
                        durationMs = durationMs ?: -1,
                        thumbnailUrl = thumbnailUrl
                    )
                } catch (e: Exception) {
                    null
                } finally {
                    retriever.release()
                    tempFile.delete() // Pulizia fondamentale!
                }
            }
        }
    }

}



fun parseWebDavResponse(inputStream: InputStream): List<WebDavItem> {
    val items = mutableListOf<WebDavItem>()
    val factory = XmlPullParserFactory.newInstance()
    factory.isNamespaceAware = true // Importante per WebDAV
    val parser = factory.newPullParser()
    parser.setInput(inputStream, "UTF-8")

    var event = parser.eventType
    var currentHref: String? = null
    var isDirectory = false
    var contentLength: Long = 0L
    var lastModified: String? = null
    var contentType: String? = null

    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "href" -> {
                        if (parser.next() == XmlPullParser.TEXT) {
                            currentHref = parser.text
                        }
                    }
                    "resourcetype" -> {
                        // Cerchiamo il tag <collection /> dentro <resourcetype>
                        // Se lo troviamo, è una directory
                        if (parser.next() == XmlPullParser.START_TAG && parser.name == "collection") {
                            isDirectory = true
                        }
                    }
                    "getcontentlength" -> {
                        if (parser.next() == XmlPullParser.TEXT) {
                            contentLength = parser.text.toLongOrNull() ?: 0L
                        }
                    }
                    "getlastmodified" -> {
                        if (parser.next() == XmlPullParser.TEXT) {
                            lastModified = parser.text
                        }
                    }
                    "getcontenttype" -> {
                        if (parser.next() == XmlPullParser.TEXT) {
                            contentType = parser.text
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                if (parser.name == "response") {
                    // Fine di un blocco <D:response>, salviamo l'item
                    if (currentHref != null) {
                        items.add(
                            WebDavItem(
                                href = currentHref!!,
                                isDirectory = isDirectory,
                                contentLength = contentLength,
                                lastModified = lastModified,
                                contentType = contentType
                            )
                        )
                    }
                    // Reset per il prossimo item
                    currentHref = null
                    isDirectory = false
                    contentLength = 0L
                    lastModified = null
                    contentType = null
                }
            }
        }
        event = parser.next()
    }
    return items
}

// Estensione per mappare i WebDavItem in Song
fun List<WebDavItem>.toSongs(baseUrl: String): List<Song> {
    val supportedExtensions = listOf("mp3", "flac", "ogg", "m4a", "wav", "opus", "webm")
    val baseHttpUrl = baseUrl.toHttpUrl()

    return this.mapNotNull { item ->
        if (item.isDirectory) return@mapNotNull null

        val extension = item.href.substringAfterLast(".", "").lowercase()
        if (extension !in supportedExtensions) return@mapNotNull null

        // Estrai il nome del file ignorando l'estensione per il titolo
        val fileNameEncoded = item.href.substringAfterLast("/").substringBeforeLast(".")

        // --- LA SOLUZIONE BULLETPROOF PER L'URL ---
        val absoluteUrl = when {
            // Se l'href è già un URL completo (es. http://...), usalo così com'è
            item.href.startsWith("http") -> item.href.toHttpUrl()

            // Se è un path assoluto che inizia con "/" (es. /remote.php/dav/files/admin/Music/brano.mp3)
            item.href.startsWith("/") -> {
                baseHttpUrl.newBuilder()
                    // Sostituisce TUTTO il path dell'URL base con il path assoluto di Nextcloud
                    .encodedPath(item.href)
                    .build()
            }

            // Fallback: se è un path relativo (non dovrebbe succedere con Nextcloud, ma per sicurezza)
            else -> baseHttpUrl.resolve(item.href)
        } ?: return@mapNotNull null

        val id = "$WEBDAV_KEY_PREFIX$absoluteUrl"
        // Decodifichiamo il nome del file per mostrarlo bene nella UI (es. %20 -> spazio)
        val title = URLDecoder.decode(fileNameEncoded, "UTF-8")
        val mediaId = title.substringAfterLast('[', "")
            .substringBeforeLast(']', "").takeIf { !it.contains(" ") }

        // Webdav non conosce la durata ma la lunghezza del file audio
        val duration = formatAsDuration(estimateDurationMillis(item.contentLength, extension))

        Timber.d("WebDavLibraryRepository toSongs: title = $title, mediaId = $mediaId contentLength = ${item.contentLength} duration = $duration")

        Song(
            id = id,
            title = title,
            mediaId = mediaId,
            durationText = duration,
            thumbnailUrl = null
        )
    }

}