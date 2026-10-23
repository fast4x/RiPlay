package it.fast4x.riplay.data.models

import java.time.LocalDate

/**
 * Classe helper per la raggruppamento della cronologia per data.
 * Ripristinata in androidApp: usa java.time.LocalDate (tipico Android-only),
 * mentre EventWithSong è migrata in commonMain senza questa parte.
 */
sealed class DateAgo {
    object Today : DateAgo()
    object Yesterday : DateAgo()
    object ThisWeek : DateAgo()
    object LastWeek : DateAgo()
    class Other(val date: LocalDate) : DateAgo() {
        override fun equals(other: Any?): Boolean {
            if (other is Other) return date == other.date
            return super.equals(other)
        }

        override fun hashCode(): Int = date.hashCode()
    }
}
