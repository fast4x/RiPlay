package it.fast4x.riplay.server

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.io.File
import java.io.InputStream
import java.net.NetworkInterface
import java.security.SecureRandom

/**
 * RiPlay Relay — server di streaming locale (Fase 2), engine Ktor CIO.
 *
 * Modulo condiviso relayserver: identico comportamento su Android e desktop.
 *
 * Doppio ruolo:
 * 1. sorgente per il cast UPnP/DLNA (Android) dei contenuti senza URL diretto
 *    (file locali, Music Vault, content://): il `SetAVTransportURI` punta a
 *    `http://<ip-telefono>:<porta>/relay/<token>/s/<id>` e l'altoparlante scarica
 *    lo stream dal telefono
 * 2. sorgente generica per software esterni (VLC, browser, altri RiPlay):
 *    playlist M3U della coda corrente + endpoint info, protetti da token
 *
 * Sicurezza:
 * - token di accesso casuale, generato al primo avvio e persistente (link M3U stabili)
 * - solo i brani nel registro (whitelist) sono servibili
 * - il server ascolta solo sulla LAN: nessuna esposizione oltre la rete locale
 *
 * Le parti dipendenti dalla piattaforma sono iniettate dal chiamante
 * (androidApp -> AndroidRelay.kt, composeApp jvmMain -> DesktopRelay.kt):
 * - [RelayStreamSource]: da dove vengono i byte dei brani (file/content Android,
 *   percorsi file desktop, buffer nei test)
 * - [RelayTokenStore]: dove persiste il token di accesso (SharedPreferences su
 *   Android, ~/.riplay/relay.properties su desktop)
 *
 * Moduli aggiuntivi sulla STESSA applicazione Ktor (stessa porta, stesso token):
 * - [extraModules]: funzioni opzionali che installano ulteriori routing/plugins
 *   nell'applicazione del relay, invocate DOPO relayEndpoints con il token
 *   corrente. Oggi ospita il bridge Music Assistant (modulo riplay-bridge,
 *   API /api/v1 della libreria per il provider Python esterno): il bridge non
 *   apre porte nuove e non eroga byte, pubblica solo URL verso /relay/<token>/s/<id>.
 *   null/assente = applicazione identica a prima (solo relay).
 */
class RelayServer(
    private val registry: RelayRegistry,
    source: RelayStreamSource? = null,
    private val tokenStore: RelayTokenStore? = null,
    private val preferredPort: Int = DEFAULT_PORT,
    private val extraModules: (Application.(token: String) -> Unit)? = null,
) {

    /** Sorgente dei byte: iniettabile nei test, obbligatoria in produzione. */
    private val effectiveSource: RelayStreamSource = source ?: NullRelaySource

    private var embedded: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    @Volatile
    var actualPort: Int = preferredPort
        private set

    @Volatile
    var token: String = loadOrCreateToken()
        private set

    val isRunning: Boolean
        get() = embedded != null

    /**
     * Avvia il server su [preferredPort] o, se occupata, sulle successive
     * (fino a [PORT_ATTEMPTS]). Ritorna true se avviato.
     *
     * r2: log diagnostici con tag esplicito (visibili anche filtrando logcat per
     * tag) e catch su Throwable: un errore di classe mancante (es. R8 in build
     * release) e' un Error e prima della r2 usciva dal loop senza log utile.
     * Nota Ktor 3.6: start(wait=false) e' sincrono (runBlocking) e rilancia gli
     * errori di bind: il catch qui e' attendibile.
     *
     * r3: host esplicito "0.0.0.0" (default della funzione embeddedServer in Ktor
     * 3.6, reso esplicito qui per garantire l'ascolto su TUTTE le interfacce: con
     * un bind su loopback il server risultava avviato ma nessun client della LAN
     * poteva collegarsi). Log del bind esplicito nel messaggio di avvio.
     */
    fun start(): Boolean {
        if (embedded != null) return true
        repeat(PORT_ATTEMPTS) { attempt ->
            val port = preferredPort + attempt
            try {
                RelayLog.i(TAG, "start: tentativo bind 0.0.0.0:%d (%d/%d)", port, attempt + 1, PORT_ATTEMPTS)
                val server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                    relayEndpoints(token = token, registry = registry, source = effectiveSource)
                    // Moduli aggiuntivi nella STESSA applicazione Ktor (stessa porta,
                    // stesso token): oggi il bridge Music Assistant (riplay-bridge).
                    // Vedi BridgeIntegration.kt (androidApp) e DesktopBridge.kt (desktop).
                    extraModules?.invoke(this, token)
                }
                server.start(wait = false)
                embedded = server
                actualPort = port
                RelayLog.i(
                    TAG, "server AVVIATO su 0.0.0.0:%d (tutte le interfacce; token %s...)",
                    port, token.take(4)
                )
                return true
            } catch (e: Throwable) {
                RelayLog.w(
                    e, TAG, "porta %d non disponibile (tentativo %d/%d): %s: %s",
                    port, attempt + 1, PORT_ATTEMPTS, e.javaClass.name, e.message
                )
            }
        }
        RelayLog.e(
            TAG, "nessuna porta libera da %d a %d (bind su 0.0.0.0, tutte le interfacce)",
            preferredPort, preferredPort + PORT_ATTEMPTS - 1
        )
        return false
    }

    fun stop() {
        runCatching { embedded?.stop(500, 1_000) }
        embedded = null
        RelayLog.i(TAG, "server FERMATO")
    }

    /** Base URL del server sull'interfaccia Wi-Fi/LAN corrente (null se offline). */
    fun baseUrl(): String? {
        val ip = firstLanIpv4()
        if (ip == null) {
            RelayLog.w(TAG, "baseUrl: nessun indirizzo IPv4 LAN rilevato (Wi-Fi spento o solo VPN/cellulare?)")
            return null
        }
        return "http://$ip:$actualPort/relay/$token"
    }

    /** URL stream per una traccia (usato dal cast per il SetAVTransportURI). */
    fun streamUrlFor(track: RelayTrack): String? =
        baseUrl()?.let { RelayUris.streamUrl(it, track) }

    // ─────────────────── Token ───────────────────

    private fun loadOrCreateToken(): String {
        tokenStore?.load()?.let { return it }
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        tokenStore?.save(token)
        return token
    }

    companion object {

        /** Tag per i log diagnostici (filtrabile anche in console/logcat). */
        const val TAG = "RelayServer"

        const val DEFAULT_PORT = 8765
        const val PORT_ATTEMPTS = 10

        /**
         * Miglior IPv4 LAN disponibile (preferisce wlan/eth).
         *
         * r2: piu' tollerante della prima versione. Prima accettava SOLO indirizzi
         * site-local (10.x, 172.16-31.x, 192.168.x): su reti con range meno comuni
         * (es. 100.64.x.x CGNAT di hotspot/tethering) restituiva null e la UI mostrava
         * "server non attivo" anche a server in ascolto. Ora: 1) site-local su
         * wlan/eth, 2) qualunque IPv4 su wlan/eth, 3) qualunque site-local, 4) qualunque
         * IPv4 non-loopback. Ogni candidato viene loggato (diagnostica VLC).
         *
         * java.net.NetworkInterface e' API JVM standard: funziona identica su Android
         * (ART) e desktop (HotSpot). I nomi interfaccia variano per OS (wlan0/eth0 su
         * Linux, en0 su macOS, "eth"/"wlan" anche su Windows): la regola wlan/eth
         * preferisce le interfacce Wi-Fi/cavo, il fallback Site-local -> qualunque
         * IPv4 non-loopback copre i nomi non standard.
         */
        fun firstLanIpv4(): String? {
            val siteLocal = mutableListOf<Pair<String, String>>()   // (nome interfaccia, ip)
            val others = mutableListOf<Pair<String, String>>()
            runCatching {
                NetworkInterface.getNetworkInterfaces().asSequence().forEach { nif ->
                    if (!nif.isUp || nif.isLoopback) return@forEach
                    nif.inetAddresses.asSequence()
                        .filter { !it.isLoopbackAddress }
                        .filterIsInstance<java.net.Inet4Address>()
                        .forEach { address ->
                            val host = address.hostAddress ?: return@forEach
                            // hostAddress puo' restituire "192.168.1.10/24" su alcune ROM: normalizziamo
                            val clean = host.substringBefore('/')
                            if (address.isSiteLocalAddress) siteLocal.add(nif.name to clean)
                            else others.add(nif.name to clean)
                        }
                }
            }
            RelayLog.d(
                TAG, "interfacce LAN: siteLocal=%s other=%s",
                siteLocal.joinToString { "${it.first}=${it.second}" },
                others.joinToString { "${it.first}=${it.second}" }
            )
            val isWifiLike = { entry: Pair<String, String> ->
                entry.first.startsWith("wlan") || entry.first.startsWith("eth")
            }
            return siteLocal.firstOrNull(isWifiLike)?.second
                ?: others.firstOrNull(isWifiLike)?.second
                ?: siteLocal.firstOrNull()?.second
                ?: others.firstOrNull()?.second
        }
    }
}

/**
 * Persistenza del token di accesso del relay (link M3U stabili tra riavvii).
 * Implementazioni: SharedPreferences su Android ("relay_prefs"), file
 * ~/.riplay/relay.properties su desktop.
 */
interface RelayTokenStore {
    /** Token salvato in precedenza, o null se mai generato. */
    fun load(): String?

    /** Salva il token generato. Chiamato una sola volta (primo avvio). */
    fun save(token: String)
}

/**
 * Sorgente di riserva quando nessuna sorgente reale e' stata iniettata
 * (istanze di test/diagnostica): nessun brano e' servibile, risponde sempre 404.
 */
private object NullRelaySource : RelayStreamSource {
    override fun probe(track: RelayTrack): RelayMeta? = null
    override fun open(track: RelayTrack, range: HttpRange?): InputStream? = null
}

/** MIME type dal nome file (per il Content-Type verso i renderer). */
object RelayMime {

    /**
     * Hook per resolver di estensioni specifici della piattaforma. Android lo
     * imposta su android.webkit.MimeTypeMap (stesso comportamento della versione
     * pre-refactor: MimeTypeMap PRIMA, poi la tabella interna); desktop lo lascia
     * null e usa solo la tabella interna.
     */
    @Volatile
    var extensionResolver: ((extension: String) -> String?)? = null

    fun fromFileName(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        // Nome con o senza percorso: l'ultimo segmento dopo '/' (senza slash = nome gia' nudo).
        // CORREZIONE durante la migrazione al modulo condiviso: la versione Android usava
        // substringAfterLast('/', "") che ritorna STRINGA VUOTA quando il nome non contiene
        // slash (es. "song.mp3" da getFileNameFromUri/ContentResolver DISPLAY_NAME): l'estensione
        // andava perduta, fallbackFor non era mai raggiunto e i file uscivano quasi sempre
        // come application/octet-stream. Con missingDelimiterValue = fileName il nome resta
        // intatto e la tabella MIME funziona (Content-Type corretto verso renderer UPnP/VLC).
        val base = fileName.substringAfterLast('/', fileName)
        // Nessun punto = nessuna estensione: null ("" come default e' qui corretto).
        val extension = base.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return null
        return extensionResolver?.invoke(extension) ?: fallbackFor(extension)
    }

    /**
     * Estensioni audio/video comuni che il resolver di piattaforma a volte non
     * conosce (MIME dell'ultima versione Android; desktop copre anche wma/aiff/
     * ape/wv/mka che MimeTypeMap non mappa comunque).
     */
    private fun fallbackFor(extension: String): String? = when (extension) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4", "aac" -> "audio/mp4"
        "flac" -> "audio/flac"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/ogg"
        "wav" -> "audio/wav"
        "m3u", "m3u8" -> "audio/x-mpegurl"
        // estensioni extra per il desktop (VLC serve un Content-Type valido)
        "wma" -> "audio/x-ms-wma"
        "aif", "aiff" -> "audio/aiff"
        "ape" -> "audio/x-ape"
        "wv" -> "audio/x-wavpack"
        "mka" -> "audio/x-matroska"
        else -> null
    }
}
