package it.fast4x.riplay.utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.PlaylistType
import it.fast4x.riplay.extensions.ondevice.Folder
import it.fast4x.riplay.ui.components.themed.SmartMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        else -> preview.playlist.id.toString()
    }

    val name = preview.playlist.name

    CoroutineScope(Dispatchers.IO).launch {
        Database.upsert(
            Blacklist(
                id = Database.blacklist(type, path),
                type = type,
                name = name,
                path = path
            )
        )
        SmartMessage(appContext().getString(R.string.blacklisted, name), context = appContext())
    }
}

fun insertOrUpdateBlacklist(
    album: Album
) {
    val type = BlacklistType.Album.name
    val name = album.title.toString()
    val path = album.id


    CoroutineScope(Dispatchers.IO).launch {
        path.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    name = name,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, name), context = appContext())
        }
    }
}

fun insertOrUpdateBlacklist(
    artist: Artist
) {
    val type = BlacklistType.Artist.name
    val name = artist.name.toString()
    val path = artist.id


    CoroutineScope(Dispatchers.IO).launch {
        path.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    name = name,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, name), context = appContext())
        }
    }
}

fun insertOrUpdateBlacklist(
    song: Song
) {
    val type = if (song.isVideo) BlacklistType.Video.name else BlacklistType.Song.name
    val name = song.title
    val path = song.id

    CoroutineScope(Dispatchers.IO).launch {
        path.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    name = name,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, name), context = appContext())
        }
    }
}

fun insertOrUpdateBlacklist(
    folder: Folder
) {
    val type = BlacklistType.Folder.name
    val name = folder.name
    val path = folder.fullPath

    CoroutineScope(Dispatchers.IO).launch {
        path.let {
            Database.upsert(
                Blacklist(
                    id = Database.blacklist(type, it),
                    type = type,
                    name = name,
                    path = it
                )
            )
            SmartMessage(appContext().getString(R.string.blacklisted, name), context = appContext())
        }
    }
}

