package it.fast4x.riplay.ui.screens.events

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.EventType
import it.fast4x.riplay.extensions.preferences.autoBackupFolderKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.scheduled.periodicAutoBackup
import it.fast4x.riplay.extensions.scheduled.periodicCheckNewFromArtists
import it.fast4x.riplay.extensions.scheduled.periodicCheckUpdate
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.formatTimeRemaining
import it.fast4x.riplay.utils.getWorkStatusFlow
import it.fast4x.riplay.utils.isWorkScheduled
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.BuildConfig


const val workNameNewRelease = "weeklyOrDailyCheckNewFromArtistsWork"
const val workNameCheckUpdate = "weeklyOrDailyCheckUpdateWork"
const val workNameAutoBackup = "weeklyOrDailyAutoBackupWork"



@Composable
fun EventsScreen() {
    val context = LocalContext.current

    val buttonsList = mutableListOf(
        EventType.NewArtistsRelease to EventType.NewArtistsRelease.textName,
        //EventType.AutoBackup to EventType.AutoBackup.textName
    ).apply {
        if(BuildConfig.BUILD_VARIANT == "full") // This is in the gradle file
            add(EventType.CheckUpdate to EventType.CheckUpdate.textName)
    }
    var eventType by remember { mutableStateOf(EventType.NewArtistsRelease) }


    val workInfoNewRelease by context.getWorkStatusFlow(workNameNewRelease).collectAsState(initial = null)
    val workInfoCheckUpdate by context.getWorkStatusFlow(workNameCheckUpdate).collectAsState(initial = null)
    val workInfoAutoBackup by context.getWorkStatusFlow(workNameAutoBackup).collectAsState(initial = null)


    var weeklyOrDaily by remember { mutableStateOf(true) }



    Column(
        modifier = Modifier
            .fillMaxHeight(.6f)
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                //.padding(horizontal = 4.dp)
                .padding(vertical = 4.dp)
                //.padding(bottom = 8.dp)
                .fillMaxWidth()
        ) {
            Box {
                ButtonsRow(
                    buttons = buttonsList,
                    currentValue = eventType,
                    onValueUpdate = { eventType = it },
                    modifier = Modifier.padding(end = 12.dp)
                )

            }
        }

        if (eventType == EventType.NewArtistsRelease) {
            val isScheduled = isWorkScheduled(workInfoNewRelease)
            val statusText = eventStatus(workInfoNewRelease)

            Text(
                text = stringResource(R.string.event_notification_for_new_release),
                style = typography().m
            )
            Text(
                text = stringResource(R.string.event_from_your_favorites_artists),
                style = typography().s
            )

            Spacer(modifier = Modifier.height(32.dp))



            Text(
                text = statusText,
                color = if (isScheduled) colorPalette().accent else colorPalette().red,
                style = typography().m
            )

            Spacer(modifier = Modifier.height(16.dp))

            val nextRunTime = workInfoNewRelease?.nextScheduleTimeMillis
            val timeRemaining = (nextRunTime?.minus(System.currentTimeMillis())) ?: 0L
            val formattedTimeRemaining = formatTimeRemaining(timeRemaining)

            if (isScheduled) {
                Text(
                    text = stringResource(R.string.event_next_run, formattedTimeRemaining),
                    style = typography().s
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isScheduled)
                Row(
                    modifier = Modifier.fillMaxWidth(.7f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !weeklyOrDaily, onClick = { weeklyOrDaily = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_daily), style = typography().s)
                    RadioButton(
                        selected = weeklyOrDaily, onClick = { weeklyOrDaily = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_weekly), style = typography().s)

                }

            Button(
                onClick = {
                    if (isScheduled) {
                        WorkManager.getInstance(context).cancelUniqueWork(workNameNewRelease)
                    } else {
                        periodicCheckNewFromArtists(context, weeklyOrDaily)
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().background0,
                    contentColor = colorPalette().text,
                )
            ) {
                Text(
                    if (isScheduled) stringResource(R.string.event_disable_notification) else stringResource(
                        R.string.event_enable_notification
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.event_if_there_are_new_releases_you_will_be_notified_with_a_system_notification_even_if_the_app_is_not_open),
                style = typography().xs,
                textAlign = TextAlign.Justify
            )
        }

        if (eventType == EventType.CheckUpdate) {
            val isScheduled = isWorkScheduled(workInfoCheckUpdate)
            val statusText = eventStatus(workInfoCheckUpdate)
            Text(
                text = stringResource(R.string.enable_check_for_update),
                style = typography().m
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = statusText,
                color = if (isScheduled) colorPalette().accent else colorPalette().red,
                style = typography().m
            )

            Spacer(modifier = Modifier.height(16.dp))

            val nextRunTime = workInfoCheckUpdate?.nextScheduleTimeMillis
            val timeRemaining = (nextRunTime?.minus(System.currentTimeMillis())) ?: 0L
            val formattedTimeRemaining = formatTimeRemaining(timeRemaining)

            if (isScheduled) {
                Text(
                    text = stringResource(R.string.event_next_run, formattedTimeRemaining),
                    style = typography().s
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isScheduled)
                Row(
                    modifier = Modifier.fillMaxWidth(.7f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !weeklyOrDaily, onClick = { weeklyOrDaily = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_daily), style = typography().s)
                    RadioButton(
                        selected = weeklyOrDaily, onClick = { weeklyOrDaily = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_weekly), style = typography().s)

                }

            Button(
                onClick = {
                    if (isScheduled) {
                        WorkManager.getInstance(context).cancelUniqueWork(workNameCheckUpdate)
                    } else {
                        periodicCheckUpdate(context, weeklyOrDaily)
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().background0,
                    contentColor = colorPalette().text,
                )
            ) {
                Text(
                    if (isScheduled) stringResource(R.string.event_disable_notification) else stringResource(
                        R.string.event_enable_notification
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup),
                style = typography().xs,
                textAlign = TextAlign.Justify
            )
        }

        if (eventType == EventType.AutoBackup) {
            val isScheduled = isWorkScheduled(workInfoAutoBackup)
            val statusText = eventStatus(workInfoAutoBackup)

            var selectedFolderUri by rememberPreference(autoBackupFolderKey, "")
            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    selectedFolderUri = uri.toString()
                }
            }

            Text(
                text = "Enable auto backup",
                style = typography().m
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusText,
                color = if (isScheduled) colorPalette().accent else colorPalette().red,
                style = typography().m
            )

            Spacer(modifier = Modifier.height(16.dp))

            val nextRunTime = workInfoAutoBackup?.nextScheduleTimeMillis
            val timeRemaining = (nextRunTime?.minus(System.currentTimeMillis())) ?: 0L
            val formattedTimeRemaining = formatTimeRemaining(timeRemaining)

            if (isScheduled) {
                Text(
                    text = stringResource(R.string.event_next_run, formattedTimeRemaining),
                    style = typography().s
                )
            } else {
                Text(
                    text = "Click to select the folder where the backup will be saved:",
                    style = typography().xs
                )
                Text(
                    text = selectedFolderUri.ifEmpty { "No folder selected, click here to select a folder" },
                    style = typography().xxs,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = colorPalette().accent,
                    modifier = Modifier.clickable {
                        folderPickerLauncher.launch(null)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(.7f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !weeklyOrDaily, onClick = { weeklyOrDaily = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_daily), style = typography().s)
                    RadioButton(
                        selected = weeklyOrDaily, onClick = { weeklyOrDaily = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorPalette().text,
                            unselectedColor = colorPalette().textDisabled
                        )
                    )
                    Text(text = stringResource(R.string.event_weekly), style = typography().s)

                }

            }

            Button(
                onClick = {
                    if (isScheduled) {
                        WorkManager.getInstance(context).cancelUniqueWork(workNameCheckUpdate)
                    } else {
                        periodicAutoBackup(context, weeklyOrDaily)
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().background0,
                    contentColor = colorPalette().text,
                )
            ) {
                Text(
                    if (isScheduled) stringResource(R.string.event_disable_notification) else stringResource(
                        R.string.event_enable_notification
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Viene eseguito un backup completo del database e delle impostazioni, anche se l'app è chiusa.",
                style = typography().xs,
                textAlign = TextAlign.Justify
            )
        }

    }

}

@Composable
fun eventStatus(workInfo: WorkInfo?): String {
    return when (workInfo?.state) {
        WorkInfo.State.ENQUEUED -> stringResource(R.string.event_scheduled)
        WorkInfo.State.RUNNING -> stringResource(R.string.event_running)
        WorkInfo.State.SUCCEEDED -> stringResource(R.string.event_completed)
        WorkInfo.State.FAILED -> stringResource(R.string.event_failed)
        WorkInfo.State.BLOCKED -> stringResource(R.string.event_blocked)
        WorkInfo.State.CANCELLED -> stringResource(R.string.event_cancelled)
        null -> stringResource(R.string.event_not_active)
        else -> stringResource(R.string.event_unknown)
    }
}