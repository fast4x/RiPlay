package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.formatMillis
import it.fast4x.riplay.utils.typography
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun ModernSeekbar(
    value: Float, // 0.0f a 1.0f
    onValueChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    totalDurationMs: Long, // Durata totale in millisecondi
    bufferedValue: Float = 0f,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    thumbRadiusExpanded: Dp = 8.dp,
    //backgroundColor: Color,
    enabled: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }

    var seekBarWidth by remember { mutableIntStateOf(0) }

    var tooltipWidth by remember { mutableIntStateOf(0) }

    val animatedThumbRadius by animateDpAsState(
        targetValue = if (isDragging) thumbRadiusExpanded else thumbRadius,
        label = "thumbRadius"
    )

    val currentTimeMs = (value * totalDurationMs).toLong()
    val timeText = remember(currentTimeMs) { formatMillis(currentTimeMs) }
    val colorPalette = colorPalette()

    Box(
        modifier = modifier
            .height(50.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .onGloballyPositioned { coordinates -> seekBarWidth = coordinates.size.width }
                .pointerInput(enabled) {
                    detectDragGestures(
                        onDragStart = {
                            if (enabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                isDragging = true

                                val x = it.x
                                val newValue = (x / size.width).coerceIn(0f, 1f)
                                onValueChange(newValue)
                            }
                        },
                        onDrag = { change, _ ->
                            if (enabled) {
                                val x = change.position.x
                                val newValue = (x / size.width).coerceIn(0f, 1f)
                                onValueChange(newValue)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging = false
                            onDragEnd()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            drawRoundRect(
                color = colorPalette.textSecondary.copy(alpha = 0.2f),
                topLeft = Offset.Zero,
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2, height / 2)
            )

            if (bufferedValue > 0) {
                drawRoundRect(
                    color = colorPalette.text.copy(alpha = 0.3f),
                    topLeft = Offset.Zero,
                    size = Size(width * bufferedValue, height),
                    cornerRadius = CornerRadius(height / 2, height / 2)
                )
            }

            drawRoundRect(
                color = colorPalette.accent,
                topLeft = Offset.Zero,
                size = Size(width * value, height),
                cornerRadius = CornerRadius(height / 2, height / 2)
            )

            drawCircle(
                color = Color.White,
                radius = animatedThumbRadius.toPx(),
                center = Offset(x = width * value, y = height / 2)
            )

            if (isDragging) {
                drawCircle(
                    color = colorPalette.accent.copy(alpha = 0.3f),
                    radius = (animatedThumbRadius * 1.5f).toPx(),
                    center = Offset(x = width * value, y = height / 2)
                )
            }
        }

        AnimatedVisibility(
            visible = isDragging,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val xPos = (seekBarWidth * value) - (tooltipWidth / 2)
                    IntOffset(x = xPos.roundToInt(), y = (-10).dp.toPx().roundToInt())
                }
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> tooltipWidth = coordinates.size.width }
                    .background(
                        color = colorPalette.text,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .alpha(0.9f)
            ) {
                BasicText(
                    text = timeText,
                    style = typography().xs.copy(
                        color = colorPalette.background0,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

