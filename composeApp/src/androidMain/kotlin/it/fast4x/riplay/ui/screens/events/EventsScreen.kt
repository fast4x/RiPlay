package it.fast4x.riplay.ui.screens.events

import android.widget.RadioGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.scheduled.periodicCheckNewFromArtists
import it.fast4x.riplay.utils.formatTimeRemaining
import it.fast4x.riplay.utils.getWorkStatusFlow
import it.fast4x.riplay.utils.isWorkScheduled
import it.fast4x.riplay.utils.typography

@Composable
fun EventsScreen() {
    val context = LocalContext.current

    val workName = "weeklyOrDailyCheckNewFromArtistsWork"

    val workInfo by context.getWorkStatusFlow(workName).collectAsState(initial = null)

    val isScheduled = isWorkScheduled(workInfo)

    var weeklyOrDaily by remember { mutableStateOf(true) }


    Column(
        modifier = Modifier
            .fillMaxHeight(.6f)
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.event_notification_for_new_release),
            style = typography().m
        )
        Text(
            text = stringResource(R.string.event_from_your_favorites_artists),
            style = typography().s
        )

        Spacer(modifier = Modifier.height(32.dp))

        val statusText = when (workInfo?.state) {
            WorkInfo.State.ENQUEUED -> stringResource(R.string.event_scheduled)
            WorkInfo.State.RUNNING -> stringResource(R.string.event_running)
            WorkInfo.State.SUCCEEDED -> stringResource(R.string.event_completed)
            WorkInfo.State.FAILED -> stringResource(R.string.event_failed)
            WorkInfo.State.BLOCKED -> stringResource(R.string.event_blocked)
            WorkInfo.State.CANCELLED -> stringResource(R.string.event_cancelled)
            null -> stringResource(R.string.event_not_active)
            else -> stringResource(R.string.event_unknown)
        }

        Text(
            text = statusText,
            color = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = typography().m
        )

        Spacer(modifier = Modifier.height(16.dp))

        val nextRunTime = workInfo?.nextScheduleTimeMillis
        val timeRemaining = (nextRunTime?.minus(System.currentTimeMillis())) ?: 0L

        if (isScheduled)
            Text(
                text = stringResource(R.string.event_next_run, formatTimeRemaining(timeRemaining)),
                style = typography().s
            )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isScheduled)
            Row(modifier = Modifier.fillMaxWidth(.7f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                RadioButton(selected = !weeklyOrDaily, onClick = { weeklyOrDaily = false })
                Text(text = stringResource(R.string.event_daily), style = typography().s)
                RadioButton(selected = weeklyOrDaily, onClick = { weeklyOrDaily = true })
                Text(text = stringResource(R.string.event_weekly), style = typography().s)

            }

        Button(
            onClick = {
                if (isScheduled) {
                    WorkManager.getInstance(context).cancelUniqueWork(workName)
                } else {
                    periodicCheckNewFromArtists(context, weeklyOrDaily)
                }
            },
            enabled = true
        ) {
            Text(if (isScheduled) stringResource(R.string.event_disable_notification) else stringResource(
                R.string.event_enable_notification
            ))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.event_if_there_are_new_releases_you_will_be_notified_with_a_system_notification_even_if_the_app_is_not_open),
            style = typography().xs,
            textAlign = TextAlign.Justify
        )
    }
}