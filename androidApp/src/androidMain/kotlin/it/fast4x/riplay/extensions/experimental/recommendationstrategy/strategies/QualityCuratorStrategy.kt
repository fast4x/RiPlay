package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationConstants
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoringUtils
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class QualityCuratorStrategy() : RecommendationStrategy {

    override val id: String = "quality_curator"
    override val displayName: String = "Capolavori del genere"
    override val displaySubtitle: String = "Album acclamati dalla community che potresti apprezzare"

    val mbAlbumDao = Database.mbAlbumDao()
    val albumDao = Database.albumDao()
    val songDao = Database.songDao()

    override suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String>
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.keywordVector.isEmpty()) return@withContext emptyList()

        val userKeywords = profile.keywordVector.keys
        val candidates = mbAlbumDao.getQualityAlbumsV2(limit = limit * 5)

        candidates
            .filter { "mb-${it.id}" !in excludedIds }  // ★ filtro excluded (MBAlbum ha id con prefisso)
            .map { mbAlbum -> scoreAlbum(mbAlbum, userKeywords, profile) }
            .filter { it.score > 0.2f }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun scoreAlbum(
        mbAlbum: MBAlbum,
        userKeywords: Set<String>,
        profile: UserProfile
    ): ScoredRecommendation {
        val albumKeywords = (mbAlbum.genres.orEmpty() + mbAlbum.tags.orEmpty())
            .map { it.lowercase().trim() }
            .toSet()

        val matchedKeywords = albumKeywords.intersect(userKeywords)
        if (matchedKeywords.isEmpty()) {
            return emptyScore(mbAlbum)
        }

        val genreMatchScore = matchedKeywords
            .sumOf { (profile.keywordVector[it] ?: 0f).toDouble() }
            .toFloat()
            .coerceIn(0f, 1f)

        val qualityBonus = if (mbAlbum.rating != null && (mbAlbum.ratingVotes ?: 0) > 0) {
            ScoringUtils.mbQualityBonus(mbAlbum.rating, mbAlbum.ratingVotes)
        } else {
            mbAlbum.popularityScore.coerceIn(0.3f, 0.7f)
        }

        val eraScore = mbAlbum.originalYear?.let { year ->
            val decade = (year / 10) * 10
            profile.eraVector[decade] ?: 0f
        } ?: 0f

        val score = (0.5f * genreMatchScore +
                0.3f * qualityBonus +
                0.2f * eraScore).coerceIn(0f, 1f)

        val album = mbAlbum.matchedAlbumId?.takeIf { it.isNotEmpty() }
            ?.let { albumDao.getById(it) }

        val reasons = buildList {
            if (mbAlbum.rating != null && (mbAlbum.ratingVotes ?: 0) > 0) {
                add("Rating MB: ${mbAlbum.rating}/5 (${mbAlbum.ratingVotes} voti)")
            } else if (mbAlbum.tagCount >= 5) {
                add("Album ben documentato su MB (${mbAlbum.tagCount} tag)")
            } else {
                add("Album presente su MusicBrainz")
            }
            if (matchedKeywords.isNotEmpty()) {
                add("Generi in comune: ${matchedKeywords.take(3).joinToString(", ")}")
            }
            mbAlbum.originalYear?.let { add("Anno: $it") }
            mbAlbum.primaryType?.let { add("Tipo: $it") }
            mbAlbum.artistCredit?.let { add("Artista: $it") }
        }

        return ScoredRecommendation(
            song = null,
            album = album ?: mapMbAlbumToAlbum(mbAlbum),
            artist = null,
            score = score,
            reasons = reasons,
            strategyId = id,
            strategyDisplayName = displayName
        )
    }

    private fun mapMbAlbumToAlbum(mb: MBAlbum): Album = Album(
        id = "mb-${mb.id}",
        title = mb.title,
        authorsText = mb.artistCredit,
        originalYear = mb.originalYear,
        albumType = mb.primaryType,
        genres = mb.genres,
        tags = mb.tags,
        rating = mb.rating,
        ratingVotes = mb.ratingVotes,
        wikipediaUrl = mb.wikipediaUrl,
        links = mb.links,
        isYoutubeAlbum = false,
        timestamp = mb.fetchedAt,
        nature = mb.nature
    )

    private fun emptyScore(mb: MBAlbum) = ScoredRecommendation(
        song = null,
        album = mapMbAlbumToAlbum(mb),
        artist = null,
        score = 0f,
        reasons = emptyList(),
        strategyId = id
    )
}