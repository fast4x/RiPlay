package it.fast4x.riplay.server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * RiPlay Relay — wiring DESKTOP del modulo server condiviso (relayserver).
 *
 * Il desktop riceve lo STESSO server di streaming dell'app Android: playlist
 * M3U della coda corrente, singoli brani con seek (Range HTTP), endpoint info,
 * tutto protetto da token e raggiungibile solo in LAN. Casi d'uso: ascoltare
 * la libreria/coda desktop da VLC, browser o da un RiPlay su telefono.
 *
 * Qui restano SOLE le parti che dipendono dalla piattaforma, iniettate nel
 * modulo all'avvio del processo (bootstrapDesktopApp in main.kt):
 *
 * - [DesktopRelayStreamSource]: byte dei brani da percorsi file assoluti e
 *   URI file:// (RandomAccessFile, seek esatto per il Range HTTP)
 * - [DesktopRelayTokenStore]: token persistente in ~/.riplay/relay.properties
 *   (chiave "token"): i link M3U restano stabili tra riavvii
 * - [DesktopRelaySettings]: interruttore delle impostazioni (stesso file,
 *   chiave "relay.enabled") + avvio/stop immediato
 * - [installDesktopRelayServer]: registro del provider in RelayManager
 *
 * Le canzoni LOCALI (id "local:<percorso>", mediaId = percorso assoluto) sono
 * servite dal relay; i brani WebDAV (id "webdav:<url>") passano per l'URL
 * diretto; i videoId YouTube restano in whitelist ma esclusi dalla M3U
 * (comportamento identico al Fase 2 Android).
 */

/**
 * Installa il wiring desktop nel modulo condiviso. Chiamare UNA SOLA VOLTA,
 * in bootstrapDesktopApp (main.kt), PRIMA di [RelayManager.startIfEnabled].
 * Idempotente: chiamate successive sovrascrivono il provider senza effetti.
 */
fun installDesktopRelayServer() {
    RelayManager.install(RelayServerProvider {
        RelayServer(
            registry = RelayManager.registry,
            source = DesktopRelayStreamSource,
            tokenStore = DesktopRelayTokenStore,
        )
    })
    println("[RiPlay-Relay] wiring desktop del server di streaming installato")
}

/**
 * Interruttore del server di streaming (Impostazioni -> Generale) con
 * persistenza immediata su ~/.riplay/relay.properties (stesso pattern di
 * DesktopCloseToTray/DesktopMiniPlayer: lo state Compose si ricompone da
 * solo e il valore e' gia' salvato quando l'utente chiude la pagina).
 * Default: OFF (come il setting Android streamRelayEnabled).
 */
object DesktopRelaySettings {

    private const val KEY_ENABLED = "relay.enabled"

    /** Stato osservabile dalla UI (tab Generale delle impostazioni). */
    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    /**
     * Cambia l'interruttore, persiste SUBITO e applica all'istante
     * (avvio o stop del server, come fa il setting Cast su Android).
     * Nome con suffisso (convenzione del progetto setEnabledXxx, es.
     * DesktopCloseToTray.setEnabledCloseToTray): evita il clash JVM con il
     * setter privato della property enabled.
     */
    fun setDesktopRelayEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        persistEnabled(value)
        RelayManager.startIfEnabled(value)
    }

    private fun readEnabled(): Boolean = try {
        val p = java.util.Properties()
        val f = relayPrefsFile
        if (f.isFile) f.inputStream().use { p.load(it) }
        p.getProperty(KEY_ENABLED, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    private fun persistEnabled(value: Boolean) {
        try {
            val p = java.util.Properties()
            val f = relayPrefsFile
            if (f.isFile) f.inputStream().use { p.load(it) }
            p.setProperty(KEY_ENABLED, value.toString())
            f.parentFile?.mkdirs()
            f.outputStream().use { p.store(it, "RiPlay desktop streaming relay") }
        } catch (t: Throwable) {
            // silenzioso: la persistenza non deve mai impedire l'uso dell'app
        }
    }
}

/** File delle preferenze del relay (~/.riplay/relay.properties): token + toggle. */
private val relayPrefsFile: File
    get() = File(System.getProperty("user.home"), ".riplay").resolve("relay.properties")

/** Persistenza del token su file Properties (chiave "token"). */
private object DesktopRelayTokenStore : RelayTokenStore {

    override fun load(): String? = try {
        val p = java.util.Properties()
        val f = relayPrefsFile
        if (f.isFile) f.inputStream().use { p.load(it) }
        p.getProperty("token")?.takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
        null
    }

    override fun save(token: String) {
        try {
            val p = java.util.Properties()
            val f = relayPrefsFile
            if (f.isFile) f.inputStream().use { p.load(it) }
            p.setProperty("token", token)
            f.parentFile?.mkdirs()
            f.outputStream().use { p.store(it, "RiPlay desktop streaming relay") }
        } catch (t: Throwable) {
            // silenzioso: senza persistenza il token cambia a ogni riavvio
        }
    }
}

/**
 * Sorgente dei byte desktop: percorsi assoluti (POSIX e Windows, la forma con
 * cui la pagina Locali registra le canzoni: mediaId = file.absolutePath) e
 * URI file://. Altri schemi (content:// di un backup Android, videoId, http
 * diretto): null -> 404 (per http il client e' comunque servito dall'URL
 * originale, il relay non e' nel percorso).
 */
private object DesktopRelayStreamSource : RelayStreamSource {

    override fun probe(track: RelayTrack): RelayMeta? {
        val file = fileOf(track.uri) ?: return null
        if (!file.isFile) return null
        val size = file.length()
        if (size <= 0) return null
        return RelayMeta(totalSize = size, mime = track.mime ?: RelayMime.fromFileName(file.name))
    }

    override fun open(track: RelayTrack, range: HttpRange?): InputStream? {
        val file = fileOf(track.uri) ?: return null
        if (!file.isFile) return null
        val raf = RandomAccessFile(file, "r")
        if (range != null) raf.seek(range.start)
        // RandomAccessFile close: delegato al chiamante via InputStream.close
        // (l'endpoint limita comunque la lettura alla lunghezza del range)
        return object : InputStream() {
            override fun read(): Int = raf.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
            override fun close() { raf.close() }
        }
    }

    private fun fileOf(uri: String): File? = when {
        uri.startsWith("file://", ignoreCase = true) -> runCatching {
            File(java.net.URI(uri).path)
        }.getOrElse {
            // URI non correttamente encodate (spazi nel percorso): estrazione diretta
            File(uri.substringAfter("://"))
        }

        uri.startsWith("http://") || uri.startsWith("https://") ||
                uri.startsWith("content://") -> null

        else -> File(uri)
    }
}

/**
 * Converte una [Song] del desktop in un candidato del registro del relay.
 *
 * - brano WebDAV (id/mediaId "webdav:<url>"): URI http diretto (isDirect ->
 *   l'M3U inserisce l'URL originale, come su Android)
 * - brano locale (id "local:<percorso>" o mediaId percorso assoluto): URI del
 *   percorso file (isLocal -> l'M3U inserisce il link relay)
 * - brano online (videoId): resta in whitelist ma ESCLUSO dalla playlist M3U
 *   (gestione sperimentale "Fase 3", identica ad Android)
 *
 * null se l'URI risultante e' vuoto (niente da registrare).
 */
fun desktopRelayCandidate(song: Song): RelayRegistry.Candidate? {
    val raw = (song.mediaId ?: song.id).trim()
    val uri = when {
        raw.startsWith(WEBDAV_KEY_PREFIX) -> raw.removePrefix(WEBDAV_KEY_PREFIX).trim()
        raw.startsWith(LOCAL_KEY_PREFIX) -> raw.removePrefix(LOCAL_KEY_PREFIX).trim()
        else -> raw
    }
    if (uri.isBlank()) return null
    return RelayRegistry.Candidate(
        uri = uri,
        title = song.title,
        artist = song.artistsText,
        mime = null, // il mime e' risolto dal server al momento della richiesta
    )
}

/**
 * Sincronizza nel registro del relay la coda corrente del desktop (stesso
 * ruolo di PlayerService.syncQueueToRelay su Android): il brano in
 * riproduzione (che non e' piu' in coda) + i brani in attesa. La whitelist
 * esistente NON viene svuotata: i link M3U gia' copiati restano validi finche'
 * i brani restano dentro il cap di [RelayRegistry.maxTracks].
 */
fun syncDesktopRelayQueue(playing: Song?, queued: List<Song>) {
    val candidates = buildList {
        playing?.let { desktopRelayCandidate(it)?.let(::add) }
        queued.forEach { desktopRelayCandidate(it)?.let(::add) }
    }
    RelayManager.registry.syncQueue(candidates)
}
