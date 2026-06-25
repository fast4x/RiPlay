package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.extensions.musicbrainz.fillers.NatureBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.SongArtistBackfiller
import timber.log.Timber

// Popola cross-ref (mensile)
class SongArtistBackfillWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("SongArtistBackfillWorke").i("Starting cross-ref backfill...")

            val backfiller = SongArtistBackfiller()
            val result = backfiller.backfill(limit = 5000)

            Timber.tag("SongArtistBackfillWorke")
                .i("Backfill completed: refs=${result.refsCreated}, matched=${result.songsMatched}")

            // Dopo backfill, riclassifica nature degli artisti
            val natureBackfiller = NatureBackfiller()
            natureBackfiller.backfillAll()

            Result.success()
        } catch (e: Exception) {
            Timber.tag("SongArtistBackfillWorke").e(e, "Failed")
            Result.retry()
        }
    }
}