package it.fast4x.riplay.extensions.lyricshelper.models

data class LRCLyricLine(
    val timeMs: Long,
    val text: String = "",
    val words: List<LRCLyricWord> = emptyList()
)

data class LRCLyricWord(
    val text: String,
    val startTimeInTheLineMs: Long,
    val durationMs: Long
)
