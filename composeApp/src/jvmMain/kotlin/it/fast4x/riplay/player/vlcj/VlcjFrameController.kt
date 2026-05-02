package it.fast4x.riplay.player.vlcj

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.`-DeprecatedOkio`.buffer
import player.PlayerController
import player.frame.FrameRenderer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import vlcj.VlcjController
import java.nio.ByteBuffer
import kotlin.getValue


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

            override fun newFormatSize(
                bufferWidth: Int,
                bufferHeight: Int,
                displayWidth: Int,
                displayHeight: Int
            ) {
                //TODO("Not yet implemented")
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) = Unit
        }
    }

    private val renderCallback by lazy {
        object : RenderCallback {

            override fun lock(mediaPlayer: MediaPlayer?) = Unit

            override fun display(
                mediaPlayer: MediaPlayer?,
                nativeBuffers: Array<out ByteBuffer?>?,
                bufferFormat: BufferFormat?,
                displayWidth: Int,
                displayHeight: Int
            ) {
                nativeBuffers?.firstOrNull()?.let { buffer ->
                    bufferFormat?.let { format ->
                        getPixels(buffer, format.width, format.height)?.let { pixels ->
                            _size.value = format.width to format.height
                            _bytes.value = pixels
                        }
                    }
                }
            }

            override fun unlock(mediaPlayer: MediaPlayer?) = Unit
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