package it.fast4x.riplay.enums

import androidx.annotation.StringRes
import it.fast4x.riplay.R

enum class EqualizerPreset(@StringRes val labelRes: Int) {
    FLAT(R.string.equalizer_flat),
    BASS_BOOST(R.string.equalizer_bass_boost),
    ROCK(R.string.equalizer_rock),
    JAZZ(R.string.equalizer_jazz),
    VOCAL(R.string.equalizer_vocal),
    CLASSICAL(R.string.equalizer_classical),
    POP(R.string.equalizer_pop),
    ELECTRONIC(R.string.equalizer_electronic),
    DANCE(R.string.equalizer_dance),
    ACOUSTIC(R.string.equalizer_acoustic);

    companion object {
        fun fromString(name: String): EqualizerPreset {
            return entries.find { it.name == name } ?: FLAT
        }
    }
}
