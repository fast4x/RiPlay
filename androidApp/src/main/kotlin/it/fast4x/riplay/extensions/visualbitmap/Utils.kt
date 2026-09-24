package it.fast4x.riplay.extensions.visualbitmap

import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import androidx.core.graphics.createBitmap

suspend fun generateBitmapFromViewSafely(view: View): Bitmap {
    if (view.width <= 0 || view.height <= 0) {
        Timber.d("VisualBitmapCreator: View width or height is <= 0")
        return createBitmap(1, 1)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return generateBitmapHardwareAccelerated(view)
    }

    return generateBitmapSoftware(view)
}

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun generateBitmapHardwareAccelerated(view: View): Bitmap =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->

            val renderNode = RenderNode("HardwareBitmapCreator")
            renderNode.setPosition(0, 0, view.width, view.height)

            val canvas = renderNode.beginRecording()
            view.draw(canvas)
            renderNode.endRecording()

            val surfaceTexture = SurfaceTexture(false)
            surfaceTexture.setDefaultBufferSize(view.width, view.height)
            val surface = Surface(surfaceTexture)

            val hardwareRenderer = HardwareRenderer()
            hardwareRenderer.setSurface(surface)
            hardwareRenderer.setContentRoot(renderNode)

            val bitmap = createBitmap(view.width, view.height)

            hardwareRenderer.createRenderRequest().syncAndDraw()

            PixelCopy.request(surface, bitmap, { result ->
                hardwareRenderer.destroy()
                surface.release()
                surfaceTexture.release()

                if (result == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap)
                } else {
                    Timber.e("VisualBitmapCreator: PixelCopy failed with code $result")
                    continuation.resume(createBitmap(1, 1))
                }
            }, Handler(Looper.getMainLooper()))

            continuation.invokeOnCancellation {
                hardwareRenderer.destroy()
                surface.release()
                surfaceTexture.release()
            }
        }
    }


private suspend fun generateBitmapSoftware(view: View): Bitmap = withContext(Dispatchers.Main) {
    try {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        Timber.d("VisualBitmapCreator: Exception: ${e.message}")
        createBitmap(1, 1)
    }
}