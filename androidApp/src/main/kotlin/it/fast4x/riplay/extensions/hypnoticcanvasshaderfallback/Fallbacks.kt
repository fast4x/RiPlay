package it.fast4x.riplay.extensions.hypnoticcanvasshaderfallback

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import java.util.Objects.hash
import kotlin.math.*

// --- Metaballs: sfere morbide additive su percorsi Lissajous -----------------
fun DrawScope.paintMetaballs(
    time: Float,
    color1: Color = Color(0xFF3D5AFE),
    color2: Color = Color(0xFF00E5FF),
    background: Color = Color(0xFF05060E),
    blobs: Int = 5,
) {
    drawRect(background)
    val w = size.width; val h = size.height; val minDim = min(w, h)
    repeat(blobs) { i ->
        val cx = w * (0.5f + 0.36f * cos(time * (0.35f + i * 0.13f) + i * 1.9f))
        val cy = h * (0.5f + 0.36f * sin(time * (0.29f + i * 0.11f) + i * 3.1f))
        val r = minDim * (0.15f + 0.05f * (i % 3)) * (1f + 0.15f * sin(time * 0.8f + i))
        val c = if (i % 2 == 0) color1 else color2
        drawCircle(
            brush = Brush.radialGradient(listOf(c.copy(alpha = 0.85f), c.copy(alpha = 0f)), Offset(cx, cy), r),
            radius = r, center = Offset(cx, cy),
            blendMode = BlendMode.Plus // additivo: le sfere si "fondono" come i metaballs
        )
    }
}

// --- Fluid: correnti di colore che fluiscono ---------------------------------
fun DrawScope.paintFluid(
    time: Float,
    palette: List<Color> = listOf(Color(0xFF0E7CFF), Color(0xFF00E5C3), Color(0xFF7B2FFF), Color(0xFFFF3D81)),
    background: Color = Color(0xFF03040A),
    streams: Int = 6,
) {
    drawRect(background)
    val minDim = min(size.width, size.height)
    repeat(streams) { k ->
        val c = palette[k % palette.size]
        val ax = time * (0.16f + 0.05f * (k % 4)) + k * 1.7f
        val ay = time * (0.13f + 0.04f * ((k + 2) % 4)) + k * 2.9f
        val cx = size.width * (0.5f + 0.42f * cos(ax) * cos(0.7f * ay))
        val cy = size.height * (0.5f + 0.42f * sin(ay) * cos(0.5f * ax))
        val r = minDim * (0.38f + 0.14f * sin(time * 0.21f + k))
        drawCircle(
            brush = Brush.radialGradient(listOf(c.copy(alpha = 0.32f), Color.Transparent), Offset(cx, cy), r),
            radius = r, center = Offset(cx, cy),
            blendMode = BlendMode.Plus
        )
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))) // vignettatura
}

// --- Hypnotic: anelli b/n in contrazione + velo a vortice rotante ------------
fun DrawScope.paintHypnotic(
    time: Float,
    colorA: Color = Color(0xFF06060C),
    colorB: Color = Color(0xFFF4F4F8),
    rings: Int = 12,
    contractionsPerSecond: Float = 0.9f,
    rotationDegPerSecond: Float = 24f,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = hypot(size.width, size.height) * 0.62f
    val step = maxR / rings
    val phase = (time * contractionsPerSecond) % 1f

    // Cerchi pieni, dal più grande al più piccolo.
    // Colore = parità dell'indice j SOLO: nessun toInt(), nessun flip a phase = 0.
    // Al wrap (phase 1→0) il cerchio j riprende esattamente raggio e colore
    // che il cerchio j+1 aveva a phase → 1: loop seamless.
    for (j in rings + 1 downTo 1) {
        drawCircle(
            color = if (j % 2 == 0) colorA else colorB,
            radius = (j - phase) * step,
            center = center,
        )
    }

    // Velo rotante (rinforzato: 0.07 → 0.12, era percettibile a malapena)
    val wedges = 8
    val overlay = buildList {
        repeat(wedges) { i ->
            val c = if (i % 2 == 0) colorB.copy(alpha = 0.12f) else Color.Transparent
            add(i / wedges.toFloat() to c)
            add((i + 1f) / wedges - 0.001f to c)
        }
    }
    rotate(degrees = time * rotationDegPerSecond) {
        drawRect(Brush.sweepGradient(*overlay.toTypedArray()))
    }
}

// --- Hypnotic: versione a spirale
fun DrawScope.paintHypnoticSpiral(
    time: Float,
    colorA: Color = Color(0xFF06060C),
    colorB: Color = Color(0xFFF4F4F8),
    sectors: Int = 16,
    rotationDegPerSecond: Float = 40f,
    breathe: Float = 0.06f, // respiro radiale del tunnel
) {
    val scale = 1f + breathe * sin(time * 2f)
    val stops = buildList {
        repeat(sectors) { i ->
            val c = if (i % 2 == 0) colorA else colorB
            add(i / sectors.toFloat() to c)
            add((i + 1f) / sectors.toFloat() - 0.001f to c) // bordi netti
        }
    }
    rotate(degrees = time * rotationDegPerSecond) {
        scale(scale) { drawRect(Brush.sweepGradient(*stops.toTypedArray())) }
    }
}

// --- Mesmerizing: nastri sinusoidali stratificati ----------------------------
fun DrawScope.paintMesmerizing(
    time: Float,
    palette: List<Color> = listOf(
        Color(0xFF00F0FF), Color(0xFF7B2FFF), Color(0xFFFF2E92), Color(0xFFFFB300), Color(0xFF00FFA3)
    ),
    background: Color = Color(0xFF03040C),
    layers: Int = 8,
) {
    drawRect(background)
    val w = size.width; val h = size.height
    val step = 12.dp.toPx()
    for (layer in layers - 1 downTo 0) {
        val f = layer / (layers - 1).coerceAtLeast(1).toFloat()
        val color = palette[layer % palette.size]
        val baseY = h * (0.28f + 0.56f * f) + sin(time * 0.3f + layer) * h * 0.012f
        val amp = h * (0.05f + 0.04f * ((layer % 3) + 1) / 3f)
        val freq = (2f * PI.toFloat() * (1.4f + layer * 0.35f)) / w
        val speed = 0.8f + layer * 0.22f
        val path = Path()
        var x = 0f; var first = true
        while (x <= w + step) {
            val y = baseY + sin(x * freq + time * speed + layer * 1.3f) * amp +
                    sin(x * freq * 0.53f - time * speed * 0.6f) * amp * 0.7f
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += step
        }
        path.lineTo(w, h); path.lineTo(0f, h); path.close()
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.55f * (1f - f) + 0.15f), Color.Transparent),
                startY = baseY - amp * 1.8f, endY = h
            )
        )
    }
}

// --- CircleWave: ripple concentrici che si espandono --------------------------
fun DrawScope.paintCircleWave(
    time: Float,
    color: Color = Color(0xFF80DEEA),
    background: Color = Color(0xFF05060F),
    rings: Int = 4,
    period: Float = 2.4f,
) {
    drawRect(background)
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = hypot(size.width, size.height) * 0.65f
    repeat(rings) { i ->
        val p = (time / period + i.toFloat() / rings) % 1f
        val alpha = 1f - p
        drawCircle(
            color = color.copy(alpha = 0.85f * alpha * alpha),
            radius = p * maxR, center = center,
            style = Stroke(width = 2.dp.toPx() + 5.dp.toPx() * alpha)
        )
    }
    drawCircle(color, radius = 5.dp.toPx(), center = center)
}

// --- Default per shader custom/non mappati -----------------------------------
fun DrawScope.paintGenericFlow(
    time: Float,
    palette: List<Color> = listOf(Color(0xFF7B2FFF), Color(0xFF00E5C3), Color(0xFFFF3D81)),
    background: Color = Color(0xFF050510),
) {
    drawRect(background)
    val minDim = min(size.width, size.height)
    palette.forEachIndexed { i, c ->
        val cx = size.width * (0.5f + 0.38f * cos(time * (0.25f + 0.08f * i) + i * 2.4f))
        val cy = size.height * (0.5f + 0.38f * sin(time * (0.19f + 0.07f * i) + i * 4.1f))
        val r = minDim * (0.42f + 0.12f * sin(time * 0.3f + i))
        drawCircle(
            brush = Brush.radialGradient(listOf(c.copy(alpha = 0.40f), Color.Transparent), Offset(cx, cy), r),
            radius = r, center = Offset(cx, cy), blendMode = BlendMode.Plus
        )
    }
}

// ─── MesmerizingLens ─────────────────────────────────────────────────────────
/** Anelli ipnotici: il centro che deriva + lo zoom oscillante simulano la lente. */
fun DrawScope.paintMesmerizingLens(
    time: Float,
    colorA: Color = Color(0xFF07070E),
    colorB: Color = Color(0xFFEDEDF7),
    rings: Int = 14,
) {
    val w = size.width; val h = size.height
    val center = Offset(
        w * (0.5f + 0.06f * cos(time * 0.7f)),
        h * (0.5f + 0.06f * sin(time * 0.53f)),
    )
    val radius = hypot(w, h) * 0.6f * (1f + 0.08f * sin(time * 1.1f))

    // Anelli hard-edge: dal più esterno al più interno
    for (j in rings - 1 downTo 0) {
        drawCircle(
            color = if (j % 2 == 0) colorA else colorB,
            radius = radius * (j + 1) / rings,
            center = center,
        )
    }

    // riflesso della "lente", coerente col centro (List<Color>: OK così com'era)
    val lp = center - Offset(radius * 0.25f, radius * 0.25f)
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), lp, radius * 0.45f),
        radius * 0.45f, lp, blendMode = BlendMode.Plus,
    )
}

// ─── GlossyGradients ─────────────────────────────────────────────────────────
fun DrawScope.paintGlossyGradients(
    time: Float,
    palette: List<Color> = listOf(Color(0xFF8EC5FF), Color(0xFFE0AAFF), Color(0xFFB8F2E6), Color(0xFFFFD6E0)),
    background: Color = Color(0xFF101018),
) {
    drawRect(background)
    val minDim = min(size.width, size.height)
    palette.forEachIndexed { i, c ->
        val c0 = Offset(
            size.width * (0.5f + 0.4f * cos(time * 0.22f + i * 2.1f)),
            size.height * (0.5f + 0.4f * sin(time * 0.17f + i * 3.3f)),
        )
        val r = minDim * (0.45f + 0.1f * sin(time * 0.4f + i))
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f), Color.Transparent), c0, r), r, c0)
    }
    // sheen "glossy": banda diagonale chiara che scorre
    val sweep = (time * 0.12f) % 1.4f - 0.2f
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f), 1f to Color.Transparent,
            start = Offset(size.width * (sweep - 0.4f), 0f), end = Offset(size.width * (sweep + 0.4f), size.height),
        )
    )
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
}

// ─── GradientFlow ── riuso del painter Fluid, rallentato ─────────────────────
fun DrawScope.paintGradientFlow(time: Float) = paintFluid(
    time * 0.55f,
    palette = listOf(Color(0xFF4361EE), Color(0xFF3A0CA3), Color(0xFF4CC9F0), Color(0xFF7209B7), Color(0xFFF72585)),
    background = Color(0xFF02030A),
    streams = 7,
)

// ─── PurpleLiquid ── riuso del painter Metaballs, palette viola ──────────────
fun DrawScope.paintPurpleLiquid(time: Float) = paintMetaballs(
    time * 0.8f,
    color1 = Color(0xFF7B2FFF),
    color2 = Color(0xFFB388FF),
    background = Color(0xFF0A0118),
    blobs = 6,
)

// ─── InkFlow ─────────────────────────────────────────────────────────────────
fun DrawScope.paintInkFlow(
    time: Float,
    paper: Color = Color(0xFFF5F1E8),
    ink: Color = Color(0xFF15110D),
    tendrils: Int = 6,
) {
    val w = size.width; val h = size.height
    drawRect(paper)
    // macchia madre che deriva lentamente
    val blotC = Offset(w * (0.5f + 0.18f * sin(time * 0.13f)), h * (0.45f + 0.14f * cos(time * 0.09f)))
    val blotR = min(w, h) * 0.34f
    drawCircle(Brush.radialGradient(listOf(ink.copy(alpha = 0.30f), Color.Transparent), blotC, blotR), blotR, blotC)
    // tendine d'inchiostro
    repeat(tendrils) { t ->
        val phase = time * (0.25f + 0.15f * hash(t * 7.3f)) + hash(t * 3.1f) * 20f
        val yBase = h * (0.2f + 0.6f * hash(t * 11.7f))
        val path = Path()
        var x = -w * 0.1f; var first = true
        while (x <= w * 1.1f) {
            val p = x / w
            val y = yBase + h * 0.10f * sin(p * 6f + phase) + h * 0.05f * sin(p * 13f - phase * 1.7f + t)
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += w / 26f
        }
        val pulse = (0.7f + 0.3f * sin(phase * 0.9f)).coerceIn(0.4f, 1f)
        drawPath(path, ink.copy(alpha = 0.10f), style = Stroke(w * 0.030f, cap = StrokeCap.Round))
        drawPath(path, ink.copy(alpha = 0.70f), style = Stroke(w * 0.011f * pulse, cap = StrokeCap.Round))
    }
}

// ─── OilFlow ─────────────────────────────────────────────────────────────────
fun DrawScope.paintOilFlow(time: Float, background: Color = Color(0xFF04050A)) {
    drawRect(background)
    val w = size.width; val h = size.height
    val center = Offset(w / 2f, h / 2f)
    // vortici iridescenti: sweep gradient con hue che cicla nel tempo
    repeat(3) { layer ->
        val colors = List(7) { i ->
            Color.hsl((time * (14f + layer * 9f) + i * 51f + layer * 120f) % 360f, 0.85f, 0.55f, alpha = 0.16f)
        }
        rotate(degrees = time * (9f + layer * 5f) + layer * 120f, pivot = center) {
            drawCircle(Brush.sweepGradient(colors, center), min(w, h) * (0.35f + layer * 0.22f), center, blendMode = BlendMode.Plus)
        }
    }
    // film sottile arcobaleno, lentissimo
    val t = (time * 0.05f) % 2f - 1f
    drawRect(
        Brush.linearGradient(
            List(8) { i -> Color.hsl((i * 45f + time * 12f) % 360f, 0.9f, 0.6f, alpha = 0.05f) },
            start = Offset(w * t, 0f), end = Offset(w * (t + 1f), h),
        ),
        blendMode = BlendMode.Plus,
    )
}

// ─── IceReflection ───────────────────────────────────────────────────────────
fun DrawScope.paintIceReflection(
    time: Float,
    deep: Color = Color(0xFF0B2A4A),
    ice: Color = Color(0xFFBFE8FF),
) {
    val w = size.width; val h = size.height
    val horizon = h * 0.62f
    drawRect(Brush.verticalGradient(listOf(deep.copy(alpha = 0.9f), ice), endY = horizon))
    drawRect(
        Brush.verticalGradient(listOf(ice.copy(alpha = 0.55f), deep), startY = horizon),
        topLeft = Offset(0f, horizon), size = Size(w, h - horizon),
    )
    drawRect(ice.copy(alpha = 0.6f), Offset(0f, horizon - 1.dp.toPx()), Size(w, 2.dp.toPx()))
    // crepe di luce diagonali
    repeat(5) { i ->
        val x = w * fract(hash(i * 5.7f) + time * 0.015f * (i + 1))
        drawLine(
            Color.White.copy(alpha = 0.10f),
            Offset(x, 0f), Offset(x + w * 0.35f, horizon),
            strokeWidth = (1f + 2f * hash(i * 9.1f)) * 1.5f,
        )
    }
    // scintille
    repeat(28) { i ->
        val tw = 0.5f + 0.5f * sin(time * (2f + hash(i) * 3f) + i)
        drawCircle(
            Color.White.copy(alpha = 0.5f * tw),
            1.dp.toPx() * (0.5f + hash(i * 4.4f) * 1.5f),
            Offset(w * hash(i * 1.37f), h * hash(i * 2.71f)),
        )
    }
}

// ─── Stage ───────────────────────────────────────────────────────────────────
fun DrawScope.paintStage(
    time: Float,
    background: Color = Color(0xFF050507),
    spot: Color = Color(0xFFFFF3D6),
) {
    val w = size.width; val h = size.height
    drawRect(background)
    val floorY = h * 0.78f
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF121216), Color(0xFF050507)), startY = floorY),
        topLeft = Offset(0f, floorY), size = Size(w, h - floorY),
    )
    // due fari rotanti
    repeat(2) { i ->
        val pivot = Offset(w * (0.3f + 0.4f * i), h * 0.02f)
        rotate(degrees = sin(time * (0.5f + 0.2f * i) + i * 2.2f) * 55f, pivot = pivot) {
            val cone = Path().apply {
                moveTo(pivot.x, pivot.y)
                lineTo(pivot.x - w * 0.22f, h)
                lineTo(pivot.x + w * 0.22f, h)
                close()
            }
            drawPath(
                cone,
                Brush.verticalGradient(listOf(spot.copy(alpha = 0.35f), spot.copy(alpha = 0.03f)), startY = pivot.y, endY = h),
                blendMode = BlendMode.Plus,
            )
        }
    }
    drawCircle(
        Brush.radialGradient(listOf(spot.copy(alpha = 0.18f), Color.Transparent), Offset(w / 2f, floorY), w * 0.35f),
        w * 0.35f, Offset(w / 2f, floorY), blendMode = BlendMode.Plus,
    )
    // polvere sospesa
    repeat(18) { i ->
        drawCircle(
            spot.copy(alpha = 0.12f), 1.5f,
            Offset(
                w * hash(i * 3.9f) + w * 0.02f * sin(time * (0.4f + hash(i)) + i),
                h * fract(hash(i * 7.7f) + time * 0.02f),
            ),
        )
    }
}

// ─── GoldenMagma ─────────────────────────────────────────────────────────────
fun DrawScope.paintGoldenMagma(
    time: Float,
    rock: Color = Color(0xFF0C0906),
    lava: Color = Color(0xFFFFB300),
) {
    val w = size.width; val h = size.height
    drawRect(rock)
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xFF3A1C00).copy(alpha = 0.4f + 0.15f * sin(time * 1.4f))),
            startY = h * 0.35f, endY = h,
        )
    )
    // fessure incandescenti: glow largo + nucleo stretto
    repeat(6) { c ->
        val path = Path()
        var x = w * hash(c * 13.3f)
        var y = h * (0.15f + 0.8f * hash(c * 5.1f))
        path.moveTo(x, y)
        repeat(8) { s ->
            x += w * (0.08f + 0.1f * hash(c * 31.7f + s))
            y += h * (hash(c * 17.9f + s) - 0.5f) * 0.16f
            path.lineTo(x, y)
        }
        val pulse = 0.55f + 0.45f * sin(time * 2.1f + c * 1.9f)
        drawPath(path, lava.copy(alpha = 0.18f * pulse), style = Stroke(w * 0.020f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
        drawPath(path, Color(0xFFFFE082).copy(alpha = 0.9f * pulse), style = Stroke(w * 0.005f, cap = StrokeCap.Round))
    }
    // bracci ascendenti
    repeat(22) { i ->
        val life = fract(hash(i * 3.3f) + time * (0.05f + 0.05f * hash(i * 9.7f)))
        drawCircle(
            Color(0xFFFF9E40).copy(alpha = (1f - life) * 0.8f),
            1.5f * (0.5f + hash(i * 5.5f)),
            Offset(w * (hash(i * 1.9f) + 0.03f * sin(time + i)), h * (1f - life)),
            blendMode = BlendMode.Plus,
        )
    }
}

// ─── BlackCherryCosmos ───────────────────────────────────────────────────────
fun DrawScope.paintBlackCherryCosmos(
    time: Float,
    space: Color = Color(0xFF030208),
    cherry: Color = Color(0xFF8B1042),
) {
    val w = size.width; val h = size.height
    drawRect(space)
    listOf(
        cherry to Offset(w * (0.5f + 0.18f * cos(time * 0.05f)), h * 0.4f),
        Color(0xFF4A0E63) to Offset(w * (0.4f + 0.15f * sin(time * 0.04f)), h * 0.6f),
        Color(0xFFB3123F) to Offset(w * (0.65f - 0.1f * cos(time * 0.06f)), h * 0.5f),
    ).forEach { (c, nebula) ->
        val r = min(w, h) * 0.5f
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.30f), Color.Transparent), nebula, r), r, nebula, blendMode = BlendMode.Plus)
    }
    repeat(90) { i ->
        val speed = 0.004f + hash(i * 8.1f) * 0.004f
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * (1.5f + hash(i) * 4f) + i * 2.1f))
        drawCircle(
            Color(0xFFFFE9F0).copy(alpha = tw),
            (if (hash(i * 4.9f) > 0.9f) 1.8f else 0.9f),
            Offset(w * fract(hash(i * 1.13f) + time * speed), h * fract(hash(i * 2.47f) + time * speed * 0.6f)),
        )
    }
}

// ─── SpectralAura ────────────────────────────────────────────────────────────
fun DrawScope.paintSpectralAura(time: Float, background: Color = Color(0xFF04040A)) {
    drawRect(background)
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = hypot(size.width, size.height) * 0.55f
    val hueShift = time * 45f
    val auraR = maxR * (1f + 0.05f * sin(time * 1.3f))

    val auraColors = List(7) { i ->
        Color.hsl((hueShift + i * 51f) % 360f, 0.85f, 0.6f, alpha = (1f - i / 6f) * 0.5f + 0.08f)
    }
    drawCircle(
        Brush.radialGradient(auraColors, center, auraR),
        auraR, center, blendMode = BlendMode.Plus,
    )
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent), center, maxR * 0.25f),
        maxR * 0.25f, center, blendMode = BlendMode.Plus,
    )
    repeat(3) { i ->
        val p = fract(time * 0.22f + i / 3f)
        drawCircle(
            Color.hsl((hueShift + i * 120f) % 360f, 0.9f, 0.65f, alpha = (1f - p) * 0.5f),
            p * maxR, center, style = Stroke(2.dp.toPx() + 4.dp.toPx() * (1f - p)),
        )
    }
}

// ─── PrismGlass ──────────────────────────────────────────────────────────────
fun DrawScope.paintPrismGlass(time: Float, background: Color = Color(0xFF08080F)) {
    drawRect(background)
    val w = size.width; val h = size.height
    val slab = Rect(Offset(w * 0.18f, h * 0.2f), Size(w * 0.64f, h * 0.6f))
    val wobble = 2f * sin(time * 1.6f) // aberrazione cromatica: 3 lastre RGB sovrapposte
    listOf(
        Color(0xFFFF3B30) to Offset(-wobble, 0f),
        Color(0xFF00E5A0) to Offset(0f, wobble),
        Color(0xFF3B82F6) to Offset(wobble, 0f),
    ).forEach { (c, off) ->
        drawRoundRect(c.copy(alpha = 0.5f), slab.topLeft + off, slab.size, CornerRadius(10f), blendMode = BlendMode.Plus)
    }
    val sweep = fract(time * 0.1f) * 1.6f - 0.3f
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.16f), 1f to Color.Transparent,
            start = Offset(w * sweep, 0f), end = Offset(w * (sweep + 0.35f), h),
        ),
        blendMode = BlendMode.Plus,
    )
    // spettro proiettato sotto la lastra
    drawRect(
        Brush.horizontalGradient(List(7) { i -> Color.hsl((i * 45f + time * 20f) % 360f, 0.9f, 0.55f) }),
        topLeft = Offset(slab.left, slab.bottom + 6.dp.toPx()),
        size = Size(slab.width, 8.dp.toPx()),
        alpha = 0.7f, blendMode = BlendMode.Plus,
    )
}

// ─── Heat ────────────────────────────────────────────────────────────────────
fun DrawScope.paintHeat(time: Float, background: Color = Color(0xFF160602)) {
    drawRect(background)
    val w = size.width; val h = size.height
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFF6D00).copy(alpha = 0.55f), Color.Transparent), Offset(w / 2f, h), w * 0.7f),
        w * 0.7f, Offset(w / 2f, h), blendMode = BlendMode.Plus,
    )
    // shimmer: scie ondulate che salgono
    repeat(9) { i ->
        val life = fract(hash(i * 12.9f) + time * (0.10f + 0.06f * hash(i * 3.7f)))
        val y = h * (1f - life)
        val amp = w * (0.02f + 0.02f * hash(i * 5.3f))
        val path = Path()
        var x = 0f
        while (x <= w) {
            val yy = y + amp * sin(x / w * 12f + time * (3f + i) + i * 2.4f)
            if (x == 0f) path.moveTo(x, yy) else path.lineTo(x, yy)
            x += 14.dp.toPx()
        }
        drawPath(path, Color(0xFFFFAB40).copy(alpha = 0.14f * (1f - life)), style = Stroke(w * 0.012f), blendMode = BlendMode.Plus)
    }
}

// ─── Stripy ──────────────────────────────────────────────────────────────────
fun DrawScope.paintStripy(
    time: Float,
    colorA: Color = Color(0xFF0E0E12),
    colorB: Color = Color(0xFFE9E9F2),
    stripes: Int = 12,
) {
    val diag = hypot(size.width, size.height)
    rotate(degrees = 18f + 8f * sin(time * 0.3f)) {
        val period = diag * 2f / stripes
        val scroll = (time * period / 2.4f) % (period * 2f) // loop seamless: shift = 2 bande
        drawRect(colorB, Offset(-diag, -diag), Size(diag * 3f, diag * 3f))
        var x = -diag - period * 2f + scroll
        var i = 0
        while (x < diag) {
            drawRect(if (i % 2 == 0) colorA else colorB, Offset(x, -diag), Size(period, diag * 3f))
            x += period; i++
        }
    }
}

// ─── BubbleRings ─────────────────────────────────────────────────────────────
fun DrawScope.paintBubbleRings(time: Float, background: Color = Color(0xFF04101C)) {
    drawRect(background)
    val w = size.width; val h = size.height
    repeat(22) { i ->
        val life = fract(hash(i * 7.7f) + time * (0.05f + 0.07f * hash(i * 2.3f)))
        val x = w * (0.1f + 0.8f * hash(i * 5.1f)) + w * 0.03f * sin(time * (0.5f + hash(i)) + i)
        val y = h * (1.05f - life * 1.1f)
        val r = (4f + 16f * hash(i * 9.3f)) * (0.6f + 0.6f * life)
        val alpha = (1f - life) * 0.7f
        drawCircle(
            Color.hsl((hash(i * 11.1f) * 360f + time * 30f) % 360f, 0.8f, 0.65f, alpha = alpha),
            r, Offset(x, y), style = Stroke(2f),
        )
        drawCircle(Color.White.copy(alpha = alpha * 0.9f), r * 0.18f, Offset(x - r * 0.35f, y - r * 0.35f))
    }
}