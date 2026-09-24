package it.fast4x.riplay.extensions.fastshare

import androidx.media3.common.MediaItem
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.enums.LinkType
import it.fast4x.riplay.utils.asSong

fun getShareUrl(content: Any, typeOfUrl: LinkType): String? {
    return when (content) {
        is MediaItem -> content.asSong.shareUrlByType(typeOfUrl)
        is Playlist ->  content.shareUrlByType(typeOfUrl)
        is Album -> content.shareUrlByType(typeOfUrl)
        is Artist -> content.shareUrlByType(typeOfUrl)
        else -> ""
    }
}