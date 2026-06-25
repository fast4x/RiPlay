package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.extensions.musicbrainz.fillers.NatureBackfiller
import timber.log.Timber

// Espande catalogo MB (settimanale)
class MBAlbumsFetcherWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("MBAlbumsFetcherWorker").i("Starting MB albums fetch...")

            val fetcher = MBAlbumsByGenreFetcher()

            val result = fetcher.fetch(
                topGenresCount = 5,
                albumsPerGenre = 50
            )

            Timber.tag("MBAlbumsFetcherWorker")
                .i("Fetch completed: saved=${result.saved}, skipped=${result.skipped}, failed=${result.failed}")

            // Classifica la nature dei nuovi album
            val natureBackfiller = NatureBackfiller()
            natureBackfiller.backfillAll()

            Result.success()
        } catch (e: Exception) {
            Timber.tag("MBAlbumsFetcherWorker").e(e, "Failed")
            Result.retry()
        }
    }
}