package it.fast4x.riplay.extensions.experimental.recommendationstrategy.builders

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ArtistAffinity
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.ln
import kotlin.math.sqrt

class UserProfileBuilder() {

    /**
     * Costruisce il profilo utente processando gli eventi da [since] in poi.
     * Passa since = 0 per una ricostruzione completa (batch notturno).
     * Passa since = lastRefresh per aggiornamento incrementale.
     */
    suspend fun build(
        userId: String,
        since: Long = 0L
    ): UserProfile = withContext(Dispatchers.IO) {

        val now = System.currentTimeMillis()
        val events = if (since > 0L) {
            Database.getEventsSince(since)
        } else {
            Database.getAllEvents()
        }

        if (events.isEmpty()) {
            return@withContext emptyProfile(userId, now)
        }

        val artistRaw = mutableMapOf<String, ArtistAccumulator>()
        val keywordRaw = mutableMapOf<String, Float>()
        val eraRaw = mutableMapOf<Int, Float>()
        val bookmarkedArtists = mutableSetOf<String>()
        val bookmarkedAlbums = mutableSetOf<String>()

        val songCache = mutableMapOf<String, Song?>()
        val artistsBySongCache = mutableMapOf<String, List<Artist>>()
        val albumBySongCache = mutableMapOf<String, Album?>()

        // Preload massivo: per ogni songId distinto, fetch artisti via cross-ref
        val distinctSongIds = events.map { it.songId }.distinct()
        preloadArtistsForSongs(distinctSongIds, artistsBySongCache)
        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG")
                .d("Preloaded artists for ${artistsBySongCache.size}/${distinctSongIds.size} songs")

        var eventsProcessedCount = 0
        var songLookupFailures = 0
        var podcastCount = 0
        var songsWithGenres = 0
        var songsWithArtistsText = 0
        var songsWithRealArtists = 0
        var eventsWithWeight = 0
        val sampleSongs = mutableMapOf<String, Song>()

        for (event in events) {
            val song = songCache.getOrPut(event.songId) { Database.getById(event.songId) }
            if (song == null) {
                songLookupFailures++
                continue
            }

            if (song.isPodcast == 1) {
                podcastCount++
                continue
            }
            eventsProcessedCount++

            if (!song.genres.isNullOrEmpty()) songsWithGenres++
            if (!song.artistsText.isNullOrBlank()) songsWithArtistsText++

            if (sampleSongs.size < 3) sampleSongs[event.songId] = song

            val eventWeight = computeEventWeight(song, event)
            if (eventWeight > 0f) eventsWithWeight++
            if (eventWeight <= 0f) continue

            val artists = artistsBySongCache[event.songId].orEmpty()
            val album = albumBySongCache.getOrPut(event.songId) {
                // Path primario: Song.albumId
                song.albumId?.let { Database.album(it).first() }
            }

            if (artists.isNotEmpty()) {
                // === PATH PRIMARIO: artisti reali via cross-ref ===
                songsWithRealArtists++

                for (artist in artists) {
                    val acc = artistRaw.getOrPut(artist.id) { ArtistAccumulator(artist.id) }
                    acc.playCount += 1
                    acc.totalPlayTimeMs += event.playTime
                    acc.lastPlayedAt = maxOf(acc.lastPlayedAt, event.timestamp)
                    acc.score += eventWeight

                    if (song.isLiked) acc.likedSongs += 1
                    if (song.isDisliked) acc.dislikedSongs += 1
                    if (artist.bookmarkedAt != null) {
                        acc.bookmarked = true
                        bookmarkedArtists.add(artist.id)
                    }

                    // Keywords da Artist.keywords (genere + tag fusi)
                    for (kw in artist.keywords) {
                        val key = kw.lowercase()
                        keywordRaw[key] = (keywordRaw[key] ?: 0f) + eventWeight
                    }

                    // Era da Artist.beginYear
                    artist.beginYear?.let { year ->
                        val decade = (year / 10) * 10
                        eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                    }
                }

                // Fallback era su album.originalYear se artisti non hanno beginYear
                if (artists.all { it.beginYear == null }) {
                    album?.originalYear?.let { year ->
                        val decade = (year / 10) * 10
                        eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                    }
                }
            } else {
                // === PATH FALLBACK: Song.genres + virtual artist ===
                for (genre in song.genres.orEmpty()) {
                    val key = genre.lowercase().trim()
                    if (key.isNotEmpty()) {
                        keywordRaw[key] = (keywordRaw[key] ?: 0f) + eventWeight
                    }
                }

                val artistName = song.artistsText?.trim()
                if (!artistName.isNullOrEmpty()) {
                    val virtualArtistId = "virtual::$artistName"
                    val acc = artistRaw.getOrPut(virtualArtistId) { ArtistAccumulator(virtualArtistId) }
                    acc.playCount += 1
                    acc.totalPlayTimeMs += event.playTime
                    acc.lastPlayedAt = maxOf(acc.lastPlayedAt, event.timestamp)
                    acc.score += eventWeight
                    if (song.isLiked) acc.likedSongs += 1
                    if (song.isDisliked) acc.dislikedSongs += 1
                }

                // Era fallback su album
                album?.originalYear?.let { year ->
                    val decade = (year / 10) * 10
                    eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                }
            }

            album?.bookmarkedAt?.let {
                bookmarkedAlbums.add(album.id)
            }
        }

        if (BuildConfig.DEBUG) {
            // === LOG DIAGNOSTICO ===
            Timber.tag("REC_DEBUG").d("=== BUILDER STATS ===")
            Timber.tag("REC_DEBUG").d("Events fetched: ${events.size}")
            Timber.tag("REC_DEBUG").d("Events processed: $eventsProcessedCount")
            Timber.tag("REC_DEBUG").d("Song lookup failures: $songLookupFailures")
            Timber.tag("REC_DEBUG").d("Podcast count: $podcastCount")
            Timber.tag("REC_DEBUG").d("Songs with real artists (cross-ref): $songsWithRealArtists")
            Timber.tag("REC_DEBUG").d("Songs with genres: $songsWithGenres")
            Timber.tag("REC_DEBUG").d("Events with weight > 0: $eventsWithWeight")
            Timber.tag("REC_DEBUG").d("Keyword raw size: ${keywordRaw.size}")
            Timber.tag("REC_DEBUG").d("Artist raw size: ${artistRaw.size}")
            Timber.tag("REC_DEBUG").d("Era raw size: ${eraRaw.size}")
        }

        // === Fase 2: TF-IDF ===
        val totalArtists = (Database.countArtists() ?: 0).toFloat().coerceAtLeast(1f)
        val keywordIdf = mutableMapOf<String, Float>()
        for (kw in keywordRaw.keys) {
            val artistsWithKeyword = Database.countArtistsByKeyword(kw) ?: 0
            keywordIdf[kw] = ln(totalArtists / (artistsWithKeyword + 1f)).coerceAtLeast(0f)
        }

        val keywordVector = keywordRaw
            .mapValues { (kw, tf) -> tf * (keywordIdf[kw] ?: 1f) }
            .normalize()

        // === Fase 3: top artists normalizzati ===
        val maxArtistScore = artistRaw.values.maxOfOrNull { it.score }?.coerceAtLeast(1e-6f) ?: 1f
        val topArtists = artistRaw.values
            .map { acc ->
                ArtistAffinity(
                    artistId = acc.artistId,
                    score = (acc.score / maxArtistScore).coerceIn(0f, 1f),
                    playCount = acc.playCount
                )
            }
            .sortedByDescending { it.score }
            .take(50)

        // === Fase 4: era normalizzata ===
        val maxEraWeight = eraRaw.values.maxOrNull()?.coerceAtLeast(1e-6f) ?: 1f
        val eraVector = eraRaw.mapValues { (_, w) -> (w / maxEraWeight).coerceIn(0f, 1f) }

        UserProfile(
            userId = userId,
            topArtists = topArtists,
            keywordVector = keywordVector,
            eraVector = eraVector,
            bookmarkedArtistIds = bookmarkedArtists,
            bookmarkedAlbumIds = bookmarkedAlbums,
            lastRefreshedAt = now
        )
    }

    /**
     * Preload batch: per ogni songId, fetch artisti via cross-ref.
     * Una sola query per TUTTI i songId, poi grouping in memory.
     */
    private suspend fun preloadArtistsForSongs(
        songIds: List<String>,
        cache: MutableMap<String, List<Artist>>
    ) {
        if (songIds.isEmpty()) return

        // Chunk per evitare SQL too long
        val chunkSize = 500
        songIds.chunked(chunkSize).forEach { chunk ->
            val artists = Database.songArtistCrossRefDao().getArtistsForSongs(chunk)
            // Per ogni artista, dobbiamo sapere a quale songId appartiene
            // Serve una query che ritorni (songId, artist) pairs
            val pairs = Database.songArtistCrossRefDao().getArtistsWithSongId(chunk)
            for ((songId, artist) in pairs) {
                val list = cache.getOrPut(songId) { mutableListOf() }
                (list as MutableList).add(artist)
            }
        }
    }

    private fun computeEventWeight(song: Song, event: Event): Float {
        val songDurationMs = durationTextToMillis(song.durationText ?: "").toFloat().coerceAtLeast(1f)
        val completionRatio = (event.playTime.toFloat() / songDurationMs).coerceIn(0f, 1f)

        // Peso base: l'utente ha interagito con il brano (aperto/avviato)
        // Anche se playTime = 0, c'è un segnale di interesse
        val baseWeight = 0.1f

        // Bonus per ascolto effettivo
        val playBonus = completionRatio * 0.7f

        // Bonus/malus per like/dislike
        val likeBonus = if (song.isLiked) 0.5f else 0f
        val dislikeMalus = if (song.isDisliked) 0.5f else 0f  // sottratto sotto

        return (baseWeight + playBonus + likeBonus - dislikeMalus).coerceAtLeast(0f)
    }

    private fun emptyProfile(
        userId: String,
        now: Long
    ) = UserProfile(
        userId = userId,
        topArtists = emptyList(),
        keywordVector = emptyMap(),
        eraVector = emptyMap(),
        bookmarkedArtistIds = emptySet(),
        bookmarkedAlbumIds = emptySet(),
        lastRefreshedAt = now
    )

    private data class ArtistAccumulator(
        val artistId: String,
        var playCount: Int = 0,
        var totalPlayTimeMs: Long = 0L,
        var lastPlayedAt: Long = 0L,
        var likedSongs: Int = 0,
        var dislikedSongs: Int = 0,
        var bookmarked: Boolean = false,
        var score: Float = 0f
    )
}

// === Extensions ===

private fun <K> Map<K, Float>.normalize(): Map<K, Float> {
    val norm = sqrt(values.sumOf { it * it.toDouble() }).toFloat().coerceAtLeast(1e-6f)
    return mapValues { it.value / norm }
}

