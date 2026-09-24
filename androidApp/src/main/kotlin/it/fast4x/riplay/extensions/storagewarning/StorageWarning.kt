package it.fast4x.riplay.extensions.storagewarning

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun StorageWarningChecker() {
    var storageInfo by remember { mutableStateOf<StorageUtils.StorageInfo?>(null) }

    LaunchedEffect(Unit) {
        val info = withContext(Dispatchers.IO) {
            StorageUtils.getStorageInfo()
        }
        if (info.status != StorageUtils.StorageStatus.OK) {
            storageInfo = info
        }
    }

    storageInfo?.let { info ->
        StorageWarningDialog(
            storageInfo = info,
            onDismiss = { storageInfo = null }
        )
    }
}

@Composable
fun StorageWarningDialog(
    storageInfo: StorageUtils.StorageInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val isCritical = storageInfo.status == StorageUtils.StorageStatus.CRITICAL

    val title = if (isCritical) stringResource(R.string.storage_almost_full) else stringResource(R.string.storage_low_storage)
    val icon = if (isCritical) R.drawable.alert_circle_not_filled else R.drawable.alert
    val iconColor = if (isCritical) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary

    val freeLabel = if (storageInfo.freeMB < 1024)
        stringResource(R.string.storage_mb_free, storageInfo.freeMB)
    else
        stringResource(R.string.storage_gb_free, "%.1f".format(storageInfo.freeGB))

    AlertDialog(
        modifier = Modifier.padding(horizontal = 24.dp),
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isCritical)
                        stringResource(R.string.storage_your_device_is_running_out_of_storage_the_app_may_not_work_properly)
                    else
                        stringResource(R.string.storage_your_device_is_low_on_storage_consider_freeing_up_some_space_soon),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Barra spazio
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = freeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = iconColor
                        )
                        Text(
                            text = stringResource(
                                R.string.storage_gb_total,
                                "%.0f".format(storageInfo.totalGB)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { storageInfo.usedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = iconColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.storage_warning_ignore))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor
                )
            ) {
                Text(stringResource(R.string.storage_manage_storage))
            }
        }
    )
}