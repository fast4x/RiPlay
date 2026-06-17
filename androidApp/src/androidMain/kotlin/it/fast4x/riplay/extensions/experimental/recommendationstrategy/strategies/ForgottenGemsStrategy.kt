package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationConstants
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoringUtils
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForgottenGemsStrategy() : RecommendationStrategy {

    override val id: String = "forgotten_gems"
    override val displayName: String = "Dimenticati nel tempo"
    override val displaySubtitle: String = "Brani che amavi ma non ascolti da mesi"

    override suspend fun generate(
        profile: UserProfile,
        limit: Int
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        val now = System.currentTimeMillis()
        val olderThan = now - (RecommendationConstants.FORGOTTEN_GEMS_MIN_AGE_DAYS * 24L * 3_600_000L)

        // Prendi più candidati del necessario, poi filtra e ordina
        //val candidates = Database.getLikedSongsNotPlayedSince(olderThan, limit * 5)
        val candidates = Database.getForgottenSongs(
            olderThan = olderThan,
            minTotalPlayMs = 60_000L,  // soglia: brano degno di nota
            limit = limit * 5
        )

        candidates
            .map { song -> scoreCandidate(song, profile, now) }
            .filter { it.score > 0.1f }      // soglia minima di rilevanza
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun scoreCandidate(
        song: Song,
        profile: UserProfile,
        now: Long
    ): ScoredRecommendation {
        val lastPlayedAt = Database.getLastPlayedAt(song.id)

        // Score basato su:
        // - Tempo passato dall'ultimo ascolto (più tempo = più "dimenticato")
        // - Quanto era amato (totalPlayTimeMs)
        // - Match con profilo attuale (se abbiamo keywords)

        val ageDays = lastPlayedAt?.let { (now - it) / (24L * 3_600_000L) } ?: 365L
        val forgottenBonus = when {
            ageDays > 365 -> 1.0f
            ageDays > 180 -> 0.8f
            ageDays > 90 -> 0.6f
            else -> 0.3f
        }

        // Quanto era amato: totalPlayTimeMs in ore
        val playHours = song.totalPlayTimeMs / 3_600_000f
        val loveAffinity = (playHours / 5f).coerceIn(0f, 1f)  // 5h = massimo amore

        // Match con profilo (anche con keywordVector vuoto, score è 0 — non rompe)
        val keywordScore = ScoringUtils.keywordSimilarity(song.genres, profile.keywordVector)

        val score = (0.4f * forgottenBonus +
                0.4f * loveAffinity +
                0.2f * keywordScore).coerceIn(0f, 1f)

        val reasons = buildList {
            add("Non lo ascolti da ${ageDays} giorni")
            if (playHours > 1f) add("Lo ascoltavi molto in passato")
            if (keywordScore > 0.3f) add("Match con i tuoi gusti attuali")
        }

        return ScoredRecommendation(
            song = song,
            album = null,
            artist = null,
            score = score,
            reasons = reasons,
            strategyId = id
        )
    }
}