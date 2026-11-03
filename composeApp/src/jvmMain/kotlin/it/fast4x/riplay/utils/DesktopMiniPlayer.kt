package it.fast4x.riplay.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.ui.components.PremiumSlider
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.components.formatShortTime
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
 * MODALITA' MINIMALE, stile "picture-in-picture"
 * di Android.
 *
 * Come funziona (tutto derivato, nessun servizio in thread separati):
 *  - l'interruttore vive nelle Impostazioni -> tab Generale
 *    ([DesktopMiniPlayer.enabled], persistito subito su disco, default OFF);
 *  - main.kt compone [DesktopMiniPlayerWindow] SOLO quando la finestra
 *    principale e' ICONIFICATA (windowState.isMinimized, stato Compose
 *    osservabile) e il toggle e' attivo: appena la principale viene
 *    RIPRISTINATA (taskbar, doppio click sul tray, click sulla copertina
 *    della mini) la condizione torna falsa e la mini esce dalla
 *    composizione -> si chiude da sola. Nessun contatore, nessuna gara.
 *  - la mini e' una finestra Compose SEPARATA: senza bordi, SEMPRE IN
 *    PRIMO PIANO, non ridimensionabile, NON che prende il focus quando
 *    compare (focusable = false: l'utente sta lavorando, il PiP non deve
 *    rubare l'attivazione alla finestra corrente);
 *  - DENTRO: copertina del brano (click = torna alla finestra principale),
 *    tasti precedente / play-pausa / successivo e la TIMELINA PREMIUM con
 *    seek (un solo seekTo al RILASCIO, come nel player full);
 *  - TRASCINAMENTO: la stessa formula della title bar (step 3o: posizione
 *    assoluta del mouse, nessun accumulo -> nessuno sfarfallio), con un
 *    guard in piu' per non litigare con lo slider (se un figlio sta gia'
 *    consumando il movimento, la finestra non si muove);
 *  - la POSIZIONE della mini viene RICORDATA in ~/.riplay/miniplayer.properties
 *    (salvataggio debounced durante il trascinamento + al momento del
 *    restore); alla prima apertura compare in basso a destra dello schermo.
 *
 * Tip: la voce taskbar della mini viene rimossa dove il sistema lo
 * consente (java.awt.Window.Type.UTILITY, tipo "palettina"): su alcuni
 * window manager puo' restare comunque visibile, nessun danno.
 */
object DesktopMiniPlayer {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("miniplayer.properties")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_POS_X = "posX"
    private const val KEY_POS_Y = "posY"

    /**
     * Toggle delle impostazioni (tab Generale). Stato Compose: il cambio
     * ricompone subito main.kt (se la principale e' minimizzata la mini
     * appare/esce all'istante) e la finestra del toggle.
     */
    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    /** Cambia e PERSISTE subito (l'utente non deve "salvare" nulla). */
    fun setEnabledMiniPlayer(value: Boolean) {
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
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop mini player") }
        } catch (t: Throwable) {
            // persistenza silenziosa: mai bloccare la UI per un file di preferenze
        }
    }

    /** Ultima posizione ricordata della mini (null = mai trascinata). */
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

    /** Salvataggio posizione (chiamato debounced mentre si trascina e al restore). */
    fun rememberPosition(position: WindowPosition) {
        try {
            if (!position.isSpecified) return
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(KEY_POS_X, position.x.value.toString())
            p.setProperty(KEY_POS_Y, position.y.value.toString())
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop mini player") }
        } catch (t: Throwable) {
            // silenzioso
        }
    }
}

/**
 * Finestra MINI PLAYER (da comporre SOLO quando la principale e' minimizzata
 * e il toggle e' attivo: main.kt la monta/smonta, qui nessuna logica di
 * visibilita'). [onRestore] riporta in primo piano la finestra principale.
 */
@Composable
fun DesktopMiniPlayerWindow(onRestore: () -> Unit) {

    // Stato della finestra: la POSIZIONE parte dall'ultima ricordata
    // (o in basso a destra alla prima apertura). rememberWindowState e'
    // per-sito-di-composizione: a ogni riapertura della mini riparte da qui.
    val miniState = rememberWindowState(
        size = DpSize(300.dp, 200.dp),
        position = DesktopMiniPlayer.lastPosition() ?: WindowPosition(Alignment.BottomEnd)
    )

    // Ripristino = torna la principale E la posizione corrente della mini
    // viene fissata (il debounce del trascinamento puo' non aver salvato
    // l'ultimo gesto: qui lo salviamo per certo).
    val currentRestore by rememberUpdatedState(onRestore)
    val restoreAndSave: () -> Unit = {
        DesktopMiniPlayer.rememberPosition(miniState.position)
        currentRestore()
    }

    // Salva la posizione debounced mentre viene trascinata (a gesto fermo):
    // collectLatest cancella l'attesa a ogni nuovo movimento, quindi il file
    // viene scritto UNA sola volta quando il trascinamento si ferma.
    LaunchedEffect(miniState) {
        snapshotFlow { miniState.position }
            .collectLatest { pos ->
                delay(600)
                DesktopMiniPlayer.rememberPosition(pos)
            }
    }

    Window(
        onCloseRequest = restoreAndSave,
        title = "RiPlay Mini",
        icon = painterResource(Res.drawable.app_icon),
        undecorated = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = false,
        state = miniState
    ) {
        // Niente voce in taskbar dove il sistema lo consente (stile PiP).
        // NB: il safe-call con assegnazione ((window as? Frame)?.type = ...)
        // NON e' Kotlin valido: prima il cast, poi l'assegnazione.
        LaunchedEffect(Unit) {
            runCatching {
                val frame = window as? java.awt.Frame
                if (frame != null) frame.type = java.awt.Window.Type.UTILITY
            }
        }

        DesktopMiniPlayerContent(
            window = window,
            onRestore = restoreAndSave
        )
    }
}

/**
 * Contenuto della mini: copertina (click = restore) + controlli + timeline.
 * Il colore resta lo "stile notte" del PiP (fondi scuri fissi, come lo
 * splashscreen) con l'accento RiPlay per slider e pulsante play.
 */
@Composable
private fun DesktopMiniPlayerContent(
    window: Window,
    onRestore: () -> Unit
) {

    // Controller del motore attivo: leggere manager.active in composizione
    // osserva anche il cambio motore (engine e' mutableStateOf).
    val controller = DesktopPlaybackHub.manager?.active
    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    // Copertina: chiusura ritardata letta a un poll lento (la finestra esiste
    // solo mentre la principale e' minimizzata: un tick al secondo non pesa)
    // cosi' cambia anche se il passaggio di brano non genera eventi player.
    var thumbnail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (isActive) {
            thumbnail = DesktopPlaybackHub.currentSongThumbnail()
            delay(800)
        }
    }
    val coverUrl = remember(thumbnail) { thumbnail?.hiResCoverUrl() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B10))
            // trascinamento dalla QUALSIASI zona libera (formula step 3o)
            .miniPlayerWindowDrag(window)
    ) {
        // ── Copertina a TUTTA FINESTRA (STEP 12c, richiesta utente) ─────
        // Evoluzione della miniatura centrata dello step 12b: la copertina
        // ORA RIEMPIE l'intera mini-finestra (stile PiP di Android / YouTube
        // Music): i tasti e la timeline "galleggiano" sopra, su un velo
        // sfumato che garantisce la leggibilita'. Il ritaglio alto/basso
        // torna ad esistere MA e' voluto e molto piu' ampio di prima:
        // l'artwork occupa tutta la finestra, non un quadratino al centro.
        // Senza copertina: fondo scuro + logo RiPlay grande al centro.
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = ds("player.song_cover"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF15151F)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    colorFilter = ColorFilter.tint(RiPlayAccent.copy(alpha = 0.55f)),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        // ── Velo sfumato per la leggibilita' dei controlli in basso ────
        // Trasparente in alto (la copertina resta viva), sempre piu' scuro
        // verso il basso dove stanno i tasti e la timeline.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color(0xFF0B0B10).copy(alpha = 0.12f),
                        1f to Color(0xFF0B0B10).copy(alpha = 0.90f)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Zona superiore: click = torna alla finestra principale ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRestore() }
            ) {
                // ── Logo dell'app (STEP 12c): piccola targhetta in alto a
                // sinistra SOPRA la copertina, con fondo semitrasparente
                // per restare visibile su qualsiasi artwork. Solo quando
                // c'e' una copertina vera: nel fallback il logo e' gia'
                // quello grande al centro.
                if (coverUrl != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .padding(5.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.app_icon),
                            contentDescription = "RiPlay",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Suggerimento al passaggio del mouse: e' cliccabile.
                MiniCoverHoverHint()
            }

            // ── Controlli: precedente / play-pausa / successivo ─────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                MiniControlIcon(
                    icon = Icons.Filled.SkipPrevious,
                    label = ds("player.previous"),
                    size = 26.dp,
                    enabled = true
                ) { DesktopPlaybackHub.onPrevious?.invoke() }

                Spacer(Modifier.width(26.dp))

                MiniControlIcon(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Rounded.PlayArrow,
                    label = ds(if (state.isPlaying) "player.pause" else "player.play"),
                    size = 32.dp,
                    enabled = true
                ) {
                    val c = DesktopPlaybackHub.activeController
                    if (c != null) {
                        if (state.isPlaying) c.pause() else c.play()
                    }
                }

                Spacer(Modifier.width(26.dp))

                MiniControlIcon(
                    icon = Icons.Filled.SkipNext,
                    label = ds("player.next"),
                    size = 26.dp,
                    enabled = true
                ) { DesktopPlaybackHub.onNext?.invoke() }
            }

            // ── Timeline trascinabile con seek (un seekTo al rilascio) ──
            val animatedTimestamp by animateFloatAsState(
                targetValue = state.timestamp.toFloat(),
                label = "miniTimestamp"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formatShortTime(state.timestamp),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    modifier = Modifier.width(34.dp)
                )
                PremiumSlider(
                    value = animatedTimestamp,
                    onValueChange = { /* durante il drag mostra solo il valore locale */ },
                    onValueChangeFinished = { controller?.seekTo(it.toLong()) },
                    valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatShortTime(state.duration),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    modifier = Modifier.width(34.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

/** Pulsante mini (icona sola, bianca, hover leggero). */
@Composable
private fun MiniControlIcon(
    icon: ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = if (enabled) 0.92f else 0.35f),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Velo che compare sulla copertina quando il mouse ci passa sopra: icona
 * "espandi" + hint testuale ("click = torna alla finestra principale").
 * Pura UI: pointerInput con loop continuo sugli eventi (stesso approccio
 * dello slider premium), NON consuma nulla: il click resta alla copertina
 * e il trascinamento resta alla finestra.
 */
@Composable
private fun MiniCoverHoverHint() {
    var hover by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> hover = true
                            PointerEventType.Exit -> hover = false
                            PointerEventType.Move -> hover = true
                        }
                    }
                }
            }
    ) {
        if (hover) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.42f))
                    .padding(bottom = 10.dp)
            ) {
                Icon(
                    Icons.Filled.OpenInFull,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ds("miniplayer.restore"),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Trascinamento della finestra mini SENZA title bar: STESSA formula della
 * title bar (step 3o, nessun accumulo, posizione = funzione pura del mouse
 * sullo schermo) con due differenze:
 *  - niente doppio click (il click sulla copertina e' il restore);
 *  - se un figlio sta gia' CONSUMANDO il movimento (lo slider durante un
 *    seek) il trascinamento non parte e non si inietta nel gesto.
 */
private fun Modifier.miniPlayerWindowDrag(window: Window): Modifier = pointerInput(window) {
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
                // un figlio (slider) sta gestendo il gesto: non spostare la finestra
                if (change.isConsumed) {
                    if (!moved) continue else break
                }
                val delta = change.position - down.position
                if (!moved) {
                    if (delta.getDistance() > slop) {
                        moved = true
                        // fissati UNA sola volta all'inizio del trascinamento:
                        // coordinate SCHERMO del mouse + posizione corrente della
                        // finestra (stessa formula collaudata della title bar:
                        // nessun accumulo, nessun loop di feedback, niente sfarfallio)
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
