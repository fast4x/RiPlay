package it.fast4x.riplay.ui.pages

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.models.bodies.SearchSuggestionsBody
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.requests.searchSuggestionsWithItems
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.data.Database
// STEP 3ac: preferiti playlist/podcast direttamente dai risultati
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SearchQuery
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.items.AlbumItem
import it.fast4x.riplay.items.ArtistItem
import it.fast4x.riplay.items.PlaylistItem
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.albumThumbnailSize
import it.fast4x.riplay.styling.Dimensions.artistThumbnailSize
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.styling.Dimensions.playlistThumbnailSize
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * STEP 3j: pagina di RICERCA su YouTube Music (l'equivalente desktop della
 * SearchScreen Android, richiesta utente n.3 "aggiungere la ricerca dei
 * brani YT come avviene lato android").
 *
 * Struttura:
 *  - campo di testo con submit a INVIO (o icona di ricerca);
 *  - senza invio: SUGGERIMENTI live (debounce 250 ms, come la OnlineSearch
 *    Android via Environment.searchSuggestionsWithItems) + CRONOLOGIA delle
 *    ricerche (tabella SearchQuery del db, stessa di Android);
 *  - dopo l'invio: risultati divisi per filtri Canzoni / Video / Album /
 *    Artisti / Playlist / Podcast (SearchFilter.InnerTube, come i tab di
 *    SearchResultsContent Android) con le STESE conversioni
 *    (fromMusicShelfRenderer);
 *  - i risultati supportano il caricamento a pagine (continuation, come
 *    l'ItemsPage Android) con il pulsante "Carica altri";
 *  - azioni sui brani: click = riproduci, "+" = aggiungi alla coda;
 *    click su album/artista/playlist = apre il dettaglio (DesktopDetail).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchPage(
    onSongClick: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    // STEP 3n (richiesta utente n.4 "se clicco un brano o un video, occorre
    // popolare la coda con l'elenco presente, direi di fissare il massimo a
    // 50 elementi da inserire in coda"): click su un brano o un video =
    // il brano cliccato parte subito e la LISTA dei risultati correnti
    // entra in coda (il cap 50 e' applicato in onPlayCollection di
    // ThreeColumnsApp, in un unico punto per tutte le raccolte)
    onPlayCollection: (List<Song>, Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    // STEP 3ad: click su una riga PODCAST = apre il dettaglio podcast
    // (DesktopDetail.Podcast -> PodcastScreen, episodi via podcastPage)
    onPodcastClick: (browseId: String, title: String?) -> Unit = { _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf(TextFieldValue("")) }
    // testo confermato con INVIO: se vuoto mostra suggerimenti/cronologia
    var submitted by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(SearchTab.SONGS) }
    var loading by remember { mutableStateOf(false) }

    // risultati per filtro (uno stato per tab, tipi diversi tra loro)
    var songResult by remember { mutableStateOf<Environment.ItemsPage<Environment.SongItem>?>(null) }
    var videoResult by remember { mutableStateOf<Environment.ItemsPage<Environment.VideoItem>?>(null) }
    var albumResult by remember { mutableStateOf<Environment.ItemsPage<Environment.AlbumItem>?>(null) }
    var artistResult by remember { mutableStateOf<Environment.ItemsPage<Environment.ArtistItem>?>(null) }
    var playlistResult by remember { mutableStateOf<Environment.ItemsPage<Environment.PlaylistItem>?>(null) }
    // STEP 3ac: risultati PODCAST (show: stesso tipo PlaylistItem, come fa
    // la ricerca Android con SearchFilter.Podcast)
    var podcastResult by remember { mutableStateOf<Environment.ItemsPage<Environment.PlaylistItem>?>(null) }

    // STEP 3ac: playlist/podcast GIA' salvati in libreria (il pulsante
    // preferiti sulle righe consulta questo set; il Flow del db rende
    // reattivo lo stato dopo ogni salvataggio/rimozione)
    val savedPlaylists by Database.playlistPreviews(PlaylistSortBy.Name, SortOrder.Ascending)
        .collectAsState(initial = emptyList())
    val savedPlaylistIds = savedPlaylists.mapNotNull { it.playlist.browseId }.toSet()

    // suggerimenti live mentre si digita (debounce, come Android)
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // cronologia ricerche nel db (gia' filtrata per il testo digitato,
    // come la SearchScreen Android: Database.queries("%...%"))
    val history by Database.queries("%${query.text.trim()}%")
        .collectAsState(initial = emptyList())

    LaunchedEffect(query.text) {
        val text = query.text.trim()
        if (text.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        suggestions = Environment
            .searchSuggestionsWithItems(SearchSuggestionsBody(input = text))
            ?.getOrNull()
            ?.queries
            .orEmpty()
            .take(8)
    }

    LaunchedEffect(submitted, activeTab) {
        if (submitted.isBlank()) return@LaunchedEffect
        loading = true
        try {
            when (activeTab) {
                SearchTab.SONGS -> songResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.Song.value),
                    Environment.SongItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()

                SearchTab.VIDEOS -> videoResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.Video.value),
                    Environment.VideoItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()

                SearchTab.ALBUMS -> albumResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.Album.value),
                    Environment.AlbumItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()

                SearchTab.ARTISTS -> artistResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.Artist.value),
                    Environment.ArtistItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()

                SearchTab.PLAYLISTS -> playlistResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.CommunityPlaylist.value),
                    Environment.PlaylistItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()

                // STEP 3ac: STESSO renderer di Android (PlaylistItem ::
                // fromMusicShelfRenderer) con il filtro Podcast di Innertube
                SearchTab.PODCASTS -> podcastResult = Environment.searchPage(
                    SearchBody(query = submitted, params = Environment.SearchFilter.Podcast.value),
                    Environment.PlaylistItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()
            }
            println("[RiPlay-Search] '$submitted' [${activeTab.label}] caricata pagina 1")
        } finally {
            loading = false
        }
    }

    fun currentCount(): Int = when (activeTab) {
        SearchTab.SONGS -> songResult?.items.orEmpty().size
        SearchTab.VIDEOS -> videoResult?.items.orEmpty().size
        SearchTab.ALBUMS -> albumResult?.items.orEmpty().size
        SearchTab.ARTISTS -> artistResult?.items.orEmpty().size
        SearchTab.PLAYLISTS -> playlistResult?.items.orEmpty().size
        SearchTab.PODCASTS -> podcastResult?.items.orEmpty().size
    }

    fun currentContinuation(): String? = when (activeTab) {
        SearchTab.SONGS -> songResult?.continuation
        SearchTab.VIDEOS -> videoResult?.continuation
        SearchTab.ALBUMS -> albumResult?.continuation
        SearchTab.ARTISTS -> artistResult?.continuation
        SearchTab.PLAYLISTS -> playlistResult?.continuation
        SearchTab.PODCASTS -> podcastResult?.continuation
    }

    // "Carica altri": appende la pagina successiva alla lista corrente
    fun loadMore() {
        coroutineScope.launch {
            loading = true
            try {
                when (activeTab) {
                    SearchTab.SONGS -> {
                        val page = songResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.SongItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        songResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }

                    SearchTab.VIDEOS -> {
                        val page = videoResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.VideoItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        videoResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }

                    SearchTab.ALBUMS -> {
                        val page = albumResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.AlbumItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        albumResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }

                    SearchTab.ARTISTS -> {
                        val page = artistResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.ArtistItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        artistResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }

                    SearchTab.PLAYLISTS -> {
                        val page = playlistResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.PlaylistItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        playlistResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }

                    // STEP 3ac: pagine successive anche per i podcast
                    SearchTab.PODCASTS -> {
                        val page = podcastResult ?: return@launch
                        val cont = page.continuation ?: return@launch
                        val more = Environment.searchPage(
                            ContinuationBody(continuation = cont),
                            Environment.PlaylistItem.Companion::fromMusicShelfRenderer
                        )?.getOrNull() ?: return@launch
                        podcastResult = Environment.ItemsPage(
                            items = (page.items.orEmpty() + more.items.orEmpty()).distinctBy { it.key },
                            continuation = more.continuation
                        )
                    }
                }
                println("[RiPlay-Search] '$submitted' [${activeTab.label}] loadMore -> ${currentCount()} risultati")
            } finally {
                loading = false
            }
        }
    }

    // submit: conferma la ricerca e la salva nella cronologia (db)
    fun submit(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        query = TextFieldValue(t)
        submitted = t
        coroutineScope.launch {
            runCatching { Database.insert(SearchQuery(query = t)) }
        }
    }

    // STEP 3ac: PREFERITI playlist/podcast dalla riga dei risultati (stessa
    // semantica del bookmark nella pagina di dettaglio: la playlist entra
    // nel db e i brani vengono scaricati in background.
    // STEP 3ad: per i PODCAST (browseId MPSP) Environment.playlistPage NON
    // restituisce brani (risposta twoColumnBrowseResultsRenderer, non una
    // pagina playlist): niente import, la Playlist resta salvata con
    // isPodcast = true e gli episodi si vedono dal dettaglio LIVE
    // (PodcastScreen, Environment.podcastPage come su Android).
    fun toggleSavedPlaylist(browseId: String, item: Environment.PlaylistItem) {
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = Database.playlistWithBrowseId(browseId)
                if (existing != null) {
                    Database.delete(existing)
                    println("[RiPlay-Search] preferiti: rimossa '${existing.name}' ($browseId)")
                } else {
                    val isPodcast = activeTab == SearchTab.PODCASTS
                    val title = item.title.orEmpty().ifBlank { browseId }
                    val playlistId = Database.insert(
                        Playlist(
                            name = title,
                            browseId = browseId,
                            isYoutubePlaylist = true,
                            isEditable = false,
                            isPodcast = isPodcast
                        )
                    )
                    println("[RiPlay-Search] preferiti: salvata '$title' ($browseId)")
                    // brani in background (come il bookmark del dettaglio):
                    // fallimenti silenziosi, la playlist resta in libreria
                    if (!isPodcast) {
                        val page = Environment.playlistPage(
                            BrowseBody(browseId = browseId)
                        )?.completed()?.getOrNull()
                        val songs = page?.songsPage?.items
                            .orEmpty()
                            .map { it.asSong }
                            .distinctBy { it.id }
                        songs.forEachIndexed { index, song ->
                            Database.upsert(song)
                            Database.insert(
                                SongPlaylistMap(
                                    songId = song.id,
                                    playlistId = playlistId,
                                    position = index
                                ).default()
                            )
                        }
                    }
                }
            }.onFailure { e ->
                println("[RiPlay-Search] preferiti '$browseId' errore: ${e.message}")
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {

        // ── Campo di ricerca ────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .clickable { }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = RiPlayAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(RiPlayAccent),
                decorationBox = { inner ->
                    Box {
                        if (query.text.isEmpty())
                            Text(
                                ds("search.hint"),
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        inner()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            submit(query.text)
                            true
                        } else false
                    }
            )
            if (query.text.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = ds("search.clear"),
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            query = TextFieldValue("")
                            submitted = ""
                            suggestions = emptyList()
                        }
                )
            }
        }

        if (submitted.isBlank()) {
            // ── Cronologia + suggerimenti (prima della ricerca) ─────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                if (history.isNotEmpty()) {
                    Text(
                        ds("search.recent"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(6.dp))
                    history.forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { submit(entry.query) }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                entry.query,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.size(14.dp))
                    Text(
                        ds("search.suggestions"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(6.dp))
                    suggestions.forEach { suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { submit(suggestion) }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                suggestion,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (history.isEmpty() && suggestions.isEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Digita per cercare brani, video, album, artisti, playlist e podcast su YouTube Music.\n\nLa cronologia delle ricerche viene salvata qui (come sull'app Android).",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        } else {
            // ── Filtri (tab) ────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                SearchTab.values().forEach { tab ->
                    val active = tab == activeTab
                    Text(
                        text = tab.label,
                        color = if (active) Color.Black else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) RiPlayAccent else Color.White.copy(alpha = 0.08f))
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            // ── Risultati ───────────────────────────────────────────────
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                when (activeTab) {

                    // STEP 3n: click su un brano = parte SUBITO e TUTTA la
                    // lista dei risultati correnti entra in coda (max 50)
                    SearchTab.SONGS -> {
                        val songsInResults = songResult?.items.orEmpty().map { it.asSong }
                        items(
                            items = songResult?.items.orEmpty(),
                            key = { it.key }
                        ) { songItem ->
                            val clickedSong = songItem.asSong
                            SongItem(
                                song = songItem,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {},
                                        onClick = { onPlayCollection(songsInResults, clickedSong) }
                                    ),
                                onAddToQueue = { onAddToQueue(clickedSong) }
                            )
                        }
                    }

                    // STEP 3n: stesso comportamento per i VIDEO (la lista dei
                    // video correnti entra in coda, convertita a brani)
                    SearchTab.VIDEOS -> {
                        val videosInResults = videoResult?.items.orEmpty().map { it.toSong() }
                        items(
                            items = videoResult?.items.orEmpty(),
                            key = { it.key }
                        ) { videoItem ->
                            val videoSong = videoItem.toSong()
                            SongItem(
                                songEntity = SongEntity(
                                    song = videoSong,
                                    contentLength = null,
                                    albumTitle = null
                                ),
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {},
                                        onClick = { onPlayCollection(videosInResults, videoSong) }
                                    ),
                                onAddToQueue = { onAddToQueue(videoSong) }
                            )
                        }
                    }

                    SearchTab.ALBUMS -> items(
                        items = albumResult?.items.orEmpty(),
                        key = { it.key }
                    ) { albumItem ->
                        AlbumItem(
                            album = albumItem,
                            thumbnailSizeDp = albumThumbnailSize,
                            alternative = true,
                            modifier = Modifier
                                .clickable { onAlbumClick(albumItem.key) }
                        )
                    }

                    SearchTab.ARTISTS -> items(
                        items = artistResult?.items.orEmpty(),
                        key = { it.key }
                    ) { artistItem ->
                        ArtistItem(
                            artist = artistItem,
                            thumbnailSizeDp = artistThumbnailSize,
                            alternative = true,
                            modifier = Modifier
                                .clickable { onArtistClick(artistItem.key) }
                        )
                    }

                    SearchTab.PLAYLISTS -> items(
                        items = playlistResult?.items.orEmpty(),
                        key = { it.key }
                    ) { playlistItem ->
                        // STEP 3ac: PREFERITI sulla riga (come il bookmark del
                        // dettaglio): un click salva in libreria, l'altro toglie
                        val saved = playlistItem.key in savedPlaylistIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlaylistItem(
                                playlist = playlistItem,
                                thumbnailSizeDp = playlistThumbnailSize,
                                alternative = true,
                                showSongsCount = false,
                                modifier = Modifier
                                    .clickable { onPlaylistClick(playlistItem.key) }
                                    .weight(1f)
                            )
                            IconButton(
                                onClick = { toggleSavedPlaylist(playlistItem.key, playlistItem) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (saved) ds("detail.playlist_favorite_ok")
                                    else ds("detail.playlist_favorite_add"),
                                    tint = if (saved) RiPlayAccent else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // STEP 3ac (riveduto 3ad): risultati PODCAST (show).
                    // La riga CLICCABILE apre il dettaglio podcast dedicato
                    // (PodcastScreen via DesktopDetail.Podcast: i browseId
                    // MPSP non sono pagine playlist, il parser desktop
                    // playlistPage non li copre); il pulsante PREFERITI
                    // salva il podcast in libreria (Playlist.isPodcast =
                    // true), il dettaglio live mostra sempre gli episodi
                    SearchTab.PODCASTS -> items(
                        items = podcastResult?.items.orEmpty(),
                        key = { it.key }
                    ) { podcastItem ->
                        val saved = podcastItem.key in savedPlaylistIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlaylistItem(
                                playlist = podcastItem,
                                thumbnailSizeDp = playlistThumbnailSize,
                                alternative = true,
                                showSongsCount = false,
                                modifier = Modifier
                                    .clickable {
                                        onPodcastClick(podcastItem.key, podcastItem.title)
                                    }
                                    .weight(1f)
                            )
                            IconButton(
                                onClick = { toggleSavedPlaylist(podcastItem.key, podcastItem) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (saved) ds("detail.playlist_favorite_ok")
                                    else ds("detail.playlist_favorite_add"),
                                    tint = if (saved) RiPlayAccent else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // stato vuoto
                if (!loading && currentCount() == 0) {
                    item {
                        Text(
                            "Nessun risultato per \"$submitted\" in \"${activeTab.label}\".",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }

                // caricamento pagina successiva o pulsante "Carica altri"
                if (currentContinuation() != null) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                        ) {
                            if (loading) {
                                Loader()
                            } else {
                                Text(
                                    ds("search.load_more"),
                                    color = RiPlayAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(RiPlayAccent.copy(alpha = 0.13f))
                                        .clickable { loadMore() }
                                        .padding(horizontal = 22.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                } else if (loading && currentCount() == 0) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            Loader()
                        }
                    }
                }
            }
        }
    }
}

/** filtri di ricerca (SearchFilter InnerTube, stessi valori di Android) */
private enum class SearchTab(val label: String) {
    SONGS("Canzoni"),
    VIDEOS("Video"),
    ALBUMS("Album"),
    ARTISTS("Artisti"),
    PLAYLISTS("Playlist"),
    // STEP 3ac: show podcast (stesso filtro Innertube della ricerca Android)
    PODCASTS("Podcast")
}

/** conversione VideoItem -> Song (per il play e la coda, come i brani) */
private fun Environment.VideoItem.toSong(): Song =
    Song(
        id = key,
        title = info?.name ?: "",
        artistsText = authors?.joinToString(", ") { it.name ?: "" },
        durationText = durationText,
        thumbnailUrl = thumbnail?.url
    )
