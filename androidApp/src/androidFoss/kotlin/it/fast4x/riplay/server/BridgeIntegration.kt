/*
 * RiPlay + Music Assistant — Glue di integrazione ANDROID del bridge libreria.
 *
 * SOLO FLAVOR FOSS (src/androidFoss): e' la versione actual del contratto
 * dichiarato in MusicAssistantBridgeExpect.kt (src/main). La versione full
 * usa lo stub MusicAssistantBridgeStub.kt e non ha nemmeno il modulo
 * `riplay-bridge` nel classpath (dipendenza fossImplementation) — condizione
 * accettata dalla community di Music Assistant per il provider `riplay`.
 *
 * Il modulo `modules/riplay-bridge` (API REST /api/v1 per il provider Python
 * esterno `riplay`) viene montato DENTRO lo stesso Ktor application del
 * RelayServer tramite il parametro `extraModules` di RelayServer: NESSUNA
 * porta aggiuntiva, NESSUN token aggiuntivo. Questo file fornisce:
 *
 *   1. RoomBridgeCatalog    -> database Room condiviso (composeApp commonMain:
 *      Song/Artist/Album/Playlist via Database.songDao()/artistDao()/albumDao()
 *      e le query playlistsPaged/countPlaylists/playlistSongsSnapshot)
 *   2. RelayStreamUrlResolver -> RelayRegistry + RelayUris ESISTENTI (nessun
 *      cambio al server di streaming: i byte continuano a essere erogati da
 *      /relay/<token>/s/<id> con supporto Range gia' operativo)
 *   3. MusicAssistantBridge (holder) + installAndroidMusicAssistantBridge():
 *      costruisce catalogo/resolver UNA volta e decide, a ogni creazione del
 *      server, se montare bridgeModule (setting maBridgeEnabled, default OFF)
 *   4. androidSongUriFor(): URI servibile di una Song (stessa logica usata da
 *      PlayerService per il cast: Music Vault -> content:// o file nella
 *      cartella vault; brani on device "local:<id>" -> URI MediaStore
 *      ricostruito; brani online YouTube -> null, non servibili)
 *
 * Avvio (MainApplication.onCreate, DOPO installAndroidRelayServer):
 *
 *   installAndroidMusicAssistantBridge(
 *       context = this,
 *       enabled = appSettingsManager.activeSettings.value.maBridgeEnabled,
 *   )
 *
 * Il token Bearer del bridge e' quello del relay: gli URL stream usano il
 * token passato DAL RelayServer al momento del montaggio (extraModules), quindi
 * non esiste possibilita' di disallineamento tra bridge e relay.
 *
 * Toggle: Impostazioni -> Generale -> sezione "RiPlay streaming server" ->
 * "Music Assistant library bridge". Un cambio di setting riavvia il server
 * (il modulo si monta solo allo start); vedi GeneralSettings e
 * PlayerService.startObservingSettings.
 *
 * NOTE DAO: le query con offset/limit usate dal catalogo (getAllSongsPaged,
 * countSongs, getSongsByAlbumPaged, getAllArtistsPaged, countArtists,
 * getAllAlbumsPaged, countAlbums, playlistsPaged, countPlaylists,
 * playlistSongsSnapshot) sono state AGGIUNTE ai DAO esistenti e a Database.kt
 * (Room le compila da sole, NESSUNA migration).
 */
package it.fast4x.riplay.server

import android.content.Context
import androidx.core.net.toUri
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
import it.fast4x.riplay.extensions.musicbrainz.workers.WorkerDependencies.database
import it.fast4x.riplay.musicvault.MusicVaultRepository
import it.fast4x.riplay.musicvault.MusicVaultState
import it.fast4x.riplay.utils.localMediaStoreUriOrNull
import kotlinx.coroutines.runBlocking
import java.io.File
import timber.log.Timber

/**
 * Implementazione del catalogo bridge sul database Room di RiPlay.
 *
 * Regola di servibilita' (stessa semantica del RelayServer/RelayUris):
 * - Music Vault (musicVaultState COMPLETED) -> file nel vault: servibile via relay
 * - brani on device (mediaId "local:<id>" con URI MediaStore ricostruibile) -> servibile via relay
 * - URI http(s) diretto (WebDAV) -> servibile direttamente (directUrl)
 * - brani solo-YouTube (videoId) -> NON servibili: esposti con servable=false
 */
class RoomBridgeCatalog(
    private val songDao: SongDao,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    /** Estrae l'URI servibile di una Song (file/vault/local/http); null se solo-YouTube. */
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
        }, database.countPlaylists())
    }

    override suspend fun artist(id: String): BridgeArtist? =
        artistDao.getById(id)?.let {
            BridgeArtist(id = it.id, name = it.name ?: "", thumbnailUrl = it.thumbnailUrl)
        }

    override suspend fun albumsOfArtist(artistId: String, offset: Int, limit: Int): CatalogPage<BridgeAlbum> {
        // In RiPlay la relazione artista-brano vive in SongArtistCrossRef/SongArtistMap
        // e l'album e' derivato dai brani (Song.albumId): si risale al nome dell'artista
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
        servableTrackCount = 0, // conteggio pesante: il provider non lo richiede per il flusso
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
class RelayStreamUrlResolver(
    private val registry: RelayRegistry,
    private val relayToken: () -> String,
    /** URI servibile del brano (file/locale/http) o null. */
    private val uriFor: (BridgeTrack) -> String?,
) : StreamUrlResolver {

    override fun streamUrl(host: String, port: Int, track: BridgeTrack): String? {
        // 1) URL diretto: il telefono resta fuori dal percorso audio
        track.directUrl?.let { direct ->
            if (direct.startsWith("http://") || direct.startsWith("https://")) return direct
        }
        // 2) locale/Music Vault/on device: registra nel relay e pubblica l'URL
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
 * URI servibile di una Song per Android: STESSA logica del cast UPnP
 * (PlayerService, ripristino coda / sincronizzazione relay):
 * - Music Vault COMPLETATO -> content:// o file nella cartella del vault
 * - brano on device (mediaId "local:<id>") -> URI MediaStore ricostruito
 * - brani online (videoId YouTube) -> null (non servibili dal bridge)
 */
internal fun androidSongUriFor(song: Song): String? {
    // Music Vault: file scaricato sul device
    if (song.musicVaultState == MusicVaultState.COMPLETED && !song.musicVaultFileName.isNullOrBlank()) {
        val fileName = song.musicVaultFileName!!
        return if (fileName.startsWith("content://")) fileName
        else File(MusicVaultRepository.getOutputDir(), fileName).toUri().toString()
    }
    // Brani on device: mediaId "local:<id>" -> URI MediaStore content://
    val mediaId = song.mediaId ?: song.id
    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) {
        return localMediaStoreUriOrNull(mediaId)?.toString()
    }
    // Brani online (videoId): nessun URL diretto -> non servibile
    return null
}

/**
 * Holder del bridge Music Assistant lato Android: costruito UNA volta da
 * [installAndroidMusicAssistantBridge] (MainApplication.onCreate) e interrogato
 * dal provider di [RelayManager] (AndroidRelay.kt) a ogni creazione del server.
 */
object MusicAssistantBridge {

    private const val TAG = "Bridge"

    /** Stato del toggle maBridgeEnabled (specchiato da MainApplication/PlayerService). */
    @Volatile
    var enabled: Boolean = false
        private set

    @Volatile
    private var relayToken: String? = null

    private var catalog: BridgeCatalog? = null
    private var resolver: StreamUrlResolver? = null

    /** Costruisce catalogo e resolver (DAO del database condiviso composeApp). */
    fun initialize(context: Context) {
        if (catalog != null && resolver != null) return
        catalog = RoomBridgeCatalog(
            songDao = Database.songDao(),
            artistDao = Database.artistDao(),
            albumDao = Database.albumDao(),
            uriFor = ::androidSongUriFor,
        )
        resolver = RelayStreamUrlResolver(
            registry = RelayManager.registry,
            relayToken = { relayToken ?: "" },
            uriFor = { track ->
                track.directUrl ?: runBlocking {
                    Database.songDao().getById(track.id)?.let { song ->
                        androidSongUriFor(song)?.takeIf { RelayUris.isLocal(it) }
                    }
                }
            },
        )
        Timber.tag(TAG).i("Music Assistant bridge pronto (catalogo Room, token condiviso con relay)")
    }

    /** Applica il setting maBridgeEnabled (MainApplication all'avvio, PlayerService al cambio). */
    fun applySetting(value: Boolean) {
        enabled = value
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
            this@MusicAssistantBridge.onMounted(token)
            bridgeModule(catalog = c, resolver = r, config = BridgeConfig(token = token))
        }
    }
}

/**
 * Il bridge Music Assistant esiste solo nel flavor FOSS: l'actual foss e'
 * sempre true (il toggle in GeneralSettings compare solo qui).
 */
val isMusicAssistantBridgeSupported: Boolean = true

/**
 * Implementazione FOSS dell'installazione del wiring Android del bridge
 * libreria. Chiamare UNA SOLA VOLTA in MainApplication.onCreate, DOPO
 * [installAndroidRelayServer] e DOPO appSettingsManager.initialize().
 */
fun installAndroidMusicAssistantBridge(context: Context, enabled: Boolean) {
    MusicAssistantBridge.initialize(context)
    MusicAssistantBridge.applySetting(enabled)
    Timber.tag("RelayManager").i(
        "wiring Android del bridge Music Assistant installato (enabled=%s)", enabled
    )
}

/**
 * Modulo Ktor per RelayServer.extraModules (AndroidRelay.kt): null se disabilitato,
 * altrimenti la funzione che monta bridgeModule nell'applicazione del relay.
 */
fun androidMusicAssistantBridgeModule(): (Application.(token: String) -> Unit)? =
    MusicAssistantBridge.moduleOrNull()
