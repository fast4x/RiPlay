package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class EventType {
    NewRelease,
    CheckUpdate,
    AutoBackup;

    val textName: String
        @Composable
        get() = when( this ) {
            NewRelease -> "New Release"
            CheckUpdate -> stringResource(R.string.check_update)
            AutoBackup -> "Auto Backup"
        }
}