package it.fast4x.riplay.extensions.nextvisualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

class FftPoly(
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE;style = Paint.Style.STROKE;strokeWidth = 2f
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
    var gapX: Float = 0f,
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
        val barWidth = (width - (num + 1) * gapX) / num

        drawHelper(canvas, side, 0f, .5f, {
            for (i in 0 until num) {
                path.moveTo(barWidth * i + gapX * (i + 1), -psf.value(i.toDouble()).toFloat())
                path.lineTo(barWidth * (i + 1) + gapX * (i + 1), -psf.value((i + 1).toDouble()).toFloat())
                path.lineTo(barWidth * (i + 1) + gapX * (i + 1), 0f)
                path.lineTo(barWidth * i + gapX * (i + 1), 0f)
                path.close()
            }
            canvas.drawPath(path, paint)
        }, {
            for (i in 0 until num) {
                path.moveTo(barWidth * i + gapX * (i + 1), -psf.value(i.toDouble()).toFloat())
                path.lineTo(barWidth * (i + 1) + gapX * (i + 1), -psf.value((i + 1).toDouble()).toFloat())
                path.lineTo(barWidth * (i + 1) + gapX * (i + 1), psf.value((i + 1).toDouble()).toFloat())
                path.lineTo(barWidth * i + gapX * (i + 1), psf.value(i.toDouble()).toFloat())
                path.close()
            }
            canvas.drawPath(path, paint)
        })
        path.reset()
    }
}