package it.fast4x.riplay.extensions.experimental.recommendationstrategy

import android.util.Log
import it.fast4x.riplay.BuildConfig
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

            if (BuildConfig.DEBUG)
                Timber.d("RecommendationService shouldShowSection topArtists ${profile.topArtists} enoughArtists $enoughArtists sections $sections ")

            enoughSongs && enoughArtists && hasNonEmptySection
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    val recommendationDao = Database.recommendationDao()

    /**
     * Rigenera tutte le strategie. Chiamato all'avvio, dopo rebuild profilo,
     * o su refresh manuale dall'UI.
     */
    suspend fun refreshAll(userId: String = RecommendationConstants.USER_ID_SELF) {
        refreshMutex.withLock {
            val profile = profileRepo.profile.value ?: run {
                profileRepo.rebuildFull(userId)
                profileRepo.profile.value
            } ?: return@withLock

            val now = System.currentTimeMillis()
            // Cutoff: 14 giorni per consumati, 30 giorni per rejected
            val consumedCutoff = now - (14L * 24 * 3600 * 1000)
            val rejectedCutoff = now - (30L * 24 * 3600 * 1000)

            // Genera tutte le strategie in parallelo
            val sections = strategies.map { strategy ->
                scope.async {
                    generateSection(
                        strategy = strategy,
                        profile = profile,
                        userId = userId,
                        limit = 10,
                        consumedCutoff = consumedCutoff,
                        rejectedCutoff = rejectedCutoff
                    )
                }
            }.awaitList()

            _sections.value = sections
        }
    }

    private suspend fun generateSection(
        strategy: RecommendationStrategy,
        profile: UserProfile,
        userId: String,
        limit: Int,
        consumedCutoff: Long,
        rejectedCutoff: Long
    ): RecommendationSection {
        return try {
            val now = System.currentTimeMillis()

            val excludedIds = recommendationDao.getRecentlyConsumedOrRejectedIds(
                userId = userId,
                consumedCutoffMs = consumedCutoff,
                rejectedCutoffMs = rejectedCutoff
            ).toSet()

            Timber.tag("REC_DEBUG")
                .d("Strategy ${strategy.id}: excludedIds size = ${excludedIds.size}")
            if (excludedIds.isNotEmpty()) {
                Timber.tag("REC_DEBUG").d("  Excluded: ${excludedIds.take(5)}")
            }

            val candidates = strategy.generate(profile, limit * 3, excludedIds)
            Timber.tag("REC_DEBUG")
                .d("Strategy ${strategy.id}: ${candidates.size} candidates after filtering")

            // Persist per tracking futuro
            recommendationDao.deleteByStrategy(userId, strategy.id)
            recommendationDao.upsertRecommendation(
                candidates.map { rec ->
                    val itemId = rec.song?.id ?: rec.album?.id ?: rec.artist?.id ?: ""
                    Recommendation(
                        userId = userId,
                        songId = itemId,
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
                items = candidates.take(limit),  // ★ take limit qui perché la strategia può restituire più del necessario
                updatedAt = now
            )
        } catch (e: Exception) {
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
     * Marca come consumato (utente ha aperto il brano).
     */
    suspend fun markConsumed(strategyId: String, itemId: String) {

        Timber.tag("REC_DEBUG").d("markConsumed called: strategyId=$strategyId, itemId=$itemId")

        val userId = RecommendationConstants.USER_ID_SELF
        val now = System.currentTimeMillis()

        // Verifica se la riga esiste prima dell'update
        val exists = recommendationDao.checkIfExists(userId, strategyId, itemId)
        Timber.tag("REC_DEBUG").d("  Row exists before update? $exists")

        recommendationDao.markConsumed(userId, strategyId, itemId, now)

        // Verifica dopo
        val consumed = recommendationDao.checkIfConsumed(userId, strategyId, itemId)
        Timber.tag("REC_DEBUG").d("  Consumed after update? $consumed")

    }

    /**
     * Marca come rejected (utente ha detto "Non mi interessa").
     * Aggiorna tutte le strategie per quell'item.
     */
    suspend fun markRejected(itemId: String) {
        recommendationDao.markRejected(
            userId = RecommendationConstants.USER_ID_SELF,
            itemId = itemId,
            now = System.currentTimeMillis()
        )
        // Rimuovi dal flow in-memory per feedback immediato
        _sections.value = _sections.value.map { section ->
            section.copy(items = section.items.filterNot { item ->
                val id = item.song?.id ?: item.album?.id ?: item.artist?.id
                id == itemId
            })
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