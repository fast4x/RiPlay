package it.fast4x.riplay.server

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import it.fast4x.riplay.utils.getFileNameFromUri
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.NetworkInterface
import java.security.SecureRandom

/**
 * RiPlay Relay — server di streaming locale (Fase 2), engine Ktor CIO.
 *
 * Doppio ruolo:
 * 1. sorgente per il cast UPnP/DLNA dei contenuti senza URL diretto (file locali,
 *    Music Vault, content://): il `SetAVTransportURI` punta a
 *    `http://<ip-telefono>:<porta>/relay/<token>/s/<id>` e l'altoparlante scarica
 *    lo stream dal telefono
 * 2. sorgente generica per software esterni (VLC, browser, altri RiPlay):
 *    playlist M3U della coda corrente + endpoint info, protetti da token
 *
 * Sicurezza:
 * - token di accesso casuale, generato al primo avvio e persistente (link M3U stabili)
 * - solo i brani nel registro (whitelist) sono servibili
 * - il server ascolta solo sulla LAN: nessuna esposizione oltre la rete locale
 */
class RelayServer(
    private val context: Context,
    private val registry: RelayRegistry,
    source: RelayStreamSource? = null,
    private val preferredPort: Int = DEFAULT_PORT,
) {

    /** Sorgente dei byte: iniettabile nei test, di default serve i file locali. */
    private val effectiveSource: RelayStreamSource = source ?: LocalSource()

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
                Timber.tag(TAG).i("start: tentativo bind 0.0.0.0:%d (%d/%d)", port, attempt + 1, PORT_ATTEMPTS)
                val server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                    relayEndpoints(token = token, registry = registry, source = effectiveSource)
                }
                server.start(wait = false)
                embedded = server
                actualPort = port
                Timber.tag(TAG).i(
                    "server AVVIATO su 0.0.0.0:%d (tutte le interfacce; token %s...)",
                    port, token.take(4)
                )
                return true
            } catch (e: Throwable) {
                Timber.tag(TAG).w(
                    e,
                    "porta %d non disponibile (tentativo %d/%d): %s: %s",
                    port, attempt + 1, PORT_ATTEMPTS, e.javaClass.name, e.message
                )
            }
        }
        Timber.tag(TAG).e(
            "nessuna porta libera da %d a %d (bind su 0.0.0.0, tutte le interfacce)",
            preferredPort, preferredPort + PORT_ATTEMPTS - 1
        )
        return false
    }

    fun stop() {
        runCatching { embedded?.stop(500, 1_000) }
        embedded = null
        Timber.tag(TAG).i("server FERMATO")
    }

    /** Base URL del server sull'interfaccia Wi-Fi/LAN corrente (null se offline). */
    fun baseUrl(): String? {
        val ip = firstLanIpv4()
        if (ip == null) {
            Timber.tag(TAG).w("baseUrl: nessun indirizzo IPv4 LAN rilevato (Wi-Fi spento o solo VPN/cellulare?)")
            return null
        }
        return "http://$ip:$actualPort/relay/$token"
    }

    /** URL stream per una traccia (usato dal cast per il SetAVTransportURI). */
    fun streamUrlFor(track: RelayTrack): String? =
        baseUrl()?.let { RelayUris.streamUrl(it, track) }

    // ─────────────────── Serving file locali ───────────────────

    /** Sorgente dei byte per i brani registrati: file:// via RandomAccessFile
     *  (Range esatto), content:// via ContentResolver (skip per il seek). */
    private inner class LocalSource : RelayStreamSource {

        override fun probe(track: RelayTrack): RelayMeta? {
            val uri = runCatching { Uri.parse(track.uri) }.getOrNull() ?: return null
            val size = localContentLength(uri) ?: return null
            if (size <= 0) return null
            return RelayMeta(totalSize = size, mime = track.mime ?: RelayMime.fromFileName(fileNameOf(uri)))
        }

        override fun open(track: RelayTrack, range: HttpRange?): InputStream? {
            val uri = runCatching { Uri.parse(track.uri) }.getOrNull() ?: return null
            return openLocalStream(uri, range)
        }
    }

    private fun fileNameOf(uri: Uri): String? = context.getFileNameFromUri(uri)

    private fun localContentLength(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: return null).takeIf { it.isFile }?.length()
                "content" -> {
                    val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                    val length = afd?.use { it.length } ?: -1L
                    if (length > 0) length
                    else
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            var total = 0L
                            val skip = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(skip)
                                if (read <= 0) break
                                total += read
                            }
                            total
                        }
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.w("RelayServer probe $uri failed: ${e.message}")
            null
        }
    }

    private fun openLocalStream(uri: Uri, range: HttpRange?): InputStream? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val file = File(uri.path ?: return null)
                    if (!file.isFile) return null
                    val raf = RandomAccessFile(file, "r")
                    if (range != null) raf.seek(range.start)
                    // RandomAccessFile close: delegato al chiamante via InputStream.close
                    object : InputStream() {
                        override fun read(): Int = raf.read()
                        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
                        override fun close() { raf.close() }
                    }
                }

                "content" -> {
                    val input = context.contentResolver.openInputStream(uri) ?: return null
                    if (range != null && range.start > 0) skipFully(input, range.start)
                    input
                }

                else -> null
            }
        } catch (e: Exception) {
            Timber.w("RelayServer open $uri range=$range failed: ${e.message}")
            null
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                // skip() puo' restare a 0: consuma un byte alla volta
                if (input.read() < 0) return
                remaining -= 1
            }
        }
    }

    // ─────────────────── Token e rete ───────────────────

    private fun loadOrCreateToken(): String {
        val prefs = context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE)
        prefs.getString("token", null)?.let { return it }
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("token", token).apply()
        return token
    }

    companion object {

        /** Tag per i log diagnostici (Timber.tag): filtrabile in logcat anche in release. */
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
            Timber.tag(TAG).d(
                "interfacce LAN: siteLocal=%s other=%s",
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

/** MIME type dal nome file (per il Content-Type verso i renderer). */
object RelayMime {

    private val FALLBACK = "application/octet-stream"

    fun fromFileName(fileName: String?): String? {
        val extension = fileName
            ?.substringAfterLast('/', "")
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?: return null
        if (extension.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: fallbackFor(extension)
    }

    /** Estensioni audio comuni che MimeTypeMap a volte non conosce. */
    private fun fallbackFor(extension: String): String? = when (extension) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4", "aac" -> "audio/mp4"
        "flac" -> "audio/flac"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/ogg"
        "wav" -> "audio/wav"
        "m3u", "m3u8" -> "audio/x-mpegurl"
        else -> null
    }
}
