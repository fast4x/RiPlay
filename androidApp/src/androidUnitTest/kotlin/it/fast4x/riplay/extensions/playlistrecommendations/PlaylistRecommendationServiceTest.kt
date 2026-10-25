package it.fast4x.riplay.extensions.playlistrecommendations

import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistRecommendationSource
import it.fast4x.riplay.data.models.Song
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistRecommendationServiceTest {
    @Test
    fun oneImportedPlaylistGeneratesDiscoveriesWithoutListeningHistory() = runBlocking {
        withFixture { fixture ->
            val state = fixture.awaitState { !it.isLoading && it.items.any { item -> item.key == videoId(2) } }

            assertEquals(1, state.profile?.playlistCount)
            assertEquals(1, state.profile?.songCount)
            assertEquals(0L, fixture.sources.value.single().song.totalPlayTimeMs)
            assertEquals(1, fixture.resolveCalls.get())
            assertEquals(1, fixture.loadCalls.get())
            assertFalse(state.offline)
        }
    }

    @Test
    fun offlineSkipsProvidersThenConnectionLoadsAndDisconnectionKeepsCachedDiscoveries() = runBlocking {
        withFixture(connected = false) { fixture ->
            val offline = fixture.awaitState { it.profile != null && it.offline && !it.isLoading }
            assertTrue(offline.items.isEmpty())
            assertEquals(0, fixture.resolveCalls.get())
            assertEquals(0, fixture.loadCalls.get())

            fixture.connected.value = true
            val online = fixture.awaitState { !it.offline && !it.isLoading && it.items.isNotEmpty() }
            val discoveredIds = online.items.map { it.key }
            assertEquals(1, fixture.resolveCalls.get())
            assertEquals(1, fixture.loadCalls.get())

            fixture.connected.value = false
            val cached = fixture.awaitState { it.offline && it.items.map { item -> item.key } == discoveredIds }
            assertFalse(cached.isLoading)
            assertEquals(1, fixture.resolveCalls.get())
            assertEquals(1, fixture.loadCalls.get())
        }
    }

    @Test
    fun explicitOfflinePreferencePreventsNetworkEvenWithConnectivity() = runBlocking {
        withFixture(offlineMode = true) { fixture ->
            fixture.awaitState { it.profile != null && it.offline && !it.isLoading }
            assertEquals(0, fixture.loadCalls.get())

            fixture.preferences.value = PlaylistRecommendationPreferences(offlineMode = false)
            fixture.awaitState { !it.offline && !it.isLoading && it.items.isNotEmpty() }
            assertEquals(1, fixture.loadCalls.get())
        }
    }

    @Test
    fun removingTheLastPlaylistMemberClearsPreviousDiscoveries() = runBlocking {
        withFixture { fixture ->
            fixture.awaitState { !it.isLoading && it.items.isNotEmpty() }

            fixture.sources.value = emptyList()
            val cleared = fixture.awaitState { it.profile?.songCount == 0 && it.items.isEmpty() }

            assertFalse(cleared.isLoading)
            assertEquals(1, fixture.loadCalls.get())
        }
    }

    @Test
    fun newDislikeRemovesCachedCandidateWithoutAnotherProviderRequest() = runBlocking {
        withFixture { fixture ->
            fixture.awaitState { !it.isLoading && it.items.map { item -> item.key }.toSet() == setOf(videoId(2), videoId(3)) }

            fixture.dislikedIds.value = listOf(videoId(2))
            val filtered = fixture.awaitState {
                !it.isLoading && videoId(2) in it.profile?.blockedSongIds.orEmpty() &&
                    it.items.map { item -> item.key } == listOf(videoId(3))
            }

            assertTrue(filtered.items.none { it.key == videoId(2) })
            assertEquals(1, fixture.resolveCalls.get())
            assertEquals(1, fixture.loadCalls.get())
        }
    }

    @Test
    fun manualRefreshLoadsNewCandidatesInsteadOfReusingPositiveCache() = runBlocking {
        withFixture { fixture ->
            fixture.awaitState { !it.isLoading && it.items.any { item -> item.key == videoId(2) } }
            fixture.related.set(listOf(neighbor(4)))

            fixture.service.refresh()
            val refreshed = fixture.awaitState {
                !it.isLoading && it.items.map { item -> item.key } == listOf(videoId(4))
            }

            assertEquals(listOf(videoId(4)), refreshed.items.map { it.key })
            assertEquals(2, fixture.resolveCalls.get())
            assertEquals(2, fixture.loadCalls.get())
        }
    }

    private suspend fun withFixture(
        connected: Boolean = true,
        offlineMode: Boolean = false,
        test: suspend (Fixture) -> Unit
    ) {
        val lifecycle = SupervisorJob()
        val scope = CoroutineScope(lifecycle + Dispatchers.Default)
        val fixture = Fixture(scope, connected, offlineMode)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { fixture.service.state.collect {} }
        try {
            test(fixture)
        } finally {
            scope.cancel()
            lifecycle.join()
        }
    }

    private class Fixture(scope: CoroutineScope, connected: Boolean, offlineMode: Boolean) {
        val sources = MutableStateFlow(listOf(
            PlaylistRecommendationSource(
                song = Song(
                    id = "spotify:pending-track",
                    title = "Imported song",
                    artistsText = "Seed Artist",
                    durationText = "3:00",
                    thumbnailUrl = null
                ),
                playlistId = 1,
                playlistName = "Imported playlist"
            )
        ))
        val dislikedIds = MutableStateFlow<List<String>>(emptyList())
        val preferences = MutableStateFlow(PlaylistRecommendationPreferences(offlineMode = offlineMode))
        val connected = MutableStateFlow(connected)
        val related = AtomicReference(listOf(neighbor(2), neighbor(3)))
        val resolveCalls = AtomicInteger()
        val loadCalls = AtomicInteger()

        val service = PlaylistRecommendationService(
            scope = scope,
            sources = sources,
            blacklists = MutableStateFlow<List<Blacklist>>(emptyList()),
            dislikedSongIds = dislikedIds,
            preferences = preferences,
            networkAvailable = this.connected,
            resolveSeed = {
                resolveCalls.incrementAndGet()
                videoId(1)
            },
            loadRelated = {
                loadCalls.incrementAndGet()
                related.get()
            },
            now = { 1_000_000L }
        )

        suspend fun awaitState(predicate: (PlaylistRecommendationState) -> Boolean): PlaylistRecommendationState =
            withTimeout(4_000L) { service.state.first { predicate(it) } }
    }

    companion object {
        private fun videoId(number: Int) = "video${number.toString().padStart(6, '0')}"

        private fun neighbor(number: Int) = Environment.SongItem(
            info = Environment.Info(
                name = "Discovery $number",
                endpoint = NavigationEndpoint.Endpoint.Watch(videoId = videoId(number))
            ),
            authors = listOf(Environment.Info(
                name = "Seed Artist",
                endpoint = NavigationEndpoint.Endpoint.Browse(browseId = "seed-artist")
            )),
            album = null,
            durationText = "3:00",
            thumbnail = null
        )
    }
}
