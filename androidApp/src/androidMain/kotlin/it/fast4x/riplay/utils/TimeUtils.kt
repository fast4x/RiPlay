package it.fast4x.riplay.utils

/**
 * Converte una stringa di durata in secondo totale.
 * Supporta formati: "s", "m:ss", "h:mm:ss"
 * Se la stringa è null, vuota o malformata, ritorna 0 per fail-safe.
 */
fun String?.parseDurationToSeconds(): Int {
    if (this.isNullOrBlank()) return 0

    // I multipli per ogni unità di tempo partendo dal basso (secondi, minuti, ore)
    val multipliers = intArrayOf(1, 60, 3600)

    return this.trim()
        .split(":")
        .map { part -> part.toIntOrNull() ?: 0 } // Se un pezzo non è un numero, assumi 0
        .reversed() // Partiamo dai secondi (che sono l'ultimo elemento)
        .mapIndexed { index, value ->
            // Moltiplica il valore per il fattore corrispondente.
            // Se per qualche motivo ci sono più di 3 parti (es. giorni), il getOrElse ritorna 0
            value * multipliers.getOrElse(index) { 0 }
        }
        .sum()
}