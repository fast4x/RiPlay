package it.fast4x.riplay.ui.screens.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.service.modern.PlayerServiceModern
import it.fast4x.riplay.service.modern.isLocal
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playOnline
import it.fast4x.riplay.utils.playOnlineAtIndex

@OptIn(UnstableApi::class)
fun fastPlay(
    mediaItem: MediaItem,
    binder: PlayerServiceModern.Binder?,
    mediaItems: List<MediaItem>? = emptyList(),
    playlistId: String? = null
) {

    println("fastPlay: ${mediaItem.mediaMetadata.extras}")
    Database.asyncTransaction {
        insert(mediaItem)
    }

    binder?.stopRadio()
    if (mediaItem.isLocal) {
        binder?.player?.forcePlay(mediaItem)

    } else {
        binder?.player?.playOnline(mediaItem)
    }
    if (mediaItems != null) binder?.player?.addMediaItems(mediaItems)
}

@OptIn(UnstableApi::class)
fun fastPlayAtIndex(
    index: Int,
    mediaItem: MediaItem,
    binder: PlayerServiceModern.Binder?,
) {
    binder?.stopRadio()
    if (mediaItem.isLocal) {
        binder?.player?.playAtIndex(index)

    } else {
        binder?.player?.playOnlineAtIndex(index)
    }
}