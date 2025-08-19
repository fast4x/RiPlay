package it.fast4x.riplay.ui.screens.player.online.components.core

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.isInvincibilityEnabledKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.playerThumbnailSizeKey
import it.fast4x.riplay.utils.rememberPreference

@OptIn(UnstableApi::class)
@Composable
fun OnlinePlayerCore(
    actAsMini: Boolean = false,
    load: Boolean = false,
    playFromSecond: Float = 0f,
    onPlayerReady: (YouTubePlayer?) -> Unit,
    onSecondChange: (Float) -> Unit,
    onDurationChange: (Float) -> Unit,
    onPlayerStateChange: (PlayerConstants.PlayerState) -> Unit,
    onTap: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    if (binder.player.currentTimeline.windowCount == 0) return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    val player = remember { mutableStateOf<YouTubePlayer?>(null) }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
                if (mediaItem != null) {
                    player.value?.loadVideo(mediaItem.mediaId, 0f)
                    println("OnlinePlayerCore: onMediaItemTransition loaded ${mediaItem.mediaId}")
                }
            }
        }
    }
    val mediaItem = nullableMediaItem ?: return

    val inflatedView = remember { LayoutInflater.from(context()).inflate(R.layout.youtube_player, null, false) }
    val onlinePlayerView = remember { inflatedView as YouTubePlayerView }
    var shouldBePlaying by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val enableBackgroundPlayback by rememberPreference(isInvincibilityEnabledKey, true)
    //var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)
    val isLandscape = isLandscape
    val playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )

    AndroidView(

        factory = {

            val iFramePlayerOptions = IFramePlayerOptions.Builder()
                .controls(0) // show/hide controls
                .listType("playlist")
                .build()

            val listener = object : AbstractYouTubePlayerListener() {

                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    player.value = youTubePlayer
                    onPlayerReady(youTubePlayer)

//                        val customPlayerUiController =
//                            CustomBasePlayerUiControllerAsListener(
//                                it,
//                                customPLayerUi,
//                                youTubePlayer,
//                                onlinePlayerView
//                            )
//                        youTubePlayer.addListener(customPlayerUiController)

                    // Used to show default player ui with defaultPlayerUiController as custom view
                    val customUiController =
                        CustomDefaultPlayerUiController(
                            onlinePlayerView,
                            youTubePlayer,
                            onTap = onTap
                        )
                    customUiController.showUi(false) // disable all default controls and buttons
                    customUiController.showMenuButton(false)
                    customUiController.showVideoTitle(false)
                    customUiController.showPlayPauseButton(false)
                    customUiController.showDuration(false)
                    customUiController.showCurrentTime(false)
                    customUiController.showSeekBar(false)
                    customUiController.showBufferingProgress(false)
                    customUiController.showYouTubeButton(false)
                    customUiController.showFullscreenButton(false)
                    onlinePlayerView.setCustomPlayerUi(customUiController.rootView)

                    //youTubePlayer.loadOrCueVideo(lifecycleOwner.lifecycle, mediaItem.mediaId, lastYTVideoSeconds)
                    println("OnlinePlayerCore: onReady shouldBePlaying: $shouldBePlaying")
                    if (!load)
                        youTubePlayer.cueVideo(mediaItem.mediaId, playFromSecond)
                    else
                        youTubePlayer.loadVideo(mediaItem.mediaId, playFromSecond)

                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    onSecondChange(second)
                    //lastYTVideoSeconds = second

                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    super.onVideoDuration(youTubePlayer, duration)
                    onDurationChange(duration)
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    super.onStateChange(youTubePlayer, state)

                    onPlayerStateChange(state)
                }


            }

            onlinePlayerView.apply {
                enableAutomaticInitialization = false

                if (enableBackgroundPlayback)
                    enableBackgroundPlayback(true)
                else
                    lifecycleOwner.lifecycle.addObserver(this)

                initialize(listener, iFramePlayerOptions)
            }

        },
        update = {
            it.enableBackgroundPlayback(enableBackgroundPlayback)
            when(actAsMini) {
                true -> {
                    it.layoutParams = ViewGroup.LayoutParams(
                        100,
                        100
                    )
                }
                false -> {
                    it.layoutParams = if (!isLandscape) {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                if (playerThumbnailSize == PlayerThumbnailSize.Expanded)
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                else playerThumbnailSize.height
                            )
                        } else {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                }
            }

        }
    )
}