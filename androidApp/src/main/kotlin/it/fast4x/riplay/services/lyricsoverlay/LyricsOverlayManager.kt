package it.fast4x.riplay.services.lyricsoverlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.services.playback.PlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

/** Stile di evidenza della riga corrente nell'overlay. */
enum class LyricsOverlayMode { LINE, WORD }

/**
 * OVERLAY TESTI FLUTTUANTE.
 *
 * Piccola finestra TYPE_APPLICATION_OVERLAY sempre sopra le altre app con la
 * riga corrente del testo (e la successiva) sincronizzata sulla riproduzione:
 * la variante mobile del karaoke desktop.
 *
 *  - vive dentro il PlayerService (gia' foreground mediaPlayback): nessun
 *    servizio aggiuntivo e nessun tipo foreground service nuovo;
 *  - toggle in Impostazioni -> Generali, permesso "Mostra sopra altre app"
 *    richiesto al primo utilizzo dalla schermata di sistema;
 *  - trascinabile da qualunque zona, posizione ricordata e sempre
 *    raggiungibile (mai completamente fuori schermo);
 *  - riga corrente con evidenza a scelta: riga intera in accento oppure
 *    parola per parola (selettore Riga/Parola nell'overlay);
 *  - dimensione del testo regolabile con i pulsanti A-/A+;
 *  - visibile anche con il PiP attivo: in quel caso il modulo Testi del PiP
 *    cede il posto alla copertina (niente testo doppio sullo schermo);
 *  - nascosto quando non c'e' un brano in riproduzione.
 */
@UnstableApi
object LyricsOverlayManager {

    /** minimo di finestra (px) che deve restare su schermo: overlay mai irraggiungibile */
    private const val MIN_VISIBLE_PX = 64

    private const val PREFS_NAME = "lyrics_overlay"
    private const val KEY_POS_X = "posX"
    private const val KEY_POS_Y = "posY"
    private const val KEY_TEXT_SCALE = "textScale"
    private const val KEY_MODE = "mode"

    private const val MIN_SCALE = 0.8f
    private const val MAX_SCALE = 1.6f

    private val mainHandler = Handler(Looper.getMainLooper())

    private var service: PlayerService? = null
    private var collectJob: Job? = null
    private var appSettings: AppSettings? = null

    // Stato osservabile da Compose: la finestra e' attualmente montata
    var active by mutableStateOf(false)
        private set

    // Brano corrente: stato Compose, il contenuto si aggiorna al cambio brano
    private var currentSong by mutableStateOf<Song?>(null)

    /** Scala della dimensione del testo (1 = dimensione standard). */
    var textScale by mutableStateOf(1f)
        private set

    /** Stile di evidenza della riga corrente. */
    var mode by mutableStateOf(LyricsOverlayMode.WORD)
        private set

    fun changeTextScale(delta: Float) = setTextScaleOverlayManager(textScale + delta)

    fun setTextScaleOverlayManager(value: Float) {
        val v = value.coerceIn(MIN_SCALE, MAX_SCALE)
        if (textScale == v) return
        textScale = v
        savePrefs { it.putFloat(KEY_TEXT_SCALE, v) }
    }

    fun setModeOverlayManager(value: LyricsOverlayMode) {
        if (mode == value) return
        mode = value
        savePrefs { it.putString(KEY_MODE, value.name) }
    }

    fun initialize(target: PlayerService) {
        if (service != null) return
        service = target
        loadPrefs(target)

        collectJob = target.serviceScope.launch(Dispatchers.Main) {
            combine(
                target.appSettingsManager.activeSettings,
                target.currentSong
            ) { settings, song -> settings to song }
                .collect { (settings, song) ->
                    appSettings = settings
                    currentSong = song
                    reevaluate()
                }
        }
    }

    fun release() {
        collectJob?.cancel()
        collectJob = null
        mainHandler.post {
            runCatching { hide() }
                .onFailure { Timber.e("LyricsOverlayManager release hide error: ${it.stackTraceToString()}") }
            service = null
            appSettings = null
            currentSong = null
        }
    }

    private fun reevaluate() {
        val s = service ?: return
        val shouldShow = appSettings?.enableFloatingLyricsOverlay == true &&
                Settings.canDrawOverlays(s) &&
                currentSong != null
        if (shouldShow == active) return
        runCatching { if (shouldShow) show() else hide() }
            .onFailure { Timber.e("LyricsOverlayManager reevaluate error: ${it.stackTraceToString()}") }
    }

    // ------------------------------------------------------------------
    // finestra
    // ------------------------------------------------------------------

    private var view: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null

    private fun show() {
        val s = service ?: return
        if (active) return

        val wm = s.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val owner = OverlayLifecycleOwner().also { it.onCreate() }

        val v = ComposeView(s).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(owner.lifecycle)
            )
            setContent {
                val song = currentSong
                val player = s.hybridPlayer
                if (song != null) {
                    LyricsOverlayContent(
                        song = song,
                        player = player
                    )
                }
            }
        }

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // posizione ricordata; senza posizione la finestra si posiziona dopo
        // il mount (quando le dimensioni sono note) in basso al centro
        val saved = readPosition()
        if (saved != null) {
            p.x = saved.first
            p.y = saved.second
        }

        try {
            wm.addView(v, p)
        } catch (t: Throwable) {
            Timber.e("LyricsOverlayManager addView error: ${t.stackTraceToString()}")
            owner.onDestroy()
            return
        }

        view = v
        lifecycleOwner = owner
        params = p
        active = true
        owner.onResume()

        // posizione predefinita o recupero d'emergenza se la finestra e'
        // fuori schermo (cambio display, rotazione tra le sessioni)
        v.post {
            if (view !== v) return@post
            val (screenW, screenH) = realDisplaySize()
            if (screenW <= 0 || screenH <= 0) return@post
            val outX = p.x + v.width < MIN_VISIBLE_PX || p.x > screenW - MIN_VISIBLE_PX
            val outY = p.y + v.height < MIN_VISIBLE_PX || p.y > screenH - MIN_VISIBLE_PX
            if (saved == null || outX || outY) {
                val density = v.resources.displayMetrics.density
                p.x = ((screenW - v.width) / 2).coerceAtLeast(0)
                p.y = (screenH - v.height - (90 * density).toInt())
                    .coerceAtLeast(0)
                clamp(p)
                runCatching {
                    val wm2 = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm2.updateViewLayout(v, p)
                }
            }
        }
    }

    private fun hide() {
        val v = view ?: return
        view = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        params = null
        active = false
        runCatching {
            val wm = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(v)
        }.onFailure {
            Timber.e("LyricsOverlayManager removeView error: ${it.stackTraceToString()}")
        }
    }

    /** Spostamento della finestra durante il drag (px, dal gesto Compose). */
    fun moveBy(dx: Float, dy: Float) {
        val p = params ?: return
        val v = view ?: return
        p.x += dx.toInt()
        p.y += dy.toInt()
        clamp(p)
        runCatching {
            val wm = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.updateViewLayout(v, p)
        }
    }

    /** Salvataggio posizione a gesto fermo. */
    fun savePosition() {
        val p = params ?: return
        savePrefs {
            it.putInt(KEY_POS_X, p.x)
            it.putInt(KEY_POS_Y, p.y)
        }
    }

    /** Mantiene sempre una parte di finestra visibile sullo schermo. */
    private fun clamp(p: WindowManager.LayoutParams) {
        val v = view ?: return
        if (v.width <= 0 || v.height <= 0) return
        val (screenW, screenH) = realDisplaySize()
        if (screenW <= 0 || screenH <= 0) return
        p.x = p.x.coerceIn(
            -(v.width - MIN_VISIBLE_PX),
            (screenW - MIN_VISIBLE_PX).coerceAtLeast(0)
        )
        p.y = p.y.coerceIn(
            -(v.height - MIN_VISIBLE_PX),
            (screenH - MIN_VISIBLE_PX).coerceAtLeast(0)
        )
    }

    // ------------------------------------------------------------------
    // metriche reali del display
    // ------------------------------------------------------------------

    /**
     * Dimensione reale del display: le metriche del contesto servizio
     * seguono l'area dell'app (es. finestra PiP) e confinerebbero l'overlay
     * dentro quell'area.
     */
    private fun realDisplaySize(): Pair<Int, Int> {
        val s = service ?: return 0 to 0
        val wm = s.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay ?: return 0 to 0
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    /** Larghezza reale dello schermo in dp, con margine di sicurezza. */
    fun realScreenWidthDp(): Float {
        val (w, _) = realDisplaySize()
        if (w <= 0) return 480f
        val density = service?.resources?.displayMetrics?.density ?: return 480f
        return (w / density) - 16f
    }

    // ------------------------------------------------------------------
    // preferenze
    // ------------------------------------------------------------------

    private fun prefs(context: Context?) =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readPosition(): Pair<Int, Int>? {
        val s = service ?: return null
        val p = prefs(s) ?: return null
        if (!p.contains(KEY_POS_X) || !p.contains(KEY_POS_Y)) return null
        val x = p.getInt(KEY_POS_X, 0)
        val y = p.getInt(KEY_POS_Y, 0)
        if (x < -2000 || y < -2000 || x > 20000 || y > 20000) return null
        return x to y
    }

    private fun loadPrefs(context: Context) {
        val p = prefs(context) ?: return
        textScale = p.getFloat(KEY_TEXT_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE)
        mode = when (
            runCatching { p.getString(KEY_MODE, LyricsOverlayMode.WORD.name) }
                .getOrNull()?.trim()?.uppercase()
        ) {
            LyricsOverlayMode.LINE.name -> LyricsOverlayMode.LINE
            else -> LyricsOverlayMode.WORD
        }
    }

    private fun savePrefs(edit: (android.content.SharedPreferences.Editor) -> Unit) {
        val s = service ?: return
        runCatching {
            prefs(s)?.let { edit(it.edit()) }
        }.onFailure { Timber.e("LyricsOverlayManager savePrefs error: ${it.message}") }
    }
}

/**
 * Lifecycle owner minimale per il ComposeView montato in una finestra
 * overlay: Compose esige un LifecycleOwner e un SavedStateRegistryOwner
 * nell'albero delle view.
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    fun onPause() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

    fun onDestroy() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
}
