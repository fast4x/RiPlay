package it.fast4x.rimusic.ui.screens.player.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.DefaultPlayerUiController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.rimusic.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.screens.player.CustomPlayerUiController
import it.fast4x.rimusic.ui.styling.collapsedPlayerProgressBar
import it.fast4x.rimusic.utils.lastVideoIdKey
import it.fast4x.rimusic.utils.lastVideoSecondsKey
import it.fast4x.rimusic.utils.rememberPreference


@Composable
fun YoutubePlayer(
    ytVideoId: String,
    lifecycleOwner: LifecycleOwner,
    showPlayer: Boolean = true,
    onCurrentSecond: (second: Float) -> Unit,
    onSwitchToAudioPlayer: () -> Unit
) {

    if (!showPlayer) return

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

//    val currentYTVideoId by remember { mutableStateOf(ytVideoId) }
//    println("mediaItem youtubePlayer called currentYTVideoId $currentYTVideoId ytVideoId $ytVideoId lastYTVideoId $lastYTVideoId")

    if (ytVideoId != lastYTVideoId) lastYTVideoSeconds = 0f

    Box {
        Box{
            Image(
                painter = painterResource(R.drawable.musical_notes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette().collapsedPlayerProgressBar),
                modifier = Modifier
                    .clickable {
                        onSwitchToAudioPlayer()
                    }
                    .padding(top = 30.dp, start = 10.dp)
                    .size(24.dp)
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                //.padding(8.dp)
                //.clip(RoundedCornerShape(10.dp))
                .zIndex(2f),
            factory = {

                val youtubePlayerView = YouTubePlayerView(context = it)
                val customPLayerUi = youtubePlayerView.inflateCustomPlayerUi(R.layout.ayp_default_player_ui)


//                val iFramePlayerOptions = IFramePlayerOptions.Builder()
//                    .controls(1) // show/hide controls
//                    .rel(0) // related video at the end
//                    .ivLoadPolicy(0) // show/hide annotations
//                    .ccLoadPolicy(0) // show/hide captions
//                    // Play a playlist by id
//                    //.listType("playlist")
//                    //.list(PLAYLIST_ID)
//                    .build()

                // Disable default view controls to set custom view
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {

                        val customPlayerUiController = CustomPlayerUiController(
                            it,
                            customPLayerUi,
                            youTubePlayer,
                            youtubePlayerView
                        )
                        youTubePlayer.addListener(customPlayerUiController)

                        youTubePlayer.loadVideo(ytVideoId, lastYTVideoSeconds)
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        onCurrentSecond(second)
                        lastYTVideoSeconds = second
                        lastYTVideoId = ytVideoId
                    }

                }

                youtubePlayerView.apply {
                    enableAutomaticInitialization = false

                    lifecycleOwner.lifecycle.addObserver(this)

                    initialize(listener, true, iFramePlayerOptions)
                }

            }
        )
    }

}
