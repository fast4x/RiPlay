package it.fast4x.riplay.services.playback.common

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerVolumeTest {
    @Test
    fun storedVolumeIsRestoredAfterActivityResume() {
        var appliedVolume = -1f

        restorePlayerVolume(0.48f, isFading = false, isServiceReady = true) { appliedVolume = it }

        assertEquals(0.48f, appliedVolume)
    }

    @Test
    fun volumeIsNotOverwrittenWhileFadeIsRunning() {
        var applyCount = 0

        restorePlayerVolume(0.48f, isFading = true, isServiceReady = true) { applyCount++ }

        assertEquals(0, applyCount)
    }

    @Test
    fun restoredVolumeIsClampedToPlayerRange() {
        var appliedVolume = -1f

        restorePlayerVolume(1.5f, isFading = false, isServiceReady = true) { appliedVolume = it }

        assertEquals(1f, appliedVolume)
    }

    @Test
    fun volumeIsNotRestoredThroughStoppedService() {
        var applyCount = 0

        restorePlayerVolume(0.48f, isFading = false, isServiceReady = false) { applyCount++ }

        assertEquals(0, applyCount)
    }
}
