package it.fast4x.riplay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.environment.Environment
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomPadding
import it.fast4x.riplay.styling.Dimensions.layoutColumnTopPadding
import it.fast4x.riplay.styling.Dimensions.layoutColumnsHorizontalPadding
import it.fast4x.riplay.ui.bars.DesktopSection
import it.fast4x.riplay.ui.bars.DesktopNavigationBar
import it.fast4x.riplay.ui.pages.AlbumsPage
import it.fast4x.riplay.ui.pages.ArtistsPage
import it.fast4x.riplay.ui.pages.PlaylistsPage
import it.fast4x.riplay.ui.pages.PlaylistSongsPage
import it.fast4x.riplay.ui.pages.SongsPage
import it.fast4x.riplay.ui.screens.AlbumScreen
import it.fast4x.riplay.ui.screens.ArtistScreen
import it.fast4x.riplay.ui.screens.DesktopPlayerOverlay
import it.fast4x.riplay.ui.screens.MoodScreen
import it.fast4x.riplay.ui.screens.PlaylistScreen
import it.fast4x.riplay.ui.screens.QuickPicsScreen
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.RiPlayWindowTitleBar
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import player.DesktopVideoSource
import player.DesktopPlayerManager
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import riplay.composeapp.generated.resources.arrow_left
import riplay.composeapp.generated.resources.back

/**
 * STEP 3b: navigazione desktop in stile ANDROID.
 *
 * La finestra desktop ora replica la struttura dell'app Android:
 *  - barra di navigazione IN BASSO (DesktopNavigationBar) con le stesse
 *    5 voci della barra Android: Home, Songs, Artists, Albums, Playlists;
 *  - il player KCEF/VLCJ resta nella colonna di sinistra (come prima);
 *  - la PAGINA DI DESTINAZIONE parte dal centro (dopo la colonna player,
 *    come la vecchia colonna centrale) ma ora e' ESPESA VERSO DESTRA
 *    fino al bordo finestra: al posto del vecchio pannello a tab di
 *    debug (Songs/Albums con TabRow) c'e' la pagina intera della sezione;
 *  - i click su artista/album/playlist aprono la schermata di dettaglio
 *    SOPRA la sezione corrente, con pulsante Back (freccia) per tornare,
 *    come la navigazione Android.
 *
 * STEP 3c (logica display + player espandibile, come Android):
 *  - i brani audio mostrano la COVER sopra il player (audio invariato);
 *    il toggle videocamera/immagine lancia il video o torna alla cover e
 *    la scelta persiste nel db con Song.isAudioOnly (semantica Android);
 *
 * STEP 3d (dalla prova utente):
 *  - la barra mini in basso e' RIMOSSA (la copertina si vede gia' sopra il
 *    browser nel pannello player): la barra in basso e' solo navigazione;
 *  - la copertina/segnaposto sopra il player e' CLICCABILE e apre il
 *    player espandibile (che ha tutti i controlli);
 *  - il player espandibile ha AVANTI/INDIETRO che navigano la CRONOLOGIA
 *    dei brani ascoltati nella sessione (songHistory/historyIndex).
 */
@Composable
fun ThreeColumnsApp(
    // STEP 3h: scope/stato della finestra per la title bar custom
    // (minimizza/massimizza/chiudi + trascinamento). Da main.kt.
    windowScope: WindowScope? = null,
    windowState: WindowState? = null,
    onMinimize: () -> Unit = {},
    onToggleMaximize: () -> Unit = {},
    onClose: () -> Unit = {}
) {

    val db = remember { Database }

    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }

    var videoId by remember { mutableStateOf("") }
    var nowPlayingSong by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(videoId) {
        if (videoId.isEmpty()) return@LaunchedEffect
        nowPlayingSong = db.getSong(videoId)
        println("nowPlayingSong $nowPlayingSong")
    }

    // STEP 3: UN gestore motore per tutta l'app con selezione automatica
    // (brano YouTube -> KCEF, brano/locale/file -> VLCJ), come Android fa
    // con HybridPlayer (switchToExo/switchToYoutube da Song.isLocal)
    val playerManager = remember { DesktopPlayerManager() }

    // STEP 3b: sezione corrente della barra in basso + dettaglio aperto
    // (artista/album/playlist/mood) sopra la sezione; null = nessun dettaglio
    var currentSection by remember { mutableStateOf(DesktopSection.HOME) }
    var detailPage by remember { mutableStateOf<DesktopDetail?>(null) }

    // STEP 3c: player espandibile (overlay a finestra intera) aperto/chiuso.
    // STEP 3d: si apre CLICCANDO la copertina/segnaposto sopra il player
    // (la barra mini e' stata rimossa).
    var playerExpanded by remember { mutableStateOf(false) }

    // STEP 3d: cronologia dei brani ascoltati nella sessione (videoId in
    // ordine di ascolto + posizione corrente): e' la coda minima dietro i
    // pulsanti AVANTI/INDIETRO del player espandibile (indietro = brano
    // precedente, avanti = brano successivo se ci si e' spostati indietro).
    val songHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }
    fun pushHistory(id: String) {
        val current = songHistory.getOrNull(historyIndex)
        if (current == id) return
        while (songHistory.size > historyIndex + 1) {
            songHistory.removeAt(songHistory.lastIndex)
        }
        songHistory.add(id)
        historyIndex = songHistory.lastIndex
    }
    val onHistoryPrevious: () -> Unit = {
        if (historyIndex > 0) {
            historyIndex--
            videoId = songHistory[historyIndex]
        }
    }
    val onHistoryNext: () -> Unit = {
        if (historyIndex < songHistory.lastIndex) {
            historyIndex++
            videoId = songHistory[historyIndex]
        }
    }

    // STEP 3c: interruttore video/cover (lanciato dall'utente). Oltre a
    // cambiare il display del player, PERSISTE la scelta nel db con
    // Song.isAudioOnly, esattamente come Android (isVideo = isAudioOnly != 1):
    // al prossimo ascolto del brano la modalita' scelta viene rispettata.
    val onToggleVideoMode: (Boolean) -> Unit = { show ->
        playerManager.videoVisible = show
        val current = nowPlayingSong
        if (current != null && current.isVideo != show) {
            coroutineScope.launch {
                db.upsert(current.copy(isAudioOnly = if (show) 0 else 1))
            }
        }
    }

    // Brano scelto da una schermata ONLINE (QuickPics/Album/Artista/
    // Playlist YouTube): va inserito nel db e poi riprodotto
    val onSongClickOnline: (Song) -> Unit = { song ->
        pushHistory(song.id)
        videoId = song.id
        coroutineScope.launch {
            db.upsert(song)
        }
        println("ThreeColumnsApp onSongClick videoId $videoId")
    }

    // Brano gia' presente nel db (SongsPage / playlist locale): solo play
    val onSongClickFromDb: (Song) -> Unit = { song ->
        //it's just in db, no need to insert
        pushHistory(song.id)
        videoId = song.id
        println("ThreeColumnsApp onSongClick videoId $videoId")
    }

    fun openDetail(detail: DesktopDetail) {
        detailPage = detail
    }

    Box(Modifier.fillMaxSize()) {
        // STEP 3h: title bar custom (prima la finestra undecorated non aveva
        // NE' pulsanti NE' zona di trascinamento) + scaffold sotto. La barra
        // sta in una Colonna SOPRA lo Scaffold: non tocca la regione interop
        // del browser (che comincia piu' in basso, nel pannello player).
        // L'overlay a tutto schermo e' disegnato DOPO (nella Box esterna):
        // lo copre completamente => il player full screen resta 100% pulito.
        Column(Modifier.fillMaxSize()) {
            RiPlayWindowTitleBar(
                windowScope = windowScope,
                maximized = windowState?.placement == WindowPlacement.Maximized,
                onMinimize = onMinimize,
                onToggleMaximize = onToggleMaximize,
                onClose = onClose
            )
            Scaffold(
            containerColor = Color.Black,
            contentColor = Color.Gray,
            bottomBar = {
                // STEP 3d: la barra mini e' RIMOSSA (la copertina si vede
                // gia' sopra il browser nel pannello player ed e' cliccabile:
                // apre il player espandibile con tutti i controlli).
                // STEP 3b: resta la barra di navigazione stile Android
                // (cambiare sezione chiude il dettaglio aperto, come i tab Android)
                DesktopNavigationBar(
                    currentSection = currentSection,
                    onSectionSelected = { section ->
                        currentSection = section
                        detailPage = null
                    }
                )
            }
        ) { innerPadding ->

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Colonna player (sinistra, invariata rispetto allo step 3)
                PlayerPanel {
                    DesktopVideoSource(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        // STEP 2k: l'id della canzone selezionata arriva al player
                        // (Song.id = videoId YouTube)
                        videoId = videoId,
                        // STEP 3: brano corrente (routing brani locali + barra mini)
                        song = nowPlayingSong,
                        // STEP 3: motore automatico YouTube KCEF / locale VLCJ
                        manager = playerManager,
                        // STEP 3c: col player espandibile aperto il livello
                        // Swing del pannello player si nasconde sotto l'overlay
                        // (l'audio prosegue)
                        browserHiddenByOverlay = playerExpanded,
                        // STEP 3d: click sulla copertina/segnaposto -> player espandibile
                        onCoverClick = {
                            if (nowPlayingSong != null) playerExpanded = true
                        }
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color.Gray.copy(alpha = 0.6f)
                )

                // STEP 3b: pagina di destinazione — parte dove stava la colonna
                // centrale ma ESPESA VERSO DESTRA fino al bordo finestra
                DestinationPanel(
                    modifier = Modifier.weight(1f),
                    currentSection = currentSection,
                    detailPage = detailPage,
                    onBackToSection = { detailPage = null },
                    onGoHome = {
                        currentSection = DesktopSection.HOME
                        detailPage = null
                    },
                    onSongClickOnline = onSongClickOnline,
                    onSongClickFromDb = onSongClickFromDb,
                    onOpenDetail = { openDetail(it) }
                )
            }
        }
        }

        // STEP 3c: player espandibile stile Android (aperto con un click
        // sulla copertina/segnaposto del pannello player, chiuso con il
        // pulsante in alto o con ESC). Disegnato DOPO lo Scaffold = sopra a
        // tutto (contenuto + barre). L'audio prosegue mentre aperto.
        if (nowPlayingSong != null) {
            DesktopPlayerOverlay(
                visible = playerExpanded,
                song = nowPlayingSong,
                controller = playerManager.active,
                videoVisible = playerManager.videoVisible,
                onToggleVideo = onToggleVideoMode,
                engineLabel = playerManager.engine.label,
                onCollapse = { playerExpanded = false },
                // STEP 3d: avanti/indietro sulla cronologia di sessione
                canGoPrevious = historyIndex > 0,
                canGoNext = historyIndex < songHistory.lastIndex,
                onPrevious = onHistoryPrevious,
                onNext = onHistoryNext
            )
        }
    }
}

/**
 * Pannello di sinistra con il player (DesktopVideoSource): stessa
 * struttura/proporzioni del vecchio RightPanelContent (step 2/3).
 */
@Composable
fun PlayerPanel(
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.23f)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding)
    ) {
        Spacer(Modifier.size(layoutColumnTopPadding))
        Row {
            content()
        }
    }
}

/**
 * Pannello contenuto principale (dal centro verso destra): header con
 * logo/Back + pagina della sezione corrente oppure la schermata di
 * dettaglio aperta.
 */
@Composable
fun DestinationPanel(
    modifier: Modifier = Modifier,
    currentSection: DesktopSection,
    detailPage: DesktopDetail?,
    onBackToSection: () -> Unit,
    onGoHome: () -> Unit,
    onSongClickOnline: (Song) -> Unit,
    onSongClickFromDb: (Song) -> Unit,
    onOpenDetail: (DesktopDetail) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding)
            .padding(bottom = layoutColumnBottomPadding)
    ) {
        // Header: logo RiPlay (torna a Home) oppure pulsante Back quando
        // e' aperto un dettaglio (artista/album/playlist/mood)
        val detail = detailPage
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (detail != null) {
                Image(
                    painter = painterResource(Res.drawable.arrow_left),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.7f)),
                    contentDescription = "back",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onBackToSection() }
                )
                Text(
                    text = stringResource(Res.string.back),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onBackToSection() }
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    colorFilter = ColorFilter.tint(RiPlayAccent),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onGoHome() }
                )
                // STEP 3h: nome completo e peso tipografico giusto
                // (prima era solo "Play" senza peso)
                Text(
                    text = "RiPlay",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onGoHome() }
                )
            }
        }

        // Contenuto: dettaglio aperto oppure pagina della sezione
        Box(
            Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            if (detail != null) {
                DetailContent(
                    detail = detail,
                    onBackToSection = onBackToSection,
                    onSongClickOnline = onSongClickOnline,
                    onSongClickFromDb = onSongClickFromDb,
                    onOpenDetail = onOpenDetail
                )
            } else {
                when (currentSection) {
                    DesktopSection.HOME ->
                        ScrollColumn {
                            QuickPicsScreen(
                                onSongClick = onSongClickOnline,
                                onAlbumClick = {
                                    onOpenDetail(DesktopDetail.Album(it))
                                },
                                onArtistClick = {
                                    onOpenDetail(DesktopDetail.Artist(it))
                                },
                                onPlaylistClick = {
                                    onOpenDetail(DesktopDetail.YtPlaylist(it))
                                },
                                onMoodClick = {
                                    onOpenDetail(DesktopDetail.Mood(it))
                                }
                            )
                        }

                    DesktopSection.SONGS ->
                        SongsPage(
                            onSongClick = onSongClickFromDb
                        )

                    DesktopSection.ARTISTS ->
                        ArtistsPage(
                            onArtistClick = {
                                onOpenDetail(DesktopDetail.Artist(it.id))
                            }
                        )

                    DesktopSection.ALBUMS ->
                        AlbumsPage(
                            onAlbumClick = {
                                onOpenDetail(DesktopDetail.Album(it.id))
                            }
                        )

                    DesktopSection.PLAYLISTS ->
                        PlaylistsPage(
                            onPlaylistClick = { preview ->
                                val browseId = preview.playlist.browseId
                                if (!browseId.isNullOrBlank())
                                    onOpenDetail(DesktopDetail.YtPlaylist(browseId))
                                else
                                    onOpenDetail(DesktopDetail.LocalPlaylist(preview.playlist))
                            }
                        )
                }
            }
        }
    }
}

/**
 * Schermate di dettaglio aperte sopra la sezione (come la navigazione
 * Android artista/album/playlist/mood). Le schermate online hanno bisogno
 * dello scroll verticale (non lo gestiscono da sole); le pagine con
 * LazyColumn (playlist locale, mood) scrollano da sole.
 */
@Composable
fun DetailContent(
    detail: DesktopDetail,
    onBackToSection: () -> Unit,
    onSongClickOnline: (Song) -> Unit,
    onSongClickFromDb: (Song) -> Unit,
    onOpenDetail: (DesktopDetail) -> Unit
) {
    when (detail) {
        is DesktopDetail.Artist ->
            ScrollColumn {
                ArtistScreen(
                    browseId = detail.browseId,
                    onSongClick = onSongClickOnline,
                    onPlaylistClick = {
                        onOpenDetail(DesktopDetail.YtPlaylist(it))
                    },
                    onViewAllAlbumsClick = {},
                    onViewAllSinglesClick = {},
                    onAlbumClick = {
                        onOpenDetail(DesktopDetail.Album(it))
                    },
                    onClosePage = onBackToSection
                )
            }

        is DesktopDetail.Album ->
            ScrollColumn {
                AlbumScreen(
                    browseId = detail.browseId,
                    onSongClick = onSongClickOnline,
                    onAlbumClick = {
                        onOpenDetail(DesktopDetail.Album(it))
                    }
                )
            }

        is DesktopDetail.YtPlaylist ->
            ScrollColumn {
                PlaylistScreen(
                    browseId = detail.browseId,
                    onSongClick = onSongClickOnline,
                    onAlbumClick = {
                        onOpenDetail(DesktopDetail.Album(it))
                    },
                    onClosePage = onBackToSection
                )
            }

        is DesktopDetail.LocalPlaylist ->
            PlaylistSongsPage(
                playlist = detail.playlist,
                // i brani di una playlist locale sono gia' nel db
                onSongClick = onSongClickFromDb
            )

        is DesktopDetail.Mood ->
            MoodScreen(
                mood = detail.mood,
                onAlbumClick = {
                    onOpenDetail(DesktopDetail.Album(it))
                },
                onArtistClick = {
                    onOpenDetail(DesktopDetail.Artist(it))
                },
                onPlaylistClick = {
                    onOpenDetail(DesktopDetail.YtPlaylist(it))
                }
            )
    }
}

/**
 * Colonna con scroll verticale per le schermate che non lo gestiscono
 * da sole (QuickPics, Album, Artista, Playlist YouTube).
 */
@Composable
private fun ScrollColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}

/**
 * STEP 3b: dettaglio aperto sopra la sezione corrente. Ogni voce e' una
 * destinazione della navigazione (equivalente desktop delle rotte
 * NavRoutes.artist/album/playlist/mood di Android).
 */
sealed interface DesktopDetail {
    data class Artist(val browseId: String) : DesktopDetail
    data class Album(val browseId: String) : DesktopDetail
    data class YtPlaylist(val browseId: String) : DesktopDetail
    data class LocalPlaylist(val playlist: Playlist) : DesktopDetail
    data class Mood(val mood: Environment.Mood.Item) : DesktopDetail
}
