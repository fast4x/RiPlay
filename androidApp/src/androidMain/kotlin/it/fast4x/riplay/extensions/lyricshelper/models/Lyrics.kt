package it.fast4x.riplay.extensions.lyricshelper.models

data class LyricLine(
    val timeMs: Long,
    val text: String = "",
    val words: List<LyricWord> = emptyList()
)

data class LyricWord(
    val text: String,
    val startTimeInTheLineMs: Long,
    val durationMs: Long
)
