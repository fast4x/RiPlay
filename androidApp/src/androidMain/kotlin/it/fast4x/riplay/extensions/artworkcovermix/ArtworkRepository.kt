package it.fast4x.riplay.extensions.artworkcovermix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import it.fast4x.riplay.utils.isLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import androidx.core.graphics.scale
import java.text.Normalizer

object ArtworkRepository {

    @Serializable
    private data class ITunesResponse(val results: List<ITunesResult> = emptyList())
    @Serializable
    private data class ITunesResult(
        val artworkUrl100: String? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val collectionName: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = LruCache<String, List<String>>(200)

    // sporco tipico dei titoli YouTube
    private val noise = Regex(
        """\s*[(\[][^)\]]*(official|video|lyrics?|audio|mv|hd|4k|remaster|visualizer)[^)\]]*[)\]]\s*""",
        RegexOption.IGNORE_CASE
    )
    private val feat = Regex("""\s*\b(?:ft|feat)\b.*$""", RegexOption.IGNORE_CASE)

    fun cleanTitle(raw: String) =
        raw.replace(noise, " ").replace(feat, " ").replace(Regex("""\s+"""), " ").trim()

    private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000; conn.readTimeout = 5_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)")
            val code = conn.responseCode
            if (code != 200) {
                Timber.w("null%s", if (code == 403) " (rate limit? 20 req/min)" else "")
                return@withContext null
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        }.onFailure { Timber.w("Artwork fetch failed: ${it.message}") }.getOrNull()
    }

    private suspend fun iTunesArtworks(term: String, expectedArtist: String?, cleanedTitle: String): List<String> {
        if (term.isBlank()) return emptyList()
        val country = Locale.getDefault().country.ifBlank { "US" }
        val body = httpGet(
            "https://itunes.apple.com/search?term=${
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(term, "UTF-8")
                }
            }" +
                    "&entity=song&limit=3&country=$country"
        ) ?: return emptyList()
        val results = runCatching { json.decodeFromString<ITunesResponse>(body).results }
            .getOrDefault(emptyList())

        results.forEachIndexed { i, r ->
            Timber.i("Artwork iTunes[$i] '${r.trackName}' — ${r.artistName} — ${r.collectionName}")
        }

        return results
            .filter { isPlausible(expectedArtist, cleanedTitle, it.artistName, it.trackName) }
            .mapNotNull { it.artworkUrl100 }
            .map { it.replace("100x100bb", "600x600bb") }
            .distinct()
    }

    // hqdefault è 4:3 con bande nere → mqdefault 16:9 è più adatto a fullscreen
    private fun toBackdrop(url: String) =
        url.replace("hqdefault", "mqdefault").replace("maxresdefault", "mqdefault")

    suspend fun artworksFor(
        context: Context,
        mediaId: String,
        rawTitle: String,
        artist: String?,
        fallbackThumb: String,
        forceRefresh: Boolean = false
    ): List<String> {
        if (!forceRefresh) cache.get(mediaId)?.let { return it }
        cache.get(mediaId)?.let { return it }

        val list = if (mediaId.isLocal) {
            listOf(fallbackThumb)
        } else {
            val title = cleanTitle(rawTitle)
            val remote = iTunesArtworks("$artist $title", artist, title)
                .ifEmpty { iTunesArtworks(title, artist, title) }   // retry senza artista
            (remote + toBackdrop(fallbackThumb)).distinct()
        }

        val deduped = dedupKeepFirst(context, list)
        cache.put(mediaId, deduped)
        return deduped

    }

    /** Bitmap piccola per Palette. allowHardware(false) è OBBLIGATORIO:
    su hardware bitmap Palette restituisce swatch vuoti. */
    suspend fun bitmapFor(context: Context, url: String, size: Int = 64): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(url).size(size, size).allowHardware(false).build()
                (context.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
            }.getOrNull()
        }


    private val stopwords = setOf("the", "and", "feat", "ft", "official", "video", "a", "an", "of", "di", "da", "del", "la", "le", "il", "un")

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")              // via gli accenti
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()

    /** quota di token attesi presenti in actual (ignorando stopwords e token corti) */
    private fun tokenOverlap(expected: String, actual: String): Float {
        val e = normalize(expected).split(" ")
            .filter { it.length > 2 && it !in stopwords }.toSet()
        val a = normalize(actual).split(" ").toSet()
        if (e.isEmpty()) return 1f   // niente da validare: non bloccare
        if (a.isEmpty()) return 0f
        return e.intersect(a).size.toFloat() / e.size
    }

    private fun isPlausible(
        expectedArtist: String?, cleanedTitle: String,
        resultArtist: String?, resultTitle: String?,
    ): Boolean {
        val artistScore = expectedArtist
            ?.takeIf { it.isNotBlank() }
            ?.let { tokenOverlap(it, resultArtist.orEmpty()) }
        val titleScore = tokenOverlap(cleanedTitle, resultTitle.orEmpty())

        val ok = when {
            artistScore == null -> titleScore >= 0.6f                 // nessun artista atteso: solo titolo
            else -> artistScore >= 0.4f ||                            // artista abbastanza coerente, oppure
                    (titleScore >= 0.7f && artistScore > 0f)          // titolo forte + artista non contraddetto
        }
        Timber.i(
            "Artwork validate '${resultArtist.orEmpty()} - ${resultTitle.orEmpty()}' " +
                    "artist=$artistScore title=$titleScore -> $ok"
        )
        return ok
    }

    data class ArtworkInfo(val dhash: Long, val dominant: Int, val vibrant: Int, val darkMuted: Int)

    private val infoCache = LruCache<String, ArtworkInfo>(256)

    private fun dhash(bitmap: Bitmap): Long {
        val w = 9; val h = 8
        val px = IntArray(w * h)
        bitmap.scale(w, h).getPixels(px, 0, w, 0, 0, w, h)
        val lum = IntArray(px.size) { i ->
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (299 * r + 587 * g + 114 * b) / 1000
        }
        var hash = 0L; var bit = 0
        for (y in 0 until h) for (x in 0 until w - 1) {
            if (lum[y * w + x] > lum[y * w + x + 1]) hash = hash or (1L shl bit)
            bit++
        }
        return hash
    }

    /** Un solo fetch per URL: dHash per il dedup + 3 colori per il gradiente. */
    suspend fun infoFor(context: Context, url: String): ArtworkInfo? {
        infoCache.get(url)?.let { return it }
        val bmp = bitmapFor(context, url, 64) ?: return null
        val info = withContext(Dispatchers.IO) {
            val p = Palette.from(bmp).generate()
            val def = Color.Gray.toArgb()
            ArtworkInfo(dhash(bmp), p.getDominantColor(def), p.getVibrantColor(def), p.getDarkMutedColor(def))
        }
        infoCache.put(url, info)
        return info
    }

    private suspend fun dedupKeepFirst(context: Context, urls: List<String>): List<String> {
        if (urls.size < 2) return urls
        val kept = mutableListOf<String>()
        for (url in urls) {
            val info = infoFor(context, url) ?: run { kept.add(url); continue }
            val duplicate = kept.any { k ->
                val ki = infoFor(context, k) ?: return@any false
                val dist = (ki.dhash xor info.dhash).countOneBits()
                Timber.i("Artwork dedup dist=$dist $url vs $k")
                dist <= 8
            }
            if (!duplicate) kept.add(url) else Timber.i("Artwork dedup DROPPED $url")
        }
        return kept
    }

}