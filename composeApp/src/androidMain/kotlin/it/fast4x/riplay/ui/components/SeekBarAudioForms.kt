package it.fast4x.riplay.ui.components

import androidx.annotation.OptIn
import kotlin.random.Random
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.formatMillis
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.typography
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(UnstableApi::class)
@Composable
fun SeekBarAudioForms(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 6.dp,
    shape: Shape = RectangleShape,
    drawSteps: Boolean = false,
    showTooltip: Boolean = true
) {
    val isDragging = remember {
        MutableTransitionState(false)
    }

    var seekBarWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }

    var draggingValue by remember { mutableLongStateOf(value) }

    LaunchedEffect(value) {
        if (!isDragging.targetState) {
            draggingValue = value
        }
    }

    val binder = LocalPlayerServiceBinder.current
    val mediaItem = binder?.player?.currentMediaItem
    val buffered = binder?.onlinePlayerBufferedFraction?.collectAsState()
//    Timber.d("Seekbar buffered $buffered")

    val timeText = remember(draggingValue) { formatMillis(if (mediaItem?.isLocal == true) draggingValue  else draggingValue * 1000) }
    val colorPalette = colorPalette()

    val waveformSeed = remember(mediaItem?.mediaId) { mediaItem?.mediaId }
    val waveData = remember(waveformSeed) { generateWaveform(waveformSeed ?: "", 100) }
    //personalize numBars
    // val numBars = (seekBarWidth / 4.dp.toPx()).toInt().coerceAtLeast(20)
    // val waveData = remember(waveformSeed, numBars) { generateWaveform(waveformSeed, numBars) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> seekBarWidth = coordinates.size.width }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging.targetState = true
                        val newValue = (it.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue.coerceIn(minimumValue, maximumValue)
                        onDragStart(draggingValue)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val currentX = change.position.x
                        val newValue = (currentX / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue.coerceIn(minimumValue, maximumValue)

                        acc += dragAmount / size.width * (maximumValue - minimumValue)

                        if (acc !in -1f..1f) {
                            onDrag(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging.targetState = false
                        acc = 0F
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging.targetState = false
                        acc = 0F
                        onDragEnd()
                    }
                )
            }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        val newValue = (offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue
                        onDragStart(newValue)
                    },
                    onTap = {
                        onDragEnd()
                    }
                )
            }
            .padding(horizontal = scrubberRadius)
            .drawBehind {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                if (width > 0 && waveData.isNotEmpty()) {
                    val barWidth = width / waveData.size
                    val gap = barWidth * 0.25f
                    val drawWidth = barWidth - gap

                    val fraction = if (maximumValue > minimumValue) {
                        ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                    } else 0f

                    waveData.forEachIndexed { index, amplitude ->
                        val barFraction = index.toFloat() / waveData.size

                        val isPlayed = barFraction <= fraction
                        val isBuffered = barFraction <= (buffered?.value ?: 0f)


                        val color = when {
                            isPlayed -> color
                            isBuffered -> color.copy(alpha = 0.5f)
                            else -> backgroundColor
                        }

                        val barHeight = (height * 0.8f * amplitude).coerceAtLeast(2.dp.toPx())

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(
                                x = index * barWidth + gap / 2,
                                y = centerY - barHeight / 2
                            ),
                            size = Size(drawWidth, barHeight),
                            cornerRadius = CornerRadius(drawWidth / 2, drawWidth / 2) // Barre tonde
                        )
                    }
                }
            }
            .height(scrubberRadius + 44.dp)
    ) {


        AnimatedVisibility(
            visible = isDragging.targetState && showTooltip,
            enter = fadeIn(), exit = fadeOut()
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val fraction = if (maximumValue > minimumValue) {
                            ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                        } else 0f
                        val xPos = if (seekBarWidth > 0) {
                            (seekBarWidth * fraction) - (scrubberRadius.toPx())
                        } else 0
                        IntOffset(x = xPos.toInt(), y = 0)
                    }
                    .size(scrubberRadius * 2)
                    .clip(CircleShape)
                    .background(scrubberColor)
            )
        }

        AnimatedVisibility(
            visible = isDragging.targetState && showTooltip,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val fraction = if (maximumValue > minimumValue) {
                        ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val xPos = if (seekBarWidth > 0) {
                        (seekBarWidth * fraction) - (tooltipWidth / 2)
                    } else {
                        0
                    }

                    IntOffset(x = xPos.toInt(), y = (-10).dp.toPx().roundToInt())
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





fun generateWaveform(seed: String, barCount: Int): List<Float> {
    val random = Random(seed.hashCode())
    return List(barCount) {
        random.nextFloat() * 0.8f + 0.2f
    }
}
