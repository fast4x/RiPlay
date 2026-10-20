package it.fast4x.spotifymeta

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test di rete: verificano che la lettura di una playlist pubblica di Spotify funzioni.
 * Richiedono una connessione a internet.
 */
class SpotifyPlaylistTest {

    private val playlistUrl = "https://open.spotify.com/playlist/1lustF63AGV0P0ShIbGPYc?si=VXT80vU8Sh6c5jN_K16UTg"

    @Test
    fun `parseId accetta url completi e id nudi`() {
        assertEquals("1lustF63AGV0P0ShIbGPYc", SpotifyPlaylist.parseId(playlistUrl))
        assertEquals("1lustF63AGV0P0ShIbGPYc", SpotifyPlaylist.parseId("1lustF63AGV0P0ShIbGPYc"))
        assertEquals(
            "1lustF63AGV0P0ShIbGPYc",
            SpotifyPlaylist.parseId("https://open.spotify.com/playlist/1lustF63AGV0P0ShIbGPYc?si=abc&utm_source=x")
        )
        assertEquals(null, SpotifyPlaylist.parseId("https://www.youtube.com/playlist?list=PL123"))
    }

    @Test
    fun `la playlist viene letta con tutte le sue tracce`() = runBlocking {
        val playlist = assertNotNull(
            SpotifyPlaylist.fetch(playlistUrl),
            "la playlist non dovrebbe essere null"
        )

        assertEquals("1lustF63AGV0P0ShIbGPYc", playlist.id)
        assertTrue(!playlist.name.isNullOrBlank(), "il nome della playlist non dovrebbe essere vuoto")
        assertTrue(playlist.tracks.isNotEmpty(), "la playlist dovrebbe contenere delle tracce")
        assertTrue(playlist.isComplete, "dovrebbero essere state lette tutte le tracce")

        // ogni traccia deve avere id, titolo e artista
        playlist.tracks.forEach { track ->
            assertTrue(track.id.length == 22, "id traccia non valido: ${track.id}")
            assertTrue(!track.name.isNullOrBlank(), "titolo traccia vuoto")
            assertTrue(!track.artist.isNullOrBlank(), "artista traccia vuoto: ${track.name}")
            // l'album arriva solo dalla API GraphQL: se manca significa che la playlist
            // e' stata letta solo in forma parziale dalla pagina embed
            assertTrue(!track.album.isNullOrBlank(), "album traccia vuoto: ${track.name}")
        }

        println("Playlist letta: '${playlist.name}' di ${playlist.owner} con ${playlist.tracks.size}/${playlist.totalCount} tracce")
        playlist.tracks.forEach { track -> println(" - ${track.artist} - ${track.name} (${track.durationMs}ms)") }
    }
}
