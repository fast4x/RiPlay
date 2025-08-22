package it.fast4x.riplay.utils

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.ui.styling.shimmer

// This hide androidview when media is not video
fun Modifier.hide(): Modifier {
    return this.size(0.dp)
}


/**
 * A loading effect that goes from top left
 * to bottom right in 2000 millis (2s).
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf( IntSize.Zero ) }
    val transition = rememberInfiniteTransition( "infiniteTransition" )
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutLinearInEasing
            ),
        ),
        label = "offsetXAnimatedTransition"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                colorPalette().background1,
                colorPalette().shimmer.copy( alpha = .3f ),
                colorPalette().background1
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun Modifier.detectGestures(
    detectPlayerGestures: Boolean = false,
    onSwipeToLeft: () -> Unit? = {},
    onSwipeToRight: () -> Unit? = {},
    onTap: () -> Unit? = {},
    onDoubleTap: () -> Unit? = {},
    onPress: () -> Unit? = {},
    onLongPress: () -> Unit? = {},
): Modifier {
    val disablePlayerHorizontalSwipe by rememberObservedPreference(disablePlayerHorizontalSwipeKey, false)
    var deltaX by remember { mutableStateOf(0f) }
    return this
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    deltaX = dragAmount
                },
                onDragStart = {
                },
                onDragEnd = {
                    if (!disablePlayerHorizontalSwipe && detectPlayerGestures) {
                        if (deltaX > 5) {
                            onSwipeToRight() // Previous
                        } else if (deltaX < -5) {
                            onSwipeToLeft() // Next
                        }
                    } else {
                        if (deltaX > 5) {
                            onSwipeToRight() // Previous
                        } else if (deltaX < -5) {
                            onSwipeToLeft() // Next
                        }
                    }

                }

            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    onTap()
                },
                onDoubleTap = {
                    onDoubleTap()
                },
                onPress = {
                    onPress()
                },
                onLongPress = {
                    onLongPress()
                }
            )
        }
}