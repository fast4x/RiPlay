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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.items.AlbumItem
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.albumThumbnailSize
import it.fast4x.riplay.styling.Dimensions.fadeSpacingBottom
import it.fast4x.riplay.styling.Dimensions.fadeSpacingTop
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.ui.components.AutoResizeText
import it.fast4x.riplay.ui.components.FontSizeRange
import it.fast4x.riplay.ui.components.HeaderActionIcon
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.components.Title
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.completed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.album_alternative_versions
import riplay.composeapp.generated.resources.from_wikipedia_cca
import riplay.composeapp.generated.resources.information
import riplay.composeapp.generated.resources.songs
import riplay.composeapp.generated.resources.translate

/**
 * STEP 3o (richiesta utente n.2 "modificare la pagina di dettaglio di
 * artista, album e playlist a quella lato android"): ristrutturata come
 * PlaylistSongList di Android in LANDSCAPE:
 *  - a SINISTRA la miniatura QUADRATA fissa (40%, 1:1, con bordo sfumato);
 *  - a DESTRA colonna scorrevole: titolo grande centrato (32..38sp), riga
 *    info ("N canzoni"), RIGA DI AZIONI (bookmark, shuffle, metti tutto in
 *    coda), brani, versioni alternative, descrizione.
 *
 * STEP 3o (richiesta utente n.1 "aggiungere alla playlist la possibilita'
 * di inserirla tra le playlist preferite come avviene con artista e
 * album"): pulsante BOOKMARK nella riga di azioni. Come fa Android
 * (PlaylistSongList: "save youtube library"), PREFERIRE una playlist YT
 * significa SALVARLA IN LIBRERIA: viene inserita nel db come Playlist
 * (name = titolo, browseId, isYoutubePlaylist = true, isEditable = false)
 * con TUTTI i suoi brani come SongPlaylistMap; appare quindi nella pagina
 * Playlists (il click su una playlist con browseId riapre questa pagina).
 * Un secondo click la RIMUOVE dalla libreria (Database.delete).
 */
@Composable
fun PlaylistScreen(
    browseId: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onClosePage: () -> Unit,
    // STEP 3i: azione "aggiungi alla coda" sui brani della playlist
    onAddToQueue: ((Song) -> Unit)? = null,
    // STEP 3j: come per gli album, cliccando un brano della playlist YT
    // l'INTERA lista va in coda e parte il brano cliccato
    onPlayCollection: ((List<Song>, Song) -> Unit)? = null,
    // STEP 3o (richiesta utente n.2): "metti in coda tutto" senza avviare
    onEnqueueAll: ((List<Song>) -> Unit)? = null
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.IO) }
    var playlistPage by remember { mutableStateOf<Environment.PlaylistOrAlbumPage?>(null) }
    var playlistSongs by remember { mutableStateOf<List<Environment.SongItem?>>(emptyList()) }
    val parentalControlEnabled by remember{ mutableStateOf(false)}
    LaunchedEffect(Unit) {
        if (playlistPage != null && playlistPage!!.songsPage?.continuation == null) return@LaunchedEffect

        playlistPage = withContext(Dispatchers.IO) {
            Environment.playlistPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
        }

        println("mediaItem playlistPage ${playlistPage?.songsPage}")

        playlistSongs = if (parentalControlEnabled)
            playlistPage?.songsPage?.items?.filter { !it.asSong.title.startsWith(EXPLICIT_PREFIX) }!!
        else playlistPage?.songsPage?.items ?: emptyList()
    }
    val playlist = playlistPage

    // STEP 3o (richiesta utente n.1): la playlist e' GIA' SALVATA in
    // libreria? (Database.playlistWithBrowseId, come il lato Android).
    // saveCheck = contatore per riverificare dopo salvataggio/rimozione.
    var savedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var saveCheck by remember { mutableStateOf(0) }
    LaunchedEffect(browseId, saveCheck) {
        savedPlaylist = Database.playlistWithBrowseId(browseId)
    }

    // lista brani (Song) per shuffle / coda tutto / click
    val allPlaylistSongs = playlistPage
        ?.songsPage?.items
        .orEmpty()
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

            // MINIATURA QUADRATA fissa a sinistra (40%, landscape Android)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            ) {
                playlistPage?.let {
                    AsyncImage(
                        model = playlistPage!!.thumbnail?.url?.resize(1200, 1200),
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

                playlistPage?.let {
                    // TITOLO grande centrato (come Android)
                    if (!it.title.isNullOrEmpty())
                        AutoResizeText(
                            text = it.title.toString(),
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

                    // RIGA INFO centrata ("N canzoni", Android align BottomCenter)
                    Text(
                        text = "${it.songsPage?.items?.size ?: 0} "
                                + ds("common.songs_word"),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // RIGA DI AZIONI (Android: bookmark salva-in-libreria,
                // shuffle, enqueue)
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    // STEP 3o (richiesta utente n.1): PREFERITI PLAYLIST
                    HeaderActionIcon(
                        icon = if (savedPlaylist == null) Icons.Filled.BookmarkBorder
                               else Icons.Filled.Bookmark,
                        contentDescription = if (savedPlaylist == null)
                            ds("detail.playlist_favorite_add")
                        else ds("detail.playlist_favorite_ok"),
                        accent = savedPlaylist != null,
                        onClick = {
                            val existing = savedPlaylist
                            val title = playlistPage?.title.orEmpty()
                            if (title.isNotBlank()) {
                                coroutineScope.launch {
                                    if (existing == null) {
                                        // SALVA IN LIBRERIA (comportamento
                                        // Android: Playlist + SongPlaylistMap)
                                        runCatching {
                                            val playlistId = Database.insert(
                                                Playlist(
                                                    name = title,
                                                    browseId = browseId,
                                                    isYoutubePlaylist = true,
                                                    isEditable = false
                                                )
                                            )
                                            allPlaylistSongs.forEachIndexed { index, song ->
                                                Database.upsert(song)
                                                Database.insert(
                                                    SongPlaylistMap(
                                                        songId = song.id,
                                                        playlistId = playlistId,
                                                        position = index
                                                    ).default()
                                                )
                                            }
                                        }.onFailure { e ->
                                            println("[RiPlay-Playlist] salvataggio fallito: ${e.message}")
                                        }
                                    } else {
                                        // RIMUOVI DALLA LIBRERIA
                                        runCatching {
                                            Database.delete(existing)
                                        }
                                    }
                                    saveCheck++
                                }
                            }
                        }
                    )

                    HeaderActionIcon(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = ds("detail.playlist_shuffle"),
                        enabled = allPlaylistSongs.isNotEmpty(),
                        onClick = {
                            if (allPlaylistSongs.isNotEmpty()) {
                                val shuffled = allPlaylistSongs.shuffled()
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
                        enabled = allPlaylistSongs.isNotEmpty(),
                        onClick = {
                            if (allPlaylistSongs.isNotEmpty()) onEnqueueAll?.invoke(allPlaylistSongs)
                        }
                    )
                }

                if (playlistPage != null) {

                    playlistPage!!.songsPage?.items.let { allSongs ->

                        val songs = if (parentalControlEnabled)
                            allSongs?.filter { !it.explicit } else allSongs
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = ds("common.songs_word")
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        songs?.forEachIndexed { index, song ->

                            SongItem(
                                song = song,
                                modifier = Modifier
                                    .clickable {
                                        // STEP 3j: TUTTA LA PLAYLIST IN CODA
                                        // (come per l'album); il pulsante
                                        // "+" resta per il singolo brano.
                                        val clicked = song.asSong
                                        val collection = onPlayCollection
                                        if (collection != null && allPlaylistSongs.size > 1) {
                                            collection(allPlaylistSongs, clicked)
                                        } else {
                                            onSongClick(clicked)
                                        }
                                    },
                                onAddToQueue = onAddToQueue?.let { action ->
                                    { action(song.asSong) }
                                }
                            )

                        }
                    }

                    playlistPage!!.otherVersions?.let { otherVersion ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = stringResource(Res.string.album_alternative_versions)
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = otherVersion,
                                key = Environment.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizeDp = albumThumbnailSize,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = {
                                            onAlbumClick(album.key)
                                        }
                                        )
                                )
                            }
                        }
                    }

                }

                // DESCRIZIONE (Information) in fondo alla colonna destra
                playlistPage?.description?.let { description ->
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
