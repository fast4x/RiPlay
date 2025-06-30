package it.fast4x.riplay.ui.screens.player.online

import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.riplay.service.OfflinePlayerService
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@UnstableApi
class MediaSessionCallback (
    val binder: OfflinePlayerService.Binder,
    val onPlayClick: () -> Unit,
    val onPauseClick: () -> Unit,
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

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    override fun onCustomAction(action: String, extras: Bundle?) {
//        super.onCustomAction(action, extras)
//
//    }
}