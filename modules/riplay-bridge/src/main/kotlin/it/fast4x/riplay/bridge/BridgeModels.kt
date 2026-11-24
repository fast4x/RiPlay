/*
 * riplay-bridge — Modelli DTO dell'API /api/v1 esposta verso Music Assistant.
 *
 * Puro Kotlin + kotlinx.serialization: nessuna dipendenza Android.
 * Le classi Page sono concrete (non generiche) perché kotlinx.serialization
 * non può ricostruire il tipo generico a runtime in ContentNegotiation.
 */
package it.fast4x.riplay.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Configurazione JSON condivisa da server (e riutilizzabile dai client). */
val BridgeJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}

// ─────────────────────────── Oggetti base ───────────────────────────

/** Artista della libreria RiPlay. */
@Serializable
data class BridgeArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
)

/** Album della libreria RiPlay. */
@Serializable
data class BridgeAlbum(
    val id: String,
    val name: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val year: Int? = null,
    val trackCount: Int = 0,
    val thumbnailUrl: String? = null,
)

/**
 * Brano della libreria RiPlay.
 *
 * [servable] indica se il brano può essere erogato in streaming dal dispositivo
 * (file locali, Music Vault, URL http diretti). I brani solo-YouTube (videoId
 * senza URI diretto) restano visibili nella libreria ma NON servibili, coerenti
 * con la regola del RelayServer.
 */
@Serializable
data class BridgeTrack(
    val id: String,
    val name: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val durationSeconds: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val year: Int? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val thumbnailUrl: String? = null,
    /** URL http(s) diretto alla sorgente, se esiste (il telefono resta fuori dal percorso audio). */
    val directUrl: String? = null,
    val servable: Boolean = false,
)

/** Playlist della libreria RiPlay. */
@Serializable
data class BridgePlaylist(
    val id: String,
    val name: String,
    val trackCount: Int = 0,
    val thumbnailUrl: String? = null,
)

// ─────────────────────────── Pagine (concrete) ───────────────────────────

@Serializable
data class ArtistsPage(
    val items: List<BridgeArtist> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)

@Serializable
data class AlbumsPage(
    val items: List<BridgeAlbum> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)

@Serializable
data class TracksPage(
    val items: List<BridgeTrack> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)

@Serializable
data class PlaylistsPage(
    val items: List<BridgePlaylist> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)

// ─────────────────────────── Risposte composite ───────────────────────────

/**
 * Dettaglio brano: la risposta usata da Music Assistant per risolvere lo stream.
 *
 * [streamUrl] è l'URL assoluto che il client (il server Music Assistant) userà
 * per scaricare l'audio. Viene calcolato a runtime dal resolver iniettato:
 * - URL http(s) diretto: restituito così com'è (il telefono non è nel percorso);
 * - file locale / Music Vault: URL del RelayServer esistente
 *   `http://<host>:<porta>/relay/<token>/s/<id>` (il token è quello del relay,
 *   l'host:porta sono quelli con cui il client ha raggiunto il bridge).
 */
@Serializable
data class BridgeTrackDetail(
    val track: BridgeTrack,
    val streamUrl: String? = null,
    /** "direct" = URL diretto alla sorgente; "relay" = streaming dal dispositivo; null = non servibile. */
    val streamKind: String? = null,
)

/** Risultato di ricerca multi-tipo. */
@Serializable
data class BridgeSearchResult(
    val artists: List<BridgeArtist> = emptyList(),
    val albums: List<BridgeAlbum> = emptyList(),
    val tracks: List<BridgeTrack> = emptyList(),
    val playlists: List<BridgePlaylist> = emptyList(),
)

/** Stato di salute del bridge (autenticato). */
@Serializable
data class BridgeHealth(
    val app: String = "RiPlay",
    val bridgeVersion: String = "0.1.0",
    val artistCount: Int = 0,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val playlistCount: Int = 0,
    val servableTrackCount: Int = 0,
)

/** Ping pubblico (nessun dato sensibile), per il "test connessione" del provider. */
@Serializable
data class BridgePing(
    val ok: Boolean = true,
    val app: String = "RiPlay",
    val bridgeVersion: String = "0.1.0",
)

/** Errore API in formato JSON. */
@Serializable
data class BridgeError(
    val error: String,
    val message: String? = null,
)
