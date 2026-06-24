package it.fast4x.riplay.extensions.musicbrainz.models

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song


data class ArtistDetailUiState(
    val isLoading: Boolean = true,
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val topTracks: List<Song> = emptyList(),
    val relations: List<ArtistRelationInfo> = emptyList(),
    val memberOf: List<Artist> = emptyList(),
    val relatedArtists: List<Artist> = emptyList(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val stats: ArtistStats? = null
)

data class ArtistRelationInfo(
    val artist: Artist,
    val relationType: String,
    val direction: String
)

data class ArtistStats(
    val totalPlayTimeMs: Long,
    val playCount: Int,
    val likedSongsCount: Int,
    val distinctAlbumsCount: Int
)
