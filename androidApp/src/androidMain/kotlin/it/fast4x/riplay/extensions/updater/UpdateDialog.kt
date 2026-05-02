package it.fast4x.riplay.extensions.updater

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import timber.log.Timber

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel = viewModel(),
    onClose: () -> Unit,
) {
    val state by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    when {
        state.isChecking -> {
            AlertDialog(
                onDismissRequest = { },
                containerColor = colorPalette().background1,
                text = {
                    Column {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        )
                    }
                },
                confirmButton = {}
            )

        }

        state.updateAvailable -> {
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    containerColor = colorPalette().background1,
                    title = { Text(stringResource(R.string.check_update),
                        fontStyle = typography().m.fontStyle,
                        fontSize = typography().m.fontSize,
                        color = colorPalette().text) },
                    text = {
                        Column {
                            Text(stringResource(R.string.update_available),
                                fontStyle = typography().s.fontStyle,
                                fontSize = typography().s.fontSize,
                                color = colorPalette().text
                            )
                            if (state.versionName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    String.format(stringResource(R.string.app_update_dialog_new),state.versionName),
                                    fontStyle = typography().xs.fontStyle,
                                    fontSize = typography().xs.fontSize,
                                    color = colorPalette().text
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPalette().accent,
                                contentColor = colorPalette().background1
                            ),
                            onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (!context.packageManager.canRequestPackageInstalls()) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        "package:${context.packageName}".toUri()
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)

                                    return@Button
                                } else
                                    viewModel.downloadAndInstall()
                            } else
                                viewModel.downloadAndInstall()
                        }
                        ) {
                            Text(stringResource(R.string.update_download_and_install),
                                fontStyle = typography().xs.fontStyle,
                                fontSize = typography().xs.fontSize,
                                color = colorPalette().text)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                            showDialog = false
                            onClose()
                        }) {
                            Text(stringResource(R.string.update_later),
                                fontStyle = typography().xs.fontStyle,
                                fontSize = typography().xs.fontSize,
                                color = colorPalette().text
                            )
                        }
                    }
                )
            }
        }

        state.isDownloading -> {
            AlertDialog(
                onDismissRequest = { },
                containerColor = colorPalette().background1,
                title = { Text(stringResource(R.string.update_downloading),
                    fontStyle = typography().m.fontStyle,
                    fontSize = typography().m.fontSize,
                    color = colorPalette().text) },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = ProgressIndicatorDefaults.linearColor,
                            trackColor = ProgressIndicatorDefaults.linearTrackColor,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(state.downloadProgress * 100).toInt()}%")
                    }
                },
                confirmButton = {}
            )
        }

        state.errorMessage != null -> {
            Timber.e(state.errorMessage)
            /*
            AlertDialog(
                onDismissRequest = { },
                containerColor = colorPalette().background1,
                title = { Text(stringResource(R.string.update_error),
                    fontStyle = typography().m.fontStyle,
                    fontSize = typography().m.fontSize,
                    color = colorPalette().text) },
                text = { Text(state.errorMessage ?: stringResource( R.string.error_unknown)) },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("OK")
                    }
                }
            )
             */
        }
    }
}