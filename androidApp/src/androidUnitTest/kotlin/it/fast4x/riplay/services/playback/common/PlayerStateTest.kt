package it.fast4x.riplay.services.playback.common

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerStateTest {
    @Test
    fun staleDatabaseResultDoesNotOverwriteNewTransition() {
        val oldDatabaseItem = MediaItem.Builder()
            .setMediaId("previous-id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Big in Japan")
                    .setArtist("Alphaville")
                    .build()
            )
            .build()
        val currentItem = MediaItem.Builder()
            .setMediaId("next-id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Take On Me")
                    .setArtist("a-ha")
                    .build()
            )
            .build()
        val currentState = PlayerState(
            mediaInfo = MediaInfo(currentItem, queueIndex = 1, queueSize = 94),
        )

        val updatedState = currentState.withDatabaseMediaItemIfCurrent(
            currentMediaId = currentItem.mediaId,
            databaseMediaItem = oldDatabaseItem,
            queueIndex = 1,
            queueSize = 94,
        )

        assertEquals("next-id", updatedState.mediaInfo?.mediaItem?.mediaId)
        assertEquals("Take On Me", updatedState.mediaInfo?.mediaItem?.mediaMetadata?.title)
    }

    @Test
    fun mediaTransitionReplacesStaleUiMetadataWithoutDatabaseRow() {
        val previousItem = MediaItem.Builder()
            .setMediaId("previous-id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Big in Japan")
                    .setArtist("Alphaville")
                    .build()
            )
            .build()
        val nextItem = MediaItem.Builder()
            .setMediaId("next-id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Take On Me")
                    .setArtist("a-ha")
                    .build()
            )
            .build()
        val staleState = PlayerState(
            mediaInfo = MediaInfo(previousItem, queueIndex = 0, queueSize = 94),
            errorMessage = "stale error",
        )

        val updatedState = staleState.withMediaTransition(
            mediaItem = nextItem,
            queueIndex = 1,
            queueSize = 94,
        )

        assertEquals("next-id", updatedState.mediaInfo?.mediaItem?.mediaId)
        assertEquals("Take On Me", updatedState.mediaInfo?.mediaItem?.mediaMetadata?.title)
        assertEquals("a-ha", updatedState.mediaInfo?.mediaItem?.mediaMetadata?.artist)
        assertEquals(1, updatedState.mediaInfo?.queueIndex)
        assertEquals(94, updatedState.mediaInfo?.queueSize)
        assertNull(updatedState.errorMessage)
    }
}
