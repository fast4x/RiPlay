package it.fast4x.riplay.extensions.musicbrainz.utils

object IdentityUtils {

    /**
     * Normalizza un nome artista per matching fuzzy.
     * Rimuove case, spazi, punteggiatura, articoli.
     */
    fun normalizeArtistName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return name
            .lowercase()
            .trim()
            .replace(Regex("^(the|a|an)\\s+"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Normalizza un titolo album per matching.
     * Rimuove articoli, versioni (remaster, deluxe, edition), punteggiatura.
     */
    fun normalizeAlbumTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        return title
            .lowercase()
            .trim()
            .replace(Regex("^(the|a|an)\\s+"), "")
            .replace(Regex("\\s*\\(.*?(remaster|edition|version|deluxe|expanded|reissue).*?\\)"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Calcola un punteggio di similarità 0..1 tra due nomi.
     * Per ora semplice: 1.0 se uguali normalizzati, 0.0 altrimenti.
     * Futuro: Levenshtein distance per match fuzzy.
     */
    fun nameSimilarity(a: String?, b: String?): Float {
        val na = normalizeArtistName(a)
        val nb = normalizeArtistName(b)
        if (na.isEmpty() || nb.isEmpty()) return 0f
        return if (na == nb) 1f else 0f
    }
}