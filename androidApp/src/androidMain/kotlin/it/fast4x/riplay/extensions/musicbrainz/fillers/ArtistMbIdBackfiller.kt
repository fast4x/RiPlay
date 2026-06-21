package it.fast4x.riplay.extensions.musicbrainz.fillers

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistMbIdBackfiller() {

    val mbClient = MusicBrainz()

    suspend fun backfill(limit: Int = 50): Result = withContext(Dispatchers.IO) {
        val artists = Database.artistDao().getArtistsWithoutMbId(limit)
        Timber.tag("MbIdBackfill").i("Artists to enrich: ${artists.size}")

        var success = 0
        var failed = 0

        for (artist in artists) {
            try {
                delay(1100) // rate limit MB
                val searchResult = mbClient.searchArtistByName(artist.name ?: continue)
                val mbid = searchResult.firstOrNull()?.id

                if (mbid != null) {
                    Database.update(artist.copy(mbId = mbid))
                    success++
                    Timber.tag("MbIdBackfill").i("  ✓ ${artist.name} → $mbid")
                } else {
                    Timber.tag("MbIdBackfill").w("  ⊘ ${artist.name} not found on MB")
                }
            } catch (e: Exception) {
                failed++
                Timber.tag("MbIdBackfill").w("  ✗ ${artist.name}: ${e.message}")
            }
        }

        Result(success, failed)
    }

    data class Result(val success: Int, val failed: Int)
}