package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PlayerControlsType {
    Essential,
    Modern;

    val displayName: String
        @Composable
        get() = when (this) {
            PlayerControlsType.Modern -> stringResource(R.string.pcontrols_modern)
            PlayerControlsType.Essential -> stringResource(R.string.pcontrols_essential)
        }
}