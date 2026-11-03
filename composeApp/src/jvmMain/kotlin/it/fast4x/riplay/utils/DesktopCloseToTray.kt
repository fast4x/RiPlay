package it.fast4x.riplay.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * CHIUSURA NELL'AREA DI NOTIFICA (tray) OPZIONALE.
 *
 * Fino allo step 9 chiudere la finestra con la X NASCONDEVA SEMPRE l'app
 * nel tray (la musica continuava e il doppio click sull'icona riapriva).
 * Da questo step il comportamento e' deciso da questo interruttore
 * (Impostazioni -> tab Generale):
 *  - ON:  la X NASCONDE la finestra nel tray, la riproduzione prosegue,
 *         il doppio click sull'icona (o "Mostra RiPlay") riporta la
 *         finestra e "Esci da RiPlay" nel menu del tray esce DAVVERO;
 *  - OFF (DEFAULT): la X ESCE dall'app, come un programma normale.
 *
 * Stesso schema di DesktopMiniPlayer: stato Compose + persistenza
 * IMMEDIATA su ~/.riplay/trayclose.properties (l'utente non deve
 * "salvare" nulla, e mai bloccare la UI per un file di preferenze).
 * DesktopTray NON viene toccato: e' main.kt a leggere questo flag e a
 * decidere cosa fare della chiusura (se il tray non e' disponibile la
 * chiusura torna comunque a uscire dall'app, come da step 9).
 */
object DesktopCloseToTray {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("trayclose.properties")

    private const val KEY_ENABLED = "enabled"

    /** Toggle delle impostazioni (tab Generale). Default: OFF. */
    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    /** Cambia e PERSISTE subito (l'utente non deve "salvare" nulla). */
    fun setEnabledCloseToTray(value: Boolean) {
        if (enabled == value) return
        enabled = value
        persistEnabled(value)
    }

    private fun readEnabled(): Boolean = try {
        val p = java.util.Properties()
        if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        p.getProperty(KEY_ENABLED, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    private fun persistEnabled(value: Boolean) {
        try {
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(KEY_ENABLED, value.toString())
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop close-to-tray") }
        } catch (t: Throwable) {
            // persistenza silenziosa: mai bloccare la UI per un file di preferenze
        }
    }
}
