package it.fast4x.riplay.extensions.shazam

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
import java.util.Locale


import java.util.regex.Pattern

/**
 * Funzione principale da chiamare quando si riceve un testo condiviso (Intent).
 * Capisce da solo se deve estrarre i dati dal testo o se deve scaricare la pagina.
 */
suspend fun handleShazamShare(sharedText: String, callback: (String, String, String?) -> Unit) {
    if (sharedText.isEmpty()) {
        callback("", "", "Testo condiviso vuoto")
        return
    }

    // Controllo se il testo corrisponde al formato dell'App Nativa di Shazam:
    // "Titolo canzone di Nome Artista https://www.shazam.com/..."
    val appSharePattern = Pattern.compile("^(.*?)\\s+di\\s+(.*?)\\s+(https?://\\S+)$", Pattern.CASE_INSENSITIVE)
    val matcher = appSharePattern.matcher(sharedText.trim())

    if (matcher.find()) {
        val title = matcher.group(1)?.trim().orEmpty()
        val artist = matcher.group(2)?.trim().orEmpty()
        // val url = matcher.group(3) // Non serve, c'è già artista e titolo nel testo condiviso dall'app di shazam

        if (title.isNotEmpty() && artist.isNotEmpty()) {
            Timber.d("handleShazamShare Dati estratti direttamente dal testo condiviso - Titolo: $title, Artista: $artist")
            withContext(Dispatchers.Main) {
                callback(artist, title, null)
            }
            return // Nessuna chiamata di rete necessaria!
        }
    }

    // Se si arriva qui, significa che è una condivisione dal browser (solo URL puro).
    // Estraggo l'URL dal testo ed effettuo la chiamata
    val urlRegex = Regex("https?://[\\w\\-]+(\\.[\\w\\-]+)+\\S*")
    val url = urlRegex.find(sharedText)?.value

    if (url != null) {
        Timber.d("Condivisione da browser rilevata, avvio download HTML per URL: $url")
        shazamSongInfo(url, callback)
    } else {
        withContext(Dispatchers.Main) {
            callback("", "", "Nessun URL Shazam valido trovato nel testo")
        }
    }
}

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
        //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")

        Timber.d("fetchHtml urlString: $urlString")
        connection.connect()

        val inputStream: InputStream = connection.inputStream
        inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        Timber.e("fetchHtml Exception: ${e.message}")
        null
    } finally {
        connection?.disconnect()
    }
}