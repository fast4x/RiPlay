package it.fast4x.riplay.extensions.experimental.recommendationstrategy

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.UserProfile

interface RecommendationStrategy {
    val id: String
    val displayName: String
    val displaySubtitle: String

    /**
     * Genera suggerimenti per il profilo utente.
     * @param profile profilo utente corrente
     * @param limit numero massimo di suggerimenti da restituire
     * @param excludedIds set di ID da escludere (brani/album/artisti già consumati o rifiutati)
     */
    suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String> = emptySet()
    ): List<ScoredRecommendation>
}

