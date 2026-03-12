package it.fast4x.riplay.extensions.lyricshelper.parsers

import it.fast4x.riplay.extensions.lyricshelper.models.LyricLine
import it.fast4x.riplay.extensions.lyricshelper.models.LyricWord
import java.util.regex.Pattern

object LyricsFakeKaraokeParser {

    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    fun parse(lrcString: String?): List<LyricLine> {
        if (lrcString.isNullOrEmpty()) return emptyList()

        val lines = lrcString.lines()
        val parsedLines = mutableListOf<LyricLine>()

        for (i in lines.indices) {
            val line = lines[i]
            val matcher = lrcPattern.matcher(line)

            if (matcher.find()) {
                val min = matcher.group(1).toLong()
                val sec = matcher.group(2).toLong()
                val ms = matcher.group(3).toLong()
                var text = matcher.group(4).trim()

                if (text.isNotEmpty()) {
                    val currentTimeMs = (min * 60 * 1000) + (sec * 1000) + ms
                    val nextTimeMs = findNextTime(lines, i + 1)
                    val duration = nextTimeMs - currentTimeMs

                    val words = splitWords(text, duration)

                    parsedLines.add(LyricLine(currentTimeMs, words = words))
                }
            }
        }
        return parsedLines
    }

    private fun findNextTime(lines: List<String>, startIndex: Int): Long {
        for (i in startIndex until lines.size) {
            val matcher = lrcPattern.matcher(lines[i])
            if (matcher.find()) {
                val min = matcher.group(1).toLong()
                val sec = matcher.group(2).toLong()
                val ms = matcher.group(3).toLong()
                return (min * 60 * 1000) + (sec * 1000) + ms
            }
        }
        return Long.MAX_VALUE
    }

    private fun splitWords(text: String, totalDurationMs: Long): List<LyricWord> {

        val rawWords = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        if (rawWords.isEmpty()) return emptyList()

        val words = mutableListOf<LyricWord>()

        val safeDuration = if (totalDurationMs <= 0) rawWords.size * 300L else totalDurationMs

        val totalChars = rawWords.sumOf { it.length }

        var currentTime = 0L
        val minWordDuration = 20L

        rawWords.forEachIndexed { index, word ->

            val ratio = if (totalChars > 0) {
                word.length.toFloat() / totalChars.toFloat()
            } else {
                1f / rawWords.size
            }

            val calculatedDuration = (safeDuration * ratio).toLong()

            val finalDuration = if (index == rawWords.size - 1) {
                (safeDuration - currentTime).coerceAtLeast(minWordDuration)
            } else {
                maxOf(calculatedDuration, minWordDuration)
            }

            words.add(LyricWord(word, currentTime, finalDuration))
            currentTime += finalDuration
        }

        return words
    }
}