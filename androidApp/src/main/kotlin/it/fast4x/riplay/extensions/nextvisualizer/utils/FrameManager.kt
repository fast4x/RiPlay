package it.fast4x.riplay.extensions.nextvisualizer.utils

class FrameManager {

    private var last: Long = 0
    private var span: Long = 0
    private var count: Float = 0f
    private var fps: Float = 0f

    fun tick() {
        val current = System.currentTimeMillis()
        span += current - last
        if (span > 1000) {
            fps = count / span * 1000f
            span = 0
            count = 0f
        }
        count++
        last = current
    }

    fun fps() = fps
}