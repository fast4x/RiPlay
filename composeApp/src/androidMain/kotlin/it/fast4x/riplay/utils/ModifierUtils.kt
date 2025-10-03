package it.fast4x.riplay.utils

import android.graphics.BlurMaskFilter
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.ui.styling.shimmer
import kotlin.math.sqrt

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

@OptIn(UnstableApi::class)
fun Modifier.animatedGradient(
    animating: Boolean,
    D: Color,
    V: Color,
    LV: Color,
    DV: Color,
    M: Color,
    LM: Color,
    DM: Color
    ): Modifier = composed {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(rotation, animating) {
        if (!animating) return@LaunchedEffect
        val target = rotation.value + 360f
        rotation.animateTo(
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 30_000,
                    easing = LinearEasing,
                ),
            ),
        )
    }

    drawWithCache {
        val rectSize = sqrt(size.width * size.width + size.height * size.height)
        val topLeft = Offset(
            x = -(rectSize - size.width) / 2,
            y = -(rectSize - size.height) / 2,
        )

        val brush1 = Brush.linearGradient(
            0f to D,
            0.33f to LV,
            0.66f to V,
            1f to DV,
            start = topLeft,
            end = Offset(rectSize * 0.7f, rectSize * 0.7f),
        )

        val brush2 = Brush.linearGradient(
            0f to D,
            0.33f to LM,
            0.66f to M,
            1f to DM,
            start = Offset(rectSize, 0f),
            end = Offset(0f, rectSize),
        )

        val maskBrush = Brush.linearGradient(
            0f to D,
            1f to Color.Companion.Transparent,
            start = Offset(rectSize / 2f, 0f),
            end = Offset(rectSize / 2f, rectSize),
        )

        onDrawBehind {
            val value = rotation.value

            withTransform(transformBlock = { rotate(value) }) {
                drawRect(
                    brush = brush1,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                )
            }

            withTransform(transformBlock = { rotate(-value) }) {
                drawRect(
                    brush = maskBrush,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                    blendMode = BlendMode.Companion.DstOut,
                )
            }

            withTransform(transformBlock = { rotate(value) }) {
                drawRect(
                    brush = brush2,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                    blendMode = BlendMode.Companion.DstAtop,
                )
            }
        }
    }
}

fun Modifier.verticalFadingEdge() =
    graphicsLayer(alpha = 0.95f)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black, Color.Black, Color.Black,
                        Color.Transparent
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }

fun Modifier.horizontalFadingEdge() =
    graphicsLayer(alpha = 0.95f)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black, Color.Black, Color.Black,
                        Color.Transparent
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }

fun Modifier.fadingEdge(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
) = graphicsLayer(alpha = 0.99f)
    .drawWithContent {
        drawContent()
        if (top != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black
                    ),
                    startY = 0f,
                    endY = top.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottom != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Transparent
                    ),
                    startY = size.height - bottom.toPx(),
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (left != null) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = left.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (right != null) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black
                    ),
                    startX = size.width - right.toPx(),
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

fun Modifier.fadingEdge(
    horizontal: Dp? = null,
    vertical: Dp? = null,
) = fadingEdge(
    left = horizontal,
    right = horizontal,
    top = vertical,
    bottom = vertical
)

fun Modifier.verticalfadingEdge2(fade: Float, showTopActionsBar: Boolean, topPadding: Boolean, expandedplayer: Boolean) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .applyIf(showTopActionsBar || topPadding || expandedplayer){
        drawWithContent {
            val topFade = Brush.verticalGradient(0f to Color.Transparent, fade to Color.Red)
            drawContent()
            drawRect(brush = topFade, blendMode = BlendMode.DstIn)
        }
    }
    .drawWithContent {
        val bottomFade = Brush.verticalGradient(0f to Color.Transparent, fade to Color.Red, startY =  Float.POSITIVE_INFINITY, endY = 0f)
        drawContent()
        drawRect(brush = bottomFade, blendMode = BlendMode.DstIn)
    }

fun Modifier.horizontalfadingEdge2(fade: Float) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        val Fade = Brush.horizontalGradient(0f to Color.Transparent, fade to Color.Black,(1f-fade) to Color.Black,1f to Color.Transparent)
        drawContent()
        drawRect(brush = Fade, blendMode = BlendMode.DstIn)
    }

fun Modifier.dropShadow(
    shape: Shape,
    color: Color = Color.Black.copy(0.25f),
    blur: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp
) = this.drawBehind {
    val shadowSize = Size(size.width + spread.toPx(), size.height + spread.toPx())
    val shadowOutline = shape.createOutline(shadowSize, layoutDirection, this)
    // Create a Paint object
    val paint = Paint()
// Apply specified color
    paint.color = color

// Check for valid blur radius
    if (blur.toPx() > 0) {
        paint.asFrameworkPaint().apply {
            // Apply blur to the Paint
            maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    }
    drawIntoCanvas { canvas ->
        // Save the canvas state
        canvas.save()
        // Translate to specified offsets
        canvas.translate(offsetX.toPx(), offsetY.toPx())
        // Draw the shadow
        canvas.drawOutline(shadowOutline, paint)
        // Restore the canvas state
        canvas.restore()
    }
}

fun Modifier.doubleShadowDrop(
    shape: Shape,
    offset: Dp = 4.dp,
    blur: Dp = 8.dp,
) = this
    .dropShadow(shape, Color.Black.copy(0.75f), blur, offset, offset)
    .dropShadow(shape, Color.Black.copy(0.50f), blur, -offset, -offset)


fun Modifier.pulsatingEffect(
    currentValue: Float,
    isVisible: Boolean,
    color: Color = Color.Gray,
): Modifier = composed {
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val thumbX by remember(currentValue) {
        mutableFloatStateOf(trackWidth * currentValue)
    }

    val transition = rememberInfiniteTransition(label = "trackAnimation")

    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                delayMillis = 200,
            )
        ), label = "width"
    )

    this then Modifier
        .onGloballyPositioned { coordinates ->
            trackWidth = coordinates.size.width.toFloat()
        }
        .drawWithContent {
            drawContent()

            val strokeWidth = size.height
            val y = size.height / 2f
            val startOffset = thumbX
            val endOffset = thumbX + animationProgress * (trackWidth - thumbX)
            val dynamicAlpha = (1f - animationProgress).coerceIn(0f, 1f)

            if (isVisible) {
                drawLine(
                    color = color.copy(alpha = dynamicAlpha),
                    start = Offset(startOffset, y),
                    end = Offset(endOffset, y),
                    cap = StrokeCap.Round,
                    strokeWidth = strokeWidth
                )
            }
        }
}