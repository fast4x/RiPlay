package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class CrossfadeDuration {
    Off,
    `2s`,
    `3s`,
    `5s`;

    val milliseconds: Long get() =
        when (this) {
            CrossfadeDuration.Off -> 0L
            CrossfadeDuration.`2s` -> 2000L
            CrossfadeDuration.`3s` -> 3000L
            CrossfadeDuration.`5s` -> 5000L
        }

    val title: String @Composable
    get() =
        when(this) {
            CrossfadeDuration.Off -> stringResource(R.string.crossfade_duration_off_gapless)
            CrossfadeDuration.`2s` -> stringResource(R.string.crossfade_duration_2_seconds)
            CrossfadeDuration.`3s` -> stringResource(R.string.crossfade_duration_3_seconds)
            CrossfadeDuration.`5s` -> stringResource(R.string.crossfade_duration_5_seconds)
        }
}