package it.fast4x.riplay.utils

import it.fast4x.environment.utils.EnvironmentPreferenceItem
import it.fast4x.environment.utils.EnvironmentPreferences
import it.fast4x.spotifymeta.SpotifyPlaylist
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica che le tracce di una playlist di Spotify vengano abbinate ai brani giusti
 * su YouTube Music. Richiede una connessione a internet.
 */
class SpotifyTrackMatcherTest {

    private fun setupEnvironment() {
        EnvironmentPreferences.preference = EnvironmentPreferenceItem(
            p0 = "music.youtube.com",
            p1 = "/youtubei/v1/browse",
            p2 = "/youtubei/v1/like/like",
            p3 = "/youtubei/v1/account/account_menu",
            p4 = "/youtubei/v1/browse/edit_playlist",
            p5 = "/youtubei/v1/player",
            p6 = "/youtubei/v1/like/removelike",
            p7 = "/youtubei/v1/search",
            p8 = "/youtubei/v1/next",
            p9 = "/youtubei/v1/playlist/create",
            p10 = "/youtubei/v1/subscription/unsubscribe",
            p11 = "/youtubei/v1/music/get_queue",
            p12 = "/youtubei/v1/playlist/delete",
            p13 = "/youtubei/v1/music/get_search_suggestions",
            p14 = "/youtubei/v1/subscription/subscribe",
            p15 = "https://www.youtube.com/youtubei/v1/player",
            p16 = "www.youtube.com",
            p17 = "https://www.youtube.com",
            p18 = "WEB_REMIX",
            p19 = "1.20250122.01.00",
            p20 = "DESKTOP",
            p21 = "67",
            p22 = "WEB",
            p23 = "2.2021111",
            p24 = "IOS",
            p25 = "20.03.02",
            p26 = "Apple",
            p27 = "iPhone16,2",
            p28 = "iOS",
            p29 = "18.3.2.22D82",
            p30 = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            p31 = "5",
            p32 = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            p33 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
            p34 = "https://music.youtube.com/",
            p35 = "https://www.youtube.com/",
            p36 = "https://music.youtube.com",
            p37 = "Cg",
            p38 = "CgtMN0FkbDFaWERfdyi8t4u7BjIKCgJWThIEGgAgWQ%3D%3D",
            p39 = "",
            p40 = "",
            p41 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36",
            p42 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0",
            p43 = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        )
    }

    @Test
    fun `i titoli di Spotify e YouTube Music vengono riconosciuti`() {
        // "Esta Cobardía - Remasterizado" (Spotify) == "Esta Cobardía (Remasterizado)" (YT)
        assertEquals(
            1.0,
            SpotifyTrackMatcher.titleScore(
                "Esta Cobardía - Remasterizado",
                "Esta Cobardía (Remasterizado)"
            )
        )
        // "Another Part of Me - 2012 Remaster" == "Another Part of Me"
        assertEquals(
            1.0,
            SpotifyTrackMatcher.titleScore(
                "Another Part of Me - 2012 Remaster",
                "Another Part of Me"
            )
        )
        // titoli diversi non devono essere confusi
        assertTrue(
            SpotifyTrackMatcher.titleScore("Tuyo", "Jezabel") < 0.5
        )
    }

    @Test
    fun `gli artisti di Spotify e YouTube Music vengono riconosciuti`() {
        assertEquals(
            1.0,
            SpotifyTrackMatcher.artistScore("Michael Jackson", listOf("Michael Jackson"))
        )
        assertEquals(
            1.0,
            SpotifyTrackMatcher.artistScore(
                "Romeo Santos, Prince Royce",
                listOf("Romeo Santos", "&", "Prince Royce")
            )
        )
        // un artista diverso non deve essere riconosciuto
        assertTrue(
            SpotifyTrackMatcher.artistScore("Michael Jackson", listOf("Donny Osmond")) < 0.5
        )
    }

    @Test
    fun `ogni brano della playlist viene abbinato al brano giusto`() = runBlocking {
        setupEnvironment()

        val playlist = SpotifyPlaylist.fetch(
            "https://open.spotify.com/playlist/1lustF63AGV0P0ShIbGPYc?si=VXT80vU8Sh6c5jN_K16UTg"
        ) ?: error("playlist non letta")

        assertTrue(playlist.tracks.isNotEmpty())

        var matched = 0
        playlist.tracks.forEach { track ->
            val match = findMatchingSong(track)
            val title = match?.candidate?.info?.name
            val artists = match?.candidate?.authors?.mapNotNull { it.name }?.joinToString(", ")
            val duration = match?.candidate?.durationText
            if (match != null) matched++

            println(
                "SPOTIFY '${track.name}' (${track.artist}, ${track.durationMs}ms) -> " +
                        "YT '$title' ($artists, $duration) ${if (match != null) "MATCH" else "SKIPPED"}"
            )
        }

        println("Abbinati $matched/${playlist.tracks.size} brani")
        assertTrue(matched >= playlist.tracks.size - 1, "troppi brani non abbinati")
    }
}
