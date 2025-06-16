package it.fast4x.riplay.extensions.players.models

import it.fast4x.environment.models.PlayerResponse

data class PlaybackData(
    val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
    val videoDetails: PlayerResponse.VideoDetails?,
    val playbackTracking: PlayerResponse.PlaybackTracking?,
    val format: PlayerResponse.StreamingData.Format,
    val streamUrl: String,
    val streamExpiresInSeconds: Int,
)
