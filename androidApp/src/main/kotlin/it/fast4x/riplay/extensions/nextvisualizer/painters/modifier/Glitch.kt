package it.fast4x.riplay.extensions.nextvisualizer.painters.modifier

import android.graphics.*
import android.os.Build
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper
import kotlin.random.Random

class Glitch(
    vararg val painters: Painter,
    //
    var startHz: Int = 60,
    var endHz: Int = 300,
    //
    var peak: Float = 50f,
    var duration: Int = 200
) : Painter() {

    override var paint = Paint()

    private val energy = GravityModel(0f)
    private var count = 0

    override fun calc(helper: VisualizerHelper) {
        energy.update(helper.getFftMagnitudeRange(startHz, endHz).average().toFloat())
        if (energy.height > peak) count = (duration / 1000f * 60f).toInt()
        painters.forEach { painter ->
            painter.calc(helper)
        }
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (count > 0) {
            val width = canvas.width.toFloat()

            val y = Random.nextFloat() * canvas.height
            val h = Random.nextFloat() * 200f + 100f
            val displacement = Random.nextFloat() * .1f - .05f
            val noise = Random.nextFloat() * .1f - .05f

            canvas.save()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(0f, y, width, y + h)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipRect(0f, y, width, y + h, Region.Op.DIFFERENCE)
            }
            drawHelper(canvas, "a", 0f, 0f) {
                painters.forEach { painter ->
                    painter.paint.apply {
                        colorFilter = null;xfermode = null
                    }
                    painter.draw(canvas, helper)
                }
            }
            canvas.restore()

            canvas.save()
            canvas.clipRect(0f, y, width, y + h)
            drawHelper(canvas, "a", displacement - noise, 0f) {
                painters.forEach { painter ->
                    painter.paint.apply {
                        colorFilter = LightingColorFilter(Color.RED, Color.BLACK)
                        ;xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                    }
                    painter.draw(canvas, helper)
                }
            }
            drawHelper(canvas, "a", displacement, 0f) {
                painters.forEach { painter ->
                    painter.paint.apply {
                        colorFilter = LightingColorFilter(Color.GREEN, Color.BLACK)
                        ;xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                    }
                    painter.draw(canvas, helper)
                }
            }
            drawHelper(canvas, "a", displacement + noise, 0f) {
                painters.forEach { painter ->
                    painter.paint.apply {
                        colorFilter = LightingColorFilter(Color.BLUE, Color.BLACK)
                        ;xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                    }
                    painter.draw(canvas, helper)
                }
            }
            canvas.restore()
            count--
        } else {
            painters.forEach { painter ->
                painter.paint.apply {
                    colorFilter = null;xfermode = null
                }
                painter.draw(canvas, helper)
            }
        }


    }
}