package vlcj

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import player.PlayerController
import player.frame.FrameRenderer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import java.nio.ByteBuffer


class VlcjFrameController constructor(
    private val controller: VlcjController = VlcjController(),
) : FrameRenderer, PlayerController by controller {

    private fun getPixels(buffer: ByteBuffer, width: Int, height: Int) = runCatching {
        buffer.rewind()
        val pixels = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = buffer.int
                val index = (y * width + x) * 4
                val b = (pixel and 0xff).toByte()
                val g = (pixel shr 8 and 0xff).toByte()
                val r = (pixel shr 16 and 0xff).toByte()
                val a = (pixel shr 24 and 0xff).toByte()
                pixels[index] = b
                pixels[index + 1] = g
                pixels[index + 2] = r
                pixels[index + 3] = a
            }
        }
        pixels
    }.getOrNull()

    private val bufferFormatCallback by lazy {
        object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int) = BufferFormat(
                "RV32",
                sourceWidth,
                sourceHeight,
                intArrayOf(sourceWidth * 4),
                intArrayOf(sourceHeight)
            )

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) = Unit
        }
    }

    private val renderCallback by lazy {
        RenderCallback { _, nativeBuffers, bufferFormat ->
            nativeBuffers?.firstOrNull()?.let { buffer ->
                getPixels(buffer, bufferFormat.width, bufferFormat.height)?.let {
                    _size.value = bufferFormat.width to bufferFormat.height
                    _bytes.value = it
                }
            }
        }
    }

    private val surface by lazy {
        controller.factory.videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, false)
    }

    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    override fun load(url: String) {
        controller.load(url)
        controller.player?.videoSurface()?.set(surface)
    }
}