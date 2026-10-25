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
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.environment.Environment
import it.fast4x.riplay.utils.asSong
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
// STEP 3j: RADIO (brani simili in coda) via Environment.nextPage
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.nextPage
// STEP 3j: pagina di RICERCA YouTube Music
import it.fast4x.riplay.ui.pages.SearchPage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import player.DesktopVideoSource
import player.DesktopPlayerManager
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import riplay.composeapp.generated.resources.arrow_left
import riplay.composeapp.generated.resources.back

/**
 * STEP 3j: brano in coda con UID STABILE. Serve per il riordino con il
 * trascinamento del pannello Coda: le chiavi degli item del LazyColumn
 * devono restare le stesse durante il drag anche se l'utente ha aggiunto
 * lo stesso brano piu' volte (con il solo id ci sarebbero chiavi duplicate).
 */
data class QueuedSong(
    val uid: Long,
    val song: Song
)

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

    // STEP 3i: CODA DI RIPRODUZIONE. E' l'elenco dei brani che partono in
    // sequenza: alla fine naturale del brano in riproduzione parte il primo
    // della coda (PlayerController.onEnded -> advanceQueue). I brani si
    // aggiungono col pulsante "+" accanto al titolo in TUTTE le liste
    // (Ricerca, Songs, album, artista, playlist, For You). Il click su un
    // brano in coda (pannello Coda del player) lo rimuove dalla coda e lo
    // suona ora.
    // STEP 3j (richiesta utente n.1): il CLICK su un brano dentro un album
    // (o una playlist YT) mette in coda l'INTERA lista (onPlayCollection).
    // STEP 3j (richiesta utente n.2): i brani in coda si RIORDINANO col
    // trascinamento (onQueueMove) => ogni item ha un uid stabile
    // (QueuedSong) perche' le chiavi del drag devono essere uniche e
    // stabili anche con brani duplicati.
    val playQueue = remember { mutableStateListOf<QueuedSong>() }
    val queueUidCounter = remember { AtomicLong(0) }
    fun makeQueued(song: Song) = QueuedSong(queueUidCounter.incrementAndGet(), song)

    // riferimento aggiornato alla funzione di avanzamento: i callback dei
    // player sono collegati UNA volta sola (remember), il lamda qui dentro
    // viene sostituito a ogni ricomposizione cosi' legge SEMPRE lo stato
    // corrente (coda/cronologia) senza closure stantie.
    val autoAdvanceRef = remember { mutableStateOf<(() -> Unit)?>(null) }

    val playerManager = remember { DesktopPlayerManager() }

    // STEP 3i: aggancio dell'AVANZAMENTO AUTOMATICO ai DUE motori:
    // - KCEF (YouTube): fine brano naturale = stato YT 0 (ENDED)
    // - VLCJ (locale): fine brano naturale = evento finished di libVLC
    // (il lamda passa dal riferimento aggiornato autoAdvanceRef: cosi'
    // vede sempre la coda corrente senza closure stantie)
    remember(playerManager) {
        playerManager.kcef.onEnded = { autoAdvanceRef.value?.invoke() }
        playerManager.vlcj.onEnded = { autoAdvanceRef.value?.invoke() }
        true
    }

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

    // STEP 3i: riproduci subito un brano della coda e toglilo dalla coda
    fun playFromQueue(song: Song) {
        val idx = playQueue.indexOfFirst { it.song.id == song.id }
        if (idx >= 0) playQueue.removeAt(idx)
        pushHistory(song.id)
        videoId = song.id
        coroutineScope.launch {
            db.upsert(song)
        }
        println("ThreeColumnsApp playFromQueue videoId $videoId (coda: ${playQueue.size})")
    }

    // STEP 3i: avanzamento automatico a fine brano: parte il primo brano
    // della coda (se presente). Se la coda e' vuota non fa nulla: il
    // player resta fermo sulla fine del video (comportamento attuale).
    val advanceQueue: () -> Unit = {
        playQueue.firstOrNull()?.let { playFromQueue(it.song) }
    }
    autoAdvanceRef.value = advanceQueue

    val onHistoryNext: () -> Unit = {
        if (historyIndex < songHistory.lastIndex) {
            historyIndex++
            videoId = songHistory[historyIndex]
        } else {
            // STEP 3i: nessun avanti nella cronologia: se c'e' una coda
            // parte il prossimo brano in coda (stesso comportamento del
            // pulsante AVANTI di un player)
            advanceQueue()
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

    // STEP 3i: aggiunta in CODA (pulsante "+" nelle liste). Il brano viene
    // anche salvato nel db (l'upsert e' idempotente e i brani locali sono
    // gia' presenti): cosi' la coda puo' contenere solo id ma il play
    // successivo trova sempre il brano nel db.
    val onAddToQueue: (Song) -> Unit = { song ->
        playQueue.add(makeQueued(song))
        coroutineScope.launch { db.upsert(song) }
        println("ThreeColumnsApp addToQueue '${song.title}' (coda: ${playQueue.size})")
    }

    // STEP 3j (richiesta utente n.2): RIORDINO della coda col trascinamento:
    // il pannello Coda passa gli uid (from -> to) e qui muoviamo il brano
    // nella SnapshotStateList (la compose vede il cambiamento subito).
    val onQueueMove: (Long, Long) -> Unit = { fromUid, toUid ->
        val from = playQueue.indexOfFirst { it.uid == fromUid }
        val to = playQueue.indexOfFirst { it.uid == toUid }
        if (from >= 0 && to >= 0 && from != to) {
            playQueue.add(to, playQueue.removeAt(from))
        }
    }

    // STEP 3j (richiesta utente n.1): CLICK SU UN BRANO DI UN ALBUM (o di
    // una playlist YouTube) => TUTTA la lista va in coda e parte subito il
    // brano cliccato (comportamento Android: forcePlayFromBeginning). La
    // coda precedente viene sostituita; il pulsante "+" resta per aggiungere
    // SOLO il singolo brano.
    val onPlayCollection: (List<Song>, Song) -> Unit = { songs, start ->
        playQueue.clear()
        songs
            .filter { it.id != start.id }
            .forEach { s ->
                playQueue.add(makeQueued(s))
                coroutineScope.launch { db.upsert(s) }
            }
        onSongClickOnline(start)
        println("ThreeColumnsApp playCollection '${start.title}' + ${playQueue.size} brani in coda")
    }

    // STEP 3j (richiesta utente n.4): RADIO. Aggiunge in fondo alla coda i
    // BRANI SIMILI al brano in riproduzione (Environment.nextPage = la
    // STESSA funzione della radio lato Android, automix inclusa). Evita i
    // duplicati di brani gia' in coda e mostra un piccolo feedback.
    var radioLoading by remember { mutableStateOf(false) }
    var radioFeedback by remember { mutableStateOf<String?>(null) }
    val onAddRadio: () -> Unit = {
        val current = nowPlayingSong
        if (current != null && !radioLoading) {
            coroutineScope.launch {
                radioLoading = true
                try {
                    val page = Environment.nextPage(NextBody(videoId = current.id))?.getOrNull()
                    val similar = page?.itemsPage?.items
                        .orEmpty()
                        .map { it.asSong }
                        .filter { it.id.isNotBlank() && it.id != current.id }
                        .distinctBy { it.id }
                        .filter { s -> playQueue.none { it.song.id == s.id } }
                    similar.forEach { s ->
                        playQueue.add(makeQueued(s))
                        db.upsert(s)
                    }
                    radioFeedback =
                        if (similar.isEmpty()) "Nessun brano simile trovato"
                        else "Aggiunti ${similar.size} brani simili"
                    println("[RiPlay-Radio] +${similar.size} brani simili in coda (coda: ${playQueue.size})")
                    delay(3000)
                    radioFeedback = null
                } catch (e: Exception) {
                    println("[RiPlay-Radio] errore: $e")
                    radioFeedback = "Radio non disponibile"
                    delay(3000)
                    radioFeedback = null
                } finally {
                    radioLoading = false
                }
            }
        }
    }

    fun openDetail(detail: DesktopDetail) {
        detailPage = detail
    }

    // STEP 3i - STORICO BRANI: a ogni cambio brano vengono registrati nel db
    // (a) l'evento di ascolto (tabella Event: alimenta l'ordinamento per
    // "data di riproduzione") e (b) il tempo di ascolto (totalPlayTimeMs,
    // accumulato a blocchi di 5 s SOLO mentre il brano suona davvero: e'
    // anche il campo che fa COMPARIRE il brano nella pagina Songs, le cui
    // query filtrano totalPlayTimeMs > 0). Un +1 ms subito dopo l'avvio
    // rende il brano visibile subito, senza aspettare i primi 5 secondi.
    LaunchedEffect(videoId) {
        if (videoId.isEmpty()) return@LaunchedEffect
        // il brano ONLINE arriva al db con un piccolo ritardo (upsert
        // asincrono al click): brevi ritenti prima di arrendersi
        var song = db.getSong(videoId)
        var tries = 0
        while (song == null && tries < 4) {
            delay(300)
            song = db.getSong(videoId)
            tries++
        }
        nowPlayingSong = song
        println("nowPlayingSong $nowPlayingSong")
        if (song == null) return@LaunchedEffect

        // (a) evento di ascolto (storico/ordinamento DatePlayed)
        db.insert(
            Event(
                songId = song.id,
                timestamp = System.currentTimeMillis(),
                playTime = song.totalPlayTimeMs
            )
        )
        // +1 ms: il brano passa il filtro totalPlayTimeMs > 0 della pagina Songs
        db.incrementTotalPlayTimeMs(song.id, 1)
        println("[RiPlay-Storico] registrato ascolto '${song.title}' (Event + totalPlayTimeMs)")

        // (b) accumulo del tempo di ascolto mentre il brano suona davvero
        while (isActive) {
            delay(5_000L)
            if (playerManager.active.state.value.isPlaying && videoId == song.id) {
                db.incrementTotalPlayTimeMs(song.id, 5_000L)
            }
        }
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

                // Pannello di destinazione — parte dove stava la colonna
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
                    onAddToQueue = onAddToQueue,
                    // STEP 3j: click su un brano di album/playlist YT =
                    // intera lista in coda
                    onPlayCollection = onPlayCollection,
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
                // STEP 3i: AVANTI anche con la coda: storia esaurita -> parte
                // il prossimo brano della coda
                canGoNext = historyIndex < songHistory.lastIndex || playQueue.isNotEmpty(),
                onPrevious = onHistoryPrevious,
                onNext = onHistoryNext,
                // STEP 3i: pannello coda di riproduzione
                queue = playQueue.toList(),
                onQueuePlay = { song -> playFromQueue(song) },
                onQueueRemove = { song ->
                    val idx = playQueue.indexOfFirst { it.song.id == song.id }
                    if (idx >= 0) playQueue.removeAt(idx)
                },
                onQueueClear = { playQueue.clear() },
                // STEP 3j: riordino col trascinamento + RADIO (brani simili)
                onQueueMove = onQueueMove,
                onAddRadio = onAddRadio,
                radioLoading = radioLoading,
                radioFeedback = radioFeedback
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
    onAddToQueue: (Song) -> Unit,
    // STEP 3j: click su un brano di album/playlist YT = intera lista in coda
    onPlayCollection: (List<Song>, Song) -> Unit,
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
                    onAddToQueue = onAddToQueue,
                    onPlayCollection = onPlayCollection,
                    onOpenDetail = onOpenDetail
                )
            } else {
                when (currentSection) {
                    DesktopSection.HOME ->
                        ScrollColumn {
                            QuickPicsScreen(
                                onSongClick = onSongClickOnline,
                                onAddToQueue = onAddToQueue,
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

                    // STEP 3j: RICERCA YouTube Music (come lato Android)
                    DesktopSection.SEARCH ->
                        SearchPage(
                            onSongClick = onSongClickOnline,
                            onAddToQueue = onAddToQueue,
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
                    DesktopSection.SONGS ->
                        SongsPage(
                            onSongClick = onSongClickFromDb,
                            onAddToQueue = onAddToQueue
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
    onAddToQueue: (Song) -> Unit,
    // STEP 3j: click su un brano di album/playlist YT = intera lista in coda
    onPlayCollection: (List<Song>, Song) -> Unit,
    onOpenDetail: (DesktopDetail) -> Unit
) {
    when (detail) {
        is DesktopDetail.Artist ->
            ScrollColumn {
                ArtistScreen(
                    browseId = detail.browseId,
                    onSongClick = onSongClickOnline,
                    onAddToQueue = onAddToQueue,
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
                    onAddToQueue = onAddToQueue,
                    // STEP 3j: click su un brano = TUTTO L'ALBUM in coda
                    onPlayCollection = onPlayCollection,
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
                    onAddToQueue = onAddToQueue,
                    // STEP 3j: click su un brano = TUTTA la PLAYLIST in coda
                    onPlayCollection = onPlayCollection,
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
                onSongClick = onSongClickFromDb,
                onAddToQueue = onAddToQueue
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
