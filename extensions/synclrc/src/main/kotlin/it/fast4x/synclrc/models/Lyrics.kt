package it.fast4x.synclrc.models

data class LyricLine(
    val timeMs: Long,
    val text: String = "",
    val words: List<LyricWord> = emptyList()
)

data class LyricWord(
    val text: String,
    val startTimeLineMs: Long,
    val durationMs: Long
)

enum class SyncLRCType (
    val type: String
) {
    SYNCED("synced"),
    KARAOKE("karaoke"),
    PLAIN("plain")
}
data class SyncLRCLyrics (
    val type: SyncLRCType = SyncLRCType.KARAOKE,
    val lyrics: String? = ""
)
