package it.fast4x.riplay.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RiPlay Relay — facciata singleton (Fase 2), modulo condiviso androidApp/desktop.
 *
 * Ogni app installa UNA VOLTA (all'avvio del processo) il proprio
 * [RelayServerProvider] tramite [install]:
 * - androidApp: MainApplication.onCreate -> installAndroidRelayServer(this)
 *   (sorgente file/content + token in SharedPreferences)
 * - desktop: bootstrapDesktopApp -> installDesktopRelayServer()
 *   (sorgente file + token in ~/.riplay/relay.properties)
 *
 * PlayerService (Android) avvia/ferma il server e registra i brani (cast + sync
 * coda); la UI (UpnpCastSelector, impostazioni Android, SettingsPage desktop)
 * legge lo stato e gli indirizzi. La coda desktop e' sincronizzata dal watcher
 * di ThreeColumnsApp (syncDesktopRelayQueue).
 *
 * r2: log diagnostici con tag "RelayManager" su ogni decisione di avvio/stop
 * (utile per capire dai log perche' il server non risulta attivo).
 */
object RelayManager {

    private const val TAG = "RelayManager"

    private val lock = Any()

    /** Registro condiviso: whitelist tracce + ordine coda per la playlist M3U. */
    val registry = RelayRegistry()

    /** Provider installato dall'app host (crea il RelayServer con le sue dipendenze). */
    @Volatile
    private var provider: RelayServerProvider? = null

    private var server: RelayServer? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _baseUrl = MutableStateFlow<String?>(null)
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()

    /** Porta effettiva (8765 o successiva se occupata). */
    val port: Int
        get() = synchronized(lock) { server?.actualPort ?: RelayServer.DEFAULT_PORT }

    /**
     * Installa il provider della piattaforma (idempotente: l'ultima chiamata vince,
     * ma ogni app lo chiama una sola volta all'avvio). Deve avvenire PRIMA del
     * primo [startIfEnabled].
     */
    fun install(provider: RelayServerProvider) {
        synchronized(lock) {
            this.provider = provider
            RelayLog.i(TAG, "provider installato: %s", provider.javaClass.name)
        }
    }

    /**
     * Avvia il server se abilitato e non gia' attivo; lo ferma se disabilitato.
     * Chiamato da PlayerService Android all'avvio (e dal setting quando l'utente
     * cambia), dal bootstrap desktop e dalla SettingsPage desktop.
     *
     * @return true se al termine il server e' in esecuzione.
     */
    fun startIfEnabled(enabled: Boolean): Boolean {
        synchronized(lock) {
            RelayLog.i(
                TAG, "startIfEnabled: enabled=%s serverAttivo=%s",
                enabled, server?.isRunning ?: false
            )
            if (!enabled) {
                stopInternal()
                return false
            }
            if (server?.isRunning == true) {
                refreshUrlInternal()
                return true
            }
            val created = try {
                provider?.create()
            } catch (e: Throwable) {
                RelayLog.e(e, TAG, "creazione RelayServer fallita (provider %s)", provider?.javaClass?.name)
                null
            }
            if (created == null) {
                _running.value = false
                _baseUrl.value = null
                RelayLog.e(TAG, "server NON avviato: nessun provider installato (chiamare install() all'avvio)")
                return false
            }
            if (created.start()) {
                server = created
                _running.value = true
                _baseUrl.value = created.baseUrl()
                RelayLog.i(TAG, "server avviato, baseUrl=%s", _baseUrl.value ?: "<null: nessun IP LAN>")
                return true
            }
            _running.value = false
            _baseUrl.value = null
            RelayLog.e(TAG, "server NON avviato (nessuna porta libera o errore bind)")
            return false
        }
    }

    /** Ricalcola l'URL (l'IP LAN puo' cambiare: riconnessione Wi-Fi, nuova rete). */
    fun refreshUrl() {
        synchronized(lock) { refreshUrlInternal() }
    }

    fun stop() {
        synchronized(lock) { stopInternal() }
    }

    /** URL dello stream per il cast UPnP (SetAVTransportURI); null se offline. */
    fun buildStreamUrl(track: RelayTrack): String? {
        synchronized(lock) {
            refreshUrlInternal()
            return server?.streamUrlFor(track)
        }
    }

    /** URL della playlist M3U per i player esterni. */
    val playlistUrl: String?
        get() {
            synchronized(lock) { refreshUrlInternal() }
            return _baseUrl.value?.let { "$it/playlist.m3u" }
        }

    // ─────────────────── interno (chiamate con lock preso) ───────────────────

    private fun refreshUrlInternal() {
        val current = server ?: return
        if (!current.isRunning) return
        val updated = current.baseUrl()
        if (updated != _baseUrl.value) {
            RelayLog.i(TAG, "baseUrl aggiornata: %s", updated ?: "<null>")
        }
        _baseUrl.value = updated
    }

    private fun stopInternal() {
        if (server != null || _running.value) {
            RelayLog.i(TAG, "stop server (enabled=false o shutdown)")
        }
        server?.stop()
        server = null
        _running.value = false
        _baseUrl.value = null
    }
}

/**
 * Crea il RelayServer con le dipendenze della piattaforma (sorgente byte +
 * persistenza token). Installato una sola volta all'avvio dell'app.
 */
fun interface RelayServerProvider {
    fun create(): RelayServer
}
