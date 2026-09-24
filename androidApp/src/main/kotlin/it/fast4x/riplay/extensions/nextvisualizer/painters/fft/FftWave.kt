package it.fast4x.riplay.extensions.nextvisualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

class FftWave(
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    },
    //
    var startHz: Int = 0,
    var endHz: Int = 2000,
    //
    var num: Int = 128,
    var interpolator: String = "sp",
    //
    var side: String = "a",
    var mirror: Boolean = false,
    var power: Boolean = false,
    //
    var ampR: Float = 1f
) : Painter() {

    private val path = Path()
    private var points = Array(0) { GravityModel() }
    private var skipFrame = false
    lateinit var fft: DoubleArray
    lateinit var psf: PolynomialSplineFunction

    override fun calc(helper: VisualizerHelper) {
        fft = helper.getFftMagnitudeRange(startHz, endHz)

        if (isQuiet(fft)) {
            skipFrame = true
            return
        } else skipFrame = false

        if (power) fft = getPowerFft(fft)
        if (mirror) fft = getMirrorFft(fft)

        if (points.size != fft.size) points =
            Array(fft.size) { GravityModel(0f) }
        points.forEachIndexed { index, bar -> bar.update(fft[index].toFloat() * ampR) }

        psf = interpolateFft(points, num, interpolator)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (skipFrame) return

        val width = canvas.width.toFloat()

        val sliceWidth = width / num
        if (paint.style == Paint.Style.STROKE) {
            path.moveTo(0f, -psf.value(0.0).toFloat())
            for (i in 1..num) {
                path.lineTo(sliceWidth * i, -psf.value(i.toDouble()).toFloat())
            }
        } else {
            path.moveTo(0f, 1f)
            for (i in 0..num) {
                path.lineTo(sliceWidth * i, -psf.value(i.toDouble()).toFloat())
            }
            path.lineTo(width, 1f)
            path.close()
        }
        drawHelper(canvas, side, 0f, .5f) { canvas.drawPath(path, paint) }
        path.reset()
    }
}