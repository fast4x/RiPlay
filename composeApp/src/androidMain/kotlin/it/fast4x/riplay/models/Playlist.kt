package it.fast4x.riplay.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.YTM_PLAYLIST_SHARE_BASEURL
import it.fast4x.riplay.YT_PLAYLIST_SHARE_BASEURL

@Immutable
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val browseId: String? = null,
    val isEditable: Boolean = true,
    val isYoutubePlaylist: Boolean = false,
) {
    val shareYTUrl: String?
        get() = if(isYoutubePlaylist) browseId?.let { "$YT_PLAYLIST_SHARE_BASEURL$it" } else null
    val shareYTMUrl: String?
        get() = if(isYoutubePlaylist) browseId?.let { "$YTM_PLAYLIST_SHARE_BASEURL$it" } else null
}
