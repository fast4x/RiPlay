package it.fast4x.riplay.ui.screens.player.online.components.core

import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
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
import it.fast4x.riplay.appContext
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.history.updateOnlineHistory
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.extensions.preferences.isInvincibilityEnabledKey
import it.fast4x.riplay.extensions.preferences.isKeepScreenOnEnabledKey
import it.fast4x.riplay.extensions.preferences.lastVideoIdKey
import it.fast4x.riplay.extensions.preferences.playbackDurationKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.getPlaybackFadeAudioDuration
import it.fast4x.riplay.isSkipMediaOnErrorEnabled
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.clearWebViewData
import it.fast4x.riplay.utils.isVideo
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
fun ExternalOnlineCore(
    onlinePlayerView: YouTubePlayerView,
    player: MutableState<YouTubePlayer?>,
    onlinePlayerIsInitialized: MutableState<Boolean> = mutableStateOf(false),
    actAsMini: Boolean = false,
    load: Boolean = false,
    playFromSecond: Float = 0f,
    onPlayerReady: (YouTubePlayer?) -> Unit,
    onSecondChange: (Float) -> Unit,
    onDurationChange: (Float) -> Unit,
    onPlayerStateChange: (PlayerConstants.PlayerState) -> Unit,
    onTap: () -> Unit,
) {

    Timber.d("OnlinePlayerCore: called")
    val binder = LocalPlayerServiceBinder.current

    var localMediaItem by remember { mutableStateOf( binder?.player?.currentMediaItem ) }
    if (localMediaItem?.isLocal == true) return

    val queueLoopType by rememberObservedPreference(queueLoopTypeKey, QueueLoopType.Default)
    var playerState by remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }

    var lastError = remember { mutableStateOf<PlayerConstants.PlayerError?>(null) }
    val lastVideoId = rememberPreference(lastVideoIdKey, "")

    binder?.player?.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    if (it.isLocal) {
                        player.value?.pause()
                        return
                    }

                    localMediaItem = it
                    lastVideoId.value = it.mediaId
                    player.value?.loadVideo(it.mediaId, 0f)
                    updateOnlineHistory(it)
                    Timber.d("OnlinePlayerCore: onMediaItemTransition ${it.mediaId}")
                }
            }

        }
    }

    val context = LocalContext.current

    var shouldBePlaying by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val enableBackgroundPlayback by rememberPreference(isInvincibilityEnabledKey, true)
    val enableKeepScreenOn by rememberPreference(isKeepScreenOnEnabledKey, false)
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

        }

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

    val iFramePlayerOptions = remember {
        IFramePlayerOptions.Builder(appContext())
            .controls(0) // show/hide controls
            .listType("playlist")
            .origin(context().resources.getString(R.string.env_fqqhBZd0cf))
            .build()
    }

    val listener = remember {
        object : AbstractYouTubePlayerListener() {

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                //player.value = youTubePlayer
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
                        context,
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
                Timber.d("OnlinePlayerCore: onReady shouldBePlaying: $shouldBePlaying")
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

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                super.onError(youTubePlayer, error)

                localMediaItem?.isLocal?.let { if (it) return }

                youTubePlayer.pause()
                clearWebViewData()

                Timber.e("OnlinePlayerCore: onError $error")
                val errorString = when (error) {
                    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "Content not playable, recovery in progress, try to click play but if the error persists try to log in"
                    PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Content not found, perhaps no longer available"
                    else -> null
                }

                if (errorString != null && lastError.value != error) {
                    SmartMessage(
                        errorString,
                        PopupType.Error,
                        //durationLong = true,
                        context = context()
                    )
                    localMediaItem?.let { youTubePlayer.cueVideo(it.mediaId, 0f) }

                }

                lastError.value = error

                if (!isSkipMediaOnErrorEnabled()) return
                val prev = binder?.player?.currentMediaItem ?: return

                binder.player.playNext()

                SmartMessage(
                    message = context().getString(
                        R.string.skip_media_on_error_message,
                        prev.mediaMetadata.title
                    ),
                    context = context(),
                )

            }

        }
    }

    val onlinePlayerViewInitalized = remember {
        onlinePlayerView.apply {
            enableAutomaticInitialization = false

            if (enableBackgroundPlayback)
                enableBackgroundPlayback(true)
            else
                lifecycleOwner.lifecycle.addObserver(this)

            onlinePlayerView.keepScreenOn = enableKeepScreenOn

            if (!onlinePlayerIsInitialized.value)
                initialize(listener, iFramePlayerOptions)

            //initialized, not initialize again
            onlinePlayerIsInitialized.value = true

            Timber.d("OnlinePlayerCore: initialize")
        }
    }

    // if not video, android view not required
    if (localMediaItem?.isVideo == true) {
        AndroidView(
            //modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            factory = {

                onlinePlayerViewInitalized

            },
            update = {

//            (it.parent as? DialogWindowProvider)
//                ?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                it.enableBackgroundPlayback(enableBackgroundPlayback)
                //inflatedView.keepScreenOn = enableKeepScreenOn
                it.keepScreenOn = enableKeepScreenOn

                when (actAsMini) {
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
    } else {
        LocalView.current.keepScreenOn = enableKeepScreenOn
        onlinePlayerViewInitalized.keepScreenOn = enableKeepScreenOn
    }


}