package it.fast4x.riplay.extensions.playlistrecommendations

import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.nextPage
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistRecommendationSource
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.findMatchingSong
import it.fast4x.spotifymeta.SpotifyTrackMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

data class PlaylistRecommendationPreferences(
    val offlineMode: Boolean = false,
    val localeKey: String = ""
)

data class PlaylistRecommendationState(
    val profile: PlaylistTasteProfile? = null,
    val items: List<Environment.SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val offline: Boolean = false,
    val canRetry: Boolean = false
)

/**
 * Observes actual playlist membership, independently of listening history. Network calls
 * only discover candidates: they never insert songs, record plays or modify the queue.
 * Sources/providers are injectable so refresh and offline behavior can be tested locally.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlaylistRecommendationService(
    scope: CoroutineScope,
    sources: Flow<List<PlaylistRecommendationSource>>,
    blacklists: Flow<List<Blacklist>>,
    dislikedSongIds: Flow<List<String>>,
    preferences: Flow<PlaylistRecommendationPreferences>,
    networkAvailable: Flow<Boolean>,
    private val resolveSeed: suspend (Song) -> String? = ::resolvePlaylistSeed,
    private val loadRelated: suspend (String) -> List<Environment.SongItem> = ::loadPlaylistNeighbors,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class CachedNeighbors(
        val videoId: String?,
        val items: List<Environment.SongItem>,
        val loadedAt: Long
    )

    private data class SeedNeighbors(val seed: Song, val neighbors: CachedNeighbors)
    private data class Request(
        val profile: PlaylistTasteProfile,
        val preferences: PlaylistRecommendationPreferences,
        val connected: Boolean,
        val refresh: Long
    )

    private val cache = LinkedHashMap<String, CachedNeighbors>()
    private val permits = Semaphore(2)
    private val refreshSignal = MutableStateFlow(0L)
    private var appliedRefresh = 0L

    private val profiles = combine(sources, blacklists, dislikedSongIds) { tracks, blocks, dislikes ->
        PlaylistTasteAnalyzer.analyze(tracks, blocks, dislikes.toSet())
    }.distinctUntilChangedBy { it.fingerprint }
        .debounce(500)

    val state: StateFlow<PlaylistRecommendationState> = combine(
        profiles, preferences.distinctUntilChanged(), networkAvailable.distinctUntilChanged(), refreshSignal
    ) { profile, prefs, connected, refresh -> Request(profile, prefs, connected, refresh) }
        .transformLatest { request ->
            if (request.refresh != appliedRefresh) {
                // transformLatest has cancelled and joined the previous generation first.
                synchronized(cache) { cache.clear() }
                appliedRefresh = request.refresh
            }
            val profile = request.profile
            if (profile.seeds.isEmpty()) {
                // Removing the last playlist must also remove the previous suggestions.
                emit(PlaylistRecommendationState(profile = profile))
                return@transformLatest
            }

            val cached = profile.seeds.mapNotNull { seed ->
                synchronized(cache) { cache[cacheKey(seed, request.preferences.localeKey)] }
                    ?.let { SeedNeighbors(seed, it) }
            }
            val cachedItems = rank(profile, cached)
            val offline = request.preferences.offlineMode || !request.connected
            emit(PlaylistRecommendationState(
                profile = profile,
                items = cachedItems,
                isLoading = !offline,
                offline = offline
            ))
            if (offline) return@transformLatest

            val responses = coroutineScope {
                profile.seeds.map { seed ->
                    async {
                        permits.withPermit {
                            SeedNeighbors(seed, neighbors(seed, request.preferences.localeKey))
                        }
                    }
                }.awaitAll()
            }
            currentCoroutineContext().ensureActive()
            val items = rank(profile, responses)
            emit(PlaylistRecommendationState(
                profile = profile,
                items = items,
                canRetry = items.isEmpty()
            ))
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), PlaylistRecommendationState())

    fun refresh() {
        refreshSignal.update { it + 1 }
    }

    private fun cacheKey(seed: Song, locale: String): String =
        "$locale|${PlaylistTasteAnalyzer.trackKey(seed).ifBlank { "id:${seed.mediaId ?: seed.id}" }}|${seed.durationText.orEmpty()}"

    private suspend fun neighbors(seed: Song, locale: String): CachedNeighbors {
        val key = cacheKey(seed, locale)
        synchronized(cache) { cache[key] }?.let { cached ->
            val ttl = if (cached.items.isEmpty()) 30_000L else 6 * 60 * 60 * 1_000L
            if (now() - cached.loadedAt < ttl) return cached
        }

        val response = try {
            withTimeoutOrNull(20_000) {
                val videoId = resolveSeed(seed)
                // Some existing API wrappers return null on cancellation. Respect the
                // parent job before accepting a result or starting another request.
                currentCoroutineContext().ensureActive()
                val items = if (videoId != null) loadRelated(videoId) else emptyList()
                currentCoroutineContext().ensureActive()
                CachedNeighbors(videoId, items.take(50), now())
            } ?: CachedNeighbors(null, emptyList(), now())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Timber.w(failure, "Could not load playlist-based recommendations")
            CachedNeighbors(null, emptyList(), now())
        }
        currentCoroutineContext().ensureActive()
        synchronized(cache) {
            cache[key] = response
            while (cache.size > 40) cache.remove(cache.keys.first())
        }
        return response
    }

    private fun rank(profile: PlaylistTasteProfile, responses: List<SeedNeighbors>): List<Environment.SongItem> {
        val resolvedSeedIds = responses.mapNotNull { it.neighbors.videoId }.toSet()
        val playable = LinkedHashMap<String, Environment.SongItem>()
        val candidates = responses.flatMap { response ->
            response.neighbors.items.mapIndexedNotNull { index, item ->
                if (item.key in resolvedSeedIds || !YOUTUBE_ID.matches(item.key) || item.title.isNullOrBlank()) {
                    return@mapIndexedNotNull null
                }
                playable.putIfAbsent(item.key, item)
                PlaylistRecommendationCandidate(
                    song = Song(
                        id = item.key,
                        title = item.title.orEmpty(),
                        artistsText = item.authors?.mapNotNull { it.name }?.joinToString(", "),
                        durationText = item.durationText,
                        thumbnailUrl = item.thumbnail?.url,
                        albumId = item.album?.endpoint?.browseId
                    ),
                    seedId = response.seed.id,
                    sourceRank = index,
                    artistIds = item.authors?.mapNotNull { it.endpoint?.browseId }?.toSet().orEmpty(),
                    albumId = item.album?.endpoint?.browseId
                )
            }
        }
        return PlaylistTasteAnalyzer.rank(profile, candidates, limit = 20)
            .mapNotNull { playable[it.song.id] }
    }
}

private val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")

private suspend fun resolvePlaylistSeed(seed: Song): String? {
    if (YOUTUBE_ID.matches(seed.id)) return seed.id
    if (seed.title.isBlank() || seed.artistsText.isNullOrBlank()) return null
    // Pending Spotify and local tracks contribute taste before they are ever played.
    // Resolve them for discovery without replacing their database rows.
    return findMatchingSong(SpotifyTrackMeta(
        id = seed.id,
        name = seed.title,
        artist = seed.artistsText,
        thumbnailUrl = seed.thumbnailUrl,
        durationMs = seed.durationText?.let(::durationTextToMillis)?.takeIf { it > 0 }
    ))?.candidate?.key
}

private suspend fun loadPlaylistNeighbors(videoId: String): List<Environment.SongItem> =
    Environment.nextPage(NextBody(videoId = videoId, isAudioOnly = true))
        ?.getOrThrow()?.itemsPage?.items.orEmpty()
