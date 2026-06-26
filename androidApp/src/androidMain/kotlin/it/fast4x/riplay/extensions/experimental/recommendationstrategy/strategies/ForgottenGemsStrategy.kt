package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationConstants
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.utils.ScoringUtils
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForgottenGemsStrategy() : RecommendationStrategy {

    override val id: String = "forgotten_gems"
    override val displayName: String = "Dimenticati nel tempo"
    override val displaySubtitle: String = "Brani che amavi ma non ascolti da mesi"

    val songDao = Database.songDao()
    val artistDao = Database.artistDao()

    override suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String>
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        val now = System.currentTimeMillis()
        val olderThan = now - (RecommendationConstants.FORGOTTEN_GEMS_MIN_AGE_DAYS * 24L * 3_600_000L)

        val candidates = songDao.getForgottenSongs(
            olderThan = olderThan,
            minTotalPlayMs = 60_000L,
            limit = limit * 5
        )

        candidates
            .filter { it.id !in excludedIds }  // ★ filtro excluded
            .map { song -> scoreCandidate(song, profile, now) }
            .filter { it.score > 0.1f }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun scoreCandidate(
        song: Song,
        profile: UserProfile,
        now: Long
    ): ScoredRecommendation {
        val lastPlayedAt = songDao.getLastPlayedAt(song.id)

        val ageDays = lastPlayedAt?.let { (now - it) / (24L * 3_600_000L) } ?: 365L
        val forgottenBonus = when {
            ageDays > 365 -> 1.0f
            ageDays > 180 -> 0.8f
            ageDays > 90 -> 0.6f
            else -> 0.3f
        }

        val playHours = song.totalPlayTimeMs / 3_600_000f
        val loveAffinity = (playHours / 5f).coerceIn(0f, 1f)

        val keywordScore = ScoringUtils.keywordSimilarity(song.genres, profile.keywordVector)

        val artist = song.artistsText?.trim()?.let { artistName ->
            val realArtist = artistDao.findByNameExactIgnoreCase(artistName)
            realArtist ?: Artist(
                id = "virtual::$artistName",
                name = artistName,
                timestamp = System.currentTimeMillis(),
                isYoutubeArtist = false,
                nature = ArtistNature.UNKNOWN
            )
        }

        val artistMatchBonus = artist?.let { a ->
            val virtualId = a.id
            if (profile.topArtists.any { it.artistId == virtualId }) 0.2f else 0f
        } ?: 0f

        val score = (0.3f * forgottenBonus +
                0.3f * loveAffinity +
                0.2f * keywordScore +
                0.2f * artistMatchBonus).coerceIn(0f, 1f)

        val reasons = buildList {
            add("Non lo ascolti da ${ageDays.toInt()} giorni")
            if (playHours > 1f) add("Lo ascoltavi molto in passato")
            if (keywordScore > 0.3f) add("Match con i tuoi gusti attuali")
            if (artistMatchBonus > 0f) add("Artista che ami")
        }

        return ScoredRecommendation(
            song = song,
            album = null,
            artist = artist,
            score = score,
            reasons = reasons,
            strategyId = id,
            strategyDisplayName = displayName
        )
    }
}