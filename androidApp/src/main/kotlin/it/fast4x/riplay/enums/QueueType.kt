package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class QueueType {
    Essential,
    Modern;

    val displayName: String
        @Composable
        get() = when (this) {
            QueueType.Modern -> stringResource(R.string.pcontrols_modern)
            QueueType.Essential -> stringResource(R.string.pcontrols_essential)
        }
}