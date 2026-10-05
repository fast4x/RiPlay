package it.fast4x.riplay.extensions.nextvisualizer.modern

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun rememberEqualizerState(
    barCount: Int = 32,
    inputGain: Float = 1.8f,
    peakHoldMs: Float = 220f,
    peakFadeMs: Float = 900f,
    peakGravity: Float = 0.55f,
    simulationEnabled: Boolean = true,
): EqualizerState = remember {
    EqualizerState(barCount, inputGain, peakHoldMs, peakFadeMs, peakGravity, simulationEnabled)
}

@Composable
fun VisualizerFeedBinding(state: EqualizerState) {
    DisposableEffect(state) {
        EqualizerFeed.register(state)
        onDispose { EqualizerFeed.unregister(state) }
    }
}

@Composable
fun rememberLevelMeterState(
    inputGain: Float = 2f,
    peakHoldMs: Float = 250f,
    peakFadeMs: Float = 900f,
    peakGravity: Float = 0.55f,
    simulationEnabled: Boolean = true,
): EqualizerState = remember {
    EqualizerState(
        barCount = 1, inputGain = inputGain,
        peakHoldMs = peakHoldMs, peakFadeMs = peakFadeMs,
        peakGravity = peakGravity, simulationEnabled = simulationEnabled,
    ).apply {
        spectrumTiltDbPerOctave = 0f
        levelAggregation = Aggregation.HYBRID
        hybridPeakMix = 0.6f
        autoRange = false
        noiseFloor = 0.05f
        gamma = 1.3f
    }
}

@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    state: EqualizerState = rememberEqualizerState(),
    barColor: Color = Color.White.copy(alpha = 0.92f),
    peakColor: Color = Color.White,
    barCornerRadius: Dp = 2.dp,
    capHeight: Dp = 3.dp,
    barSpacing: Dp = 3.dp,
    running: Boolean = true,
) {
    VisualizerFeedBinding(state)

    val started = rememberLifecycleStarted()
    val tick = remember { mutableFloatStateOf(0f) } // Contatore che guida solo il draw

//    LaunchedEffect(state, started) {
//        var frame = 0
//        while (true) {
//            withInfiniteAnimationFrameNanos { now ->
//                state.step(now)
//                if (frame++ % 30 == 0) {
//                    Timber.d("EqDebug EqualizerBars target=${state.targets[0]} level=${state.levels[0]}")
//                }
//                tick.floatValue = frame.toFloat()
//            }
//        }
//    }

    LaunchedEffect(state, running, started) {
        if (!running || !started) return@LaunchedEffect
        var frame = 0f
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                state.step(now)
                tick.floatValue = ++frame // invalida SOLO la fase di disegno
            }
        }
    }

    Canvas(modifier) {
        tick.floatValue // lettura necessaria: sottoscrive l'invalidazione del draw
        drawEqualizer(state, barColor, peakColor, barCornerRadius, capHeight, barSpacing)
    }
}

private fun DrawScope.drawEqualizer(
    state: EqualizerState,
    barColor: Color,
    peakColor: Color,
    cornerRadius: Dp,
    capHeight: Dp,
    spacing: Dp,
) {
    val n = state.barCount
    if (n == 0) return
    val levels = state.levels       // snapshot locale dei buffer:
    val peaks = state.peaks         // consistente per tutto il passaggio
    val peakAge = state.peakAgeSec

    val bottom = size.height
    val gap = spacing.toPx()
    val capH = capHeight.toPx().coerceAtMost(bottom)
    val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
    val radius = CornerRadius(cornerRadius.toPx().coerceAtMost(barW / 2f))
    val minBar = 2.dp.toPx()
    val holdSec = state.peakHoldMs / 1000f
    val fadeSec = (state.peakFadeMs / 1000f).coerceAtLeast(0.001f)

    var x = 0f
    for (i in 0 until n) {
        val level = levels[i].coerceIn(0f, 1f)
        val h = (level * bottom).coerceAtLeast(if (level > 0.01f) minBar else 0f)
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, bottom - h),
            size = Size(barW, h),
            cornerRadius = radius,
        )

        val fade = 1f - ((peakAge[i] - holdSec) / fadeSec).coerceIn(0f, 1f)
        if (fade > 0f && peaks[i] > 0.01f) {
            val capTop = bottom - peaks[i].coerceIn(0f, 1f) * bottom - capH
            drawRoundRect(
                color = peakColor.copy(alpha = peakColor.alpha * fade),
                topLeft = Offset(x, capTop),
                size = Size(barW, capH),
                cornerRadius = radius,
            )
        }
        x += barW + gap
    }
}

@Composable
fun rememberLifecycleStarted(): Boolean {
    val owner = LocalLifecycleOwner.current
    var started by remember {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> started = true
                Lifecycle.Event.ON_STOP -> started = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return started
}