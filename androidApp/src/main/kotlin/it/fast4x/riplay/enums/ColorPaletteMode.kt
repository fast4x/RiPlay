package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ColorPaletteMode {
    Light,
    Dark,
    PitchBlack,
    System;

    val displayName: String
        @Composable
        get() = when (this) {
            ColorPaletteMode.Dark -> stringResource(R.string.dark)
            ColorPaletteMode.Light -> stringResource(R.string._light)
            ColorPaletteMode.System -> stringResource(R.string.system)
            ColorPaletteMode.PitchBlack -> stringResource(R.string.theme_mode_pitch_black)
        }
}
