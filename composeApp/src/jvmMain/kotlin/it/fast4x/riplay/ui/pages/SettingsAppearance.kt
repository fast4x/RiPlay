package it.fast4x.riplay.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.styling.DesktopAccent
import it.fast4x.riplay.ui.components.RiPlayAccent

/**
 * TAB "ASPETTO" delle impostazioni desktop (richiesta utente:
 * "colore accent personalizzato").
 *
 * La tab contiene la scelta del COLORE ACCENT, il colore identita' dell'app
 * (barra di navigazione, barre del player, slider premium, pulsanti, tab,
 * splash). Il colore e' lo stato [DesktopAccent.accent]: ogni modifica viene
 * applicata IN DIRETTA a tutta l'interfaccia (i composables che leggono
 * RiPlayAccent si ricompone da soli) e persistita in ~/.riplay/app.properties
 * alla conferma.
 *
 * Il selettore offre tre strade, come nei picker moderni:
 *  - PRESET: 12 tavolozze pronte (il primo e' il verde di fabbrica);
 *  - RIQUADRO saturaZIONE/LUMINOSITA' + BARRA di TONALITA' (HSV), con drag
 *    continuo: durante il drag vale [DesktopAccent.previewAccent] (solo
 *    anteprima, nessuna scrittura su disco a ogni frame), alla fine del
 *    gesto [DesktopAccent.persistCurrent] salva;
 *  - CAMPO HEX "#RRGGBB": antepreview a ogni carattere valido, salvataggio
 *    a perdita del focus / tasto Done della tastiera.
 *
 * Nota implementativa: i callback dei gesti passano per
 * [rememberUpdatedState] perche' i pointerInput(Unit) vivono a lungo e
 * catturerebbero le lambda della PRIMA composizione (stesso motivo dello
 * slider premium).
 */
@Composable
fun AppearanceTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentSection()
    }
}

@Composable
private fun AccentSection() {
    // Lettura reattiva dell'accent: al cambio (preset, drag, hex, reset)
    // l'intera sezione si ricompone gia' allineata (cursori, swatch, hex).
    val current = DesktopAccent.accent
    val hsv = remember(current) { colorToHsv(current) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Text(
            text = ds("settings.appearance.accent"),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = ds("settings.appearance.accent_info"),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(12.dp))

        // ── PRESET (2 file da 6) ─────────────────────────────────────────
        DesktopAccent.PRESETS.chunked(6).forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                rowColors.forEach { c ->
                    AccentSwatch(
                        color = c,
                        selected = DesktopAccent.toHex(c)
                            .equals(DesktopAccent.toHex(current), ignoreCase = true),
                        onClick = { DesktopAccent.setAccent(c) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = ds("settings.appearance.accent_custom"),
            color = RiPlayAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = ds("settings.appearance.accent_custom_info"),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(10.dp))

        // ── selettore HSV: riquadro S/L + barra tonalita' ────────────────
        SatValBox(
            hue = hsv[0],
            sat = hsv[1],
            value = hsv[2],
            onChange = { s, v -> DesktopAccent.previewAccent(hsvToColor(hsv[0], s, v)) },
            onCommit = { DesktopAccent.persistCurrent() }
        )
        Spacer(Modifier.height(10.dp))
        HueBar(
            hue = hsv[0],
            onChange = { h -> DesktopAccent.previewAccent(hsvToColor(h, hsv[1], hsv[2])) },
            onCommit = { DesktopAccent.persistCurrent() }
        )

        Spacer(Modifier.height(14.dp))

        // ── riga di riepilogo: campione, codice hex, reset ───────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(current)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = DesktopAccent.toHex(current),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = ds("settings.appearance.accent_reset"),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { DesktopAccent.resetToDefault() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        HexField(current = current)
    }
}

/** quadratino tondo di un colore preset; spunta sopra se selezionato. */
@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Riquadro SATURAZIONE (orizzontale) / LUMINOSITA' (verticale, alto=chiaro)
 * del colore con la tonalita' [hue]. Anteprima continua durante il gesto,
 * salvataggio alla fine ([onCommit]).
 */
@Composable
private fun SatValBox(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit,
    onCommit: () -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnCommit by rememberUpdatedState(onCommit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(pos: Offset) {
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val s = (pos.x / w).coerceIn(0f, 1f)
                        val v = 1f - (pos.y / h).coerceIn(0f, 1f)
                        currentOnChange(s, v)
                    }
                    update(down.position)
                    drag(down.id) { change ->
                        update(change.position)
                        change.consume()
                    }
                    currentOnCommit()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // base = tonalita' pura; poi velo bianco orizzontale (satura-
            // zione) e velo nero verticale (luminosita')
            drawRect(hsvToColor(hue, 1f, 1f))
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color(0x00FFFFFF))))
            drawRect(Brush.verticalGradient(listOf(Color(0x00000000), Color.Black)))
            // cursore: anello bianco (visibile su ogni fondo)
            val cx = sat * size.width
            val cy = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Barra di TONALITA' (hue 0..360) con gradiente arcobaleno e cursore.
 * Stessa semantica del riquadro: anteprima continua, salvataggio a fine gesto.
 */
@Composable
private fun HueBar(
    hue: Float,
    onChange: (hue: Float) -> Unit,
    onCommit: () -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnCommit by rememberUpdatedState(onCommit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(pos: Offset) {
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val hue = (pos.x / w).coerceIn(0f, 1f) * 360f
                        currentOnChange(hue)
                    }
                    update(down.position)
                    drag(down.id) { change ->
                        update(change.position)
                        change.consume()
                    }
                    currentOnCommit()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stops = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
                .map { hsvToColor(it, 1f, 1f) }
            drawRoundRect(
                brush = Brush.horizontalGradient(stops),
                cornerRadius = CornerRadius(size.height / 2f)
            )
            val cx = (hue / 360f).coerceIn(0f, 1f) * size.width
            drawCircle(
                color = Color.White,
                radius = size.height * 0.40f,
                center = Offset(cx, size.height / 2f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = hsvToColor(hue, 1f, 1f),
                radius = size.height * 0.26f,
                center = Offset(cx, size.height / 2f)
            )
        }
    }
}

/**
 * CAMPO HEX "#RRGGBB": mentre l'utente digita ogni codice valido viene
 * applicato in anteprima; alla perdita del focus (o col tasto Done della
 * tastiera) il colore corrente viene SALVATO. Fuori dal focus il campo
 * mostra sempre il colore attivo (presets/drag/reset lo tengono allineato).
 */
@Composable
private fun HexField(current: Color) {
    var text by remember { mutableStateOf(DesktopAccent.toHex(current)) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = if (focused) text else DesktopAccent.toHex(current),
        onValueChange = { v ->
            text = v
            DesktopAccent.parseHex(v)?.let { DesktopAccent.previewAccent(it) }
        },
        label = { Text("HEX", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RiPlayAccent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            cursorColor = RiPlayAccent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                val was = focused
                focused = it.isFocused
                if (it.isFocused) {
                    // all'ingresso nel campo: parti SEMPRE dal colore attivo
                    // (potrebbe essere cambiato via preset/drag/reset)
                    text = DesktopAccent.toHex(current)
                } else if (was && !it.isFocused) {
                    DesktopAccent.parseHex(text)?.let { c -> DesktopAccent.setAccent(c) }
                }
            }
    )
}

// ── conversioni HSV (jvmMain non ha le estensioni HSL di androidMain) ────

/** Color -> [hue, saturazione, luminosita'] (h 0..360, s/v 0..1). */
private fun colorToHsv(c: Color): FloatArray {
    val r = c.red
    val g = c.green
    val b = c.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d <= 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val s = if (max <= 0f) 0f else d / max
    return floatArrayOf(h, s, max)
}

/** [hue, saturazione, luminosita'] -> Color. */
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hh = (((h % 360f) + 360f) % 360f) / 60f
    val i = hh.toInt()
    val f = hh - i
    val p = v * (1f - s)
    val q = v * (1f - s * f)
    val t = v * (1f - s * (1f - f))
    return when (i % 6) {
        0 -> Color(v, t, p)
        1 -> Color(q, v, p)
        2 -> Color(p, v, t)
        3 -> Color(p, q, v)
        4 -> Color(t, p, v)
        else -> Color(v, p, q)
    }
}
