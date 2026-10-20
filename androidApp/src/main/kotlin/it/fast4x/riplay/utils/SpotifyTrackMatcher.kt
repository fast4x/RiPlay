package it.fast4x.riplay.utils

import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.spotifymeta.SpotifyTrackMeta
import java.text.Normalizer

/**
 * Confronto tra una traccia di Spotify e i risultati della ricerca su YouTube Music.
 *
 * Spotify e YouTube Music non usano gli stessi titoli ("Esta Cobardía - Remasterizado" vs
 * "Esta Cobardía (Remasterizado)"), quindi il confronto ignora i marcatori di versione e usa
 * la durata per sciogliere le ambiguità.
 */
object SpotifyTrackMatcher {

    /** parole che indicano una versione specifica del brano */
    private val versionWords = listOf(
        "remaster", "remastered", "remasterizado", "remasterizato", "remasterizacion",
        "live", "acoustic", "instrumental", "version", "edit", "remix", "radio", "demo",
        "mono", "stereo", "deluxe", "bonus", "reissue", "expanded", "anniversary",
        "reprise", "unplugged", "cover", "original", "mix", "extended", "club", "dub"
    )

    /** parole da ignorare nel confronto degli artisti */
    private val artistNoise = setOf(
        "feat", "ft", "featuring", "with", "topic", "vevo", "official", "lyrics", "lyric",
        "audio", "video", "music", "records", "channel", "mv"
    )

    private val yearRegex = Regex("""\b(19|20)\d{2}\b""")
    private val parensRegex = Regex("""\([^)]*\)|\[[^]]*\]""")
    private val nonAlphanumericRegex = Regex("[^a-z0-9]+")
    private val spacesRegex = Regex("""\s+""")

    /** normalizza un testo: minuscole, senza accenti, solo lettere e numeri */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("""\p{InCombiningDiacriticalMarks}+""".toRegex(), "")
        return nonAlphanumericRegex.replace(withoutAccents.lowercase(), " ")
            .replace(spacesRegex, " ")
            .trim()
    }

    /** titolo "nudo": senza parentesi, senza anni e senza marcatori di versione */
    fun coreTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        var text = parensRegex.replace(title, " ")
        text = yearRegex.replace(text, " ")
        text = text.replace("""[-–—:]""".toRegex(), " ")
        text = normalize(text)
        versionWords.forEach { word ->
            text = text.replace("""\b${Regex.escape(word)}\b""".toRegex(), " ")
        }
        return text.replace(spacesRegex, " ").trim()
    }

    private fun tokens(text: String): Set<String> =
        text.split(" ").filter { it.isNotBlank() }.toSet()

    /**
     * Quanta parte del primo insieme di parole e' contenuta nel secondo (0..1).
     * Se il primo e' vuoto vale 0.
     */
    private fun containment(first: Set<String>, second: Set<String>): Double {
        if (first.isEmpty()) return 0.0
        return first.count { it in second }.toDouble() / first.size
    }

    /** somiglianza tra due titoli, fra 0 e 1 */
    fun titleScore(spotifyTitle: String?, candidateTitle: String?): Double {
        val spotify = coreTitle(spotifyTitle)
        val candidate = coreTitle(candidateTitle)
        if (spotify.isEmpty() || candidate.isEmpty()) return 0.0
        if (spotify == candidate) return 1.0
        val spotifyTokens = tokens(spotify)
        val candidateTokens = tokens(candidate)
        return maxOf(
            containment(spotifyTokens, candidateTokens),
            containment(candidateTokens, spotifyTokens)
        )
    }

    /** somiglianza "grezza" tra due titoli, senza rimuovere i marcatori di versione */
    fun rawTitleScore(spotifyTitle: String?, candidateTitle: String?): Double {
        val spotify = normalize(spotifyTitle)
        val candidate = normalize(candidateTitle)
        if (spotify.isEmpty() || candidate.isEmpty()) return 0.0
        if (spotify == candidate) return 1.0
        val spotifyTokens = tokens(spotify)
        val candidateTokens = tokens(candidate)
        return maxOf(
            containment(spotifyTokens, candidateTokens),
            containment(candidateTokens, spotifyTokens)
        )
    }

    /** somiglianza fra il nome dell'artista di Spotify e quelli del risultato, fra 0 e 1 */
    fun artistScore(spotifyArtist: String?, candidateArtists: List<String>): Double {
        val spotifyTokens = tokens(normalize(spotifyArtist)).filterNot { it in artistNoise }.toSet()
        if (spotifyTokens.isEmpty()) return 0.0

        val candidateTokens = candidateArtists
            .flatMap { tokens(normalize(it)).toList() }
            .filterNot { it in artistNoise }
            .toSet()

        if (candidateTokens.isEmpty()) return 0.0
        return containment(spotifyTokens, candidateTokens)
    }

    /** score della durata: 1 se quasi identica, 0 se troppo diversa */
    fun durationScore(spotifyDurationMs: Long?, candidateDurationMs: Long?): Double {
        if (spotifyDurationMs == null || spotifyDurationMs <= 0) return 0.5
        if (candidateDurationMs == null || candidateDurationMs <= 0) return 0.5
        val diff = kotlin.math.abs(spotifyDurationMs - candidateDurationMs)
        return when {
            diff <= 3_000 -> 1.0
            diff <= 8_000 -> 0.7
            diff <= 20_000 -> 0.3
            else -> 0.0
        }
    }

    /** un risultato e' accettato solo se titolo, artista e durata sono compatibili */
    fun isAcceptable(
        title: Double,
        artist: Double,
        duration: Double
    ) = title >= 0.85 && artist >= 0.5 && duration >= 0.3

    /** score complessivo, usato per scegliere il risultato migliore */
    fun overallScore(title: Double, artist: Double, duration: Double, rawTitle: Double) =
        title * 3 + artist * 2 + duration * 2 + rawTitle
}

/** una traccia di Spotify con il corrispondente brano su YouTube Music */
data class MatchedTrack(
    val spotify: SpotifyTrackMeta,
    val candidate: Environment.SongItem,
    val score: Double
)

/**
 * Cerca su YouTube Music la traccia di Spotify che corrisponde.
 * @return null se non e' stato trovato un brano abbastanza simile.
 */
suspend fun findMatchingSong(spotifyTrack: SpotifyTrackMeta): MatchedTrack? {
    val title = spotifyTrack.name?.takeIf { it.isNotBlank() } ?: return null
    val artist = spotifyTrack.artist.orEmpty()

    val query = listOf(title, artist)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .trim()

    val results = Environment.searchPage(
        body = SearchBody(
            query = query,
            params = Environment.SearchFilter.Song.value
        ),
        fromMusicShelfRendererContent = Environment.SongItem.Companion::fromMusicShelfRenderer
    )?.getOrNull()?.items.orEmpty()

    if (results.isEmpty()) return null

    val spotifyDuration = spotifyTrack.durationMs

    val scored = results.asSequence()
        .filter { it.key.isNotBlank() }
        .map { candidate ->
            val candidateArtists = candidate.authors?.mapNotNull { it.name }.orEmpty()
            val titleScore = SpotifyTrackMatcher.titleScore(spotifyTrack.name, candidate.info?.name)
            val rawTitleScore = SpotifyTrackMatcher.rawTitleScore(spotifyTrack.name, candidate.info?.name)
            val artistScore = SpotifyTrackMatcher.artistScore(spotifyTrack.artist, candidateArtists)
            val durationScore = SpotifyTrackMatcher.durationScore(
                spotifyDuration,
                candidate.durationText?.let { durationTextToMillis(it) }
            )

            val acceptable = SpotifyTrackMatcher.isAcceptable(titleScore, artistScore, durationScore)

            MatchedTrack(
                spotify = spotifyTrack,
                candidate = candidate,
                score = SpotifyTrackMatcher.overallScore(titleScore, artistScore, durationScore, rawTitleScore)
            ) to acceptable
        }
        .filter { it.second }
        .map { it.first }
        .toList()

    return scored.maxByOrNull { it.score }
}
