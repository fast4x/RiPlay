/*
 * riplay-bridge — Interfacce di dominio: catalogo libreria e risoluzione stream.
 *
 * Il bridge NON conosce Room né il RelayServer: chi integra (androidApp) fornisce
 * l'implementazione del [BridgeCatalog] leggendo il database reale e un
 * [StreamUrlResolver] che registra i brani nel RelayRegistry e costruisce gli
 * URL del relay. Così il modulo è testabile su JVM puro (vedi BridgeE2eTest).
 */
package it.fast4x.riplay.bridge

/** Pagina generica restituita dalle query del catalogo (uso interno, mai serializzata). */
data class CatalogPage<T>(
    val items: List<T>,
    val total: Int,
)

/** Sorgente dati della libreria RiPlay. Tutti i metodi sono sospensivi (DB async). */
interface BridgeCatalog {

    /** Artisti in libreria, ordinati per nome. */
    suspend fun artists(offset: Int, limit: Int): CatalogPage<BridgeArtist>

    /** Album in libreria, ordinati per titolo. */
    suspend fun albums(offset: Int, limit: Int): CatalogPage<BridgeAlbum>

    /** Brani in libreria, ordinati per titolo. */
    suspend fun tracks(offset: Int, limit: Int): CatalogPage<BridgeTrack>

    /** Playlist dell'utente. */
    suspend fun playlists(offset: Int, limit: Int): CatalogPage<BridgePlaylist>

    /** Singolo artista, o null se assente. */
    suspend fun artist(id: String): BridgeArtist?

    /** Album di un artista, ordinati per anno. */
    suspend fun albumsOfArtist(artistId: String, offset: Int, limit: Int): CatalogPage<BridgeAlbum>

    /** Singolo album, o null se assente. */
    suspend fun album(id: String): BridgeAlbum?

    /** Brani di un album, ordinati per disco/traccia. */
    suspend fun tracksOfAlbum(albumId: String, offset: Int, limit: Int): CatalogPage<BridgeTrack>

    /** Singola playlist, o null se assente. */
    suspend fun playlist(id: String): BridgePlaylist?

    /** Brani di una playlist, in ordine di inserimento. */
    suspend fun tracksOfPlaylist(playlistId: String, offset: Int, limit: Int): CatalogPage<BridgeTrack>

    /** Singolo brano, o null se assente. */
    suspend fun track(id: String): BridgeTrack?

    /** Ricerca full-text (titolo brano, artista, album, playlist). */
    suspend fun search(query: String, limit: Int): BridgeSearchResult

    /** Conteggi per /health. */
    suspend fun stats(): BridgeHealth
}

/**
 * Strategia di risoluzione dello stream per un brano.
 *
 * Implementazione di riferimento nel glue di androidApp (RelayStreamUrlResolver):
 * registra il brano nel RelayRegistry e ritorna l'URL
 * `http://<host>:<porta>/relay/<token>/s/<sha256(uri)[:20]>`, dove host:porta
 * sono quelli con cui il CLIENT ha raggiunto il bridge (come fa playlist.m3u).
 */
interface StreamUrlResolver {

    /**
     * @param host host con cui il client ha raggiunto il bridge (request origin)
     * @param port porta con cui il client ha raggiunto il bridge
     * @param track il brano richiesto
     * @return URL assoluto dello stream, o null se il brano non è servibile
     */
    fun streamUrl(host: String, port: Int, track: BridgeTrack): String?

    /** Tipo di stream prodotto ("direct" | "relay" | null). */
    fun streamKind(host: String, port: Int, track: BridgeTrack): String?
}

/**
 * Resolver di default: eroga solo brani con URL http(s) diretto.
 * Per i file locali serve l'implementazione del relay (glue androidApp).
 */
object DirectUrlResolver : StreamUrlResolver {

    override fun streamUrl(host: String, port: Int, track: BridgeTrack): String? =
        track.directUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    override fun streamKind(host: String, port: Int, track: BridgeTrack): String? =
        if (streamUrl(host, port, track) != null) "direct" else null
}
