package it.fast4x.riplay.utils

import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.PlaylistType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun insertOrUpdateBlacklist(
    playlistType: PlaylistType,
    preview: PlaylistPreview
) {
    val type = when (playlistType) {
        PlaylistType.OnDevicePlaylist -> BlacklistType.Folder.name
        else -> BlacklistType.Playlist.name
    }

    val path = when (playlistType) {
        PlaylistType.OnDevicePlaylist -> preview.folder.toString()
        else -> preview.playlist.name
    }

    CoroutineScope(Dispatchers.IO).launch {
        Database.upsert(
            Blacklist(
                id = Database.blacklist(type, path),
                type = type,
                path = path
            )
        )
        SmartMessage(appContext().getString(R.string.blacklisted, preview.playlist.name), context = appContext())
    }
}

fun insertOrUpdateBlacklist(
    album: Album
) {
    val type = BlacklistType.Album.name
    val path = album.title

    CoroutineScope(Dispatchers.IO).launch {
        path?.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, album.title), context = appContext())
        }
    }
}

fun insertOrUpdateBlacklist(
    artist: Artist
) {
    val type = BlacklistType.Artist.name
    val path = artist.name

    CoroutineScope(Dispatchers.IO).launch {
        path?.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, artist.name), context = appContext())
        }
    }
}