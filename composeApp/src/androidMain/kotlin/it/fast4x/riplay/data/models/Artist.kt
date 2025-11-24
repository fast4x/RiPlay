package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.commonutils.YTM_ARTIST_SHARE_BASEURL
import it.fast4x.riplay.commonutils.YT_ARTIST_SHARE_BASEURL

@Immutable
@Entity
data class Artist(
    @PrimaryKey val id: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null,
    val isYoutubeArtist: Boolean = false,
) {
    val shareYTUrl: String?
        get() = id.let { "$YT_ARTIST_SHARE_BASEURL$it" }
    val shareYTMUrl: String?
        get() = id.let { "$YTM_ARTIST_SHARE_BASEURL$it" }

}
