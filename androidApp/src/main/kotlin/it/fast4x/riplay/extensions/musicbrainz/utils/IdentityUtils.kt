package it.fast4x.riplay.extensions.musicbrainz.utils

/**
 * Utilità di identità per il match verso MusicBrainz.
 */
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
     * FIX (Fase 2): prima era binario (1.0 se uguali, 0 altrimenti — il
     * commento "futuro: Levenshtein" non è mai stato implementato) e quindi
     * INUTILE per la disambiguazione: tutti i candidati con nome diverso
     * valdevano 0 e si sceglieva solo per score MB.
     *
     * Ora: 1.0 se normalizzati identici, altrimenti Jaro-Winkler (algoritmo
     * già collaudato nel modulo :zoundhub — un processo usa un solo standard
     * di similarità).
     */
    fun nameSimilarity(a: String?, b: String?): Float {
        val na = normalizeArtistName(a)
        val nb = normalizeArtistName(b)
        if (na.isEmpty() || nb.isEmpty()) return 0f
        if (na == nb) return 1f
        return it.fast4x.zoundhub.ZoundHubMatcher.jaroWinkler(na, nb).toFloat()
    }

    /**
     * Soglia minima di similarità per accettare un match artista->MBID.
     * Sotto questa soglia NON si persiste il mbId: meglio un artista senza
     * mbId (riproponibile) che il mbId sbagliato (permanente e fuorviante).
     */
    const val MIN_NAME_SIMILARITY = 0.85f

    /**
     * Scoring di disambiguazione per i candidati di una ricerca MB (Fase 2).
     *
     * Combina:
     *  - score di ricerca MB (0..100 -> 0..1): rilevanza del motore interno;
     *  - similarità del nome (Jaro-Winkler sui nomi normalizzati);
     *  - bonus di coerenza tra tipo/country dell'artista locale e del candidato
     *    (quando entrambi noti).
     *
     * Il punteggio MB da solo non basta: per nomi generici le prime hits sono
     * quasi sempre >= 90 indipendentemente dalla correttezza.
     *
     * @param localName     nome artista locale (RiPlay)
     * @param localType     tipo noto localmente ("Group", "Person"...) o null
     * @param localCountry  country code noto localmente ("IT", "US"...) o null
     * @param candidates    risultati della ricerca MB già filtrati per score
     * @param extractor     estrae i dati di disambiguazione dal candidato
     */
    fun <T> pickBestCandidate(
        localName: String,
        localType: String?,
        localCountry: String?,
        candidates: List<T>,
        extractor: (T) -> CandidateInfo
    ): ScoredCandidate<T>? {
        val normalizedLocal = normalizeArtistName(localName)
        if (normalizedLocal.isEmpty() || candidates.isEmpty()) return null

        var best: ScoredCandidate<T>? = null

        for (candidate in candidates) {
            val info = extractor(candidate)
            val similarity = nameSimilarity(normalizedLocal, info.name)

            // Guardia anti-artista-sbagliato: se il nome non somiglia abbastanza
            // il candidato viene scartato a prescindere dallo score MB.
            if (similarity < MIN_NAME_SIMILARITY) continue

            val mbScore = (info.mbSearchScore.coerceIn(0, 100)) / 100f
            var score = 0.6f * similarity + 0.4f * mbScore

            // Coerenza di tipo (Group/Person): piccolo bonus/malus
            if (localType != null && info.type != null) {
                score += if (localType.equals(info.type, ignoreCase = true)) 0.05f else -0.05f
            }
            // Coerenza di paese: piccolo bonus/malus
            if (localCountry != null && info.country != null) {
                score += if (localCountry.equals(info.country, ignoreCase = true)) 0.05f else -0.05f
            }

            score = score.coerceIn(0f, 1f)

            if (best == null || score > best.score) {
                best = ScoredCandidate(candidate, score, similarity)
            }
        }

        return best
    }

    /** Informazioni di disambiguazione estratte da un candidato MB. */
    data class CandidateInfo(
        val name: String,
        val mbSearchScore: Int,
        val type: String?,
        val country: String?
    )

    /** Candidato vincente col punteggio calcolato. */
    data class ScoredCandidate<T>(
        val candidate: T,
        val score: Float,
        val nameSimilarity: Float
    )
}
