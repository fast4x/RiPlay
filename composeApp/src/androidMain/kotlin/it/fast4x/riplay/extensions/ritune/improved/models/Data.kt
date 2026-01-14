package it.fast4x.riplay.extensions.ritune.improved.models

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import kotlinx.serialization.Serializable

@Serializable
data class RiTunePlayerState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val title: String? = null,
    val state: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNSTARTED
)

@Serializable
data class RiTuneRemoteCommand(
    val action: String, // "load", "play", "pause", "seek", "sync"
    val mediaId: String? = null,
    val position: Float? = null
)

sealed class RiTuneConnectionStatus {
    object Disconnected : RiTuneConnectionStatus()
    object Connecting : RiTuneConnectionStatus()
    object Connected : RiTuneConnectionStatus()
    data class Error(val message: String) : RiTuneConnectionStatus()
}