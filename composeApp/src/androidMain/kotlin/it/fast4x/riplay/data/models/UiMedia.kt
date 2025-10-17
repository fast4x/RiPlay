package it.fast4x.riplay.data.models

import androidx.media3.common.MediaItem
import it.fast4x.riplay.service.isLocal

data class UiMedia(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val isLocal: Boolean
)

fun MediaItem.toUiMedia(duration: Long) = UiMedia(
    id = mediaId,
    title = mediaMetadata.title?.toString() ?: "",
    artist = mediaMetadata.artist?.toString() ?: "",
    duration = duration,
    isLocal = isLocal
)