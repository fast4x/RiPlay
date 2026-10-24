package it.fast4x.riplay.player.kcef

/**
 * Utility per il riconoscimento dei video YouTube.
 */
object YouTubeUtils {

    private val BARE_ID = Regex("^[A-Za-z0-9_-]{11}$")

    private val PATTERNS = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),          // youtube.com/watch?v=ID
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),     // youtu.be/ID
        Regex("/shorts/([A-Za-z0-9_-]{11})"),        // youtube.com/shorts/ID
        Regex("/embed/([A-Za-z0-9_-]{11})"),         // youtube.com/embed/ID
        Regex("/live/([A-Za-z0-9_-]{11})")           // youtube.com/live/ID
    )

    /**
     * Estrae l'ID video (11 caratteri) da un URL YouTube in uno dei formati
     * comuni (watch, youtu.be, shorts, embed, live) oppure accetta l'ID nudo.
     */
    fun extractVideoId(input: String): String? {
        val s = input.trim()
        if (s.isEmpty()) return null
        if (BARE_ID.matches(s)) return s
        PATTERNS.forEach { p ->
            p.find(s)?.let { return it.groupValues[1] }
        }
        return null
    }

    fun isYouTubeUrl(s: String): Boolean =
        s.contains("youtube.com", ignoreCase = true) ||
                s.contains("youtu.be", ignoreCase = true)
}

/** Estensione comoda usata da [KcefYouTubePlayer]. */
fun extractYouTubeVideoId(input: String): String? = YouTubeUtils.extractVideoId(input)
