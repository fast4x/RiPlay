package it.fast4x.riplay.extensions.lyricshelper.models

import kotlinx.serialization.Serializable

@Serializable
data class SyncLRCResponse(
    val artist: String?,
    val id: String?,
    val lyrics: String?,
    val track: String?,
    val type: String?,
    val error: String?
)


enum class SyncLRCType (
    val type: String
) {
    SYNCED("synced"),
    KARAOKE("karaoke"),
    PLAIN("plain"),
    NONE("none")
}
data class SyncLRCLyrics (
    val type: SyncLRCType = SyncLRCType.KARAOKE,
    val lyrics: String? = "",
    val error: String? = ""
)