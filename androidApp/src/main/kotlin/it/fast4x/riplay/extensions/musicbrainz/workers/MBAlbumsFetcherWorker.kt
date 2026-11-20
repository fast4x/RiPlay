package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.fetchers.MBAlbumsByGenreFetcher
import it.fast4x.riplay.extensions.musicbrainz.fillers.NatureBackfiller
import timber.log.Timber

// Espande catalogo MB (settimanale)
class MBAlbumsFetcherWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        /** TTL del catalogo mb_album: 90 giorni. */
        const val MB_ALBUM_TTL_MS = 90L * 24 * 60 * 60 * 1000
    }

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("MBAlbumsFetcherWorker").i("Starting MB albums fetch...")

            // FIX (Fase 3): il catalogo mb_album cresceva senza limiti.
            // È una cache di arricchimento, non un archivio: si eliminano le
            // righe non aggiornate negli ultimi MB_ALBUM_TTL_MS (90 giorni).
            val pruned = Database.mbAlbumDao() // WorkerDependencies.database.mbAlbumDao()
                .deleteOlderThan(System.currentTimeMillis() - MB_ALBUM_TTL_MS)
            if (pruned > 0) {
                Timber.tag("MBAlbumsFetcherWorker").i("Pruned $pruned stale mb_album rows (>90 days)")
            }

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
