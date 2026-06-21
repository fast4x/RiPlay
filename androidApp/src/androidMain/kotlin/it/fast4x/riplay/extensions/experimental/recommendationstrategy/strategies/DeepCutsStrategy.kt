package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
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

    override suspend fun generate(
        profile: UserProfile,
        limit: Int
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.topArtists.isEmpty()) return@withContext emptyList()

        // Prendi top 10 artisti REALI (non virtuali)
        val topArtists = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(10)

        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG").d("DeepCuts: top ${topArtists.size} artists to explore")

        val results = mutableListOf<ScoredRecommendation>()

        for (affinity in topArtists) {
            // Trova brani non ascoltati di questo artista
            val unplayedSongs = Database.songArtistCrossRefDao().getUnplayedSongsByArtist(
                artistId = affinity.artistId,
                limit = 5
            )

            for (song in unplayedSongs) {
                val score = scoreCandidate(song, affinity)
                if (score.score > 0.2f) {
                    results.add(score)
                }
            }

            if (results.size >= limit * 2) break  // early stop
        }

        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG").d("DeepCuts: ${results.size} candidates, returning top $limit")

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun scoreCandidate(
        song: Song,
        artistAffinity: ArtistAffinity
    ): ScoredRecommendation {
        // Score basato su:
        // - Affinità artista (più alto = più rilevante)
        // - Brano non ascoltato = puro discovery
        // - Piccolo bonus per brani con generi matching

        val artistScore = artistAffinity.score  // già 0..1

        // Bonus: se il brano ha generi, controlla se matchano profilo (impossibile qui senza profile, ma possiamo usare artist keywords)
        val score = (0.7f * artistScore + 0.3f * 0.5f).coerceIn(0f, 1f)  // 0.5 base per "non ascoltato"

        val reasons = buildList {
            add("Brano non ancora ascoltato di un artista che ami")
            add("Affinità artista: ${(artistScore * 100).toInt()}%")
        }

        return ScoredRecommendation(
            song = song,
            album = null,
            artist = null,  // potresti caricarlo via DAO se serve
            score = score,
            reasons = reasons,
            strategyId = id
        )
    }
}