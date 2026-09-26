package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class MessageType {
    Essential,
    Modern;

    val displayName: String
        @Composable
        get() = when (this) {
            Modern -> stringResource(R.string.message_type_modern)
            Essential -> stringResource(R.string.message_type_essential)
        }
}