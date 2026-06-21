package it.fast4x.riplay.extensions.musicbrainz.workers

import android.util.Log
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.ArtistRelation
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistRelationFetcher() {

    val profileRepository = Dependencies.application.profileRepository
    val mbClient = MusicBrainz()
    /**
     * Per i top N artisti del profilo utente, fetch artist-relations da MB.
     */
    suspend fun fetch(topArtistsCount: Int = 20): Result = withContext(Dispatchers.IO) {
        val profile = profileRepository.profile.value
            ?: return@withContext Result(0, 0, "Profile is null")

        // Verifica che faccia questo filtro
        val topArtists = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(topArtistsCount)
            .mapNotNull { affinity ->
                val artist = Database.artist(affinity.artistId).first() ?: return@mapNotNull null
                if (artist.mbId.isNullOrBlank()) return@mapNotNull null  // ← FILTRO CHIAVE
                Triple(artist, artist.mbId, affinity)
            }

        Timber.tag("RelFetcher")
            .i("=== START: ${topArtists.size}/${topArtistsCount} artists have mbId ===")

        var totalSaved = 0
        var totalFailed = 0

        for ((artist, mbId, affinity) in topArtists) {
            try {
                delay(1100)
                val relations = mbClient.fetchArtistRelations(mbId)

                val entities = relations.mapNotNull { rel ->
                    val targetMbId = rel.artist?.id ?: return@mapNotNull null
                    val relationType = rel.type ?: return@mapNotNull null

                    if (relationType in setOf("wikipedia", "wikidata", "allmusic", "discogs", "imdb")) {
                        return@mapNotNull null
                    }

                    ArtistRelation(
                        fromArtistId = mbId,  // ← MBID, non YT id
                        toArtistId = targetMbId,
                        relationType = relationType,
                        direction = "bidirectional",
                        fetchedAt = System.currentTimeMillis()
                    )
                }

                if (entities.isNotEmpty()) {
                    Database.upsertArtistRelation(entities)
                    totalSaved += entities.size
                    Timber.tag("RelFetcher")
                        .i("  ✓ ${artist.name} ($mbId): ${entities.size} relations")
                }
            } catch (e: Exception) {
                totalFailed++
                Timber.tag("RelFetcher").w("  ✗ ${artist.name}: ${e.message}")
            }
        }

        Timber.tag("RelFetcher").i("=== DONE: saved=$totalSaved, failed=$totalFailed ===")
        Result(totalSaved, totalFailed, "OK")
    }

    data class Result(val saved: Int, val failed: Int, val status: String)
}