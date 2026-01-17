package it.fast4x.riplay.utils

import android.content.Context
import androidx.work.*
import it.fast4x.riplay.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


fun Context.getWorkStatusFlow(uniqueWorkName: String): Flow<WorkInfo?> {
    val workQuery = WorkQuery.fromUniqueWorkNames(uniqueWorkName)
    return WorkManager.getInstance(this)
        .getWorkInfosFlow(workQuery)
        .map { listOfWorkInfo ->
            listOfWorkInfo.firstOrNull()
        }
}

fun isWorkScheduled(workInfo: WorkInfo?): Boolean {
    return workInfo != null && (workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING)
}

fun formatTimeRemaining(millis: Long): String {
    val days = millis / (1000 * 60 * 60 * 24)
    val hours = (millis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)

    return when {
        days > 0 -> appContext().getString(R.string.formattedtime_within_days_and_hours, days, hours)
        hours > 0 -> "Within $hours hours"
        else -> "Soon"
    }
}