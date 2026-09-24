package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.utils.RecommendationConstants
import timber.log.Timber

// Ricalcola profilo utente (giornaliero)
class ProfileRebuildWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("ProfileRebuildWorker").i("Starting full profile rebuild...")

            WorkerDependencies.profileRepository.rebuildFull(
                userId = RecommendationConstants.USER_ID_SELF
            )

            Timber.tag("ProfileRebuildWorker").i("Profile rebuild completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("ProfileRebuildWorker").e(e, "Failed")
            Result.retry()
        }
    }
}