package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class MiniPlayerType {
    Essential,
    Modern;

    val displayName: String
        @Composable
        get() = when (this) {
            MiniPlayerType.Modern -> stringResource(R.string.pcontrols_modern)
            MiniPlayerType.Essential -> stringResource(R.string.pcontrols_essential)
        }
}