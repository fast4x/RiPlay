package it.fast4x.riplay.enums

enum class RestoreMode {
    REPLACE,    // Cancella i dati attuali e carica quelli del backup
    MERGE       // Integra i dati del backup, sovrascrivendo solo se c'è conflitto sull'ID (da implementare in futuro...)
}