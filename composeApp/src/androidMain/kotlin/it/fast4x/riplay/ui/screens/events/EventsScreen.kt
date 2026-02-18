package it.fast4x.riplay.ui.screens.events

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import it.fast4x.riplay.ui.styling.semiBold


const val workNameNewRelease = "weeklyOrDailyCheckNewFromArtistsWork"
const val workNameCheckUpdate = "weeklyOrDailyCheckUpdateWork"
const val workNameAutoBackup = "weeklyOrDailyAutoBackupWork"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen() {
    val context = LocalContext.current

    val buttonsList = mutableListOf(
        EventType.NewArtistsRelease to EventType.NewArtistsRelease.textName,
        EventType.AutoBackup to EventType.AutoBackup.textName
    ).apply {
        if(BuildConfig.BUILD_VARIANT == "full")
            add(EventType.CheckUpdate to EventType.CheckUpdate.textName)
    }
    var eventType by remember { mutableStateOf(EventType.NewArtistsRelease) }

    val workInfoNewRelease by context.getWorkStatusFlow(workNameNewRelease).collectAsState(initial = null)
    val workInfoCheckUpdate by context.getWorkStatusFlow(workNameCheckUpdate).collectAsState(initial = null)
    val workInfoAutoBackup by context.getWorkStatusFlow(workNameAutoBackup).collectAsState(initial = null)

    var weeklyOrDaily by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorPalette().background1.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(modifier = Modifier.fillMaxWidth()) {
                    ButtonsRow(
                        buttons = buttonsList,
                        currentValue = eventType,
                        onValueUpdate = { eventType = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                AnimatedContent(
                    targetState = eventType,
                    label = "event_content",
                    transitionSpec = {

                        val enterTransition = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { it / 3 }
                        )

                        val exitTransition = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                            animationSpec = tween(300),
                            targetOffsetY = { -it / 3 }
                        )


                        enterTransition.togetherWith(exitTransition).using(
                            SizeTransform(clip = false)
                        )
                    }
                ) { currentType ->

                    when (currentType) {
                        EventType.NewArtistsRelease -> NewArtistsContent(
                            context = context,
                            workInfo = workInfoNewRelease,
                            weeklyOrDaily = weeklyOrDaily,
                            onToggleFrequency = { weeklyOrDaily = it },
                            onAction = { if (isWorkScheduled(workInfoNewRelease)) {
                                WorkManager.getInstance(context).cancelUniqueWork(workNameNewRelease)
                            } else {
                                periodicCheckNewFromArtists(context, weeklyOrDaily)
                            }}
                        )
                        EventType.CheckUpdate -> CheckUpdateContent(
                            context = context,
                            workInfo = workInfoCheckUpdate,
                            weeklyOrDaily = weeklyOrDaily,
                            onToggleFrequency = { weeklyOrDaily = it },
                            onAction = { if (isWorkScheduled(workInfoCheckUpdate)) {
                                WorkManager.getInstance(context).cancelUniqueWork(workNameCheckUpdate)
                            } else {
                                periodicCheckUpdate(context, weeklyOrDaily)
                            }}
                        )
                        EventType.AutoBackup -> AutoBackupContent(
                            context = context,
                            workInfo = workInfoAutoBackup,
                            weeklyOrDaily = weeklyOrDaily,
                            onToggleFrequency = { weeklyOrDaily = it },
                            onAction = { if (isWorkScheduled(workInfoAutoBackup)) {
                                WorkManager.getInstance(context).cancelUniqueWork(workNameAutoBackup)
                            } else {
                                periodicAutoBackup(context, weeklyOrDaily)
                            }}
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StatusHeader(
    iconRes: Int,
    titleRes: Int,
    subtitleRes: Int,
    isScheduled: Boolean,
    nextRunTimeMs: Long?,
    workInfo: WorkInfo?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = colorPalette().text,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = typography().m)
            Text(text = stringResource(subtitleRes), style = typography().s, color = colorPalette().textSecondary)
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isScheduled) colorPalette().accent else colorPalette().textDisabled)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    val statusText = eventStatus(workInfo)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = statusText,
            color = if (isScheduled) colorPalette().accent else colorPalette().textSecondary,
            style = typography().m,
            fontWeight = FontWeight.Bold
        )
        if (isScheduled) {
            Spacer(modifier = Modifier.width(8.dp))
            val timeRemaining = (nextRunTimeMs?.minus(System.currentTimeMillis())) ?: 0L
            Text(
                text = "• ${formatTimeRemaining(timeRemaining)}",
                style = typography().xs,
                color = colorPalette().textSecondary
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun FrequencySelector(
    isWeekly: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    if (!enabled) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color = colorPalette().background0)
                .clickable { if (isWeekly) onToggle(false) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !isWeekly,
                onClick = { onToggle(false) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorPalette().accent,
                    unselectedColor = colorPalette().textDisabled
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.event_daily), style = typography().s)
        }

        Spacer(modifier = Modifier.width(16.dp))


        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color = colorPalette().background0)
                .clickable { if (!isWeekly) onToggle(true) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isWeekly,
                onClick = { onToggle(true) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorPalette().accent,
                    unselectedColor = colorPalette().textDisabled
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.event_weekly), style = typography().s)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun EventActionButton(
    isScheduled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = true,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorPalette().background0,
            contentColor = colorPalette().text
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorPalette().textDisabled)
    ) {
        Text(
            if (isScheduled) stringResource(R.string.event_disable_notification) else stringResource(R.string.event_enable_notification),
            style = typography().m.semiBold
        )
    }
}


@Composable
fun NewArtistsContent(
    context: Context,
    workInfo: WorkInfo?,
    weeklyOrDaily: Boolean,
    onToggleFrequency: (Boolean) -> Unit,
    onAction: () -> Unit
) {
    val isScheduled = isWorkScheduled(workInfo)

    Column() {
        StatusHeader(
            iconRes = R.drawable.musical_notes,
            titleRes = R.string.event_notification_for_new_release,
            subtitleRes = R.string.event_from_your_favorites_artists,
            isScheduled = isScheduled,
            nextRunTimeMs = workInfo?.nextScheduleTimeMillis,
            workInfo = workInfo
        )

        Spacer(modifier = Modifier.height(24.dp))
        FrequencySelector(weeklyOrDaily, onToggleFrequency, enabled = !isScheduled)

        Spacer(modifier = Modifier.height(24.dp))
        EventActionButton(isScheduled, onAction)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.event_if_there_are_new_releases_you_will_be_notified_with_a_system_notification_even_if_the_app_is_not_open),
            style = typography().xs,
            textAlign = TextAlign.Center,
            color = colorPalette().textSecondary
        )
    }
}

@Composable
fun CheckUpdateContent(
    context: Context,
    workInfo: WorkInfo?,
    weeklyOrDaily: Boolean,
    onToggleFrequency: (Boolean) -> Unit,
    onAction: () -> Unit
) {
    val isScheduled = isWorkScheduled(workInfo)

    Column() {
        StatusHeader(
            iconRes = R.drawable.update,
            titleRes = R.string.enable_check_for_update,
            subtitleRes = R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup,
            isScheduled = isScheduled,
            nextRunTimeMs = workInfo?.nextScheduleTimeMillis,
            workInfo = workInfo
        )

        Spacer(modifier = Modifier.height(24.dp))

        FrequencySelector(weeklyOrDaily, onToggleFrequency, enabled = !isScheduled)

        Spacer(modifier = Modifier.height(24.dp))
        EventActionButton(isScheduled, onAction)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup),
            style = typography().xs,
            textAlign = TextAlign.Center,
            color = colorPalette().textSecondary
        )
    }
}

@Composable
fun AutoBackupContent(
    context: Context,
    workInfo: WorkInfo?,
    weeklyOrDaily: Boolean,
    onToggleFrequency: (Boolean) -> Unit,
    onAction: () -> Unit
) {
    val isScheduled = isWorkScheduled(workInfo)
    val DEFAULT_DOWNLOADS_URI = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
    var selectedFolderUri by rememberPreference(autoBackupFolderKey, DEFAULT_DOWNLOADS_URI)

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

    Column() {

        StatusHeader(
            iconRes = R.drawable.update,
            titleRes = R.string.event_enable_auto_backup,
            subtitleRes = R.string.event_a_full_backup_of_the_database_and_settings_is_performed_even_if_the_app_is_closed,
            isScheduled = isScheduled,
            nextRunTimeMs = workInfo?.nextScheduleTimeMillis,
            workInfo = workInfo
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { folderPickerLauncher.launch(null) },
            colors = CardDefaults.cardColors(containerColor = colorPalette().background0),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (selectedFolderUri.isNotEmpty()) colorPalette().accent else colorPalette().textDisabled.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.folder),
                    contentDescription = null,
                    tint = if (selectedFolderUri.isNotEmpty()) colorPalette().accent else colorPalette().textDisabled,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.event_click_to_select_the_folder_where_the_backup_will_be_saved),
                        style = typography().s,
                        color = colorPalette().text
                    )
                    Text(
                        text = selectedFolderUri.ifEmpty { "No folder selected" },
                        style = typography().xxs,
                        color = colorPalette().textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.chevron_forward),
                    contentDescription = null,
                    tint = colorPalette().textDisabled
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FrequencySelector(weeklyOrDaily, onToggleFrequency, enabled = !isScheduled)

        Spacer(modifier = Modifier.height(24.dp))

        EventActionButton(isScheduled, onAction)
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
