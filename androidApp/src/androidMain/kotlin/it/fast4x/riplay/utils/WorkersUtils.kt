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

fun formatTimeRemaining(nextRunTimeMs: Long?): String {
    if (nextRunTimeMs == null || nextRunTimeMs <= 0L || nextRunTimeMs < 1500000000000L) {
        return ""
    }

    val currentTime = System.currentTimeMillis()
    var targetTime = nextRunTimeMs

    // Rilevamento automatico dell'unità di misura (Microsecondi o Nanosecondi)
    if (targetTime > 5_000_000_000_000L) {
        if (targetTime > 5_000_000_000_000_000L) {
            targetTime /= 1_000_000L
        } else {
            targetTime /= 1_000L
        }
    }

    val millisRemaining = targetTime - currentTime

    // Se il tempo residuo calcolato è negativo o imminente, mostra "presto"
    if (millisRemaining <= 60_000L) {
        return appContext().getString(R.string.formattedtime_soon)
    }

    val days = millisRemaining / (1000 * 60 * 60 * 24)
    val hours = (millisRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)

    return when {
        days > 0 -> appContext().getString(R.string.formattedtime_within_days_and_hours, days, hours)
        hours > 0 -> appContext().getString(R.string.formattedtime_within_hours, hours)
        else -> appContext().getString(R.string.formattedtime_soon)
    }
}




/*
fun formatTimeRemaining(millis: Long): String {
    val days = millis / (1000 * 60 * 60 * 24)
    val hours = (millis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)

    return when {
        days > 0 -> appContext().getString(R.string.formattedtime_within_days_and_hours, days, hours)
        hours > 0 -> appContext().getString(R.string.formattedtime_within_hours, hours)
        else -> appContext().getString(R.string.formattedtime_soon)
    }
}

 */