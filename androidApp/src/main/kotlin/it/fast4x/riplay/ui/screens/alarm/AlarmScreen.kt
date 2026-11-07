package it.fast4x.riplay.ui.screens.alarm

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.alarm.AlarmItem
import it.fast4x.riplay.extensions.alarm.AlarmOptions
import it.fast4x.riplay.extensions.alarm.AlarmScheduler
import it.fast4x.riplay.extensions.alarm.loadAlarmOptions
import it.fast4x.riplay.extensions.alarm.loadAlarms
import it.fast4x.riplay.extensions.alarm.nextTriggerAtMillis
import it.fast4x.riplay.extensions.alarm.saveAlarmOptions
import it.fast4x.riplay.extensions.alarm.saveAlarms
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.Switch
import it.fast4x.riplay.ui.components.themed.ValueSelectorDialog
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@UnstableApi
@ExperimentalMaterial3Api
@Composable
fun AlarmScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        AlarmContent(
            navController = navController,
            listIndex = it
        )
    }
}

@UnstableApi
@ExperimentalMaterial3Api
@Composable
private fun AlarmContent(
    navController: NavController,
    listIndex: Int,
) {
    val context = LocalContext.current

    var alarms by remember { mutableStateOf(loadAlarms()) }
    var options by remember { mutableStateOf(loadAlarmOptions()) }

    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var isNewAlarm by remember { mutableStateOf(false) }
    var deletingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showAutoStopSelector by remember { mutableStateOf(false) }

    /**
     * Salva la lista sveglie e allinea le programmazioni:
     *  - sveglie attive -> programmate (o riprogrammate)
     *  - sveglie disattivate o rimosse -> cancellate
     */
    fun persistAlarms(oldList: List<AlarmItem>, newList: List<AlarmItem>) {
        alarms = newList
        saveAlarms(context, newList)
        newList.forEach { alarm -> AlarmScheduler.schedule(context, alarm) }
        (oldList.map { it.id } - newList.map { it.id }.toSet())
            .forEach { AlarmScheduler.cancel(context, it) }
    }

    val nextTriggerMillis = alarms
        .filter { it.enabled }
        .minOfOrNull { it.nextTriggerAtMillis() }

    val nextAlarmText = nextTriggerMillis?.let {
        SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(it))
    } ?: context.resources.getString(R.string.alarm_none)

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(R.string.alarm_title) {
                HeaderInfo(nextAlarmText, R.drawable.alarm)
            }

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SecondaryTextButton(
                            text = stringResource(R.string.alarm_add),
                            onClick = {
                                isNewAlarm = true
                                editingAlarm = AlarmItem()
                            },
                            modifier = Modifier,
                        )

                        Spacer(Modifier.width(12.dp))

                        BasicText(
                            text = stringResource(R.string.alarm_subtitle),
                            style = typography().xxs.secondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Suggerimento permesso sveglia esatta (Android 12+ se revocato)
                if (!AlarmScheduler.canScheduleExactAlarms(context)) {
                    item {
                        AlarmExactPermissionHint()
                    }
                }

                items(
                    items = alarms.sortedWith(
                        compareBy({ it.hour }, { it.minute })
                    ),
                    key = { it.id },
                ) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        onToggle = { checked ->
                            persistAlarms(alarms, alarms.map {
                                if (it.id == alarm.id) it.copy(enabled = checked) else it
                            })
                        },
                        onClick = {
                            isNewAlarm = false
                            editingAlarm = alarm
                        },
                        onDelete = { deletingAlarm = alarm },
                    )
                }

                // Opzioni globali della sveglia
                item {
                    AlarmOptionsSection(
                        options = options,
                        onUpdateOptions = { newOptions ->
                            options = newOptions
                            saveAlarmOptions(context, newOptions)
                        },
                        onAutoStopClick = { showAutoStopSelector = true },
                    )
                }
            }
        }
    }

    // Editor sveglia
    if (editingAlarm != null) {
        AlarmEditorDialog(
            alarm = editingAlarm!!,
            isNew = isNewAlarm,
            onDismiss = { editingAlarm = null },
            onSave = { newAlarm ->
                editingAlarm = null
                val old = alarms
                persistAlarms(
                    old,
                    if (isNewAlarm) old + newAlarm
                    else old.map { if (it.id == newAlarm.id) newAlarm else it }
                )
            }
        )
    }

    // Conferma eliminazione
    if (deletingAlarm != null) {
        ConfirmationDialog(
            text = stringResource(R.string.alarm_delete_confirm),
            onDismiss = { deletingAlarm = null },
            onConfirm = {
                deletingAlarm?.let { alarm ->
                    persistAlarms(alarms, alarms.filter { it.id != alarm.id })
                }
                deletingAlarm = null
            }
        )
    }

    // Selettore auto-stop
    if (showAutoStopSelector) {
        val autoStopOffText = stringResource(R.string.alarm_auto_stop_off)
        val autoStopMinutesText = stringResource(R.string.alarm_auto_stop_value)
        ValueSelectorDialog(
            onDismiss = { showAutoStopSelector = false },
            title = stringResource(R.string.alarm_auto_stop),
            selectedValue = options.autoStopMinutes,
            values = remember { listOf(0, 5, 10, 15, 30, 60) },
            onValueSelected = {
                options = options.copy(autoStopMinutes = it)
                saveAlarmOptions(context, options)
                showAutoStopSelector = false
            },
            valueText = { minutes ->
                if (minutes == 0) autoStopOffText
                else String.format(LocalLocale.current.platformLocale, autoStopMinutesText, minutes)
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AlarmRow(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            BasicText(
                text = alarm.timeLabel,
                style = typography().l.semiBold.copy(
                    color = if (alarm.enabled) colorPalette().text else colorPalette().textDisabled
                )
            )
            BasicText(
                text = alarm.repeatDescription(),
                style = typography().xxs.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (alarm.sourceName.isNotBlank()) {
                BasicText(
                    text = alarm.sourceName,
                    style = typography().xxs.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onDelete,
            icon = R.drawable.trash,
            color = colorPalette().textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .clip(getAlarmShape())
                .clickable { onToggle(!alarm.enabled) }
                .padding(4.dp)
        ) {
            Switch(isChecked = alarm.enabled)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AlarmOptionsSection(
    options: AlarmOptions,
    onUpdateOptions: (AlarmOptions) -> Unit,
    onAutoStopClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                colorPalette().background1,
                shape = getAlarmShape()
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Fade-in volume
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = stringResource(R.string.alarm_fade_in),
                    style = typography().s.medium
                )
                BasicText(
                    text = stringResource(R.string.alarm_fade_in_desc),
                    style = typography().xxs.secondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(getAlarmShape())
                    .clickable { onUpdateOptions(options.copy(fadeInEnabled = !options.fadeInEnabled)) }
                    .padding(4.dp)
            ) {
                Switch(isChecked = options.fadeInEnabled)
            }
        }

        // Auto-stop
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = stringResource(R.string.alarm_auto_stop),
                    style = typography().s.medium
                )
                BasicText(
                    text = stringResource(R.string.alarm_auto_stop_desc),
                    style = typography().xxs.secondary
                )
            }

            val autoStopOffText = stringResource(R.string.alarm_auto_stop_off)
            val autoStopMinutesText = stringResource(R.string.alarm_auto_stop_value)
            SecondaryTextButton(
                text = if (options.autoStopMinutes == 0) autoStopOffText
                else String.format(LocalLocale.current.platformLocale, autoStopMinutesText, options.autoStopMinutes),
                onClick = onAutoStopClick,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AlarmExactPermissionHint() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                colorPalette().background1,
                shape = getAlarmShape()
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = stringResource(R.string.alarm_exact_permission_hint),
            style = typography().xxs.secondary
        )
        Spacer(Modifier.height(6.dp))
        SecondaryTextButton(
            text = stringResource(R.string.alarm_exact_permission_open_settings),
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
        )
    }
}
