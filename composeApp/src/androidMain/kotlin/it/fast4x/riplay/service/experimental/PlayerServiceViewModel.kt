package it.fast4x.riplay.service.experimental

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.utils.isLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentMediaItemIndex: Int = 0,
    val mediaQueue: List<MediaItem> = emptyList(),
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = false
)

@OptIn(UnstableApi::class)
class PlayerServiceViewModel
    (binder: PlayerService.Binder) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.value = _uiState.value.copy(
                currentMediaItemIndex = binder.player.currentMediaItemIndex,
                // Aggiorna la durata quando cambia la traccia
                duration = if (binder.player.currentMediaItem?.isLocal == true)
                    binder.player.duration else binder.onlinePlayerCurrentDuration.toLong()
            )
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING
            )
            // Aggiorna la durata quando è disponibile
            if (playbackState == Player.STATE_READY && binder.player.duration != C.TIME_UNSET) {
                _uiState.value = _uiState.value
                    .copy(duration = if (binder.player.currentMediaItem?.isLocal == true)
                        binder.player.duration else binder.onlinePlayerCurrentDuration.toLong())
            }
        }

    }
}