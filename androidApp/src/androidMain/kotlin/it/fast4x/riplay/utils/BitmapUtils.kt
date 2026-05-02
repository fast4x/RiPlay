package it.fast4x.riplay.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.graphics.applyCanvas
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import it.fast4x.riplay.commonutils.thumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import timber.log.Timber
import java.util.concurrent.ExecutionException
import kotlin.toString
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import coil.size.Scale
import coil.transform.Transformation

suspend fun getBitmapFromUrl(context: Context, url: String): Bitmap {
    val loading = context.imageLoader
    val request = ImageRequest.Builder(context).data(url)
        // Required to get works getPixels()
        .allowHardware(false)
        .build()
    val result = loading.execute(request)
    if(result is ErrorResult) {
        throw result.throwable
    }
    val drawable = (result as SuccessResult).drawable
    return (drawable as BitmapDrawable).bitmap
}


@UnstableApi
class BitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val bitmapSize: Int,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context)
                    //.networkCachePolicy(CachePolicy.ENABLED)
                    .data(uri.toString().thumbnail(bitmapSize))
                    .size(bitmapSize)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .allowHardware(false)
                    .diskCacheKey(uri.toString().thumbnail(bitmapSize).toString())
                    .build()
            )
            if (result is ErrorResult) {
                throw ExecutionException(result.throwable)
            }
            try {
                (result.drawable as BitmapDrawable).bitmap
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }
}

class BitmapProvider(
    private val bitmapSize: Int,
    private val colorProvider: (isSystemInDarkMode: Boolean) -> Int
) {
    var lastUri: Uri? = null
        private set

    var lastBitmap: Bitmap? = null
    private var lastIsSystemInDarkMode = false

    private var lastEnqueued: Disposable? = null

    private lateinit var defaultBitmap: Bitmap

    val bitmap: Bitmap
        get() = lastBitmap ?: defaultBitmap

    var listener: ((Bitmap?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(bitmap)
        }

    init {
        setDefaultBitmap()
    }

    fun setDefaultBitmap(): Boolean {
        val isSystemInDarkMode = appContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (::defaultBitmap.isInitialized && isSystemInDarkMode == lastIsSystemInDarkMode) return false

        lastIsSystemInDarkMode = isSystemInDarkMode

        runCatching {
            defaultBitmap =
                createBitmap(bitmapSize, bitmapSize).applyCanvas {
                    drawColor(colorProvider(isSystemInDarkMode))
                }
        }.onFailure {
            Timber.e("Failed set default bitmap in BitmapProvider ${it.stackTraceToString()}")
        }

        return lastBitmap == null
    }

    fun load(uri: Uri?, onDone: (Bitmap) -> Unit) {
        Timber.d("BitmapProvider load method being called")

        if (uri == null) {
            onDone(bitmap)
            return
        }

        if (lastUri == uri) {
            onDone(bitmap)
            return
        }

        lastEnqueued?.dispose()
        lastUri = uri

        lastBitmap = null

        val url = uri.toString().thumbnail(bitmapSize)
        runCatching {
            lastEnqueued = appContext().imageLoader.enqueue(
                ImageRequest.Builder(appContext())
                    .data(url)
                    .size(bitmapSize, bitmapSize)
                    .transformations(LandscapeToSquareTransformation(bitmapSize))
                    .allowHardware(false)
                    .diskCacheKey(url.toString())
                    .memoryCacheKey(url.toString())
                    .listener(
                        onError = { _, result ->
                            Timber.e("Failed to load bitmap ${result.throwable.stackTraceToString()}")
                            lastBitmap = null
                            onDone(bitmap)
                            //listener?.invoke(bitmap)
                        },
                        onSuccess = { _, result ->
                            val drawable = result.drawable
                            if (drawable is BitmapDrawable) {
                                lastBitmap = drawable.bitmap
                            } else {
                                lastBitmap = null
                            }
                            onDone(bitmap)
                            // listener?.invoke(bitmap)
                        }
                    )
                    .build()
            )
        }.onFailure {
            Timber.e("Failed enqueue in BitmapProvider ${it.stackTraceToString()}")
            onDone(bitmap)
        }
    }
}

class LandscapeToSquareTransformation(private val targetSize: Int) : Transformation {

    override val cacheKey: String = "landscape_square_crop_$targetSize"

    override suspend fun transform(input: Bitmap, size: coil.size.Size): Bitmap {

        if (input.width <= input.height) {
            return input
        }

        val output = createBitmap(targetSize, targetSize)

        val scale = targetSize.toFloat() / input.height

        val scaledWidth = input.width * scale

        val dx = (scaledWidth - targetSize) / 2f

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale)
            postTranslate(-dx, 0f)
        }

        canvas.drawBitmap(input, matrix, paint)

        return output
    }

    override fun equals(other: Any?): Boolean = other is LandscapeToSquareTransformation && other.targetSize == targetSize
    override fun hashCode(): Int = targetSize.hashCode()
}

/*
class BitmapProvider(
    private val bitmapSize: Int,
    private val colorProvider: (isSystemInDarkMode: Boolean) -> Int
) {
    var lastUri: Uri? = null
        private set

    var lastBitmap: Bitmap? = null
    private var lastIsSystemInDarkMode = false

    private var lastEnqueued: Disposable? = null

    private lateinit var defaultBitmap: Bitmap

    val bitmap: Bitmap
        get() = lastBitmap ?: defaultBitmap

    var listener: ((Bitmap?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(lastBitmap)
        }

    init {
        setDefaultBitmap()
    }

    fun setDefaultBitmap(): Boolean {
        val isSystemInDarkMode = appContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (::defaultBitmap.isInitialized && isSystemInDarkMode == lastIsSystemInDarkMode) return false

        lastIsSystemInDarkMode = isSystemInDarkMode

        runCatching {
            defaultBitmap =
                createBitmap(bitmapSize, bitmapSize).applyCanvas {
                    drawColor(colorProvider(isSystemInDarkMode))
                }
        }.onFailure {
            Timber.e("Failed set default bitmap in BitmapProvider ${it.stackTraceToString()}")
        }

        return lastBitmap == null
    }

    fun load(uri: Uri?, onDone: (Bitmap) -> Unit) {
        Timber.d("BitmapProvider load method being called")
        if (lastUri == uri || uri == null) {
            listener?.invoke(lastBitmap)
            return
        }

        lastEnqueued?.dispose()
        lastUri = uri

        val url = uri.toString().thumbnail(bitmapSize)
        runCatching {
            lastEnqueued = appContext().imageLoader.enqueue(
                ImageRequest.Builder(appContext())
                    //.networkCachePolicy(CachePolicy.ENABLED)
                    .data(url)
                    .allowHardware(false)
                    .diskCacheKey(url.toString())
                    .memoryCacheKey(url.toString())
                    .listener(
                        onError = { _, result ->
                            Timber.Forest.e("Failed to load bitmap ${result.throwable.stackTraceToString()}")
                            lastBitmap = null
                            onDone(bitmap)
                            //listener?.invoke(lastBitmap)
                        },
                        onSuccess = { _, result ->
                            lastBitmap = (result.drawable as BitmapDrawable).bitmap
                            onDone(bitmap)
                            //listener?.invoke(lastBitmap)
                        }
                    )

                    .build()
            )
        }.onFailure {
            Timber.Forest.e("Failed enqueue in BitmapProvider ${it.stackTraceToString()}")
        }
    }
}
 */