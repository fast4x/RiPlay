package it.fast4x.riplay.ui.screens.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.service.modern.PlayerServiceModern
import it.fast4x.riplay.service.modern.isLocal
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.playOnline

@OptIn(UnstableApi::class)
fun fastPlay(
    mediaItem: MediaItem,
    binder: PlayerServiceModern.Binder?,
    mediaItems: List<MediaItem>? = emptyList(),
    playlistId: String? = null
) {
    binder?.stopRadio()
    if (mediaItem.isLocal) {
        binder?.player?.forcePlay(mediaItem)

    } else {
        binder?.player?.playOnline(mediaItem)
    }
    if (mediaItems != null) binder?.player?.addMediaItems(mediaItems)
}