package it.fast4x.riplay.extensions.musicbrainz.workers

import timber.log.Timber
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Aggiorna grafo MB (settimanale)
class ArtistRelationFetcherWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("ArtistRelationWorker").i("Starting artist relations fetch...")

            val fetcher = ArtistRelationFetcher()

            val result = fetcher.fetch(topArtistsCount = 20)

            Timber.tag("ArtistRelationWorker")
                .i("Fetch completed: saved=${result.saved}, failed=${result.failed}")

            Result.success()
        } catch (e: Exception) {
            Timber.tag("ArtistRelationWorker").e(e, "Failed")
            Result.retry()
        }
    }
}