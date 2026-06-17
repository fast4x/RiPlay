package it.fast4x.riplay.extensions.experimental.recommendationstrategy

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.UserArtistAffinity
import it.fast4x.riplay.data.models.UserEraAffinity
import it.fast4x.riplay.data.models.UserKeywordAffinity
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.builders.UserProfileBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class UserProfileRepository(
    private val builder: UserProfileBuilder
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rebuildMutex = Mutex()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    // Lascia traccia dell'ultimo refresh per update incrementale
    private var lastRefreshedAt: Long = 0L

    /**
     * Ricostruisce il profilo da zero. Da chiamare periodicamente (job notturno)
     * o quando si vuole ripartire da una base pulita.
     */
    suspend fun rebuildFull(userId: String = RecommendationConstants.USER_ID_SELF) {
        rebuildMutex.withLock {
            val profile = builder.build(userId, since = 0L)
            persist(profile)
            _profile.value = profile
            lastRefreshedAt = profile.lastRefreshedAt
        }
    }

    /**
     * Aggiornamento incrementale: processa solo eventi più recenti dell'ultimo refresh.
     * Da chiamare on-demand dopo ogni evento di ascolto (debounced).
     */
    suspend fun refreshIncremental(userId: String = RecommendationConstants.USER_ID_SELF) {
        rebuildMutex.withLock {
            val since = lastRefreshedAt
            val updated = builder.build(userId, since = since)

            // Merge con il profilo esistente (somma pesata per evitare perdita segnale)
            val merged = _profile.value?.let { current -> mergeProfiles(current, updated) } ?: updated

            persist(merged)
            _profile.value = merged
            lastRefreshedAt = merged.lastRefreshedAt
        }
    }

    /**
     * Applica un singolo evento in modo fire-and-forget. Utile per il online update
     * quando l'evento viene registrato durante il playback.
     */
    fun applyEventAsync(event: Event) {
        scope.launch {
            // Debounce naturale: il rebuild legge tutti gli eventi da lastRefreshedAt
            // quindi eventi multipli ravvicinati vengono aggregati in un solo rebuild
            refreshIncremental()
        }
    }

    private suspend fun persist(profile: UserProfile) {
        val userId = profile.userId
        val now = profile.lastRefreshedAt

        // Pulizia + reinserimento (più semplice che fare diff)
        Database.deleteEraAffinityAllForUser(userId)

        Database.upsertUserArtistAffinity(
            profile.topArtists.map { aff ->
                UserArtistAffinity(
                    userId = userId,
                    artistId = aff.artistId,
                    score = aff.score,
                    playCount = aff.playCount,
                    totalPlayTimeMs = 0L,  // TODO: tracciare separatamente se serve
                    lastPlayedAt = now,
                    likedSongs = 0,
                    dislikedSongs = 0,
                    bookmarked = aff.artistId in profile.bookmarkedArtistIds,
                    updatedAt = now
                )
            }
        )

        Database.upsertUserKeywordAffinity(
            profile.keywordVector.entries.map { (kw, weight) ->
                UserKeywordAffinity(
                    userId = userId,
                    keyword = kw,
                    weight = weight,
                    playCount = 0,
                    updatedAt = now
                )
            }
        )

        Database.upsertUserEraAffinity(
            profile.eraVector.entries.map { (decade, weight) ->
                UserEraAffinity(
                    userId = userId,
                    decade = decade,
                    weight = weight,
                    playCount = 0,
                    updatedAt = now
                )
            }
        )
    }

    /**
     * Merge di un profilo esistente con un update incrementale.
     * Strategia: per le keyword facciamo media pesata; per gli artisti prendiamo max(score).
     */
    private fun mergeProfiles(current: UserProfile, incremental: UserProfile): UserProfile {
        val mergedKeywords = (current.keywordVector.keys + incremental.keywordVector.keys)
            .associateWith { kw ->
                val current_w = current.keywordVector[kw] ?: 0f
                val incr_w = incremental.keywordVector[kw] ?: 0f
                // Pesiamo l'incrementale di meno perché è solo un delta
                (current_w * 0.7f + incr_w * 0.3f)
            }
            .filterValues { it > 0.01f }

        val mergedEras = (current.eraVector.keys + incremental.eraVector.keys)
            .associateWith { decade ->
                val current_w = current.eraVector[decade] ?: 0f
                val incr_w = incremental.eraVector[decade] ?: 0f
                maxOf(current_w, incr_w)
            }

        val mergedArtists = (current.topArtists + incremental.topArtists)
            .groupBy { it.artistId }
            .mapValues { (_, list) ->
                list.maxByOrNull { it.score }!!.copy(
                    score = list.maxOf { it.score },
                    playCount = list.sumOf { it.playCount }
                )
            }
            .values
            .sortedByDescending { it.score }
            .take(50)

        return UserProfile(
            userId = current.userId,
            topArtists = mergedArtists,
            keywordVector = mergedKeywords,
            eraVector = mergedEras,
            bookmarkedArtistIds = current.bookmarkedArtistIds + incremental.bookmarkedArtistIds,
            bookmarkedAlbumIds = current.bookmarkedAlbumIds + incremental.bookmarkedAlbumIds,
            lastRefreshedAt = incremental.lastRefreshedAt
        )
    }

    /**
     * Carica il profilo dal DB all'avvio dell'app.
     * Chiamare in Application.onCreate.
     */
    suspend fun loadFromDb(userId: String = RecommendationConstants.USER_ID_SELF) {
        val artists = Database.getTopArtists(userId, 50)
        val keywords = Database.getTopKeywords(userId, 1000)
        val eras = Database.getAll(userId)

        if (artists.isEmpty() && keywords.isEmpty() && eras.isEmpty()) {
            // Nessun profilo persistito — resta null, verrà creato al primo rebuild
            return
        }

        val profile = UserProfile(
            userId = userId,
            topArtists = artists.map {
                ArtistAffinity(
                    artistId = it.artistId,
                    score = it.score,
                    playCount = it.playCount
                )
            },
            keywordVector = keywords.associate { it.keyword to it.weight },
            eraVector = eras.associate { it.decade to it.weight },
            bookmarkedArtistIds = artists.filter { it.bookmarked }.map { it.artistId }.toSet(),
            bookmarkedAlbumIds = emptySet(),  // non persistito per ora
            lastRefreshedAt = artists.maxOfOrNull { it.updatedAt } ?: 0L
        )

        _profile.value = profile
        lastRefreshedAt = profile.lastRefreshedAt
        Timber.tag("REC_DEBUG")
            .d("Profile loaded from DB: ${artists.size} artists, ${keywords.size} keywords")
    }
}