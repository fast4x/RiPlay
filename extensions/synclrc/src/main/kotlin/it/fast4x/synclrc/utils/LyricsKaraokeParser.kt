package it.fast4x.synclrc.utils

import it.fast4x.synclrc.models.LyricLine
import it.fast4x.synclrc.models.LyricWord
import java.util.regex.Pattern

object LyricsKaraokeParser {

    // Regex per il tempo della riga: [00:10.53]
    private val lineTimePattern = Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    // Regex per le parole: <00:10.53>Hello
    // Cattura il tempo e il testo fino al prossimo tag <
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

            // 1. Ottieni tempo assoluto di inizio riga
            val lineStartMs = toMs(
                lineMatcher.group(1).toLong(),
                lineMatcher.group(2).toLong(),
                lineMatcher.group(3).toLong()
            )

            // 2. Estrai il contenuto dopo il timestamp della riga
            val contentStart = lineMatcher.end()
            val content = line.substring(contentStart)

            // 3. Trova tutte le parole e i loro tempi assoluti
            val wordMatches = mutableListOf<WordMatch>()
            val wordMatcher = wordPattern.matcher(content)

            while (wordMatcher.find()) {
                val wMin = wordMatcher.group(1).toLong()
                val wSec = wordMatcher.group(2).toLong()
                val wMs = wordMatcher.group(3).toLong()
                val text = wordMatcher.group(4).trim()

                // Ignoriamo i match vuoti (pause) o spazi bianchi
                if (text.isNotEmpty()) {
                    wordMatches.add(
                        WordMatch(
                            absoluteTimeMs = toMs(wMin, wSec, wMs),
                            text = text
                        )
                    )
                }
            }

            // Se non ci sono parole valide, salta la riga
            if (wordMatches.isEmpty()) continue

            // 4. Calcola le durate convertendo i tempi assoluti in relativi
            val lyricsWords = mutableListOf<LyricWord>()

            // Trova il tempo di inizio della PROSSIMA riga per calcolare la durata dell'ultima parola
            val nextLineStartMs = findNextLineTime(lines, i + 1)

            for (j in wordMatches.indices) {
                val current = wordMatches[j]

                // Calcola quando finisce questa parola
                val endTimeMs = if (j + 1 < wordMatches.size) {
                    // Finisce quando inizia la prossima parola
                    wordMatches[j + 1].absoluteTimeMs
                } else {
                    // Se è l'ultima parola, finisce quando inizia la prossima riga
                    nextLineStartMs
                }

                val duration = endTimeMs - current.absoluteTimeMs

                // Evitiamo durate negative o zero dovute a glitch dell'API
                if (duration > 0) {
                    // Convertiamo il tempo assoluto della parola in tempo RELATIVO alla riga
                    val relativeStartTime = current.absoluteTimeMs - lineStartMs

                    lyricsWords.add(
                        LyricWord(
                            text = current.text,
                            startTimeLineMs = relativeStartTime,
                            durationMs = duration
                        )
                    )
                }
            }

            parsedLines.add(LyricLine(lineStartMs, "", lyricsWords))
        }
        return parsedLines
    }

    // Helper per trovare l'inizio della riga successiva
    private fun findNextLineTime(lines: List<String>, startIndex: Int): Long {
        for (i in startIndex until lines.size) {
            val matcher = lineTimePattern.matcher(lines[i])
            if (matcher.find()) {
                return toMs(
                    matcher.group(1).toLong(),
                    matcher.group(2).toLong(),
                    matcher.group(3).toLong()
                )
            }
        }
        // Se non c'è una prossima riga, diamo un buffer di 3 secondi o MAX_VALUE
        return Long.MAX_VALUE
    }

    // Helper per conversione tempo -> millisecondi
    private fun toMs(min: Long, sec: Long, ms: Long): Long {
        return (min * 60 * 1000) + (sec * 1000) + ms
    }

    // Data class temporanea per il parsing
    private data class WordMatch(
        val absoluteTimeMs: Long,
        val text: String
    )
}