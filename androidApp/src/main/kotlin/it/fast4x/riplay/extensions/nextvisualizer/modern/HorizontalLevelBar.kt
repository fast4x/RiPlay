package it.fast4x.riplay.extensions.nextvisualizer.modern

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DefaultMeterColors = listOf(
    Color(0xFF4ADE80), // verde
    Color(0xFFFACC15), // giallo
    Color(0xFFF97316), // arancio
    Color(0xFFEF4444), // rosso
)

enum class Aggregation { MAX, MEAN, HYBRID }

@Composable
fun HorizontalLevelBar(
    modifier: Modifier = Modifier,
    state: EqualizerState = rememberLevelMeterState(),
    colors: List<Color> = DefaultMeterColors,
    barHeight: Dp = 10.dp,
    cornerRadius: Dp = barHeight / 2,
    capWidth: Dp = 2.dp,
    trackColor: Color = Color.White.copy(alpha = 0.12f),
    running: Boolean = true,
) {

    VisualizerFeedBinding(state)

    val started = rememberLifecycleStarted()
    val tick = remember { mutableFloatStateOf(0f) }

//    LaunchedEffect(state, started) {
//        var frame = 0
//        while (true) {
//            withInfiniteAnimationFrameNanos { now ->
//                state.step(now)
//                if (frame++ % 30 == 0) {
//                    Timber.d("EqDebug horizontalLevelBar target=${state.targets[0]} level=${state.levels[0]}")
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
                tick.floatValue = ++frame
            }
        }
    }

    val brushCache = remember { BrushCache() }

    Canvas(modifier) {
        tick.floatValue
        drawLevelMeter(state, colors, barHeight.toPx(), cornerRadius.toPx(), capWidth.toPx(), trackColor, brushCache)
    }
}


/** Gradient con hard stop: ogni colore occupa una fetta uguale del percorso. */
private fun buildSegmentBrush(colors: List<Color>, width: Float): Brush {
    val n = colors.size
    val stops = ArrayList<Pair<Float, Color>>(n * 2)
    for (i in 0 until n) {
        val from = i / n.toFloat()
        val to = (i + 1) / n.toFloat()
        stops.add(from to colors[i])
        stops.add(to to colors[i]) // offset duplicato = transizione netta
    }
    // vararg colorStops: serve lo spread, non esiste l'overload con List<Pair>
    return Brush.horizontalGradient(*stops.toTypedArray(), startX = 0f, endX = width)
}

private class BrushCache {
    var colorsHash = 0
    var width = 0f
    var brush: Brush? = null

    fun isValid(hash: Int, w: Float) = brush != null && colorsHash == hash && width == w
}

private fun DrawScope.drawLevelMeter(
    state: EqualizerState,
    colors: List<Color>,
    barHeightPx: Float,
    cornerRadiusPx: Float,
    capWidthPx: Float,
    trackColor: Color,
    cache: BrushCache,
) {
    val level = state.levels[0].coerceIn(0f, 1f)
    val peak = state.peaks[0].coerceIn(0f, 1f)
    val peakAge = state.peakAgeSec[0]

    val h = barHeightPx.coerceAtMost(size.height)
    if (h <= 0f) return
    val top = (size.height - h) / 2f
    val radius = CornerRadius(cornerRadiusPx.coerceAtMost(h / 2f))
    val capW = capWidthPx.coerceAtMost(size.width)

    val colorsHash = colors.hashCode()
    if (!cache.isValid(colorsHash, size.width)) {
        cache.brush = buildSegmentBrush(colors, size.width)
        cache.colorsHash = colorsHash
        cache.width = size.width
    }
    val brush = cache.brush ?: return

    // Tutto in spazio LTR; in RTL si specchia il canvas: barra e frammento
    // ereditano da soli geometria e colori corretti.
    val rtl = layoutDirection == LayoutDirection.Rtl
    withTransform({
        if (rtl) scale(-1f, 1f, pivot = Offset(size.width / 2f, size.height / 2f))
    }) {
        if (trackColor.alpha > 0f) {
            drawRoundRect(trackColor, Offset(0f, top), Size(size.width, h), radius)
        }

        if (level > 0.001f) {
            val levelW = (level * size.width).coerceAtLeast(2.dp.toPx()).coerceAtMost(size.width)
            drawRoundRect(brush, Offset(0f, top), Size(levelW, h), radius)
        }

        val holdSec = state.peakHoldMs / 1000f
        val fadeSec = (state.peakFadeMs / 1000f).coerceAtLeast(0.001f)
        val fade = 1f - ((peakAge - holdSec) / fadeSec).coerceIn(0f, 1f)
        if (fade > 0f && peak > 0.01f) {
            val peakW = peak * size.width
            val capLeft = (peakW - capW).coerceAtLeast(0f)
            // Frammento = ritaglio della barra completa: mostro il colore
            // della SUA posizione lungo il gradient, boundary inclusi.
            clipRect(capLeft, top, capLeft + capW, top + h) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, h),
                    cornerRadius = radius,
                    alpha = fade,
                )
            }
        }
    }
}