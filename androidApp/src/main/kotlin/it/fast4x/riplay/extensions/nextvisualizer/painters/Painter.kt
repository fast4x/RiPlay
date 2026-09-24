package it.fast4x.riplay.extensions.nextvisualizer.painters

import android.graphics.Canvas
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import kotlin.math.cos
import kotlin.math.sin

abstract class Painter {

    private val li = LinearInterpolator()
    private val sp = AkimaSplineInterpolator()

    abstract var paint:Paint

    /**
     * An abstract function that every painters must implement and do their calculation there.
     *
     * @param helper the visualizerHelper from VisualizerView
     */
    abstract fun calc(helper: VisualizerHelper)

    /**
     * An abstract function that every painters must implement and do their drawing there.
     *
     * @param canvas the canvas from VisualizerView
     * @param helper the visualizerHelper from VisualizerView
     */
    abstract fun draw(canvas: Canvas, helper: VisualizerHelper)

    /**
     * Interpolate FFT spectrum
     *
     * Android don't capture a high resolution spectrum, and we want the number of bands be adjustable,
     * so we do an interpolation here.
     *
     * (For example, to show 64 bands at frequencies from 0Hz to 1200Hz, Android only return ~10 FFT values.
     * So we need to interpolate those ~10 values into 64 values in order to fit it into our bands)
     *
     * @param gravityModels Array of gravityModel
     * @param sliceNum Number of Slice
     * @param interpolator Which interpolator to use, `li` for Linear, `sp` for Spline
     *
     * @return a `PolynomialSplineFunction` (psf). To get the value, use
     * `psf.value(x)`, where `x` must be a Double value from 0 to `num`
     */
    fun interpolateFft(
        gravityModels: Array<GravityModel>, sliceNum: Int, interpolator: String
    ): PolynomialSplineFunction {
        val nRaw = gravityModels.size
        val xRaw = DoubleArray(nRaw) { (it * sliceNum).toDouble() / (nRaw - 1) }
        val yRaw = DoubleArray(nRaw)
        gravityModels.forEachIndexed { index, bar -> yRaw[index] = bar.height.toDouble() }
        val psf: PolynomialSplineFunction
        psf = when (interpolator) {
            "li", "linear" -> li.interpolate(xRaw, yRaw)
            "sp", "spline" -> sp.interpolate(xRaw, yRaw)
            else -> li.interpolate(xRaw, yRaw)
        }
        return psf
    }

    /**
     * Interpolate FFT spectrum (Circle)
     *
     * Similar to `interpolateFft()`. However this is meant for Fft from `getCircleFft()`
     *
     * @param gravityModels Array of gravityModel
     * @param sliceNum Number of Slice
     * @param interpolator Which interpolator to use, `li` for Linear, `sp` for Spline
     *
     * @return a `PolynomialSplineFunction` (psf). To get the value, use
     * `psf.value(x)`, where `x` must be a Double value from 0 to `num`
     */
    fun interpolateFftCircle(
        gravityModels: Array<GravityModel>, sliceNum: Int, interpolator: String
    ): PolynomialSplineFunction {
        val nRaw = gravityModels.size
        val xRaw = DoubleArray(nRaw) { ((it - 1) * sliceNum).toDouble() / (nRaw - 1 - 2) }
        val yRaw = DoubleArray(nRaw)
        gravityModels.forEachIndexed { index, bar -> yRaw[index] = bar.height.toDouble() }
        val psf: PolynomialSplineFunction
        psf = when (interpolator) {
            "li", "linear" -> li.interpolate(xRaw, yRaw)
            "sp", "spline" -> sp.interpolate(xRaw, yRaw)
            else -> li.interpolate(xRaw, yRaw)
        }
        return psf
    }

    /**
     * Check if it's quiet enough such that we can skip the drawing
     * @param fft Fft
     * @return true if it's quiet, false otherwise
     */
    fun isQuiet(fft: DoubleArray): Boolean {
        val threshold = 5f
        fft.forEach { if (it > threshold) return false }
        return true
    }

    /**
     * Convert Polar to Cartesian
     * @param radius Radius
     * @param theta Theta
     * @return FloatArray of (x,y) of Cartesian
     */
    fun toCartesian(radius: Float, theta: Float): FloatArray {
        val x = radius * cos(theta)
        val y = radius * sin(theta)
        return floatArrayOf(x, y)
    }

    /**
     * Patch the Fft so that the start and the end connect perfectly. Use this with `interpolateFftCircle()`
     *
     * `[0, 1, ..., n] -> [n-1, 0, 1, ..., n-1, 0, 1]`
     *
     * @param fft Fft
     * @return CircleFft
     */
    fun getCircleFft(fft: DoubleArray): DoubleArray {
        val patched = DoubleArray(fft.size + 2)
        fft.forEachIndexed { index, d -> patched[index + 1] = d }
        patched[0] = fft[fft.lastIndex - 1]
        patched[patched.lastIndex - 1] = fft[0]
        patched[patched.lastIndex] = fft[1]
        return patched
    }

    /**
     * Patch the Fft to a MirrorFft
     *
     * @param fft Fft
     * @param mode when 0 -> do nothing
     *             when 1 ->
     *              `[0, 1, ..., n] -> [n, ..., 1, 0, 0, 1, ..., n]`
     *             when 2 ->
     *              `[0, 1, ..., n] -> [0, 1, ..., n, n, ..., 1, 0]`
     *             when 3 ->
     *             `[0, 1, ..., n] -> [n/2, ..., 1, 0, 0, 1, ..., n/2]`
     *             when 4 ->
     *             `[0, 1, ..., n] -> [0, 1, ..., n/2, n/2, ..., 1, 0]`
     * @return MirrorFft
     */
    fun getMirrorFft(fft: DoubleArray, mode: Int = 1): DoubleArray {
        return when (mode) {
            1 -> {
                fft.sliceArray(0..fft.lastIndex).reversedArray() + fft.sliceArray(0..fft.lastIndex)
            }
            2 -> {
                fft.sliceArray(0..fft.lastIndex) + fft.sliceArray(0..fft.lastIndex).reversedArray()
            }
            3 -> {
                fft.sliceArray(0..fft.lastIndex / 2).reversedArray() + fft.sliceArray(0..fft.lastIndex / 2)
            }
            4 -> {
                fft.sliceArray(0..fft.lastIndex / 2) + fft.sliceArray(0..fft.lastIndex / 2).reversedArray()
            }
            else -> fft
        }
    }

    /**
     * Boost high values while suppress low values, generally give a powerful feeling
     * @param fft Fft
     * @param param Parameter, adjust to fit your liking
     * @return PowerFft
     */
    fun getPowerFft(fft: DoubleArray, param: Double = 100.0): DoubleArray {
        return fft.map { it * it / param }.toDoubleArray()
    }

    /**
     * A helper to Rotate the canvas, use `Rotate` painter instead if you want to rotate the entire painter(s)
     * @param canvas Canvas
     * @param rot Rotation in degree
     * @param xR Rotation point X, 1f = `canvas.width`
     * @param yR Rotation point Y, 1f = `canvas.height`
     * @param d Drawing operation here
     */
    fun rotateHelper(canvas: Canvas, rot: Float, xR: Float, yR: Float, d: () -> Unit) {
        canvas.save()
        canvas.rotate(rot, canvas.width * xR, canvas.height * yR)
        d()
        canvas.restore()
    }

    /**
     * A helper to Translate the canvas and to provide Side functionality
     * @param canvas Canvas
     * @param side `a` `b` `ab` generally means Up(or Out), Down(or In), Both
     * @param xR Rotation point X, 1f = `canvas.width`
     * @param yR Rotation point Y, 1f = `canvas.height`
     * @param d Drawing operation for side a here
     */
    fun drawHelper(canvas: Canvas, side: String, xR: Float, yR: Float, d: () -> Unit) {
        canvas.save()
        when (side) {
            "a" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                d()
            }
            "b" -> {
                canvas.scale(1f, -1f, canvas.width / 2f, canvas.height / 2f)
                canvas.translate(canvas.width * xR, canvas.height * yR)
                d()
            }
            "ab" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                d()
                canvas.scale(1f, -1f)
                d()
            }
        }
        canvas.restore()
    }

    /**
     * A helper to Translate the canvas and to provide Side functionality
     * @param canvas Canvas
     * @param side `a` `b` `ab` generally means Up(or Out), Down(or In), Both
     * @param xR Rotation point X, 1f = `canvas.width`
     * @param yR Rotation point Y, 1f = `canvas.height`
     * @param d Drawing operation for side a here
     * @param dab Drawing operation for side ab here
     */
    fun drawHelper(canvas: Canvas, side: String, xR: Float, yR: Float, d: () -> Unit, dab: () -> Unit) {
        canvas.save()
        when (side) {
            "a" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                d()
            }
            "b" -> {
                canvas.scale(1f, -1f, canvas.width / 2f, canvas.height / 2f)
                canvas.translate(canvas.width * xR, canvas.height * yR)
                d()
            }
            "ab" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                dab()
            }
        }
        canvas.restore()
    }

    /**
     * A helper to Translate the canvas and to provide Side functionality
     * @param canvas Canvas
     * @param side `a` `b` `ab` generally means Up(or Out), Down(or In), Both
     * @param xR Rotation point X, 1f = `canvas.width`
     * @param yR Rotation point Y, 1f = `canvas.height`
     * @param da Drawing operation for side a here
     * @param db Drawing operation for side b here
     * @param dab Drawing operation for side ab here
     */
    fun drawHelper(
        canvas: Canvas, side: String, xR: Float, yR: Float, da: () -> Unit, db: () -> Unit, dab: () -> Unit
    ) {
        canvas.save()
        when (side) {
            "a" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                da()
            }
            "b" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                db()
            }
            "ab" -> {
                canvas.translate(canvas.width * xR, canvas.height * yR)
                dab()
            }
        }
        canvas.restore()
    }

    /**
     * A model with gravity. Useful to smooth raw Fft values.
     */
    class GravityModel(
        var height: Float = 0f,
        var dy: Float = 0f,
        var ay: Float = 2f
    ) {
        fun update(h: Float) {
            if (h > height) {
                height = h
                dy = 0f
            }
            height -= dy
            dy += ay
            if (height < 0) {
                height = 0f
                dy = 0f
            }
        }
    }
}