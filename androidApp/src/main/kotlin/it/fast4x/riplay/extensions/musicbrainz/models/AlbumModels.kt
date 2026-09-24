package it.fast4x.riplay.extensions.musicbrainz.models

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: Album? = null,
    val tracks: List<Song> = emptyList(),
    val artist: Artist? = null,
    val otherAlbums: List<Album> = emptyList(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val stats: AlbumStats? = null
)

data class AlbumStats(
    val totalPlayTimeMs: Long,
    val playCount: Int,
    val likedSongsCount: Int,
    val tracksCount: Int
)