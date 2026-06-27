package it.fast4x.riplay.extensions.experimental.recommendationstrategy.models

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song

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

data class RelatedArtist(
    val artist: Artist,
    val score: Float,
    val reason: String,
    val source: RelatedSource
)

data class RelatedAlbum(
    val album: Album,
    val score: Float,
    val reason: String,
    val source: RelatedSource
)

data class RelatedSong(
    val song: Song,
    val score: Float,
    val reason: String,
    val source: RelatedSource
)

enum class RelatedSource {
    MB_GRAPH,
    KEYWORD_SIMILARITY,
    SAME_ARTIST,
    SAME_ALBUM,
    SAME_GENRE,
    SAME_ERA_GENRE,
    RELATED_ARTIST,
    MB_QUALITY,
    TITLE_MATCH
}