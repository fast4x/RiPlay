package it.fast4x.riplay.utils

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.player.lyrics.DesktopLyricsRepository
import it.fast4x.riplay.player.lyrics.LyricsUiState
import it.fast4x.riplay.ui.components.RiPlayAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import player.PlayerState
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import java.io.File
import java.awt.Window

/**
 * OVERLAY KARAOKE.
 *
 * Una piccola finestra SEMPRE IN PRIMO PIANO che mostra la riga del testo
 * corrente (+ la successiva, attenuata) sincronizzata sul timestamp del
 * player: l'utente canta leggendo sopra qualunque altra applicazione.
 *
 */
object DesktopKaraokeOverlay {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("karaoke.properties")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_POS_X = "posX"
    private const val KEY_POS_Y = "posY"

    /**
     * Toggle delle impostazioni (tab Generale). Stato Compose: il cambio
     * ricompone subito main.kt (l'overlay appare/scompare all'istante).
     */
    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    /** Cambia e PERSISTE subito (l'utente non deve "salvare" nulla). */
    fun setEnabledKaraokeOverlay(value: Boolean) {
        if (enabled == value) return
        enabled = value
        persistEnabled(value)
    }

    private fun readEnabled(): Boolean = try {
        val p = java.util.Properties()
        if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        p.getProperty(KEY_ENABLED, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    private fun persistEnabled(value: Boolean) {
        try {
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(KEY_ENABLED, value.toString())
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop karaoke overlay") }
        } catch (t: Throwable) {
            // persistenza silenziosa: mai bloccare la UI per un file di preferenze
        }
    }

    /** Ultima posizione ricordata dell'overlay (null = mai trascinato). */
    fun lastPosition(): WindowPosition? = try {
        val p = java.util.Properties()
        if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        val x = p.getProperty(KEY_POS_X)?.toFloatOrNull()
        val y = p.getProperty(KEY_POS_Y)?.toFloatOrNull()
        if (x != null && y != null &&
            // prudenza: una posizione assurda (schermo cambiato) torna al default
            x >= -200f && y >= -200f && x <= 20000f && y <= 20000f
        ) WindowPosition(x.dp, y.dp) else null
    } catch (t: Throwable) {
        null
    }

    /** Salvataggio posizione (chiamato debounced mentre si trascina). */
    fun rememberPosition(position: WindowPosition) {
        try {
            if (!position.isSpecified) return
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(KEY_POS_X, position.x.value.toString())
            p.setProperty(KEY_POS_Y, position.y.value.toString())
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop karaoke overlay") }
        } catch (t: Throwable) {
            // silenzioso
        }
    }
}

/**
 * Finestra OVERLAY KARAOKE (da comporre SOLO quando il toggle e' attivo e
 * c'e' un brano caricato: main.kt la monta/smonta, qui nessuna logica di
 * visibilita').
 */
@Composable
fun DesktopKaraokeOverlayWindow() {

    // Stato della finestra: la POSIZIONE parte dall'ultima ricordata
    // (o in basso al centro alla prima apertura, stile sottotitoli).
    val overlayState = rememberWindowState(
        size = DpSize(620.dp, 132.dp),
        position = DesktopKaraokeOverlay.lastPosition() ?: WindowPosition(Alignment.BottomCenter)
    )

    // Salva la posizione debounced mentre viene trascinata (a gesto fermo):
    // collectLatest cancella l'attesa a ogni nuovo movimento, quindi il file
    // viene scritto UNA sola volta quando il trascinamento si ferma.
    LaunchedEffect(overlayState) {
        snapshotFlow { overlayState.position }
            .collectLatest { pos ->
                delay(600)
                DesktopKaraokeOverlay.rememberPosition(pos)
            }
    }

    Window(
        onCloseRequest = { /* non chiudibile con la X: si spegne dalle impostazioni */ },
        title = "RiPlay Karaoke",
        icon = painterResource(Res.drawable.app_icon),
        undecorated = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = false,
        state = overlayState
    ) {
        // Niente voce in taskbar dove il sistema lo consente (stesso trucco
        // della mini player): prima il cast, poi l'assegnazione.
        LaunchedEffect(Unit) {
            runCatching {
                val frame = window as? java.awt.Frame
                if (frame != null) frame.type = java.awt.Window.Type.UTILITY
            }
        }

        DesktopKaraokeOverlayContent(
            window = window
        )
    }
}

/**
 * Pagina karaoke mostrata in un istante: riga corrente + successiva come
 * SNAPSHOT di testo DENTRO la chiave di transizione. Cosi' il blocco in
 * uscita di AnimatedContent continua a mostrare le proprie righe anche se
 * brano o indice cambiano nel frattempo (nessuna riga "di un altro brano"
 * visibile durante la dissolvenza).
 */
private data class KaraokePage(
    val songId: String?,
    val lineIndex: Int,
    val current: String,
    val next: String?
)

/**
 * Contenuto dell'overlay: riga corrente (accento) + successiva (attenuata)
 * sincronizzate sul timestamp del motore attivo; fallback "titolo - artista"
 * quando il testo sincronizzato non e' disponibile.
 */
@Composable
private fun DesktopKaraokeOverlayContent(
    window: Window
) {

    // Brano corrente: chiusura ritardata letta a un poll lento (stesso
    // approccio della copertina nella mini player: un tick a 800ms non pesa
    // e coglie anche i passaggi di brano senza eventi espliciti).
    var song by remember { mutableStateOf(DesktopPlaybackHub.currentSong()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            song = DesktopPlaybackHub.currentSong()
            delay(800)
        }
    }

    // Controller del motore attivo: leggere manager.active in composizione
    // osserva anche il cambio motore (engine e' mutableStateOf).
    val controller = DesktopPlaybackHub.manager?.active
    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    // Stato del testo del brano: caricato UNA volta sola per brano e SOLO
    // se non c'e' gia' nulla in corso (Idle): lo stato e' un flusso condiviso
    // con il pannello testi del player, quindi se l'utente l'ha gia' aperto
    // l'overlay leggerlo e' istantaneo e senza nessuna rete in piu'.
    val currentSong = song
    val lyricsState: LyricsUiState? = if (currentSong != null) {
        val flow = remember(currentSong.id) {
            DesktopLyricsRepository.stateFor(currentSong.id)
        }
        LaunchedEffect(currentSong.id) {
            if (flow.value == LyricsUiState.Idle) {
                DesktopLyricsRepository.load(currentSong)
            }
        }
        val s by flow.collectAsState()
        s
    } else null

    // Riga corrente e successiva: l'ULTIMA riga con timeMs <= timestamp
    // (identica formula del pannello testi). Il costo e' un ciclo O(n) di
    // poche centinaia di righe a ogni tick del player: trascurabile.
    val lines = (lyricsState as? LyricsUiState.Synced)?.lines
    var currentIndex = -1
    if (lines != null) {
        for (i in lines.indices) {
            if (lines[i].timeMs <= state.timestamp) currentIndex = i else break
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B10))
            // trascinamento dalla QUALSIASI zona (formula step 3o, come la
            // title bar e la mini player)
            .karaokeOverlayDrag(window)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lines != null) {
                // ── Transizione fluida riga -> riga  ──────────
                AnimatedContent(
                    targetState = KaraokePage(
                        songId = currentSong?.id,
                        lineIndex = currentIndex,
                        current = (lines.getOrNull(currentIndex)?.text ?: "").ifBlank { "\u266A" },
                        next = lines.getOrNull(currentIndex + 1)?.text?.ifBlank { "\u266A" }
                    ),
                    transitionSpec = {
                        val move = tween<IntOffset>(380, easing = FastOutSlowInEasing)
                        val fade = tween<Float>(380, easing = FastOutSlowInEasing)
                        if (targetState.lineIndex >= initialState.lineIndex) {
                            // avanti nel brano: il testo sale sfumando
                            (slideInVertically(move) { it / 2 } + fadeIn(fade)) togetherWith
                                    (slideOutVertically(move) { -it / 2 } + fadeOut(fade))
                        } else {
                            // seek all'indietro: il testo scende sfumando
                            (slideInVertically(move) { -it / 2 } + fadeIn(fade)) togetherWith
                                    (slideOutVertically(move) { it / 2 } + fadeOut(fade))
                        }
                    },
                    contentAlignment = Alignment.Center,
                    label = "KaraokeLines",
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // ── Riga corrente (accento) ─────────────────────
                        Text(
                            text = page.current,
                            color = RiPlayAccent.copy(alpha = if (state.isPlaying) 1f else 0.7f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        // ── Riga successiva (attenuata) ─────────────────
                        val next = page.next
                        if (next != null) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = next,
                                color = Color.White.copy(alpha = if (state.isPlaying) 0.45f else 0.30f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // ── Fallback senza testo sincronizzato ──────────────────
                // Titolo e artista: la finestra resta un "now playing"
                // compatto invece di sparire misteriosamente.
                Text(
                    text = currentSong?.title ?: "RiPlay",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (lyricsState) {
                        is LyricsUiState.Loading, null -> ds("lyrics.loading")
                        else -> ds("karaoke.no_synced")
                    },
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Trascinamento della finestra overlay SENZA title bar
 */
private fun Modifier.karaokeOverlayDrag(window: Window): Modifier = pointerInput(window) {
    awaitPointerEventScope {
        // ciclo continuo: ogni gesto (down -> move -> up) viene gestito qui
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            var moved = false
            var screenStart: java.awt.Point? = null
            var windowStart: java.awt.Point? = null
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                // un figlio sta gestendo il gesto: non spostare la finestra
                if (change.isConsumed) {
                    if (!moved) continue else break
                }
                val delta = change.position - down.position
                if (!moved) {
                    if (delta.getDistance() > slop) {
                        moved = true
                        // fissati UNA sola volta all'inizio del trascinamento:
                        // coordinate SCHERMO del mouse + posizione corrente
                        // della finestra (nessun accumulo, nessun feedback loop)
                        screenStart = java.awt.MouseInfo.getPointerInfo()?.location
                        windowStart = window.location
                    }
                }
                if (moved) {
                    val mouse = java.awt.MouseInfo.getPointerInfo()?.location
                    val s0 = screenStart
                    val w0 = windowStart
                    if (mouse != null && s0 != null && w0 != null) {
                        window.setLocation(
                            w0.x + (mouse.x - s0.x),
                            w0.y + (mouse.y - s0.y)
                        )
                    }
                    change.consume()
                }
            }
        }
    }
}
