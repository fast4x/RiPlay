package it.fast4x.riplay.enums

import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class PlayerTransitionAnimation {
    Smooth, Bouncy, Snappy, Elegant, Instant, Adaptive;

    companion object {
        fun playerBoundsAnimation(
            type: PlayerTransitionAnimation,
            initial: Rect,
            target: Rect
        ): FiniteAnimationSpec<Rect> = when (type) {
            Smooth ->   // default
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

            Bouncy ->
                spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

            Snappy ->
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

            Elegant ->
                tween(380, easing = FastOutSlowInEasing)

            Instant ->
                snap()
            Adaptive -> {
                val distance = hypot(
                    target.center.x - initial.center.x,
                    target.center.y - initial.center.y
                )
                tween((distance / 3f).roundToInt().coerceIn(250, 450), easing = FastOutSlowInEasing)
            }
        }
    }
}