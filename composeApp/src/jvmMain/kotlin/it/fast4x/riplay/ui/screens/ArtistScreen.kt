package it.fast4x.riplay.ui.screens

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.artistPage
import it.fast4x.environment.utils.ArtistDiscographyType
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.items.AlbumItem
import it.fast4x.riplay.items.PlaylistItem
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.albumThumbnailSize
import it.fast4x.riplay.styling.Dimensions.fadeSpacingBottom
import it.fast4x.riplay.styling.Dimensions.fadeSpacingTop
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.styling.Dimensions.playlistThumbnailSize
import it.fast4x.riplay.ui.components.AutoResizeText
import it.fast4x.riplay.ui.components.FontSizeRange
import it.fast4x.riplay.ui.components.HeaderActionIcon
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.components.Title
import it.fast4x.riplay.ui.components.Title2Actions
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Shuffle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.albums
import riplay.composeapp.generated.resources.artist_subscribers
import riplay.composeapp.generated.resources.dice
import riplay.composeapp.generated.resources.from_wikipedia_cca
import riplay.composeapp.generated.resources.information
import riplay.composeapp.generated.resources.playlists
import riplay.composeapp.generated.resources.singles
import riplay.composeapp.generated.resources.songs
import riplay.composeapp.generated.resources.translate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.random.Random

/**
 * Dati UI della pagina artista, popolati dalla STESSA sorgente di Android:
 * EnvironmentExt.getArtistPage (parsing delle sezioni INDIPENDENTE dai
 * titoli localizzati) + getArtistDiscography (Album/Single) per la
 * discografia completa + getArtistItemsPage(...).completed() per la lista
 * brani integrale.
 */
private data class ArtistUiData(
    val name: String?,
    val thumbnailUrl: String?,
    val subscribersText: String?,
    val description: String?,
    val songs: List<Environment.SongItem>,
    val playlists: List<Environment.PlaylistItem>,
    val albums: List<Environment.AlbumItem>,
    val singles: List<Environment.AlbumItem>,
    // endpoint "vedi tutti" della sezione brani (per il caricamento
    // COMPLETO come fa Android su click)
    val songsBrowseId: String? = null,
    val songsParams: String? = null,
    // STEP 3aq (richiesta utente: "procedi con la view all come avviene
    // in android"): gli endpoint "vedi tutto" delle ALTRE sezioni. Su
    // Android OGNI sezione con moreEndpoint apre la lista completa
    // (ArtistOverviewItems): qui catturiamo gli stessi endpoint.
    // Nota albums/singles: YT Music mette le sezioni disco con item
    // AlbumItem in ordine (prima ALBUM poi SINGOLI): la prima sezione
    // AlbumItem e' quella degli album, la seconda quella dei singoli.
    val albumsBrowseId: String? = null,
    val albumsParams: String? = null,
    val singlesBrowseId: String? = null,
    val singlesParams: String? = null,
    val playlistsBrowseId: String? = null,
    val playlistsParams: String? = null
)

/**
 * STEP 3o (richiesta utente n.2 "modificare la pagina di dettaglio di
 * artista, album e playlist a quella lato android"): ristrutturata come
 * ArtistOverview di Android in LANDSCAPE:
 *  - a SINISTRA la foto QUADRATA fissa dell'artista (40%, 1:1);
 *  - a DESTRA colonna scorrevole: NOME grande centrato (32..38sp), riga
 *    iscritti centrata, RIGA DI AZIONI (Segui artista / shuffle brani),
 *    poi Brani, Playlists, Albums, Singles e descrizione (Information).
 *
 * FIX (richiesta utente: "in desktop si vedono solo gli album"): la pagina
 * NON usa piu' Environment.artistPage (che matchava i titoli delle sezioni
 * con stringhe INGLESI hardcoded "Songs"/"Albums"/"Singles"/"Playlists" e
 * leggeva solo singleColumnBrowseResultsRenderer: con le risposte
 * localizzate o a due colonne brani/singoli/playlist sparivano). Ora usiamo
 * le STESSHE chiamate di ArtistOverview Android:
 *  - EnvironmentExt.getArtistPage(browseId)      -> sezioni per TIPO item
 *  - EnvironmentExt.getArtistDiscography(...Album)  -> album COMPLETI
 *  - EnvironmentExt.getArtistDiscography(...Single) -> singoli COMPLETI
 *  - EnvironmentExt.getArtistItemsPage(...).completed() -> brani integrali
 * Il vecchio Environment.artistPage resta solo come FALLBACK se la
 * chiamata primaria fallisce.
 *
 * Le funzioni restano IDENTICHE: salvataggio dell'artista nel db al load,
 * SEGUI (bookmarkedAt come gli album preferiti, step3i), traduzione della
 * biografia, shuffle dei brani dell'artista.
 */
@OptIn(ExperimentalSerializationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArtistScreen(
    browseId: String,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit,
    // STEP 3aq: "VEDI TUTTO" di una sezione (come Android che apre
    // ArtistOverviewItems nel bottom sheet): la schermata riceve la
    // richiesta completa (endpoint della sezione + nome artista/sezione
    // + item gia' caricati come fallback) e la gestisce con l'overlay
    // ArtistItemsScreen. null = l'app non gestisce la view all.
    onViewAllSection: ((ArtistSectionRequest) -> Unit)? = null,
    onAlbumClick: (String) -> Unit,
    onClosePage: () -> Unit,
    // STEP 3i: azione "aggiungi alla coda" sui brani dell'artista
    onAddToQueue: ((Song) -> Unit)? = null,
    // STEP 3o: click = TUTTA la lista in coda (usato dallo shuffle)
    onPlayCollection: ((List<Song>, Song) -> Unit)? = null,
    // STEP 3o (richiesta utente n.2): "metti in coda tutto" senza avviare
    // (passato dall'app per uniformita' con album/playlist; la pagina
    // artista lo lascia ai singoli pulsanti "+" dei brani)
    onEnqueueAll: ((List<Song>) -> Unit)? = null
) {
    val artistUi = remember { mutableStateOf<ArtistUiData?>(null) }

    // lista brani COMPLETI dell'artista (per lo shuffle): parte dai brani
    // della sezione e viene poi sostituita dal fetch integrale (completed)
    var allArtistSongsItems by remember {
        mutableStateOf<List<Environment.SongItem>>(emptyList())
    }

    // STEP 3i - ARTISTA SEGUITO: lo stato dell'artista nel db (se esiste,
    // con bookmarkedAt = "seguito") arriva reattivo dal Flow
    val currentArtist by Database.artist(browseId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(browseId) {
        // ─────────────────────────────────────────────────────────────
        // STESSA SORGENTE DATI DI ANDROID (ArtistOverview):
        // EnvironmentExt.getArtistPage -> parsing per tipo item, non per
        // titolo: funziona con qualunque lingua e con layout a 1 o 2 colonne
        // ─────────────────────────────────────────────────────────────
        val page = EnvironmentExt.getArtistPage(browseId = browseId).getOrNull()

        if (page != null) {
            val sectionSongs = page.sections
                .flatMap { it.items.filterIsInstance<Environment.SongItem>() }
                .distinctBy { it.key }
            val sectionPlaylists = page.sections
                .flatMap { it.items.filterIsInstance<Environment.PlaylistItem>() }
                .distinctBy { it.key }
            val sectionAlbums = page.sections
                .flatMap { it.items.filterIsInstance<Environment.AlbumItem>() }
                .distinctBy { it.key }
            val songsSection = page.sections
                .firstOrNull { it.items.firstOrNull() is Environment.SongItem }

            // STEP 3aq: sezioni con item AlbumItem (albums + singles) e
            // PlaylistItem: i loro moreEndpoint alimentano la view all
            val albumSections = page.sections
                .filter { it.items.firstOrNull() is Environment.AlbumItem }
            val playlistsSection = page.sections
                .firstOrNull { it.items.firstOrNull() is Environment.PlaylistItem }

            artistUi.value = ArtistUiData(
                name = page.artist.info?.name,
                thumbnailUrl = page.artist.thumbnail?.url,
                subscribersText = page.subscribers ?: page.artist.subscribersCountText,
                description = page.description,
                songs = sectionSongs,
                playlists = sectionPlaylists,
                // provvisorio: sotto vengono sostituiti dalla discografia completa
                albums = sectionAlbums,
                singles = emptyList(),
                songsBrowseId = songsSection?.moreEndpoint?.browseId,
                songsParams = songsSection?.moreEndpoint?.params,
                albumsBrowseId = albumSections.getOrNull(0)?.moreEndpoint?.browseId,
                albumsParams = albumSections.getOrNull(0)?.moreEndpoint?.params,
                singlesBrowseId = albumSections.getOrNull(1)?.moreEndpoint?.browseId,
                singlesParams = albumSections.getOrNull(1)?.moreEndpoint?.params,
                playlistsBrowseId = playlistsSection?.moreEndpoint?.browseId,
                playlistsParams = playlistsSection?.moreEndpoint?.params
            )
            allArtistSongsItems = sectionSongs

            // STEP 3i: SALVA L'ARTISTA nel db (stesso schema di
            // AlbumScreen per gli album): cosi' l'artista VISITATO
            // compare nella pagina Artists e PUO' essere seguito con
            // il bookmark qui sotto. Se l'artista esisteva gia' (con
            // generi/biografia/bookmark) i dati esistenti sono
            // PRESERVATI: si aggiornano solo nome e thumbnail.
            runCatching {
                val existing = Database.artist(browseId).first()
                val toSave = if (existing != null)
                    existing.copy(
                        name = page.artist.info?.name ?: existing.name,
                        thumbnailUrl = page.artist.thumbnail?.url ?: existing.thumbnailUrl
                    )
                else
                    Artist(
                        id = browseId,
                        name = page.artist.info?.name,
                        thumbnailUrl = page.artist.thumbnail?.url,
                        timestamp = System.currentTimeMillis(),
                        isYoutubeArtist = true
                    )
                if (existing == null ||
                    existing.name != toSave.name ||
                    existing.thumbnailUrl != toSave.thumbnailUrl
                ) {
                    Database.upsert(toSave)
                }
            }.onFailure { e -> println("[RiPlay-Artista] salvataggio artista fallito: ${e.message}") }

            // ─────────────────────────────────────────────────────────
            // DISCOGRAFIA COMPLETA (come Android): albums e SINGOLI dalla
            // discografia ufficiale (browse MPAD) cosi' si vedono ENTRAMBI
            // anche quando la home dell'artista non espone il carosello
            // ─────────────────────────────────────────────────────────
            val albumsDiscography = runCatching {
                EnvironmentExt.getArtistDiscography(browseId, ArtistDiscographyType.Album)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<Environment.AlbumItem>()
                    .orEmpty()
                    .distinctBy { it.key }
            }.getOrDefault(emptyList())

            val singlesDiscography = runCatching {
                EnvironmentExt.getArtistDiscography(browseId, ArtistDiscographyType.Single)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<Environment.AlbumItem>()
                    .orEmpty()
                    .distinctBy { it.key }
            }.getOrDefault(emptyList())

            if (albumsDiscography.isNotEmpty() || singlesDiscography.isNotEmpty()) {
                artistUi.value = artistUi.value?.copy(
                    albums = albumsDiscography.ifEmpty { sectionAlbums },
                    singles = singlesDiscography
                )
            }

            // ─────────────────────────────────────────────────────────
            // BRANI INTEGRALI (come Android su click): getArtistItemsPage
            // + completed() segue le pagine di continuazione
            // ─────────────────────────────────────────────────────────
            val songsBrowseId = artistUi.value?.songsBrowseId
            if (!songsBrowseId.isNullOrBlank()) {
                val fullSongs = runCatching {
                    EnvironmentExt.getArtistItemsPage(
                        BrowseEndpoint(
                            browseId = songsBrowseId,
                            // FIX (correzione utente): songsParams (non
                            // songsToParams): e' il nome del campo del
                            // data class ArtistUiData
                            params = artistUi.value?.songsParams
                        )
                    ).completed()
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<Environment.SongItem>()
                        .orEmpty()
                        .distinctBy { it.key }
                }.getOrDefault(emptyList())

                if (fullSongs.isNotEmpty())
                    allArtistSongsItems = fullSongs
            }
        } else {
            // FALLBACK (solo se getArtistPage fallisce): vecchia chiamata
            // Environment.artistPage (parsing per titolo, meno affidabile)
            Environment.artistPage(BrowseBody(browseId = browseId))
                ?.onSuccess { legacy ->
                    artistUi.value = ArtistUiData(
                        name = legacy.name,
                        thumbnailUrl = legacy.thumbnail?.url,
                        subscribersText = legacy.subscriberCountText,
                        description = legacy.description,
                        songs = legacy.songs.orEmpty().distinctBy { it.key },
                        playlists = legacy.playlists.orEmpty().distinctBy { it.key },
                        albums = legacy.albums.orEmpty().distinctBy { it.key },
                        singles = legacy.singles.orEmpty().distinctBy { it.key },
                        songsBrowseId = legacy.songsEndpoint?.browseId,
                        songsParams = legacy.songsEndpoint?.params
                    )
                    allArtistSongsItems = legacy.songs.orEmpty().distinctBy { it.key }

                    runCatching {
                        val existing = Database.artist(browseId).first()
                        val toSave = if (existing != null)
                            existing.copy(
                                name = legacy.name ?: existing.name,
                                thumbnailUrl = legacy.thumbnail?.url ?: existing.thumbnailUrl
                            )
                        else
                            Artist(
                                id = browseId,
                                name = legacy.name,
                                thumbnailUrl = legacy.thumbnail?.url,
                                timestamp = System.currentTimeMillis(),
                                isYoutubeArtist = true
                            )
                        if (existing == null ||
                            existing.name != toSave.name ||
                            existing.thumbnailUrl != toSave.thumbnailUrl
                        ) {
                            Database.upsert(toSave)
                        }
                    }.onFailure { e ->
                        println("[RiPlay-Artista] salvataggio artista fallito: ${e.message}")
                    }
                }
        }
    }
    val artist = artistUi.value

    // lista brani dell'artista (per lo shuffle), mappata al modello Song
    val allArtistSongs = allArtistSongsItems
        .map { it.asSong }
        .distinctBy { it.id }

    var translateEnabled by remember {
        mutableStateOf(false)
    }

    val translator = Translator()
    val languageDestination = languageDestination()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxSize()
        ) {

            // FOTO QUADRATA fissa a sinistra (40%, landscape Android)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            ) {
                artistUi.value?.let {
                    AsyncImage(
                        model = artistUi.value!!.thumbnailUrl?.resize(1200, 1200),
                        contentDescription = ds("common.loading"),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .fadingEdge(
                                top = fadeSpacingTop,
                                bottom = fadeSpacingBottom
                            )
                    )
                } ?: Loader()
            }

            // COLONNA DESTRA scorrevole
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize()
            ) {

                artistUi.value?.let {
                    // NOME grande centrato (Android: AutoResizeText 32..38sp)
                    if (!it.name.isNullOrEmpty())
                        AutoResizeText(
                            text = it.name.toString(),
                            style = typography.displayLarge,
                            fontSizeRange = FontSizeRange(30.sp, 38.sp),
                            fontWeight = typography.displayLarge.fontWeight,
                            fontFamily = typography.displayLarge.fontFamily,
                            color = typography.displayLarge.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                    // RIGA INFO: iscritti (Android: subscriberCountText)
                    it.subscribersText?.let { subscribers ->
                        Text(
                            text = String.format(
                                stringResource(Res.string.artist_subscribers),
                                subscribers
                            ),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // RIGA DI AZIONI: SEGUI (bookmark) + shuffle brani
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    // STEP 3i: pulsante SEGUI (bookmark, stesso meccanismo
                    // degli album preferiti)
                    HeaderActionIcon(
                        icon = if (currentArtist?.bookmarkedAt == null) Icons.Filled.BookmarkBorder
                               else Icons.Filled.Bookmark,
                        contentDescription = if (currentArtist?.bookmarkedAt == null)
                            ds("detail.artist_follow")
                        else ds("detail.artist_followed"),
                        accent = currentArtist?.bookmarkedAt != null,
                        onClick = {
                            val a = currentArtist
                            if (a != null) {
                                coroutineScope.launch {
                                    Database.upsert(
                                        a.copy(
                                            bookmarkedAt =
                                                if (a.bookmarkedAt == null) System.currentTimeMillis()
                                                else null
                                        )
                                    )
                                }
                            }
                        }
                    )

                    HeaderActionIcon(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = ds("detail.artist_shuffle"),
                        enabled = allArtistSongs.isNotEmpty(),
                        onClick = {
                            if (allArtistSongs.isNotEmpty()) {
                                val shuffled = allArtistSongs.shuffled()
                                val collection = onPlayCollection
                                if (collection != null)
                                    collection(shuffled, shuffled.first())
                                else
                                    onSongClick(shuffled.first())
                            }
                        }
                    )
                }

                if (artistUi.value != null) {

                    artistUi.value!!.songs.takeIf { it.isNotEmpty() }?.let { allSongs ->

                        val parentalControlEnabled = false

                        val songs = if (parentalControlEnabled)
                            allSongs.filter { !it.explicit } else allSongs

                        // STEP 3aq: "VEDI TUTTO" dei BRANI (come Android:
                        // Title cliccabile quando la sezione ha un
                        // moreEndpoint). Il titolo va letto FUORI dalla
                        // lambda di click (stringResource e' composable)
                        val songsTitle = stringResource(Res.string.songs)
                        val songsViewAllBrowseId = artistUi.value?.songsBrowseId
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = songsTitle,
                                onClick = songsViewAllBrowseId?.let { bid ->
                                    {
                                        onViewAllSection?.invoke(
                                            ArtistSectionRequest(
                                                artistName = artistUi.value?.name,
                                                sectionName = songsTitle,
                                                browseId = bid,
                                                params = artistUi.value?.songsParams,
                                                // i brani gia' caricati
                                                // (fetch integrale a
                                                // pag load) come fallback
                                                fallbackItems = allArtistSongsItems
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        songs.forEachIndexed { index, song ->

                            SongItem(
                                song = song,
                                modifier = Modifier
                                    .clickable {
                                        onSongClick(song.asSong)
                                    },
                                onAddToQueue = onAddToQueue?.let { action ->
                                    { action(song.asSong) }
                                }
                            )

                        }
                    }

                    artistUi.value!!.playlists.takeIf { it.isNotEmpty() }?.let { playlists ->

                        // STEP 3aq: "VEDI TUTTO" delle PLAYLIST (come
                        // Android: ogni sezione con moreEndpoint apre la
                        // lista completa)
                        val playlistsTitle = stringResource(Res.string.playlists)
                        val playlistsViewAllBrowseId = artistUi.value?.playlistsBrowseId
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = playlistsTitle,
                                onClick = playlistsViewAllBrowseId?.let { bid ->
                                    {
                                        onViewAllSection?.invoke(
                                            ArtistSectionRequest(
                                                artistName = artistUi.value?.name,
                                                sectionName = playlistsTitle,
                                                browseId = bid,
                                                params = artistUi.value?.playlistsParams,
                                                fallbackItems = playlists
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = playlists,
                                key = Environment.PlaylistItem::key
                            ) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    thumbnailSizeDp = playlistThumbnailSize,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = {
                                            onPlaylistClick(playlist.key)
                                        }
                                        )
                                )
                            }
                        }
                    }

                    artistUi.value!!.albums.takeIf { it.isNotEmpty() }?.let { albums ->

                        // STEP 3aq: "VEDI TUTTO" degli ALBUM: il piu'
                        // apre la lista COMPLETA della sezione via
                        // moreEndpoint (stesso flusso di Android);
                        // fallback = discografia gia' caricata
                        val albumsTitle = stringResource(Res.string.albums)
                        val albumsViewAllBrowseId = artistUi.value?.albumsBrowseId
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title2Actions(
                                title = albumsTitle,
                                onClick1 = albumsViewAllBrowseId?.let { bid ->
                                    {
                                        onViewAllSection?.invoke(
                                            ArtistSectionRequest(
                                                artistName = artistUi.value?.name,
                                                sectionName = albumsTitle,
                                                browseId = bid,
                                                params = artistUi.value?.albumsParams,
                                                fallbackItems = albums
                                            )
                                        )
                                    }
                                },
                                icon2 = Res.drawable.dice,
                                onClick2 = {
                                    if (albums.isEmpty()) return@Title2Actions
                                    val albumId = albums.get(
                                        if (albums.size > 1)
                                            Random(System.currentTimeMillis()).nextInt(
                                                0,
                                                albums.size - 1
                                            )
                                        else 0
                                    ).key
                                    onAlbumClick(albumId)
                                }
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = albums,
                                key = Environment.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizeDp = albumThumbnailSize,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) })
                                )
                            }
                        }
                    }

                    artistUi.value!!.singles.takeIf { it.isNotEmpty() }?.let { singles ->

                        // STEP 3aq: "VEDI TUTTO" dei SINGOLI: stessa
                        // meccanica degli album (moreEndpoint della
                        // sezione, fallback = discografia singoli)
                        val singlesTitle = stringResource(Res.string.singles)
                        val singlesViewAllBrowseId = artistUi.value?.singlesBrowseId
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title2Actions(
                                title = singlesTitle,
                                onClick1 = singlesViewAllBrowseId?.let { bid ->
                                    {
                                        onViewAllSection?.invoke(
                                            ArtistSectionRequest(
                                                artistName = artistUi.value?.name,
                                                sectionName = singlesTitle,
                                                browseId = bid,
                                                params = artistUi.value?.singlesParams,
                                                fallbackItems = singles
                                            )
                                        )
                                    }
                                },
                                icon2 = Res.drawable.dice,
                                onClick2 = {
                                    if (singles.isEmpty()) return@Title2Actions
                                    val albumId = singles.get(
                                        if (singles.size > 1)
                                            Random(System.currentTimeMillis()).nextInt(
                                                0,
                                                singles.size - 1
                                            )
                                        else 0
                                    ).key
                                    onAlbumClick(albumId)
                                }
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = singles,
                                key = Environment.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizeDp = albumThumbnailSize,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) })
                                )
                            }

                        }
                    }


                }

                // BIOGRAFIA (Information) in fondo alla colonna destra
                artistUi.value?.description?.let { description ->
                    val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                    Title(
                        title = stringResource(Res.string.information)
                    )

                    var translatedText by remember { mutableStateOf("") }
                    val nonTranslatedText by remember {
                        mutableStateOf(
                            if (attributionsIndex == -1) {
                                description
                            } else {
                                description.substring(0, attributionsIndex)
                            }
                        )
                    }

                    Image(
                        painter = painterResource(Res.drawable.translate),
                        colorFilter = ColorFilter.tint(if (translateEnabled) Color.White else Color.Gray),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable{
                                translateEnabled = !translateEnabled
                            }
                    )

                    if (translateEnabled == true) {
                        LaunchedEffect(Unit) {
                            val result = withContext(Dispatchers.IO) {
                                try {
                                    translator.translate(
                                        nonTranslatedText,
                                        languageDestination,
                                        Language.AUTO
                                    ).translatedText
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            translatedText =
                                if (result.toString() == "kotlin.Unit") "" else result.toString()
                        }
                    } else translatedText = nonTranslatedText

                    Column (
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ){
                        Text(
                            text = "“ $translatedText „",
                            style = TextStyle(
                                fontSize = typography.titleSmall.fontSize,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            )
                        )

                        if (attributionsIndex != -1) {
                            Text(
                                text = stringResource(Res.string.from_wikipedia_cca),
                                style = TextStyle(
                                    fontSize = typography.titleSmall.fontSize,
                                    fontWeight = typography.titleSmall.fontWeight,
                                    color = Color.White,
                                    textAlign = TextAlign.Start
                                )
                            )
                        }
                    }

                }
                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}
