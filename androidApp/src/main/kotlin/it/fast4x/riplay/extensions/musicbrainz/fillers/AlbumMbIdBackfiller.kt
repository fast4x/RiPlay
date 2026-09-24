package it.fast4x.riplay.extensions.musicbrainz.fillers

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.repository.AlbumRepository
import it.fast4x.riplay.extensions.musicbrainz.utils.IdentityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class AlbumMbIdBackfiller() {

    private val mbClient = MusicBrainz()
    private val albumRepository = AlbumRepository()

    suspend fun backfill(limit: Int = 200): BackfillResult = withContext(Dispatchers.IO) {
        Timber.tag("AlbumMbIdBackfill").i("=== START ===")

        // Prendi album YTM senza mbId, escludendo rumore
        val albums = Database.albumDao().getYtAlbumsWithoutMbId(limit)
        Timber.tag("AlbumMbIdBackfill").i("Albums to enrich: ${albums.size}")

        var matched = 0
        var notFound = 0
        var failed = 0

        for (album in albums) {
            val title = album.title ?: continue
            val artist = album.authorsText ?: ""

            if (title.isBlank() || artist.isBlank()) {
                notFound++
                continue
            }

            try {
                delay(1100) // rate limit MB

                // Step 1: Cerca per titolo + artista (match preciso)
                var results = mbClient.searchReleaseGroup(title, artist)

                // Step 2: Fallback solo per titolo se il primo non trova
                if (results.isEmpty()) {
                    results = mbClient.searchReleaseGroupByTitle(title)
                }

                // Step 3: Verifica match normalizzato
                val normalizedTitle = IdentityUtils.normalizeAlbumTitle(title)
                val bestMatch = results.firstOrNull { result ->
                    IdentityUtils.normalizeAlbumTitle(result.title) == normalizedTitle
                }

                if (bestMatch != null) {
                    // Step 4: Fetch dettagli per ottenere generi, rating, ecc.
                    delay(1100)
                    val details = mbClient.getReleaseGroupDetailWithArtist(bestMatch.id)

                    // Step 5: Aggiorna album con mbId e metadati via upsertSmart
                    val updated = album.copy(
                        mbId = bestMatch.id,
                        genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() } ?: album.genres,
                        tags = details.tags?.map { it.name }?.takeIf { it.isNotEmpty() } ?: album.tags,
                        originalYear = parseYear(details.firstReleaseDate) ?: album.originalYear,
                        albumType = details.primaryType ?: album.albumType,
                        rating = details.rating?.value ?: album.rating,
                        ratingVotes = details.rating?.votesCount ?: album.ratingVotes,
                        wikipediaUrl = details.relations
                            ?.find { it.type == "wikipedia" }
                            ?.url?.resource ?: album.wikipediaUrl
                    )

                    albumRepository.upsertSmart(updated)
                    matched++
                    Timber.tag("AlbumMbIdBackfill")
                        .d("  ✓ '${title}' by ${artist} → ${bestMatch.id}")
                } else {
                    notFound++
                }
            } catch (e: Exception) {
                failed++
                Timber.tag("AlbumMbIdBackfill").w("  ✗ '${title}': ${e.message}")
            }
        }

        Timber.tag("AlbumMbIdBackfill")
            .i("=== DONE: matched=$matched, notFound=$notFound, failed=$failed ===")
        BackfillResult(matched, notFound, failed)
    }

    private fun parseYear(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank()) return null
        return dateStr.substring(0, 4).toIntOrNull()
    }

    data class BackfillResult(val matched: Int, val notFound: Int, val failed: Int)
}