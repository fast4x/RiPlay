package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class OnDeviceSong (
    @PrimaryKey val id: String,
    val mediaId: String? = null,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?,
    val likedAt: Long? = null,
    val relativePath: String,
    val totalPlayTimeMs: Long = 0
) {
    val formattedTotalPlayTime: String
        get() {
            val seconds = totalPlayTimeMs / 1000

            val hours = seconds / 3600

            return when {
                hours == 0L -> "${seconds / 60}m"
                hours < 24L -> "${hours}h"
                else -> "${hours / 24}d"
            }
        }

    fun toggleLike(): OnDeviceSong {
        return copy(
            likedAt = if (likedAt == null) System.currentTimeMillis() else null
        )
    }
    fun toSong(): Song {
        return Song(
            id = id,
            mediaId = mediaId,
            title = title,
            artistsText = artistsText,
            durationText = durationText,
            thumbnailUrl = thumbnailUrl,
            likedAt = likedAt,
            totalPlayTimeMs = totalPlayTimeMs
        )
    }

    fun toSongEntity(): SongEntity {
        return SongEntity(
            Song(
                id = id,
                mediaId = mediaId,
                title = title,
                artistsText = artistsText,
                durationText = durationText,
                thumbnailUrl = thumbnailUrl,
                likedAt = likedAt,
                totalPlayTimeMs = totalPlayTimeMs
            ),
            albumTitle = "",
            contentLength = 0L
        )
    }
}
