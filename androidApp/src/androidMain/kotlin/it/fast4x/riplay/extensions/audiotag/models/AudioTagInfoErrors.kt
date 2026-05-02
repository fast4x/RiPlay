package it.fast4x.riplay.extensions.audiotag.models

import androidx.compose.runtime.Composable

enum class AudioTagInfoErrors {
    NoCredit,
    NoAudioFound,
    AudioTooShort,
    InternalError,
    NoApiKey;

    val textName: String
    @Composable
    get() = when(this) {
        NoCredit -> "Not enough credit point to process the file"
        NoAudioFound -> "The file does not include audio track or format is not supported"
        AudioTooShort -> "Audio track duration is too short"
        InternalError -> "Can't process the file due to server unavailability, please try again later"
        NoApiKey -> "API key is not present, please add in settings"

    }

    companion object {
        fun getAudioTagInfoError(message: String): AudioTagInfoErrors =
            when (message) {
                "credit balance exhausted", "credit balance insufficient or exhausted" -> NoCredit
                "no audio found, format invalid or unsupported" -> NoAudioFound
                "audio duration is too short" -> AudioTooShort
                "internal error: could not communicate to the recognition server",
                "internal error: could not process the file" -> InternalError
                "API key is not present, please add in settings." -> NoApiKey
                else -> InternalError
            }
    }

}