package it.fast4x.riplay.extensions.rewind.utils

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlay

@OptIn(UnstableApi::class)
fun rewindPlayMedia(song: Song?, binder: PlayerService.Binder?) {
    if (binder == null || song == null) return

    if (song.isLocal)
        binder.player.forcePlay(song.asMediaItem)
    else
        binder.onlinePlayer?.loadVideo(song.id, 0f)

}

@OptIn(UnstableApi::class)
fun rewindPauseMedia(binder: PlayerService.Binder?) {
    if (binder == null) return

    binder.player.pause()
    binder.onlinePlayer?.pause()
}