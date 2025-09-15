package it.fast4x.riplay.ui.screens.player.online

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.service.AndroidAutoService.Companion.localPlayerBinder
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.ui.screens.player.fastPlay
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@UnstableApi
class MediaSessionCallback (
    val binder: LocalPlayerService.Binder,
    val onPlayClick: () -> Unit,
    val onPauseClick: () -> Unit,
    val onSeekToPos: (Long) -> Unit,
    val onPlayNext: () -> Unit,
    val onPlayPrevious: () -> Unit,
) : MediaSessionCompat.Callback() {

    override fun onPlay() {
        Timber.d("MediaSessionCallback onPlay()")
        onPlayClick()
    }
    override fun onPause() {
        Timber.d("MediaSessionCallback onPause()")
        onPauseClick()
    }
    override fun onSkipToPrevious() {
        Timber.d("MediaSessionCallback onSkipToPrevious()")
        onPlayPrevious()
    }
    override fun onSkipToNext() {
        Timber.d("MediaSessionCallback onSkipToNext()")
        onPlayNext()
    }

    override fun onSeekTo(pos: Long) {
        Timber.d("MediaSessionCallback onSeekTo() $pos")
        onSeekToPos(pos)
    }

    @OptIn(UnstableApi::class)
    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} called")
        val data = mediaId?.split('/') ?: return
        val id = data.getOrNull(1)

        Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} data $data elaborated")

        CoroutineScope(Dispatchers.Main).launch {
            val mediaItem = Database.song(id).first()?.asMediaItem ?: return@launch
            fastPlay(
                mediaItem,
                binder
            )
        }
    }

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    override fun onCustomAction(action: String, extras: Bundle?) {
//        super.onCustomAction(action, extras)
//
//    }
}