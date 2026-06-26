package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.utils.RecommendationConstants
import it.fast4x.riplay.extensions.musicbrainz.fillers.AlbumMbIdBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.ArtistMbIdBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.NatureBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.SongArtistBackfiller
import timber.log.Timber

// Setup one-shot per prima installazione
class InitialSetupWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {
            Timber.tag("InitialSetupWorker").i("=== STARTING INITIAL SETUP ===")

            // 1. Backfill SongArtistCrossRef
            Timber.tag("InitialSetupWorker").i("Step 1: SongArtistCrossRef backfill")
            val saBackfiller = SongArtistBackfiller()
            val saResult = saBackfiller.backfill(limit = 5000)
            Timber.tag("InitialSetupWorker").i("  Result: $saResult")

            // 2. Backfill Artist.mbId
            Timber.tag("InitialSetupWorker").i("Step 2: Artist mbId backfill")
            val mbIdBackfiller = ArtistMbIdBackfiller()
            val mbIdResult = mbIdBackfiller.backfill(limit = 50)
            Timber.tag("InitialSetupWorker").i("  Result: $mbIdResult")

            // 3. Backfill Album.mbId
            Timber.tag("InitialSetupWorker").i("Step 3: Album mbId backfill")
            val albumBackfiller = AlbumMbIdBackfiller()
            val albumResult = albumBackfiller.backfill(limit = 200)
            Timber.tag("InitialSetupWorker").i("  Result: $albumResult")

            // 4. Classificazione nature
            Timber.tag("InitialSetupWorker").i("Step 4: Nature classification")
            val natureBackfiller = NatureBackfiller()
            val natureResult = natureBackfiller.backfillAll()
            Timber.tag("InitialSetupWorker").i("  Result: $natureResult")

            // 5. Profile rebuild
            Timber.tag("InitialSetupWorker").i("Step 5: Profile rebuild")
            WorkerDependencies.profileRepository.rebuildFull(
                userId = RecommendationConstants.USER_ID_SELF
            )

            // 6. Fetch ArtistRelations
            Timber.tag("InitialSetupWorker").i("Step 6: Artist relations fetch")
            val relFetcher = ArtistRelationFetcher()
            val relResult = relFetcher.fetch(topArtistsCount = 20)
            Timber.tag("InitialSetupWorker").i("  Result: $relResult")

            Timber.tag("InitialSetupWorker").i("=== INITIAL SETUP COMPLETED ===")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("InitialSetupWorker").e(e, "Failed")
            Result.retry()
        }
    }
}