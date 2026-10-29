package it.fast4x.riplay.ui.pages

import it.fast4x.riplay.i18n.DesktopI18n
import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HomePage
import it.fast4x.environment.requests.discoverPage
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.AlbumItem
import it.fast4x.riplay.items.ArtistItem
import it.fast4x.riplay.items.MoodItemColored
import it.fast4x.riplay.items.PlaylistItem
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.albumThumbnailSize
import it.fast4x.riplay.styling.Dimensions.artistThumbnailSize
import it.fast4x.riplay.styling.Dimensions.itemInHorizontalGridWidth
import it.fast4x.riplay.styling.Dimensions.itemsVerticalPadding
import it.fast4x.riplay.styling.Dimensions.playlistThumbnailSize
import it.fast4x.riplay.ui.components.Loader
import it.fast4x.riplay.ui.components.Title
import it.fast4x.riplay.ui.components.Title2Actions
import it.fast4x.riplay.utils.asSong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.moods_and_genres
import riplay.composeapp.generated.resources.refresh

/**
 * STEP 3m (richiesta utente n.3 "vorrei che la home della versione desktop
 * sia la stessa di quella caricata lato android da HomePageOnline"):
 * HOME desktop alimentata dalla STESSA API della home Android:
 *  - EnvironmentExt.getHomePage(setLogin): le SEZIONI REALI della home
 *    YouTube Music (carousel con etichetta + titolo), le stesse di
 *    HomePageOnline Android; il login usa i cookie Environment (step3am),
 *    la presenza del cookie decide setLogin;
 *  - Environment.discoverPage("", ""): i MOODS & GENRES in fondo (come la
 *    sezione "Moods e generi" mostrata da Android quando showMoodsAndGenres).
 *
 * STEP 3ap (lingua home): al cambio lingua a runtime la home si RI-CARICA
 * nella nuova lingua (DesktopI18n.languageRevision e' bump-ata da
 * setLanguageChoice, che allinea anche Environment.locale).
 *
 * STEP 3aq (fix "ricarica in loop senza mai completare" al cambio lingua):
 * la vecchia effect usava confronti ytLoaded-ecc./reset distribuiti e letture
 * dello state DENTRO il while di retry; con la cancellazione/ripartenza
 * dell'effetto al cambio chiave (e getOrNull() che ingoia anche la
 * CancellationException) il caricamento poteva restare appeso o ripartire
 * senza mai completare. NUOVA macchina di caricamento, a prova di loop:
 *  - UNA SOLA chiave composta (refresh|login|lingua): la effect riparte
 *    solo quando serve davvero una ricarica;
 *  - il risultato passa da una variabile LOCALE: lo state home viene
 *    aggiornato SOLO a caricamento riuscito (durante la ricarica resta
 *    visibile la pagina precedente, come Android; niente flash/schermo vuoto);
 *  - TIMEOUT per tentativo (12s): una richiesta appesa non puo' piu'
 *    tenere il loader per sempre;
 *  - la CancellationException viene RILANCIATA (anti-zombie: getOrNull()
 *    non deve ingoiare la cancellazione dell'effetto);
 *  - loading = false in finally: il loader si chiude in OGNI scenario
 *    (successo, 3 tentativi falliti, timeout, cambio lingua in corsa);
 *  - log diagnostico [RiPlay-Home] con chiave, tentativi ed esito.
 *
 * Rendering (parita' con Android HomePageOnline):
 *  - sezione di BRANI (almeno un SongItem) -> LazyHorizontalGrid a 3 righe
 *    con il "+" per la coda, identico alla griglia "For You" Android;
 *  - altre sezioni -> carosello orizzontale (LazyRow) con SongItem (play),
 *    AlbumItem / ArtistItem / PlaylistItem (aprono il dettaglio desktop) e
 *    VideoItem (riprodotto come brano, stessa conversione di SearchPage);
 *  - l'aggiornamento manuale avviene con l'icona refresh in alto
 *    (PullToRefreshBox di Android non esiste su desktop) con 3 tentativi
 *    e backoff, come il caricamento Android (retry 500/1000 ms).
 *
 * Lascia il posto alla home precedente (QuickPicsScreen) SENZA toccare la
 * catena di riproduzione: i click passano da onSongClick/onAddToQueue,
 * le stesse callback usate da tutte le altre schermate desktop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageOnline(
    onSongClick: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoodClick: (Environment.Mood.Item) -> Unit
) {

    var homePage by remember { mutableStateOf<HomePage?>(null) }
    var discover by remember { mutableStateOf<Environment.DiscoverPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadTick by remember { mutableStateOf(0) }

    // STEP 3am: stato login = presenza dei cookie in Environment (impostati
    // da DesktopAccountManager al restore/login). La home cosi' porta le
    // SEZIONI AUTENTICATE come la Android (isYtLoggedIn).
    val ytLoggedIn = !Environment.cookie.isNullOrEmpty()

    // STEP 3ap: revisione lingua (incrementata da DesktopI18n
    // .setLanguageChoice a ogni cambio dal selettore title bar)
    val languageRevision = DesktopI18n.languageRevision

    // STEP 3aq: CHIAVE UNICA di caricamento. La effect riparte solo quando
    // cambia davvero qualcosa (refresh manuale | login | lingua); dentro non
    // ci sono piu' confronti con stati "loaded*" ne' reset distribuiti.
    val loadKey = "$reloadTick|$ytLoggedIn|$languageRevision"

    // Caricamento con 3 tentativi, backoff e TIMEOUT per tentativo (stessa
    // strategia della home Android HomePageOnline.loadData: retry 500/1000 ms).
    LaunchedEffect(loadKey) {
        val startedAt = System.currentTimeMillis()
        println("[RiPlay-Home] reload start key=$loadKey")
        loading = true
        try {
            val page = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var attempts = 0
                var result: HomePage? = null
                while (result == null && attempts < 3 && isActive) {
                    attempts++
                    result = try {
                        withTimeout(12_000L) {
                            EnvironmentExt.getHomePage(setLogin = ytLoggedIn).getOrNull()
                        }
                    } catch (t: TimeoutCancellationException) {
                        println("[RiPlay-Home] getHomePage TIMEOUT (tentativo $attempts)")
                        null
                    } catch (t: CancellationException) {
                        // anti-zombie: la cancellazione dell'effetto (es. cambio
                        // lingua durante il load) NON va ingoiata da getOrNull
                        throw t
                    } catch (t: Throwable) {
                        println("[RiPlay-Home] getHomePage errore (tentativo $attempts): ${t::class.simpleName}: ${t.message}")
                        null
                    }
                    if (result == null && attempts < 3 && isActive) {
                        delay(500L * attempts)
                    }
                }
                result
            }
            // swap solo a caricamento riuscito: la pagina precedente resta
            // visibile durante la ricarica (niente loader su schermo vuoto)
            if (page != null) homePage = page

            // MOODS & GENRES: facoltativi (timeout propio; se falliscono la
            // home resta comunque utilizzabile, come gia' prima)
            if (isActive) {
                val disc = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        withTimeout(12_000L) {
                            Environment.discoverPage("", "").getOrNull()
                        }
                    } catch (t: TimeoutCancellationException) {
                        println("[RiPlay-Home] discoverPage TIMEOUT")
                        null
                    } catch (t: CancellationException) {
                        throw t
                    } catch (t: Throwable) {
                        println("[RiPlay-Home] discoverPage errore: ${t::class.simpleName}: ${t.message}")
                        null
                    }
                }
                if (disc != null) discover = disc
            }

            println(
                "[RiPlay-Home] reload done in ${System.currentTimeMillis() - startedAt} ms " +
                "sezioni=${homePage?.sections?.size ?: 0} " +
                "moods=${discover?.moods?.size ?: 0}"
            )
        } finally {
            // STEP 3aq: in OGNI scenario il loader si chiude (successo,
            // tentativi esauriti, timeout, cancellazione/nuovo reload)
            loading = false
        }
    }

    Title2Actions(
        title = ds("page.home"),
        onClick1 = {},
        icon2 = Res.drawable.refresh,
        onClick2 = {
            if (!loading) {
                homePage = null
                discover = null
                reloadTick++
            }
        }
    )

    if (loading && homePage == null) {
        Loader()
        return
    }

    val page = homePage
    if (page == null) {
        Text(
            ds("page.home_unavailable"),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    } else {
        page.sections.forEach { section ->
            val items = section.items.filterNotNull().distinctBy { it.key }
            if (items.isEmpty() || items.firstOrNull()?.key == null) return@forEach

            // Android: TitleMiniSection (etichetta piccola) + Title (titolo)
            section.label?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Title(title = section.title)

            val isSongSection = items.any { it is Environment.SongItem }
            if (isSongSection) {
                // griglia a 3 righe dei BRANI (come la griglia Quick picks
                // di Android): items misti, ma i NON brani della sezione
                // sono comunque cliccabili secondo il loro tipo
                LazyHorizontalGrid(
                    state = rememberLazyGridState(),
                    rows = GridCells.Fixed(3),
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemsVerticalPadding * 3 * 9)
                ) {
                    items(items = items, key = { it.key }) { item ->
                        when (item) {
                            is Environment.SongItem -> SongItem(
                                song = item,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {},
                                        onClick = { onSongClick(item.asSong) }
                                    )
                                    .width(itemInHorizontalGridWidth),
                                onAddToQueue = { onAddToQueue(item.asSong) }
                            )
                            is Environment.AlbumItem -> AlbumItem(
                                album = item,
                                alternative = true,
                                thumbnailSizeDp = itemInHorizontalGridWidth,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onAlbumClick(item.key) }
                            )
                            is Environment.ArtistItem -> ArtistItem(
                                artist = item,
                                thumbnailSizeDp = itemInHorizontalGridWidth,
                                alternative = true,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onArtistClick(item.key) }
                            )
                            is Environment.PlaylistItem -> PlaylistItem(
                                playlist = item,
                                thumbnailSizeDp = itemInHorizontalGridWidth,
                                alternative = true,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onPlaylistClick(item.key) }
                            )
                            else -> {}
                        }
                    }
                }
            } else {
                // carosello orizzontale (come le LazyRow di Android)
                LazyRow(
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    state = rememberLazyListState(),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(items = items, key = { it.key }) { item ->
                        when (item) {
                            is Environment.SongItem -> SongItem(
                                song = item,
                                modifier = Modifier
                                    .width(300.dp)
                                    .combinedClickable(
                                        onLongClick = {},
                                        onClick = { onSongClick(item.asSong) }
                                    ),
                                onAddToQueue = { onAddToQueue(item.asSong) }
                            )
                            is Environment.AlbumItem -> AlbumItem(
                                album = item,
                                alternative = true,
                                thumbnailSizeDp = albumThumbnailSize,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onAlbumClick(item.key) }
                            )
                            is Environment.ArtistItem -> ArtistItem(
                                artist = item,
                                thumbnailSizeDp = artistThumbnailSize,
                                alternative = true,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onArtistClick(item.key) }
                            )
                            is Environment.PlaylistItem -> PlaylistItem(
                                playlist = item,
                                thumbnailSizeDp = playlistThumbnailSize,
                                alternative = true,
                                showSongsCount = false,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { onPlaylistClick(item.key) }
                            )
                            // STEP 3m: i VIDEO della home si riproducono
                            // come brani (stessa conversione di SearchPage)
                            is Environment.VideoItem -> {
                                val videoSong = item.toSong()
                                SongItem(
                                    songEntity = it.fast4x.riplay.data.models.SongEntity(song = videoSong),
                                    modifier = Modifier
                                        .width(300.dp)
                                        .combinedClickable(
                                            onLongClick = {},
                                            onClick = { onSongClick(videoSong) }
                                        ),
                                    onAddToQueue = { onAddToQueue(videoSong) }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    // MOODS & GENRES (come Android: sezione in fondo alla home, da
    // discoverPage). Stesso rendering della vecchia home desktop.
    discover?.let { pageDiscover ->
        Title(title = stringResource(Res.string.moods_and_genres))
        LazyHorizontalGrid(
            state = rememberLazyGridState(),
            rows = GridCells.Fixed(4),
            flingBehavior = ScrollableDefaults.flingBehavior(),
            modifier = Modifier
                .height(itemsVerticalPadding * 4 * 8)
        ) {
            pageDiscover.moods?.let { moods ->
                items(
                    items = moods.sortedBy { it.title },
                    key = { it.endpoint.params ?: it.title }
                ) {
                    MoodItemColored(
                        mood = it,
                        onClick = {
                            it.endpoint.browseId?.let { _ -> onMoodClick(it) }
                        },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
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
