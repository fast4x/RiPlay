package it.fast4x.riplay.extensions.htmlreader

import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.saveFileToInternalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

fun shazamSongInfo(url: String, callback: (String, String, String?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val html = fetchHtml(url)
            //saveFileToInternalStorage(appContext(), "shazam.html", html.orEmpty())

            if (html != null) {
                val doc = Jsoup.parse(html)
                var title = ""
                var artist = ""

                // --- ESTRAZIONE ARTISTA (A 2 livelli di fallback) ---
                val artistLink = doc.selectFirst("a[data-test-id='track_userevent_artist_link']")
                if (artistLink != null) {
                    artist = artistLink.text().trim().replace(Regex("[^a-zA-Z0-9àèéìòùÀÈÉÌÒÙ\\s\\-']"), "").trim()
                }


                // --- ESTRAZIONE TITOLO (A 5 livelli di fallback) ---

                // Livello 1: Ricerca "Contiene" (Copre qualsiasi variazione di ComponentName_trackTitle__)
                var titleEl = doc.selectFirst("[class*='trackTitle'], [class*='TrackTitle']")
                if (titleEl != null) {
                    title = titleEl.text().trim()
                }

                // Livello 2: Cerca un Tag H1 o H2 che abbia le classi di testo di Shazam
                if (title.isEmpty()) {
                    titleEl = doc.selectFirst("h1[class*='Text-module'], h2[class*='Text-module']")
                    if (titleEl != null) {
                        title = titleEl.text().trim()
                    }
                }

                // Livello 3: Fallback dal Canonical URL
                if (title.isEmpty()) {
                    val canonical = doc.selectFirst("link[rel='canonical']")?.attr("href")
                    if (canonical != null) {
                        // Cerca dove si trova "/track/" nell'URL, indipendentemente dalla lunghezza
                        val trackIndex = canonical.indexOf("/track/")
                        if (trackIndex != -1) {
                            val afterTrack = canonical.substring(trackIndex + 7) // Salta "/track/"
                            // afterTrack sarà "690343079/i-want-to-quit-my-job"
                            val parts = afterTrack.split("/")
                            if (parts.size > 1 && parts[1].isNotEmpty()) {
                                title = formatSlug(parts[1])
                            }
                        }
                    }
                }

                // Livello 4: "Nuclear Option" - Sfrutta il DOM fratello dell'artista
                // Shazam mette quasi sempre Artista e Titolo nello stesso contenitore padre.
                if (title.isEmpty() && artistLink != null) {
                    val parent = artistLink.parent() // Il div che contiene il link dell'artista
                    val grandParent = parent?.parent() // Il div che contiene sia il titolo che l'artista
                    if (grandParent != null) {
                        // Prendi tutti i div figli diretti
                        val children = grandParent.children().select("div")
                        // Cerca il div che NON contiene il link dell'artista (sarà il div del titolo)
                        val titleContainer = children.firstOrNull {
                            it.select("a[data-test-id='track_userevent_artist_link']").isEmpty()
                        }
                        if (titleContainer != null) {
                            title = titleContainer.text().trim()
                        }
                    }
                }

                // Livello 5: Fallback artista dall'URL (se per qualche motivo fallisse anche quello)
                if (artist.isEmpty()) {
                    val artistHref = doc.selectFirst("a[href^='/artist/']")?.attr("href")
                    if (artistHref != null) {
                        val segments = artistHref.split("/")
                        if (segments.size >= 3) artist = formatSlug(segments[2])
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(artist, title, null)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback("", "", "ShazamSongInfo Errore: Impossibile caricare la pagina")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback("", "", "ShazamSongInfo Errore: ${e.message}")
            }
        }
    }
}

/**
 * Trasforma "i-want-to-quit-my-job" in "I Want To Quit My Job"
 */
private fun formatSlug(slug: String): String {
    return slug.replace("-", " ")
        .split(" ")
        .joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            } else {
                word
            }
        }
}

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
    })
}