package it.fast4x.riplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/** stili come su Android (AudioVisualizerBaseStyle) */
enum class VisualizerStyle {
    NONE,
    SPECTRUM,
    LEVEL;


    fun next(): VisualizerStyle = when (this) {
        NONE -> SPECTRUM
        SPECTRUM -> LEVEL
        LEVEL -> NONE
    }
}

/** numero di barre dello spettro */
private const val SPECTRUM_BARS = 52

/**
 * Visualizer del player. [seed] cambia la "firma" dell'animazione a ogni
 * brano (cosi' ogni canzone ha il suo spettro); [isPlaying] guida l'energia
 * complessiva (in pausa lo spettro si spegne dolcemente).
 */
@Composable
fun AudioVisualizer(
    style: VisualizerStyle,
    isPlaying: Boolean,
    seed: String,
    modifier: Modifier = Modifier,
    color: Color = RiPlayAccent
) {
    if (style == VisualizerStyle.NONE) return

    // energia complessiva: 1 = suona, ~0 = in pausa (decadimento dolce).
    // STEP 6b (richiesta utente n.2): per LEVEL l'attacco e' MOLTO piu'
    // deciso (stiffness alto): il livello deve marcarsi subito quando parte
    // la riproduzione, senza la salita lenta dello spettro.
    val activity = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, style) {
        activity.animateTo(
            targetValue = if (isPlaying) 1f else 0.04f,
            animationSpec = when {
                !isPlaying -> tween(900)
                style == VisualizerStyle.LEVEL -> spring(dampingRatio = 0.9f, stiffness = 900f)
                else -> spring(dampingRatio = 0.85f, stiffness = 90f)
            }
        )
    }

    // seed numerico stabile per il brano corrente
    val seedNum = remember(seed) { seed.fold(1125899906842597L) { acc, c -> 31 * acc + c.code } }

    // altezze normalizzate 0..1 per ogni barra (state: il Canvas si ridisegna
    // a ogni frame mentre suona; un FloatArray nuovo a frame e' voluto:
    // e' immutabile e senza contese tra il loop e il draw thread)
    var heights by remember { mutableStateOf(FloatArray(SPECTRUM_BARS) { 0.12f }) }

    LaunchedEffect(isPlaying, style, seedNum) {
        if (!isPlaying) {
            // in pausa: nessun loop, le barre restano al decadimento gestito
            // dall'activity (il canvas si ridisegna finche' l'animata va in 0.04)
            return@LaunchedEffect
        }
        val start = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val t = (now - start) / 1_000_000_000.0
                val arr = FloatArray(SPECTRUM_BARS) { i ->
                    val x = i.toFloat() / SPECTRUM_BARS
                    // componente "basso" (barre a sinistra piu' cariche)
                    val bass = 0.45f * (1f - x * 0.75f)
                    // onde multiple con fasi derivate dal seed del brano:
                    // combinazione deterministica ma "viva" (mai uguale a se stessa)
                    val p1 = (seedNum % 997) / 997.0 * 6.283 + i * 0.55
                    val p2 = (seedNum % 733) / 733.0 * 6.283 + i * 0.23
                    val w1 = kotlin.math.sin(t * (2.1 + (seedNum % 17) / 40.0) + p1)
                    val w2 = kotlin.math.sin(t * (3.7 + (seedNum % 23) / 30.0) + p2)
                    val w3 = kotlin.math.sin(t * 0.9 + i * 0.11)
                    // "beat" globale lento: respiro del brano
                    val beat = 0.82f + 0.18f * kotlin.math.sin(t * 3.49).toFloat()
                    val v = bass + 0.34f * (w1.toFloat() * 0.6f + w2.toFloat() * 0.4f) + 0.18f * w3.toFloat()
                    // clamp con pavimento minimo (le barre non spariscono mai del tutto)
                    (v * beat).coerceIn(0.06f, 1f)
                }
                heights = arr
            }
        }
    }

    when (style) {
        VisualizerStyle.NONE -> Unit
        VisualizerStyle.SPECTRUM -> SpectrumCanvas(heights, activity.value, color, modifier)
        VisualizerStyle.LEVEL -> LevelCanvas(activity.value, isPlaying, seedNum, color, modifier)
    }
}

/** SPECTRUM: barre verticali arrotondate con leggero gradiente */
@Composable
private fun SpectrumCanvas(
    heights: FloatArray,
    activity: Float,
    color: Color,
    modifier: Modifier
) {
    Canvas(modifier) {
        val gap = size.width * 0.004f
        val barWidth = (size.width - gap * (SPECTRUM_BARS - 1)) / SPECTRUM_BARS
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        // le barre usano l'84% dell'altezza: il restante fa da spazio al
        // riflesso attenuato (il DrawScope CLIPPA ai bounds: tutto dentro)
        val barArea = size.height * 0.84f
        val mirrorTop = size.height * 0.87f

        for (i in 0 until SPECTRUM_BARS) {
            val h = (heights[i] * activity).coerceIn(0.02f, 1f) * barArea
            val x = i * (barWidth + gap)
            val y = mirrorTop - h
            drawRoundRect(
                color = color.copy(alpha = 0.28f + 0.62f * heights[i]),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = corner
            )
            // riflesso sottile sotto (specchio attenuato, stile player chic)
            drawRoundRect(
                color = color.copy(alpha = 0.12f * activity),
                topLeft = Offset(x, mirrorTop + 2f),
                size = Size(barWidth, (h * 0.24f).coerceAtMost(size.height - mirrorTop - 3f)),
                cornerRadius = corner
            )
        }
    }
}

/**
 * LEVEL: barra orizzontale di livello con tacche leggere.
 *
 * La DINAMICA di un VU-meter che segue il battito del
 * brano (il ritmo e' derivato dal seed: ogni canzone ha il suo groove):
 *  - "kick": colpo netto ad ogni battito, ottenuto elevando a potenza un
 *    seno (picchi stretti: attacco istantaneo, rilascio rapido);
 *  - "off": colpo secondario sul controtempo, piu' leggero;
 *  - "jitter": fruscio veloce che rende il movimento irregolare come
 *    l'audio reale (nessun loop perfettamente periodico);
 *  - "breath": lenta variazione di fondo, per non rendere meccanico il
 *    minimo della barra.
 * Il livello viene assegnato GREZZO ogni frame (nessuna interpolazione):
 * la reattivita' e' quella del valore calcolato.
 */
@Composable
private fun LevelCanvas(
    activity: Float,
    isPlaying: Boolean,
    seedNum: Long,
    color: Color,
    modifier: Modifier
) {
    // in pausa il loop si ferma: il collasso lo gestisce activity
    var level by remember { mutableStateOf(0.2f) }
    LaunchedEffect(isPlaying, seedNum) {
        if (!isPlaying) return@LaunchedEffect
        val start = System.nanoTime()
        val bps = 1.65 + (seedNum % 11) / 11.0 // battiti al secondo (~99-160 BPM)
        val ph1 = (seedNum % 911) / 911.0 * 6.283
        val ph2 = (seedNum % 577) / 577.0 * 6.283
        while (true) {
            withFrameNanos { now ->
                val t = (now - start) / 1_000_000_000.0
                val beat = t * bps * 6.283 + ph1
                val kick = (0.5 + 0.5 * kotlin.math.sin(beat)).pow(6.0)
                val off = (0.5 + 0.5 * kotlin.math.sin(beat * 0.5 + 3.1416)).pow(9.0) * 0.4
                val jitter = 0.5 + 0.5 * kotlin.math.sin(t * 21.0 + ph2) * kotlin.math.sin(t * 8.1 + ph1)
                val breath = 0.5 + 0.5 * kotlin.math.sin(t * 0.6 + ph2)
                level = (0.14 + 0.58 * kick + 0.16 * off +
                        0.14 * jitter * (0.4 + 0.6 * kick) + 0.10 * breath)
                    .coerceIn(0.08, 0.98).toFloat()
            }
        }
    }

    Canvas(modifier) {
        val trackHeight = size.height * 0.5f
        val y = (size.height - trackHeight) / 2f
        // track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, y),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )
        // fill
        val w = size.width * (level * activity).coerceIn(0.02f, 1f)
        drawRoundRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(0f, y),
            size = Size(w, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )
        // puntatore
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(w - 2f, y - 2f),
            size = Size(4f, trackHeight + 4f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // tacche di riferimento
        for (k in 1..4) {
            val x = size.width * k / 5f
            drawLine(
                color = Color.Black.copy(alpha = 0.25f),
                start = Offset(x, y + trackHeight * 0.2f),
                end = Offset(x, y + trackHeight * 0.8f),
                strokeWidth = 2f
            )
        }
    }
}
