package it.fast4x.riplay.extensions.experimental.recommendationstrategy.utils

import it.fast4x.riplay.data.models.Song
import kotlin.math.sqrt

object ScoringUtils {

    /**
     * Similarità Jaccard pesata tra le keyword del candidato e il vettore utente.
     * Restituisce 0..1.
     */
    fun keywordSimilarity(
        candidateKeywords: List<String>?,
        userVector: Map<String, Float>
    ): Float {
        if (candidateKeywords.isNullOrEmpty() || userVector.isEmpty()) return 0f

        val candidateSet = candidateKeywords.map { it.lowercase() }.toSet()
        val userSet = userVector.keys

        val intersectionWeight = candidateSet
            .intersect(userSet)
            .sumOf { (userVector[it] ?: 0f).toDouble() }
            .toFloat()

        val unionWeight = userVector.values.sum() +
                candidateSet.subtract(userSet).size.toFloat()

        return if (unionWeight > 0f) {
            (intersectionWeight / unionWeight).coerceIn(0f, 1f)
        } else 0f
    }

    /**
     * Bayesian average del rating MB, normalizzato su 0..1.
     */
    fun mbQualityBonus(
        rating: Float?,
        votes: Int?,
        priorM: Float = 10f,
        priorC: Float = 3.5f
    ): Float {
        val v = (votes ?: 0).toFloat()
        val R = rating ?: return 0f
        if (R <= 0f) return 0f
        val bayes = (v / (v + priorM)) * R + (priorM / (v + priorM)) * priorC
        return (bayes / 5f).coerceIn(0f, 1f)
    }

    /**
     * Penalità per brano già ampiamente ascoltato.
     * Usa Song.relativePlayTime() già esistente.
     */
    fun alreadyPlayedPenalty(song: Song): Float {
        val relative = song.relativePlayTime()
        return when {
            relative > 5f -> 1f
            relative > 2f -> 0.6f
            relative > 1f -> 0.3f
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    /**
     * Penalità di recency: brani ascoltati di recente pesano meno per la scoperta.
     */
    fun recencyPenalty(lastPlayedAt: Long?, now: Long): Float {
        val last = lastPlayedAt ?: return 0f
        val hoursAgo = (now - last) / 3_600_000L
        return when {
            hoursAgo < 24 -> 1f
            hoursAgo < 168 -> 0.5f
            hoursAgo < 720 -> 0.2f
            else -> 0f
        }
    }

    fun normalize(vector: Map<String, Float>): Map<String, Float> {
        val norm = sqrt(vector.values.sumOf { it * it.toDouble() }).toFloat().coerceAtLeast(1e-6f)
        return vector.mapValues { it.value / norm }
    }
}