package it.fast4x.riplay.player.lyrics

import it.fast4x.lrclib.utils.runCatchingCancellable
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Lyrics
import it.fast4x.riplay.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** una riga di testo sincronizzato: istante di inizio (ms) + contenuto */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/** stato del testo per un brano (osservato dalla UI del player full) */
sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data object NotFound : LyricsUiState

    /** testo senza timecode (plain lyrics) */
    data class Fixed(val text: String) : LyricsUiState

    /** testo sincronizzato: righe ordinate per istante di inizio */
    data class Synced(val lines: List<LyricLine>) : LyricsUiState
}

object DesktopLyricsRepository {

    /** stato corrente per songId (la UI lo osserva con collectAsState) */
    private val states = ConcurrentHashMap<String, MutableStateFlow<LyricsUiState>>()

    fun stateFor(songId: String) =
        states.getOrPut(songId) { MutableStateFlow(LyricsUiState.Idle) }

    /**
     * Carica il testo del brano: cache db -> LRCLIB -> KuGou.
     * [force] riscarica anche se in cache (pulsante "ricarica" del pannello).
     */
    suspend fun load(song: Song, force: Boolean = false): LyricsUiState {
        val flow = stateFor(song.id)
        if (!force) {
            val cached = flow.value
            if (cached != LyricsUiState.Idle && cached != LyricsUiState.NotFound) return cached
        }
        flow.value = LyricsUiState.Loading

        val result = withContext(Dispatchers.IO) { resolve(song) }
        flow.value = result
        return result
    }

    private suspend fun resolve(song: Song): LyricsUiState {
        // 1) CACHE LOCALE: tabella Lyrics (songId, fixed, synced)
        runCatchingCancellable {
            Database.lyrics(song.id).first()
        }?.getOrNull()?.let { cached ->
            val fromDb = parseLyricsModel(cached.fixed, cached.synced)
            if (fromDb != null) return fromDb
        }

        val artist = primaryArtist(song.artistsText)
        val title = cleanTitle(song.title)
        if (artist.isBlank() || title.isBlank())
            return LyricsUiState.NotFound

        // 2) LRCLIB: ricerca completa (restituisce tutti i brani trovati con
        //    plain e synced); si sceglie il migliore: sincronizzato > fisso
        val lrclib = runCatchingCancellable {
            LrcLib.lyrics(artist = artist, title = title)
        }?.getOrNull()?.getOrNull()
        if (lrclib != null && lrclib.isNotEmpty()) {
            val track = lrclib.firstOrNull { it.syncedLyrics != null }
                ?: lrclib.firstOrNull { it.plainLyrics != null }
            if (track != null) {
                val state = parseLyricsModel(track.plainLyrics, track.syncedLyrics)
                if (state != null) {
                    save(song, track.plainLyrics, track.syncedLyrics)
                    return state
                }
            }
        }

        // 3) KUGOU: fallback (ricerca keyword + durata; il modulo restituisce
        //    gia' LRC normalizzato)
        val durationSec = song.durationText?.toSecondsOrNull() ?: 0L
        val kugou = runCatchingCancellable {
            KuGou.lyrics(artist = artist, title = title, duration = durationSec)
        }?.getOrNull()?.getOrNull()?.value
        if (!kugou.isNullOrBlank()) {
            val state = parseLyricsModel(fixed = null, synced = kugou)
            if (state != null) {
                save(song, fixed = null, synced = kugou)
                return state
            }
        }

        return LyricsUiState.NotFound
    }

    private suspend fun save(song: Song, fixed: String?, synced: String?) {
        if (fixed == null && synced == null) return
        runCatching {
            Database.upsert(Lyrics(songId = song.id, fixed = fixed, synced = synced))
        }.onFailure {
            println("[RiPlay-Lyrics] cache non salvata per '${song.id}': ${it.message}")
        }.onSuccess {
            println("[RiPlay-Lyrics] testo in cache per '${song.id}' (synced=${synced != null})")
        }
    }

    private fun parseLyricsModel(fixed: String?, synced: String?): LyricsUiState? {
        if (!synced.isNullOrBlank()) {
            val lines = parseSyncedLyrics(synced)
            if (lines.isNotEmpty()) return LyricsUiState.Synced(lines)
        }
        if (!fixed.isNullOrBlank()) return LyricsUiState.Fixed(fixed.trim())
        return null
    }

    /**
     * Primo artista di artistsText ("A, B, C" -> "A"): le API dei testi
     * funzionano meglio con UN solo artista principale.
     */
    internal fun primaryArtist(artistsText: String?): String =
        artistsText
            ?.split(",", "/", "&", "feat.", "ft.")
            ?.firstOrNull()
            ?.trim()
            ?: ""

    /**
     * Pulizia del titolo per la ricerca: il titolo YouTube porta spesso
     * suffissi tipo "(Official Video)", "[Lyrics]", "(Audio)" che rompono
     * il match sui cataloghi dei testi.
     */
    internal fun cleanTitle(title: String): String {
        var t = title
        // parentesi/quadre con parole tipiche dei video musicali
        t = t.replace(
            Regex("\\s*[\\(\\[][^\\)\\]]*?(official|video|audio|lyric|lyrics|hd|4k|hq|mv|m/v|visualizer|color coded|sub esp|sub eng|remaster)[^\\)\\]]*?[\\)\\]]", RegexOption.IGNORE_CASE),
            ""
        )
        t = t.replace(Regex("\\s*[\\(\\[][0-9]{4}[\\)\\]]"), "") // (2021)
        t = t.replace(Regex("\\s*-\\s*(official|lyric|audio|video).*$", RegexOption.IGNORE_CASE), "")
        return t.trim()
    }

    /** "3:45" / "63:12" / "1:03:45" -> secondi (per la ricerca KuGou) */
    private fun String.toSecondsOrNull(): Long? {
        val parts = trim().split(":")
        return when {
            parts.size == 2 ->
                (parts[0].toLongOrNull() ?: return null) * 60 +
                        (parts[1].toLongOrNull() ?: return null)
            parts.size == 3 ->
                (parts[0].toLongOrNull() ?: return null) * 3600 +
                        (parts[1].toLongOrNull() ?: return null) * 60 +
                        (parts[2].toLongOrNull() ?: return null)
            else -> null
        }
    }

    /**
     * Parser LRC robusto. Gestisce:
     *  - "[mm:ss.xx] testo" / "[mm:ss.xxx] testo" / "[mm:ss] testo"
     *  - tag multipli sulla stessa riga: "[00:12.00][01:45.30] ritornello"
     *  - virgola come separatore dei centesimi ("[00:12,50]")
     *  - tag globali [offset:+/-ms] (applicati a tutte le righe)
     *  - tag metadati [ar:..] [ti:..] [al:..] [by:..] [re:..] [ve:..] ignorati
     * Restituisce le righe ordinate per istante di inizio.
     */
    internal fun parseSyncedLyrics(raw: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        var offsetMs = 0L

        val tagRegex = Regex("\\[([^\\]]*)]")
        raw.trim().lines().forEach { sourceLine ->
            val tags = tagRegex.findAll(sourceLine).toList()
            if (tags.isEmpty()) return@forEach

            // testo dopo l'ultimo tag
            val text = sourceLine.substring(tags.last().range.last + 1).trim()

            for (tag in tags) {
                val content = tag.groupValues[1].trim()
                if (content.isEmpty()) continue

                // tag globale offset: "[offset:+500]" (ms)
                if (content.startsWith("offset:", ignoreCase = true)) {
                    offsetMs = content.substringAfter(':').trim().toLongOrNull() ?: 0L
                    continue
                }
                // tag metadati "ar:xxx": chiave tutta letterale, non e' un tempo
                val colonIndex = content.indexOf(':')
                if (colonIndex > 0 && content.substring(0, colonIndex).all { it.isLetter() }) continue

                // formato tempo "mm:ss.xx" | "mm:ss.xxx" | "mm:ss,xx" | "mm:ss"
                val timeParts = content.replace(',', '.').split(':')
                if (timeParts.size < 2 || timeParts.size > 3) continue
                val minutes = timeParts[0].trim().toLongOrNull() ?: continue
                val seconds = timeParts[1].trim().toDoubleOrNull() ?: continue
                val ms = minutes * 60_000L + (seconds * 1000L).toLong()
                out.add(LyricLine(timeMs = ms, text = text))
            }
        }

        // ordina e applica offset (il tag [offset] LRC: positivo = anticipa)
        return out.asSequence()
            .sortedBy { it.timeMs }
            .map { if (offsetMs != 0L) it.copy(timeMs = it.timeMs - offsetMs) else it }
            .toList()
    }
}
