package it.fast4x.riplay.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.abs

@JvmInline
value class PinchDirection private constructor(private val out: Boolean) {
    companion object {
        val Out = PinchDirection(out = true)
        val In = PinchDirection(out = false)
    }

    fun reachedThreshold(
        value: Float,
        threshold: Float
    ) = when (this) {
        Out -> value >= threshold
        In -> value <= threshold
        else -> error("Unreachable")
    }
}

fun Modifier.pinchToToggle(
    direction: PinchDirection,
    threshold: Float,
    key: Any? = Unit,
    onPinch: (scale: Float) -> Unit
) = this.pointerInput(key) {
    coroutineScope {
        awaitEachGesture {
            val touchSlop = viewConfiguration.touchSlop / 2
            var scale = 1f
            var touchSlopReached = false

            awaitFirstDown(requireUnconsumed = false)

            @Suppress("LoopWithTooManyJumpStatements")
            while (isActive) {
                val event = awaitPointerEvent()
                if (event.changes.fastAny { it.isConsumed }) break
                if (!event.changes.fastAny { it.pressed }) continue

                scale *= event.calculateZoom()
                if (!touchSlopReached) {
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    if (abs(1 - scale) * centroidSize > touchSlop) touchSlopReached = true
                }

                if (touchSlopReached) event.changes.fastForEach { if (it.positionChanged()) it.consume() }

                if (
                    direction.reachedThreshold(
                        value = scale,
                        threshold = threshold
                    )
                ) {
                    onPinch(scale)
                    break
                }
            }
        }
    }
}
