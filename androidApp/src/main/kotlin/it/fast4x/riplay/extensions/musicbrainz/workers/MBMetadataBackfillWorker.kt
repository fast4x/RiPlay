package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.MBMetadataHelper
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class MBMetadataBackfillWorker(
    ctx: Context, params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songsToEnrich = Database.getTopSongsWithoutGenres(limit = 100)
        var enriched = 0

        for (song in songsToEnrich) {
            try {
                // Chiama il tuo servizio MB esistente
                val mbclient = MusicBrainz()
                val mdHelper = MBMetadataHelper(mbclient)
                mdHelper.onArtistViewed(song.artistsText ?: continue)
                enriched++

                // Rate limit MB: max 1 req/sec
                delay(1100)
            } catch (e: Exception) {
                Timber.tag("MBBackfill").e(e, "Failed for ${song.id}")
            }
        }

        Timber.tag("MBBackfill").i("Enriched $enriched/${songsToEnrich.size} songs")
        Result.success()
    }
}