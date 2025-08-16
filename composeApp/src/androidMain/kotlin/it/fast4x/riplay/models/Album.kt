package it.fast4x.riplay.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.LOCAL_KEY_PREFIX
import it.fast4x.riplay.YTM_ALBUM_SHARE_BASEURL
import it.fast4x.riplay.YTM_VIDEOORSONG_SHARE_BASEURL
import it.fast4x.riplay.YT_ALBUM_SHARE_BASEURL
import it.fast4x.riplay.YT_VIDEOORSONG_SHARE_BASEURL

@Immutable
@Entity
data class Album(
    @PrimaryKey val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null,
    val isYoutubeAlbum: Boolean = false,
) {
    val shareYTUrl: String?
        get() = shareUrl?.replace("music.","www.")

    val shareYTMUrl: String?
        get() = shareUrl?.replace("www.","music.")

    fun toggleBookmark(): Album {
        return copy(
            bookmarkedAt = if (bookmarkedAt == null) System.currentTimeMillis() else null
        )
    }
}
