package it.fast4x.riplay.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
                Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888).applyCanvas {
                    drawColor(colorProvider(isSystemInDarkMode))
                }
        }.onFailure {
            Timber.Forest.e("Failed set default bitmap in BitmapProvider ${it.stackTraceToString()}")
        }

        return lastBitmap == null
    }

    fun load(uri: Uri?, onDone: (Bitmap) -> Unit) {
        Timber.Forest.d("BitmapProvider load method being called")
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