package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable
data class PlaylistPreview(
    @Embedded val playlist: Playlist,
    val songCount: Int,
    val isOnDevice: Boolean = false,
    val folder: String? = null,
    val totalPlayTimeMs: Long? = null,
)

