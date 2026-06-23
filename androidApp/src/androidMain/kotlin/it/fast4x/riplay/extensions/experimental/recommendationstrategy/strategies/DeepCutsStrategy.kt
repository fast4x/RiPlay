package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ArtistAffinity
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class DeepCutsStrategy() : RecommendationStrategy {

    override val id: String = "deep_cuts"
    override val displayName: String = "Tracce da scoprire"
    override val displaySubtitle: String = "Brani meno noti di artisti che ami"


    val songArtistRefDao = Database.songArtistCrossRefDao()
    val songDao = Database.songDao()
    val artistDao = Database.artistDao()

    override suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String>
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.topArtists.isEmpty()) return@withContext emptyList()

        val topArtists = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(10)

        val results = mutableListOf<ScoredRecommendation>()

        for (affinity in topArtists) {
            val unplayedSongs = songArtistRefDao.getUnplayedSongsByArtist(
                artistId = affinity.artistId,
                limit = 5
            )

            val artist = artistDao.getById(affinity.artistId)

            for (song in unplayedSongs) {
                if (song.id in excludedIds) continue  // ★ filtro excluded

                val score = scoreCandidate(song, affinity, artist)
                if (score.score > 0.2f) {
                    results.add(score)
                }
            }

            if (results.size >= limit * 2) break
        }

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun scoreCandidate(
        song: Song,
        artistAffinity: ArtistAffinity,
        artist: Artist?
    ): ScoredRecommendation {
        val artistScore = artistAffinity.score
        val score = (0.7f * artistScore + 0.3f * 0.5f).coerceIn(0f, 1f)

        val reasons = buildList {
            add("Brano non ancora ascoltato di un artista che ami")
            add("Affinità artista: ${(artistScore * 100).toInt()}%")
        }

        return ScoredRecommendation(
            song = song,
            album = null,
            artist = artist,
            score = score,
            reasons = reasons,
            strategyId = id
        )
    }
}