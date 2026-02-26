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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.formatMillis
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.typography
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.ui.graphics.lerp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.shouldBePlaying
import it.fast4x.riplay.utils.windows
import timber.log.Timber

@OptIn(UnstableApi::class)
@Composable
fun SeekBarSegmentColored(
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
    var localMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val buffered = binder?.onlinePlayerBufferedFraction?.collectAsState()
    val colorPalette = colorPalette()

    val baseColors = listOf(colorPalette.collapsedPlayerProgressBar, colorPalette.textSecondary, colorPalette.textDisabled,colorPalette.text)

    val segmentColors = remember(localMediaItem?.mediaId, baseColors) {
        generateGradientPalette(
            localMediaItem?.mediaId ?: "",
            steps = 30,
            baseColors = baseColors
        )
    }

    binder?.player?.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
               localMediaItem = mediaItem
            }
        }
    }

    val timeText = remember(draggingValue) { formatMillis(if (localMediaItem?.isLocal == true) draggingValue  else draggingValue * 1000) }


    val gap = 2.dp.px
    val numSegments = segmentColors.size

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
                    val segmentWidth = (width - (gap * (numSegments - 1))) / numSegments
                    val pieceHeight = height * 0.6f

                    // Posizione cursore riproduzione
                    val fraction = if (maximumValue > minimumValue) {
                        ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                    } else 0f
                    val progressWidth = width * fraction

                    // Posizione buffer (scaricamento)
                    val bufferedWidth = width * (buffered?.value ?: 0f)

                    segmentColors.forEachIndexed { index, color ->
                        val xPos = index * (segmentWidth + gap)

                        val rect = android.graphics.RectF(
                            xPos,
                            centerY - pieceHeight / 2,
                            xPos + segmentWidth,
                            centerY + pieceHeight / 2
                        )

                        // 1. SEZIONE RIPRODOTTA (Priorità massima)
                        if (rect.right <= progressWidth) {
                            drawRoundRect(
                                color = color,
                                topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                cornerRadius = CornerRadius(segmentWidth / 2, segmentWidth / 2)
                            )
                        } else if (rect.left < progressWidth) {
                            // Pezzo parzialmente riprodotto
                            clipRect(
                                left = rect.left,
                                top = rect.top,
                                right = progressWidth,
                                bottom = rect.bottom
                            ) {
                                drawRoundRect(
                                    color = color,
                                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                    size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                    cornerRadius = CornerRadius(segmentWidth / 2, segmentWidth / 2)
                                )
                            }
                        } else {
                            // 2. SEZIONE NON RIPRODOTTA: Controlliamo il BUFFER

                            // Colore per il buffer (solitamente grigio chiaro)
                            // Puoi usare anche color.copy(alpha = 0.3f) se vuoi mantenere l'arcobaleno sbiadito
                            val bufferColor = color.copy(alpha = 0.5f)

                            if (rect.right <= bufferedWidth) {
                                // Pezzo completamente bufferizzato
                                drawRoundRect(
                                    color = bufferColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                    size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                    cornerRadius = CornerRadius(segmentWidth / 2, segmentWidth / 2)
                                )
                            } else if (rect.left < bufferedWidth) {
                                // Pezzo parzialmente bufferizzato
                                clipRect(
                                    left = rect.left,
                                    top = rect.top,
                                    right = bufferedWidth,
                                    bottom = rect.bottom
                                ) {
                                    drawRoundRect(
                                        color = bufferColor,
                                        topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                        size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                        cornerRadius = CornerRadius(segmentWidth / 2, segmentWidth / 2)
                                    )
                                }
                            } else {
                                // 3. Pezzo neanche scaricato (Sfondo/Scuro)
                                drawRoundRect(
                                    color = color.copy(alpha = 0.2f), // Molto scuro/trasparente
                                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                    size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                    cornerRadius = CornerRadius(segmentWidth / 2, segmentWidth / 2)
                                )
                            }
                        }
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

fun generateGradientPalette(
    seed: String,
    steps: Int = 30,
    baseColors: List<Color>? = null
): List<Color> {
    Timber.d("generateGradientPalette seed: $seed steps: $steps baseColors: $baseColors")
    if (baseColors.isNullOrEmpty()) {
        val random = Random(seed.hashCode())
        val startHue = random.nextFloat() * 270f
        val endHue = startHue + 90f

        return (0 until steps).map { i ->
            val hue = startHue + (i.toFloat() / steps) * (endHue - startHue)
            Color.hsv(hue % 360f, 0.75f, 0.9f)
        }
    } else {
        if (baseColors.size == 1) return List(steps) { baseColors[0] }

        val result = mutableListOf<Color>()
        val segments = baseColors.size - 1
        val stepsPerSegment = steps.toFloat() / segments

        for (i in 0 until steps) {
            val segmentIndex = (i / stepsPerSegment).toInt().coerceIn(0, segments - 1)

            val segmentProgress = (i % stepsPerSegment) / stepsPerSegment

            val startColor = baseColors[segmentIndex]
            val endColor = baseColors[segmentIndex + 1]

            val color = lerp(startColor, endColor, segmentProgress)
            result.add(color)
        }
        return result
    }
}