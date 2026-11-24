/*
 * riplay-bridge — Test E2E su JVM: Ktor testApplication + catalogo in memoria.
 *
 * Verifica l'intero comportamento visibile dal provider Music Assistant:
 * ping pubblico, 401 senza token, paginazione libreria, dettaglio con streamUrl,
 * ricerca, 404. Il resolver finto simula la registrazione nel RelayRegistry.
 */
package it.fast4x.riplay.bridge

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class InMemoryCatalog : BridgeCatalog {

    val tracks = listOf(
        BridgeTrack(
            id = "local-1",
            name = "Canzone Locale",
            artistId = "artist-1",
            artistName = "Artisti Locali",
            albumId = "album-1",
            albumName = "Album Locale",
            durationSeconds = 212,
            trackNumber = 1,
            mimeType = "audio/flac",
            sizeBytes = 41_000_000L,
            servable = true,
        ),
        BridgeTrack(
            id = "yt-1",
            name = "Brano Solo YouTube",
            artistId = "artist-1",
            artistName = "Artisti Locali",
            durationSeconds = 180,
            servable = false,
        ),
    )

    val artists = listOf(
        BridgeArtist(id = "artist-1", name = "Artisti Locali", trackCount = 2, albumCount = 1),
    )

    val albums = listOf(
        BridgeAlbum(id = "album-1", name = "Album Locale", artistId = "artist-1", year = 2024, trackCount = 1),
    )

    val playlists = listOf(
        BridgePlaylist(id = "playlist-1", name = "Preferite", trackCount = 1),
    )

    override suspend fun artists(offset: Int, limit: Int) = CatalogPage(artists, artists.size)
    override suspend fun albums(offset: Int, limit: Int) = CatalogPage(albums, albums.size)
    override suspend fun tracks(offset: Int, limit: Int) = CatalogPage(tracks, tracks.size)
    override suspend fun playlists(offset: Int, limit: Int) = CatalogPage(playlists, playlists.size)
    override suspend fun artist(id: String) = artists.firstOrNull { it.id == id }
    override suspend fun albumsOfArtist(artistId: String, offset: Int, limit: Int) =
        CatalogPage(albums.filter { it.artistId == artistId }, albums.size)

    override suspend fun album(id: String) = albums.firstOrNull { it.id == id }
    override suspend fun tracksOfAlbum(albumId: String, offset: Int, limit: Int) =
        CatalogPage(tracks.filter { it.albumId == albumId }, tracks.size)

    override suspend fun tracksOfPlaylist(playlistId: String, offset: Int, limit: Int) =
        CatalogPage(tracks, tracks.size)

    override suspend fun playlist(id: String) = playlists.firstOrNull { it.id == id }

    override suspend fun track(id: String) = tracks.firstOrNull { it.id == id }

    override suspend fun search(query: String, limit: Int): BridgeSearchResult {
        val q = query.lowercase()
        return BridgeSearchResult(
            artists = artists.filter { it.name.lowercase().contains(q) },
            albums = albums.filter { it.name.lowercase().contains(q) },
            tracks = tracks.filter { it.name.lowercase().contains(q) },
            playlists = playlists.filter { it.name.lowercase().contains(q) },
        )
    }

    override suspend fun stats() = BridgeHealth(
        artistCount = artists.size,
        albumCount = albums.size,
        trackCount = tracks.size,
        playlistCount = playlists.size,
        servableTrackCount = tracks.count { it.servable },
    )
}

/** Resolver finto: simula il glue del relay (URL deterministico, verificabile). */
private class FakeRelayResolver : StreamUrlResolver {
    override fun streamUrl(host: String, port: Int, track: BridgeTrack): String? {
        track.directUrl?.let { if (it.startsWith("http")) return it }
        if (!track.servable) return null
        return "http://$host:$port/relay/testtoken/s/${track.id}"
    }

    override fun streamKind(host: String, port: Int, track: BridgeTrack): String? {
        track.directUrl?.let { if (it.startsWith("http")) return "direct" }
        return if (track.servable) "relay" else null
    }
}

class BridgeE2eTest {

    private val token = "testtoken"

    private suspend fun withBridge(block: suspend (InMemoryCatalog) -> Unit) {
        val catalog = InMemoryCatalog()
        testApplication {
            application {
                bridgeModule(catalog, FakeRelayResolver(), BridgeConfig(token = token))
            }
            block(catalog)
        }
    }

    @Test
    fun pingPubblicoSenzaToken() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response = client.get("/api/v1/ping")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"ok\":true"))
        }
    }

    @Test
    fun libraryTracksSenzaTokenRitorna401() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response = client.get("/api/v1/library/tracks")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun libraryTracksConToken() = kotlinx.coroutines.runBlocking {
        withBridge { catalog ->
            val response = client.get("/api/v1/library/tracks") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("2", json["total"]!!.jsonPrimitive.content)
            assertEquals(catalog.tracks.size, json["items"]!!.jsonArray.size)
        }
    }

    @Test
    fun trackDetailHaStreamUrlDelRelay() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response = client.get("/api/v1/tracks/local-1") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val streamUrl = json["streamUrl"]!!.jsonPrimitive.content
            assertTrue(streamUrl.startsWith("http://localhost"), "streamUrl deve usare l'origin del client: $streamUrl")
            assertTrue(streamUrl.contains("/relay/testtoken/s/"), "streamUrl deve puntare al relay: $streamUrl")
            assertEquals("relay", json["streamKind"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun trackSoloYouTubeNonEServibile() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response = client.get("/api/v1/tracks/yt-1") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertTrue(!json.containsKey("streamUrl") || json["streamUrl"] == null)
        }
    }

    @Test
    fun ricercaPerTitolo() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response =
                client.get("/api/v1/search?q=locale&limit=10") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertTrue(json["tracks"]!!.jsonArray.isNotEmpty())
            assertTrue(json["albums"]!!.jsonArray.isNotEmpty())
        }
    }

    @Test
    fun trackInesistenteRitorna404() = kotlinx.coroutines.runBlocking {
        withBridge { _ ->
            val response = client.get("/api/v1/tracks/nope") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
}
