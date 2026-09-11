package it.fast4x.riplay.extensions.webdavlibrary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavBackupInfo
import it.fast4x.riplay.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun WebDavDatabaseRestoreConfirmDialog(
    backupInfo: WebDavBackupInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Formattiamo la data in un formato leggibile (es. "06 Sep 2026 at 19:08")
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault())
    }
    val formattedDate = dateFormat.format(Date(backupInfo.timestamp))

    val deviceName = backupInfo.deviceName ?: stringResource(R.string.webdav_restore_dialog_unknown_device)
    val colorPalette = colorPalette()
    val typography = typography()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette.background1,
        title = {
            Text(
                text = stringResource(R.string.webdav_restore_dialog_title),
                style = typography.xl.copy(color = colorPalette.text)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.webdav_restore_dialog_body),
                    style = typography.m
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.webdav_restore_dialog_last_backup_label),
                    style = typography.xs,
                    color = colorPalette.text
                )
                Text(
                    text = formattedDate,
                    style = typography.s.copy(color = colorPalette.accent),
                )

                Text(
                    text = stringResource(R.string.webdav_restore_dialog_device_name, deviceName),
                    style = typography.s.copy(color = colorPalette.accent),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.webdav_restore_dialog_warning),
                    style = typography.xs.copy(color = colorPalette.red),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette.accent,
                    contentColor = colorPalette.text
                )
            ) {
                Text(stringResource(R.string.webdav_restore_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.webdav_restore_dialog_cancel),
                    color = colorPalette.text
                )
            }
        }
    )
}