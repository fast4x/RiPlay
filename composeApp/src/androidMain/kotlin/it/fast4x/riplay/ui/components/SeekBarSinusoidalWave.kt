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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun SeekBarSinusoidalWave(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 6.dp,
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

    val waveParams = remember(mediaItem?.mediaId) { generateSinusoidalParams(mediaItem?.mediaId ?: "") }


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

                if (width > 0) {

                    val path = Path()
                    val step = 2f

                    for (x in 0..width.toInt() step step.toInt()) {
                        val y = centerY + (height * 0.5f * waveParams.amplitude *
                                sin(x * 0.02f * waveParams.frequency + waveParams.phase))

                        if (x == 0) path.moveTo(x.toFloat(), y)
                        else path.lineTo(x.toFloat(), y)
                    }

                    val fraction = if (maximumValue > minimumValue) {
                        ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                    } else 0f
                    val progressWidth = width * fraction

                    val bufferedWidth = width * (buffered?.value ?: 0f)

                    drawPath(
                        path = path,
                        color = backgroundColor,
                        style = Stroke(
                            width = 4.dp.toPx(), // Spessore linea
                            pathEffect = null,
                            cap = StrokeCap.Round // Puntonde alle estremità
                        )
                    )

                    clipRect(left = 0f, top = 0f, right = bufferedWidth, bottom = height) {
                        drawPath(
                            path = path,
                            color = color.copy(alpha = .5f),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    clipRect(left = 0f, top = 0f, right = progressWidth, bottom = height) {
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
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


data class SinusoidalWaveParams(
    val frequency: Float,
    val amplitude: Float,
    val phase: Float
)


fun generateSinusoidalParams(seed: String): SinusoidalWaveParams {
    val random = Random(seed.hashCode())
    return SinusoidalWaveParams(
        frequency = random.nextFloat() * 4f + 2f,
        amplitude = random.nextFloat() * 0.6f + 0.3f,
        phase = random.nextFloat() * PI.toFloat() * 2
    )
}