package it.fast4x.riplay.extensions.experimental.recommendationstrategy

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song

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

data class UserProfile(
    val userId: String,
    val topArtists: List<ArtistAffinity>,
    val keywordVector: Map<String, Float>,
    val eraVector: Map<Int, Float>,
    val bookmarkedArtistIds: Set<String>,
    val bookmarkedAlbumIds: Set<String>,
    val lastRefreshedAt: Long
)

data class ArtistAffinity(
    val artistId: String,
    val score: Float,
    val playCount: Int
)

data class ScoredRecommendation(
    val song: Song?,          // nullable: alcune strategy producono solo album/artist
    val album: Album?,
    val artist: Artist?,
    val score: Float,
    val reasons: List<String>,
    val strategyId: String,
    val strategyDisplayName: String = ""
) {
    val primaryTitle: String
        get() = song?.title ?: album?.title ?: artist?.name ?: "—"

    val primarySubtitle: String
        get() = song?.artistsText ?: album?.authorsText ?: artist?.info ?: ""
}

data class DiscoveryInfo(
    val strategyId: String,
    val strategyDisplayName: String,
    val reasons: List<String>,
    val itemId: String
)