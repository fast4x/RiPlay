package it.fast4x.riplay.extensions.lyricshelper.parsers

enum class LyricsType {
    PLAIN,       // Testo semplice, nessun timestamp
    LINE_SYNCED, // [mm:ss.xx] Una riga per volta (formato LRC standard)
    WORD_SYNCED  // <mm:ss.xx> Parola per parola (formato Enhanced LRC / LRC+)
}

data class LyricsRaw(
    val type: LyricsType,
    val rawContent: String,           // Contenuto grezzo originale
    val plainText: String,            // Solo testo, senza timestamp
    val lines: List<SyncedLine> = emptyList() // Popolato se LINE_SYNCED o WORD_SYNCED
)

data class SyncedLine(
    val timestampMs: Long,            // Inizio riga in millisecondi
    val text: String,                 // Testo della riga completo
    val words: List<SyncedWord> = emptyList() // Popolato solo se WORD_SYNCED
)

data class SyncedWord(
    val timestampMs: Long,
    val word: String
)

object LyricsParser {

    // [mm:ss.xx] oppure [mm:ss:xx]
    private val LINE_TIMESTAMP = Regex("""^\[(\d{2}):(\d{2})[.:](\d{2,3})\](.*)$""")

    // <mm:ss.xx> parola per parola
    private val WORD_TIMESTAMP = Regex("""<(\d{2}):(\d{2})[.:](\d{2,3})>([^<]*)""")

    fun parse(raw: String): LyricsRaw {
        val type = detect(raw)
        return when (type) {
            LyricsType.PLAIN       -> LyricsRaw(type, raw, raw.trim())
            LyricsType.LINE_SYNCED -> parseLineSynced(raw, type)
            LyricsType.WORD_SYNCED -> parseWordSynced(raw)
        }
    }

    // ── Rilevamento tipo ────────────────────────────────────────────────

    fun detect(raw: String): LyricsType {
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return LyricsType.PLAIN

        val hasWordSync  = lines.any { WORD_TIMESTAMP.containsMatchIn(it) }
        if (hasWordSync) return LyricsType.WORD_SYNCED

        val hasLineSync  = lines.any { LINE_TIMESTAMP.containsMatchIn(it) }
        if (hasLineSync) return LyricsType.LINE_SYNCED

        return LyricsType.PLAIN
    }

    // ── LINE_SYNCED ─────────────────────────────────────────────────────

    private fun parseLineSynced(raw: String, type: LyricsType): LyricsRaw {
        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines  = mutableListOf<String>()

        raw.lines().forEach { line ->
            val match = LINE_TIMESTAMP.find(line.trim())
            if (match != null) {
                val (mm, ss, cs, text) = match.destructured
                val ms = toMs(mm, ss, cs)
                val trimmed = text.trim()
                syncedLines.add(SyncedLine(ms, trimmed))
                if (trimmed.isNotBlank()) plainLines.add(trimmed)
            } else if (line.isNotBlank()) {
                plainLines.add(line.trim())
            }
        }

        syncedLines.sortBy { it.timestampMs }

        return LyricsRaw(
            type        = type,
            rawContent  = raw,
            plainText   = plainLines.joinToString("\n"),
            lines       = syncedLines
        )
    }

    // ── WORD_SYNCED ─────────────────────────────────────────────────────

    private fun parseWordSynced(raw: String): LyricsRaw {
        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines  = mutableListOf<String>()

        raw.lines().forEach { line ->
            // Timestamp di riga (opzionale nell'Enhanced LRC)
            val lineTs = LINE_TIMESTAMP.find(line.trim())
            val lineStartMs = lineTs?.let {
                val (mm, ss, cs, _) = it.destructured
                toMs(mm, ss, cs)
            } ?: 0L

            val words = mutableListOf<SyncedWord>()
            WORD_TIMESTAMP.findAll(line).forEach { match ->
                val (mm, ss, cs, word) = match.destructured
                val ms = toMs(mm, ss, cs)
                val trimmed = word.trim()
                if (trimmed.isNotBlank()) words.add(SyncedWord(ms, trimmed))
            }

            if (words.isNotEmpty()) {
                val lineText = words.joinToString(" ") { it.word }
                syncedLines.add(SyncedLine(lineStartMs, lineText, words))
                plainLines.add(lineText)
            }
        }

        syncedLines.sortBy { it.timestampMs }

        return LyricsRaw(
            type       = LyricsType.WORD_SYNCED,
            rawContent = raw,
            plainText  = plainLines.joinToString("\n"),
            lines      = syncedLines
        )
    }

    // ── Utility ─────────────────────────────────────────────────────────

    /**
     * Converte mm, ss, centesimi/millisecondi in millisecondi totali.
     * Gestisce sia 2 cifre (centesimi) che 3 cifre (millisecondi).
     */
    private fun toMs(mm: String, ss: String, cs: String): Long {
        val msFromCs = if (cs.length == 3) cs.toLong() else cs.toLong() * 10
        return mm.toLong() * 60_000 + ss.toLong() * 1_000 + msFromCs
    }
}