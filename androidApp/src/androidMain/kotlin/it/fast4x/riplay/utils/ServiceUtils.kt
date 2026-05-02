package it.fast4x.riplay.utils

import android.content.Context
import android.content.Intent
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
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.restartActivityKey
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.settings.SettingsDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun RestartPlayerService(
    restartService: Boolean = false,
    onRestart: () -> Unit
) {
    //val context = LocalContext.current
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
                    val intent = Intent(globalContext(), PlayerService::class.java)
                    globalContext().stopService(intent)
                    if (isAtLeastAndroid8)
                        globalContext().startForegroundService(intent)
                    else
                        globalContext().startService(intent)

                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                    }.invokeOnCompletion { onRestart() }

                    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext() )
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
    //var restartActivity by rememberPreference(restartActivityKey, false)
    val context = LocalContext.current
    AnimatedVisibility(visible = restart) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsDescription(
                text = stringResource(R.string.app_must_be_restarted_to_apply_changes),
                important = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_app_please),
                onClick = {
                    //restartActivity = !restartActivity
                    restartApp(context)
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

//fun restartApp(context: Context) {
//    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
//    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//    context.startActivity(intent)
//    Runtime.getRuntime().exit(0)
//}

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
    val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
    context.startActivity(restartIntent)
    Runtime.getRuntime().exit(0)
}

fun sendCommandToPlayerService(intent: Intent) {
    if (isAtLeastAndroid8)
        globalContext().startForegroundService(intent)
    else
        globalContext().startService(intent)
}