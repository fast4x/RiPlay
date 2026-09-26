package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PlayerPosition {
    Top,
    Bottom;

    val displayName: String
        @Composable
        get() = when (this) {
            PlayerPosition.Top -> stringResource(R.string.position_top)
            PlayerPosition.Bottom -> stringResource(R.string.position_bottom)
        }
}