package it.fast4x.riplay.data.models

import androidx.room.Embedded

/** A saved playlist membership, including tracks that have never been played. */
data class PlaylistRecommendationSource(
    @Embedded val song: Song,
    val playlistId: Long,
    val playlistName: String,
    val artistIds: String? = null,
    val albumIds: String? = null
)
