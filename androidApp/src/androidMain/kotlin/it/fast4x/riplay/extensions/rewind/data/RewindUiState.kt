package it.fast4x.riplay.extensions.rewind.data

import it.fast4x.riplay.data.models.Song

data class RewindUiState(
    val isLoading: Boolean = true,

    val topSongs: List<SongMostListened?> = emptyList(),
    val topAlbums: List<AlbumMostListened?> = emptyList(),
    val topArtists: List<ArtistMostListened?> = emptyList(),
    val topPlaylists: List<PlaylistMostListened?> = emptyList(),

    val favoriteSong: AchievementData? = null,
    val favoriteAlbum: AchievementData? = null,
    val favoriteArtist: AchievementData? = null,
    val favoritePlaylist: AchievementData? = null,


    val totals: TotalsData? = null,
    val year: Int? = null
)

data class AchievementData(
    val title: String?,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val minutes: Long,
    val levelEnum: Any,
    val songPreview: Song?
)

data class TotalsData(
    val songsCount: Int,
    val songsMinutes: Long,
    val albumsCount: Int,
    val albumsMinutes: Long,
    val artistsCount: Int,
    val artistsMinutes: Long,
    val playlistsCount: Int,
    val playlistsMinutes: Long
)