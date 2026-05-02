package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class BlacklistType {
    Album, Artist, Song, Folder, Playlist, Video;

    val title: Int
        get() = when(this) {
            Album -> R.string.albums
            Artist -> R.string.artists
            Song -> R.string.songs
            Folder -> R.string.folders
            Playlist -> R.string.playlists
            Video -> R.string.videos
        }
}