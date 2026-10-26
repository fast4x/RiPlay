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
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.artistPage
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
import kotlin.random.Random

/**
 * STEP 3o (richiesta utente n.2 "modificare la pagina di dettaglio di
 * artista, album e playlist a quella lato android"): ristrutturata come
 * ArtistOverview di Android in LANDSCAPE:
 *  - a SINISTRA la foto QUADRATA fissa dell'artista (40%, 1:1);
 *  - a DESTRA colonna scorrevole: NOME grande centrato (32..38sp), riga
 *    iscritti centrata, RIGA DI AZIONI (Segui artista / shuffle brani),
 *    poi Brani, Playlists, Albums, Singles e descrizione (Information).
 *
 * Le funzioni restano IDENTICHE: salvataggio dell'artista nel db al load,
 * SEGUI (bookmarkedAt come gli album preferiti, step3i), traduzione della
 * biografia. NUOVO (richiesta utente n.2): shuffle dei brani dell'artista
 * (tutta la lista in coda in ordine casuale e parte il primo).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArtistScreen(
    browseId: String,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onViewAllAlbumsClick: () -> Unit,
    onViewAllSinglesClick: () -> Unit,
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
    val artistPage = remember { mutableStateOf<Environment.ArtistInfoPage?>(null) }

    // STEP 3i - ARTISTA SEGUITO: lo stato dell'artista nel db (se esiste,
    // con bookmarkedAt = "seguito") arriva reattivo dal Flow
    val currentArtist by Database.artist(browseId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(browseId) {
        Environment.artistPage(BrowseBody(browseId = browseId))
            ?.onSuccess {
                artistPage.value = it
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
                            name = it.name ?: existing.name,
                            thumbnailUrl = it.thumbnail?.url ?: existing.thumbnailUrl
                        )
                    else
                        Artist(
                            id = browseId,
                            name = it.name,
                            thumbnailUrl = it.thumbnail?.url,
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
            }
    }
    val artist = artistPage.value

    // lista brani dell'artista (per lo shuffle)
    val allArtistSongs = artistPage.value
        ?.songs
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

            // FOTO QUADRATA fissa a sinistra (40%, landscape Android)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            ) {
                artistPage.value?.let {
                    AsyncImage(
                        model = artistPage.value!!.thumbnail?.url?.resize(1200, 1200),
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

                artistPage.value?.let {
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
                    it.subscriberCountText?.let { subscribers ->
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

                if (artistPage.value != null) {

                    artistPage.value!!.songs?.let { allSongs ->

                        val parentalControlEnabled = false

                        val songs = if (parentalControlEnabled)
                            allSongs.filter { !it.explicit } else allSongs
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = stringResource(Res.string.songs)
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

                    artistPage.value!!.playlists?.let { playlists ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = stringResource(Res.string.playlists)
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

                    artistPage.value!!.albums?.let { albums ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title2Actions(
                                title = stringResource(Res.string.albums),
                                onClick1 = {
                                    onViewAllAlbumsClick()
                                },
                                icon2 = Res.drawable.dice,
                                onClick2 = {
                                    val albumId = albums.get(
                                        Random(System.currentTimeMillis()).nextInt(
                                            0,
                                            albums.size - 1
                                        )
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

                    artistPage.value!!.singles?.let { singles ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Title(
                                title = stringResource(Res.string.singles)
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
                artistPage.value?.description?.let { description ->
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
