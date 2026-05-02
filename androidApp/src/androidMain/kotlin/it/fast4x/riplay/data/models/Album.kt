package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.enums.LinkType

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

    fun shareUrlByType(typeOfUrl: LinkType): String? {
        return when(typeOfUrl) {
            LinkType.Main -> this.shareYTUrl
            LinkType.Alternative -> this.shareYTMUrl
        }
    }

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
