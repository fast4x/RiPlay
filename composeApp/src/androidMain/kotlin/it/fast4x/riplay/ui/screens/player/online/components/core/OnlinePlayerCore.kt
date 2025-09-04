package it.fast4x.riplay.ui.screens.player.online.components.core

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOnlinePlayer
import it.fast4x.riplay.extensions.history.updateOnlineHistory
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.extensions.preferences.isInvincibilityEnabledKey
import it.fast4x.riplay.extensions.preferences.playbackDurationKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.getPlaybackFadeAudioDuration
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.startFadeAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun OnlinePlayerCore(
    actAsMini: Boolean = false,
    load: Boolean = false,
    playFromSecond: Float = 0f,
    discordPresenceManager: DiscordPresenceManager?,
    onPlayerReady: (YouTubePlayer?) -> Unit,
    onSecondChange: (Float) -> Unit,
    onDurationChange: (Float) -> Unit,
    onPlayerStateChange: (PlayerConstants.PlayerState) -> Unit,
    onTap: () -> Unit,
) {
    println("OnlinePlayerCore: called")
    val binder = LocalPlayerServiceBinder.current
//    binder?.player ?: return
//    if (binder.player.currentTimeline.windowCount == 0) return

//    var nullableMediaItem by remember {
//        mutableStateOf(binder?.player?.currentMediaItem, neverEqualPolicy())
//    }
    var localMediaItem = remember { binder?.player?.currentMediaItem }

    val player = remember { mutableStateOf<YouTubePlayer?>(null) }

    val queueLoopType by rememberObservedPreference(queueLoopTypeKey, QueueLoopType.Default)
    var playerState by remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var currentSecond by remember { mutableFloatStateOf(0f) }

    binder?.player?.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    localMediaItem = it
                    player.value?.loadVideo(it.mediaId, 0f)
                    updateOnlineHistory(it)

//                    updateDiscordPresenceWithOnlinePlayer(
//                        discordPresenceManager,
//                        mediaItem = it,
//                        playerState = mutableStateOf(playerState),
//                        currentDuration,
//                        currentSecond
//                    )

                    Timber.d("OnlinePlayerCore: onMediaItemTransition loaded ${it.mediaId}")
                }
            }

        }
    }

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

    //MedleyMode for online player
    val playbackDuration by rememberObservedPreference(playbackDurationKey, 0f)


    LaunchedEffect(playerState) {
        if (playerState == PlayerConstants.PlayerState.ENDED) {
            // TODO Implement repeat mode in queue
            when (queueLoopType) {
                QueueLoopType.RepeatOne -> {
                    player.value?.seekTo(0f)
                    Timber.d("OnlinePlayerCore Repeat: RepeatOne fired")
                }
                QueueLoopType.Default -> {
                    val hasNext = binder?.player?.hasNextMediaItem()
                    Timber.d("OnlinePlayerCore Repeat: Default fired")
                    if (hasNext == true) {
                        binder.player.playNext()
                        Timber.d("OnlinePlayerCore Repeat: Default fired next")
                    }
                }
                QueueLoopType.RepeatAll -> {
                    val hasNext = binder?.player?.hasNextMediaItem()
                    Timber.d("OnlinePlayerCore Repeat: RepeatAll fired")
                    if (hasNext == false) {
                        binder.player.seekTo(0, 0)
                        player.value?.play()
                        Timber.d("OnlinePlayerCore Repeat: RepeatAll fired first")
                    } else {
                        binder?.player?.playNext()
                        Timber.d("OnlinePlayerCore Repeat: RepeatAll fired next")
                    }
                }
            }
//            if (getQueueLoopType() == QueueLoopType.RepeatOne) {
//                player.value?.seekTo(0f)
//            }
//
//            if (binder?.player?.hasNextMediaItem() == true)
//                binder.player.playNext()

        }

//        updateDiscordPresenceWithOnlinePlayer(
//            discordPresenceManager,
//            mediaItem = localMediaItem!!,
//            playerState = mutableStateOf(playerState),
//            currentDuration,
//            currentSecond
//        )
    }

    LaunchedEffect(playbackDuration) {
        if (playbackDuration > 0f)
            while (isActive) {
                delay((1.seconds * playbackDuration.roundToInt()) + 2.seconds)
                withContext(Dispatchers.Main) {
                    Timber.d("MedleyMode: Pre fired next")
                    if (playerState == PlayerConstants.PlayerState.PLAYING) {
                        player.value?.pause()
                        player.value?.seekTo(0f)
                        binder?.player?.playNext()
                        Timber.d("MedleyMode: next fired")
                    }
                }
            }
    }

    //Playback speed for online player
    var playbackSpeed by rememberObservedPreference(playbackSpeedKey, 1f)
    LaunchedEffect(playbackSpeed) {
        val plabackRate = when {
            (playbackSpeed.toDouble() in 0.0..0.25)     -> PlayerConstants.PlaybackRate.RATE_0_25
            (playbackSpeed.toDouble() in 0.26..0.5)     -> PlayerConstants.PlaybackRate.RATE_0_5
            (playbackSpeed.toDouble() in 0.51..0.75)    -> PlayerConstants.PlaybackRate.RATE_0_75
            (playbackSpeed.toDouble() in 0.76..1.0)     -> PlayerConstants.PlaybackRate.RATE_1
            (playbackSpeed.toDouble() in 1.01..1.25)    -> PlayerConstants.PlaybackRate.RATE_1_25
            (playbackSpeed.toDouble() in 1.26..1.5)     -> PlayerConstants.PlaybackRate.RATE_1_5
            (playbackSpeed.toDouble() in 1.51..1.75)    -> PlayerConstants.PlaybackRate.RATE_1_75
            (playbackSpeed.toDouble() > 1.76) -> PlayerConstants.PlaybackRate.RATE_2
            else -> PlayerConstants.PlaybackRate.RATE_1
        }
        player.value?.setPlaybackRate(plabackRate)
    }

    println("OnlinePlayerCore: before create androidview")

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
                    if (localMediaItem != null) {
                        if (!load)
                            youTubePlayer.cueVideo(localMediaItem!!.mediaId, playFromSecond)
                        else
                            youTubePlayer.loadVideo(localMediaItem!!.mediaId, playFromSecond)
                    }

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

//                    val fadeDisabled = getPlaybackFadeAudioDuration() == DurationInMilliseconds.Disabled
//                    val duration = getPlaybackFadeAudioDuration().milliSeconds
//                    if (!fadeDisabled)
//                        startFadeAnimator(
//                            player = player,
//                            duration = duration,
//                            fadeIn = true
//                        )

                    playerState = state
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
                println("OnlinePlayerCore: initialize")
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

    fun callPause(
        onPause: () -> Unit
    ) {
        val fadeDisabled = getPlaybackFadeAudioDuration() == DurationInMilliseconds.Disabled
        val duration = getPlaybackFadeAudioDuration().milliSeconds
        if (playerState == PlayerConstants.PlayerState.PLAYING) {
            if (fadeDisabled) {
                player.value?.pause()
                onPause()
            } else {
                //fadeOut
                startFadeAnimator(player, duration, false) {
                    player.value?.pause()
                    onPause()
                }
            }
        }
    }

}