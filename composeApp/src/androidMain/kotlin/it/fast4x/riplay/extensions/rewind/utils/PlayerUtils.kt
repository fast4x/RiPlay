package it.fast4x.riplay.extensions.rewind.utils

import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlay

fun rewindPlayMedia(song: Song?, binder: PlayerService.Binder?) {
    if (binder == null || song == null) return

    if (song.isLocal)
        binder.player.forcePlay(song.asMediaItem)
    else
        binder.onlinePlayer?.loadVideo(song.id, 0f)

}

fun rewindPauseMedia(binder: PlayerService.Binder?) {
    if (binder == null) return

    binder.player.pause()
    binder.onlinePlayer?.pause()
}