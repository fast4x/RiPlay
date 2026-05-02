package it.fast4x.riplay.extensions.nextvisualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import kotlin.math.PI
import kotlin.math.min

class FftCWave(
    var colorPaint: Int = Color.TRANSPARENT,
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPaint
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
    var power: Boolean = true,
    //
    var radiusR: Float = .4f,
    var ampR: Float = .6f
) : Painter() {

    private val path = Path()
    private var points = Array(0) { GravityModel() }
    private var skipFrame = false
    lateinit var fft: DoubleArray
    lateinit var psf: PolynomialSplineFunction

    override fun calc(helper: VisualizerHelper) {
        fft = helper.getFftMagnitudeRange(startHz, endHz)
        if (isQuiet(fft) && paint.style == Paint.Style.STROKE) {
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
        val shortest = min(canvas.width, canvas.height)

        drawHelper(canvas, side, .5f, .5f, {
            for (i in 0..num) {
                val point = toCartesian(shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), angle * i)
                if (i == 0) path.moveTo(point[0], point[1])
                else path.lineTo(point[0], point[1])
            }
            path.close()
            canvas.drawPath(path, paint)
            path.reset()
        }, {
            for (i in 0..num) {
                val point = toCartesian(shortest / 2f * radiusR, angle * i)
                if (i == 0) path.moveTo(point[0], point[1])
                else path.lineTo(point[0], point[1])
            }
            path.close()
            for (i in 0..num) {
                val point = toCartesian(shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), angle * i)
                if (i == 0) path.moveTo(point[0], point[1])
                else path.lineTo(point[0], point[1])
            }
            path.close()
            path.fillType = Path.FillType.EVEN_ODD
            canvas.drawPath(path, paint)
            path.reset()
        }, {
            for (i in 0..num) {
                val point = toCartesian(shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), angle * i)
                if (i == 0) path.moveTo(point[0], point[1])
                else path.lineTo(point[0], point[1])
            }
            path.close()
            for (i in 0..num) {
                val point = toCartesian(shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), angle * i)
                if (i == 0) path.moveTo(point[0], point[1])
                else path.lineTo(point[0], point[1])
            }
            path.close()
            path.fillType = Path.FillType.EVEN_ODD
            canvas.drawPath(path, paint)
            path.reset()
        })
    }


}