package it.fast4x.riplay.cast.ritune.models

import android.net.nsd.NsdServiceInfo
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
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


@Serializable
data class RiTuneDevices(
    var devices: List<RiTuneDevice> = emptyList<RiTuneDevice>(),
)

@Serializable
data class RiTuneDevice (
    val name: String,
    val host: String,
    val port: Int,
    var selected: Boolean = false,
)

fun NsdServiceInfo.toRiTuneDevice() = RiTuneDevice(
    name = this.serviceName,
    host = this.host.toString(),
    port = this.port,
)

fun String.toRiTuneDevice() = RiTuneDevice(
    name = this.split(",")[0],
    host = this.split(",")[1],
    port = this.split(",")[2].toInt(),
)