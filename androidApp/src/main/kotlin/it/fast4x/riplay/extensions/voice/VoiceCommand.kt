package it.fast4x.riplay.extensions.voice

/**
 * A playback action understood from a spoken sentence.
 *
 * The parser never touches Android or the player: it only turns text into one of
 * these commands, which makes it unit testable on the JVM.
 */
sealed interface VoiceCommand {
    data class PlaySong(val query: String) : VoiceCommand

    data class PlayPlaylist(val name: String) : VoiceCommand

    data object Pause : VoiceCommand

    data object Resume : VoiceCommand

    data object Next : VoiceCommand

    data object Previous : VoiceCommand

    data object Replay : VoiceCommand

    data object LoopSong : VoiceCommand

    data object LoopQueue : VoiceCommand

    data object LoopOff : VoiceCommand

    data object Shuffle : VoiceCommand

    data object Unknown : VoiceCommand
}

/**
 * Turns a recognized utterance such as "play faded" or "play songs from my gym
 * playlist" into a [VoiceCommand].
 */
object VoiceCommandParser {

    private val wakeWord = Regex("^(?:hey |ok |okay |yo )?(?:riplay|ri play|r i play|ryplay)(?: |$)")

    private val politeness = Regex(
        "^(?:please|could you|can you|would you|will you|i want you to|i would like to|i want to|i wanna|lets|let us|just)(?: |$)"
    )

    private val playlistFrom = Regex(
        "^(?:play|put on|start|shuffle) (?:the )?(?:(?:songs|tracks|music|everything) )?(?:from|in|on) (?:the |my )?(.+?) (?:playlist|list)$"
    )

    private val playlistDirect = Regex(
        "^(?:play|put on|start|shuffle) (?:the |my |our )?(.+?) playlist$"
    )

    private val playlistKeyword = Regex(
        "^(?:play|put on|start|shuffle) playlist (?:called |named )?(.+)$"
    )

    private val loopOff = listOf(
        Regex("^(?:stop|turn off|disable|cancel|kill|remove) (?:the )?(?:loop|looping|repeat|repeating|repeat mode|loop mode)$"),
        Regex("^(?:no loop|loop off|repeat off|stop the loop|dont loop|do not loop)$")
    )

    private val loopSong = listOf(
        Regex("^(?:loop|repeat) (?:the |this |current |one )?(?:song|track|one|it|tune|this)$"),
        Regex("^(?:set )?(?:repeat|loop) (?:mode )?(?:to )?one$")
    )

    private val loopQueue = listOf(
        Regex("^(?:loop|repeat)(?: (?:the |this |whole |entire )?(?:queue|playlist|list|everything|songs|all))?$"),
        Regex("^(?:loop|repeat) all(?: songs| tracks)?$"),
        Regex("^(?:turn on|enable|start) (?:the )?loop(?: mode)?$")
    )

    private val replay = Regex(
        "^(?:replay|replay (?:the |this )?(?:song|track|it)|play (?:it|this song|the song|this track|that song) again|play again|restart(?: the song| this song| it| the track)?|start over|from the beginning|back to the beginning)$"
    )

    private val shuffle = listOf(
        Regex("^(?:shuffle|randomize|randomise|mix it up|mix up)(?: (?:the |this |my )?(?:song|songs|queue|playlist|music|list|tracks|everything))?$"),
        Regex("^(?:turn on|enable|start) (?:the )?shuffle(?: mode)?$")
    )

    private val pause = Regex(
        "^(?:pause|stop|stop playing|stop the music|stop music|hold on|hold)(?: (?:the )?(?:music|song|playback|playing|it))?$"
    )

    private val resume = Regex(
        "^(?:resume|resume playing|resume playback|continue|continue playing|unpause|keep playing|carry on)(?: (?:the )?(?:song|music|playback|playing|it))?$"
    )

    private val next = Regex(
        "^(?:next|next song|next track|skip|skip song|skip track|skip this(?: song| track)?|skip to (?:the )?next(?: song| track)?|play (?:the )?next(?: song| track)?|go to (?:the )?next(?: song| track)?)$"
    )

    private val previous = Regex(
        "^(?:previous|previous song|previous track|last song|last track|go back|back|play (?:the )?previous(?: song| track)?|go to (?:the )?previous(?: song| track)?)$"
    )

    private val playAlone = Regex(
        "^(?:play|put on|start) (?:some |the |any )?(?:music|songs|something)$|^(?:play|start|put on)$"
    )

    private val playPrefix = Regex(
        "^(?:play|put on|start|listen to) (.+)$"
    )

    fun parse(rawInput: String): VoiceCommand {
        val text = stripPoliteness(stripWakeWord(normalize(rawInput)))
        if (text.isEmpty()) return VoiceCommand.Unknown

        classifyPlaylist(text)?.let { return it }

        if (loopOff.any { it.matches(text) }) return VoiceCommand.LoopOff
        if (loopSong.any { it.matches(text) }) return VoiceCommand.LoopSong
        if (loopQueue.any { it.matches(text) }) return VoiceCommand.LoopQueue
        if (replay.matches(text)) return VoiceCommand.Replay
        if (shuffle.any { it.matches(text) }) return VoiceCommand.Shuffle
        if (pause.matches(text)) return VoiceCommand.Pause
        if (resume.matches(text)) return VoiceCommand.Resume
        if (next.matches(text)) return VoiceCommand.Next
        if (previous.matches(text)) return VoiceCommand.Previous
        if (playAlone.matches(text)) return VoiceCommand.Resume

        val playQuery = playPrefix.find(text)?.groupValues?.getOrNull(1)
        val query = playQuery?.let(::cleanSongQuery)
        if (!query.isNullOrEmpty()) return VoiceCommand.PlaySong(query)

        return VoiceCommand.Unknown
    }

    private fun classifyPlaylist(text: String): VoiceCommand.PlayPlaylist? {
        val name = (playlistFrom.find(text)?.groupValues?.getOrNull(1)
            ?: playlistDirect.find(text)?.groupValues?.getOrNull(1)
            ?: playlistKeyword.find(text)?.groupValues?.getOrNull(1))
            ?.trim()
            ?.trim('\'', '"')
            ?.trim()
        return if (name.isNullOrEmpty()) null else VoiceCommand.PlayPlaylist(name)
    }

    private fun cleanSongQuery(raw: String): String {
        var query = raw.trim()
        while (true) {
            val stripped = query
                .removePrefix("the ")
                .removePrefix("a ")
                .removePrefix("an ")
                .removePrefix("some ")
                .removePrefix("song called ")
                .removePrefix("track called ")
                .removePrefix("song named ")
                .removePrefix("track named ")
                .removePrefix("called ")
                .removePrefix("named ")
            query = if (stripped == query) break else stripped.trim()
        }
        // "play the song faded" -> "faded", but keep titles such as "Song 2".
        val songWord = Regex("^(?:song|track) (.+)$").find(query)
        if (songWord != null) {
            val rest = songWord.groupValues[1].trim()
            if (rest.isNotEmpty() && !rest.first().isDigit()) query = rest
        }
        return query.removeSuffix(" please").trim()
    }

    private fun normalize(input: String): String =
        input
            .lowercase()
            .replace('\u2019', '\'')
            .replace("'", "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun stripWakeWord(text: String): String = text.replaceFirst(wakeWord, "").trim()

    private fun stripPoliteness(text: String): String {
        var result = text
        while (politeness.containsMatchIn(result)) {
            result = result.replaceFirst(politeness, "").trim()
        }
        return result
    }
}

/** Address book for spoken names: tolerates articles, punctuation and misrecognitions. */
object VoiceNameMatcher {

    private const val MIN_SCORE = 0.62

    fun bestMatch(spoken: String, candidates: List<String>): String? {
        val index = bestMatchIndex(spoken, candidates) ?: return null
        return candidates[index]
    }

    fun bestMatchIndex(spoken: String, candidates: List<String>): Int? {
        if (candidates.isEmpty()) return null
        val target = normalizeName(spoken)
        if (target.isEmpty()) return null
        var bestIndex: Int? = null
        var bestScore = 0.0
        candidates.forEachIndexed { index, candidate ->
            val score = score(target, candidate)
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex?.takeIf { bestScore >= MIN_SCORE }
    }

    fun score(spoken: String, candidate: String): Double {
        val a = normalizeName(spoken)
        val b = normalizeName(candidate)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (a.replace(" ", "") == b.replace(" ", "")) return 0.97
        if (b.contains(a)) return 0.9
        if (a.contains(b)) return 0.85
        val distance = levenshtein(a, b)
        return 1.0 - distance.toDouble() / maxOf(a.length, b.length).toDouble()
    }

    private fun normalizeName(value: String): String =
        value
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + substitution
                )
            }
            for (j in 0..b.length) previous[j] = current[j]
        }
        return previous[b.length]
    }
}
