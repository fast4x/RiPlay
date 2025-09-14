package it.fast4x.riplay.ui.screens.player.online

import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious

@UnstableApi
class MediaSessionCallback (
    val binder: LocalPlayerService.Binder,
    val onPlayClick: () -> Unit,
    val onPauseClick: () -> Unit,
    val onSeekToPos: (Long) -> Unit
) : MediaSessionCompat.Callback() {
    override fun onPlay() {
        println("MediaSessionCallback onPlay()")
        onPlayClick()
    }
    override fun onPause() { onPauseClick() }
    override fun onSkipToPrevious() {
        binder.player.playPrevious()
    }
    override fun onSkipToNext() {
        binder.player.playNext()
    }

    override fun onSeekTo(pos: Long) {
        onSeekToPos(pos)

    }

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    override fun onCustomAction(action: String, extras: Bundle?) {
//        super.onCustomAction(action, extras)
//
//    }
}