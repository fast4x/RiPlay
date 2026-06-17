package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
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

    override suspend fun generate(
        profile: UserProfile,
        limit: Int
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.keywordVector.isEmpty()) return@withContext emptyList()

        // Usa TUTTE le keyword del profilo, non solo le top 5
        val userKeywords = profile.keywordVector.keys
        Timber.tag("REC_DEBUG")
            .d("QualityCurator: userKeywords (${userKeywords.size}) = $userKeywords")

        val candidates = Database.getQualityAlbumsV2(limit = limit * 5)
        Timber.tag("REC_DEBUG").d("QualityCurator: ${candidates.size} candidates from DB")

        val results = candidates
            .map { mbAlbum -> scoreAlbum(mbAlbum, userKeywords, profile) }
            .filter { it.score > 0.2f }
            .sortedByDescending { it.score }
            .take(limit)

        Timber.tag("REC_DEBUG").d("QualityCurator: ${results.size} final results")
        results
    }

    private suspend fun scoreAlbum(
        mbAlbum: MBAlbum,
        userKeywords: Set<String>,
        profile: UserProfile
    ): ScoredRecommendation {
        val albumKeywords = (mbAlbum.genres.orEmpty() + mbAlbum.tags.orEmpty())
            .map { it.lowercase().trim() }
            .toSet()

        // LOG DIAGNOSTICO per i primi 5 album (limita per non spammar log)
        if (mbAlbum.id.hashCode() % 6 == 0) {
            Timber.tag("REC_DEBUG").d("  Album '${mbAlbum.title}' by ${mbAlbum.artistCredit}")
            Timber.tag("REC_DEBUG").d("    albumKeywords: $albumKeywords")
        }

        // Filtro compilation - "Various Artists" è rumore per discovery
        val isCompilation = mbAlbum.artistCredit?.let {
            it.contains("Various Artists", ignoreCase = true) ||
                    it.contains("Varios Artistas", ignoreCase = true) ||
                    it.contains("V.A.", ignoreCase = true) ||
                    it.contains("V/A", ignoreCase = true)
        } ?: false

        if (isCompilation) {
            return emptyScore(mbAlbum)
        }

        // Filtro secondary type Compilation
        if (mbAlbum.secondaryTypes?.any {
                it.equals("Compilation", ignoreCase = true) ||
                        it.equals("Live", ignoreCase = true)
            } == true) {
            return emptyScore(mbAlbum)
        }

        // Intersezione con TUTTE le keyword utente
        val matchedKeywords = albumKeywords.intersect(userKeywords)

        if (matchedKeywords.isEmpty()) {
            return emptyScore(mbAlbum)
        }

        if (mbAlbum.id.hashCode() % 6 == 0) {
            Timber.tag("REC_DEBUG").d("    matched: $matchedKeywords")
        }

        // Score: somma dei pesi delle keyword matchate (normalizzato)
        val genreMatchScore = matchedKeywords
            .sumOf { (profile.keywordVector[it] ?: 0f).toDouble() }
            .toFloat()
            .coerceIn(0f, 1f)

        // Quality: bayesian rating se c'è, altrimenti popularityScore
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
            album = mapMbAlbumToAlbum(mbAlbum),
            artist = null,
            score = score,
            reasons = reasons,
            strategyId = id
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
        timestamp = mb.fetchedAt
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