package it.fast4x.riplay.extensions.htmlscraper

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

fun shazamSongInfo(url: String, callback: (String, String, String?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val html = fetchHtml(url)

            if (html != null) {
                val doc = Jsoup.parse(html)

                // Cerca titolo della canzone
                var title = ""
                val titleElements = doc.select(".title, .song-title, .track-title, h1")
                if (titleElements.isNotEmpty()) {
                    title = titleElements.first()?.text()?.trim().orEmpty()
                } else {
                    val metaTitle = doc.selectFirst("meta[property='og:title']")?.attr("content")
                    if (metaTitle != null) title = metaTitle.trim()
                }

                // Cerca artista
                var artist = ""
                val artistElements = doc.select(".artist, .performer, .track-artist")
                if (artistElements.isNotEmpty()) {
                    artist = artistElements.first()?.text()?.trim().orEmpty()
                } else {
                    val metaArtist = doc.selectFirst("meta[property='og:artist']")?.attr("content")
                    if (metaArtist != null) artist = metaArtist.trim()
                }

                // Se non trova nulla, prova con altri selettori comuni
                if (title.isEmpty()) {
                    val titleFromMeta = doc.selectFirst("meta[name='title']")?.attr("content")
                    if (titleFromMeta != null) title = titleFromMeta.trim()
                }

                if (artist.isEmpty()) {
                    val artistFromMeta = doc.selectFirst("meta[name='artist']")?.attr("content")
                    if (artistFromMeta != null) artist = artistFromMeta.trim()
                }

                // Esegui il callback sul thread principale
                withContext(Dispatchers.Main) {
                    callback(artist, title, null)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback("", "", "Impossibile caricare la pagina")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback("", "", "Errore: ${e.message}")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun fetchHtml(urlString: String): String? {
    var connection: HttpURLConnection? = null
    return try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val inputStream: InputStream = connection.inputStream
        inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

fun shazamSongInfoExtractor(url: String, callback: (String, String, String?) -> Unit) {
    shazamSongInfo(url, { artistResult, songTitleResult, errorMessage ->
        callback (artistResult, songTitleResult, errorMessage)
        Timber.d("shazamSongInfoExtractor Artist: $artistResult")
        Timber.d("shazamSongInfoExtractor Song Title: $songTitleResult")
        Timber.d("shazamSongInfoExtractor Error Message: $errorMessage")
    })
}