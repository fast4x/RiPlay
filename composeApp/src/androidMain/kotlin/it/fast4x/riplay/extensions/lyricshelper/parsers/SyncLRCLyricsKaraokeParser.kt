package it.fast4x.riplay.extensions.lyricshelper.parsers

import it.fast4x.riplay.extensions.lyricshelper.models.LyricLine
import it.fast4x.riplay.extensions.lyricshelper.models.LyricWord
import timber.log.Timber
import java.util.regex.Pattern

object SyncLRCLyricsKaraokeParser {

    // Regex per il tempo della riga: [00:10.53]
    private val lineTimePattern = Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    // Regex per le parole
    private val wordPattern = Pattern.compile("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)")

    fun parse(lrcString: String?): List<LyricLine> {
        if (lrcString.isNullOrEmpty()) return emptyList()

        val lines = lrcString.lines()
        val parsedLines = mutableListOf<LyricLine>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val lineMatcher = lineTimePattern.matcher(line)
            if (!lineMatcher.find()) continue

            // PASSAGGIO CHIAVE: Passiamo la stringa grezza per gestire centesimi/millisecondi
            val lineStartMs = toMs(
                lineMatcher.group(1).toLong(),
                lineMatcher.group(2).toLong(),
                lineMatcher.group(3) // Passiamo la stringa, non il Long
            )

            val contentStart = lineMatcher.end()
            val content = line.substring(contentStart)

            val wordMatches = mutableListOf<WordMatch>()
            val wordMatcher = wordPattern.matcher(content)

            while (wordMatcher.find()) {
                val text = wordMatcher.group(4).trim()

                if (text.isNotEmpty()) {
                    wordMatches.add(
                        WordMatch(
                            absoluteTimeMs = toMs(
                                wordMatcher.group(1).toLong(),
                                wordMatcher.group(2).toLong(),
                                wordMatcher.group(3) // Stringa
                            ),
                            text = text
                        )
                    )
                }
            }

            if (wordMatches.isEmpty()) continue

            val lyricsWords = mutableListOf<LyricWord>()
            // Cerca il tempo della prossima riga, ma con un fallback sicuro
            val nextLineStartMs = findNextLineTime(lines, i + 1)

            // Se non c'è prossima riga, diamo un buffer di 3 secondi all'ultima parola
            val fallbackEndTime = lineStartMs + 3000

            for (j in wordMatches.indices) {
                val current = wordMatches[j]

                val endTimeMs = if (j + 1 < wordMatches.size) {
                    wordMatches[j + 1].absoluteTimeMs
                } else {
                    // Usa il tempo della prossima riga se valido, altrimenti il fallback
                    if (nextLineStartMs > current.absoluteTimeMs) nextLineStartMs else fallbackEndTime
                }

                val duration = endTimeMs - current.absoluteTimeMs

                if (duration > 0) {
                    val relativeStartTime = current.absoluteTimeMs - lineStartMs
                    lyricsWords.add(
                        LyricWord(
                            text = current.text,
                            startTimeInTheLineMs = relativeStartTime,
                            durationMs = duration
                        )
                    )
                }
            }
            Timber.d("LyricsKaraokeParser line index $i lineStartMs $lineStartMs")
            parsedLines.add(LyricLine(lineStartMs, "", lyricsWords))
        }
        return parsedLines
    }

    private fun findNextLineTime(lines: List<String>, startIndex: Int): Long {
        for (i in startIndex until lines.size) {
            val matcher = lineTimePattern.matcher(lines[i])
            if (matcher.find()) {
                return toMs(
                    matcher.group(1).toLong(),
                    matcher.group(2).toLong(),
                    matcher.group(3)
                )
            }
        }
        return -1 // Indica che non c'è una prossima riga
    }

    // Funzione corretta per gestire Centesimi (standard LRC) vs Millisecondi
    private fun toMs(min: Long, sec: Long, msStr: String): Long {
        val msValue = msStr.toLong()
        // Se ha 2 cifre è in centesimi di secondo (es. 04 -> 40ms)
        // Se ha 3 cifre è già in millisecondi (es. 040 -> 40ms o 400 -> 400ms)
        val actualMs = if (msStr.length == 2) msValue * 10 else msValue

        return (min * 60 * 1000) + (sec * 1000) + actualMs
    }

    private data class WordMatch(val absoluteTimeMs: Long, val text: String)
}