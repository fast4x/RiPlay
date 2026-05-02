package it.fast4x.riplay.extensions.rewind.data

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song

@Immutable
data class SongMostListened(
    @Embedded val song: Song,
    val minutes: Long,
)

@Immutable
data class AlbumMostListened(
    @Embedded val album: Album,
    val minutes: Long,
)

@Immutable
data class PlaylistMostListened(
    @Embedded val playlist: Playlist,
    val minutes: Long,
    val songs: Int,
)

@Immutable
data class ArtistMostListened(
    @Embedded val artist: Artist,
    val minutes: Long,
)

@Immutable
data class SongsListenedCount(
    val songs: Int,
    val minutes: Long,
)

@Immutable
data class AlbumsListenedCount(
    val albums: Int,
    val minutes: Long,
)

@Immutable
data class ArtistsListenedCount(
    val artists: Int,
    val minutes: Long,
)

@Immutable
data class PlaylistsListenedCount(
    val playlists: Int,
    val minutes: Long,
)