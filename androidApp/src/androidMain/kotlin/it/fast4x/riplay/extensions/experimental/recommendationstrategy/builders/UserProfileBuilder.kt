package it.fast4x.riplay.extensions.experimental.recommendationstrategy.builders

import android.util.Log
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ArtistAffinity
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
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

        // === Fase 1: aggregazione raw ===
        val artistRaw = mutableMapOf<String, ArtistAccumulator>()
        val keywordRaw = mutableMapOf<String, Float>()
        val eraRaw = mutableMapOf<Int, Float>()
        val bookmarkedArtists = mutableSetOf<String>()
        val bookmarkedAlbums = mutableSetOf<String>()

        // Cache locale per evitare query ripetute
        val songCache = mutableMapOf<String, Song?>()
        val artistsBySongCache = mutableMapOf<String, List<Artist>>()
        val albumBySongCache = mutableMapOf<String, Album?>()

        // Prima passata: raccogliamo le songId distinte per pre-caricare gli artisti
        val distinctSongIds = events.map { it.songId }.distinct()
        preloadSongArtists(distinctSongIds, artistsBySongCache)

        //log
        var eventsProcessedCount = 0
        var songLookupFailures = 0
        var podcastCount = 0
        var songsWithGenres = 0
        var songsWithArtistsText = 0
        var eventsWithWeight = 0
        val sampleSongs = mutableMapOf<String, Song>()  // primi 3

        for (event in events) {

            //log
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

            val artists = artistsBySongCache[event.songId].orEmpty()
            val album = albumBySongCache.getOrPut(event.songId) {
                // Quando ci sarà Song.albumId: albumDao.getById(song.albumId)
                // Per ora: null
                null
            }

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

                // IMPORTANTE: salta eventi con peso zero (non portano segnale)
                if (eventWeight <= 0f) continue

                val artists = artistsBySongCache[event.songId].orEmpty()
                val album = albumBySongCache.getOrPut(event.songId) { null }

                if (artists.isNotEmpty()) {
                    // === PATH PRIMARIO (con SongArtistCrossRef — futuro Step 4) ===
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

                        for (kw in artist.keywords) {
                            val key = kw.lowercase()
                            keywordRaw[key] = (keywordRaw[key] ?: 0f) + eventWeight
                        }

                        artist.beginYear?.let { year ->
                            val decade = (year / 10) * 10
                            eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                        }
                    }

                    // Era fallback su album se gli artisti non hanno beginYear
                    if (artists.all { it.beginYear == null }) {
                        album?.originalYear?.let { year ->
                            val decade = (year / 10) * 10
                            eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                        }
                    }
                } else {
                    // === PATH FALLBACK (ora, senza SongArtistCrossRef) ===

                    // 1. Accumula generi direttamente da Song.genres
                    for (genre in song.genres.orEmpty()) {
                        val key = genre.lowercase().trim()
                        if (key.isNotEmpty()) {
                            keywordRaw[key] = (keywordRaw[key] ?: 0f) + eventWeight
                        }
                    }

                    // 2. Crea un "artista virtuale" basato su artistsText
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

                    // 3. Era: senza artist.beginYear o album.originalYear, resta vuoto
                    //    Si popolerà quando avremo Song.albumId o SongArtistCrossRef
                }

                // Bookmark album (comune a entrambi i path)
                album?.bookmarkedAt?.let {
                    bookmarkedAlbums.add(album.id)
                }
            }

            // Era: fallback su originalYear dell'album se l'artista non ha beginYear
            if (artists.isEmpty() || artists.all { it.beginYear == null }) {
                album?.originalYear?.let { year ->
                    val decade = (year / 10) * 10
                    eraRaw[decade] = (eraRaw[decade] ?: 0f) + eventWeight
                }
            }

            // Bookmark album
            album?.bookmarkedAt?.let {
                if (it != null) bookmarkedAlbums.add(album.id)
            }
        }

        // === LOG DIAGNOSTICO TEMPORANEO ===
        Timber.tag("REC_DEBUG").d("=== BUILDER STATS ===")
        Timber.tag("REC_DEBUG").d("Events fetched: ${events.size}")
        Timber.tag("REC_DEBUG").d("Events processed (passed podcast filter): $eventsProcessedCount")
        Timber.tag("REC_DEBUG").d("Song lookup failures (getById null): $songLookupFailures")
        Timber.tag("REC_DEBUG").d("Songs with isPodcast=1: $podcastCount")
        Timber.tag("REC_DEBUG").d("Songs with non-empty genres: $songsWithGenres")
        Timber.tag("REC_DEBUG").d("Songs with non-empty artistsText: $songsWithArtistsText")
        Timber.tag("REC_DEBUG").d("Events with eventWeight > 0: $eventsWithWeight")
        Timber.tag("REC_DEBUG").d("Keyword raw size: ${keywordRaw.size}")
        Timber.tag("REC_DEBUG").d("Artist raw size: ${artistRaw.size}")
        Timber.tag("REC_DEBUG").d("Era raw size: ${eraRaw.size}")

        // Sample delle prime 3 song processate
        Log.d("REC_DEBUG", "=== SAMPLE SONGS ===")
        sampleSongs.forEach { (id, song) ->
            Log.d("REC_DEBUG", "  id=$id, title='${song.title}', artistsText='${song.artistsText}', genres=${song.genres}, isPodcast=${song.isPodcast}, durationText='${song.durationText}'")
        }

        // === Fase 2: TF-IDF sulle keyword ===
        val totalArtists = (Database.countArtists() ?: 0).toFloat().coerceAtLeast(1f)
        val keywordIdf = mutableMapOf<String, Float>()
        for (kw in keywordRaw.keys) {
            val artistsWithKeyword = Database.countArtistsByKeyword(kw) ?: 0
            // IDF = log(N / (df + 1))  — aggiungiamo 1 per evitare divisione per zero
            keywordIdf[kw] = ln(totalArtists / (artistsWithKeyword + 1f)).coerceAtLeast(0f)
        }

        val keywordVector = keywordRaw
            .mapValues { (kw, tf) -> tf * (keywordIdf[kw] ?: 1f) }
            .normalize()

        // === Fase 3: normalizza score artisti su 0..1 ===
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

        // === Fase 4: normalizza era su 0..1 ===
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

    private suspend fun preloadSongArtists(
        songIds: List<String>,
        cache: MutableMap<String, List<Artist>>
    ) {
        // TODO Step 4: quando avrai SongArtistCrossRef, query batch:
        // val refs = songArtistDao.getArtistsForSongs(songIds)
        // val artistIds = refs.map { it.artistId }.distinct()
        // val artists = artistDao.getByIds(artistIds).associateBy { it.id }
        // for (songId in songIds) {
        //     cache[songId] = refs.filter { it.songId == songId }.mapNotNull { artists[it.artistId] }
        // }
        // Per ora: cache vuota, le strategie funzioneranno ma il profilo artista sarà limitato
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

