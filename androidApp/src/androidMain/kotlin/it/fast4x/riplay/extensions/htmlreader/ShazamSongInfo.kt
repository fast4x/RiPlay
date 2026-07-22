package it.fast4x.riplay.extensions.htmlreader

import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.saveFileToInternalStorage
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

fun shazamSongInfo(url: String, callback: (String, String, String?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val html = fetchHtml(url)

            //saveFileToInternalStorage(appContext(), "shazam.html", html.orEmpty())

            if (html != null) {
                val doc = Jsoup.parse(html)
                var title = ""
                var artist = ""

                // --- METODO 1: Selettori CSS "Resilienti" ---

                // Titolo: usiamo "^=" che significa "inizia con".
                // In questo modo ignoriamo l'hash __YYbL7 che cambierà domani!
                val titleEl = doc.selectFirst("div[class^='NewTrackPageHeader_trackTitle__']")
                if (titleEl != null) {
                    title = titleEl.text().trim()
                }

                // Artista: usiamo il data-test-id che è rimasto stabile e non ha hash
                val artistLink = doc.selectFirst("a[data-test-id='track_userevent_artist_link']")
                if (artistLink != null) {
                    // Prendiamo il testo. Se Jsoup prende anche testi di icone interni, facciamo un po' di pulizia
                    artist = artistLink.text().trim().replace(Regex("[^a-zA-Z0-9àèéìòùÀÈÉÌÒÙ\\s\\-']"), "").trim()
                }


                // --- METODO 2: Fallback dal Canonical URL (BULLET-PROOF al 100%) ---
                // Se Shazam cambia radicalmente i nomi delle classi o i data-test-id,
                // l'URL canonico rimarrà sempre formattato così per ragioni di SEO.

                if (title.isEmpty()) {
                    val canonical = doc.selectFirst("link[rel='canonical']")?.attr("href")
                    if (canonical != null) {
                        val segments = canonical.split("/")
                        // Esempio URL: https://www.shazam.com/track/690343079/i-want-to-quit-my-job
                        if (segments.size >= 6 && segments[3] == "track") {
                            // segments[5] è "i-want-to-quit-my-job"
                            title = formatSlug(segments[5])
                        }
                    }
                }

                if (artist.isEmpty()) {
                    // Cerchiamo qualsiasi link che porti alla pagina dell'artista
                    val artistHref = doc.selectFirst("a[href^='/artist/']")?.attr("href")
                    if (artistHref != null) {
                        // Esempio href: /artist/blessed-wealthy-bol/1728227002
                        val segments = artistHref.split("/")
                        if (segments.size >= 3) {
                            artist = formatSlug(segments[2])
                        }
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
 * Funzione di supporto per trasformare "i-want-to-quit-my-job" in "I Want To Quit My Job"
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