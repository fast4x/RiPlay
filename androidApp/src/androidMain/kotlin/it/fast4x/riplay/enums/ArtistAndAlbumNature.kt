package it.fast4x.riplay.enums

enum class ArtistNature {
    HUMAN,           // Artista umano tradizionale
    AI_GENERATED,    // Generato da AI (Suno, Udio, etc.)
    VIRTUAL,         // Vocaloid/VTuber (Hatsune Miku, Gorillaz)
    COMPILATION,     // "Various Artists", raccolte
    COVER_TRIBUTE,   // Band tributo / cover band
    UNKNOWN          // Non classificato (default)
}

enum class AlbumNature {
    STUDIO_ALBUM,
    LIVE,
    COMPILATION,
    SOUNDTRACK,
    SINGLE,
    EP,
    AI_GENERATED,
    UNKNOWN
}