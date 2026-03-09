package it.fast4x.riplay.extensions.lyricsparser

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