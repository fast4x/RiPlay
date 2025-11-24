package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.YTM_PLAYLIST_SHARE_BASEURL
import it.fast4x.riplay.commonutils.YT_PLAYLIST_SHARE_BASEURL

@Immutable
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val browseId: String? = null,
    val isEditable: Boolean = true,
    val isYoutubePlaylist: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPodcast: Boolean = false,
) {
    val shareYTUrl: String?
        get() = browseId?.let { "$YT_PLAYLIST_SHARE_BASEURL${it.removePrefix("VL")}" }
    val shareYTMUrl: String?
        get() =  browseId?.let { "$YTM_PLAYLIST_SHARE_BASEURL${it.removePrefix("VL")}" }
    val shareYTUrlAsPodcast: String?
        get() = browseId?.let { "$YT_PLAYLIST_SHARE_BASEURL${it.removePrefix("MPSP")}" }
    val shareYTMUrlAsPodcast: String?
        get() =  browseId?.let { "$YTM_PLAYLIST_SHARE_BASEURL${it.removePrefix("MPSP")}" }

    val isPinned: Boolean
        get() = name.startsWith(PINNED_PREFIX)
    val isMonthly: Boolean
        get() = name.startsWith(MONTHLY_PREFIX)

}
