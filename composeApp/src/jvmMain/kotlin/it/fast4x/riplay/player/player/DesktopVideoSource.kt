package player

import it.fast4x.riplay.i18n.ds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.player.kcef.CoverOverlay
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.player.kcef.KcefVideoSurface
import it.fast4x.riplay.player.kcef.KcefYouTubePlayer
import it.fast4x.riplay.player.kcef.extractYouTubeVideoId
import it.fast4x.riplay.player.vlcj.VlcjFrameController
import it.fast4x.riplay.ui.bars.DefaultBottomBar
import it.fast4x.riplay.utils.hiResCoverUrl
import player.frame.FrameContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * In modalita' video espanso (teatro) i
 * controlli spariscono dopo questo tempo senza movimenti del mouse e
 * tornano al primo movimento: look immersivo
 */
private const val THEATER_CONTROLS_HIDE_DELAY_MS = 3_000L

/**
 * Sorgente video desktop SENZA tab di debug, con SELEZIONE
 * AUTOMATICA del motore (come fa Android PlayerService/HybridPlayer con
 * Song.isLocal: "if (currentSong.value?.isLocal == true) switchToExo()
 * else switchToYoutube()").
 */
enum class DesktopPlayerEngine(val label: String) {
    YOUTUBE("YouTube (KCEF)"),
    LOCAL("Locale (VLCJ)")
}

/**
 * Estensioni di file che contengono davvero una traccia video.
 * I brani locali importati dalla pagina Locali sono SOLO audio
 */
val LOCAL_VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "mpg", "mpeg", "wmv", "ts", "3gp"
)

/** Percorso/URL riproducibile da VLCJ per un brano locale, se risolvibile. */
fun localMediaPath(song: Song): String? {
    val raw = (song.mediaId ?: song.id).trim()
    val cleaned = raw.removePrefix(LOCAL_KEY_PREFIX).trim()
    return when {
        cleaned.isEmpty() -> null
        // URI content:// di MediaStore: esistono solo sul dispositivo Android
        cleaned.startsWith("content://") -> null
        // Solo l'id numerico MediaStore senza percorso: non riproducibile da desktop
        cleaned.toLongOrNull() != null -> null
        else -> cleaned
    }
}

private fun String.isStreamUrl(): Boolean =
    startsWith("http", true) || startsWith("rtsp", true) ||
            startsWith("mms", true) || startsWith("file:", true)

/**
 * Gestore del motore attivo del player desktop. Un'unica istanza vive per
 * tutta l'app (remember in ThreeColumnsApp): contiene i DUE controller
 * (KCEF e VLCJ), il motore selezionato e fa il routing automatico.
 */
class DesktopPlayerManager(
    val kcef: KcefYouTubePlayer = KcefYouTubePlayer(),
    val vlcj: VlcjFrameController = VlcjFrameController()
) {

    var engine by mutableStateOf(DesktopPlayerEngine.YOUTUBE)
        private set

    /**
     * TRUE = nessun media in riproduzione (avvio dell'app): la
     * superficie KCEF mostra il SEGNAPOSTO RiPlay invece del browser nudo
     * (in prova si vedeva la pagina player come pannello bianco/grigio).
     * route() lo azzera appena viene caricato qualunque media.
     */
    var idle by mutableStateOf(true)
        private set

    /** Messaggio per l'utente quando un media locale non e' riproducibile. */
    var localUnavailableReason by mutableStateOf<String?>(null)
        private set

    /**
     * VideoId del brano caricato in CUE all'avvio (caricato ma NON avviato). Null = nessun cue attivo
     */
    @Volatile
    var cuedVideoId: String? = null
        private set

    /**
     * TRUE quando il brano in cue e' REALMENTE partito (l'utente
     * ha premuto play e il motore ha iniziato a suonare)
     */
    @Volatile
    var cuePlaybackConfirmed: Boolean = false
        private set

    /**
     * Chiamata dall'app quando il brano in cue comincia davvero a
     * suonare (transizione isPlaying osservata sulla UI): sblocca il dedup
     * di route() per lo stesso videoId.
     */
    fun confirmCuedPlayback() {
        cuePlaybackConfirmed = true
    }

    /**
     * TRUE = il player (video) e' VISIBILE; FALSE = sopra il player
     * viene mostrata la COVER del brano.
     */
    var videoVisible: Boolean
        get() = _videoVisible
        set(value) {
            if (_videoVisible == value) return
            _videoVisible = value
            KcefEngine.logKcef(
                if (value) "modalita' player: VIDEO visibile (cover nascosta)"
                else "modalita' player: COVER del brano sopra il player (audio invariato)"
            )
        }
    private var _videoVisible by mutableStateOf(true)

    /** Controller del motore attivo (per MiniPlayer e barre di controllo). */
    val active: PlayerController
        get() = when (engine) {
            DesktopPlayerEngine.YOUTUBE -> kcef
            DesktopPlayerEngine.LOCAL -> vlcj
        }

    // Deduplica del routing: la stessa richiesta (es. canzone arrivata
    // subito dopo il click mentre db.getSong completa dopo) NON deve
    // riavviare il video da zero. La chiave include il percorso locale
    // risolvibile perche' il brano locale viene risolto in due passate
    // (prima videoId, poi Song con mediaId dal db).
    private var lastRouteKey: String? = null

    /**
     * Routing automatico: sceglie il motore e carica il media giusto.
     * Chiamato dal LaunchedEffect di [DesktopVideoSource] (finestra visibile)
     * e dal watcher di ThreeColumnsApp (anche a finestra nascosta):
     * @Synchronized rende atomico il check-then-act su [lastRouteKey] cosi'
     * le chiamate concorrenti si deduplicano senza doppiare il load.
     */
    @Synchronized
    fun route(song: Song?, videoId: String, manualInput: String) {
        localUnavailableReason = null
        val manual = manualInput.trim()
        val localPath = song?.let { localMediaPath(it) }
        // zero poco dopo l'avvio.
        val localKey = if (videoId.startsWith(LOCAL_KEY_PREFIX)) (localPath ?: "") else ""
        val key = "$manual|$videoId|$localKey"
        if (key == lastRouteKey) return
        val cued = cuedVideoId
        if (cued != null && manual.isEmpty()) {
            if (videoId.isEmpty()) return
            if (videoId == cued && cuePlaybackConfirmed) {
                cuePlaybackConfirmed = false
                lastRouteKey = key
                return
            }
        }
        // qualunque altro percorso e' un LOAD VERO: lo stato cue finisce qui
        cuedVideoId = null
        cuePlaybackConfirmed = false
        lastRouteKey = key
        // qualunque media caricato toglie l'app dallo stato "segna\nposto"
        idle = false

        when {
            // 1) input manuale (URL o id nudo): l'utente ha incollato
            //    un video da guardare -> player visibile
            manual.isNotEmpty() && extractYouTubeVideoId(manual) != null -> {
                KcefEngine.logKcef("motore automatico: YouTube (input manuale)")
                selectEngine(DesktopPlayerEngine.YOUTUBE)
                videoVisible = true
                kcef.loadVideoById(manual, 0f)
            }

            // 2) input manuale locale/stream: file esistente o URL supportato
            //    (file scelto dall'utente: default player visibile)
            manual.isNotEmpty() -> {
                selectEngine(DesktopPlayerEngine.LOCAL)
                videoVisible = true
                if (manual.isStreamUrl() || File(manual).isFile) {
                    loadLocalMedia(manual, "input manuale")
                } else {
                    localUnavailableReason =
                        ds("error.manual_unsupported", "value" to manual)
                    KcefEngine.logKcef(
                        "input manuale scartato: '$manual' non e' ne' un file esistente ne' un URL supportato (http/rtsp/mms/file)"
                    )
                }
            }

            // 3) brano locale (Song.isLocal, id con prefisso local:)
            //    STEP 3c: display secondo Song.isVideo (stesso campo db Android)
            videoId.startsWith(LOCAL_KEY_PREFIX) -> {
                selectEngine(DesktopPlayerEngine.LOCAL)
                videoVisible = song?.isVideo == true
                logDisplayMode(song)
                if (localPath != null && (localPath.isStreamUrl() || File(localPath).isFile)) {
                    loadLocalMedia(localPath, "brano locale '${song.title}'")
                } else {
                    localUnavailableReason = ds("error.local_unavailable")
                    KcefEngine.logKcef(
                        "brano locale NON riproducibile su desktop: id='$videoId' " +
                                "mediaId='${song?.mediaId}' percorso risolto='$localPath' " +
                                "(uri MediaStore Android o file inesistente)"
                    )
                }
            }

            // 4) brano online: Song.id e' il videoId YouTube
            //    STEP 3c: display secondo Song.isVideo (isAudioOnly, semantica
            //    Android). I brani arrivati da YouTube nascono audio-only
            //    (isAudioOnly=1) -> la COVER copre il player; il toggle in UI
            //    (videocamera/immagine) cambia modalita' e persiste nel db.
            videoId.isNotBlank() -> {
                KcefEngine.logKcef("motore automatico: YouTube (videoId='$videoId')")
                selectEngine(DesktopPlayerEngine.YOUTUBE)
                videoVisible = song?.isVideo == true
                logDisplayMode(song)
                kcef.loadVideoById(videoId, 0f)
            }

            // 5) nessun media: superficie YouTube in attesa (pagina player
            //    pronta al primo click) coperta dal SEGNAPOSTO RiPlay
            else -> {
                idle = true
                videoVisible = true
                selectEngine(DesktopPlayerEngine.YOUTUBE)
            }
        }
    }

    /** Log della decisione display (cover/video) presa da Song.isVideo. */
    private fun logDisplayMode(song: Song?) {
        val asVideo = song?.isVideo == true
        KcefEngine.logKcef(
            "modalita' display: ${if (asVideo) "video" else "cover sopra il player"} " +
                    "(Song.isVideo=${song?.isVideo}, semantica Android isAudioOnly)"
        )
    }

    private fun selectEngine(target: DesktopPlayerEngine) {
        if (engine == target) return
        KcefEngine.logKcef("cambio motore: ${engine.label} -> ${target.label} (pausa dell'altro motore)")
        when (target) {
            // vado su locale: pausa il player YT (il JS funziona anche con
            // la superficie non piu' montata)
            DesktopPlayerEngine.LOCAL -> kcef.pause()
            // torno su YouTube: pausa VLCJ
            DesktopPlayerEngine.YOUTUBE -> vlcj.pause()
        }
        engine = target
    }

    private fun loadLocalMedia(path: String, source: String) {
        KcefEngine.logKcef("motore automatico: VLCJ -> $source, media='$path'")
        vlcj.load(path)
        vlcj.play()
    }

    /**
     * CARICA UN BRANO NEL PLAYER SENZA AVVIARLO (cue). All'avvio
     * dell'app, con la coda ripristinata dalla persistenza, l'app chiama
     * questa funzione col PRIMO brano della coda: il player lo mostra
     * (titolo/cover/durata) pronto a partire col play dell'utente.
     *
     *  - brano YouTube: kcef.cueVideoById (IFrame API cueVideoById:
     *    metadati e durata arrivano, la riproduzione resta ferma; il
     *    watchdog in modalita' cue-safe non forzera' mai il play);
     *  - brano locale: vlcj.load (media().prepare di libVLC: il media
     *    viene preparato, non avviato); play() dell'utente lo parte.
     *
     * Ritorna TRUE se il brano e' stato caricato (l'app allora aggiorna
     * nowPlayingSong); FALSE se il brano locale non e' risolvibile su
     * questo pc (in quel caso la coda resta utilizzabile a mano).
     *
     * NON tocca lastRouteKey: il primo route() della UI (videoId vuoto)
     * viene fermato dalla guardia del cue dentro route() e la chiave
     * effettiva viene registrata solo da un load vero.
     */
    fun cueForStartup(song: Song): Boolean {
        val id = song.id.trim()
        if (id.isEmpty()) return false
        cuePlaybackConfirmed = false
        val isLocal = id.startsWith(LOCAL_KEY_PREFIX)
        if (isLocal) {
            val localPath = localMediaPath(song)
            if (localPath == null || !(localPath.isStreamUrl() || File(localPath).isFile)) {
                KcefEngine.logKcef(
                    "cue avvio SALTATO: brano locale non riproducibile su desktop " +
                            "(id='$id', percorso risolto=$localPath): la coda resta utilizzabile a mano"
                )
                return false
            }
            KcefEngine.logKcef(
                "cue avvio: brano locale '${song.title}' caricato nel motore VLCJ SENZA avviarlo " +
                        "(partira' col play dell'utente)"
            )
            selectEngine(DesktopPlayerEngine.LOCAL)
            videoVisible = song.isVideo
            logDisplayMode(song)
            idle = false
            cuedVideoId = id
            vlcj.load(localPath)   // prepare SENZA play
            return true
        }
        KcefEngine.logKcef(
            "cue avvio: brano YouTube '$id' caricato nel motore KCEF SENZA avviarlo " +
                    "(partira' col play dell'utente)"
        )
        selectEngine(DesktopPlayerEngine.YOUTUBE)
        videoVisible = song.isVideo
        logDisplayMode(song)
        idle = false
        cuedVideoId = id
        kcef.cueVideoById(id, 0f)
        return true
    }
}

/**
 * Area video desktop automatica (niente tab): mostra il motore scelto da
 * [DesktopPlayerManager] e sotto una sola barra di controllo agganciata al
 * motore attivo + l'input manuale (URL YouTube oppure file/URL locale).
 */
@Composable
fun DesktopVideoSource(
    modifier: Modifier = Modifier,
    videoId: String?,
    song: Song?,
    manager: DesktopPlayerManager,
    browserHiddenByOverlay: Boolean = false,
    onCoverClick: () -> Unit = {},
    onToggleVideo: (Boolean) -> Unit = {},
    onAddRadio: (() -> Unit)? = null,
    radioLoading: Boolean = false,
    radioFeedback: String? = null,
    onExpand: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    canGoPrevious: Boolean = false,
    canGoNext: Boolean = false,
    widened: Boolean = false,
    onToggleWiden: (() -> Unit)? = null
) {
    val manualInput = remember { mutableStateOf("") }

    // VISIBILITA' dei controlli in modalita' video espanso. TRUE di default;
    var theaterControlsVisible by remember { mutableStateOf(true) }
    // ultimo movimento del mouse visto nell'app (scritto dal listener AWT)
    val lastPointerActivityMs = remember { AtomicLong(0L) }

    val lastPointerX = remember { AtomicLong(Long.MIN_VALUE) }
    val lastPointerY = remember { AtomicLong(Long.MIN_VALUE) }

    DisposableEffect(Unit) {
        val listener = java.awt.event.AWTEventListener { event ->
            if (event is java.awt.event.MouseEvent &&
                (event.id == java.awt.event.MouseEvent.MOUSE_MOVED ||
                        event.id == java.awt.event.MouseEvent.MOUSE_DRAGGED)
            ) {
                val p = runCatching { event.locationOnScreen }.getOrNull()
                if (p != null) {
                    val x = p.x.toLong()
                    val y = p.y.toLong()
                    val prevX = lastPointerX.getAndSet(x)
                    val prevY = lastPointerY.getAndSet(y)
                    if (x != prevX || y != prevY) {
                        lastPointerActivityMs.set(System.currentTimeMillis())
                    }
                } else {
                    // posizione non disponibile (componente non mostrato):
                    // conta come attivita' come prima, non e' un redispatch
                    lastPointerActivityMs.set(System.currentTimeMillis())
                }
            }
        }
        java.awt.Toolkit.getDefaultToolkit()
            .addAWTEventListener(listener, java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK)
        onDispose {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
        }
    }

    // Timer dei controlli: attivo SOLO a video espanso.
    LaunchedEffect(widened) {
        if (!widened) {
            theaterControlsVisible = true
            return@LaunchedEffect
        }
        lastPointerActivityMs.set(System.currentTimeMillis())
        while (isActive) {
            delay(200)
            val idleFor = System.currentTimeMillis() - lastPointerActivityMs.get()
            theaterControlsVisible = idleFor < THEATER_CONTROLS_HIDE_DELAY_MS
        }
    }


    val controlsVisible = !widened || theaterControlsVisible

    // Routing automatico del motore + caricamento media, a finestra visibile.
    // Con la finestra nascosta nel tray la composizione si congela: in quel
    // caso il load lo invia il watcher snapshotFlow di ThreeColumnsApp, che
    // chiama lo stesso [manager.route]; la chiave deduplica i due percorsi.
    LaunchedEffect(videoId, song, manualInput.value) {
        manager.route(song, videoId.orEmpty(), manualInput.value)
    }

    // il pulsante fullscreen della barra ha senso solo con un
    // media effettivamente caricato (a riposo c'e' solo il segnaposto).
    val widenCallback = if (manager.idle) null else onToggleWiden

    val isLocalEngine = manager.engine == DesktopPlayerEngine.LOCAL
    val localPath = song?.let { localMediaPath(it) }
    val localFileIsVideo = isLocalEngine && localPath != null && !localPath.isStreamUrl() &&
            File(localPath).extension.lowercase() in LOCAL_VIDEO_EXTENSIONS
    val videoSwitchCallback: ((Boolean) -> Unit)? =
        if (!manager.idle && (!isLocalEngine || localFileIsVideo)) onToggleVideo else null

    val radioCallback: (() -> Unit)? =
        if (!manager.idle && !isLocalEngine) onAddRadio else null

    Column(modifier) {
        when (manager.engine) {
            DesktopPlayerEngine.YOUTUBE -> YoutubeKcefPane(
                manager, song, browserHiddenByOverlay, onCoverClick,
                videoSwitchCallback, radioCallback, radioLoading, radioFeedback, onExpand,
                widened, widenCallback,
                // navigazione brani anche nel miniplayer
                onPrevious, onNext, canGoPrevious, canGoNext,
                // visibilita' controlli (teatro auto-hide)
                controlsVisible,
                Modifier.weight(1f)
            )
            DesktopPlayerEngine.LOCAL -> LocalVlcjPane(
                manager, song, onCoverClick,
                videoSwitchCallback, radioCallback, radioLoading, radioFeedback, onExpand,
                widened, widenCallback,
                // navigazione brani anche nel miniplayer
                onPrevious, onNext, canGoPrevious, canGoNext,
                // visibilita' controlli (teatro auto-hide)
                controlsVisible,
                Modifier.weight(1f)
            )
        }
        // a video espanso l'input manuale e' nascosto (solo video
        // + barra di controllo, look teatro). E' un campo Compose: puo'
        // uscire/rientrare nella composizione senza toccare il browser,
        // che vive nel pane sopra.
        if (!widened) {
            PlayerInput(Modifier.fillMaxWidth()) { manualInput.value = it }
        }
    }
}

@Composable
private fun YoutubeKcefPane(
    manager: DesktopPlayerManager,
    song: Song?,
    overlayOpen: Boolean,
    onCoverClick: () -> Unit,
    onToggleVideo: ((Boolean) -> Unit)?,
    onAddRadio: (() -> Unit)?,
    radioLoading: Boolean,
    radioFeedback: String?,
    onExpand: (() -> Unit)?,
    widened: Boolean,
    onToggleWiden: (() -> Unit)?,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore il browser viene rilasciato (come nei
    // vecchi tab): al ritorno attachBrowser() ricrea la pagina e il videoId
    // in attesa viene iniettato nell'HTML (meccanismo step2r).
    DisposableEffect(Unit) {
        onDispose { manager.kcef.dispose() }
    }

    // logica display ANDROID (UnifiedPlayer: "if (!mediaItem.isVideo)
    // Image(coverPainter)") a livello SWING. Il browser KCEF RESTA ATTIVO E
    // VISIBILE sotto la cover (l'audio non si interrompe, come ExoPlayer su
    // Android)
    val ytErr by manager.kcef.ytError.collectAsState()
    // la cover del player viene caricata alla RISOLUZIONE PIU' ALTA disponibile
    val coverUrl = song?.thumbnailUrl?.hiResCoverUrl()
    val showCoverArt = manager.idle || (song != null && !manager.videoVisible)
    val errorText = ytErr.takeIf { it != 0 }?.let { code ->
        ds("error.yt_with_code", "code" to code, "meaning" to KcefEngine.ytErrorMeaning(code))
    }

    Box(modifier) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            KcefVideoSurface(
                Modifier.weight(1f).fillMaxWidth(),
                manager.kcef,
                showCover = showCoverArt || errorText != null,
                coverUrl = if (errorText != null) null else coverUrl,
                overlayOpen = overlayOpen,
                errorText = errorText,
                onCoverClick = onCoverClick
            )

            AnimatedVisibility(
                visible = controlsVisible,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(160))
            ) {
                DefaultBottomBar(
                    Modifier.fillMaxWidth(),
                    manager.kcef,
                    videoVisible = manager.videoVisible,
                    onToggleVideo = onToggleVideo?.let { cb -> { cb(!manager.videoVisible) } },
                    onAddRadio = onAddRadio,
                    radioLoading = radioLoading,
                    radioFeedback = radioFeedback,
                    onExpand = onExpand,
                    widened = widened,
                    onToggleWiden = onToggleWiden,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    // titolo+artista nel miniplayer
                    title = song?.title,
                    artist = song?.artistsText
                )
            }
        }
        // anche il badge del motore si nasconde con i controlli
        if (controlsVisible) {
            EngineBadge(DesktopPlayerEngine.YOUTUBE)
        }
    }
}

@Composable
private fun LocalVlcjPane(
    manager: DesktopPlayerManager,
    song: Song?,
    onCoverClick: () -> Unit,
    onToggleVideo: ((Boolean) -> Unit)?,
    onAddRadio: (() -> Unit)?,
    radioLoading: Boolean,
    radioFeedback: String?,
    onExpand: (() -> Unit)?,
    widened: Boolean,
    onToggleWiden: (() -> Unit)?,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore VLCJ viene fermato (il controller resta
    // riutilizzabile: ogni load crea un nuovo media player).
    DisposableEffect(Unit) {
        onDispose { manager.vlcj.stop() }
    }

    val size by manager.vlcj.size.collectAsState(null)
    val bytes by manager.vlcj.bytes.collectAsState(null)

    // stessa logica display Android del pannello KCEF. Il frame VLCJ
    // e' un'immagine Compose: la cover e' disegnata SOPRA con lo z-order
    // normale (niente trucchi AWT).

    val coverUrl = song?.thumbnailUrl?.hiResCoverUrl()
    val showCover = song != null && !manager.videoVisible &&
            !coverUrl.isNullOrEmpty() && manager.localUnavailableReason == null

    Box(modifier) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                FrameContainer(
                    Modifier.fillMaxSize(),
                    size?.run { IntSize(first, second) } ?: IntSize.Zero,
                    bytes
                )
                if (showCover) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { onCoverClick() }
                    ) {
                        CoverOverlay(coverUrl)
                    }
                }
                manager.localUnavailableReason?.let { reason ->
                    Text(
                        reason,
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(160))
            ) {
                DefaultBottomBar(
                    Modifier.fillMaxWidth(),
                    manager.vlcj,
                    videoVisible = manager.videoVisible,
                    onToggleVideo = onToggleVideo?.let { cb -> { cb(!manager.videoVisible) } },
                    onAddRadio = onAddRadio,
                    radioLoading = radioLoading,
                    radioFeedback = radioFeedback,
                    onExpand = onExpand,
                    widened = widened,
                    onToggleWiden = onToggleWiden,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    title = song?.title,
                    artist = song?.artistsText
                )
            }
        }

        if (controlsVisible) {
            EngineBadge(DesktopPlayerEngine.LOCAL)
        }
    }
}

/**
 * Indicatore discreto del motore attivo: utile per verificare il routing.
 * (Nel pannello KCEF resta coperto dal browser nativo: visibile solo nel
 * pannello VLCJ.)
 */
@Composable
private fun BoxScope.EngineBadge(engine: DesktopPlayerEngine) {
    Text(
        engine.label,
        color = Color.White.copy(alpha = 0.35f),
        fontSize = 11.sp,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
