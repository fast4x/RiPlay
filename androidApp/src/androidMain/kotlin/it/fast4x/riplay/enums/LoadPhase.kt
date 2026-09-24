package it.fast4x.riplay.enums

enum class LoadPhase {
    NONE,       // Caricamento non in corso
    PENDING,    // Caricamento in corso
    STALE;       // Caricamento mai avviato

    val isPending: Boolean
        get() = this == PENDING
}