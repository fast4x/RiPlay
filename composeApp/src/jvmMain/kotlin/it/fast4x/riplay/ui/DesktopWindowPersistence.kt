package it.fast4x.riplay.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.io.File

/**
 * STEP 3h (migliorie visive): RICORDA LA DIMENSIONE DELLA FINESTRA.
 *
 * Prima la finestra nasceva SEMPRE massimizzata (WindowState di default
 * in main.kt): riavviare dopo aver ripristinato perdeva la scelta.
 *
 * Ora all'uscita (pulsante chiudi, Alt+F4: in entrambi i casi passa da
 * onCloseRequest) viene salvato un piccolo file di proprieta' in
 * ~/.riplay/window.properties con:
 *  - maximized = true/false;
 *  - width/height (dp) quando la finestra NON e' massimizzata.
 * All'avvio le preferenze vengono rilette: se mancano o sono corrotte
 * si torna al comportamento di prima (massimizzata).
 *
 * La POSIZIONE non viene salvata di proposito: la lasciamo alla
 * piattaforma (position default), cosi' non si rischia di riaprire la
 * finestra fuori dallo schermo dopo un cambio monitor.
 */

private val windowPrefsFile: File
    get() = File(System.getProperty("user.home"), ".riplay").resolve("window.properties")

/** Preferenze di finestra lette dal disco (valori di default sicuri). */
data class DesktopWindowPrefs(
    val maximized: Boolean = true,
    val widthDp: Float = 1280f,
    val heightDp: Float = 800f
)

fun loadDesktopWindowPrefs(): DesktopWindowPrefs = try {
    val p = java.util.Properties()
    if (windowPrefsFile.isFile) {
        windowPrefsFile.inputStream().use { p.load(it) }
    }
    DesktopWindowPrefs(
        maximized = p.getProperty("maximized", "true").toBooleanStrictOrNull() ?: true,
        widthDp = p.getProperty("width")?.toFloatOrNull()?.coerceIn(480f, 7680f) ?: 1280f,
        heightDp = p.getProperty("height")?.toFloatOrNull()?.coerceIn(360f, 4320f) ?: 800f
    )
} catch (t: Throwable) {
    DesktopWindowPrefs()
}

/** Stato iniziale della finestra dalle preferenze salvate. */
fun initialWindowState(prefs: DesktopWindowPrefs = loadDesktopWindowPrefs()): WindowState =
    if (prefs.maximized)
        WindowState(placement = WindowPlacement.Maximized)
    else
        WindowState(
            placement = WindowPlacement.Floating,
            size = DpSize(prefs.widthDp.dp, prefs.heightDp.dp)
        )

/** Salvataggio allo spegnimento: ignora ogni errore (mai bloccare l'uscita). */
fun saveDesktopWindowPrefs(state: WindowState) {
    try {
        val p = java.util.Properties()
        val maximized = state.placement == WindowPlacement.Maximized
        p.setProperty("maximized", maximized.toString())
        if (!maximized) {
            p.setProperty("width", state.size.width.value.toString())
            p.setProperty("height", state.size.height.value.toString())
        }
        windowPrefsFile.parentFile?.mkdirs()
        windowPrefsFile.outputStream().use { p.store(it, "RiPlay desktop window") }
    } catch (t: Throwable) {
        // silenzioso: la persistenza non deve mai impedire la chiusura
    }
}
