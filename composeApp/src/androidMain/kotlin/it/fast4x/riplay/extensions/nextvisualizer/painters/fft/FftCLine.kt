package it.fast4x.riplay.extensions.nextvisualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import kotlin.math.PI
import kotlin.math.min

class FftCLine(
    val colorPaint: Int = Color.WHITE,
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPaint;style = Paint.Style.STROKE;strokeWidth = 2f
    },
    //
    var startHz: Int = 0,
    var endHz: Int = 2000,
    //
    var num: Int = 64,
    var interpolator: String = "li",
    //
    var side: String = "a",
    var mirror: Boolean = false,
    var power: Boolean = true,
    //
    var radiusR: Float = .4f,
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
        fft = if (mirror) getMirrorFft(fft)
        else getCircleFft(fft)

        if (points.size != fft.size) points = Array(fft.size) { GravityModel(0f) }
        points.forEachIndexed { index, bar -> bar.update(fft[index].toFloat() * ampR) }

        psf = interpolateFftCircle(points, num, interpolator)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (skipFrame) return

        val angle = 2 * PI.toFloat() / num
        val pts = FloatArray(4 * num)
        val shortest = min(canvas.width, canvas.height)

        drawHelper(canvas, side, .5f, .5f, {
            for (i in 0 until num) {
                val start =
                    toCartesian(shortest / 2f * radiusR, angle * i)
                val stop = toCartesian(
                    shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), angle * i
                )
                pts[4 * i] = start[0];pts[4 * i + 1] = start[1]
                pts[4 * i + 2] = stop[0];pts[4 * i + 3] = stop[1]
            }
            canvas.drawLines(pts, paint)
        }, {
            for (i in 0 until num) {
                val start =
                    toCartesian(shortest / 2f * radiusR, angle * i)
                val stop = toCartesian(
                    shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), angle * i
                )
                pts[4 * i] = start[0];pts[4 * i + 1] = start[1]
                pts[4 * i + 2] = stop[0];pts[4 * i + 3] = stop[1]
            }
            canvas.drawLines(pts, paint)
        }, {
            for (i in 0 until num) {
                val start =
                    toCartesian(
                        shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), angle * i
                    )
                val stop = toCartesian(
                    shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), angle * i
                )
                pts[4 * i] = start[0];pts[4 * i + 1] = start[1]
                pts[4 * i + 2] = stop[0];pts[4 * i + 3] = stop[1]
            }
            canvas.drawLines(pts, paint)
        })
    }
}