package it.fast4x.riplay.extensions.scheduled

import android.content.Context
import androidx.work.Constraints
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import it.fast4x.riplay.extensions.scheduled.workers.NewFromArtistsWorker

fun periodicCheckNewFromArtists(context: Context, weeklyOrDaily: Boolean = false) {
    val now = System.currentTimeMillis()

    val calendar = Calendar.getInstance().apply {
        if (!weeklyOrDaily) {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        } else {
            set(Calendar.DAY_OF_YEAR, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (timeInMillis <= now) {
            add(if (!weeklyOrDaily) Calendar.DAY_OF_MONTH else Calendar.WEEK_OF_YEAR, 1)
        }
    }

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val initialDelayInMillis = calendar.timeInMillis - now

    val weeklyOrDailyWorkRequest = PeriodicWorkRequestBuilder<NewFromArtistsWorker>(
        if (!weeklyOrDaily) 24 else 7, if (!weeklyOrDaily) TimeUnit.HOURS else TimeUnit.DAYS
    )
        .setInitialDelay(initialDelayInMillis, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "weeklyOrDailyCheckNewFromArtistsWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        weeklyOrDailyWorkRequest
    )
}