package it.fast4x.riplay.extensions.nextvisualizer.painters.modifier

import android.graphics.Canvas
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper

class Beat(
    vararg val painters: Painter,
    //
    var startHz: Int = 60,
    var endHz: Int = 800,
    //
    var pxR: Float = .5f,
    var pyR: Float = .5f,
    //
    var radiusR: Float = 1f,
    var beatAmpR: Float = 1f,
    var peak: Float = 200f
) : Painter() {

    override var paint = Paint()

    private val energy = GravityModel(0f)

    override fun calc(helper: VisualizerHelper) {
        energy.update(helper.getFftMagnitudeRange(startHz, endHz).average().toFloat())
        painters.forEach { painter ->
            painter.calc(helper)
        }
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.save()
        val width = canvas.width * (radiusR + energy.height / peak * beatAmpR)
        canvas.scale(
            width / canvas.width, width / canvas.width,
            canvas.width * pxR, canvas.height * pyR
        )
        painters.forEach { painter ->
            painter.paint.apply { colorFilter = paint.colorFilter;xfermode = paint.xfermode }
            painter.draw(canvas, helper)
        }
        canvas.restore()
    }
}