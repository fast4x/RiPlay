package it.fast4x.riplay.utils

import it.fast4x.spotifymeta.SpotifyTrackMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotifyPlaylistImportResultTest {
    @Test
    fun `pending-only playlists are useful imports without claiming playable tracks`() {
        val result = SpotifyImportResult(imported = 0, skipped = 3, pending = 3, total = 3)
        assertTrue(result.isSuccess)
        assertEquals(0, result.imported)
        assertEquals(3, result.pending)
        assertEquals(result.total, result.imported + result.skipped)
    }

    @Test
    fun `empty imports and real failures are not successful`() {
        assertFalse(SpotifyImportResult(total = 2, skipped = 2).isSuccess)
        assertFalse(SpotifyImportResult(pending = 2, error = "Cannot save playlist").isSuccess)
        assertTrue(SpotifyImportResult(imported = 2, pending = 1, skipped = 1, total = 3).isSuccess)
    }

    @Test
    fun `pending metadata retains musical identity without adding listening history`() {
        val song = assertNotNull(
            SpotifyTrackMeta(
                id = "spotify:track:track-id",
                name = "  Song title  ",
                artist = "Artist One, Artist Two",
                durationMs = 185_000L
            ).asPendingSong("playlist-cover")
        )
        assertEquals("spotify:track:track-id", song.id)
        assertEquals("Song title", song.title)
        assertEquals("Artist One, Artist Two", song.artistsText)
        assertEquals("3:05", song.durationText)
        assertEquals("playlist-cover", song.thumbnailUrl)
        assertEquals(0L, song.totalPlayTimeMs)
        assertNull(song.likedAt)
    }

    @Test
    fun `track artwork wins and incomplete metadata does not create unusable seeds`() {
        val track = SpotifyTrackMeta(id = "track", name = "Title", thumbnailUrl = "track-cover")
        assertEquals("track-cover", assertNotNull(track.asPendingSong("playlist-cover")).thumbnailUrl)
        assertNull(track.copy(id = "").asPendingSong())
        assertNull(track.copy(name = "  ").asPendingSong())
        assertNull(track.copy(durationMs = 0L).asPendingSong()?.durationText)
    }
}
