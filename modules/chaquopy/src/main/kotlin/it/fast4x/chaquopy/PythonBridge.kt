package it.fast4x.chaquopy

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import it.fast4x.riplay.PythonEngine
import it.fast4x.riplay.PythonResponse

/**
 * Avvio sicuro del runtime Python (Chaquopy).
 *
 * FIX crash-loop Android TV: in precedenza Python.start(AndroidPlatform(...))
 * veniva chiamato senza protezione e, su dispositivi la cui ABI non ha i
 * binari Chaquopy nell'APK (es. TV box ARM a 32 bit con un build limitato a
 * arm64/x86_64), il risultato era un UnsatisfiedLinkError NON gestito che
 * uccideva il processo → l'app ripartiva in loop ("lampeggio" di MainActivity).
 * Ora ogni avvio è protetto e restituisce un messaggio d'errore leggibile
 * invece di far morire il processo.
 */
@Keep
object ChaquopySupport {

    private const val TAG = "ChaquopySupport"

    /**
     * ABI per cui i binari Chaquopy sono inclusi nel build (Python 3.13 = 64-bit only).
     * DEVE restare allineata con `ndk.abiFilters` in modules/chaquopy/build.gradle.kts.
     * Un dispositivo con ABI fuori da questo elenco (es. TV box ARM a 32 bit) NON
     * proverà nemmeno ad avviare Python: niente crash, solo degradazione controllata.
     */
    val SUPPORTED_ABIS = listOf("arm64-v8a", "x86_64")

    /** true se almeno una ABI del dispositivo è coperta dai binari Chaquopy. */
    fun isAbiSupported(): Boolean =
        Build.SUPPORTED_ABIS.any { it in SUPPORTED_ABIS }

    /**
     * Avvia il runtime Python una sola volta, in modo sicuro.
     *
     * @return `null` se Python è avviato (o lo era già), altrimenti un
     *         messaggio d'errore pronto da mostrare all'utente / scrivere nel log.
     */
    fun safeStart(context: Context): String? {
        if (Python.isStarted()) return null

        if (!isAbiSupported()) {
            val abis = Build.SUPPORTED_ABIS.joinToString(", ")
            val msg = "Motore Python non supportato su questo dispositivo (ABI: $abis)"
            Log.e(TAG, msg)
            return msg
        }

        return try {
            Python.start(AndroidPlatform(context))
            Log.i(TAG, "Runtime Python avviato correttamente")
            null
        } catch (t: Throwable) {
            // UnsatisfiedLinkError, errori di estrazione libs, ecc.:
            // MAI rilanciare, il chiamante decide come degradare la funzionalità.
            val msg = "Motore Python non disponibile: ${t.message ?: t.javaClass.simpleName}"
            Log.e(TAG, "Chaquopy start failed", t)
            msg
        }
    }
}

@Keep
class PythonEngineImpl : PythonEngine {

    private val TAG = "PythonEngineImpl"

    override fun executeScript(url: String, privateDir: String): PythonResponse {
        // Ogni fallimento viene convertito in un'eccezione ORDINARIA
        // (IllegalStateException estende Exception): così MusicVaultWorker,
        // che intercetta Exception, marca il download come FAILED e l'utente
        // vede l'icona di errore invece di un crash del processo.
        try {
            val py = Python.getInstance()
            val result = py.getModule("MusicVault")
                .callAttr("download_audio", url, privateDir)

            // Nel caso di errore Python
            val errorObj = result.callAttr("get", "error")
            if (errorObj != null) {
                throw Exception("Python Script Error: ${errorObj.toString()}")
            }

            val path              = result.callAttr("get", "path")?.toString() ?: ""
            val fileName          = result.callAttr("get", "filename")?.toString() ?: ""
            val thumbnailFileName = result.callAttr("get", "thumbnail_filename")?.toString() ?: ""
            val title             = result.callAttr("get", "title")?.toString() ?: ""

            val durationObj = result.callAttr("get", "duration")
            val duration = durationObj?.toInt() ?: 0

            val artist            = result.callAttr("get", "artist")?.toString() ?: ""

            return PythonResponse(
                path = path,
                fileName = fileName,
                thumbnailFileName = thumbnailFileName,
                title = title,
                duration = duration,
                artist = artist
            )
        } catch (t: Throwable) {
            throw IllegalStateException("MusicVault engine failure: ${t.message ?: t.javaClass.simpleName}", t)
        }
    }

    override fun testAndStartChaquopy(context: Context): Triple<String, String, Boolean> {
        val startError = ChaquopySupport.safeStart(context)
        if (startError != null) {
            Log.e(TAG, "testAndStartChaquopy: $startError")
            return Triple("", startError, false)
        }

        return try {
            val py = Python.getInstance()

            // Test 1: Python funziona?
            val sys = py.getModule("sys")
            val pyVersion = sys["version"].toString()

            // Test 2: yt-dlp è installato? (se manca non deve comunque far crashare l'app)
            val ytdlpVersion = try {
                py.getModule("yt_dlp")["version"]?.get("__version__").toString()
            } catch (t: Throwable) {
                Log.e(TAG, "yt-dlp non disponibile", t)
                ""
            }

            val ytdlpIsReady = (pyVersion.isNotEmpty() && ytdlpVersion.isNotEmpty())

            Triple(pyVersion, ytdlpVersion, ytdlpIsReady)
        } catch (t: Throwable) {
            val msg = "Runtime Python non utilizzabile: ${t.message ?: t.javaClass.simpleName}"
            Log.e(TAG, msg, t)
            Triple("", msg, false)
        }
    }
}
