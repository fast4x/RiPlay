package it.fast4x.riplay.ui.screens

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import dev.rebelonion.translator.Translator
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.albumPage
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
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
import it.fast4x.riplay.utils.getHttpClient
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.rebelonion.translator.Language
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.foundation.clickable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.album_alternative_versions
import riplay.composeapp.generated.resources.from_wikipedia_cca
import riplay.composeapp.generated.resources.information
import riplay.composeapp.generated.resources.songs
import riplay.composeapp.generated.resources.translate
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Dati UI della pagina album, popolati dalla STESSA sorgente di Android
 * (AlbumDetails -> EnvironmentExt.getAlbum): header moderno
 * musicResponsiveHeaderRenderer (titolo/anno/autori/thumbnail), descrizione
 * inclusa, brani presi dalla playlist VL dell'album e versioni alternative.
 */
private data class AlbumUiData(
    val title: String?,
    val year: String?,
    val thumbnailUrl: String?,
    val authorsText: String?,
    val songs: List<Environment.SongItem>,
    val otherVersions: List<Environment.AlbumItem>,
    val description: String?,
    val shareUrl: String?
)

/**
 * STEP 3o (richiesta utente n.2 "modificare la pagina di dettaglio di
 * artista, album e playlist a quella lato android"): la pagina e'
 * RISTRUTTURATA come AlbumDetails di Android in LANDSCAPE (la desktop e'
 * una finestra larga):
 *  - a SINISTRA la copertina QUADRATA fissa (40% della larghezza, 1:1,
 *    immagine centrata con bordo sfumato) - prima 4:3 su mezza colonna;
 *  - a DESTRA un'unica colonna scorrevole con:
 *      il TITOLO grande centrato (AutoResizeText 32..38sp, come Android),
 *      la riga info centrata ("anno - N canzoni"),
 *      la RIGA DI AZIONI con Arrangement.SpaceAround (HeaderActionIcon:
 *      bookmark preferiti, shuffle, metti tutto in coda),
 *      i BRANI con NUMERO DI TRACCIA al posto della miniatura (come la
 *      lista Android: thumbnailContent = "${index + 1}"),
 *      le versioni alternative e la descrizione (Information).
 *
 * FIX (richiesta utente: "non vedo tutto cio' che e' presente in android"):
 * la pagina NON usa piu' Environment.albumPage -> playlistPage, che
 *  - POSTava il body con Context.DefaultWeb (senza hl/gl di
 *    Environment.locale, a differenza di Environment.browse usato da
 *    Android) e quindi riceveva campi localizzati in modo diverso;
 *  - nel layout NUOVO a due colonne non leggeva la descrizione
 *    (description = null nel modulo) e l'anno dal runs index fisso 2;
 *  - passava da urlCanonical -> VL playlist con doppia chiamata fragile.
 * Ora usiamo la STESSA chiamata di AlbumDetails Android:
 *  - EnvironmentExt.getAlbum(browseId): browse locale-aware, header
 *    musicResponsiveHeaderRenderer, descrizione, brani via
 *    getAlbumSongs(VL playlist), versioni alternative.
 * La vecchia Environment.albumPage resta solo come FALLBACK se la chiamata
 * primaria fallisce.
 *
 * Le funzioni (salvataggio album nel db, bookmark preferiti, traduzione,
 * click = album intero in coda) restano IDENTICHE: cambia solo la sorgente
 * dati e il layout resta quello gia' allineato ad Android.
 */
@OptIn(ExperimentalSerializationApi::class)
@Composable
fun AlbumScreen(
    browseId: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    // STEP 3i: azione "aggiungi alla coda" sui brani dell'album
    onAddToQueue: ((Song) -> Unit)? = null,
    // STEP 3j (richiesta utente n.1): cliccando una canzone dell'album,
    // L'INTERA LISTA DELL'ALBUM va in coda e parte il brano cliccato
    // (stesso comportamento del lato Android: forcePlayFromBeginning).
    onPlayCollection: ((List<Song>, Song) -> Unit)? = null,
    // STEP 3o (richiesta utente n.2): "metti in coda tutto" senza avviare
    // (l'icona enqueue della riga di azioni Android)
    onEnqueueAll: ((List<Song>) -> Unit)? = null
) {
    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }

    var album by remember { mutableStateOf<Album?>(null) }

    val albumUi = remember { mutableStateOf<AlbumUiData?>(null) }
    LaunchedEffect(browseId) {
        Database.album(browseId).collect { currentAlbum ->
            album = currentAlbum

            if (albumUi.value == null) {
                // ─────────────────────────────────────────────────────
                // STESSA SORGENTE DATI DI ANDROID (AlbumDetails):
                // EnvironmentExt.getAlbum(browseId)
                // ─────────────────────────────────────────────────────
                val page = EnvironmentExt.getAlbum(browseId).getOrNull()

                val ui = page?.let {
                    AlbumUiData(
                        title = it.album.info?.name,
                        year = it.album.year,
                        thumbnailUrl = it.album.thumbnail?.url,
                        authorsText = it.album.authors?.joinToString("") { a -> a.name ?: "" },
                        songs = it.songs,
                        otherVersions = it.otherVersions,
                        description = it.description,
                        shareUrl = it.url
                    )
                } ?: Environment.albumPage(BrowseBody(browseId = browseId))
                    ?.getOrNull()
                    ?.let { legacy ->
                        // FALLBACK (solo se getAlbum fallisce): vecchia
                        // chiamata Environment.albumPage
                        AlbumUiData(
                            title = legacy.title,
                            year = legacy.year,
                            thumbnailUrl = legacy.thumbnail?.url,
                            authorsText = legacy.authors?.joinToString("") { a -> a.name ?: "" },
                            songs = legacy.songsPage?.items.orEmpty(),
                            otherVersions = legacy.otherVersions.orEmpty(),
                            description = legacy.description,
                            shareUrl = legacy.url
                        )
                    }

                println("browseId: $browseId albumUi: ${ui?.title} songs=${ui?.songs?.size}")

                if (ui != null) {
                    albumUi.value = ui

                    coroutineScope.launch {
                        Database.upsert(
                            Album(
                                id = browseId,
                                title = if (currentAlbum?.title?.startsWith(MODIFIED_PREFIX) == true) currentAlbum.title else ui.title,
                                thumbnailUrl = if (currentAlbum?.thumbnailUrl?.startsWith(
                                        MODIFIED_PREFIX
                                    ) == true
                                ) currentAlbum.thumbnailUrl else ui.thumbnailUrl,
                                year = ui.year,
                                authorsText = if (currentAlbum?.authorsText?.startsWith(
                                        MODIFIED_PREFIX
                                    ) == true
                                ) currentAlbum.authorsText else ui.authorsText,
                                shareUrl = ui.shareUrl,
                                timestamp = System.currentTimeMillis(),
                                bookmarkedAt = currentAlbum?.bookmarkedAt
                            )
                        )

                        ui.songs
                            .map(Environment.SongItem::asSong)
                            .onEach {
                                Database.upsert(it)
                            }
                            .mapIndexed { position, song ->
                                Database.upsert(
                                    SongAlbumMap(
                                        songId = song.id,
                                        albumId = browseId,
                                        position = position
                                    )
                                )
                            }
                    }
                }
            }
        }

    }

    var translateEnabled by remember {
        mutableStateOf(false)
    }

    val translator = Translator()
    val languageDestination = languageDestination()

    // lista brani dell'album (per shuffle / metti in coda tutto / click)
    val allAlbumSongs = albumUi.value
        ?.songs
        .orEmpty()
        .map { it.asSong }
        .distinctBy { it.id }

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

            // COPERTINA QUADRATA fissa a sinistra (40%, come il landscape
            // Android di AlbumDetails: fillMaxWidth(.4f) + aspectRatio(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            ) {
                albumUi.value?.let {
                    AsyncImage(
                        model = albumUi.value!!.thumbnailUrl?.resize(1200, 1200),
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

            // COLONNA DESTRA scorrevole (titolo, info, azioni, brani...)
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize()
            ) {

                // TITOLO grande centrato (Android: AutoResizeText 32..38sp
                // in fondo all'header, qui in testa alla colonna destra)
                albumUi.value?.let {
                    if (!it.title.isNullOrEmpty())
                        AutoResizeText(
                            text = it.title.toString(),
                            style = typography.displayMedium,
                            fontSizeRange = FontSizeRange(30.sp, 38.sp),
                            fontWeight = typography.displayMedium.fontWeight,
                            fontFamily = typography.displayMedium.fontFamily,
                            color = typography.displayMedium.color,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                    // RIGA INFO centrata (Android: "anno - N songs - durata")
                    Text(
                        text = "${it.year ?: ""}${if (it.year != null) " - " else ""}${it.songs.size} "
                                + ds("common.songs_word"),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // RIGA DI AZIONI (Android: Row SpaceAround di HeaderIconButton):
                // preferiti (bookmark), shuffle, metti tutto in coda
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    HeaderActionIcon(
                        icon = if (album?.bookmarkedAt == null) Icons.Filled.BookmarkBorder
                               else Icons.Filled.Bookmark,
                        contentDescription = if (album?.bookmarkedAt == null)
                            ds("detail.album_favorite_add")
                        else ds("detail.album_favorite_ok"),
                        accent = album?.bookmarkedAt != null,
                        onClick = {
                            val bookmarkedAt =
                                if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                            album.let {
                                coroutineScope.launch {
                                    if (it != null) {
                                        Database.upsert(it.copy(bookmarkedAt = bookmarkedAt))
                                    }
                                }
                            }
                        }
                    )

                    HeaderActionIcon(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = ds("detail.album_shuffle"),
                        enabled = allAlbumSongs.isNotEmpty(),
                        onClick = {
                            if (allAlbumSongs.isNotEmpty()) {
                                val shuffled = allAlbumSongs.shuffled()
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
                        contentDescription = ds("action.enqueue_album"),
                        enabled = allAlbumSongs.isNotEmpty(),
                        onClick = {
                            if (allAlbumSongs.isNotEmpty()) onEnqueueAll?.invoke(allAlbumSongs)
                        }
                    )
                }

                if (albumUi.value != null) {

                    // BRANI con NUMERO DI TRACCIA al posto della miniatura
                    // (come la lista Android: BasicText("${index + 1}"))
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

                    allAlbumSongs.forEachIndexed { index, song ->
                        SongItem(
                            title = song.title,
                            authors = song.artistsText,
                            duration = song.durationText,
                            thumbnailSizeDp = 50.dp,
                            thumbnailContent = {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    val collection = onPlayCollection
                                    if (collection != null && allAlbumSongs.size > 1) {
                                        collection(allAlbumSongs, song)
                                    } else {
                                        onSongClick(song)
                                    }
                                },
                            onAddToQueue = onAddToQueue?.let { action ->
                                { action(song) }
                            },
                            // per la spunta "gia' in coda" (STEP 3n)
                            songId = song.id
                        )
                    }

                    albumUi.value!!.otherVersions.takeIf { it.isNotEmpty() }?.let { otherVersion ->
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

                // DESCRIZIONE (Information): in fondo alla colonna destra,
                // come la sezione "Info & community" di Android
                albumUi.value?.description?.let { description ->
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
                                description.take(attributionsIndex)
                            }
                        )
                    }

                    Image(
                        painter = painterResource(Res.drawable.translate),
                        colorFilter = ColorFilter.tint(if (translateEnabled) Color.White else Color.Gray),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
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

                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "“ $translatedText „",
                            color = Color.White,
                            fontSize = typography.titleSmall.fontSize,
                            textAlign = TextAlign.Start
                        )

                        if (attributionsIndex != -1) {
                            Text(
                                text = stringResource(Res.string.from_wikipedia_cca),
                                color = Color.White,
                                fontSize = typography.titleSmall.fontSize,
                                fontWeight = typography.titleSmall.fontWeight,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                }
                Spacer(Modifier.height(layoutColumnBottomSpacer))
            }
        }

    }

}
