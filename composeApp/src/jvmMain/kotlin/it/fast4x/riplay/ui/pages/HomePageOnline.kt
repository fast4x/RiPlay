package it.fast4x.riplay.ui.pages

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
import it.fast4x.environment.utils.parseCookieString
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.moods_and_genres
import riplay.composeapp.generated.resources.refresh

/**
 * STEP 3m (richiesta utente n.3 "vorrei che la home della versione desktop
 * sia la stessa di quella caricata lato android da HomePageOnline"):
 * HOME desktop alimentata dalla STESSA API della home Android:
 *  - EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()): le SEZIONI REALI
 *    della home YouTube Music (carousel con etichetta + titolo), le stesse
 *    di HomePageOnline Android (che usa EnvironmentExt.getHomePage con la
 *    variabile isYtLoggedIn);
 *  - Environment.discoverPage("", ""): i MOODS & GENRES in fondo (come la
 *    sezione "Moods e generi" mostrata da Android quando showMoodsAndGenres).
 *
 * STEP 3al (segnalazione utente v55: "la homepage viene ritornata come se
 * l'utente non e' autenticato" - Android mostra molte piu' sezioni): qui
 * setLogin era CABLATO a false (allo step 3m il desktop non aveva login).
 * Ora esiste il login desktop (step 3ae..3ak: cookie + pageId/authUser in
 * Environment) e vale lo stesso schema di Android:
 * setLogin = DesktopAccountManager.isYtLoggedIn() (true se il cookie attivo
 * contiene SAPISID). Con setLogin=true Environment.kt aggiunge gli header
 * Cookie + Authorization SAPISIDHASH + X-Goog-PageId/X-Goog-Authuser ->
 * YouTube risponde con la home PERSONALIZZATA dell'utente (Listen again,
 * Mixed for you, New releases, ...) invece di quella anonima. Lo stato
 * login e' chiave del LaunchedEffect: login/logout in tab Account
 * ricaricano la home anche mentre e' gia' in primo piano.
 *
 * STEP 3am (test v56: home ANCORA anonima nonostante il login): il test
 * ha smascherato TRE divergenze residue rispetto ad Android, corrette a
 * monte ma incatenate qui:
 *  1. isYtLoggedIn() accettava SOLO il cookie "SAPISID" plain, mentre la
 *     raccolta CEF da' OK anche con solo __Secure-1PAPISID/__Secure-3PAPISID
 *     -> in quel caso setLogin restava false (NESSUN Cookie header -> home
 *     anonima) mentre accountInfo continuava a funzionare ("connesso" in
 *     tab Account + home anonima). Ora la famiglia PAPISID e' la stessa
 *     dell'harvest (DesktopAccountManager.isYtLoggedIn).
 *  2. pageId/authUser restavano null per sempre con un account singolo
 *     (il selettore profili appare solo con piu' profili): su Android il
 *     selettore gira SEMPRE -> X-Goog-Authuser/X-Goog-PageId sempre
 *     presenti. Ora DesktopAccountManager.withDefaultProfile colma il gap
 *     (isSelected/first, fallback authUser="0") e processHarvest
 *     ri-applica ALL'Environment l'account completo.
 *  3. lo stato login non era osservabile da Compose (Environment.cookie e'
 *     un var qualsiasi): la home composta non si ricaricava al login.
 *     Ora DesktopLoginState.revision e' chiave del LaunchedEffect.
 * In più il modulo environment (CRLF) ora: SAPISIDHASH con fallback
 * __Secure-*PAPISID (Environment.setLogin), visitorData LIVE nel body
 * della browse (il context DefaultWeb lo congela alla class-init) e
 * allineamento del visitorData al responseContext della home.
 *
 * Rendering (parita' con Android HomePageOnline):
 *  - sezione di BRANI (almeno un SongItem) -> LazyHorizontalGrid a 3 righe
 *    con il "+" per la coda, identico alla griglia "For You" Android;
 *  - altre sezioni -> carosello orizzontale (LazyRow) con SongItem (play),
 *    AlbumItem / ArtistItem / PlaylistItem (aprono il dettaglio desktop) e
 *    VideoItem (riprodotto come brano, stessa conversione di SearchPage);
 *  - l'aggiornamento avviene con l'icona refresh in alto (PullToRefreshBox
 *    di Android non esiste su desktop) con 3 tentativi e backoff, come il
 *    caricamento Android (retry 500/1000 ms).
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

    // STEP 3al: stato login ALL'ATTO del caricamento (stesso schema di
    // Android isYtLoggedIn()): true -> browse autenticato (cookie +
    // SAPISIDHASH + PageId/Authuser) -> home personalizzata dell'utente;
    // false -> home anonima (quick picks), come prima dello step 3al.
    val ytLoggedIn = DesktopAccountManager.isYtLoggedIn()
    // STEP 3am: revisione osservabile dello stato login (bump da
    // applyToEnvironment/clear): rende il cambio login VISIBILE da Compose
    // anche mentre la home e' gia' in primo piano (Environment.cookie non
    // e' uno stato Compose: senza questa chiave il reload dipendeva dal
    // caso). Chiave del LaunchedEffect insieme a ytLoggedIn.
    val loginRevision = DesktopLoginState.revision
    // Stato login con cui la homePage in cache e' stata caricata: se
    // cambia (login/logout in tab Account) la cache non e' piu' valida.
    var ytLoadedLoggedIn by remember { mutableStateOf(false) }

    // Caricamento con 3 tentativi e backoff (stessa strategia della home
    // Android HomePageOnline.loadData): cache null -> retry 500/1000 ms.
    // STEP 3al: ytLoggedIn tra le chiavi -> cambio login ricarica la home.
    // STEP 3am: anche loginRevision (stato osservabile del login).
    LaunchedEffect(reloadTick, ytLoggedIn, loginRevision) {
        if (homePage != null && ytLoadedLoggedIn != ytLoggedIn) {
            homePage = null
        }
        loading = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var attempts = 0
            while (homePage == null && attempts < 3) {
                homePage = EnvironmentExt.getHomePage(setLogin = ytLoggedIn).getOrNull()
                ytLoadedLoggedIn = ytLoggedIn
                attempts++
                if (homePage == null && attempts < 3) delay(500L * attempts)
            }
            discover = Environment.discoverPage("", "").getOrNull()
        }
        loading = false
        // STEP 3am: diagnostica PRIVACY-SAFE completa (nomi cookie, mai
        // valori): da questa riga si legge se la richiesta era autenticata
        // (setLogin), con quali header di profilo (pageId/authUser) e con
        // quale famiglia di cookie di autorizzazione e' partita.
        val cookieNames = Environment.cookie
            ?.takeIf { it.isNotBlank() }
            ?.let { parseCookieString(it) }
            ?.keys
            ?.sorted()
            ?.joinToString(",") ?: "NULL"
        println(
            "[RiPlay-Home] getHomePage setLogin=$ytLoggedIn sezioni=${homePage?.sections?.size ?: 0} " +
                    "pageId=${!Environment.pageId.isNullOrBlank()} authUser=${!Environment.authUser.isNullOrBlank()} " +
                    "visitorLen=${Environment.visitorData.length} cookieNames=$cookieNames"
        )
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
