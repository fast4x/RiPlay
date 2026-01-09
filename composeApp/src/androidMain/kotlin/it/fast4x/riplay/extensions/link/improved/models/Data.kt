package it.fast4x.riplay.extensions.link.improved.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val title: String? = null
)

@Serializable
data class RemoteCommand(
    val action: String, // "load", "play", "pause", "seek", "sync"
    val mediaId: String? = null,
    val position: Float? = null
)

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}