package it.fast4x.simpmusiclyrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Wrapper ───────────────────────────────────────────────────────────────────

@Serializable
data class LyricsResponse(
    @SerialName("type")    val type: String,
    @SerialName("data")    val data: List<LyricsData>,
    @SerialName("success") val success: Boolean,
)

// ── Payload ───────────────────────────────────────────────────────────────────

@Serializable
data class LyricsData(
    /** Identificativo univoco nel database SimpMusic Lyrics */
    @SerialName("id")               val id: String,

    /** YouTube video ID usato come chiave di ricerca */
    @SerialName("videoId")          val videoId: String,

    @SerialName("songTitle")        val songTitle: String,
    @SerialName("artistName")       val artistName: String,
    @SerialName("albumName")        val albumName: String,
    @SerialName("durationSeconds")  val durationSeconds: Int,

    /** Testo non sincronizzato (plain text, newline-delimited) */
    @SerialName("plainLyric")       val plainLyric: String,

    /**
     * Testo sincronizzato per riga in formato LRC standard.
     * Ogni riga: `[mm:ss.xx] testo`
     *
     * Esempio: `[00:05.61] Hello darkness, my old friend`
     */
    @SerialName("syncedLyrics")     val syncedLyrics: String,

    /**
     * Testo sincronizzato **parola per parola** (Rich Sync / Karaoke).
     *
     * Formato: ogni riga ha un timestamp di riga `[mm:ss.xx]` seguito da
     * coppie `<mm:ss.xx>parola` per ogni singola parola.
     *
     * Esempio:
     * `[00:06.00] <00:06.00>Hello <00:06.64>darkness, <00:07.17>my <00:07.36>old <00:07.83>friend`
     *
     * Nota: le entità HTML (es. `&#x27;` → `'`, `&quot;` → `"`) vanno
     * decodificate prima di mostrare il testo all'utente.
     */
    @SerialName("richSyncLyrics")   val richSyncLyrics: String,

    /** Tipo di traccia, es. "SONG" */
    @SerialName("trackType")        val trackType: TrackType?,

    /** Voto aggregato della community (-1 / 0 / 1) */
    @SerialName("vote")             val vote: Int,

    @SerialName("contributor")      val contributor: String,
    @SerialName("contributorEmail") val contributorEmail: String,
)

// ── Enums ─────────────────────────────────────────────────────────────────────

@Serializable
enum class TrackType {
    @SerialName("SONG")    SONG,
    @SerialName("VIDEO")   VIDEO,
    @SerialName("PODCAST") PODCAST,
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Rappresenta una singola riga del richSyncLyrics già parsata.
 *
 * @param lineStartMs  timestamp di inizio riga in millisecondi
 * @param words        lista ordinata di parole con il proprio timestamp
 */
data class RichSyncLine(
    val lineStartMs: Long,
    val words: List<RichSyncWord>,
)

/**
 * Singola parola con il proprio timestamp di inizio.
 *
 * @param startMs  timestamp di inizio parola in millisecondi
 * @param text     testo della parola (già con entità HTML decodificate)
 */
data class RichSyncWord(
    val startMs: Long,
    val text: String,
)

/**
 * Parsa il campo [LyricsData.richSyncLyrics] in una lista di [RichSyncLine].
 *
 * Gestisce automaticamente la decodifica delle entità HTML più comuni
 * (`&#x27;` → `'`, `&quot;` → `"`, `&amp;` → `&`, `&lt;` → `<`, `&gt;` → `>`).
 *
 * Formato atteso per ogni riga:
 * `[mm:ss.xx] <mm:ss.xx>word1 <mm:ss.xx>word2 ...`
 */
fun LyricsData.parseRichSync(): List<RichSyncLine> {
    // Regex per il timestamp di riga:  [mm:ss.xx]
    val lineTimestampRegex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2})]""")
    // Regex per ogni coppia timestamp+parola:  <mm:ss.xx>testo
    val wordRegex = Regex("""<(\d{2}):(\d{2})\.(\d{2})>([^<\n]*)""")

    fun timestampToMs(min: String, sec: String, centisec: String): Long =
        min.toLong() * 60_000L +
                sec.toLong() * 1_000L +
                centisec.toLong() * 10L

    fun decodeHtmlEntities(text: String): String =
        text.replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&amp;",  "&")
            .replace("&lt;",   "<")
            .replace("&gt;",   ">")
            .trim()

    return richSyncLyrics
        .lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val lineMatch = lineTimestampRegex.find(line) ?: return@mapNotNull null
            val (min, sec, cs) = lineMatch.destructured
            val lineStartMs = timestampToMs(min, sec, cs)

            val words = wordRegex.findAll(line).map { m ->
                val (wMin, wSec, wCs, rawText) = m.destructured
                RichSyncWord(
                    startMs = timestampToMs(wMin, wSec, wCs),
                    text    = decodeHtmlEntities(rawText),
                )
            }.filter { it.text.isNotEmpty() }.toList()

            if (words.isEmpty()) null
            else RichSyncLine(lineStartMs = lineStartMs, words = words)
        }
}

@JvmInline
value class Lyrics(val text: String) {

    val sentences: List<Pair<Long, String>>
        get() = mutableListOf(0L to "").apply {
            for (line in text.trim().lines()) {
                try {
                    val position = line.take(10).run {
                        get(8).digitToInt() * 10L +
                                get(7).digitToInt() * 100 +
                                get(5).digitToInt() * 1000 +
                                get(4).digitToInt() * 10000 +
                                get(2).digitToInt() * 60 * 1000 +
                                get(1).digitToInt() * 600 * 1000
                    }

                    add(position to line.substring(10))
                } catch (_: Throwable) {
                }
            }
        }

}