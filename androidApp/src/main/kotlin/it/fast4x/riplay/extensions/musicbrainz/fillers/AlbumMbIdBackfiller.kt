package it.fast4x.riplay.extensions.musicbrainz.fillers

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

        // Negative cache (Fase 3), come per ArtistMbIdBackfiller
        val failedBefore = System.currentTimeMillis() - NEGATIVE_CACHE_TTL_MS
        val albums = Database.albumDao().getYtAlbumsWithoutMbId(limit, failedBefore)
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
                var artistVerifiedInQuery = true

                // Step 2: Fallback solo per titolo se il primo non trova
                if (results.isEmpty()) {
                    results = mbClient.searchReleaseGroupByTitle(title)
                    artistVerifiedInQuery = false
                }

                // Step 3: Verifica match normalizzato (manteniamo il PRIMO che
                // combacia, come prima, ma ora la lista è già ordinata per score)
                val normalizedTitle = IdentityUtils.normalizeAlbumTitle(title)
                val bestMatch = results.firstOrNull { result ->
                    IdentityUtils.normalizeAlbumTitle(result.title) == normalizedTitle
                }

                if (bestMatch != null) {
                    // Step 4: Fetch dettagli per ottenere generi, rating, ecc.
                    delay(1100)
                    val details = mbClient.getReleaseGroupDetailWithArtist(bestMatch.id)

                    // FIX (Fase 2): il fallback solo-titolo poteva bindare il
                    // release-group di un ALTRO artista. Ora, se la query non
                    // includeva l'artista, si verifica l'artist-credit del
                    // dettaglio contro authorsText prima di persistere il mbId.
                    var artistOk = artistVerifiedInQuery
                    var creditName: String? = null
                    if (!artistVerifiedInQuery) {
                        creditName = details.artistCredit?.firstNotNullOfOrNull { it.name }
                        artistOk = creditName != null &&
                                IdentityUtils.nameSimilarity(artist, creditName) >= CREDIT_SIMILARITY
                    }

                    if (artistOk) {
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
                                ?.url?.resource ?: album.wikipediaUrl,
                            mbMatchedAt = System.currentTimeMillis(),
                            mbLookupFailedAt = null
                        )

                        albumRepository.upsertSmart(updated)
                        matched++
                        Timber.tag("AlbumMbIdBackfill")
                            .d("  ✓ '${title}' by ${artist} → ${bestMatch.id}")
                    } else {
                        // Artist credit diverso: non è il nostro album. Negative cache.
                        Database.albumDao().upsert(
                            album.copy(mbLookupFailedAt = System.currentTimeMillis())
                        )
                        Timber.tag("AlbumMbIdBackfill")
                            .w("  ⊘ '${title}' artist credit mismatch ($creditName ≠ $artist)")
                    }
                } else {
                    Database.albumDao().upsert(
                        album.copy(mbLookupFailedAt = System.currentTimeMillis())
                    )
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

    companion object {
        /** Un tentativo fallito non si ripete prima di 30 giorni. */
        const val NEGATIVE_CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000

        /**
         * Similarità minima tra authorsText e l'artist-credit MB nel fallback
         * solo-titolo. Soglia più morbida di MIN_NAME_SIMILARITY (0.85) perché
         * authorsText può contenere più artisti ("A, B") o suffissi.
         */
        const val CREDIT_SIMILARITY = 0.60f
    }
}
