package it.fast4x.riplay.ui.screens.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playOnline
import it.fast4x.riplay.utils.playOnlineAtIndex

@OptIn(UnstableApi::class)
fun fastPlay(
    mediaItem: MediaItem,
    binder: LocalPlayerService.Binder?,
    mediaItems: List<MediaItem>? = emptyList(),
    playlistId: String? = null,
    replace: Boolean = false
) {

    Database.asyncTransaction {
        insert(mediaItem)
    }

    binder?.stopRadio()

    if (mediaItem.isLocal) {
        binder?.player?.forcePlay(mediaItem, replace)
    } else {
        binder?.player?.playOnline(mediaItem, replace)
    }
    if (mediaItems != null) binder?.player?.addMediaItems(mediaItems)
}

@OptIn(UnstableApi::class)
fun fastPlayAtIndex(
    index: Int,
    mediaItem: MediaItem,
    binder: LocalPlayerService.Binder?,
) {
    binder?.stopRadio()
    if (mediaItem.isLocal) {
        binder?.player?.playAtIndex(index)

    } else {
        binder?.player?.playOnlineAtIndex(index)
    }
}