package it.fast4x.riplay.enums

enum class PlaybackOrigin {
    SUGGESTION,          // dai suggerimenti dell'algoritmo
    SEARCH,              // ricerca manuale utente
    LIBRARY,             // dalla libreria/playlist utente
    RELATED,             // click su un "brano simile"
    EXTERNAL,            // intent esterno / auto-next
    UNKNOWN
}