package it.fast4x.riplay.extensions.smoothloader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin


/**
 * Costruisce un poligono il cui numero di lati varia CONTINUAMENTE con [sidesFloat]:
 * a valori interi è un poligono regolare esatto, nei valori intermedi è la
 * fusione fluida tra i due poligoni adiacenti — morphing vero, non crossfade.
 *
 * Trucco: il raggio del bordo di un poligono regolare a N lati in funzione
 * dell'angolo è  r(θ) = R · cos(π/N) / cos( mod(θ, 2π/N) - π/N ).
 * Si valuta la formula a floor(N) e ceil(N) e si interpolano i raggi campionati
 * sugli STESSI angoli → il path è chiuso per costruzione e le forme intermedie
 * sono deformazioni continue.
 */
fun buildMorphingPolygon(path: Path, center: Offset, radius: Float, sidesFloat: Float) {
    val nLow = floor(sidesFloat).toInt().coerceAtLeast(3)
    val t = (sidesFloat - nLow).coerceIn(0f, 1f)
    val samples = 72 //120 è il massimo non aumentare potrebbe causare rallentamenti in vecchi dispositivi

    // Conversioni fatte UNA volta, fuori dal loop
    val radiusD = radius.toDouble()
    val tD = t.toDouble()

    for (s in 0 until samples) {
        val theta = 2.0 * PI * s / samples - PI / 2.0 // -π/2 → punta in alto
        val rLow = polygonEdgeRadius(theta, nLow)
        val rHigh = polygonEdgeRadius(theta, nLow + 1)
        val r = (rLow + (rHigh - rLow) * tD) * radiusD

        // Unico punto di conversione Double → Float, in coda
        val x = center.x + (r * cos(theta)).toFloat()
        val y = center.y + (r * sin(theta)).toFloat()
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}

/** Raggio del bordo di un poligono regolare a [sides] lati all'angolo [theta], normalizzato al circoraggio. */
private fun polygonEdgeRadius(theta: Double, sides: Int): Double {
    val sector = 2.0 * PI / sides
    val local = mod2pi(theta) % sector - sector / 2.0
    return cos(PI / sides) / cos(local)
}

private fun mod2pi(theta: Double): Double {
    val twoPi = 2.0 * PI
    return ((theta % twoPi) + twoPi) % twoPi
}