package it.fast4x.riplay.utils

import it.fast4x.riplay.extensions.lyricshelper.models.LRCLyricLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatestValueProviderTest {
    @Test
    fun synchronizedLyricsUsesPlaybackPositionUpdatesAfterInitialization() {
        val positionProvider = LatestValueProvider(1_000L)
        val lyrics = SynchronizedLyricsLines(
            sentences = listOf(
                LRCLyricLine(timeMs = 0L, text = "first"),
                LRCLyricLine(timeMs = 10_000L, text = "second"),
                LRCLyricLine(timeMs = 20_000L, text = "third"),
            ),
            positionProvider = positionProvider,
        )

        assertEquals(0, lyrics.index)

        positionProvider.value = 15_000L

        assertTrue(lyrics.update())
        assertEquals(1, lyrics.index)
    }
}
