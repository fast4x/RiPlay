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

class FftCPoly(
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE;style = Paint.Style.STROKE;strokeWidth = 2f
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
        fft = if (mirror) getMirrorFft(fft)
        else getCircleFft(fft)

        if (points.size != fft.size) points = Array(fft.size) { GravityModel(0f) }
        points.forEachIndexed { index, bar -> bar.update(fft[index].toFloat() * ampR) }

        psf = interpolateFftCircle(points, num, interpolator)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (skipFrame) return

        val shortest = min(canvas.width, canvas.height)
        val gapTheta = gapX / (shortest / 2f * radiusR)
        val angle = 2 * PI.toFloat() / num - gapTheta

        drawHelper(canvas, side, .5f, .5f, {
            for (i in 0 until num) {
                val start1 =
                    toCartesian(shortest / 2f * radiusR, (angle + gapTheta) * i)
                val stop1 = toCartesian(
                    shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), (angle + gapTheta) * i
                )
                val start2 =
                    toCartesian(shortest / 2f * radiusR, (angle) * (i + 1) + gapTheta * i)
                val stop2 = toCartesian(
                    shortest / 2f * radiusR + psf.value((i + 1).toDouble()).toFloat(), (angle) * (i + 1) + gapTheta * i
                )
                path.moveTo(start1[0], start1[1])
                path.lineTo(stop1[0], stop1[1])
                path.lineTo(stop2[0], stop2[1])
                path.lineTo(start2[0], start2[1])
                path.close()
            }
            canvas.drawPath(path, paint)
        }, {
            for (i in 0 until num) {
                val start1 =
                    toCartesian(shortest / 2f * radiusR, (angle + gapTheta) * i)
                val stop1 = toCartesian(
                    shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), (angle + gapTheta) * i
                )
                val start2 =
                    toCartesian(shortest / 2f * radiusR, (angle) * (i + 1) + gapTheta * i)
                val stop2 = toCartesian(
                    shortest / 2f * radiusR - psf.value((i + 1).toDouble()).toFloat(), (angle) * (i + 1) + gapTheta * i
                )
                path.moveTo(start1[0], start1[1])
                path.lineTo(stop1[0], stop1[1])
                path.lineTo(stop2[0], stop2[1])
                path.lineTo(start2[0], start2[1])
                path.close()
            }
            canvas.drawPath(path, paint)
        }, {
            for (i in 0 until num) {
                val start1 =
                    toCartesian(
                        shortest / 2f * radiusR + psf.value(i.toDouble()).toFloat(), (angle + gapTheta) * i
                    )
                val stop1 = toCartesian(
                    shortest / 2f * radiusR - psf.value(i.toDouble()).toFloat(), (angle + gapTheta) * i
                )
                val start2 =
                    toCartesian(
                        shortest / 2f * radiusR + psf.value((i + 1).toDouble()).toFloat(), (angle) * (i + 1) + gapTheta * i
                    )
                val stop2 = toCartesian(
                    shortest / 2f * radiusR - psf.value((i + 1).toDouble()).toFloat(), (angle) * (i + 1) + gapTheta * i
                )
                path.moveTo(start1[0], start1[1])
                path.lineTo(stop1[0], stop1[1])
                path.lineTo(stop2[0], stop2[1])
                path.lineTo(start2[0], start2[1])
                path.close()
            }
            canvas.drawPath(path, paint)
        })
        path.reset()
    }
}