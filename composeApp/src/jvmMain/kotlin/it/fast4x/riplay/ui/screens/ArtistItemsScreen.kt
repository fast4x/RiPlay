package it.fast4x.riplay.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.requests.ArtistItemsPage
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.AlbumItem
import it.fast4x.riplay.items.ArtistItem
import it.fast4x.riplay.items.PlaylistItem
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.albumThumbnailSize
import it.fast4x.riplay.styling.Dimensions.artistThumbnailSize
import it.fast4x.riplay.styling.Dimensions.playlistThumbnailSize
import it.fast4x.riplay.ui.components.HeaderActionIcon
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.Title
import it.fast4x.riplay.utils.LoadImage
import it.fast4x.riplay.utils.asSong
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.chevron_down
import riplay.composeapp.generated.resources.enqueue_all
import riplay.composeapp.generated.resources.shuffle
import riplay.composeapp.generated.resources.view_all

/**
 * STEP 3aq (richiesta utente: "procedi con la view all come avviene in
 * android"): Richiesta di apertura del "VEDI TUTTO" di una sezione della
 * pagina artista (l'equivalente desktop di ArtistOverviewItems di Android).
 *
 * Viene prodotta da ArtistScreen quando l'utente clicca la freccia di una
 * sezione (Brani / Albums / Singles / Playlists) e contiene TUTTO ciò che
 * serve alla schermata di lista completa:
 *  - [browseId]/[params] = il moreEndpoint della sezione (stesso meccanismo
 *    di Android: browseId = section.moreEndpoint?.browseId, params =
 *    section.moreEndpoint?.params, quindi EnvironmentExt.getArtistItemsPage
 *    + completed() per la lista INTEGRALE oltre la prima pagina);
 *  - [fallbackItems] = gli item GIA' presenti nella pagina artista
 *    (discografia completa / brani gia' caricati): usati subito e come
 *    rete di sicurezza se la chiamata di rete fallisce o non arriva nulla.
 */
data class ArtistSectionRequest(
    val artistName: String?,
    val sectionName: String?,
    val browseId: String,
    val params: String? = null,
    val fallbackItems: List<Environment.Item> = emptyList()
)

/**
 * Un Environment.Item trasformato nel modello Song riproducibile del
 * desktop. Le liste "vedi tutto" possono contenere anche VideoItem
 * (il parser ArtistItemsPage li produce per i contenuti video): anche
 * quelli diventano riproducibili come brani (stesso comportamento che
 * Android ottiene con item.asMediaItem).
 */
private fun Environment.Item.toPlayableSong(): Song? = when (this) {
    is Environment.SongItem -> asSong
    is Environment.VideoItem ->
        Song(
            id = key,
            title = info?.name ?: "",
            artistsText = authors?.joinToString(", ") { it.name ?: "" },
            durationText = durationText,
            thumbnailUrl = thumbnail?.url
        )
    else -> null
}

/**
 * STEP 3aq: schermata "VEDI TUTTO" di una sezione artista, CALCO di
 * ArtistOverviewItems Android:
 *  - fetch INTEGRALE della lista: EnvironmentExt.getArtistItemsPage(
 *    BrowseEndpoint(browseId, params)) + .completed() che segue le
 *    pagine di continuazione (come Android);
 *  - se il primo item e' un BRANO -> lista verticale di brani con la
 *    riga di azioni (shuffle / riproduci tutti / metti in coda tutto)
 *    e click su un brano = riparte da li' con il resto in coda
 *    (forcePlayAtIndex di Android = onPlayCollection del desktop);
 *  - altrimenti -> GRIGLIA adattiva di album / playlist / artisti
 *    (LazyVerticalGrid con GridCells.Adaptive come Android) dove ogni
 *    item apre il dettaglio corrispondente;
 *  - header con il nome dell'artista (Title con chevron_down = chiudi,
 *    come Android) e il nome della sezione.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistItemsScreen(
    request: ArtistSectionRequest,
    onSongClick: (Song) -> Unit,
    // click su un brano = riproduci da quello con il RESTO della lista in
    // coda (equivalente desktop di forcePlayAtIndex di Android)
    onPlayCollection: ((List<Song>, Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onEnqueueAll: ((List<Song>) -> Unit)? = null,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    // pagina completa della sezione (fetch integrale con continuazioni)
    var itemsPage by remember(request) {
        mutableStateOf<ArtistItemsPage?>(null)
    }
    var loading by remember(request) { mutableStateOf(true) }

    LaunchedEffect(request) {
        // stesso schema di ArtistOverviewItems Android: LAUNCHED EFFECT
        // che scarica la pagina completa della sezione
        if (request.browseId.isBlank()) {
            // nessun endpoint (sezione senza moreEndpoint): si usano i
            // fallbackItems gia' presenti nella pagina artista
            loading = false
            return@LaunchedEffect
        }
        itemsPage = runCatching {
            EnvironmentExt.getArtistItemsPage(
                BrowseEndpoint(
                    browseId = request.browseId,
                    params = request.params
                )
            ).completed().getOrNull()
        }.getOrNull()
        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize()) {
            Loader(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            )
        }
        return
    }

    // la lista completa scaricata Vince, altrimenti i fallbackItems
    // (discografia / brani gia' caricati dalla pagina artista)
    val items: List<Environment.Item> =
        itemsPage?.items?.takeIf { it.isNotEmpty() } ?: request.fallbackItems

    // riga di azioni SOLO per la lista brani (come Android: shuffle,
    // enqueue, play; la griglia album ha solo le singole aperture)
    val songRows: List<Pair<Environment.Item, Song>> = items
        .mapNotNull { item -> item.toPlayableSong()?.let { item to it } }

    if (items.firstOrNull() is Environment.SongItem) {
        // ─────────────────────────────────────────────────────────────
        // LISTA BRANI (Android: LazyColumn di SongItem con azioni)
        // ─────────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "artist-items-header") {
                Column {
                    Title(
                        title = request.artistName ?: "",
                        icon = Res.drawable.chevron_down,
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    request.sectionName?.let { section ->
                        Text(
                            text = section,
                            style = typography.titleSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                    ) {
                        // SHUFFLE: l'intera lista mescolata in coda (come
                        // lo shuffle della pagina artista)
                        HeaderActionIcon(
                            icon = Icons.Filled.Shuffle,
                            contentDescription = stringResource(Res.string.shuffle),
                            enabled = songRows.isNotEmpty(),
                            onClick = {
                                if (songRows.isNotEmpty()) {
                                    val shuffled = songRows.map { it.second }.shuffled()
                                    val collection = onPlayCollection
                                    if (collection != null)
                                        collection(shuffled, shuffled.first())
                                    else
                                        onSongClick(shuffled.first())
                                }
                            }
                        )

                        // RIPRODUCI TUTTO: parte dal primo brano con il
                        // resto in coda (il "play" dell'header Android)
                        HeaderActionIcon(
                            icon = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(Res.string.view_all),
                            enabled = songRows.isNotEmpty(),
                            onClick = {
                                if (songRows.isNotEmpty()) {
                                    val all = songRows.map { it.second }
                                    val collection = onPlayCollection
                                    if (collection != null)
                                        collection(all, all.first())
                                    else
                                        onSongClick(all.first())
                                }
                            }
                        )

                        // METTI IN CODA TUTTO senza avviare (uniformita'
                        // con album/playlist del desktop)
                        HeaderActionIcon(
                            icon = Icons.Filled.Add,
                            contentDescription = stringResource(Res.string.enqueue_all),
                            enabled = songRows.isNotEmpty(),
                            onClick = {
                                if (songRows.isNotEmpty())
                                    onEnqueueAll?.invoke(songRows.map { it.second })
                            }
                        )
                    }
                }
            }

            items(songRows) { (item, song) ->
                val rowModifier = Modifier.clickable {
                    val collection = onPlayCollection
                    if (collection != null)
                        collection(songRows.map { it.second }, song)
                    else
                        onSongClick(song)
                }
                when (item) {
                    is Environment.SongItem ->
                        SongItem(
                            song = item,
                            modifier = rowModifier,
                            onAddToQueue = onAddToQueue?.let { action ->
                                { action(song) }
                            }
                        )

                    is Environment.VideoItem ->
                        // i video della lista diventano righe brano
                        // riproducibili (thumbnail via LoadImage)
                        SongItem(
                            title = item.info?.name,
                            authors = item.authors
                                ?.joinToString(", ") { it.name ?: "" },
                            duration = item.durationText,
                            thumbnailSizeDp = 50.dp,
                            modifier = rowModifier,
                            thumbnailContent = {
                                item.thumbnail?.url?.let { url ->
                                    LoadImage(url)
                                }
                            },
                            onAddToQueue = onAddToQueue?.let { action ->
                                { action(song) }
                            },
                            songId = item.key
                        )

                    else -> {}
                }
            }

            item(key = "artist-items-bottom-space") {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    } else {
        // ─────────────────────────────────────────────────────────────
        // GRIGLIA (Android: LazyVerticalGrid di Album/Playlist/Artist)
        // ─────────────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Adaptive(albumThumbnailSize + 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "artist-items-header") {
                Column {
                    Title(
                        title = request.artistName ?: "",
                        icon = Res.drawable.chevron_down,
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    request.sectionName?.let { section ->
                        Text(
                            text = section,
                            style = typography.titleSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            items(items) { item ->
                when (item) {
                    is Environment.AlbumItem ->
                        AlbumItem(
                            album = item,
                            thumbnailSizeDp = albumThumbnailSize,
                            alternative = true,
                            modifier = Modifier.clickable {
                                onAlbumClick(item.key)
                            }
                        )

                    is Environment.PlaylistItem ->
                        PlaylistItem(
                            playlist = item,
                            thumbnailSizeDp = playlistThumbnailSize,
                            alternative = true,
                            modifier = Modifier.clickable {
                                onPlaylistClick(item.key)
                            }
                        )

                    is Environment.ArtistItem ->
                        ArtistItem(
                            artist = item,
                            thumbnailSizeDp = artistThumbnailSize,
                            alternative = true,
                            modifier = Modifier.clickable {
                                onArtistClick(item.key)
                            }
                        )

                    is Environment.SongItem ->
                        // sezione mista: un brano dentro la griglia resta
                        // riproducibile (riga compatta come le altre celle)
                        SongItem(
                            song = item,
                            modifier = Modifier.clickable {
                                item.asSong.let { song ->
                                    val collection = onPlayCollection
                                    if (collection != null)
                                        collection(songRows.map { it.second }, song)
                                    else
                                        onSongClick(song)
                                }
                            }
                        )

                    else -> {}
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "artist-items-bottom-space") {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
