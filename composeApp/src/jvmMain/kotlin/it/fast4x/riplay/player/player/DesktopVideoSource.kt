package player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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

    /** Messaggio per l'utente quando un media locale non e' riproducibile. */
    var localUnavailableReason by mutableStateOf<String?>(null)
        private set

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
        val key = "$manual|$videoId|${localPath ?: ""}"
        if (key == lastRouteKey) return
        lastRouteKey = key

        when {
            // 1) input manuale YouTube (URL o id nudo)
            manual.isNotEmpty() && extractYouTubeVideoId(manual) != null -> {
                KcefEngine.logKcef("motore automatico: YouTube (input manuale)")
                selectEngine(DesktopPlayerEngine.YOUTUBE)
                kcef.loadVideoById(manual, 0f)
            }

            // 2) input manuale locale/stream: file esistente o URL supportato
            manual.isNotEmpty() -> {
                selectEngine(DesktopPlayerEngine.LOCAL)
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
            videoId.startsWith(LOCAL_KEY_PREFIX) -> {
                selectEngine(DesktopPlayerEngine.LOCAL)
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
            videoId.isNotBlank() -> {
                KcefEngine.logKcef("motore automatico: YouTube (videoId='$videoId')")
                selectEngine(DesktopPlayerEngine.YOUTUBE)
                kcef.loadVideoById(videoId, 0f)
            }

            // 5) nessun media: superficie YouTube in attesa (pagina player
            //    con segnaposto, pronta al primo click)
            else -> selectEngine(DesktopPlayerEngine.YOUTUBE)
        }
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
 */
@Composable
fun DesktopVideoSource(
    modifier: Modifier = Modifier,
    videoId: String?,
    song: Song?,
    manager: DesktopPlayerManager
) {
    val manualInput = remember { mutableStateOf("") }

    // Routing automatico del motore + caricamento media. Gli input sono gli
    // stessi valori che il routing consuma: ogni variazione (click su una
    // canzone, canzone risolta dal db, input manuale debounced) riesegue
    // [DesktopPlayerManager.route] in modo idempotente.
    LaunchedEffect(videoId, song, manualInput.value) {
        manager.route(song, videoId.orEmpty(), manualInput.value)
    }

    Column(modifier) {
        when (manager.engine) {
            DesktopPlayerEngine.YOUTUBE -> YoutubeKcefPane(manager, Modifier.weight(1f))
            DesktopPlayerEngine.LOCAL -> LocalVlcjPane(manager, Modifier.weight(1f))
        }
        PlayerInput(Modifier.fillMaxWidth()) { manualInput.value = it }
    }
}

@Composable
private fun YoutubeKcefPane(
    manager: DesktopPlayerManager,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore il browser viene rilasciato (come nei
    // vecchi tab): al ritorno attachBrowser() ricrea la pagina e il videoId
    // in attesa viene iniettato nell'HTML (meccanismo step2r).
    DisposableEffect(Unit) {
        onDispose { manager.kcef.dispose() }
    }

    Box(modifier) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            KcefVideoSurface(Modifier.weight(1f).fillMaxWidth(), manager.kcef)
            DefaultBottomBar(Modifier.fillMaxWidth(), manager.kcef)
        }
        EngineBadge(DesktopPlayerEngine.YOUTUBE)
    }
}

@Composable
private fun LocalVlcjPane(
    manager: DesktopPlayerManager,
    modifier: Modifier = Modifier
) {
    // Al passaggio all'altro motore VLCJ viene fermato (il controller resta
    // riutilizzabile: ogni load crea un nuovo media player).
    DisposableEffect(Unit) {
        onDispose { manager.vlcj.stop() }
    }

    val size by manager.vlcj.size.collectAsState(null)
    val bytes by manager.vlcj.bytes.collectAsState(null)

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
            DefaultBottomBar(Modifier.fillMaxWidth(), manager.vlcj)
        }
        EngineBadge(DesktopPlayerEngine.LOCAL)
    }
}

/** Indicatore discreto del motore attivo: utile per verificare il routing. */
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
