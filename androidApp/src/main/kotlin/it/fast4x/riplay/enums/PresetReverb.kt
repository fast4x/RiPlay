package it.fast4x.riplay.enums

import android.media.audiofx.PresetReverb
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PresetsReverb {
    NONE,
    SMALLROOM,
    MEDIUMROOM,
    LARGEROOM,
    MEDIUMHALL,
    LARGEHALL,
    PLATE;

    val preset: Short
        get() = when (this) {
            NONE -> PresetReverb.PRESET_NONE
            SMALLROOM -> PresetReverb.PRESET_SMALLROOM
            MEDIUMROOM -> PresetReverb.PRESET_MEDIUMROOM
            LARGEROOM -> PresetReverb.PRESET_LARGEROOM
            MEDIUMHALL -> PresetReverb.PRESET_MEDIUMHALL
            LARGEHALL -> PresetReverb.PRESET_LARGEHALL
            PLATE -> PresetReverb.PRESET_PLATE
        }

    val textName: String
        @Composable
        get() = when (this) {
            NONE -> stringResource(R.string.audio_preset_none)
            SMALLROOM -> stringResource(R.string.audio_preset_small_room)
            MEDIUMROOM -> stringResource(R.string.audio_preset_medium_room)
            LARGEROOM -> stringResource(R.string.audio_preset_large_room)
            MEDIUMHALL -> stringResource(R.string.audio_preset_medium_hall)
            LARGEHALL -> stringResource(R.string.audio_preset_large_hall)
            PLATE -> stringResource(R.string.audio_preset_plate)
        }



}