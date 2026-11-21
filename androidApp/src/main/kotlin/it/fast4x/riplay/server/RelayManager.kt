package it.fast4x.riplay.server

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * RiPlay Relay — facciata singleton (Fase 2).
 *
 * PlayerService avvia/ferma il server e registra i brani (cast + sync coda),
 * la UI (UpnpCastSelector, impostazioni) legge lo stato e gli indirizzi.
 *
 * r2: log diagnostici con tag "RelayManager" su ogni decisione di avvio/stop
 * (utile per capire da logcat perche' il server non risulta attivo).
 */
object RelayManager {

    private const val TAG = "RelayManager"

    private val lock = Any()

    /** Registro condiviso: whitelist tracce + ordine coda per la playlist M3U. */
    val registry = RelayRegistry()

    private var server: RelayServer? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _baseUrl = MutableStateFlow<String?>(null)
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()

    /** Porta effettiva (8765 o successiva se occupata). */
    val port: Int
        get() = synchronized(lock) { server?.actualPort ?: RelayServer.DEFAULT_PORT }

    /**
     * Avvia il server se abilitato e non gia' attivo; lo ferma se disabilitato.
     * Chiamato da PlayerService all'avvio (e dal setting quando l'utente cambia).
     */
    fun startIfEnabled(context: Context, enabled: Boolean) {
        synchronized(lock) {
            Timber.tag(TAG).i(
                "startIfEnabled: enabled=%s serverAttivo=%s",
                enabled, server?.isRunning ?: false
            )
            if (!enabled) {
                stopInternal()
                return
            }
            if (server?.isRunning == true) {
                refreshUrlInternal()
                return
            }
            val relay = RelayServer(context.applicationContext, registry)
            if (relay.start()) {
                server = relay
                _running.value = true
                _baseUrl.value = relay.baseUrl()
                Timber.tag(TAG).i("server avviato, baseUrl=%s", _baseUrl.value ?: "<null: nessun IP LAN>")
            } else {
                _running.value = false
                _baseUrl.value = null
                Timber.tag(TAG).e("server NON avviato (nessuna porta libera o errore bind)")
            }
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
            Timber.tag(TAG).i("baseUrl aggiornata: %s", updated ?: "<null>")
        }
        _baseUrl.value = updated
    }

    private fun stopInternal() {
        if (server != null || _running.value) {
            Timber.tag(TAG).i("stop server (enabled=false o shutdown)")
        }
        server?.stop()
        server = null
        _running.value = false
        _baseUrl.value = null
    }
}
