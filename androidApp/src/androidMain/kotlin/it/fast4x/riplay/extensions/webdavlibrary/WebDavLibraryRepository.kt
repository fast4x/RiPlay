package it.fast4x.riplay.extensions.webdavlibrary

import android.media.MediaMetadataRetriever
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavBackupInfo
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavItem
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavSongMetadata
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.JsonManager
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.estimateDurationMillis
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.saveByteArrayToFilesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class WebDavLibraryRepository() {
    val client = CustomHttpClient.okHttpClient

    val context = appContext()

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

    suspend fun listMusicDirectory(account: WebDavAccount): List<WebDavItem> {
        // Usiamo l'account.baseUrl e decriptiamo la password al volo per la singola richiesta PROPFIND
        val rawPassword = CryptoManager.decrypt(account.encryptedPassword)
        val authHeader = Credentials.basic(account.username, rawPassword)

        val targetUrl = resolveUrl(account.baseUrl, account.remoteFolder)

        val request = Request.Builder()
            .url(targetUrl)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .header("Depth", "1")
            .header("Authorization", authHeader)
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
    suspend fun listMusicDirectoryRecursive(account: WebDavAccount): List<WebDavItem> {
        val allItems = mutableListOf<WebDavItem>()

        val queue = ArrayDeque<String>()
        queue.add(account.remoteFolder)

        // Estraiamo il percorso base dall'URL dell'account per poterlo rimuovere dagli href assoluti
        // Es. se baseUrl è "https://server.com/remote.php/dav/files/admin", basePath sarà "/remote.php/dav/files/admin"
        val basePath = try {
            account.baseUrl.toHttpUrl().encodedPath.trimEnd('/')
        } catch (e: Exception) {
            ""
        }

        while (queue.isNotEmpty()) {
            val currentPath = queue.removeFirst()
            Timber.d("WebDavLibraryRepository listDirectoryRecursive > listDirectory called with folderPath: $currentPath")

            val items = try {
                listMusicDirectory(account.copy(remoteFolder = currentPath)).drop(1)
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository listDirectoryRecursive Errore listando la cartella: $currentPath")
                emptyList()
            }

            for (item in items) {
                if (item.isDirectory) {
                    var dirPath = item.href.trimEnd('/')

                    // Se l'href è assoluto, gli togliamo il basePath per renderlo relativo
                    if (basePath.isNotEmpty() && dirPath.startsWith(basePath)) {
                        dirPath = dirPath.removePrefix(basePath)
                    }

                    // Assicuriamoci che abbia lo slash finale per il prossimo ciclo
                    if (!dirPath.endsWith("/")) dirPath += "/"

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
                if (response.code != 200 && response.code != 201 && response.code != 405) {
                    throw IOException("Impossibile creare la cartella remota: ${response.code}")
                }
            }
        }
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

    /**
     * Esegue il backup di un file locale in modo atomico (Upload .tmp -> MOVE).
     */
    suspend fun uploadFileAtomically(config: WebDavConfig, remoteFolder: String, localFile: File) {
        // 1. Assicurati che la cartella di backup esista
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

        // Client paziente per operazioni critiche
        val patientClient = client.newBuilder()
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .build()

        withContext(Dispatchers.IO) {

            // --- FASE 1: UPLOAD DEL .tmp ---
            try {
                client.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("WebDavLibraryRepository Upload fallito: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository Errore durante il PUT di ${localFile.name}")
                try { deleteFile(config, tempUrlStr) } catch (_: Exception) {}
                throw e
            }

            // --- FASE 2: ELIMINAZIONE PREVENTIVA ---
            // Cancelliamo il vecchio file .db.
            // Se il file non esiste ancora (primo backup), pCloud risponderà 404, e va benissimo!
            try {
                val deleteRequest = Request.Builder()
                    .url(finalUrl)
                    .delete()
                    .header("Authorization", Credentials.basic(config.username, config.password))
                    .build()

                client.newCall(deleteRequest).execute().use { response ->
                    Timber.d("WebDavLibraryRepository Eliminazione preventiva di ${localFile.name}: ${response.code}")
                }
            } catch (e: Exception) {
                Timber.w("WebDavLibraryRepository Eliminazione preventiva fallita (probabilmente non esisteva): ${e.message}")
            }

            // --- FASE 3: MOVE ATOMICO ---
            // Ora il vecchio file è sparito. Il MOVE non ha ostacoli.
            val moveRequest = Request.Builder()
                .url(tempUrl)
                .method("MOVE", "".toRequestBody("application/xml".toMediaType()))
                .header("Destination", finalUrl.toString())
                .header("Authorization", Credentials.basic(config.username, config.password))
                .build()

            try {
                client.newCall(moveRequest).execute().use { response ->
                    if (!response.isSuccessful && response.code != 204 && response.code != 201) {
                        throw IOException("WebDavLibraryRepository MOVE fallito: ${response.code}")
                    }
                    Timber.d("WebDavLibraryRepository MOVE response code = ${response.code}")
                }
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository Errore durante il MOVE di ${localFile.name}")
                throw e
            }
        }
    }

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
            .header("Expect", "") // Non comunica la grandezza del file da inviare (rimuove dall'header "Expect: 100-continue)
            .build()

        val patientClient = client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .build()

        withContext(Dispatchers.IO + NonCancellable) {
            try {
                Timber.d("WebDavLibraryRepository Inizio upload diretto per ${localFile.name} (${localFile.length()} bytes)...")
                patientClient.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("WebDavLibraryRepository Upload fallito: ${response.code}")
                    }
                    Timber.d("WebDavLibraryRepository Upload completato con successo per ${localFile.name}")
                }
            } catch (e: Exception) {
                // Controlliamo se l'errore è il famigerato Timeout o Socket Chiuso
                val isTimeoutError = e is SocketTimeoutException || e is SocketException ||
                        (e.cause != null && e.cause is SocketException)

                if (isTimeoutError) {
                    Timber.w("WebDavLibraryRepository Timeout/Chiusura durante l'upload di ${localFile.name}. Verifico se il server ha comunque salvato il file...")

                    // Aspettiamo 3 secondi per dare il tempo al server di finire di scrivere sul disco
                    delay(3000.milliseconds)

                    // Verifichiamo se il file esiste e ha la dimensione corretta
                    val fileExists = checkRemoteFileExists(patientClient, config, finalUrlStr, localFile.length())

                    if (fileExists) {
                        // IL TRUCCO FUNZIONA! Il server ha il file, ignoriamo l'errore di timeout.
                        Timber.d("WebDavLibraryRepository Verifica riuscita! Il file ${localFile.name} è presente sul server. Considero l'upload valido.")
                    } else {
                        // Il file non c'è o ha dimensione sbagliata, l'upload è davvero fallito.
                        Timber.e(e, "WebDavLibraryRepository Verifica fallita. Il file non è sul server. Pulizia in corso...")
                        try { deleteFile(config, finalUrlStr) } catch (_: Exception) {}
                        throw e
                    }
                } else {
                    // Errore diverso (es. 401 Non autorizzato, 500 Errore server)
                    Timber.e(e, "WebDavLibraryRepository Errore non di timeout durante l'upload di ${localFile.name}.")
                    try { deleteFile(config, finalUrlStr) } catch (_: Exception) {}
                    throw e
                }
            }
        }
    }


    /**
     * Invia una richiesta HEAD per vedere se il file esiste e quanti byte pesa.
     */
    private suspend fun checkRemoteFileExists(client: OkHttpClient, config: WebDavConfig, fileUrlStr: String, expectedSize: Long): Boolean {
        val request = Request.Builder()
            .url(fileUrlStr.toHttpUrl())
            .head() // HEAD è più leggero del GET, scarica solo gli header
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        // Opzionale: controlla anche la dimensione se il server la fornisce
                        val contentLength = response.header("Content-Length")?.toLongOrNull()
                        if (contentLength != null && contentLength != expectedSize) {
                            Timber.w("WebDavLibraryRepository Il file esiste ma la dimensione non combacia: $contentLength vs $expectedSize")
                            return@withContext false
                        }
                        return@withContext true
                    }
                    false
                }
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository Errore durante la verifica di esistenza del file")
                false
            }
        }
    }

    private suspend fun deleteFile(config: WebDavConfig, fileUrlStr: String) {
        val request = Request.Builder()
            .url(fileUrlStr.toHttpUrl())
            .delete()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().close()
        }
    }

    /**
     * Recupera la data di ultima modifica del file remoto.
     * Ritorna un Long (millisecondi epoca) o 0 se il file non esiste.
     */
    suspend fun getRemoteFileLastModified(config: WebDavConfig, remoteFilePath: String): Long {
        val targetUrl = resolveUrl(config.baseUrl, remoteFilePath)

        val propfindBody = """
        <?xml version="1.0" encoding="utf-8" ?>
        <D:propfind xmlns:D="DAV:">
            <D:prop>
                <D:getlastmodified/>
            </D:prop>
        </D:propfind>
    """.trimIndent()

        val request = Request.Builder()
            .url(targetUrl)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .header("Depth", "0") // Fondamentale: interroga solo il file, non lista la cartella
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext 0L // File non esiste ancora
                if (!response.isSuccessful) throw IOException("PROPFIND fallito: ${response.code}")

                val xml = response.body?.string() ?: return@withContext 0L

                // Estrai la data dal tag <D:getlastmodified>...</D:getlastmodified>
                val regex = "<[^>]*getlastmodified[^>]*>(.*?)<".toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(xml)

                if (match != null) {
                    try {
                        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                        val date = format.parse(match.groupValues[1])
                        date?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                } else {
                    0L
                }
            }
        }
    }

    /**
     * Scarica un file remoto in un file locale.
     */
    suspend fun downloadFile(config: WebDavConfig, remoteFilePath: String, localTempFile: File) {
        val targetUrl = resolveUrl(config.baseUrl, remoteFilePath)

        Timber.d("WebDavLibraryRepository downloadFile: Scaricamento di $remoteFilePath in corso...")

        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Authorization", Credentials.basic(config.username, config.password))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("WebDavLibraryRepository Download fallito: ${response.code}")

                response.body?.byteStream()?.use { input ->
                    localTempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("WebDavLibraryRepository Body vuoto nel download")
            }
        }
    }

    // Helper per risolvere gli URL (semplificato, OkHttp è rigoroso sugli slash)
    private fun resolveUrl(base: String, path: String): HttpUrl {
        val fullUrl = base.trimEnd('/') + "/" + path.trimStart('/')
        return fullUrl.toHttpUrl()
    }

    // Funzione per leggere le info del backup remoto
    suspend fun fetchBackupInfo(config: WebDavConfig): WebDavBackupInfo? {
        return try {
            val tempMetaFile = File(context.cacheDir, "temp_meta.json")
            downloadFile(config, "$WEBDAV_DEFAULT_BACKUP_FOLDER/$WEBDAV_DEFAULT_BACKUP_METADATA_FILE", tempMetaFile)

            val json = try {
                tempMetaFile.readText()
            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryRepository fetchBackupInfo: Errore lettura temp_meta.json")
                return null
            }

            tempMetaFile.delete()

            // Parsa il JSON e ritorna data e nome dispositivo
            val info = JsonManager.decodeFromString<WebDavBackupInfo>(json)
            info
        } catch (e: Exception) {
            null // Il backup non esiste ancora
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