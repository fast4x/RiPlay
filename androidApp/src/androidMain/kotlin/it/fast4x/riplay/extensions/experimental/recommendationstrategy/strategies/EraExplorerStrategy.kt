package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.dao.getSongsByDecade
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class EraExplorerStrategy() : RecommendationStrategy {

    override val id: String = "era_explorer"
    override val displayName: String = "Nello stesso periodo, altro genere"
    override val displaySubtitle: String = "Cosa succedeva in altri generi quando ascoltavi"

    override suspend fun generate(
        profile: UserProfile,
        limit: Int
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.eraVector.isEmpty() || profile.keywordVector.isEmpty()) {
            return@withContext emptyList()
        }

        // Top 3 decadi per l'utente
        val topDecades = profile.eraVector.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG").d("EraExplorer: top decades = $topDecades")

        val userKeywords = profile.keywordVector.keys
        val results = mutableListOf<ScoredRecommendation>()

        for (decade in topDecades) {
            // Brani di questa decade (cerchiamo per anno su Song o Artist)
            // Necessita di una query che join Song ↔ Artist.beginYear o Album.originalYear
            val candidates = Database.songDao().getSongsByDecade(decade, limit * 3)

            for (song in candidates) {
                // Skip brani già ascoltati
                if (song.totalPlayTimeMs > 0) continue

                // Skip se generi matchano completamente (vogliamo cross-genre)
                val songGenres = song.genres.orEmpty().map { it.lowercase() }.toSet()
                val matchingGenres = songGenres.intersect(userKeywords)

                // Vogliamo ALMENO 1 match (per非 essere rilevanti) MA NON più di 2 (per essere cross-genre)
                if (matchingGenres.isEmpty() || matchingGenres.size > 2) continue

                // Cerca artista reale via cross-ref
                val artists = Database.songArtistCrossRefDao().getArtistsForSong(song.id)
                val primaryArtist = artists.firstOrNull()

                val score = scoreCandidate(song, decade, profile, matchingGenres, primaryArtist)
                results.add(score)
            }

            if (results.size >= limit * 2) break
        }

        Timber.tag("REC_DEBUG").d("EraExplorer: ${results.size} candidates")

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun scoreCandidate(
        song: Song,
        decade: Int,
        profile: UserProfile,
        matchingGenres: Set<String>,
        artist: Artist?
    ): ScoredRecommendation {
        val eraWeight = profile.eraVector[decade] ?: 0f

        // Score: era weight × matching genre weight × novelty
        val genreScore = matchingGenres
            .sumOf { (profile.keywordVector[it] ?: 0f).toDouble() }
            .toFloat()
            .coerceIn(0f, 1f)

        // Bonus per "cross-genre": se ci sono 1-2 match ma non di più, è un buon bridge
        val crossGenreBonus = when (matchingGenres.size) {
            1 -> 0.6f
            2 -> 0.4f
            else -> 0.2f
        }

        val score = (0.4f * eraWeight +
                0.3f * genreScore +
                0.3f * crossGenreBonus).coerceIn(0f, 1f)

        val reasons = buildList {
            add("Anni ${decade}s — stessa tua decade preferita")
            add("Bridge verso: ${matchingGenres.joinToString(", ")}")
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