package it.fast4x.riplay.extensions.musicbrainz.workers

import android.util.Log
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.models.MBReleaseGroupDetailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class MBAlbumsByGenreFetcher() {

    val profileRepository = Dependencies.application.profileRepository
    val mbClient = MusicBrainz()

    suspend fun fetch(
        topGenresCount: Int = 5,
        albumsPerGenre: Int = 50
    ): Result = withContext(Dispatchers.IO) {
        try {
            val profile = profileRepository.profile.value
                ?: return@withContext Result(0, 0, 0, "Profile is null")

            val topGenres = profile.keywordVector.entries
                .sortedByDescending { it.value }
                .take(topGenresCount)
                .map { it.key }

            Timber.tag("MBAlbumFetcher").i("=== START: fetching for genres $topGenres ===")

            var totalSaved = 0
            var totalSkipped = 0
            var totalFailed = 0

            for (genre in topGenres) {
                Timber.tag("MBAlbumFetcher").i("=== Genre: $genre ===")

                try {
                    val searchResults = mbClient.searchAlbumsByGenre(genre, limit = albumsPerGenre)
                    Timber.tag("MBAlbumFetcher").i("Found ${searchResults.size} release groups")

                    for (result in searchResults) {
                        try {
                            delay(1100)

                            val details = mbClient.getReleaseGroupDetailWithArtist(result.id)

                            // Nel fetcher, dopo aver ottenuto details:
                            val tagCount = details.genres.size + (details.tags?.size ?: 0)
                            val rating = details.rating?.value
                            val votes = details.rating?.votesCount ?: 0

                            // Salva SOLO se:
                            // - Ha rating MB reale, oppure
                            // - Ha almeno 3 generi/tags totali, oppure
                            // - Ha artist credit (album "vero", non raccolte anonime)
                            val isQuality = (rating != null && rating > 0) ||
                                    tagCount >= 3 ||
                                    (details.artistCredit?.isNotEmpty() == true && details.primaryType == "Album")

                            if (!isQuality) {
                                totalSkipped++
                                Timber.tag("MBAlbumFetcher")
                                    .d("  Skip low-quality: '${details.title}' (tagCount=$tagCount, rating=$rating)")
                                continue
                            }

                            val mbAlbum = MBAlbum(
                                id = details.id,
                                title = details.title,
                                primaryType = details.primaryType,
                                secondaryTypes = details.secondaryTypes.takeIf { it.isNotEmpty() },
                                firstReleaseDate = details.firstReleaseDate,
                                originalYear = parseYear(details.firstReleaseDate),
                                genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
                                tags = details.tags?.map { it.name }?.takeIf { it.isNotEmpty() },
                                rating = details.rating?.value,
                                ratingVotes = details.rating?.votesCount,
                                wikipediaUrl = details.relations
                                    ?.find { it.type == "wikipedia" }
                                    ?.url?.resource,
                                links = null,
                                artistCredit = extractArtistCredit(details),
                                artistMbIds = extractArtistMbIds(details),
                                matchedAlbumId = null,
                                matchScore = null,
                                matchedAt = null,
                                fetchedAt = System.currentTimeMillis(),
                                popularityScore = computePopularityScore(details)
                            )

                            Database.upsert(mbAlbum)
                            totalSaved++

                            if (totalSaved % 5 == 0) {
                                Timber.tag("MBAlbumFetcher")
                                    .i("Progress: saved=$totalSaved skipped=$totalSkipped failed=$totalFailed")
                            }
                        } catch (e: Exception) {
                            totalFailed++
                            Timber.tag("MBAlbumFetcher").w("Failed ${result.id}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("MBAlbumFetcher").e("Genre $genre failed: ${e.message}")
                }
            }

            Timber.tag("MBAlbumFetcher")
                .i("=== DONE: saved=$totalSaved skipped=$totalSkipped failed=$totalFailed ===")
            Timber.tag("MBAlbumFetcher").i("Total MBAlbums in DB: ${Database.count()}")
            Timber.tag("MBAlbumFetcher").i("With rating: ${Database.countWithRating()}")

            Result(totalSaved, totalSkipped, totalFailed, "OK")
        } catch (e: Exception) {
            Timber.tag("MBAlbumFetcher").e(e, "Fetcher failed")
            Result(0, 0, 0, e.message ?: "Unknown error")
        }
    }

    private fun extractArtistCredit(details: MBReleaseGroupDetailResponse): String? {
        val credits = details.artistCredit ?: return null
        return credits.joinToString("") { credit ->
            buildString {
                credit.name?.let { append(it) }
                credit.joinpath?.let { append(it) }
            }
        }.ifBlank { null }
    }

    private fun extractArtistMbIds(details: MBReleaseGroupDetailResponse): List<String>? {
        val credits = details.artistCredit ?: return null
        val ids = credits.mapNotNull { it.artist?.id }
        return ids.ifEmpty { null }
    }

    private fun computePopularityScore(details: MBReleaseGroupDetailResponse): Float {
        val rating = details.rating?.value
        val votes = (details.rating?.votesCount ?: 0).toFloat()

        val bayesianRating = if (rating != null && rating > 0) {
            val m = 5f
            val c = 3.5f
            ((votes / (votes + m)) * rating + (m / (votes + m)) * c) / 5f
        } else {
            0.5f
        }

        val tagDensity = ((details.genres.size + (details.tags?.size ?: 0)).toFloat() / 10f)
            .coerceIn(0f, 1f)

        return (bayesianRating * 0.7f + tagDensity * 0.3f).coerceIn(0f, 1f)
    }

    private fun parseYear(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank()) return null
        return dateStr.substring(0, 4).toIntOrNull()
    }

    data class Result(
        val saved: Int,
        val skipped: Int,
        val failed: Int,
        val status: String
    )
}