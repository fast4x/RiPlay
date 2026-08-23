package it.fast4x.riplay.services.playback.common

import androidx.media3.common.MediaItem
import it.fast4x.riplay.enums.QueueLoopType

data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val mediaInfo: MediaInfo? = null,
    val settings: PlayerSettings = PlayerSettings(),
    val errorMessage: String? = null
) {
    val isPlaying: Boolean
        get() = playbackState == PlaybackState.PLAYING

    fun withMediaTransition(
        mediaItem: MediaItem,
        queueIndex: Int,
        queueSize: Int,
    ): PlayerState = copy(
        mediaInfo = MediaInfo(
            mediaItem = mediaItem,
            queueIndex = queueIndex,
            queueSize = queueSize,
        ),
        errorMessage = null,
    )

    fun withDatabaseMediaItemIfCurrent(
        currentMediaId: String?,
        databaseMediaItem: MediaItem,
        queueIndex: Int,
        queueSize: Int,
    ): PlayerState = if (currentMediaId == databaseMediaItem.mediaId) {
        withMediaTransition(
            mediaItem = databaseMediaItem,
            queueIndex = queueIndex,
            queueSize = queueSize,
        )
    } else {
        this
    }
}

data class PlayerSettings(
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: QueueLoopType = QueueLoopType.Default,
    val playbackSpeed: Float = 1.0f
)

data class MediaInfo(
    val mediaItem: MediaItem,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
)

enum class PlaybackState {
    IDLE,
    UNSTARTED,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}