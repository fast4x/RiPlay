package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import androidx.work.WorkManager
import timber.log.Timber

fun logWorkersStatus(context: Context) {
    val workManager = WorkManager.getInstance(context)

    workManager.getWorkInfosForUniqueWork("profile_rebuild").get().forEach { info ->
        Timber.tag("WORK_DEBUG")
            .d("profile_rebuild: state=${info.state}, last=${info.runAttemptCount}")
    }
    workManager.getWorkInfosForUniqueWork("mb_albums_fetch").get().forEach { info ->
        Timber.tag("WORK_DEBUG")
            .d("mb_albums_fetch: state=${info.state}, last=${info.runAttemptCount}")
    }
    workManager.getWorkInfosForUniqueWork("initial_setup").get().forEach { info ->
        Timber.tag("WORK_DEBUG").d("initial_setup: state=${info.state}")
    }
}