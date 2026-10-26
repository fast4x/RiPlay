package player

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
import player.frame.FrameContainer
import java.io.File

/**
 * STEP 3: sorgente video desktop SENZA tab di debug, con SELEZIONE
 * AUTOMATICA del motore (come fa Android PlayerService/HybridPlayer con
 * Song.isLocal: "if (currentSong.value?.isLocal == true) switchToExo()
 * else switchToYoutube()").
 *
 * Regole di routing ([DesktopPlayerManager.route]):
 *  - input manuale YouTube (URL o id a 11 caratteri) -> motore KCEF;
 *  - input manuale file locale/stream (percorso esistente, http/rtsp) -> motore VLCJ;
 *  - brano con id "local:..." (Song.isLocal) -> motore VLCJ (se il
 *    percorso del file e' risolvibile; i brani MediaStore di Android non
 *    hanno un percorso desktop accessibile e vengono segnalati in UI);
 *  - brano online (Song.id = videoId YouTube) -> motore KCEF;
 *  - nessun media -> superficie KCEF in attesa (comportamento step2k).
 *
 * Al cambio motore l'altro motore viene messo in PAUSA (stessa igiene di
 * Android: exoPlayer.pause() quando si va su YT, _internalYouTubePlayer
 * .pause() quando si torna su ExoPlayer). La superficie KCEF al ritorno
 * ricrea il browser e il videoId in attesa viene iniettato nell'HTML
 * (meccanismo Android <<injectedVideoId>> gia' validato in step2r).
 */
enum class DesktopPlayerEngine(val label: String) {
    YOUTUBE("YouTube (KCEF)"),
    LOCAL("Locale (VLCJ)")
}

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
 *
 * [engine] e' stato Compose: la UI (video pane, MiniPlayer, barre) si
 * ricompone quando cambia. [active] restituisce il controller del motore
 * attivo, cosi' TUTTE le barre di controllo sono sempre agganciate a
 * cio' che sta effettivamente suonando.
 */
class DesktopPlayerManager(
    val kcef: KcefYouTubePlayer = KcefYouTubePlayer(),
    val vlcj: VlcjFrameController = VlcjFrameController()
) {

    var engine by mutableStateOf(DesktopPlayerEngine.YOUTUBE)
        private set

    /**
     * STEP 3d: TRUE = nessun media in riproduzione (avvio dell'app): la
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
     * STEP 3c: TRUE = il player (video) e' VISIBILE; FALSE = sopra il player
     * viene mostrata la COVER del brano. L'audio NON si interrompe mai: il
     * motore resta attivo sotto la cover, esattamente come Android dove la
     * UI mostra la copertina ("if (!mediaItem.isVideo) Image(coverPainter)")
     * mentre il player suona. Default TRUE (nessuna sorpresa quando non c'e'
     * ancora un brano); [route] lo azzera per ogni NUOVO brano in base a
     * Song.isVideo (campo db isAudioOnly, stessa semantica Android) e il
     * toggle nella UI lo cambia a richiesta dell'utente.
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
     * Chiamato da [DesktopVideoSource] quando cambiano i suoi input.
     */
    fun route(song: Song?, videoId: String, manualInput: String) {
        localUnavailableReason = null
        val manual = manualInput.trim()
        val localPath = song?.let { localMediaPath(it) }
        // STEP 3e: il percorso locale entra nella chiave SOLO per i brani
        // LOCALI (risolti in due passate, perche' la Song col mediaId arriva
        // dopo il click). Per i brani ONLINE localMediaPath restituisce
        // l'id stesso (Song.id e' un videoId, non un percorso): includerlo
        // faceva cambiare la chiave tra la passata senza Song e quella con
        // Song -> loadVideoById eseguito DUE volte -> il brano ripartiva da
        // zero poco dopo l'avvio.
        val localKey = if (videoId.startsWith(LOCAL_KEY_PREFIX)) (localPath ?: "") else ""
        val key = "$manual|$videoId|$localKey"
        if (key == lastRouteKey) return
        lastRouteKey = key
        // qualunque media caricato toglie l'app dallo stato "segna\nposto"
        idle = false

        when {
            // 1) input manuale YouTube (URL o id nudo): l'utente ha incollato
            //    un video da guardare -> player visibile (come prima dello step 3c)
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
                        "Percorso non trovato o URL non supportato: '$manual'"
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
                    localUnavailableReason =
                        "Brano locale non riproducibile su desktop: il percorso del file non e' " +
                                "accessibile (origine Android MediaStore o file spostato). Puoi " +
                                "riprodurlo scegliendo il file dal computer con il pulsante " +
                                "cartella nel campo qui sotto."
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
}

/**
 * Area video desktop automatica (niente tab): mostra il motore scelto da
 * [DesktopPlayerManager] e sotto una sola barra di controllo agganciata al
 * motore attivo + l'input manuale (URL YouTube oppure file/URL locale).
 *
 * STEP 3c/3d: quando un brano audio e' in riproduzione la COVER del brano
 * copre il player (logica Android, audio invariato); quando l'app e' senza
 * brano ([DesktopPlayerManager.idle]) la superficie mostra il segnaposto
 * RiPlay. La cover/segnaposto e' CLICCABILE ([onCoverClick]) e apre il
 * player espandibile. Con [browserHiddenByOverlay] aperto il player
 * espandibile copre l'intera finestra e il livello Swing viene nascosto
 * (l'audio prosegue).
 */
@Composable
fun DesktopVideoSource(
    modifier: Modifier = Modifier,
    videoId: String?,
    song: Song?,
    manager: DesktopPlayerManager,
    browserHiddenByOverlay: Boolean = false,
    onCoverClick: () -> Unit = {},
    // STEP 3k (richiesta utente n.1): SWITCH AUDIO/VIDEO anche nella barra
    // del player di sinistra (prima esisteva solo nel player full).
    onToggleVideo: (Boolean) -> Unit = {},
    // STEP 3k (richiesta utente n.4): pulsante RADIO nella barra del player
    // di sinistra in home (stessa funzione del Radio nel player full).
    onAddRadio: (() -> Unit)? = null,
    radioLoading: Boolean = false,
    radioFeedback: String? = null,
    // STEP 3k (richiesta utente n.2): pulsante ESPANDI per aprire il player
    // full screen anche quando la cover NON e' visibile (video in riproduzione).
    onExpand: (() -> Unit)? = null,
    // STEP 3l (richiesta utente): TRUE = VIDEO A TUTTA LARGHEZZA FINESTRA
    // (modalita' teatro). Cambia SOLO il layout attorno allo stesso
    // SwingPanel (il browser non viene ricreato ne' riappeso: la
    // riproduzione prosegue come durante un resize della finestra); a
    // video espanso l'input manuale in fondo e' nascosto per un look
    // immersivo e la barra mostra il pulsante per TORNARE alla larghezza
    // iniziale.
    widened: Boolean = false,
    // STEP 3l: toggle espandi/ripristina larghezza (null = pulsante nascosto)
    onToggleWiden: (() -> Unit)? = null
) {
    val manualInput = remember { mutableStateOf("") }

    // Routing automatico del motore + caricamento media. Gli input sono gli
    // stessi valori che il routing consuma: ogni variazione (click su una
    // canzone, canzone risolta dal db, input manuale debounced) riesegue
    // [DesktopPlayerManager.route] in modo idempotente.
    LaunchedEffect(videoId, song, manualInput.value) {
        manager.route(song, videoId.orEmpty(), manualInput.value)
    }

    // STEP 3l: il pulsante fullscreen della barra ha senso solo con un
    // media effettivamente caricato (a riposo c'e' solo il segnaposto).
    val widenCallback = if (manager.idle) null else onToggleWiden

    Column(modifier) {
        when (manager.engine) {
            DesktopPlayerEngine.YOUTUBE -> YoutubeKcefPane(
                manager, song, browserHiddenByOverlay, onCoverClick,
                onToggleVideo, onAddRadio, radioLoading, radioFeedback, onExpand,
                widened, widenCallback,
                Modifier.weight(1f)
            )
            DesktopPlayerEngine.LOCAL -> LocalVlcjPane(
                manager, song, onCoverClick,
                onToggleVideo, onAddRadio, radioLoading, radioFeedback, onExpand,
                widened, widenCallback,
                Modifier.weight(1f)
            )
        }
        // STEP 3l: a video espanso l'input manuale e' nascosto (solo video
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
    onToggleVideo: (Boolean) -> Unit,
    onAddRadio: (() -> Unit)?,
    radioLoading: Boolean,
    radioFeedback: String?,
    onExpand: (() -> Unit)?,
    // STEP 3l: video a tutta larghezza finestra (teatro)
    widened: Boolean,
    onToggleWiden: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore il browser viene rilasciato (come nei
    // vecchi tab): al ritorno attachBrowser() ricrea la pagina e il videoId
    // in attesa viene iniettato nell'HTML (meccanismo step2r).
    DisposableEffect(Unit) {
        onDispose { manager.kcef.dispose() }
    }

    // STEP 3d: logica display ANDROID (UnifiedPlayer: "if (!mediaItem.isVideo)
    // Image(coverPainter)") a livello SWING. Il browser KCEF RESTA ATTIVO E
    // VISIBILE sotto la cover (l'audio non si interrompe, come ExoPlayer su
    // Android); la cover e' la Canvas di KcefPlayerHostPanel sopra di lui.
    // Con idle (nessun brano) al posto della pagina player nuda si vede il
    // segnaposto RiPlay. Se il player YT segnala un errore la canvas mostra
    // il messaggio (il vecchio banner Compose era coperto dal browser nativo).
    val ytErr by manager.kcef.ytError.collectAsState()
    val coverUrl = song?.thumbnailUrl
    val showCoverArt = manager.idle || (song != null && !manager.videoVisible)
    val errorText = ytErr.takeIf { it != 0 }?.let { code ->
        "Errore YouTube (codice $code: ${KcefEngine.ytErrorMeaning(code)}). " +
                "Dettagli nel log: righe [RiPlay-KCEF] / [RiPlay-YT]."
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
            // STEP 3k: switch audio/video + radio + espandi passano alla barra
            // STEP 3l: anche il toggle video a tutta larghezza finestra
            DefaultBottomBar(
                Modifier.fillMaxWidth(),
                manager.kcef,
                videoVisible = manager.videoVisible,
                onToggleVideo = { onToggleVideo(!manager.videoVisible) },
                onAddRadio = onAddRadio,
                radioLoading = radioLoading,
                radioFeedback = radioFeedback,
                onExpand = onExpand,
                widened = widened,
                onToggleWiden = onToggleWiden
            )
        }
        EngineBadge(DesktopPlayerEngine.YOUTUBE)
    }
}

@Composable
private fun LocalVlcjPane(
    manager: DesktopPlayerManager,
    song: Song?,
    onCoverClick: () -> Unit,
    onToggleVideo: (Boolean) -> Unit,
    onAddRadio: (() -> Unit)?,
    radioLoading: Boolean,
    radioFeedback: String?,
    onExpand: (() -> Unit)?,
    // STEP 3l: video a tutta larghezza finestra (teatro)
    widened: Boolean,
    onToggleWiden: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore VLCJ viene fermato (il controller resta
    // riutilizzabile: ogni load crea un nuovo media player).
    DisposableEffect(Unit) {
        onDispose { manager.vlcj.stop() }
    }

    val size by manager.vlcj.size.collectAsState(null)
    val bytes by manager.vlcj.bytes.collectAsState(null)

    // STEP 3c: stessa logica display Android del pannello KCEF. Il frame VLCJ
    // e' un'immagine Compose: la cover e' disegnata SOPRA con lo z-order
    // normale (niente trucchi AWT). Il messaggio di errore resta visibile
    // perche' la cover lascia posto a lui.
    // STEP 3d: la cover e' CLICCABILE come nel pannello KCEF.
    val coverUrl = song?.thumbnailUrl
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
            // STEP 3k: switch audio/video + radio + espandi passano alla barra
            // STEP 3l: anche il toggle video a tutta larghezza finestra
            DefaultBottomBar(
                Modifier.fillMaxWidth(),
                manager.vlcj,
                videoVisible = manager.videoVisible,
                onToggleVideo = { onToggleVideo(!manager.videoVisible) },
                onAddRadio = onAddRadio,
                radioLoading = radioLoading,
                radioFeedback = radioFeedback,
                onExpand = onExpand,
                widened = widened,
                onToggleWiden = onToggleWiden
            )
        }
        EngineBadge(DesktopPlayerEngine.LOCAL)
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
