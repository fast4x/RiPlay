/*
 * riplay-bridge — Endpoint REST /api/v1.
 *
 * Tutti gli endpoint (tranne /ping) richiedono l'header
 *     Authorization: Bearer <token>
 * con lo stesso token del RelayServer (configurazione condivisa in androidApp).
 *
 * Endpooint montati DALLA APPLICAZIONE HOST: in produzione il RelayServer passa
 * bridgeModule() a embeddedServer tramite il parametro extraModules, quindi il
 * bridge vive NELLA STESSA applicazione Ktor del server di streaming (stessa
 * porta, stesso token). Per questo bridgeModule NON installa IgnoreTrailingSlash:
 * lo installa gia' relayEndpoints sull'applicazione e in Ktor 3 installare lo
 * stesso plugin due volte lancia DuplicatePluginException (BridgeServer, che
 * esegue il bridge da solo, lo installa esplicitamente nel suo blocco applicativo).
 *
 * Endpoint:
 * - GET /api/v1/ping                        pubblico, nessun dato sensibile
 * - GET /api/v1/health                      conteggi libreria
 * - GET /api/v1/library/artists?offset&limit
 * - GET /api/v1/library/albums?offset&limit
 * - GET /api/v1/library/tracks?offset&limit
 * - GET /api/v1/library/playlists?offset&limit
 * - GET /api/v1/artists/{id}/albums?offset&limit
 * - GET /api/v1/albums/{id}/tracks?offset&limit
 * - GET /api/v1/playlists/{id}/tracks?offset&limit
 * - GET /api/v1/tracks/{id}                 dettaglio + streamUrl calcolato per il client
 * - GET /api/v1/search?q&limit
 *
 * Integrazione consigliata (RelayServer di RiPlay, stesso processo/porta):
 * ```
 * // dentro RelayServer, dopo relayEndpoints(...):
 * extraModules = { token ->
 *     bridgeModule(catalog, resolver, BridgeConfig(token = token))
 * }
 * ```
 */
package it.fast4x.riplay.bridge

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import sun.security.util.KeyUtil.validate

/** Configurazione del bridge (token condiviso con il RelayServer). */
data class BridgeConfig(
    /** Token Bearer: lo stesso token del relay (/relay/<token>). */
    val token: String,
    val realm: String = "riplay-bridge",
    val defaultLimit: Int = 500,
    val maxLimit: Int = 1000,
    val bridgeVersion: String = "0.1.0",
)

/**
 * Modulo Ktor da montare dentro l'applicazione esistente del RelayServer
 * (parametro extraModules di RelayServer). Non installa IgnoreTrailingSlash:
 * lo installa gia' relayEndpoints nella stessa applicazione.
 *
 * ```
 * // RelayServer.kt (modulo relayserver), dentro embeddedServer(...):
 * relayEndpoints(token = token, registry = registry, source = effectiveSource)
 * extraModules?.invoke(this, token)   // <- qui entra bridgeModule
 * ```
 */
fun Application.bridgeModule(
    catalog: BridgeCatalog,
    resolver: StreamUrlResolver,
    config: BridgeConfig,
) {
    install(ContentNegotiation) { json(BridgeJson) }
    install(Authentication) {
        bearer("bridge") {
            realm = config.realm
            authenticate { credentials ->
                if (credentials.token == config.token) UserIdPrincipal("riplay") else null
            }
        }
    }

    routing {
        route("/api/v1") {
            get("/ping") {
                call.respond(BridgePing(bridgeVersion = config.bridgeVersion))
            }
            authenticate("bridge") {
                protectedRoutes(catalog, resolver, config)
            }
        }
    }
}

/** Tutte le route che leggono il catalogo (già dietro autenticazione Bearer). */
private fun Route.protectedRoutes(
    catalog: BridgeCatalog,
    resolver: StreamUrlResolver,
    config: BridgeConfig,
) {
    get("/health") {
        call.respondGuarded("health") {
            call.respond(catalog.stats())
        }
    }

    get("/library/artists") {
        call.respondGuarded("library/artists") {
            val offset = call.offsetOrZero(config)
            val page = catalog.artists(offset, call.limitOrDefault(config))
            call.respond(ArtistsPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/library/albums") {
        call.respondGuarded("library/albums") {
            val offset = call.offsetOrZero(config)
            val page = catalog.albums(offset, call.limitOrDefault(config))
            call.respond(AlbumsPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/library/tracks") {
        call.respondGuarded("library/tracks") {
            val offset = call.offsetOrZero(config)
            val page = catalog.tracks(offset, call.limitOrDefault(config))
            call.respond(TracksPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/library/playlists") {
        call.respondGuarded("library/playlists") {
            val offset = call.offsetOrZero(config)
            val page = catalog.playlists(offset, call.limitOrDefault(config))
            call.respond(PlaylistsPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/artists/{id}") {
        call.respondGuarded("artists/{id}") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id artista mancante")
            val artist = catalog.artist(id)
                ?: return@respondGuarded call.respondNotFound("Artista $id")
            call.respond(artist)
        }
    }

    get("/artists/{id}/albums") {
        call.respondGuarded("artists/{id}/albums") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id artista mancante")
            catalog.artist(id)
                ?: return@respondGuarded call.respondNotFound("Artista $id")
            val offset = call.offsetOrZero(config)
            val page = catalog.albumsOfArtist(id, offset, call.limitOrDefault(config))
            call.respond(AlbumsPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/albums/{id}") {
        call.respondGuarded("albums/{id}") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id album mancante")
            val album = catalog.album(id)
                ?: return@respondGuarded call.respondNotFound("Album $id")
            call.respond(album)
        }
    }

    get("/albums/{id}/tracks") {
        call.respondGuarded("albums/{id}/tracks") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id album mancante")
            catalog.album(id)
                ?: return@respondGuarded call.respondNotFound("Album $id")
            val offset = call.offsetOrZero(config)
            val page = catalog.tracksOfAlbum(id, offset, call.limitOrDefault(config))
            call.respond(TracksPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/playlists/{id}") {
        call.respondGuarded("playlists/{id}") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id playlist mancante")
            val playlist = catalog.playlist(id)
                ?: return@respondGuarded call.respondNotFound("Playlist $id")
            call.respond(playlist)
        }
    }

    get("/playlists/{id}/tracks") {
        call.respondGuarded("playlists/{id}/tracks") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id playlist mancante")
            val offset = call.offsetOrZero(config)
            val page = catalog.tracksOfPlaylist(id, offset, call.limitOrDefault(config))
            call.respond(TracksPage(page.items, page.total, page.items.size, offset))
        }
    }

    get("/tracks/{id}") {
        call.respondGuarded("tracks/{id}") {
            val id = call.parameters["id"]
                ?: return@respondGuarded call.respondBadRequest("id brano mancante")
            val track = catalog.track(id)
                ?: return@respondGuarded call.respondNotFound("Brano $id")
            // host:porta con cui IL CLIENT ha raggiunto il bridge: gli URL restano
            // corretti qualunque interfaccia di rete usi (stessa regola del playlist.m3u).
            val origin = call.request.origin
            val url = resolver.streamUrl(origin.serverHost, origin.serverPort, track)
            val kind = resolver.streamKind(origin.serverHost, origin.serverPort, track)
            call.respond(BridgeTrackDetail(track = track, streamUrl = url, streamKind = kind))
        }
    }

    get("/search") {
        call.respondGuarded("search") {
            val query = call.request.queryParameters["q"]?.trim().orEmpty()
            if (query.isEmpty()) {
                return@respondGuarded call.respondBadRequest("Parametro 'q' obbligatorio")
            }
            val limit = call.limitOrDefault(config)
            call.respond(catalog.search(query, limit))
        }
    }
}

// ─────────────────────────── helper ───────────────────────────

private fun ApplicationCall.limitOrDefault(config: BridgeConfig): Int =
    request.queryParameters["limit"]?.toIntOrNull()
        ?.coerceIn(1, config.maxLimit)
        ?: config.defaultLimit

private fun ApplicationCall.offsetOrZero(config: BridgeConfig): Int =
    request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

/** Risponde 404 con corpo JSON standard. */
private suspend fun ApplicationCall.respondNotFound(what: String) =
    respond(HttpStatusCode.NotFound, BridgeError("not_found", "$what non trovato"))

/** Risponde 400 con corpo JSON standard. */
private suspend fun ApplicationCall.respondBadRequest(message: String) =
    respond(HttpStatusCode.BadRequest, BridgeError("bad_request", message))

/**
 * Esegue [block] e converte le eccezioni in risposta 500 JSON:
 * un errore del catalogo non deve mai arrivare al client come stack trace.
 */
private suspend fun ApplicationCall.respondGuarded(
    tag: String,
    block: suspend () -> Unit,
) {
    try {
        block()
    } catch (e: Exception) {
        application.log.error("$tag: ${e.message}", e)
        respond(
            HttpStatusCode.InternalServerError,
            BridgeError("internal_error", e.message ?: e.javaClass.simpleName),
        )
    }
}
