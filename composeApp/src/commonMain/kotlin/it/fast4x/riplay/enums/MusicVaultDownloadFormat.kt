package it.fast4x.riplay.enums

import kotlinx.serialization.Serializable

/**
 * Formato richiesto alla app esterna di download (plugin Music Vault).
 *
 *  - Audio: bestaudio/best, identico alla logica storica di MusicVault.py
 *  - Video: best pre-mergiato (best[ext=mp4]/best): yt-dlp in-process non
 *    dispone di ffmpeg, quindi si usa un formato progressive gia' unito
 */
@Serializable
enum class MusicVaultDownloadFormat(val value: String) {
    Audio("audio"),
    Video("video");

    companion object {
        fun fromValue(value: String?): MusicVaultDownloadFormat =
            entries.firstOrNull { it.value == value } ?: Audio
    }
}
