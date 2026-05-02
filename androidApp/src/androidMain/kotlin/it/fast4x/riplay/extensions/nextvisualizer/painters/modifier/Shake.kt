package it.fast4x.riplay.extensions.nextvisualizer.painters.modifier

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import it.fast4x.riplay.extensions.nextvisualizer.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.utils.VisualizerHelper

class Shake(
    vararg val painters: Painter,
    //
    var animX: ValueAnimator = ValueAnimator.ofFloat(0f, .01f, 0f, -.01f, 0f).apply {
        duration = 16000;repeatCount = ValueAnimator.INFINITE
    },
    var animY: ValueAnimator = ValueAnimator.ofFloat(0f, .01f, 0f, -.01f, 0f).apply {
        duration = 8000;repeatCount = ValueAnimator.INFINITE
    }
) : Painter() {

    override var paint = Paint()

    init {
        animX.start()
        animY.start()
    }

    override fun calc(helper: VisualizerHelper) {
        painters.forEach { painter ->
            painter.calc(helper)
        }

    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.save()
        drawHelper(canvas, "a", animX.animatedValue as Float, animY.animatedValue as Float) {
            painters.forEach { painter ->
                painter.paint.apply { colorFilter = paint.colorFilter;xfermode = paint.xfermode }
                painter.draw(canvas, helper)
            }
        }
        canvas.restore()
    }
}