package it.fast4x.riplay.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.LOCAL_KEY_PREFIX
import it.fast4x.riplay.YTM_PLAYLIST_SHARE_BASEURL
import it.fast4x.riplay.YTM_VIDEOORSONG_SHARE_BASEURL
import it.fast4x.riplay.YT_VIDEOORSONG_SHARE_BASEURL
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.utils.durationTextToMillis
import it.fast4x.riplay.utils.setDisLikeState
import it.fast4x.riplay.utils.setLikeState
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0,
    @ColumnInfo(defaultValue = "1")
    val isAudioOnly: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val isPodcast: Int = 0
) {

    val shareYTUrl: String?
        get() = if(!id.startsWith(LOCAL_KEY_PREFIX))
            id.let { "${YT_VIDEOORSONG_SHARE_BASEURL}$it" } else null

    val shareYTMUrl: String?
        get() = if(!id.startsWith(LOCAL_KEY_PREFIX))
            id.let { "${YTM_VIDEOORSONG_SHARE_BASEURL}$it" } else null

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

    val isVideo: Boolean
        get() = isAudioOnly == 0

    fun toggleLike(): Song {
        return copy(
            //likedAt = if (likedAt == null) System.currentTimeMillis() else null
            likedAt = setLikeState(likedAt)
        )
    }

    fun toggleDislike(): Song {
        return copy(
            likedAt = setDisLikeState(likedAt)
        )
    }

    fun cleanTitle() = cleanPrefix( this.title )

    fun relativePlayTime(): Double {
        val totalPlayTimeMs = durationTextToMillis(this.durationText ?: "")
        return if(totalPlayTimeMs > 0) this.totalPlayTimeMs.toDouble() / totalPlayTimeMs.toDouble() else 0.0
    }
}