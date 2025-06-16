package it.fast4x.riplay.enums

import it.fast4x.riplay.R

enum class DownloadedStateMedia {
    CACHED,
    CACHED_AND_DOWNLOADED,
    DOWNLOADED,
    NOT_CACHED_OR_DOWNLOADED;

    val icon: Int
    get() = when (this){
        CACHED -> R.drawable.download
        DOWNLOADED, CACHED_AND_DOWNLOADED -> R.drawable.downloaded
        NOT_CACHED_OR_DOWNLOADED -> R.drawable.download
    }
}