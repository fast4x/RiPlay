package it.fast4x.riplay.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import java.io.InputStream
import timber.log.Timber

/**
 * RiPlay Relay — endpoint HTTP puro Ktor (Fase 2), senza dipendenze Android.
 *
 * Il serving dei byte e' delegato a [RelayStreamSource]: nell'app l'implementazione
 * legge da file locali/content:// (RelayServer), nei test JVM da un buffer.
 * Cosi' routing, token, Range e M3U sono testabili E2E su JVM con il codice reale.
 *
 * r2: OGNI richiesta viene loggata (tag "Relay"): prima una connessione VLC
 * riuscita non lasciava NESSUNA traccia nel log e sembrava un "server morto".
 *
 * r3: dai test reali (bug #2) e' emerso il caso peggiore: l'URL BASE
 * `/relay/<token>` (quello mostrato/copiato per primo nella UI) non matchava
 * NESSUNA route: Ktor rispondeva 404 di default senza che nessun handler
 * loggasse, quindi VLC/browser fallivano e il log restava mutu. Due mitigazioni:
 * 1. logger a livello ENGINE: ogni richiesta ricevuta (anche fuori routing) ora
 *    viene loggata con metodo, path, client e status -> un 404 silenzioso non
 *    e' piu' possibile;
 * 2. l'URL base e' VIVO: 302 redirect a /playlist.m3u, cosi' il link copiato
 *    dalla UI funziona anche in VLC (che segue i redirect) e nel browser.
 *    Con IgnoreTrailingSlash la versione con slash finale (/relay/<token>/)
 *    si comporta allo stesso modo.
 *
 * Endpoints (tutti sotto /relay/<token>/, token nel path; token errato = 404,
 * senza indizi per chi prova a sondare la porta):
 * - GET  /relay/<token>              redirect 302 -> playlist.m3u (r3)
 * - GET  /relay/<token>/s/{id}       singolo brano (Range supportato per il seek)
 * - HEAD /relay/<token>/s/{id}       metadati (dimensione, tipo, accept-ranges)
 * - GET  /relay/<token>/playlist.m3u coda corrente per player esterni (VLC, browser...)
 * - GET  /relay/<token>/info         stato JSON (now playing, dimensione coda)
 */

private const val RELAY_LOG_TAG = "Relay"

/** Metadati statici di una traccia servibile. */
data class RelayMeta(
    val totalSize: Long,
    val mime: String?,
)

/**
 * Sorgente dei byte per l'endpoint /s/{id}.
 * Implementazioni: file locali (app), buffer (test).
 */
interface RelayStreamSource {

    /** Metadati o null se la traccia non e' servibile (risposta 404). */
    fun probe(track: RelayTrack): RelayMeta?

    /**
     * Apre il flusso per il range richiesto (null = contenuto integrale).
     * Il chiamante chiude l'InputStream a fine risposta.
     */
    fun open(track: RelayTrack, range: HttpRange?): InputStream?
}

fun Application.relayEndpoints(
    token: String,
    registry: RelayRegistry,
    source: RelayStreamSource,
) {
    // r3: logger a livello engine, ATTIVO ANCHE per le richieste fuori routing
    // (es. l'URL base senza endpoint, path inesistenti, token mancante). Prima
    // una 404 di routing era completamente invisibile nel log.
    install(createApplicationPlugin(name = "RelayRequestLogger") {
        on(ResponseSent) { call ->
            Timber.tag(RELAY_LOG_TAG).i(
                "<-%s %s da %s -> %s",
                call.request.origin.method.value,
                call.request.origin.uri,
                call.request.origin.remoteHost,
                call.response.status()?.value ?: "-"
            )
        }
        on(CallFailed) { call, cause ->
            Timber.tag(RELAY_LOG_TAG).e(
                cause,
                "XX %s %s da %s FALLITA: %s",
                call.request.origin.method.value,
                call.request.origin.uri,
                call.request.origin.remoteHost,
                cause.message ?: cause.javaClass.simpleName
            )
        }
    })

    // r3: l'URL base con slash finale (/relay/<token>/) deve comportarsi come
    // quello senza: Ktor 3 di default distingue i due path.
    install(IgnoreTrailingSlash)

    routing {
        route("/relay/{token}") {

            fun Parameters.validToken(): Boolean = get("token") == token

            // ─────────────────── URL base (r3) ───────────────────

            // L'URL base /relay/<token> SENZA endpoint non matchava nessuna route:
            // 404 silenzioso, VLC/browser fallivano e il log restava vuoto (bug #2).
            // Ora: redirect alla playlist della coda corrente — VLC segue i redirect,
            // quindi anche il solo link base avvia la coda; nel browser scarica l'm3u.
            get {
                if (!call.parameters.validToken()) {
                    Timber.tag(RELAY_LOG_TAG).w("GET /relay token ERRATO (404)")
                    return@get call.respondText("", status = HttpStatusCode.NotFound)
                }
                // Host/porta con cui il client ci ha raggiunto (coerente con playlist.m3u):
                // il link redirect funziona anche se il server e' raggiunto via IP diverso.
                val origin = call.request.origin
                val target = "http://${origin.serverHost}:${origin.serverPort}/relay/$token/playlist.m3u"
                Timber.tag(RELAY_LOG_TAG).i("GET /relay da %s -> 302 %s", origin.remoteHost, target)
                call.respondRedirect(target)
            }

            // ─────────────────── singolo brano ───────────────────

            get("/s/{id}") {
                if (!call.parameters.validToken()) {
                    Timber.tag(RELAY_LOG_TAG).w("GET /s/%s token ERRATO (404)", call.parameters["id"])
                    return@get call.respondText("", status = HttpStatusCode.NotFound)
                }
                serveTrack(call, registry, source)
            }

            head("/s/{id}") {
                if (!call.parameters.validToken()) {
                    Timber.tag(RELAY_LOG_TAG).w("HEAD /s/%s token ERRATO (404)", call.parameters["id"])
                    return@head call.respondText("", status = HttpStatusCode.NotFound)
                }
                val id = call.parameters["id"] ?: return@head call.respondText("", status = HttpStatusCode.NotFound)
                val track = registry.track(id) ?: run {
                    Timber.tag(RELAY_LOG_TAG).w("HEAD /s/%s traccia non in whitelist (404)", id)
                    return@head call.respondText("", status = HttpStatusCode.NotFound)
                }
                val meta = source.probe(track) ?: run {
                    Timber.tag(RELAY_LOG_TAG).w("HEAD /s/%s sorgente non leggibile (404): %s", id, track.uri)
                    return@head call.respondText("", status = HttpStatusCode.NotFound)
                }
                Timber.tag(RELAY_LOG_TAG).i("HEAD /s/%s -> 200 size=%s mime=%s", id, meta.totalSize, meta.mime)
                call.respond(object : OutgoingContent.NoContent() {
                    override val contentLength: Long? = if (meta.totalSize > 0) meta.totalSize else null
                    override val contentType: ContentType? = meta.mime?.takeIf { it.isNotBlank() }?.let { ContentType.parse(it) }
                    override val headers: Headers = Headers.build {
                        if (meta.totalSize > 0) append(HttpHeaders.AcceptRanges, "bytes")
                    }
                })
            }

            // ─────────────────── playlist coda corrente ───────────────────

            get("/playlist.m3u") {
                if (!call.parameters.validToken()) {
                    Timber.tag(RELAY_LOG_TAG).w("GET playlist.m3u token ERRATO (404)")
                    return@get call.respondText("", status = HttpStatusCode.NotFound)
                }

                // L'indirizzo del telefono e' quello con cui il client ha raggiunto il
                // server: i link generati funzionano sempre, indipendentemente da quale
                // interfaccia di rete il client ha usato per connettersi.
                val origin = call.request.origin
                val base = "http://${origin.serverHost}:${origin.serverPort}/relay/$token"

                val m3u = RelayM3u.build(
                    playlistName = "RiPlay",
                    tracks = registry.queue(),
                ) { track ->
                    when {
                        RelayUris.isDirect(track.uri) -> track.uri
                        RelayUris.isLocal(track.uri) -> RelayUris.streamUrl(base, track)
                        else -> null // videoId e altri formati non servibili (Fase 3)
                    }
                }
                Timber.tag(RELAY_LOG_TAG).i(
                    "GET playlist.m3u da %s -> %d brani in coda",
                    origin.remoteHost, registry.queue().size
                )
                call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                call.respondText(m3u, ContentType.parse("audio/x-mpegurl"))
            }

            // ─────────────────── stato JSON ───────────────────

            get("/info") {
                if (!call.parameters.validToken()) {
                    Timber.tag(RELAY_LOG_TAG).w("GET /info token ERRATO (404)")
                    return@get call.respondText("", status = HttpStatusCode.NotFound)
                }
                val queue = registry.queue()
                val nowPlaying = queue.firstOrNull()
                Timber.tag(RELAY_LOG_TAG).d("GET info da %s (coda=%d)", call.request.origin.remoteHost, queue.size)
                call.respondText(
                    buildString {
                        append("{")
                        append("\"app\":\"RiPlay\"")
                        append(",\"title\":").append(jsonString(nowPlaying?.title))
                        append(",\"artist\":").append(jsonString(nowPlaying?.artist))
                        append(",\"queueSize\":").append(queue.size)
                        append("}")
                    },
                    ContentType.Application.Json,
                )
            }
        }
    }
}

/** Serving di un brano con supporto Range (seek) su /s/{id}. */
private suspend fun serveTrack(call: ApplicationCall, registry: RelayRegistry, source: RelayStreamSource) {
    val id = call.parameters["id"] ?: return call.respondText("", status = HttpStatusCode.NotFound)
    val track = registry.track(id)
    if (track == null) {
        Timber.tag(RELAY_LOG_TAG).w("GET /s/%s traccia non in whitelist (404)", id)
        return call.respondText("", status = HttpStatusCode.NotFound)
    }
    val meta = source.probe(track)
    if (meta == null) {
        Timber.tag(RELAY_LOG_TAG).w("GET /s/%s sorgente non leggibile (404): %s", id, track.uri)
        return call.respondText("", status = HttpStatusCode.NotFound)
    }

    val rangeHeader = call.request.headers[HttpHeaders.Range]
    val range = HttpRanges.parse(rangeHeader, meta.totalSize)

    // Header Range presente ma oltre fine risorsa: 416 (RFC 7233)
    if (rangeHeader != null && range == null && meta.totalSize > 0) {
        Timber.tag(RELAY_LOG_TAG).w("GET /s/%s range oltre fine (416): %s size=%d", id, rangeHeader, meta.totalSize)
        call.response.headers.append(HttpHeaders.ContentRange, "bytes */${meta.totalSize}")
        return call.respondText("", status = HttpStatusCode.RequestedRangeNotSatisfiable)
    }

    val input = source.open(track, range)
    if (input == null) {
        Timber.tag(RELAY_LOG_TAG).w("GET /s/%s apertura stream fallita (404): %s", id, track.uri)
        return call.respondText("", status = HttpStatusCode.NotFound)
    }

    try {
        val status = if (range != null) HttpStatusCode.PartialContent else HttpStatusCode.OK
        Timber.tag(RELAY_LOG_TAG).i(
            "GET /s/%s da %s -> %s range=%s mime=%s",
            id,
            call.request.origin.remoteHost,
            status.value,
            rangeHeader ?: "-",
            meta.mime ?: "-"
        )
        if (range != null) call.response.headers.append(HttpHeaders.ContentRange, range.contentRange)
        if (meta.totalSize > 0) call.response.headers.append(HttpHeaders.AcceptRanges, "bytes")
        val mime = meta.mime?.takeIf { it.isNotBlank() } ?: FALLBACK_MIME
        call.response.headers.append(HttpHeaders.ContentType, mime)
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")

        val contentLength = range?.length ?: if (meta.totalSize > 0) meta.totalSize else null

        call.respondBytesWriter(status = status, contentLength = contentLength) {
            pump(input, this)
        }
    } finally {
        runCatching { input.close() }
    }
}

/** Copia il flusso nella risposta, con flush progressivo per gli stream live. */
private suspend fun pump(input: InputStream, writer: ByteWriteChannel) {
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        writer.writeFully(buffer, 0, read)
        writer.flush()
    }
}

/** Stringa JSON con escape minimale (virgolette, backslash, caratteri di controllo). */
internal fun jsonString(value: String?): String {
    if (value == null) return "null"
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    return "\"$escaped\""
}

internal const val FALLBACK_MIME = "application/octet-stream"
