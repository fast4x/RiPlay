package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class AudioVisualizerStyle {
    NONE,
    SPECTRUM,
    LEVEL,
    ANIMATIONS;

    val displayName: String
        @Composable
        get() = when (this) {
            NONE -> stringResource(R.string.audio_visualizer_style_none)
            SPECTRUM -> stringResource(R.string.audio_visualizer_style_spectrum)
            LEVEL -> stringResource(R.string.audio_visualizer_style_level)
            ANIMATIONS -> stringResource(R.string.audio_visualizer_style_animations)
        }
}