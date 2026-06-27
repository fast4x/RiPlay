package it.fast4x.riplay.extensions.experimental.recommendationstrategy.service

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.dao.getSongsByGenres
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedAlbum
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedArtist
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedSong
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RelatedItemsService() {


    private val artistDao = Database.artistDao()
    private val albumDao = Database.albumDao()
    private val songDao = Database.songDao()
    private val songArtistRefDao = Database.songArtistCrossRefDao()
    private val artistRelationDao = Database.relationDao()
    private val mbAlbumDao = Database.mbAlbumDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preloadedRelated = MutableStateFlow<Map<String, List<RelatedSong>>>(emptyMap())
    val preloadedRelatedSongs: StateFlow<Map<String, List<RelatedSong>>> = preloadedRelated

    /**
     * Pre-carica i related per un brano in background.
     * Non blocca il chiamante. La UI può osservare preloadedRelatedSongs.
     */
    fun preloadRelated(songId: String) {
        scope.launch {
            if (preloadedRelated.value.containsKey(songId)) return@launch  // già caricato

            val related = getRelatedSongs(songId, limit = 10)
            preloadedRelated.value += (songId to related)
        }
    }

    /**
     * Recupera i related pre-caricati (istantaneo se già pronti).
     */
    suspend fun getRelatedSongsCached(songId: String): List<RelatedSong> {
        preloadedRelated.value[songId]?.let { return it }
        // Se non pre-caricati, fetch sincrono
        val related = getRelatedSongs(songId, limit = 10)
        preloadedRelated.value += (songId to related)
        return related
    }

    /**
     * Pulisci cache quando troppa grande.
     */
    fun clearPreloadedCache(keepSongId: String? = null) {
        if (keepSongId != null) {
            val keep = preloadedRelated.value[keepSongId]
            preloadedRelated.value = if (keep != null) mapOf(keepSongId to keep) else emptyMap()
        } else {
            preloadedRelated.value = emptyMap()
        }
    }


    // ========== ARTISTA ==========

    suspend fun getRelatedArtists(
        artistId: String,
        limit: Int = 10
    ): List<RelatedArtist> = withContext(Dispatchers.IO) {
        val sourceArtist = artistDao.getById(artistId) ?: return@withContext emptyList()

        val results = mutableListOf<RelatedArtist>()

        // 1. MB graph walk (1-hop)
        if (sourceArtist.mbId != null) {
            val relations = artistRelationDao.getBidirectional(sourceArtist.mbId)
            for (rel in relations) {
                val otherMbId = if (rel.fromArtistId == sourceArtist.mbId) rel.toArtistId else rel.fromArtistId
                val other = artistDao.getByMbId(otherMbId) ?: continue
                results.add(RelatedArtist(
                    artist = other,
                    score = 1.0f,
                    reason = rel.relationType,  // "member of band", "collaborator"
                    source = RelatedSource.MB_GRAPH
                ))
            }
        }

        // 2. Fallback: keyword similarity se MB graph è vuoto
        if (results.size < limit) {
            val sourceKeywords = sourceArtist.keywords.map { it.lowercase() }.toSet()
            if (sourceKeywords.isNotEmpty()) {
                val candidates = artistDao.getAllWithName()
                    .filter { it.id != artistId && it.id !in results.map { r -> r.artist.id } }

                for (candidate in candidates) {
                    val candidateKeywords = candidate.keywords.map { it.lowercase() }.toSet()
                    val intersection = sourceKeywords.intersect(candidateKeywords)
                    if (intersection.isEmpty()) continue

                    // Jaccard similarity
                    val union = sourceKeywords.union(candidateKeywords).size
                    val score = intersection.size.toFloat() / union
                    if (score > 0.2f) {
                        results.add(RelatedArtist(
                            artist = candidate,
                            score = score * 0.7f,  // peso più basso di MB graph
                            reason = "${intersection.size} generi in comune",
                            source = RelatedSource.KEYWORD_SIMILARITY
                        ))
                    }
                }
            }
        }

        results.sortedByDescending { it.score }.take(limit)
    }

    // ========== ALBUM ==========

    suspend fun getRelatedAlbums(
        albumId: String,
        limit: Int = 10
    ): List<RelatedAlbum> = withContext(Dispatchers.IO) {
        val sourceAlbum = albumDao.getById(albumId) ?: return@withContext emptyList()

        val results = mutableListOf<RelatedAlbum>()

        // 1. Altri album stesso artista (peso massimo)
        sourceAlbum.authorsText?.let { artistName ->
            val sameArtist = albumDao.getAlbumsByArtistName(artistName)
                .filter { it.id != albumId }
                .take(5)
            sameArtist.forEach { album ->
                results.add(RelatedAlbum(
                    album = album,
                    score = 1.0f,
                    reason = "Stesso artista",
                    source = RelatedSource.SAME_ARTIST
                ))
            }
        }

        // 2. Album stessa era + genere (cross-artist)
        val sourceKeywords = (sourceAlbum.genres.orEmpty() + sourceAlbum.tags.orEmpty())
            .map { it.lowercase() }.toSet()
        val sourceYear = sourceAlbum.originalYear

        if (sourceKeywords.isNotEmpty() && sourceYear != null && results.size < limit) {
            val eraAlbums = albumDao.getAlbumsByEraAndGenre(
                yearStart = sourceYear - 3,
                yearEnd = sourceYear + 3,
                limit = 50
            ).filter { it.id != albumId && it.id !in results.map { r -> r.album.id } }

            for (album in eraAlbums) {
                val albumKeywords = (album.genres.orEmpty() + album.tags.orEmpty())
                    .map { it.lowercase() }.toSet()
                val intersection = sourceKeywords.intersect(albumKeywords)
                if (intersection.isEmpty()) continue

                val score = (intersection.size.toFloat() / sourceKeywords.size) * 0.6f
                results.add(RelatedAlbum(
                    album = album,
                    score = score,
                    reason = "Stessa epoca, ${intersection.size} generi in comune",
                    source = RelatedSource.SAME_ERA_GENRE
                ))
            }
        }

        // 3. MBAlbum top per stessi generi
        if (results.size < limit && sourceKeywords.isNotEmpty()) {
            val mbCandidates = mbAlbumDao.getQualityAlbumsV2(limit = 50)
                .filter { "mb-${it.id}" != albumId }

            for (mb in mbCandidates) {
                val mbKeywords = (mb.genres.orEmpty() + mb.tags.orEmpty())
                    .map { it.lowercase() }.toSet()
                val intersection = sourceKeywords.intersect(mbKeywords)
                if (intersection.size < 2) continue

                val score = (intersection.size.toFloat() / sourceKeywords.size) * 0.5f
                results.add(RelatedAlbum(
                    album = mapMbToAlbum(mb),
                    score = score,
                    reason = "Capolavoro • ${intersection.take(2).joinToString(", ")}",
                    source = RelatedSource.MB_QUALITY
                ))
            }
        }

        results.sortedByDescending { it.score }.take(limit)
    }

    // ========== SONG ==========

    suspend fun getRelatedSongs(
        songId: String,
        limit: Int = 10
    ): List<RelatedSong> = withContext(Dispatchers.IO) {
        val sourceSong = songDao.getById(songId) ?: return@withContext emptyList()

        val results = mutableListOf<RelatedSong>()
        val sourceArtists = songArtistRefDao.getArtistsForSong(songId)
        val primaryArtist = sourceArtists.firstOrNull()

        // === FONTE 1: Stesso artista (se c'è cross-ref) ===
        if (primaryArtist != null) {
            val sameArtist = songArtistRefDao.getSongsByArtist(primaryArtist.id, limit = 10)
                .filter { it.id != songId }
            sameArtist.forEach { song ->
                results.add(RelatedSong(song, 1.0f, "Stesso artista", RelatedSource.SAME_ARTIST))
            }
        } else if (!sourceSong.artistsText.isNullOrBlank()) {
            // ★ FALLBACK: usa artistsText se non c'è cross-ref
            val sameArtistByText = songDao.getSongsByArtistsTextLike(sourceSong.artistsText, limit = 10)
                .filter { it.id != songId }
            sameArtistByText.forEach { song ->
                results.add(RelatedSong(song, 0.9f, "Stesso artista", RelatedSource.SAME_ARTIST))
            }
        }

        // === FONTE 2: Stesso album (se c'è albumId) ===
        sourceSong.albumId?.let { albumId ->
            val sameAlbum = songDao.getSongsByAlbum(albumId, limit = 10)
                .filter { it.id != songId && it.id !in results.map { it.song.id } }
            sameAlbum.forEach { song ->
                results.add(RelatedSong(song, 0.8f, "Stesso album", RelatedSource.SAME_ALBUM))
            }
        }

        // === FONTE 3: Stesso genere (se ci sono genres) ===
        val sourceGenres = sourceSong.genres.orEmpty().map { it.lowercase() }.toSet()
        if (sourceGenres.isNotEmpty()) {
            val similarSongs = songDao.getSongsWithGenres(limit * 5)
                .filter { it.id != songId && it.id !in results.map { it.song.id } }
                .map { song ->
                    val songGenres = song.genres.orEmpty().map { it.lowercase() }.toSet()
                    val intersection = sourceGenres.intersect(songGenres)
                    Triple(song, intersection, songGenres)
                }
                .filter { it.second.isNotEmpty() }
                .sortedByDescending { it.second.size.toFloat() / it.third.size }
                .take(limit)

            similarSongs.forEach { (song, intersection, _) ->
                results.add(RelatedSong(
                    song = song,
                    score = (intersection.size.toFloat() / sourceGenres.size) * 0.5f,
                    reason = "${intersection.size} generi in comune",
                    source = RelatedSource.SAME_GENRE
                ))
            }
        }

        // === FONTE 4: MB graph (se l'artista ha mbId) ===
        if (primaryArtist?.mbId != null && results.size < limit) {
            val relations = artistRelationDao.getBidirectional(primaryArtist.mbId)
                .filter { getRelationWeight(it.relationType) > 0.5f }

            for (rel in relations.take(5)) {
                if (results.size >= limit) break
                val otherMbId = if (rel.fromArtistId == primaryArtist.mbId) rel.toArtistId else rel.fromArtistId
                val otherArtist = artistDao.getByMbId(otherMbId) ?: continue

                val otherSongs = songArtistRefDao.getSongsByArtist(otherArtist.id, limit = 3)
                otherSongs.forEach { song ->
                    if (song.id !in results.map { it.song.id }) {
                        results.add(RelatedSong(
                            song = song,
                            score = 0.4f,
                            reason = "${otherArtist.name} — ${rel.relationType}",
                            source = RelatedSource.RELATED_ARTIST
                        ))
                    }
                }
            }
        }

        // === FALLBACK FINALE: se la song è Livello 0 e non abbiamo nulla ===
        if (results.isEmpty() && !sourceSong.artistsText.isNullOrBlank()) {
            // Mostra almeno brani che matchano per titolo parziale (cover, versioni)
            val titleFallback = songDao.getSongsByTitleLike(sourceSong.title, limit = 5)
                .filter { it.id != songId }
            titleFallback.forEach { song ->
                results.add(RelatedSong(
                    song = song,
                    score = 0.2f,
                    reason = "Titolo simile",
                    source = RelatedSource.TITLE_MATCH
                ))
            }
        }

        results.sortedByDescending { it.score }.take(limit)
    }

    private fun getRelationWeight(type: String): Float = when (type.lowercase().trim()) {
        "member of band", "member of", "subgroup of", "founder of" -> 1.0f
        "collaborator", "collaborated with" -> 0.7f
        "influenced by", "influencer" -> 0.4f
        else -> 0.0f
    }

    private fun mapMbToAlbum(mb: MBAlbum): Album = Album(
        id = "mb-${mb.id}",
        title = mb.title,
        authorsText = mb.artistCredit,
        originalYear = mb.originalYear,
        albumType = mb.primaryType,
        genres = mb.genres,
        tags = mb.tags,
        rating = mb.rating,
        ratingVotes = mb.ratingVotes,
        isYoutubeAlbum = false,
        timestamp = mb.fetchedAt,
        nature = mb.nature
    )
}


