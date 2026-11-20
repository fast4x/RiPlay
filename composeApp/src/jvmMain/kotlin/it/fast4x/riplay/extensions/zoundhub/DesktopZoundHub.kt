package it.fast4x.riplay.extensions.zoundhub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.fast4x.riplay.data.ZoundHubDecisionPersistenceRoom
import it.fast4x.zoundhub.ZoundHubCatalogStore
import it.fast4x.zoundhub.ZoundHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties

/**
 * PORTING DESKTOP del client ZoundHub (specchio di ZoundHubClient.kt
 * androidApp, stessa filosofia di DesktopLastFm.kt).
 *
 * Il modulo :zoundhub e' un modulo kotlin("jvm") puro (ktor/okhttp) gia'
 * incluso nelle dipendenze commonMain di composeApp: funziona su desktop
 * senza modifiche. Questo object fornisce solo il WIRING desktop:
 *  - directory cache = data dir utente/RiPlay/zoundhub (stessa convenzione
 *    del database, vedi DatabaseInitializer.desktop.kt);
 *  - toggle delle impostazioni persistito su ~/.riplay/zoundhub.properties
 *    (stesso pattern di DesktopMiniPlayer);
 *  - caricamento della cache a freddo (nessuna rete all'avvio).
 */
object DesktopZoundHub {

    /** Data dir dell'app desktop (Windows/macOS/Linux), come il database. */
    private fun dataDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = when {
            os.contains("win") ->
                File(System.getenv("APPDATA") ?: File(home, "AppData/Roaming").absolutePath, "RiPlay")
            os.contains("mac") ->
                File(home, "Library/Application Support/RiPlay")
            else ->
                File(
                    System.getenv("XDG_DATA_HOME") ?: File(home, ".local/share").absolutePath,
                    "RiPlay"
                )
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("zoundhub.properties")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_HIDDEN = "hide_ai"

    private fun loadPrefs(): Properties = try {
        Properties().also { p ->
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        }
    } catch (t: Throwable) {
        Properties()
    }

    private fun savePrefs(p: Properties) = try {
        prefsFile.parentFile?.mkdirs()
        prefsFile.outputStream().use { p.store(it, "RiPlay desktop zoundhub") }
    } catch (t: Throwable) {
        // persistenza silenziosa: mai bloccare la UI per un file di preferenze
    }

    private fun readEnabled(): Boolean = try {
        loadPrefs().getProperty(KEY_ENABLED, "true").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        true
    }

    private fun readHidden(): Boolean = try {
        loadPrefs().getProperty(KEY_HIDDEN, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    /** Scope di avvio del catalogo (una sola volta per processo, fuori dalla UI). */
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Persistenza del catalogo in memoria: avviata al primo accesso (lazy).
     *
     * v1.4: le decisioni del filtro AI sono persistite nel database Room
     * (tabella zoundhub_decisions, cache derivata taggata per versione di
     * catalogo) — al riavvio il filtro parte a caldo anche su hardware
     * lento, senza ripagare i miss fuzzy per i testi autori gia' visti.
     */
    val store: ZoundHubCatalogStore by lazy {
        val dir = dataDir().resolve("zoundhub")
        ZoundHubCatalogStore(
            cacheDir = dir,
            service = ZoundHubService(),
            decisionPersistence = ZoundHubDecisionPersistenceRoom.fromCurrentDatabase(),
        ).also {
            // FIX FREEZE (v1.1): come su Android, niente runBlocking sull'EDT:
            // il parse del JSON (~3 MB) gira in background e la UI osserva store.state.
            initScope.launch { it.loadFromCache() }
        }
    }

    /**
     * Toggle delle impostazioni (tab Generale, SettingsPage). Stato Compose:
     * la UI (badge nelle liste e nel player) si ricompone subito.
     */
    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    /** Cambia e PERSISTE subito (l'utente non deve "salvare" nulla). */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        try {
            val p = loadPrefs()
            p.setProperty(KEY_ENABLED, value.toString())
            savePrefs(p)
        } catch (t: Throwable) {
            // silenzioso
        }
    }

    /**
     * Filtro "nascondi contenuti AI": rimuove dalle liste home/pagine i
     * contenuti di artisti segnalati come AI (default OFF, come su Android).
     * Stato Compose: le liste si ri-filtrano subito al cambio.
     */
    var hidden: Boolean by mutableStateOf(readHidden())
        private set

    /** Cambia e PERSISTE subito. */
    fun setHidden(value: Boolean) {
        if (hidden == value) return
        hidden = value
        try {
            val p = loadPrefs()
            p.setProperty(KEY_HIDDEN, value.toString())
            savePrefs(p)
        } catch (t: Throwable) {
            // silenzioso
        }
    }
}
