package it.fast4x.riplay.extensions.players

import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import it.fast4x.riplay.utils.globalContext

@UnstableApi
@Composable
fun VideoPlayer(uri: String) {

    val playerListener = remember {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                println("MediaPlayerView Player error: $error")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    //keepScreenOn = true
                } else {
                    //keepScreenOn = false
                }
            }
        }
    }

    val exoPlayer by lazy { ExoPlayer.Builder(globalContext()).build().apply {
        addListener(playerListener)
    } }
    val mediaItem = MediaItem.fromUri(uri)

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(mediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also {
                exoPlayer.setVideoTextureView(it)
                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
            .zIndex(3f)
    )


    /*
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .padding(8.dp)
                .zIndex(3f),
            factory = {
                PlayerView(it).apply {
                    player =
                }
            }
            }
        )
        */
}

