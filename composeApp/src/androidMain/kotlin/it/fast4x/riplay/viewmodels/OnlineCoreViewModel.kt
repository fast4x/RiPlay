package it.fast4x.riplay.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.riplay.getResumePlaybackOnStart
import it.fast4x.riplay.ui.screens.player.online.components.core.OnlinePlayerCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnlineCoreState (
    val onlineCore: @Composable () -> Unit = {},
    val onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null),
    val currentSecond: MutableState<Float> = mutableFloatStateOf(0f),
    val currentDuration: MutableState<Float> = mutableFloatStateOf(0f),
    val currentPlaybackPosition: MutableState<Long> = mutableLongStateOf(0),
    val currentPlaybackDuration: MutableState<Long> = mutableLongStateOf(0),
    val onlinePlayerState: MutableState<PlayerConstants.PlayerState> =
        mutableStateOf(PlayerConstants.PlayerState.UNSTARTED),
    val onlinePlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)
)

class OnlineCoreViewModel: ViewModel() {
    private val _state = MutableStateFlow(OnlineCoreState())
    val state: StateFlow<OnlineCoreState> = _state.asStateFlow()

    private val onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
    private val currentSecond: MutableState<Float> = mutableStateOf(0f)
    private val currentDuration: MutableState<Float> = mutableStateOf(0f)
    private val currentPlaybackPosition: MutableState<Long> = mutableLongStateOf(0)
    private val currentPlaybackDuration: MutableState<Long> = mutableLongStateOf(0)
    private val onlinePlayerState: MutableState<PlayerConstants.PlayerState> =
        mutableStateOf(PlayerConstants.PlayerState.UNSTARTED)
    private val onlinePlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)
    private val onlineCore: @Composable () -> Unit = {
        OnlinePlayerCore(
            load = getResumePlaybackOnStart(),
            playFromSecond = currentSecond.value,
            discordPresenceManager = null,
            onPlayerReady = {
                onlinePlayer.value = it
                updateState()
            },
            onSecondChange = {
                currentSecond.value = it
                currentPlaybackPosition.value = (it * 1000).toLong()
                updateState()
                //println("MainActivity onSecondChange ${currentPlaybackPosition.value}")
            },
            onDurationChange = {
                currentDuration.value = it
                currentPlaybackDuration.value = (it * 1000).toLong()
                updateState()
                //updateOnlineNotification()
                //println("MainActivity onDurationChange ${currentPlaybackDuration.value}")
            },
            onPlayerStateChange = {
                onlinePlayerState.value = it
                onlinePlayerPlayingState.value =
                    it == PlayerConstants.PlayerState.PLAYING
                updateState()
                //updateOnlineNotification()

            },
            onTap = {
                //showControls = !showControls
            },
        )
    }

    fun updateState() {
        _state.value = OnlineCoreState(
            onlineCore = onlineCore,
            onlinePlayer = onlinePlayer,
            currentSecond = currentSecond,
            currentDuration = currentDuration,
            currentPlaybackPosition = currentPlaybackPosition,
            currentPlaybackDuration = currentPlaybackDuration,
            onlinePlayerState = onlinePlayerState,
            onlinePlayerPlayingState = onlinePlayerPlayingState
        )
    }

}