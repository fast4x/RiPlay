package it.fast4x.riplay.styling

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File


object DesktopAccent {

    /** Accento di fabbrica (il verde storico di RiPlay). */
    val DEFAULT: Color = Color(0xFF30D158)

    /** Accento attivo: state Compose, la UI che lo legge si ricompone al cambio. */
    val accentState: MutableState<Color> = mutableStateOf(DEFAULT)

    /** Colore accent corrente (lettura reattiva se avviene in composizione). */
    val accent: Color
        get() = accentState.value

    /**
     * Variante chiara dell'accent (testa del gradiente degli slider premium).
     * DERIVATA: mix con bianco del 30%, come la relazione visiva tra i due
     * verdi originali (0xFF30D158 -> 0xFF6BE894).
     */
    val accentLight: Color
        get() = accent.lighten(0.30f)

    /** Tavolozza di partenza (2 file da 6): il primo e' il predefinito. */
    val PRESETS: List<Color> = listOf(
        Color(0xFF30D158), // verde RiPlay (predefinito)
        Color(0xFF4C8DFF), // blu
        Color(0xFF7C4DFF), // viola
        Color(0xFFE91E63), // rosa
        Color(0xFFFF5252), // rosso
        Color(0xFFFF9800), // arancio
        Color(0xFFFFC107), // ambra
        Color(0xFF00BCD4), // ciano
        Color(0xFF26A69A), // teal
        Color(0xFF8BC34A), // lime
        Color(0xFFFF7043), // salmone
        Color(0xFF9C27B0), // magenta
    )

    private const val PREF_KEY = "ui.accent"

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("app.properties")

    /** Guardia run-once (stessa regola di DesktopI18n.initialize). */
    private var initializeCalled = false

    /** Chiamare UNA volta all'avvio (main.kt) PRIMA della prima UI. */
    fun initialize() {
        if (initializeCalled) return
        initializeCalled = true
        val saved = readSavedAccent()
        if (saved != null) accentState.value = saved
        println("[RiPlay-Accent] accent iniziale: ${toHex(accentState.value)}")
    }

    /** Applica il colore e lo PERSISTE (preset, reset, conferma hex, fine drag). */
    fun setAccent(c: Color) {
        accentState.value = c
        persist(c)
    }

    /** Applica il colore in ANTEPRIMA (drag del selettore): nessun salvataggio. */
    fun previewAccent(c: Color) {
        accentState.value = c
    }

    /** Salva il colore corrente (usato alla fine di un trascinamento). */
    fun persistCurrent() {
        persist(accentState.value)
    }

    /** Ripristina l'accento di fabbrica. */
    fun resetToDefault() = setAccent(DEFAULT)

    // ── persistenza ──────────────────────────────────────────────────────

    private fun readSavedAccent(): Color? = try {
        val p = java.util.Properties()
        if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        parseHex(p.getProperty(PREF_KEY)?.trim().orEmpty())
    } catch (t: Throwable) {
        null
    }

    private fun persist(c: Color) {
        try {
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(PREF_KEY, toHex(c))
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop preferences") }
            println("[RiPlay-Accent] accent salvato: ${toHex(c)}")
        } catch (t: Throwable) {
            // la persistenza non deve mai rompere l'app: il colore resta
            // attivo per la sessione corrente
        }
    }

    // ── conversioni colore (jvmMain: niente android.graphics qui) ────────

    /**
     * "#RRGGBB" / "RRGGBB" / "#AARRGGBB" -> Color. null se non valido.
     * Parsing manuale (il progetto desktop non ha android.graphics).
     */
    fun parseHex(s: String): Color? {
        val raw = s.trim().removePrefix("#").lowercase()
        if (raw.isEmpty()) return null
        val v = raw.toLongOrNull(16) ?: return null
        return when (raw.length) {
            6 -> Color(0xFF000000L or v)
            8 -> Color(v)
            else -> null
        }
    }

    /** Color -> "#RRGGBB" (alpha scartato: l'accent e' sempre opaco). */
    fun toHex(c: Color): String {
        val rgb = c.toArgb() and 0x00FFFFFF
        return "#%06X".format(rgb)
    }

    /** Schiarisce il colore mescolandolo con bianco (fraction 0..1). */
    private fun Color.lighten(fraction: Float): Color = Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = alpha
    )
}
