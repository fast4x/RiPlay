package it.fast4x.riplay.musicvault

enum class MusicVaultState {
    NONE,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    FILE_MISSING
}