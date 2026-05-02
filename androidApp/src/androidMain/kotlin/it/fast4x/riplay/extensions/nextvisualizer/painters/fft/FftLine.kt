package it.fast4x.riplay.extensions.nextvisualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

class FftLine(
    val colorPaint: Int = Color.WHITE,
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPaint;style = Paint.Style.STROKE;strokeWidth = 2f
    },
    //
    var startHz: Int = 0,
    var endHz: Int = 2000,
    //
    var num: Int = 128,
    var interpolator: String = "li",
    //
    var side: String = "a",
    var mirror: Boolean = false,
    var power: Boolean = false,
    //
    var ampR: Float = 1f
) : Painter() {

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
        val gapWidth = width / num

        val pts = FloatArray(4 * num)
        drawHelper(canvas, side, 0f, .5f, {
            for (i in 0 until num) {
                pts[4 * i] = gapWidth * (i + .5f)
                pts[4 * i + 1] = -psf.value(i.toDouble()).toFloat()
                pts[4 * i + 2] = gapWidth * (i + .5f)
                pts[4 * i + 3] = 0f
            }
            canvas.drawLines(pts, paint)
        }, {
            for (i in 0 until num) {
                pts[4 * i] = gapWidth * (i + .5f)
                pts[4 * i + 1] = -psf.value(i.toDouble()).toFloat()
                pts[4 * i + 2] = gapWidth * (i + .5f)
                pts[4 * i + 3] = psf.value(i.toDouble()).toFloat()
            }
            canvas.drawLines(pts, paint)
        })
    }
}