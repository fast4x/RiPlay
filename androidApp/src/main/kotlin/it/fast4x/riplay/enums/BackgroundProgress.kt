package it.fast4x.riplay.enums;

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class BackgroundProgress {
    Player,
    MiniPlayer,
    Both,
    Disabled;

    val displayName: String
        @Composable
        get() = when (this) {
            BackgroundProgress.Player -> stringResource(R.string.player)
            BackgroundProgress.MiniPlayer -> stringResource(R.string.minimized_player)
            BackgroundProgress.Both -> stringResource(R.string.both)
            BackgroundProgress.Disabled -> stringResource(R.string.vt_disabled)
        }
}
