/*
 * RiPlay + Music Assistant — Glue di integrazione DESKTOP del bridge libreria.
 *
 * Il modulo `modules/riplay-bridge` (API REST /api/v1 per il provider Python
 * esterno `riplay`) viene montato DENTRO lo stesso Ktor application del
 * RelayServer tramite il parametro `extraModules` di RelayServer (vedi
 * DesktopRelay.kt): NESSUNA porta aggiuntiva, NESSUN token aggiuntivo.
 *
 * Questo file fornisce:
 *
 *   1. DesktopDatabaseBridgeCatalog -> database Room condiviso (composeApp
 *      commonMain): Song/Artist/Album via Database.songDao()/artistDao()/
 *      albumDao() e playlist via Database.playlistsPaged()/countPlaylists()/
 *      playlistById()/playlistSongsSnapshot()
 *   2. RelayStreamUrlResolver -> vedi androidApp BridgeIntegration.kt (stessa
 *      classe, duplicata qui: composeApp jvmMain non vede i sorgenti androidApp)
 *   3. DesktopMusicAssistantBridge (holder) + installDesktopMusicAssistantBridge():
 *      costruisce catalogo/resolver UNA volta e decide, a ogni creazione del
 *      server, se montare bridgeModule (interruttore in Impostazioni -> Generale,
 *      card del server di streaming; persistenza in ~/.riplay/relay.properties,
 *      chiave "ma.enabled"; default OFF)
 *   4. desktopSongUriFor(): URI servibile di una Song (stessa logica di
 *      desktopRelayCandidate in DesktopRelay.kt: WebDAV -> URL diretto; locale
 *      "local:<percorso>" o percorso assoluto -> percorso file; online -> null)
 *
 * Avvio (bootstrapDesktopApp in main.kt, DOPO installDesktopRelayServer):
 *
 *   installDesktopMusicAssistantBridge()
 *   if (DesktopRelaySettings.enabled) { RelayManager.startIfEnabled(true) }
 *
 * Il token Bearer del bridge e' quello del relay: gli URL stream usano il token
 * passato DAL RelayServer al momento del montaggio (extraModules -> onMounted).
 *
 * Toggle: Impostazioni -> Generale -> card "RiPlay streaming server" ->
 * interruttore "Libreria Music Assistant" (DesktopMusicAssistantBridge.
 * setMaBridgeEnabled persiste e riavvia il server per rimontare/smontare il
 * modulo). La parte Python sta nel repo esterno riplay-music-assistant
 * (provider Music Assistant): NESSUNA parte Python dentro RiPlay.
 */
package it.fast4x.riplay.server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.server.application.Application
import it.fast4x.riplay.bridge.BridgeAlbum
import it.fast4x.riplay.bridge.BridgeArtist
import it.fast4x.riplay.bridge.BridgeCatalog
import it.fast4x.riplay.bridge.BridgeConfig
import it.fast4x.riplay.bridge.BridgeHealth
import it.fast4x.riplay.bridge.BridgePlaylist
import it.fast4x.riplay.bridge.BridgeSearchResult
import it.fast4x.riplay.bridge.BridgeTrack
import it.fast4x.riplay.bridge.CatalogPage
import it.fast4x.riplay.bridge.StreamUrlResolver
import it.fast4x.riplay.bridge.bridgeModule
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.dao.AlbumDao
import it.fast4x.riplay.data.dao.ArtistDao
import it.fast4x.riplay.data.dao.SongDao
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Implementazione del catalogo bridge sul database Room condiviso (desktop).
 *
 * Regola di servibilita' (stessa semantica del RelayServer/RelayUris):
 * - brano locale (percorso assoluto / "local:<percorso>") -> servibile via relay
 * - WebDAV ("webdav:<url>") -> URL http diretto (directUrl)
 * - brani online (videoId YouTube) -> NON servibili: esposti con servable=false
 */
private class DesktopDatabaseBridgeCatalog(
    private val songDao: SongDao,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    /** Estrae l'URI servibile di una Song (webdav/locale/http); null se solo-YouTube. */
    private val uriFor: (Song) -> String?,
) : BridgeCatalog {

    // ─────────────── mappatura Song -> BridgeTrack ───────────────

    private fun isServable(song: Song): Boolean {
        val uri = uriFor(song) ?: return false
        return RelayUris.isDirect(uri) || RelayUris.isLocal(uri)
    }

    private fun durationSeconds(song: Song): Int =
        (durationTextToMillis(song.durationText ?: "") / 1000L).toInt()

    private fun toBridgeTrack(song: Song, albumTitle: String? = null): BridgeTrack {
        val uri = uriFor(song)
        return BridgeTrack(
            id = song.id,
            name = song.title,
            artistId = null,
            artistName = song.artistsText,
            albumId = song.albumId,
            albumName = albumTitle,
            durationSeconds = durationSeconds(song),
            trackNumber = 0,
            discNumber = 0,
            year = null,
            mimeType = null,
            sizeBytes = null,
            thumbnailUrl = song.thumbnailUrl,
            directUrl = uri?.takeIf { RelayUris.isDirect(it) },
            servable = isServable(song),
        )
    }

    // ─────────────── BridgeCatalog ───────────────

    override suspend fun artists(offset: Int, limit: Int): CatalogPage<BridgeArtist> {
        val items = artistDao.getAllArtistsPaged(limit, offset)
        return CatalogPage(items.map { a ->
            BridgeArtist(
                id = a.id,
                name = a.name ?: "Artista sconosciuto",
                thumbnailUrl = a.thumbnailUrl,
            )
        }, artistDao.countArtists())
    }

    override suspend fun albums(offset: Int, limit: Int): CatalogPage<BridgeAlbum> {
        val items = albumDao.getAllAlbumsPaged(limit, offset)
        return CatalogPage(items.map { al ->
            BridgeAlbum(
                id = al.id,
                name = al.title ?: "Album sconosciuto",
                artistName = al.authorsText,
                year = al.year?.toIntOrNull(),
                thumbnailUrl = al.thumbnailUrl,
            )
        }, albumDao.countAlbums())
    }

    override suspend fun tracks(offset: Int, limit: Int): CatalogPage<BridgeTrack> {
        val songs = songDao.getAllSongsPaged(limit, offset)
        return CatalogPage(songs.map { toBridgeTrack(it) }, songDao.countSongs())
    }

    override suspend fun playlists(offset: Int, limit: Int): CatalogPage<BridgePlaylist> {
        val items = Database.playlistsPaged(limit, offset)
        return CatalogPage(items.map { p ->
            BridgePlaylist(
                id = p.id.toString(),
                name = p.name,
            )
        }, Database.countPlaylists())
    }

    override suspend fun artist(id: String): BridgeArtist? =
        artistDao.getById(id)?.let {
            BridgeArtist(id = it.id, name = it.name ?: "", thumbnailUrl = it.thumbnailUrl)
        }

    override suspend fun albumsOfArtist(artistId: String, offset: Int, limit: Int): CatalogPage<BridgeAlbum> {
        // Come su Android (BridgeIntegration.kt): si risale al nome dell'artista
        // e si cercano i suoi brani per artistsText, poi gli album registrati.
        val artistName = artistDao.getById(artistId)?.name
            ?: return CatalogPage(emptyList(), 0)
        val songs = songDao.getSongsByArtistsTextLike(artistName, limit = 1000)
        val albums = songs.mapNotNull { it.albumId }.distinct().mapNotNull { albumDao.getById(it) }
        return CatalogPage(albums.drop(offset).take(limit).map { al ->
            BridgeAlbum(
                id = al.id,
                name = al.title ?: "",
                artistName = al.authorsText,
                year = al.year?.toIntOrNull(),
                thumbnailUrl = al.thumbnailUrl,
            )
        }, albums.size)
    }

    override suspend fun album(id: String): BridgeAlbum? =
        albumDao.getById(id)?.let {
            BridgeAlbum(id = it.id, name = it.title ?: "", artistName = it.authorsText, year = it.year?.toIntOrNull())
        }

    override suspend fun tracksOfAlbum(albumId: String, offset: Int, limit: Int): CatalogPage<BridgeTrack> {
        val songs = songDao.getSongsByAlbumPaged(albumId, limit, offset)
        return CatalogPage(
            songs.map { toBridgeTrack(it, albumTitle = albumDao.getById(albumId)?.title) },
            songDao.countSongsByAlbum(albumId),
        )
    }

    override suspend fun tracksOfPlaylist(playlistId: String, offset: Int, limit: Int): CatalogPage<BridgeTrack> {
        val id = playlistId.toLongOrNull() ?: return CatalogPage(emptyList(), 0)
        val songs = Database.playlistSongsSnapshot(id)
        return CatalogPage(songs.drop(offset).take(limit).map { toBridgeTrack(it) }, songs.size)
    }

    override suspend fun playlist(id: String): BridgePlaylist? {
        val pid = id.toLongOrNull() ?: return null
        return Database.playlistById(pid)?.let {
            BridgePlaylist(id = it.id.toString(), name = it.name)
        }
    }

    override suspend fun track(id: String): BridgeTrack? =
        songDao.getById(id)?.let { toBridgeTrack(it) }

    override suspend fun search(query: String, limit: Int): BridgeSearchResult {
        val byTitle = songDao.getSongsByTitleLike(query, limit = limit)
        val byArtist = songDao.getSongsByArtistsTextLike(query, limit = limit)
        val tracks = (byTitle + byArtist).distinctBy { it.id }.map { toBridgeTrack(it) }
        val artists = artistDao.getAllArtistsPaged(limit, 0)
            .filter { it.name?.contains(query, ignoreCase = true) == true }
            .map { BridgeArtist(id = it.id, name = it.name ?: "", thumbnailUrl = it.thumbnailUrl) }
        val albums = albumDao.getAllAlbumsPaged(limit, 0)
            .filter { it.title?.contains(query, ignoreCase = true) == true }
            .map { BridgeAlbum(id = it.id, name = it.title ?: "", artistName = it.authorsText) }
        val playlists = Database.playlistsPaged(limit, 0)
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { BridgePlaylist(id = it.id.toString(), name = it.name) }
        return BridgeSearchResult(
            artists = artists.take(limit),
            albums = albums.take(limit),
            tracks = tracks.take(limit),
            playlists = playlists.take(limit),
        )
    }

    override suspend fun stats(): BridgeHealth = BridgeHealth(
        artistCount = artistDao.countArtists(),
        albumCount = albumDao.countAlbums(),
        trackCount = songDao.countSongs(),
        playlistCount = Database.countPlaylists(),
        servableTrackCount = 0,
    )
}

/**
 * Resolver verso il RelayServer ESISTENTE: registra il brano nel [RelayRegistry]
 * e costruisce l'URL /relay/<token>/s/<sha256(uri)[:20]> con host:porta del client.
 * Nessun byte passa dal bridge: lo streaming resta al RelayServer (Range incluso).
 *
 * Il token NON viene copiato qui: arriva dal RelayServer al momento del montaggio
 * (extraModules -> onMounted), cosi' bridge e relay restano sempre allineati.
 */
private class DesktopRelayStreamUrlResolver(
    private val registry: RelayRegistry,
    private val relayToken: () -> String,
    /** URI servibile del brano (locale/webdav) o null. */
    private val uriFor: (BridgeTrack) -> String?,
) : StreamUrlResolver {

    override fun streamUrl(host: String, port: Int, track: BridgeTrack): String? {
        // 1) URL diretto: il telefono/il client resta fuori dal percorso audio
        track.directUrl?.let { direct ->
            if (direct.startsWith("http://") || direct.startsWith("https://")) return direct
        }
        // 2) locale: registra nel relay e pubblica l'URL
        val uri = uriFor(track) ?: return null
        if (!RelayUris.isLocal(uri) && !RelayUris.isDirect(uri)) return null
        val relayTrack = registry.register(
            RelayRegistry.Candidate(
                uri = uri,
                title = track.name,
                artist = track.artistName,
                mime = track.mimeType,
            )
        )
        val base = "http://$host:$port/relay/${relayToken()}"
        return RelayUris.streamUrl(base, relayTrack)
    }

    override fun streamKind(host: String, port: Int, track: BridgeTrack): String? {
        track.directUrl?.let { if (it.startsWith("http")) return "direct" }
        val uri = uriFor(track) ?: return null
        return if (RelayUris.isLocal(uri) || RelayUris.isDirect(uri)) "relay" else null
    }
}

/**
 * URI servibile di una Song per il desktop: STESSA logica di
 * [desktopRelayCandidate] (DesktopRelay.kt).
 */
internal fun desktopSongUriFor(song: Song): String? {
    val raw = (song.mediaId ?: song.id).trim()
    return when {
        raw.startsWith(WEBDAV_KEY_PREFIX) ->
            raw.removePrefix(WEBDAV_KEY_PREFIX).trim()
                .takeIf { RelayUris.isDirect(it) }
        raw.startsWith(LOCAL_KEY_PREFIX) ->
            raw.removePrefix(LOCAL_KEY_PREFIX).trim()
                .takeIf { RelayUris.isLocal(it) }
        RelayUris.isLocal(raw) -> raw // percorso assoluto POSIX/Windows (pagina Locali)
        else -> null                  // videoId/online: non servibile dal bridge
    }
}

/** File delle preferenze del bridge: STESSO file del relay (~/.riplay/relay.properties). */
private val bridgePrefsFile: File
    get() = File(System.getProperty("user.home"), ".riplay").resolve("relay.properties")

private fun readBridgeEnabled(): Boolean = try {
    val p = java.util.Properties()
    val f = bridgePrefsFile
    if (f.isFile) f.inputStream().use { p.load(it) }
    p.getProperty("ma.enabled", "false").trim().equals("true", ignoreCase = true)
} catch (t: Throwable) {
    false
}

private fun persistBridgeEnabled(value: Boolean) {
    try {
        val p = java.util.Properties()
        val f = bridgePrefsFile
        if (f.isFile) f.inputStream().use { p.load(it) }
        p.setProperty("ma.enabled", value.toString())
        f.parentFile?.mkdirs()
        f.outputStream().use { p.store(it, "RiPlay desktop Music Assistant bridge") }
    } catch (t: Throwable) {
        // silenzioso: la persistenza non deve mai impedire l'uso dell'app
    }
}

/**
 * Holder del bridge Music Assistant lato desktop. `enabled` e' Compose state
 * (l'interruttore nella SettingsPage si ricompone da solo); le altre campagne
 * non osservabili restano campi normali.
 */
object DesktopMusicAssistantBridge {

    /**
     * Stato osservabile dalla UI (Impostazioni -> Generale, card del server).
     * Valore iniziale: chiave "ma.enabled" di ~/.riplay/relay.properties.
     */
    var enabled: Boolean by mutableStateOf(readBridgeEnabled())
        private set

    @Volatile
    private var relayToken: String? = null

    private var catalog: BridgeCatalog? = null
    private var resolver: StreamUrlResolver? = null

    /** Costruisce catalogo e resolver (DAO del database condiviso composeApp). */
    fun initialize() {
        if (catalog != null && resolver != null) return
        catalog = DesktopDatabaseBridgeCatalog(
            songDao = Database.songDao(),
            artistDao = Database.artistDao(),
            albumDao = Database.albumDao(),
            uriFor = ::desktopSongUriFor,
        )
        resolver = DesktopRelayStreamUrlResolver(
            registry = RelayManager.registry,
            relayToken = { relayToken ?: "" },
            uriFor = { track ->
                track.directUrl ?: runBlocking {
                    Database.songDao().getById(track.id)?.let { song ->
                        desktopSongUriFor(song)?.takeIf { RelayUris.isLocal(it) }
                    }
                }
            },
        )
        println("[RiPlay-Bridge] catalogo Music Assistant pronto (database condiviso)")
    }

    /**
     * Cambia l'interruttore, persiste SUBITO e riavvia il server per
     * rimontare/smontare il modulo (il bridge si monta solo allo start).
     * Nome con suffisso (convenzione del progetto setEnabledXxx, es.
     * DesktopRelaySettings.setDesktopRelayEnabled): evita il clash JVM con
     * il setter privato della property enabled.
     */
    fun setMaBridgeEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        persistBridgeEnabled(value)
        // Riavvio del server di streaming: il provider (DesktopRelay.kt) legge
        // desktopMusicAssistantBridgeModule() alla CREAZIONE del RelayServer.
        RelayManager.stop()
        RelayManager.startIfEnabled(DesktopRelaySettings.enabled)
    }

    /** Registrato dal RelayServer al momento del montaggio (extraModules). */
    internal fun onMounted(token: String) {
        relayToken = token
    }

    /**
     * Modulo Ktor da passare a RelayServer.extraModules, o null se il bridge
     * e' disabilitato (applicazione identica a prima).
     */
    internal fun moduleOrNull(): (Application.(token: String) -> Unit)? {
        if (!enabled) return null
        val c = catalog ?: return null
        val r = resolver ?: return null
        return { token ->
            this@DesktopMusicAssistantBridge.onMounted(token)
            bridgeModule(catalog = c, resolver = r, config = BridgeConfig(token = token))
        }
    }
}

/**
 * Installa il wiring desktop del bridge libreria. Chiamare UNA SOLA VOLTA in
 * bootstrapDesktopApp (main.kt), PRIMA di [RelayManager.startIfEnabled] e
 * insieme a [installDesktopRelayServer] (DesktopRelay.kt).
 */
fun installDesktopMusicAssistantBridge() {
    DesktopMusicAssistantBridge.initialize()
    println(
        "[RiPlay-Bridge] wiring desktop del bridge Music Assistant installato (enabled=${DesktopMusicAssistantBridge.enabled})"
    )
}

/**
 * Modulo Ktor per RelayServer.extraModules (DesktopRelay.kt): null se disabilitato,
 * altrimenti la funzione che monta bridgeModule nell'applicazione del relay.
 */
fun desktopMusicAssistantBridgeModule(): (Application.(token: String) -> Unit)? =
    DesktopMusicAssistantBridge.moduleOrNull()
