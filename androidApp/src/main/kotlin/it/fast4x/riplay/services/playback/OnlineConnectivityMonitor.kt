package it.fast4x.riplay.services.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * OnlineConnectivityMonitor
 *
 * Monitoraggio robusto e centralizzato della connettività dati per il motore
 * di riproduzione online (AYP / WebView + IFrame API di YouTube).
 *
 * Caratteristiche:
 *  - API 24+ : [ConnectivityManager.registerDefaultNetworkCallback] → segue ESATTAMENTE
 *              la rete di default, cioè quella che la WebView usa davvero per lo streaming.
 *  - API 21..23: [ConnectivityManager.registerNetworkCallback] con NetworkRequest filtrata
 *              sui trasporti dati reali (WiFi / Cellular / Ethernet).
 *  - Fallback estremo (se la registrazione fallisce): receiver sul broadcast legacy.
 *  - DEDUPLICAZIONE dei flapping: gli eventi vengono notificati ai listener SOLO su
 *    transizione reale di stato (perde → perde, ritorna → ritorna).
 *  - Stato iniziale affidabile: [isNetworkAvailable] è valorizzato immediatamente con
 *    un'interrogazione diretta del sistema, senza attendere il primo callback.
 *
 * NOTA DI POLICY: questo monitor misura SOLO la presenza fisica/logica della rete.
 * Le decisioni di playback (grazia sull'handover WiFi↔Cellulare, pausa, ripresa,
 * escalation di recovery) sono di competenza di PlayerService, che è l'unico
 * proprietario dello stato del player ibrido.
 */
class OnlineConnectivityMonitor(private val context: Context) {

    interface Listener {
        /** La rete di default è appena andata persa (o ha perso la capability INTERNET). */
        fun onNetworkLost()

        /** La rete di default è appena tornata disponibile. */
        fun onNetworkRestored()
    }

    private val listeners = mutableListOf<Listener>()

    private val _isAvailable = MutableStateFlow(false)

    /** Flow osservabile dello stato di connettività (true = rete dati presente). */
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    /** Lettura sincrona e sempre aggiornata dello stato di connettività. */
    @Volatile
    var isNetworkAvailable: Boolean = false
        private set

    private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var requestNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var legacyReceiver: BroadcastReceiver? = null

    private var started = false

    // ─────────────────────────────── API pubblica ───────────────────────────────

    @Synchronized
    fun addListener(listener: Listener) {
        synchronized(listeners) { listeners.add(listener) }
    }

    @Synchronized
    fun removeListener(listener: Listener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    /**
     * Avvia l'osservazione. Idempotente: chiamate successive sono no-op.
     * Emette lo stato iniziale senza generare eventi ai listener (solo StateFlow).
     */
    @Synchronized
    fun start() {
        if (started) return
        started = true

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            Timber.e("OnlineConnectivityMonitor: ConnectivityManager non disponibile, monitor disattivo")
            setAvailabilityInternal(false, "no-connectivity-service", dispatchToListeners = false)
            return
        }

        // Stato iniziale immediato: se il servizio parte con il telefono in aereo
        // (o in un garage) dobbiamo saperlo PRIMA del primo callback di sistema.
        val initial = queryCurrentAvailability(connectivityManager)
        setAvailabilityInternal(initial, "stato-iniziale", dispatchToListeners = false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateAvailability(true, "onAvailable")
                }

                override fun onLost(network: Network) {
                    updateAvailability(false, "onLost")
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    // Aggiornamenti frequenti (link stats): deduplicati da updateAvailability.
                    // Usiamo NET_CAPABILITY_INTERNET e non VALIDATED: dietro alcuni portali
                    // captive o APN lenti la validated arriva in ritardo ma lo streaming
                    // può comunque ripartire; gli stalli residui li copre il watchdog.
                    updateAvailability(
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                        "onCapabilitiesChanged"
                    )
                }
            }
            defaultNetworkCallback = callback
            runCatching {
                connectivityManager.registerDefaultNetworkCallback(callback)
            }.onFailure {
                Timber.e("OnlineConnectivityMonitor: registerDefaultNetworkCallback fallito: ${it.message}")
                fallbackToRequestCallback(connectivityManager)
            }
        } else {
            fallbackToRequestCallback(connectivityManager)
        }

        Timber.i("OnlineConnectivityMonitor: avviato (rete disponibile = $initial, SDK = ${Build.VERSION.SDK_INT})")
    }

    /** Arresta l'osservazione e rilascia tutte le risorse di sistema. */
    @Synchronized
    fun stop() {
        if (!started) return
        started = false

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        defaultNetworkCallback?.let { callback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
            }
        }
        defaultNetworkCallback = null

        requestNetworkCallback?.let { callback ->
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        }
        requestNetworkCallback = null

        legacyReceiver?.let { receiver ->
            runCatching { context.unregisterReceiver(receiver) }
        }
        legacyReceiver = null

        Timber.i("OnlineConnectivityMonitor: arrestato")
    }

    /**
     * Interrogazione diretta dello stato attuale (usata anche come doppio controllo
     * prima di decidere pausa/ripresa, per filtrare i falsi positivi dei callback).
     */
    fun queryCurrentAvailability(connectivityManager: ConnectivityManager? = null): Boolean {
        val cm = connectivityManager
            ?: context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                val info = cm.activeNetworkInfo
                info != null && info.isConnected
            }
        }.getOrDefault(false)
    }

    // ─────────────────────────────── implementazione ───────────────────────────────

    private fun fallbackToRequestCallback(connectivityManager: ConnectivityManager) {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = updateAvailability(true, "request.onAvailable")
            override fun onLost(network: Network) = updateAvailability(false, "request.onLost")
            override fun onUnavailable() = updateAvailability(false, "request.onUnavailable")
        }

        requestNetworkCallback = callback
        val registered = runCatching {
            connectivityManager.registerNetworkCallback(request, callback)
        }.onFailure {
            Timber.e("OnlineConnectivityMonitor: registerNetworkCallback fallito: ${it.message}")
        }.isSuccess

        if (!registered) registerLegacyReceiver(connectivityManager)
    }

    /** Fallback d'emergenza per dispositivi che rifiutano entrambe le registrazioni callback. */
    private fun registerLegacyReceiver(@Suppress("UNUSED_PARAMETER") connectivityManager: ConnectivityManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                updateAvailability(queryCurrentAvailability(cm), "legacy-broadcast")
            }
        }
        legacyReceiver = receiver
        runCatching {
            context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }.onFailure {
            Timber.e("OnlineConnectivityMonitor: registrazione receiver legacy fallita: ${it.message}")
        }
    }

    /**
     * Aggiorna lo stato SOLO su transizione reale e notifica i listener.
     * I callback di sistema possono arrivare ripetuti (handover, capability refresh):
     * qui li filtriamo per non far decidere pausa/ripresa al player due volte.
     * Sincronizzato: i callback di sistema arrivano su thread dedicati.
     */
    @Synchronized
    private fun updateAvailability(available: Boolean, source: String) {
        if (available == isNetworkAvailable) return
        setAvailabilityInternal(available, source, dispatchToListeners = true)
    }

    private fun setAvailabilityInternal(available: Boolean, source: String, dispatchToListeners: Boolean) {
        _isAvailable.value = available
        isNetworkAvailable = available
        Timber.i("OnlineConnectivityMonitor: rete ${if (available) "DISPONIBILE" else "PERDUTA"} (origine: $source)")

        if (dispatchToListeners) {
            val snapshot: List<Listener> = synchronized(listeners) { listeners.toList() }
            snapshot.forEach { listener ->
                runCatching {
                    if (available) listener.onNetworkRestored() else listener.onNetworkLost()
                }.onFailure {
                    Timber.e("OnlineConnectivityMonitor: errore notifica listener: ${it.message}")
                }
            }
        }
    }
}
