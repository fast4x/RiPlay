package it.fast4x.riplay.extensions.nextvisualizer.painters.modifier

import android.graphics.Canvas
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper

class Scale(
    vararg val painters: Painter,
    //
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    //
    var pxR: Float = .5f,
    var pyR: Float = .5f
) : Painter() {

    override var paint = Paint()

    override fun calc(helper: VisualizerHelper) {
        painters.forEach { painter ->
            painter.calc(helper)
        }
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.save()
        canvas.scale(scaleX, scaleY, pxR * canvas.width, pyR * canvas.height)
        painters.forEach { painter ->
            painter.paint.apply { colorFilter = paint.colorFilter;xfermode = paint.xfermode }
            painter.draw(canvas, helper)
        }
        canvas.restore()
    }
}