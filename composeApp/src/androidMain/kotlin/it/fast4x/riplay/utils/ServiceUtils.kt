package it.fast4x.riplay.utils

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.restartActivityKey
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.settings.SettingsDescription

@OptIn(UnstableApi::class)
@Composable
fun RestartPlayerService(
    restartService: Boolean = false,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    AnimatedVisibility(visible = restartService) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsDescription(
                text = stringResource(R.string.minimum_silence_length_warning),
                important = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_service),
                onClick = {
                    binder?.restartForegroundOrStop().let { onRestart() }
                    SmartMessage(context.resources.getString(R.string.done), context = context )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun RestartActivity(
    restart: Boolean = false,
    onRestart: () -> Unit
) {
    var restartActivity by rememberPreference(restartActivityKey, false)
    val context = LocalContext.current
    AnimatedVisibility(visible = restart) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsDescription(
                text = stringResource(R.string.minimum_silence_length_warning),
                important = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_service),
                onClick = {
                    restartActivity = !restartActivity
                    onRestart()
                    SmartMessage(context.resources.getString(R.string.done), context = context )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            )
        }
    }
}