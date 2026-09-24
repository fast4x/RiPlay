package it.fast4x.riplay.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import it.fast4x.riplay.commonutils.toThumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import timber.log.Timber
import kotlin.toString
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.transform.Transformation
import android.graphics.Color
import android.graphics.RectF

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
            val targetUrl = uri.toString().toThumbnail(bitmapSize)

            val result = context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(targetUrl)
                    .size(bitmapSize)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .allowHardware(false) // Mantiene la bitmap software per Android Auto
                    .diskCacheKey(targetUrl)
                    .build()
            )

            // 1. GESTIONE ERRORI CORRETTA: Lanciamo l'eccezione originale senza annidarla
            if (result is ErrorResult) {
                throw result.throwable
            }

            // 2. ESTRAZIONE SICURA DELLA BITMAP: Evita il ClassCastException con i Drawable complessi
            try {
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else {
                    // Se Coil restituisce un CrossfadeDrawable o altro, lo convertiamo in modo sicuro in Bitmap
                    drawable?.toBitmap(width = bitmapSize, height = bitmapSize, config = Bitmap.Config.ARGB_8888)
                        ?: error("Drawable convertito è nullo")
                }
            } catch (e: Exception) {
                // Lanciamo l'eccezione nativa, ci penserà la coroutine a impacchettarla per Media3
                throw e
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

        val url = uri.toString().toThumbnail(bitmapSize)
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

// Funzione di utilità opzionale lato App per convertire Vettori XML in Bitmap
fun vectorToBitmap(context: Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Adatta il logo centrandolo in una tela nera proporzionata sullo schermo corrente.
 * Ridimensiona il logo in base all'altezza o alla larghezza disponibile per impedire
 * che strabordi o si schiacci quando si ruota il dispositivo in Landscape.
 */
fun Bitmap.toFitWindowWithBlackBackground(context: Context): Bitmap {
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // 1. Creiamo la tela nera delle dimensioni esatte della finestra corrente
    val outputBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)
    canvas.drawColor(Color.BLACK)

    // 2. Calcoliamo la scala ideale per il logo affinché non riempia mai tutto lo schermo,
    // lasciando un margine di sicurezza (es. il logo occuperà al massimo il 40% del lato minore)
    val maxLogoSize = (Math.min(screenWidth, screenHeight) * 0.40f)
    val scale = Math.min(maxLogoSize / this.width, maxLogoSize / this.height)

    val scaledWidth = this.width * scale
    val scaledHeight = this.height * scale

    // 3. Centriamo le coordinate della bounding box del logo sulla tela
    val left = (screenWidth - scaledWidth) / 2f
    val top = (screenHeight - scaledHeight) / 2f
    val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

    // 4. Disegniamo il logo scalato in alta qualità
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(this, null, destRect, paint)

    return outputBitmap
}
