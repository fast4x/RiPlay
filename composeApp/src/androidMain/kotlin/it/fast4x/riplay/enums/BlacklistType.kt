package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class BlacklistType {
    Album, Artist, Song, Folder, Playlist;

    val title: String
        @Composable
        get() = when(this) {
            Album -> stringResource(R.string.albums)
            Artist -> stringResource(R.string.artists)
            Song -> stringResource(R.string.songs)
            Folder -> stringResource(R.string.folders)
            Playlist -> stringResource(R.string.playlists)
        }
}