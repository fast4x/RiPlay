package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class CheckUpdateState {
    Enabled,
    Disabled,
    OnlyCheck;
    //Ask

    val displayName: String
        @Composable
        get() = when (this) {
            CheckUpdateState.Disabled -> stringResource(R.string.vt_disabled)
            CheckUpdateState.Enabled -> stringResource(R.string.enabled)
            CheckUpdateState.OnlyCheck -> stringResource(R.string.only_check_update)
            //CheckUpdateState.Ask -> stringResource(R.string.ask)
        }
}