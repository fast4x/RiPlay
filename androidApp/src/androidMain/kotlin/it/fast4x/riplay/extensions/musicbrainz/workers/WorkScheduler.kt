package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import androidx.work.*
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.appContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

object WorkScheduler {

    private const val INITIAL_SETUP_WORK_NAME = "initial_setup"
    private const val PROFILE_REBUILD_WORK_NAME = "profile_rebuild"
    private const val MB_ALBUMS_WORK_NAME = "mb_albums_fetch"
    private const val ARTIST_RELATIONS_WORK_NAME = "artist_relations_fetch"
    private const val SONG_ARTIST_BACKFILL_WORK_NAME = "song_artist_backfill"

    private const val NEW_RELEASES_FETCH = "new_releases_fetch"

    /**
     * Registra tutti i job periodici. Da chiamare in Application.onCreate.
     * Idempotente: chiamate successive non duplicano i job.
     */
    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // === Initial Setup (one-shot, solo prima installazione) ===
        scheduleInitialSetupIfNeeded(workManager)

        // === Profile Rebuild (giornaliero, notte) ===
        val profileWork = PeriodicWorkRequestBuilder<ProfileRebuildWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()
        ).setInitialDelay(
            calculateDelayUntilNight(),
            TimeUnit.MILLISECONDS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PROFILE_REBUILD_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            profileWork
        )

        // === MB Albums Fetch (settimanale, WiFi + charging) ===
        val mbAlbumsWork = PeriodicWorkRequestBuilder<MBAlbumsFetcherWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            MB_ALBUMS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            mbAlbumsWork
        )

        // === Artist Relations Fetch (settimanale, WiFi + charging) ===
        val relationsWork = PeriodicWorkRequestBuilder<ArtistRelationFetcherWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            ARTIST_RELATIONS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            relationsWork
        )

        // === SongArtist Backfill (mensile, qualsiasi rete) ===
        val backfillWork = PeriodicWorkRequestBuilder<SongArtistBackfillWorker>(
            30, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            SONG_ARTIST_BACKFILL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backfillWork
        )

        // === New Releases Fetch (giornaliero, WiFi) ===
        val newReleasesWork = PeriodicWorkRequestBuilder<NewReleasesFetcherWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
        ).setInitialDelay(
            calculateDelayUntilMorning(),  // alle 8 AM, quando l'utente si sveglia
            TimeUnit.MILLISECONDS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            NEW_RELEASES_FETCH,
            ExistingPeriodicWorkPolicy.KEEP,
            newReleasesWork
        )


    }

    private fun calculateDelayUntilMorning(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
    /**
     * Lancia il setup iniziale solo se non è mai stato eseguito.
     * Usa SharedPreferences per tracciare lo stato.
     */
    private fun scheduleInitialSetupIfNeeded(workManager: WorkManager) {
        val prefs = appContext().preferences

        if (prefs.getBoolean(PreferenceKey.INITIAL_SETUP_WORKER_DONE.key, false)) {
            return
        }

        val initialWork = OneTimeWorkRequestBuilder<InitialSetupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            INITIAL_SETUP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            initialWork
        )

        // Segna come schedulato ( verrà segnato done dal Worker stesso)
        prefs.edit { putBoolean(PreferenceKey.INITIAL_SETUP_WORKER_DONE.key, true) }
    }

    /**
     * Calcola i millisecondi fino alle 3 AM di notte.
     */
    private fun calculateDelayUntilNight(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }

    /**
     * Forza il rebuild del profilo (utile dopo pull-to-refresh o settings change).
     */
    fun forceProfileRebuild(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val work = OneTimeWorkRequestBuilder<ProfileRebuildWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "force_profile_rebuild",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }
}