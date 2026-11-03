package it.fast4x.riplay.ui

import it.fast4x.riplay.i18n.ds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.environment.Environment
import it.fast4x.riplay.utils.asSong
// STEP 3ac: prefisso degli id dei brani LOCALI (la radio non si applica)
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomPadding
import it.fast4x.riplay.styling.Dimensions.layoutColumnTopPadding
import it.fast4x.riplay.styling.Dimensions.layoutColumnsHorizontalPadding
import it.fast4x.riplay.ui.bars.DesktopSection
import it.fast4x.riplay.ui.bars.DesktopNavigationBar
// STEP 3m: pannello coda PUBBLICO condiviso col player full screen
import it.fast4x.riplay.ui.components.PlayQueuePanel
// STEP 3n (richiesta utente n.6): id dei brani in coda per la SPUNTA sul "+"
import it.fast4x.riplay.items.LocalQueuedSongIds
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.pages.AlbumsPage
import it.fast4x.riplay.ui.pages.ArtistsPage
// STEP 3m (richiesta utente n.3): home desktop = HomePageOnline Android
import it.fast4x.riplay.ui.pages.HomePageOnline
// STEP 3m (richiesta utente n.2): pagina dei FILE LOCALI (VLCJ)
import it.fast4x.riplay.ui.pages.LocalSongsPage
// STEP 3o (richiesta utente n.4): CODA PERSISTENTE (salvataggio/ripristino)
import it.fast4x.riplay.ui.loadSavedQueueIds
import it.fast4x.riplay.ui.saveQueueIds
// STEP 3ac: preferenza AUTO-RADIO (stesso file della coda)
import it.fast4x.riplay.ui.loadAutoRadioEnabled
import it.fast4x.riplay.ui.saveAutoRadioEnabled
// STEP 3o (richiesta utente n.3): etichetta del player full SENZA nome motore
import player.DesktopPlayerEngine
import it.fast4x.riplay.ui.pages.PlaylistsPage
import it.fast4x.riplay.ui.pages.PlaylistSongsPage
import it.fast4x.riplay.ui.pages.SettingsPage
import it.fast4x.riplay.ui.pages.SongsPage
import it.fast4x.riplay.ui.screens.AlbumScreen
import it.fast4x.riplay.ui.screens.ArtistScreen
// STEP 3aq: schermata "VEDI TUTTO" delle sezioni della pagina artista
import it.fast4x.riplay.ui.screens.ArtistItemsScreen
import it.fast4x.riplay.ui.screens.ArtistSectionRequest
import it.fast4x.riplay.ui.screens.DesktopPlayerOverlay
import it.fast4x.riplay.ui.screens.MoodScreen
import it.fast4x.riplay.ui.screens.PlaylistScreen
import it.fast4x.riplay.ui.screens.PodcastScreen
// STEP 3aq: clip + rottondeggiatura del pannello overlay view all
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.ui.RiPlayWindowTitleBar
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import it.fast4x.riplay.utils.DesktopUpdateChecker
import it.fast4x.riplay.utils.DesktopPlaybackHub
import java.util.concurrent.atomic.AtomicLong

import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.nextPage
import it.fast4x.riplay.ui.pages.SearchPage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import player.DesktopVideoSource
import player.DesktopPlayerManager
import riplay.composeapp.generated.resources.Res

import riplay.composeapp.generated.resources.arrow_left
import riplay.composeapp.generated.resources.back

const val QUEUE_MAX_ITEMS = 50

const val AUTO_RADIO_THRESHOLD = 3

const val AUTO_RADIO_COOLDOWN_MS = 30_000L

data class QueuedSong(
    val uid: Long,
    val song: Song
)


@Composable
fun ThreeColumnsApp(
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

    val playQueue = remember { mutableStateListOf<QueuedSong>() }
    val queueUidCounter = remember { AtomicLong(0) }
    fun makeQueued(song: Song) = QueuedSong(queueUidCounter.incrementAndGet(), song)

    var playingUid by remember { mutableStateOf<Long?>(null) }

    val autoAdvanceRef = remember { mutableStateOf<(() -> Unit)?>(null) }

    val playerManager = remember { DesktopPlayerManager() }

    remember(playerManager) {
        playerManager.kcef.onEnded = { autoAdvanceRef.value?.invoke() }
        playerManager.vlcj.onEnded = { autoAdvanceRef.value?.invoke() }
        true
    }

    var updateAvailable by remember { mutableStateOf<DesktopUpdateChecker.UpdateCheckResult.Available?>(null) }
    var updateBannerClosed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(6_000L)
        runCatching { DesktopUpdateChecker.check() }
            .getOrNull()
            ?.let { if (it is DesktopUpdateChecker.UpdateCheckResult.Available) updateAvailable = it }
    }

    var currentSection by remember { mutableStateOf(DesktopSection.HOME) }
    var detailPage by remember { mutableStateOf<DesktopDetail?>(null) }

    var playerExpanded by remember { mutableStateOf(false) }

    var videoWidened by remember { mutableStateOf(false) }

    var queuePanelOpen by remember { mutableStateOf(false) }

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

    fun playFromQueue(song: Song) {
        val idx = playQueue.indexOfFirst { it.song.id == song.id }
        if (idx >= 0) playingUid = playQueue[idx].uid
        pushHistory(song.id)
        videoId = song.id
        coroutineScope.launch {
            db.upsert(song)
        }
        println("ThreeColumnsApp playFromQueue videoId $videoId (coda: ${playQueue.size}, marcato: $playingUid)")
    }

    val advanceQueue: () -> Unit = {
        val endedId = videoId
        val markedIdx = playingUid
            ?.let { uid -> playQueue.indexOfFirst { it.uid == uid } } ?: -1
        if (markedIdx >= 0 && playQueue[markedIdx].song.id == endedId) {
            playQueue.removeAt(markedIdx)
            playingUid = null
        }
        playQueue.firstOrNull()?.let { playFromQueue(it.song) }
    }
    autoAdvanceRef.value = advanceQueue

    val onHistoryNext: () -> Unit = {
        if (historyIndex < songHistory.lastIndex) {
            historyIndex++
            videoId = songHistory[historyIndex]
        } else {
            advanceQueue()
        }
    }

    remember(playerManager) {
        DesktopPlaybackHub.manager = playerManager
        DesktopPlaybackHub.onNext = onHistoryNext
        DesktopPlaybackHub.onPrevious = onHistoryPrevious
        DesktopPlaybackHub.currentSongTitle = { nowPlayingSong?.title }
        true
    }

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
        pushHistory(song.id)
        videoId = song.id
        println("ThreeColumnsApp onSongClick videoId $videoId")
    }

    val onAddToQueue: (Song) -> Unit = { song ->
        playQueue.add(makeQueued(song))
        coroutineScope.launch { db.upsert(song) }
        println("ThreeColumnsApp addToQueue '${song.title}' (coda: ${playQueue.size})")
    }

    val onEnqueueAll: (List<Song>) -> Unit = { songs ->
        val toAdd = songs
            .distinctBy { it.id }
            .filter { s -> playQueue.none { it.song.id == s.id } }
            .take(QUEUE_MAX_ITEMS)
        toAdd.forEach { s ->
            playQueue.add(makeQueued(s))
            coroutineScope.launch { db.upsert(s) }
        }
        println("ThreeColumnsApp enqueueAll +${toAdd.size} brani (coda: ${playQueue.size})")
    }

    val onQueueMove: (Long, Long) -> Unit = { fromUid, toUid ->
        val from = playQueue.indexOfFirst { it.uid == fromUid }
        val to = playQueue.indexOfFirst { it.uid == toUid }
        if (from >= 0 && to >= 0 && from != to) {
            playQueue.add(to, playQueue.removeAt(from))
        }
    }


    val onPlayCollection: (List<Song>, Song) -> Unit = { songs, start ->
        playQueue.clear()
        playingUid = null

        val startItem = makeQueued(start)
        playQueue.add(startItem)
        playingUid = startItem.uid
        songs
            .filter { it.id != start.id }
            .distinctBy { it.id }
            .take(QUEUE_MAX_ITEMS)
            .forEach { s ->
                playQueue.add(makeQueued(s))
                coroutineScope.launch { db.upsert(s) }
            }
        onSongClickOnline(start)
        println("ThreeColumnsApp playCollection '${start.title}' + ${playQueue.size} brani in coda")
    }


    var radioLoading by remember { mutableStateOf(false) }
    var radioFeedback by remember { mutableStateOf<String?>(null) }

    // Interruttore auto-radio (default ACCESO, scelta utente)
    var autoRadioEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        autoRadioEnabled = loadAutoRadioEnabled()
        println("[RiPlay-AutoRadio] preferenza caricata: $autoRadioEnabled")
    }
    val onToggleAutoRadio: () -> Unit = {
        autoRadioEnabled = !autoRadioEnabled
        saveAutoRadioEnabled(autoRadioEnabled)
        println("[RiPlay-AutoRadio] interruttore: $autoRadioEnabled")
    }

    fun requestRadioSongs(auto: Boolean) {
        val current = nowPlayingSong ?: return
        if (radioLoading) return
        // la radio esiste solo per i brani ONLINE: i file locali (prefisso
        // "local:") non hanno brani simili
        if (current.id.startsWith(LOCAL_KEY_PREFIX)) {
            if (!auto) {
                radioFeedback = ds("radio.unavailable")
                coroutineScope.launch {
                    delay(3000)
                    radioFeedback = null
                }
            }
            return
        }
        coroutineScope.launch {
            radioLoading = true
            try {
                val page = Environment.nextPage(NextBody(videoId = current.id))?.getOrNull()
                val similar = page?.itemsPage?.items
                    .orEmpty()
                    .map { it.asSong }
                    .filter { it.id.isNotBlank() && it.id != current.id }
                    .distinctBy { it.id }
                    // STEP 3ac: il brano in riproduzione ora e' IN CODA
                    // (marcato): il filtro "non in coda" lo esclude gia';
                    // il confronto esplicito resta per sicurezza
                    .filter { s -> playQueue.none { it.song.id == s.id } && s.id != current.id }
                similar.forEach { s ->
                    playQueue.add(makeQueued(s))
                    db.upsert(s)
                }
                radioFeedback =
                    if (similar.isEmpty()) ds("radio.none_found")
                    else if (auto) ds("radio.auto_added", "n" to similar.size)
                    else ds("radio.added", "n" to similar.size)
                println(
                    "[RiPlay-Radio]" + (if (auto) " [AUTO]" else "") +
                            " +${similar.size} brani simili in coda (coda: ${playQueue.size})"
                )

                val enginePlaying = playerManager.active.state.value.isPlaying
                if (auto && similar.isNotEmpty() && !enginePlaying) {
                    println("[RiPlay-Radio] [AUTO] player fermo: riparte la riproduzione")
                    advanceQueue()
                }
                delay(3000)
                radioFeedback = null
            } catch (e: Exception) {
                println("[RiPlay-Radio] errore: $e")
                if (!auto) {
                    radioFeedback = ds("radio.unavailable")
                    delay(3000)
                    radioFeedback = null
                }
            } finally {
                radioLoading = false
            }
        }
    }

    val onAddRadio: () -> Unit = { requestRadioSongs(auto = false) }


    val lastAutoRadioAtMs = remember { mutableStateOf(0L) }
    LaunchedEffect(playQueue.size, videoId, autoRadioEnabled) {
        if (!autoRadioEnabled) return@LaunchedEffect
        // niente riproduzione attiva (avvio, brano in cue mai partito):
        // l'auto-radio parte solo con la musica davvero in corso
        if (videoId.isEmpty()) return@LaunchedEffect
        val current = nowPlayingSong ?: return@LaunchedEffect
        if (current.id.startsWith(LOCAL_KEY_PREFIX)) return@LaunchedEffect
        if (playQueue.size > AUTO_RADIO_THRESHOLD) return@LaunchedEffect
        if (radioLoading) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastAutoRadioAtMs.value < AUTO_RADIO_COOLDOWN_MS) return@LaunchedEffect
        lastAutoRadioAtMs.value = now
        println(
            "[RiPlay-AutoRadio] coda=${playQueue.size} <= $AUTO_RADIO_THRESHOLD: " +
                    "richiedo brani simili a '${current.title}'"
        )
        requestRadioSongs(auto = true)
    }

    fun openDetail(detail: DesktopDetail) {
        detailPage = detail
    }


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


    LaunchedEffect(Unit) {
        val saved = loadSavedQueueIds()
        if (saved.isNotEmpty()) {
            val restored = saved.mapNotNull { db.getSong(it) }
            restored.forEach { playQueue.add(makeQueued(it)) }
            println("ThreeColumnsApp coda ripristinata: ${restored.size} brani")
            restored.firstOrNull()?.let { primo ->
                if (playerManager.cueForStartup(primo)) {
                    nowPlayingSong = primo
                    println(
                        "ThreeColumnsApp primo brano della coda caricato nel player SENZA avviarlo: " +
                                "'${primo.title}' (partira' col play)"
                    )
                }
            }
        }
    }

    LaunchedEffect(playQueue.toList()) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            delay(300)
            saveQueueIds(playQueue.map { it.song.id })
        }
    }


    val activeEngineState by playerManager.active.state.collectAsState()
    LaunchedEffect(activeEngineState.isPlaying) {
        if (!activeEngineState.isPlaying) return@LaunchedEffect
        val cued = playerManager.cuedVideoId ?: return@LaunchedEffect
        if (videoId.isNotEmpty()) return@LaunchedEffect
        val song = nowPlayingSong
        if (song == null || song.id != cued) return@LaunchedEffect
        playerManager.confirmCuedPlayback()

        val idx = playQueue.indexOfFirst { it.song.id == cued }
        if (idx >= 0) playingUid = playQueue[idx].uid
        pushHistory(cued)
        videoId = cued
        println(
            "ThreeColumnsApp brano in cue AVVIATO col play dell'utente: '$cued' " +
                    "(coda: ${playQueue.size})"
        )
    }

    Box(Modifier.fillMaxSize()) {

        val queuedSongIds = playQueue.map { it.song.id }.toSet()
        CompositionLocalProvider(LocalQueuedSongIds provides queuedSongIds) {

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

                PlayerPanel(widened = videoWidened) {
                    DesktopVideoSource(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),

                        videoId = videoId,

                        song = nowPlayingSong,

                        manager = playerManager,

                        browserHiddenByOverlay = playerExpanded,

                        onCoverClick = {
                            if (nowPlayingSong != null) playerExpanded = true
                        },

                        onToggleVideo = onToggleVideoMode,

                        onAddRadio = onAddRadio,
                        radioLoading = radioLoading,
                        radioFeedback = radioFeedback,

                        onPrevious = onHistoryPrevious,
                        onNext = onHistoryNext,
                        canGoPrevious = historyIndex > 0,
                        canGoNext = historyIndex < songHistory.lastIndex || playQueue.isNotEmpty(),

                        onExpand = {
                            if (nowPlayingSong != null) playerExpanded = true
                        },

                        widened = videoWidened,
                        onToggleWiden = { videoWidened = !videoWidened }
                    )
                }


                if (!videoWidened) {
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                }


                DestinationPanel(
                    modifier = Modifier.weight(1f),
                    currentSection = currentSection,
                    detailPage = detailPage,
                    onBackToSection = { detailPage = null },
                    onSongClickOnline = onSongClickOnline,
                    onSongClickFromDb = onSongClickFromDb,
                    onAddToQueue = onAddToQueue,
                    onPlayCollection = onPlayCollection,
                    onEnqueueAll = onEnqueueAll,
                    onOpenDetail = { openDetail(it) }
                )


                if (!videoWidened) {
                    AnimatedVisibility(
                        visible = queuePanelOpen,
                        enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
                        exit = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(180))
                    ) {
                        Row {
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                                color = Color.Gray.copy(alpha = 0.6f)
                            )
                            PlayQueuePanel(
                                queue = playQueue.toList(),
                                onPlay = { song -> playFromQueue(song) },
                                onRemove = { song ->
                                    val idx = playQueue.indexOfFirst { it.song.id == song.id }
                                    if (idx >= 0) {
                                        if (playQueue[idx].uid == playingUid) playingUid = null
                                        playQueue.removeAt(idx)
                                    }
                                },
                                onClear = {
                                    playingUid = null
                                    playQueue.clear()
                                },
                                onMove = onQueueMove,
                                playingUid = playingUid,
                                autoRadioEnabled = autoRadioEnabled,
                                onToggleAutoRadio = onToggleAutoRadio,
                                onClose = { queuePanelOpen = false },
                                topPadding = 0.dp
                            )
                        }
                    }
                    if (!queuePanelOpen) {
                        QueueRailButton(
                            count = playQueue.size,
                            onClick = { queuePanelOpen = true }
                        )
                    }
                }
            }
        }
        }


        if (nowPlayingSong != null) {
            DesktopPlayerOverlay(
                visible = playerExpanded,
                song = nowPlayingSong,
                controller = playerManager.active,
                videoVisible = playerManager.videoVisible,
                engineLabel = when (playerManager.engine) {
                    DesktopPlayerEngine.YOUTUBE -> ds("player.online")
                    DesktopPlayerEngine.LOCAL -> ds("player.local")
                },
                onCollapse = { playerExpanded = false },
                canGoPrevious = historyIndex > 0,
                canGoNext = historyIndex < songHistory.lastIndex || playQueue.isNotEmpty(),
                onPrevious = onHistoryPrevious,
                onNext = onHistoryNext,
                queue = playQueue.toList(),
                onQueuePlay = { song -> playFromQueue(song) },
                onQueueRemove = { song ->
                    val idx = playQueue.indexOfFirst { it.song.id == song.id }
                    if (idx >= 0) {
                        if (playQueue[idx].uid == playingUid) playingUid = null
                        playQueue.removeAt(idx)
                    }
                },
                onQueueClear = {
                    playingUid = null
                    playQueue.clear()
                },
                onQueueMove = onQueueMove,
                playingUid = playingUid,
                autoRadioEnabled = autoRadioEnabled,
                onToggleAutoRadio = onToggleAutoRadio,
                onAddRadio = onAddRadio,
                radioLoading = radioLoading,
                radioFeedback = radioFeedback
            )
        }
        }

        AnimatedVisibility(
            visible = updateAvailable != null && !updateBannerClosed,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) +
                    androidx.compose.animation.slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val upd = updateAvailable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101010).copy(alpha = 0.94f))
                    .border(
                        1.dp,
                        RiPlayAccent.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Icon(
                    Icons.Filled.NewReleases,
                    contentDescription = null,
                    tint = RiPlayAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    ds("update.banner_available", "version" to (upd?.versionName ?: "")),
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RiPlayAccent)
                        .clickable {
                            updateBannerClosed = true
                            DesktopUpdateChecker.openInstallerDownload(upd?.versionName ?: "")
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        ds("update.banner_download"),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RiPlayAccent.copy(alpha = 0.16f))
                        .clickable {
                            updateBannerClosed = true
                            DesktopUpdateChecker.openReleasesPage()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        ds("update.banner_open_releases"),
                        color = RiPlayAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = ds("update.banner_close"),
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { updateBannerClosed = true }
                )
            }
        }
    }
}


@Composable
fun PlayerPanel(
    widened: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(if (widened) 1f else 0.23f)
            .padding(horizontal = if (widened) 0.dp else layoutColumnsHorizontalPadding)
            .padding(top = if (widened) 0.dp else layoutColumnTopPadding)
    ) {
        if (!widened) Spacer(Modifier.size(layoutColumnTopPadding))
        Row {
            content()
        }
    }
}


@Composable
private fun QueueRailButton(
    count: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(34.dp)
            .background(Color(0xFF101010))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Filled.QueueMusic,
            contentDescription = "apri la coda di riproduzione",
            tint = if (count > 0) RiPlayAccent else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            "$count",
            color = if (count > 0) RiPlayAccent else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Coda",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp
        )
    }
}


@Composable
fun DestinationPanel(
    modifier: Modifier = Modifier,
    currentSection: DesktopSection,
    detailPage: DesktopDetail?,
    onBackToSection: () -> Unit,
    onSongClickOnline: (Song) -> Unit,
    onSongClickFromDb: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayCollection: (List<Song>, Song) -> Unit,
    onEnqueueAll: (List<Song>) -> Unit,
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

        val detail = detailPage
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (detail != null) {
                Image(
                    painter = painterResource(Res.drawable.arrow_left),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.7f)),
                    contentDescription = ds("common.back"),
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
            }
        }


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
                    onEnqueueAll = onEnqueueAll,
                    onOpenDetail = onOpenDetail
                )
            } else {
                when (currentSection) {

                    DesktopSection.HOME ->
                        ScrollColumn {
                            HomePageOnline(
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


                    DesktopSection.SEARCH ->
                        SearchPage(
                            onSongClick = onSongClickOnline,
                            onAddToQueue = onAddToQueue,
                            onPlayCollection = onPlayCollection,
                            onAlbumClick = {
                                onOpenDetail(DesktopDetail.Album(it))
                            },
                            onArtistClick = {
                                onOpenDetail(DesktopDetail.Artist(it))
                            },
                            onPlaylistClick = {
                                onOpenDetail(DesktopDetail.YtPlaylist(it))
                            },
                            onPodcastClick = { browseId, title ->
                                onOpenDetail(DesktopDetail.Podcast(browseId, title))
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
                                if (preview.playlist.isPodcast && !browseId.isNullOrBlank())
                                    onOpenDetail(DesktopDetail.Podcast(browseId, preview.playlist.name))
                                else if (!browseId.isNullOrBlank())
                                    onOpenDetail(DesktopDetail.YtPlaylist(browseId))
                                else
                                    onOpenDetail(DesktopDetail.LocalPlaylist(preview.playlist))
                            }
                        )


                    DesktopSection.LOCAL ->
                        LocalSongsPage(
                            onPlayCollection = onPlayCollection,
                            onAddToQueue = onAddToQueue
                        )


                    DesktopSection.SETTINGS ->
                        SettingsPage()
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
    // STEP 3o (richiesta utente n.2): "metti in coda tutto" senza avviare
    onEnqueueAll: (List<Song>) -> Unit,
    onOpenDetail: (DesktopDetail) -> Unit
) {
    when (detail) {
        is DesktopDetail.Artist -> {
            // STEP 3aq (richiesta utente: "procedi con la view all come
            // avviene in android"): lo stato della VIEW ALL aperta da
            // una sezione della pagina artista. Su Android e' un
            // CustomModalBottomSheet SOPRA la pagina (ArtistOverviewItems):
            // qui e' lo stesso overlay DENTRO il pannello contenuti, ma
            // FUORI dallo ScrollColumn (cosi' la lista completa scrolla
            // da sola con vincoli di altezza reali invece che infinite).
            // Click sullo sfondo o sulla freccia in alto = chiudi, e la
            // pagina artista resta esattamente dov'era.
            // Chiave remember = browseId: cambiando artista l'overlay si
            // chiude da solo.
            var viewAllRequest by remember(detail.browseId) {
                mutableStateOf<ArtistSectionRequest?>(null)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                ScrollColumn {
                    ArtistScreen(
                        browseId = detail.browseId,
                        onSongClick = onSongClickOnline,
                        onAddToQueue = onAddToQueue,
                        onPlayCollection = onPlayCollection,
                        onEnqueueAll = onEnqueueAll,
                        onPlaylistClick = {
                            onOpenDetail(DesktopDetail.YtPlaylist(it))
                        },
                        // STEP 3aq: la pagina artista chiede l'apertura
                        // della view all di una sezione (Brani / Albums /
                        // Singles / Playlists con moreEndpoint)
                        onViewAllSection = { viewAllRequest = it },
                        onAlbumClick = {
                            onOpenDetail(DesktopDetail.Album(it))
                        },
                        onClosePage = onBackToSection
                    )
                }

                viewAllRequest?.let { request ->
                    // sfondo oscurato: click = chiudi (l'equivalente del
                    // dismiss del bottom sheet di Android)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewAllRequest = null }
                    )
                    // pannello lista completa: stesso colore del pannello
                    // coda (0xF2101110), angoli arrotondati Heavy
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 20.dp)
                            .clip(ThumbnailRoundness.Heavy.shape())
                            .background(Color(0xF2101110))
                    ) {
                        ArtistItemsScreen(
                            request = request,
                            onSongClick = onSongClickOnline,
                            // click su un brano = riparte da li' con la
                            // lista COMPLETA in coda (forcePlayAtIndex)
                            onPlayCollection = onPlayCollection,
                            onAddToQueue = onAddToQueue,
                            onEnqueueAll = onEnqueueAll,
                            // apertura dettaglio = chiude l'overlay e
                            // naviga (come Android che chiude il sheet)
                            onAlbumClick = {
                                viewAllRequest = null
                                onOpenDetail(DesktopDetail.Album(it))
                            },
                            onArtistClick = {
                                viewAllRequest = null
                                onOpenDetail(DesktopDetail.Artist(it))
                            },
                            onPlaylistClick = {
                                viewAllRequest = null
                                onOpenDetail(DesktopDetail.YtPlaylist(it))
                            },
                            onDismiss = { viewAllRequest = null }
                        )
                    }
                }
            }
        }

        is DesktopDetail.Album ->
            ScrollColumn {
                AlbumScreen(
                    browseId = detail.browseId,
                    onSongClick = onSongClickOnline,
                    onAddToQueue = onAddToQueue,
                    // STEP 3j: click su un brano = TUTTO L'ALBUM in coda
                    onPlayCollection = onPlayCollection,
                    // STEP 3o: metti in coda tutto (stile Android)
                    onEnqueueAll = onEnqueueAll,
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
                    // STEP 3o: metti in coda tutto (stile Android)
                    onEnqueueAll = onEnqueueAll,
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

        // STEP 3ad: dettaglio PODCAST (episodi live via podcastPage)
        is DesktopDetail.Podcast ->
            ScrollColumn {
                PodcastScreen(
                    browseId = detail.browseId,
                    fallbackTitle = detail.title,
                    onSongClick = onSongClickOnline,
                    onAddToQueue = onAddToQueue,
                    onPlayCollection = onPlayCollection,
                    onEnqueueAll = onEnqueueAll,
                    onClosePage = onBackToSection
                )
            }
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
    // STEP 3ad: dettaglio PODCAST (show).browseId MPSP: NON e' una pagina
    // playlist YT (PlaylistScreen/playlistPage non restituisce brani, era
    // la causa della lista episodi vuota): usa PodcastScreen con
    // Environment.podcastPage, lo stesso parser di Android. title = titolo
    // di cortesia dai risultati di ricerca / playlist salvata.
    data class Podcast(
        val browseId: String,
        val title: String? = null
    ) : DesktopDetail
}
