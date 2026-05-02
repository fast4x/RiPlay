package it.fast4x.riplay.extensions.lyricshelper.parsers

import it.fast4x.riplay.extensions.lyricshelper.models.LyricLine
import it.fast4x.riplay.extensions.lyricshelper.models.LyricWord
import timber.log.Timber
import java.util.regex.Pattern


object SyncLRCLyricsKaraokeParser {

    private val lineTimePattern = Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
    private val wordPattern = Pattern.compile("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)")


    fun parse(lrcString: String?, isOnline: Boolean = false): List<LyricLine> {
        if (lrcString.isNullOrEmpty()) return emptyList()

        val lines = lrcString.lines()
        val parsedLines = mutableListOf<LyricLine>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val lineMatcher = lineTimePattern.matcher(line)
            if (!lineMatcher.find()) continue

            val lineStartMs = toMs(
                lineMatcher.group(1).toLong(),
                lineMatcher.group(2).toLong(),
                lineMatcher.group(3)
            )

            val contentStart = lineMatcher.end()
            val content = line.substring(contentStart)

            // Estrazione parole grezza (testo + timestamp originali)
            val rawWords = mutableListOf<RawWord>()
            val wordMatcher = wordPattern.matcher(content)
            while (wordMatcher.find()) {
                val text = wordMatcher.group(4).trim()
                if (text.isNotEmpty()) {
                    rawWords.add(
                        RawWord(
                            absoluteTimeMs = toMs(
                                wordMatcher.group(1).toLong(),
                                wordMatcher.group(2).toLong(),
                                wordMatcher.group(3)
                            ),
                            text = text
                        )
                    )
                }
            }

            if (rawWords.isEmpty()) continue

            // Trova inizio riga successiva
            val nextLineStartMs = findNextLineTime(lines, i + 1)
            // Calcola durata totale della riga corrente
            val lineDurationMs = if (nextLineStartMs > lineStartMs) {
                nextLineStartMs - lineStartMs
            } else {
                // Fallback se è l'ultima riga o formato strano: 5 secondi
                5000L
            }

            val lyricsWords = mutableListOf<LyricWord>()

            if (isOnline) {
                // --- LOGICA ONLINE: DURATA PROPORZIONALE ---

                // 1. Calcola il numero totale di caratteri nella riga
                val totalChars = rawWords.sumOf { it.text.length }

                if (totalChars > 0) {
                    var currentTimeOffset = 0L

                    for (raw in rawWords) {
                        // 2. Calcola la frazione di tempo basata sulla lunghezza della parola
                        // formula: (lunghezza parola / lunghezza totale) * durata totale riga
                        val wordDuration = (lineDurationMs * raw.text.length) / totalChars

                        lyricsWords.add(
                            LyricWord(
                                text = raw.text,
                                startTimeInTheLineMs = currentTimeOffset,
                                durationMs = wordDuration
                            )
                        )

                        // Accumula il tempo per la prossima parola
                        currentTimeOffset += wordDuration
                    }
                }

            } else {
                // --- LOGICA STANDARD: TIMESTAMP ORIGINALI ---

                for (j in rawWords.indices) {
                    val current = rawWords[j]
                    val endTimeMs = if (j + 1 < rawWords.size) {
                        rawWords[j + 1].absoluteTimeMs
                    } else {
                        // Per l'ultima parola, usa la fine della riga
                        lineStartMs + lineDurationMs
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
            }

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
        return -1
    }

    // Helper che gestisce centesimi (2 cifre) vs millisecondi (3 cifre)
    private fun toMs(min: Long, sec: Long, msStr: String): Long {
        val msValue = msStr.toLong()
        val actualMs = if (msStr.length == 2) msValue * 10 else msValue
        return (min * 60 * 1000) + (sec * 1000) + actualMs
    }

    // Classe helper interna
    private data class RawWord(
        val absoluteTimeMs: Long,
        val text: String
    )
}

