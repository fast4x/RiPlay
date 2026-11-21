package it.fast4x.riplay.extensions.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechTriggerGateTest {

    private val frameMs = 20L

    /** Feeds [frames] frames and returns the timestamp after the last one. */
    private fun feed(
        gate: SpeechTriggerGate,
        levelDb: Double,
        frames: Int,
        startMs: Long,
        onFrame: (Boolean) -> Unit = {},
    ): Long {
        var now = startMs
        repeat(frames) {
            onFrame(gate.onFrame(levelDb, now))
            now += frameMs
        }
        return now
    }

    private fun warmUp(gate: SpeechTriggerGate, levelDb: Double = -120.0): Long =
        feed(gate, levelDb, frames = 80, startMs = 0L)

    @Test
    fun speechAfterWarmupAndQuietSettleTriggers() {
        val gate = SpeechTriggerGate(playbackActive = false)
        gate.reset(0L)
        var now = warmUp(gate)
        now = feed(gate, -120.0, frames = 75, startMs = now)
        assertFalse(gate.isWarmingUp(now))

        repeat(3) {
            assertFalse(gate.onFrame(-20.0, now))
            now += frameMs
        }
        assertTrue(gate.onFrame(-20.0, now))
    }

    @Test
    fun loudBurstRightAfterWarmupDoesNotTrigger() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)
        val now = warmUp(gate, levelDb = -30.0)

        feed(gate, -10.0, frames = 4, startMs = now) { triggered ->
            assertFalse(triggered)
        }
    }

    @Test
    fun burstDuringSettleDoesNotTrigger() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)
        var now = warmUp(gate, levelDb = -30.0)
        now = feed(gate, -120.0, frames = 10, startMs = now)

        feed(gate, -10.0, frames = 4, startMs = now) { triggered ->
            assertFalse(triggered)
        }
    }

    @Test
    fun audibleMusicKeepsTheGateUnsettled() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)
        var now = warmUp(gate, levelDb = -30.0)
        // The echo canceler has not converged yet and music is still audible,
        // even though it sits below the speech margin.
        now = feed(gate, -28.0, frames = 75, startMs = now)
        assertTrue(gate.peakFloorDb > -30.0)

        feed(gate, -10.0, frames = 4, startMs = now) { triggered ->
            assertFalse(triggered)
        }
    }

    @Test
    fun unvoicedFrameResetsTheTriggerCount() {
        val gate = SpeechTriggerGate(playbackActive = false)
        gate.reset(0L)
        var now = warmUp(gate)
        now = feed(gate, -120.0, frames = 75, startMs = now)

        now = feed(gate, -20.0, frames = 3, startMs = now)
        now = feed(gate, -120.0, frames = 1, startMs = now)
        now = feed(gate, -20.0, frames = 3, startMs = now) { triggered ->
            assertFalse(triggered)
        }
        assertTrue(gate.onFrame(-20.0, now))
    }

    @Test
    fun veryQuietSoundDoesNotTrigger() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)
        var now = warmUp(gate)
        now = feed(gate, -120.0, frames = 75, startMs = now)

        feed(gate, -60.0, frames = 20, startMs = now) { triggered ->
            assertFalse(triggered)
        }
    }

    @Test
    fun sustainedMusicDoesNotTrigger() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)
        val now = warmUp(gate, levelDb = -25.0)

        feed(gate, -25.0, frames = 200, startMs = now) { triggered ->
            assertFalse(triggered)
        }
    }

    @Test
    fun warmupKeepsTheLoudestFrameAsNoiseFloor() {
        val gate = SpeechTriggerGate()
        gate.reset(0L)

        var now = 0L
        repeat(59) {
            gate.onFrame(-100.0, now)
            now += frameMs
        }
        gate.onFrame(-20.0, now)

        assertTrue(gate.isWarmingUp(now))
        assertEquals(-20.0, gate.peakFloorDb)
    }
}
