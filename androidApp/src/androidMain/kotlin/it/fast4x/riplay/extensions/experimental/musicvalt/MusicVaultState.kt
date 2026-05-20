package it.fast4x.riplay.extensions.experimental.musicvalt

enum class MusicVaultState {
    NONE,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    FILE_MISSING
}