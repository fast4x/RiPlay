package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.AlbumClassifier
import it.fast4x.riplay.extensions.musicbrainz.fetchers.NewReleasesFetcher
import it.fast4x.riplay.extensions.musicbrainz.models.MBReleaseGroupDetailResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NewReleasesFetcherWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WorkerDependencies.isInitialized) return Result.retry()

        return try {

            val fetcher = NewReleasesFetcher()
            fetcher.fetch()
            /*
            Timber.tag("NewReleasesWorker").i("=== START ===")

            val profile = WorkerDependencies.profileRepository.profile.value
                ?: return Result.retry()

            val now = LocalDate.now()
            val oneWeekAgo = now.minusDays(7).format(DateTimeFormatter.ISO_DATE)

            var totalSaved = 0
            var totalFailed = 0

            // === 1. Fetch novità globali per top generi ===
            val topGenres = profile.keywordVector.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }

            for (genre in topGenres) {
                try {
                    delay(1100)
                    // Cerca album recenti con questo tag
                    val results = WorkerDependencies.mbClient.searchAlbumsByGenre(
                        genre = genre,
                        limit = 30
                    )

                    for (result in results) {
                        try {
                            delay(1100)
                            val details = WorkerDependencies.mbClient.getReleaseGroupDetailWithArtist(result.id)

                            // Salta se già presente
                            if (WorkerDependencies.database.getMBAlbumById(result.id) != null) continue

                            // Salta se non ha data
                            if (details.firstReleaseDate.isNullOrBlank()) continue

                            // Salta se più vecchio di 90 giorni
                            val releaseDate = LocalDate.parse(details.firstReleaseDate.take(10))
                            if (releaseDate.isBefore(now.minusDays(90))) continue

                            val mbAlbum = MBAlbum(
                                id = details.id,
                                title = details.title,
                                primaryType = details.primaryType,
                                secondaryTypes = details.secondaryTypes.takeIf { it.isNotEmpty() },
                                firstReleaseDate = details.firstReleaseDate,
                                originalYear = details.firstReleaseDate.take(4).toIntOrNull(),
                                genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
                                tags = details.tags?.map { it.name }?.takeIf { it.isNotEmpty() },
                                rating = details.rating?.value,
                                ratingVotes = details.rating?.votesCount,
                                wikipediaUrl = details.relations
                                    ?.find { it.type == "wikipedia" }
                                    ?.url?.resource,
                                artistCredit = extractArtistCredit(details),
                                fetchedAt = System.currentTimeMillis(),
                                popularityScore = computePopularityScore(details),
                                nature = AlbumClassifier.classify(
                                    Album(
                                        id = details.id,
                                        title = details.title,
                                        genres = details.genres.map { it.name },
                                        tags = details.tags?.map { it.name },
                                        albumType = details.primaryType
                                    )
                                )
                            )

                            WorkerDependencies.database.upsert(mbAlbum)
                            totalSaved++
                        } catch (e: Exception) {
                            totalFailed++
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("NewReleasesWorker").w("Genre $genre failed: ${e.message}")
                }
            }

            // === 2. Fetch novità per top artisti (i loro ultimi album) ===
            val topArtistMbIds = profile.topArtists
                .filter { !it.artistId.startsWith("virtual::") }
                .take(10)
                .mapNotNull { WorkerDependencies.database.artist(it.artistId).first()?.mbId }

            for (artistMbId in topArtistMbIds) {
                try {
                    delay(1100)
                    val recentReleases = WorkerDependencies.mbClient.searchReleaseGroupsByArtist(
                        artistMbId = artistMbId,
                        limit = 5
                    )

                    for (result in recentReleases) {
                        delay(1100)
                        val details = WorkerDependencies.mbClient.getReleaseGroupDetailWithArtist(result.id)

                        if (WorkerDependencies.database.getMBAlbumById(result.id) != null) continue
                        if (details.firstReleaseDate.isNullOrBlank()) continue

                        val releaseDate = LocalDate.parse(details.firstReleaseDate.take(10))
                        if (releaseDate.isBefore(now.minusDays(180))) continue  // 6 mesi per artisti preferiti

                        val mbAlbum = MBAlbum(
                            id = details.id,
                            title = details.title,
                            primaryType = details.primaryType,
                            secondaryTypes = details.secondaryTypes.takeIf { it.isNotEmpty() },
                            firstReleaseDate = details.firstReleaseDate,
                            originalYear = details.firstReleaseDate.take(4).toIntOrNull(),
                            genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
                            tags = details.tags?.map { it.name }?.takeIf { it.isNotEmpty() },
                            rating = details.rating?.value,
                            ratingVotes = details.rating?.votesCount,
                            artistCredit = extractArtistCredit(details),
                            fetchedAt = System.currentTimeMillis(),
                            popularityScore = computePopularityScore(details),
                            nature = AlbumClassifier.classify(
                                Album(
                                    id = details.id,
                                    title = details.title,
                                    genres = details.genres.map { it.name },
                                    tags = details.tags?.map { it.name },
                                    albumType = details.primaryType
                                )
                            )
                        )

                        WorkerDependencies.database.upsert(mbAlbum)
                        totalSaved++
                    }
                } catch (e: Exception) {
                    Timber.tag("NewReleasesWorker").w("Artist $artistMbId failed: ${e.message}")
                }
            }

            Timber.tag("NewReleasesWorker")
                .i("=== DONE: saved=$totalSaved, failed=$totalFailed ===")
            */
            Result.success()
        } catch (e: Exception) {
            Timber.tag("NewReleasesWorker").e(e, "Failed")
            Result.retry()
        }
    }

    private fun extractArtistCredit(details: MBReleaseGroupDetailResponse): String? {
        // Se hai aggiunto artistCredit al data class, usalo
        // Altrimenti lascia null e verrà recuperato on-demand
        return null
    }

    private fun computePopularityScore(details: MBReleaseGroupDetailResponse): Float {
        val rating = details.rating?.value
        val votes = (details.rating?.votesCount ?: 0).toFloat()
        val bayesianRating = if (rating != null && rating > 0) {
            ((votes / (votes + 5f)) * rating + (5f / (votes + 5f)) * 3.5f) / 5f
        } else 0.5f
        val tagDensity = ((details.genres.size + (details.tags?.size ?: 0)).toFloat() / 10f).coerceIn(0f, 1f)
        return (bayesianRating * 0.7f + tagDensity * 0.3f).coerceIn(0f, 1f)
    }
}