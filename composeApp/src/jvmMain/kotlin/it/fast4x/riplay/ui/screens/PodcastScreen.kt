package it.fast4x.riplay.ui.screens

import it.fast4x.riplay.i18n.ds

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.podcastPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.fadeSpacingBottom
import it.fast4x.riplay.styling.Dimensions.fadeSpacingTop
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.ui.components.AutoResizeText
import it.fast4x.riplay.ui.components.FontSizeRange
import it.fast4x.riplay.ui.components.HeaderActionIcon
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.Title
import it.fast4x.riplay.utils.LoadImage
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * STEP 3ad (seguimento di 3ac, segnalazione utente "non posso cliccarci
 * sopra ... se lo inserisco tra i preferiti si apre ma e' vuota la lista
 * degli episodi"): dettaglio PODCAST (show) del desktop, equivalente della
 * rotta NavRoutes.podcast di Android (ui/screens/podcast/Podcast.kt).
 *
 * Perche' una schermata dedicata: i browseId podcast (MPSP...) NON sono
 * pagine playlist di YouTube Music (twoColumnBrowseResultsRenderer con
 * musicShelfRenderer di episodi), quindi Environment.playlistPage usato
 * da PlaylistScreen non restituisce brani (era la causa della lista
 * vuota). Qui si usa Environment.podcastPage, lo stesso parser usato da
 * Android, che estrae la lista EPISODI (videoId normali: la riproduzione
 * e la coda restano quelle standard).
 *
 * Comportamento:
 *  - caricamento LIVE ad ogni apertura (i podcast pubblicano episodi
 *    nuovi: nessuno snapshot nel db che diventerebbe obsoleto);
 *  - click su un episodio = come le playlist YT: TUTTI gli episodi
 *    entrano in coda e parte quello cliccato; "+" = solo in coda;
 *  - BOOKMARK salva/rimuove il podcast in libreria (Playlist con
 *    isPodcast = true, compare nella pagina Playlists); come su Android
 *    il bookmark NON importa gli episodi nel db (la schermata e' live);
 *  - shuffle e "metti in coda tutto" sulla lista episodi.
 */
@Composable
fun PodcastScreen(
    browseId: String,
    // titolo di cortesia dai risultati di ricerca / playlist salvata,
    // usato finche' la pagina non e' caricata
    fallbackTitle: String? = null,
    onSongClick: (Song) -> Unit,
    onClosePage: () -> Unit = {},
    // azioni coda (stesse firme di PlaylistScreen)
    onAddToQueue: ((Song) -> Unit)? = null,
    onPlayCollection: ((List<Song>, Song) -> Unit)? = null,
    onEnqueueAll: ((List<Song>) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // pagina podcast live (Environment.Podcast con la lista episodi)
    var podcastPage by remember { mutableStateOf<Environment.Podcast?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(browseId) {
        loading = true
        podcastPage = withContext(Dispatchers.IO) {
            runCatching {
                Environment.podcastPage(BrowseBody(browseId = browseId)).getOrNull()
            }.getOrNull()
        }
        loading = false
        println("[RiPlay-Podcast] '$browseId' episodi: ${podcastPage?.listEpisode?.size ?: "load ko"}")
    }

    // BOOKMARK: il podcast e' GIA' salvato in libreria? (stesso schema di
    // PlaylistScreen: saveCheck riverifica dopo salvataggio/rimozione)
    var savedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var saveCheck by remember { mutableStateOf(0) }
    LaunchedEffect(browseId, saveCheck) {
        savedPlaylist = Database.playlistWithBrowseId(browseId)
    }

    // episodi convertiti in Song (videoId normali: play e coda standard)
    val allEpisodeSongs = podcastPage
        ?.listEpisode
        .orEmpty()
        .filter { it.videoId.isNotBlank() }
        .map { it.toSong() }
        .distinctBy { it.id }

    val displayTitle = podcastPage?.title?.takeIf { it.isNotBlank() }
        ?: fallbackTitle.orEmpty().ifBlank { browseId }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxSize()
        ) {

            // MINIATURA QUADRATA fissa a sinistra (40%, come PlaylistScreen)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            ) {
                val thumbUrl = podcastPage?.thumbnail
                    ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
                    ?.url
                if (thumbUrl != null) {
                    AsyncImage(
                        model = thumbUrl.resize(1200, 1200),
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .fadingEdge(
                                top = fadeSpacingTop,
                                bottom = fadeSpacingBottom
                            )
                    )
                } else if (loading) {
                    Loader()
                }
            }

            // COLONNA DESTRA scorrevole
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize()
            ) {

                // TITOLO grande centrato (come PlaylistScreen)
                AutoResizeText(
                    text = displayTitle,
                    style = typography.displayLarge,
                    fontSizeRange = FontSizeRange(26.sp, 36.sp),
                    fontWeight = typography.displayLarge.fontWeight,
                    fontFamily = typography.displayLarge.fontFamily,
                    color = typography.displayLarge.color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // AUTORE (strapline della pagina podcast, se presente)
                podcastPage?.author?.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        text = author,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // RIGA INFO centrata ("N episodi")
                Text(
                    text = "${allEpisodeSongs.size} " + ds("podcast.episodes"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // RIGA DI AZIONI (bookmark, shuffle, enqueue)
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    HeaderActionIcon(
                        icon = if (savedPlaylist == null) Icons.Filled.BookmarkBorder
                               else Icons.Filled.Bookmark,
                        contentDescription = if (savedPlaylist == null)
                            ds("detail.playlist_favorite_add")
                        else ds("detail.playlist_favorite_ok"),
                        accent = savedPlaylist != null,
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                runCatching {
                                    val existing = savedPlaylist
                                    if (existing == null) {
                                        // SALVA IN LIBRERIA: Playlist con
                                        // isPodcast = true (come il tab
                                        // Podcast della ricerca). Nessun
                                        // import episodi: la schermata e'
                                        // live, come su Android.
                                        Database.insert(
                                            Playlist(
                                                name = displayTitle,
                                                browseId = browseId,
                                                isYoutubePlaylist = true,
                                                isEditable = false,
                                                isPodcast = true
                                            )
                                        )
                                        println("[RiPlay-Podcast] salvato in libreria '$displayTitle' ($browseId)")
                                    } else {
                                        Database.delete(existing)
                                        println("[RiPlay-Podcast] rimosso da libreria '$browseId'")
                                    }
                                }.onFailure { e ->
                                    println("[RiPlay-Podcast] bookmark '$browseId' errore: ${e.message}")
                                }
                                saveCheck++
                            }
                        }
                    )

                    HeaderActionIcon(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = ds("detail.playlist_shuffle"),
                        enabled = allEpisodeSongs.isNotEmpty(),
                        onClick = {
                            if (allEpisodeSongs.isNotEmpty()) {
                                val shuffled = allEpisodeSongs.shuffled()
                                val collection = onPlayCollection
                                if (collection != null)
                                    collection(shuffled, shuffled.first())
                                else
                                    onSongClick(shuffled.first())
                            }
                        }
                    )

                    HeaderActionIcon(
                        icon = Icons.Filled.PlaylistAdd,
                        contentDescription = ds("action.enqueue_playlist"),
                        enabled = allEpisodeSongs.isNotEmpty(),
                        onClick = {
                            if (allEpisodeSongs.isNotEmpty()) onEnqueueAll?.invoke(allEpisodeSongs)
                        }
                    )
                }

                // LISTA EPISODI (come le righe brani di PlaylistSongsPage)
                if (podcastPage != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Title(
                            title = ds("podcast.episodes").replaceFirstChar { it.uppercase() }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    allEpisodeSongs.forEach { episodeSong ->
                        val original = podcastPage?.listEpisode
                            ?.firstOrNull { it.videoId == episodeSong.id }
                        SongItem(
                            title = episodeSong.title,
                            authors = listOfNotNull(
                                original?.createdDay?.takeIf { it.isNotBlank() },
                                episodeSong.durationText?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                                .ifBlank { episodeSong.artistsText },
                            duration = null,
                            thumbnailSizeDp = 50.dp,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // STEP 3j: TUTTI GLI EPISODI IN CODA
                                    // (come le playlist YT); il "+"
                                    // resta per il singolo episodio
                                    val collection = onPlayCollection
                                    if (collection != null && allEpisodeSongs.size > 1) {
                                        collection(allEpisodeSongs, episodeSong)
                                    } else {
                                        onSongClick(episodeSong)
                                    }
                                },
                            thumbnailContent = {
                                episodeSong.thumbnailUrl?.let { LoadImage(it) }
                            },
                            // STEP 3n: spunta accentata se l'episodio e'
                            // gia' in coda
                            songId = episodeSong.id,
                            onAddToQueue = { onAddToQueue?.invoke(episodeSong) }
                        )
                    }
                } else if (loading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp)
                    ) {
                        Loader()
                    }
                }

                // DESCRIZIONE del podcast in fondo (se presente)
                podcastPage?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Title(
                            title = ds("podcast.description")
                        )
                    }
                    Text(
                        text = description,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}

/** conversione episodio podcast -> Song (videoId normale, come i video) */
private fun Environment.Podcast.EpisodeItem.toSong(): Song =
    Song(
        id = videoId,
        title = title,
        artistsText = author,
        durationText = durationString,
        thumbnailUrl = thumbnail
            .maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
            ?.url
    )
