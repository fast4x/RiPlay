package it.fast4x.riplay.server

import java.security.MessageDigest

/**
 * RiPlay Relay — registro dei brani servibili dal server di streaming locale (Fase 2).
 *
 * Puro Kotlin, senza dipendenze Android: testabile su JVM.
 *
 * Due strutture:
 * - whitelist tracce (id -> RelayTrack): solo i brani registrati sono servibili
 *   dall'endpoint /s/{id}: il server non esposto mai un accesso arbitrario ai file
 * - ordine coda corrente: alimenta la playlist M3U per i player esterni
 *
 * Semantica:
 * - [register] aggiunge/aggiorna una traccia (usato dal cast UPnP per il brano da inviare)
 * - [syncQueue] ricostruisce l'ordine coda registrando anche i brani nuovi (usato da
 *   PlayerService ad ogni mutazione della coda): NON svuota la whitelist, cosi' il brano
 *   in cast resta servibile anche se fuori coda
 */
class RelayRegistry {

    data class Candidate(
        val uri: String,
        val title: String? = null,
        val artist: String? = null,
        val mime: String? = null,
    )

    private val lock = Any()
    private val tracks = LinkedHashMap<String, RelayTrack>()
    private var queueOrder: List<String> = emptyList()

    /** Cap per la whitelist: i piu' vecchi (per inserimento) vengono evitati. */
    var maxTracks: Int = 512

    /** Registra (o aggiorna) un brano e ne restituisce la traccia con id stabile. */
    fun register(candidate: Candidate): RelayTrack {
        val track = RelayTrack(
            id = RelayUris.streamId(candidate.uri),
            uri = candidate.uri,
            title = candidate.title,
            artist = candidate.artist,
            mime = candidate.mime,
        )
        synchronized(lock) {
            tracks[track.id] = track
            if (track.id !in queueOrder) queueOrder = queueOrder + track.id
            evictOldestIfNeeded()
        }
        return track
    }

    /**
     * Ricostruisce la coda corrente: registra i brani mancanti e reimposta l'ordine.
     * Le tracce in whitelist restano servibili (il cast in corso non si interrompe).
     */
    fun syncQueue(candidates: List<Candidate>) {
        synchronized(lock) {
            candidates.forEach { candidate ->
                val id = RelayUris.streamId(candidate.uri)
                tracks[id] = RelayTrack(id, candidate.uri, candidate.title, candidate.artist, candidate.mime)
            }
            queueOrder = candidates.map { RelayUris.streamId(it.uri) }.distinct()
            evictOldestIfNeeded()
        }
    }

    /** Traccia servibile dall'id, o null se non in whitelist. */
    fun track(id: String): RelayTrack? = synchronized(lock) { tracks[id] }

    /** Snapshot della coda corrente in ordine (per la playlist M3U). */
    fun queue(): List<RelayTrack> = synchronized(lock) {
        queueOrder.mapNotNull { tracks[it] }
    }

    fun size(): Int = synchronized(lock) { tracks.size }

    private fun evictOldestIfNeeded() {
        while (tracks.size > maxTracks) {
            val oldest = tracks.keys.firstOrNull() ?: break
            tracks.remove(oldest)
        }
    }
}

/**
 * Una traccia servibile dal relay. L'id e' stabile (dipende solo dalla URI):
 * i link M3U restano validi tra riavvii dell'app.
 */
data class RelayTrack(
    val id: String,
    val uri: String,
    val title: String? = null,
    val artist: String? = null,
    val mime: String? = null,
)

/**
 * Helper sulle URI servibili dal relay.
 *
 * Regola del cast UPnP:
 * - http(s) diretto -> il dispositivo preleva lo stream dalla rete, il telefono
 *   non e' nel percorso audio (Fase 1)
 * - file/content    -> il dispositivo preleva dal server di streaming locale (Fase 2)
 * - altri casi (es. videoId YouTube senza scheme) -> nessun URL diretto disponibile,
 *   gestione sperimentale rimandata alla Fase 3
 */
object RelayUris {

    fun isDirect(uri: String): Boolean =
        uri.startsWith("http://") || uri.startsWith("https://")

    fun isLocal(uri: String): Boolean {
        val scheme = uri.substringBefore(":", "").lowercase()
        return scheme == "file" || scheme == "content"
    }

    /** Id stabile della traccia: sha-256 della URI, troncato a 20 caratteri esadecimali. */
    fun streamId(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }

    /** URL assoluto dello stream per una traccia (baseUrl = http://ip:porta/relay/token). */
    fun streamUrl(baseUrl: String, track: RelayTrack): String = "$baseUrl/s/${track.id}"
}

/**
 * Generatore playlist M3U per i player esterni (VLC, browser, altri RiPlay).
 *
 * - brani con URI http(s): viene inserito l'URL originale (il telefono non e' nel percorso)
 * - brani locali: URL del relay (il telefono serve il file)
 * - brani senza URI servibile: esclusi dalla playlist
 */
object RelayM3u {

    fun build(
        playlistName: String,
        tracks: List<RelayTrack>,
        urlFor: (RelayTrack) -> String?,
    ): String = buildString {
        appendLine("#EXTM3U")
        appendLine("#PLAYLIST:$playlistName")
        tracks.forEach { track ->
            val url = urlFor(track) ?: return@forEach
            val label = buildString {
                if (!track.artist.isNullOrBlank() && !track.title.isNullOrBlank())
                    append("${track.artist} - ${track.title}")
                else
                    append(track.title?.takeIf { it.isNotBlank() } ?: track.artist ?: track.id)
            }
            appendLine("#EXTINF:-1,$label")
            appendLine(url)
        }
    }
}

/**
 * Parsing dell'header HTTP Range (RFC 7233, subset bytes=) per il seek sui renderer.
 * Formati supportati: "bytes=start-end", "bytes=start-", "bytes=-suffix".
 * Un range non soddisfacibile ritorna null con [unsatisfiable] = true (risposta 416).
 */
data class HttpRange(
    val start: Long,
    val endInclusive: Long,
    val total: Long,
) {
    val length: Long get() = endInclusive - start + 1

    /** Content-Range RFC: "bytes start-end/total". */
    val contentRange: String get() = "bytes $start-$endInclusive/$total"
}

object HttpRanges {

    /**
     * @param header valore raw dell'header Range (null/assente = contenuto integrale)
     * @param total dimensione totale della risorsa; -1 se sconosciuta
     * @return range normalizzato, o null se l'header e' assente/invalido e serve tutto
     */
    fun parse(header: String?, total: Long): HttpRange? {
        if (header.isNullOrBlank() || total <= 0) return null
        val spec = header.trim().removePrefix("bytes=").trim()
        if (!spec.contains('-')) return null

        val startPart = spec.substringBefore('-').trim()
        val endPart = spec.substringAfter('-').trim()

        // suffix form: "bytes=-N" -> ultimi N byte
        if (startPart.isEmpty()) {
            val suffix = endPart.toLongOrNull() ?: return null
            if (suffix <= 0) return null
            if (suffix >= total) return HttpRange(0, total - 1, total)
            return HttpRange(total - suffix, total - 1, total)
        }

        val start = startPart.toLongOrNull() ?: return null
        if (start >= total) {
            // range oltre fine risorsa: 416, gestito dal chiamante tramite unsatisfiable()
            return null
        }
        val end = if (endPart.isEmpty()) total - 1 else minOf(endPart.toLongOrNull() ?: return null, total - 1)
        if (end < start) return null
        return HttpRange(start, end, total)
    }
}
