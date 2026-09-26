package it.fast4x.riplay.extensions.experimental.smoothloader

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private fun fract(value: Float): Float = value - floor(value)

/** Stili di animazione disponibili per [SmoothLoader]. */
enum class LoaderStyle {

    /** Default rotante di M3 */
    Default,

    /** Arco rotante stile M3, con sweep che "respira". */
    Arc,

    /** Punti che rimbalzano in sequenza, stile "typing indicator". */
    Dots,

    /** Punti in orbita ellittica con falso effetto 3D. */
    Orbit,

    /** Barrette ondulate, stile equalizzatore. */
    Wave,

    /** Anelli concentrici in espansione, stile ripple. */
    Pulse,

    /** Cerchio morbido con squash & stretch rotante, effetto "gelatina". */
    Blob,

    /** Segmenti circolari con scia, stile spinner classico. */
    Dash,

    /** Blob morbidi che si fondono e si separano, effetto "gooey". */
    Gooey,

    /** Quadrati arrotondati che si rincorrono e respirano, stile Material. */
    Squares,

    /** Arco "cometa" con scia sfumata a gradiente. */
    Gradient,

    /** Doppia elica di punti in pseudo-3D, effetto DNA. */
    Helix,

    /** Pallina che rimbalza con squash & stretch e ombra dinamica. */
    Bounce,

    /** Poligoni casuali che ruotano su uno sfondo pulsante */
    Polygons;

    val displayName: String
        @Composable
        get() = when (this) {
            Default -> stringResource(R.string.loader_style_default)
            Arc -> stringResource(R.string.loader_style_arc)
            Dots -> stringResource(R.string.loader_style_dots)
            Orbit -> stringResource(R.string.loader_style_orbit)
            Wave -> stringResource(R.string.loader_style_wave)
            Pulse -> stringResource(R.string.loader_style_pulse)
            Blob -> stringResource(R.string.loader_style_blob)
            Dash -> stringResource(R.string.loader_style_dash)
            Gooey -> stringResource(R.string.loader_style_gooey)
            Squares -> stringResource(R.string.loader_style_squares)
            Gradient -> stringResource(R.string.loader_style_gradient)
            Helix -> stringResource(R.string.loader_style_helix)
            Bounce -> stringResource(R.string.loader_style_bounce)
            Polygons -> stringResource(R.string.loader_style_polygons)
        }
}

/**
 * Loader animato ridimensionabile, ispirato all'API del CircularProgressIndicator M3.
 *
 * Gli stati animati vengono letti DENTRO la lambda di disegno: ogni frame
 * invalida solo la fase di *draw*, mai la recomposizione.
 *
 * @param style          stile dell'animazione.
 * @param size           dimensione complessiva del loader.
 * @param color          colore principale.
 * @param trackColor     colore della traccia (usato da [LoaderStyle.Arc] e [LoaderStyle.Gradient]).
 * @param strokeWidth    spessore del tratto; se non specificato è proporzionale a [size].
 * @param durationMillis durata di un ciclo completo; se < 0 usa la durata naturale dello stile.
 * @param elementCount   numero di punti/barre/anelli/segmenti; se <= 0 usa il default dello stile.
 */
@Composable
fun SmoothLoader(
    modifier: Modifier = Modifier,
    isStartingUp: Boolean = false,
    style: LoaderStyle,
    size: Dp = 48.dp,
    color: Color = if (isStartingUp) MaterialTheme.colorScheme.surface else colorPalette().accent,
    trackColor: Color = color.copy(alpha = 0.15f),
    strokeWidth: Dp = Dp.Unspecified,
    durationMillis: Int = -1,
    elementCount: Int = -1,

) {
    val stroke = if (strokeWidth.isSpecified) strokeWidth else size * 0.09f
    val duration = if (durationMillis > 0) durationMillis else defaultDurationMillis(style)
    val count = elementCount.takeIf { it > 0 }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when (style) {
            LoaderStyle.Arc      -> ArcLoader(color, trackColor, stroke, duration)
            LoaderStyle.Dots     -> DotsLoader(color, duration, count ?: 3)
            LoaderStyle.Orbit    -> OrbitLoader(color, duration, count ?: 5)
            LoaderStyle.Wave     -> WaveLoader(color, duration, count ?: 5)
            LoaderStyle.Pulse    -> PulseLoader(color, duration, count ?: 3)
            LoaderStyle.Blob     -> BlobLoader(color, duration)
            LoaderStyle.Dash     -> DashLoader(color, duration, count ?: 8)
            LoaderStyle.Gooey    -> GooeyLoader(color, duration, count ?: 4)
            LoaderStyle.Squares  -> SquaresLoader(color, duration, count ?: 4)
            LoaderStyle.Gradient -> GradientLoader(color, trackColor, stroke, duration)
            LoaderStyle.Helix    -> HelixLoader(color, duration, count ?: 9)
            LoaderStyle.Bounce   -> BounceLoader(color, duration)
            LoaderStyle.Polygons -> PolygonsLoader(color, duration, count ?: 3)
            LoaderStyle.Default  -> DefaultLoader(color, trackColor, stroke)

        }
    }
}

/** Durata "naturale" di ogni stile: ogni effetto ha il suo ritmo giusto. */
private fun defaultDurationMillis(style: LoaderStyle): Int = when (style) {
    LoaderStyle.Arc      -> 1200
    LoaderStyle.Dots     -> 1100
    LoaderStyle.Orbit    -> 1800
    LoaderStyle.Wave     -> 1000
    LoaderStyle.Pulse    -> 1600
    LoaderStyle.Blob     -> 1100
    LoaderStyle.Dash     -> 1000
    LoaderStyle.Gooey    -> 1400
    LoaderStyle.Squares  -> 1600
    LoaderStyle.Gradient -> 1300
    LoaderStyle.Helix    -> 1600
    LoaderStyle.Bounce   -> 900
    LoaderStyle.Polygons -> 1800
    LoaderStyle.Default  -> 1000 // come Polygons
}

// ---------------------------------------------------------------- Arc

private const val MinSweep = 30f
private const val MaxSweep = 300f

@Composable
private fun ArcLoader(color: Color, trackColor: Color, strokeWidth: Dp, durationMillis: Int) {
    val transition = rememberInfiniteTransition(label = "arc")

    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "rotation"
    )
    val sweep by transition.animateFloat(
        initialValue = MinSweep, targetValue = MinSweep,
        animationSpec = infiniteRepeatable(
            keyframes {
                MinSweep at 0
                MaxSweep at durationMillis / 2 using FastOutSlowInEasing
                MinSweep at durationMillis
            }
        ),
        label = "sweep"
    )

    Canvas(Modifier.fillMaxSize()) {
        val strokePx = strokeWidth.toPx()
        val diameter = size.minDimension - strokePx
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val strokeStyle = Stroke(strokePx, cap = StrokeCap.Round)

        drawArc(trackColor, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = strokeStyle)
        drawArc(color, rotation, sweep, false, topLeft = topLeft, size = arcSize, style = strokeStyle)
    }
}

// ---------------------------------------------------------------- Dots

@Composable
private fun DotsLoader(color: Color, durationMillis: Int, dotCount: Int) {
    val progress by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.09f
        val spacing = radius * 2.6f
        val centerY = size.height / 2f

        repeat(dotCount) { i ->
            // Ogni punto è sfasato di un periodo rispetto al precedente
            val phase = fract(progress + i.toFloat() / dotCount)
            val bounce = sin(phase * PI).toFloat() // 0 → 1 → 0

            drawCircle(
                color = color,
                radius = radius * (0.55f + 0.45f * bounce),
                center = Offset(
                    x = size.width / 2f + (i - (dotCount - 1) / 2f) * spacing,
                    y = centerY - bounce * size.minDimension * 0.18f
                ),
                alpha = 0.35f + 0.65f * bounce
            )
        }
    }
}

// ---------------------------------------------------------------- Orbit

@Composable
private fun OrbitLoader(color: Color, durationMillis: Int, dotCount: Int) {
    val progress by rememberInfiniteTransition(label = "orbit").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val dotRadius = size.minDimension * 0.07f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radiusX = cx - dotRadius * 2.5f
        val radiusY = radiusX * 0.45f // ellisse schiacciata → falso 3D

        drawCircle(color = color, radius = dotRadius * 0.8f, center = Offset(cx, cy))

        repeat(dotCount) { i ->
            val angle = 2.0 * PI * fract(progress + i.toFloat() / dotCount)
            val depth = (sin(angle).toFloat() + 1f) / 2f // 1 = davanti, 0 = dietro

            drawCircle(
                color = color,
                radius = dotRadius * (0.5f + 0.5f * depth),
                center = Offset(
                    x = cx + radiusX * cos(angle).toFloat(),
                    y = cy + radiusY * sin(angle).toFloat()
                ),
                alpha = 0.25f + 0.75f * depth
            )
        }
    }
}

// ---------------------------------------------------------------- Wave

@Composable
private fun WaveLoader(color: Color, durationMillis: Int, barCount: Int) {
    val progress by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val barWidth = size.minDimension * 0.10f
        val gap = barWidth * 0.7f
        val centerY = size.height / 2f
        val maxHalfHeight = size.height * 0.32f
        var x = (size.width - (barCount * barWidth + (barCount - 1) * gap)) / 2f + barWidth / 2f

        repeat(barCount) { i ->
            val wave = 0.5f + 0.5f * sin(2.0 * PI * (progress + i.toFloat() / barCount)).toFloat()
            val halfHeight = maxHalfHeight * (0.2f + 0.8f * wave)

            drawLine(
                color = color,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
            x += barWidth + gap
        }
    }
}

// ---------------------------------------------------------------- Pulse

@Composable
private fun PulseLoader(color: Color, durationMillis: Int, ringCount: Int) {
    val progress by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f * 0.94f
        val coreRadius = maxRadius * 0.22f

        // Nucleo centrale
        drawCircle(color = color, radius = coreRadius, center = center)

        // Anelli in espansione, sfasati tra loro
        repeat(ringCount) { i ->
            val phase = fract(progress + i.toFloat() / ringCount)
            val eased = 1f - (1f - phase) * (1f - phase) // ease-out

            drawCircle(
                color = color,
                radius = coreRadius + (maxRadius - coreRadius) * eased,
                center = center,
                alpha = (1f - phase) * 0.6f,
                style = Stroke(maxRadius * 0.08f, cap = StrokeCap.Round)
            )
        }
    }
}

// ---------------------------------------------------------------- Blob

@Composable
private fun BlobLoader(color: Color, durationMillis: Int) {
    val progress by rememberInfiniteTransition(label = "blob").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.28f
        val wobble = sin(2.0 * PI * progress).toFloat() // -1 → 1 → -1

        withTransform({
            rotate(degrees = progress * 360f, pivot = center)
            scale(scaleX = 1f + 0.22f * wobble, scaleY = 1f - 0.22f * wobble, pivot = center)
        }) {
            drawCircle(color = color, radius = radius, center = center)
        }
    }
}

// ---------------------------------------------------------------- Dash

@Composable
private fun DashLoader(color: Color, durationMillis: Int, segmentCount: Int) {
    val progress by rememberInfiniteTransition(label = "dash").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.08f
        val tickLength = stroke * 1.7f
        val radius = size.minDimension / 2f - tickLength - stroke * 0.4f
        val center = Offset(size.width / 2f, size.height / 2f)
        val step = 360f / segmentCount

        rotate(degrees = progress * 360f, pivot = center) {
            repeat(segmentCount) { i ->
                // I segmenti "dietro" la testa sfumano progressivamente → scia
                val alpha = 1f - i.toFloat() / segmentCount

                rotate(degrees = -i * step) {
                    drawLine(
                        color = color,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y - radius - tickLength),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                        alpha = 0.15f + 0.85f * alpha
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Gooey

@Composable
private fun GooeyLoader(color: Color, durationMillis: Int, dotCount: Int) {
    val progress by rememberInfiniteTransition(label = "gooey").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxOrbit = size.minDimension * 0.26f
        val dotRadius = size.minDimension * 0.105f

        // L'orbita "respira": 0 → tutto fuso in un blob, 1 → splat aperto
        val open = 0.5f - 0.5f * cos(2.0 * PI * progress).toFloat()
        val orbit = maxOrbit * (0.22f + 0.78f * open)

        repeat(dotCount) { i ->
            val angle = 2.0 * PI * (progress + i.toFloat() / dotCount)
            val dot = Offset(
                center.x + orbit * cos(angle).toFloat(),
                center.y + orbit * sin(angle).toFloat()
            )
            // Braccio nucleo→satellite con cap tondo = fusione morbida tra le forme
            drawLine(color, center, dot, strokeWidth = dotRadius * 1.5f, cap = StrokeCap.Round)
            drawCircle(color, dotRadius, dot)
        }
        drawCircle(color, dotRadius * 1.15f, center)
    }
}

// ---------------------------------------------------------------- Squares

@Composable
private fun SquaresLoader(color: Color, durationMillis: Int, squareCount: Int) {
    val progress by rememberInfiniteTransition(label = "squares").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val orbit = size.minDimension * 0.24f
        val square = size.minDimension * 0.18f

        repeat(squareCount) { i ->
            val phase = fract(progress + i.toFloat() / squareCount)
            val angle = phase * 2.0 * PI
            val pos = Offset(
                center.x + orbit * cos(angle).toFloat(),
                center.y + orbit * sin(angle).toFloat()
            )
            // Cresce mentre "insegue", si sgonfia all'arrivo
            val grow = 0.5f + 0.5f * sin(phase * 2.0 * PI).toFloat()
            val scale = 0.35f + 0.65f * grow

            withTransform({
                rotate(degrees = phase * 270f, pivot = pos)
                scale(scaleX = scale, scaleY = scale, pivot = pos)
            }) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pos.x - square / 2f, pos.y - square / 2f),
                    size = Size(square, square),
                    cornerRadius = CornerRadius(square * 0.22f),
                    alpha = 0.4f + 0.6f * grow
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Gradient

@Composable
private fun GradientLoader(color: Color, trackColor: Color, strokeWidth: Dp, durationMillis: Int) {
    val rotation by rememberInfiniteTransition(label = "gradient").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "rotation"
    )

    Canvas(Modifier.fillMaxSize()) {
        val strokePx = strokeWidth.toPx()
        val diameter = size.minDimension - strokePx
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val strokeStyle = Stroke(strokePx, cap = StrokeCap.Round)
        val sweep = 130f

        drawArc(trackColor, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = strokeStyle)

        rotate(rotation) {
            // Gradiente che parte trasparente in coda e arriva pieno in testa: effetto cometa.
            // L'arco parte a 0° (ore 3), stessa origine dello sweep gradient → fade allineato.
            val brush = Brush.sweepGradient(
                0f to color.copy(alpha = 0f),
                (sweep / 360f) to color,
                center = center
            )
            drawArc(
                brush = brush,
                startAngle = 0f, sweepAngle = sweep, useCenter = false,
                topLeft = topLeft, size = arcSize, style = strokeStyle
            )
        }
    }
}

// ---------------------------------------------------------------- Helix

@Composable
private fun HelixLoader(color: Color, durationMillis: Int, dotCount: Int) {
    val progress by rememberInfiniteTransition(label = "helix").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val amplitude = size.height * 0.32f
        val radius = size.minDimension * 0.065f
        val spread = size.minDimension * 0.12f // semi-asse orizzontale → pseudo-3D

        repeat(dotCount) { i ->
            val phase = 2.0 * PI * progress + i * (2.0 * PI / dotCount)
            val wave = sin(phase).toFloat()
            val depth = (cos(phase).toFloat() + 1f) / 2f // 1 = davanti, 0 = dietro

            // Filamento A
            drawCircle(
                color = color,
                radius = radius * (0.55f + 0.45f * depth),
                center = Offset(cx + (depth - 0.5f) * 2f * spread, cy + amplitude * wave),
                alpha = 0.35f + 0.65f * depth
            )
            // Filamento B: la stessa elica, in controfase
            drawCircle(
                color = color,
                radius = radius * (0.55f + 0.45f * (1f - depth)),
                center = Offset(cx - (depth - 0.5f) * 2f * spread, cy - amplitude * wave),
                alpha = 0.35f + 0.65f * (1f - depth)
            )
        }
    }
}

// ---------------------------------------------------------------- Bounce

@Composable
private fun BounceLoader(color: Color, durationMillis: Int) {
    val progress by rememberInfiniteTransition(label = "bounce").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )

    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val floorY = size.height * 0.84f
        val travel = size.height * 0.52f
        val ballR = size.minDimension * 0.11f

        // |sin| → veloce a terra, "sospesa" all'apice: rimbalzo credibile
        val lift = abs(sin(PI * progress)).toFloat()
        val squash = (1f - lift) * (1f - lift) // si schiaccia solo vicino al suolo
        val sx = 1f + 0.35f * squash
        val sy = 1f - 0.30f * squash

        val ballCenter = Offset(cx, floorY - travel * lift - ballR * sy)

        // Ombra: più piccola e tenue quando la palla è in alto
        val shadowW = ballR * (2.3f - 1.2f * lift)
        drawOval(
            color = color,
            topLeft = Offset(cx - shadowW / 2f, floorY - ballR * 0.30f),
            size = Size(shadowW, ballR * 0.55f),
            alpha = 0.30f * (1f - lift * 0.75f)
        )

        withTransform({ scale(scaleX = sx, scaleY = sy, pivot = ballCenter) }) {
            drawCircle(color, ballR, ballCenter)
        }
    }
}

// ---------------------------------------------------------------- Polygons (fused v2)

/**
 * Fusione delle due versioni:
 * - la tua: poligoni che cambiano forma nel tempo (qui con MORPHING continuo, non crossfade)
 * - la mia: un solo Canvas in draw-phase, zero recomposition, tutto proporzionale a `size`
 */
@Composable
private fun PolygonsLoader(color: Color, durationMillis: Int, ringCount: Int) {
    val progress by rememberInfiniteTransition(label = "polygons").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress"
    )
    // Riusato per ogni anello: rewind() ricicla il buffer interno, zero alloc per frame
    val reusablePath = remember { Path() }

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f * 0.9f
        val strokePx = maxRadius * 0.06f
        val pulse = 0.5f - 0.5f * cos(2.0 * PI * progress).toFloat()

        // Sfondo pulsante (la tua idea, portata in draw-phase)
        drawCircle(
            color = color,
            radius = maxRadius * (0.55f + 0.42f * pulse),
            center = center,
            alpha = 0.06f + 0.10f * pulse
        )
        drawCircle(color = color, radius = maxRadius * (0.08f + 0.03f * pulse), center = center)

        // Anelli poligonali che morfano, sfasati tra loro e controrotanti
        repeat(ringCount) { i ->
            val ring = if (ringCount > 1) i.toFloat() / (ringCount - 1) else 0f
            val direction = if (i % 2 == 0) 1f else -1f

            // Lati che mutano 3 → 8 → 3, in controfase tra gli anelli
            val breathe = 0.5f - 0.5f * cos(2.0 * PI * (progress - ring)).toFloat()
            val sidesFloat = 3f + 5f * breathe

            reusablePath.rewind()
            buildMorphingPolygon(
                reusablePath, center,
                radius = maxRadius * (0.98f - 0.48f * ring),
                sidesFloat = sidesFloat
            )

            rotate(degrees = progress * 360f * direction, pivot = center) {
                drawPath(
                    reusablePath,
                    color = color,
                    alpha = (1f - 0.35f * ring) * (0.8f + 0.2f * breathe),
                    style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Default

@Composable
private fun DefaultLoader(color: Color, trackColor: Color, stroke: Dp) {
    CircularProgressIndicator(
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        strokeWidth = stroke
    )
}