package it.fast4x.riplay.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STEP 3h (migliorie visive): SLIDER PREMIUM per timeline e volume.
 *
 * Perche' custom e non material3.Slider: lo slider Material ha un thumb
 * ingombrante, track fissa e nessuna informazione di anteprima; per un
 * player desktop l'aspettativa e' lo stile dei player "chic" (YouTube/
 * Spotify): track sottile che SI INGRANDISCE al passaggio del mouse,
 * riempimento a gradiente, thumb bianco con alone che si anima, e sulla
 * timeline una BOLLA con il tempo del punto puntato (anteprima del seek
 * PRIMA di confermarlo col click).
 *
 * Comportamenti chiave:
 *  - DRAG LOCALE: durante il trascinamento il valore mostrato e' LOCALE
 *    (dragValue); l'esterno non fa "saltare" il cursore. La callback
 *    [onValueChange] riceve il valore continuo (utile per il volume) e
 *    [onValueChangeFinished] il valore alla RILASCIO (utile per la
 *    timeline: UN solo seekTo alla fine, non decine di comandi JS al
 *    bridge KCEF durante il drag);
 *  - CLICK = SEEK immediato: premere in un punto della track porta gia'
 *    il cursore li' e al rilascio conferma (come YouTube);
 *  - BUBBLA TEMPO: con [showTimeBubble] al passaggio mouse/drag appare
 *    sopra il puntatore il tempo formattato del punto puntato
 *    (formattazione compatta h:mm:ss / m:ss, diversa da formatTimestamp
 *    che scrive sempre 00:mm:ss).
 *
 * Disegno (Canvas, nessuna dipendenza extra):
 *  - track arrotondata bianca al 20% su fondo scuro;
 *  - parte attiva a GRADIENTE (RiPlayAccentLight -> [accent]);
 *  - thumb bianco con alone dell'accento (glow) che appare/scala con
 *    animazioni; l'intera area e' cliccabile (40dp di altezza).
 */

/** Accento premium dell'app (verde brillante, usato da slider/barre/player). */
val RiPlayAccent = Color(0xFF30D158)

/** Testa del gradiente della parte attiva (verde piu' chiaro). */
val RiPlayAccentLight = Color(0xFF6BE894)

/**
 * Tempo compatto per la bolla: 2:34 invece di 00:02:34 (piu' leggibile
 * in uno spazio piccolo).
 */
internal fun formatShortTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
           else String.format("%d:%02d", m, s)
}

@Composable
fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    showTimeBubble: Boolean = false,
    accent: Color = RiPlayAccent
) {
    // Callback sempre aggiornate: il pointerInput vive a lungo e senza
    // rememberUpdatedState catturerebbe le lambda della PRIMA composizione.
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    var hoverFraction by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(value) }

    val hovered = hoverFraction != null || dragging
    val shownValue = if (dragging) dragValue else value
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val fraction = ((shownValue - valueRange.start) / range).coerceIn(0f, 1f)

    // Animazioni: la track si ingrandisce, il thumb scala e l'alone appare
    val trackHeight by animateDpAsState(
        targetValue = if (hovered) 8.dp else 5.dp,
        animationSpec = tween(140),
        label = "trackHeight"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (hovered) 1f else 0.78f,
        animationSpec = tween(140),
        label = "thumbScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (hovered) 0.32f else 0f,
        animationSpec = tween(160),
        label = "glowAlpha"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (hovered) 1f else 0f,
        animationSpec = tween(120),
        label = "bubbleAlpha"
    )

    BoxWithConstraints(modifier.height(40.dp)) {
        val wPx = constraints.maxWidth.toFloat()
        // STEP 3h: maxWidth capturato a livello BoxWithConstraints: dentro
        // il Box interno l'implicit receiver cambierebbe (BoxScope) e
        // maxWidth non sarebbe piu' accessibile
        val maxWDp = maxWidth

        // Zona interattiva (tutto il box, 40dp di bersaglio)
        Box(
            Modifier
                .fillMaxSize()
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(valueRange, wPx) {
                    val rPx = 7.dp.toPx()
                    val usable = (wPx - 2 * rPx).coerceAtLeast(1f)
                    fun fractionAt(x: Float): Float =
                        ((x - rPx) / usable).coerceIn(0f, 1f)
                    fun valueAt(x: Float): Float =
                        valueRange.start + fractionAt(x) * range

                    awaitPointerEventScope {
                        var pressed = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            when (event.type) {
                                PointerEventType.Press -> {
                                    pressed = true
                                    dragging = true
                                    dragValue = valueAt(change.position.x)
                                    hoverFraction = fractionAt(change.position.x)
                                    change.consume()
                                }
                                PointerEventType.Move -> {
                                    if (pressed) {
                                        dragValue = valueAt(change.position.x)
                                        hoverFraction = fractionAt(change.position.x)
                                        currentOnValueChange(dragValue)
                                        change.consume()
                                    } else {
                                        hoverFraction = fractionAt(change.position.x)
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (pressed) {
                                        pressed = false
                                        val final = dragValue
                                        currentOnValueChange(final)
                                        currentOnFinished?.invoke(final)
                                        dragging = false
                                        change.consume()
                                    }
                                }
                                PointerEventType.Exit -> {
                                    if (!pressed) hoverFraction = null
                                }
                            }
                        }
                    }
                }
        ) {
            // Track + thumb (Canvas non ritaglia il disegno: il thumb puo'
            // essere piu' grande della track senza problemi)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .align(Alignment.CenterStart)
            ) {
                val w = size.width
                val h = size.height
                val r = h / 2f
                val thumbR = 7.dp.toPx() * thumbScale

                // Track inattiva
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.20f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r)
                )

                val usable = (w - 2 * r).coerceAtLeast(1f)
                val cx = r + fraction * usable
                val cy = h / 2f

                // Parte attiva a gradiente
                val activeW = cx.coerceAtLeast(r)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(RiPlayAccentLight, accent),
                        startX = 0f,
                        endX = w
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(activeW, h),
                    cornerRadius = CornerRadius(r, r)
                )

                // Alone (glow) + thumb bianco
                if (glowAlpha > 0.01f) {
                    drawCircle(
                        color = accent.copy(alpha = glowAlpha),
                        radius = thumbR * 1.8f,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = Offset(cx, cy)
                )
            }

            // Bolla del tempo (anteprima del punto puntato): segue il
            // puntatore, si clampa ai bordi per non uscire dalla finestra
            if (showTimeBubble && bubbleAlpha > 0.05f) {
                val bubbleFraction = hoverFraction ?: fraction
                val bubbleValue = valueRange.start + bubbleFraction * range
                val bubbleW = 64.dp
                val centerDp = maxWDp * bubbleFraction
                val bubbleX = (centerDp - bubbleW / 2).coerceIn(0.dp, maxWDp - bubbleW)
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = bubbleX, y = 1.dp)
                        .alpha(bubbleAlpha)
                        .width(bubbleW)
                        .height(19.dp)
                        .background(Color(0xFF232423), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatShortTime(bubbleValue.toLong()),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
