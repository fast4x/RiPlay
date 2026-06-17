package it.fast4x.riplay.extensions.experimental.recommendationstrategy

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Recommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui.RecommendationSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import timber.log.Timber

class RecommendationService(
    private val profileRepo: UserProfileRepository,
    private val strategies: List<RecommendationStrategy>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    private val _sections = MutableStateFlow<List<RecommendationSection>>(emptyList())
    val sections: StateFlow<List<RecommendationSection>> = _sections.asStateFlow()

    /**
     * Solo le sottosezioni non vuote, pronte per l'UI.
     */
    val visibleSections: StateFlow<List<RecommendationSection>> =
        _sections.map { all -> all.filter { it.items.isNotEmpty() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Gating: l'utente ha abbastanza storico per ricevere suggerimenti?
     */
    val shouldShowSection: StateFlow<Boolean> =
        combine(profileRepo.profile, _sections) { profile, sections ->
            if (profile == null) return@combine false

            val enoughSongs = profile.topArtists.isNotEmpty() || hasEnoughHistory()
            val enoughArtists = profile.topArtists.size >= 3 // proxy senza cross-ref
            val hasNonEmptySection = sections.any { it.items.isNotEmpty() }

            Timber.d("RecommendationService shouldShowSection topArtists ${profile.topArtists} enoughArtists $enoughArtists sections $sections ")

            enoughSongs && enoughArtists && hasNonEmptySection
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Rigenera tutte le strategie. Chiamato:
     * - All'avvio (se profilo esiste già)
     * - Dopo un rebuild del profilo
     * - Su refresh manuale dall'UI (pull-to-refresh)
     */
    suspend fun refreshAll(userId: String = RecommendationConstants.USER_ID_SELF) {
        refreshMutex.withLock {
            // Assicurati che il profilo sia aggiornato
            val profile = profileRepo.profile.value ?: run {
                profileRepo.rebuildFull(userId)
                profileRepo.profile.value
            } ?: return@withLock

            // Genera tutte le strategie in parallelo
            val sections = strategies.map { strategy ->
                scope.async {
                    generateSection(strategy, profile, userId, limit = 10)
                }
            }.awaitList()

            _sections.value = sections
        }
    }

    private suspend fun generateSection(
        strategy: RecommendationStrategy,
        profile: UserProfile,
        userId: String,
        limit: Int
    ): RecommendationSection {
        return try {
            val recommendations = strategy.generate(profile, limit)

            // Persist per tracking consumed/rejected
            val now = System.currentTimeMillis()
            Database.deleteByStrategy(userId, strategy.id)
            Database.upsertRecommendation(
                recommendations.map { rec ->
                    Recommendation(
                        userId = userId,
                        songId = rec.song?.id ?: rec.album?.id ?: rec.artist?.id ?: "",
                        strategyId = strategy.id,
                        score = rec.score,
                        reasonsJson = encodeReasons(rec.reasons),
                        generatedAt = now
                    )
                }
            )

            RecommendationSection(
                id = strategy.id,
                title = strategy.displayName,
                subtitle = strategy.displaySubtitle,
                items = recommendations,
                updatedAt = now
            )
        } catch (e: Exception) {
            // Log.error("Strategy ${strategy.id} failed", e)
            RecommendationSection(
                id = strategy.id,
                title = strategy.displayName,
                subtitle = strategy.displaySubtitle,
                items = emptyList(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Marca un suggerimento come consumato (utente ha avviato il brano).
     */
    suspend fun markConsumed(strategyId: String, songId: String) {
        Database.markConsumed(
            userId = RecommendationConstants.USER_ID_SELF,
            songId = songId,
            strategyId = strategyId,
            now = System.currentTimeMillis()
        )
    }

    /**
     * Marca come rifiutato (utente ha detto "non interessato").
     */
    suspend fun markRejected(songId: String) {
        Database.markRejected(
            userId = RecommendationConstants.USER_ID_SELF,
            songId = songId,
            now = System.currentTimeMillis()
        )
        // Rimuovi dal flow in-memory per feedback immediato
        _sections.value = _sections.value.map { section ->
            section.copy(items = section.items.filterNot { it.song?.id == songId })
        }
    }

    private suspend fun hasEnoughHistory(): Boolean {
        return try {
            val count = Database.countDistinctPlayedSongs() ?: 0
            count >= RecommendationConstants.MIN_SONGS_PLAYED
        } catch (e: Exception) {
            false
        }
    }

    private fun encodeReasons(reasons: List<String>): String {
        // Stesso formato di RecommendationConverters
        return reasons.joinToString(separator = "||") { it.replace("|", "") }
    }

    private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitList(): List<T> =
        awaitAll()
}