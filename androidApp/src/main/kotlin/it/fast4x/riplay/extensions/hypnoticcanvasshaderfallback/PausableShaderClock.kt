package it.fast4x.riplay.extensions.hypnoticcanvasshaderfallback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.State
/**
 * Clock "pausable": replica l'uniform `speed` di HypnoticCanvas.
 * Il tempo accumulato avanza di delta * speed ad ogni frame.
 */
class PausableShaderClock(
    val loopSeconds: Float = 60f,
    val maxDeltaSeconds: Float = 0.1f,
) {
    /** Scrivibile anche fuori dal frame loop (es. dal tuo stato play/pause). */
    var speed: Float = 1f
        set(value) { field = value.coerceAtLeast(0f) } // anti-rewind (spring overshoot)

    var timeSeconds: Float = 0f
        private set

    private var lastFrameNanos = -1L

    fun tick(frameNanos: Long) {
        if (lastFrameNanos < 0L) lastFrameNanos = frameNanos
        val delta = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(maxDeltaSeconds)
        lastFrameNanos = frameNanos
        timeSeconds = (timeSeconds + delta * speed) % loopSeconds
    }

    fun tick(frameNanos: Long, speed: Float) {
        this.speed = speed
        tick(frameNanos)
    }

    fun reset() { timeSeconds = 0f; lastFrameNanos = -1L }
}

@Composable
fun rememberPausableShaderClock(
    speed: () -> Float,
    loopSeconds: Float = 60f,
    maxDeltaSeconds: Float = 0.1f,
): State<Float> {
    val clock = remember(loopSeconds) { PausableShaderClock(loopSeconds, maxDeltaSeconds) }
    return produceState(0f, clock) {
        while (true) {
            withFrameNanos { now ->
                clock.tick(now, speed())
                value = clock.timeSeconds
            }
        }
    }
}

// Overload comodi
@Composable
fun rememberPausableShaderClock(
    speedState: State<Float>,
    loopSeconds: Float = 60f,
): State<Float> = rememberPausableShaderClock({ speedState.value }, loopSeconds)

@Composable
fun rememberPausableShaderClock(
    speed: Float,
    loopSeconds: Float = 60f,
): State<Float> = rememberPausableShaderClock({ speed }, loopSeconds)