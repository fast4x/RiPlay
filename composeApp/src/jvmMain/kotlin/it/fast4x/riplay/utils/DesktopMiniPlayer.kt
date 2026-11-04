package it.fast4x.riplay.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.data.models.Song
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
 * MODALITA' MINIMALE, stile "picture-in-picture" di Android.
 *
 * Come funziona (tutto derivato, nessun servizio in thread separati):
 *  - l'interruttore vive nelle Impostazioni -> tab Generale
 *    ([DesktopMiniPlayer.enabled], persistito subito su disco, default OFF);
 *  - main.kt compone [DesktopMiniPlayerWindow] SOLO quando la finestra
 *    principale e' ICONIFICATA (windowState.isMinimized, stato Compose
 *    osservabile) e il toggle e' attivo: appena la principale viene
 *    RIPRISTINATA la condizione torna falsa e la mini esce dalla
 *    composizione -> si chiude da sola. Nessun contatore, nessuna gara.
 *  - finestra Compose SEPARATA: senza bordi, SEMPRE IN PRIMO PIANO,
 *    non prende il focus quando compare (focusable = false: l'utente
 *    sta lavorando, la mini non deve rubare l'attivazione);
 *  - DENTRO: copertina del brano (click = torna alla finestra principale)
 *    e timeline con tempi sempre visibili; al passaggio del mouse compaiono
 *    i tasti precedente / play-pausa / successivo e il selettore di
 *    dimensione S/M/L (ricordato su disco); il volume e' lasciato ai
 *    controlli di sistema; l'etichetta a destra della timeline alterna con
 *    un click tempo totale e rimanente;
 *  - TRASCINAMENTO: la stessa formula della title bar (posizione assoluta
 *    del mouse, nessun accumulo -> nessuno sfarfallio), con un guard per
 *    non litigare con gli slider (se un figlio sta gia' consumando il
 *    movimento, la finestra non si muove);
 *  - POSIZIONE e DIMENSIONE ricordate in ~/.riplay/miniplayer.properties
 *    (salvataggio debounced durante il trascinamento + al restore); alla
 *    prima apertura compare in basso a destra dello schermo.
 *
 * Tip: la voce taskbar della mini viene rimossa dove il sistema lo
 * consente (java.awt.Window.Type.UTILITY, tipo "palettina"): su alcuni
 * window manager puo' restare comunque visibile, nessun danno.
 */
/** Dimensioni predefinite della mini, rapporto ~3:2. */
enum class MiniSize(val widthDp: Int, val heightDp: Int) {
    SMALL(240, 160),
    MEDIUM(300, 200),
    LARGE(380, 254)
}

object DesktopMiniPlayer {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("miniplayer.properties")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_POS_X = "posX"
    private const val KEY_POS_Y = "posY"
    private const val KEY_SIZE = "size"
    private const val KEY_SHOW_REMAINING = "showRemainingTime"

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

    private fun loadPrefs(): java.util.Properties = try {
        java.util.Properties().also { p ->
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        }
    } catch (t: Throwable) {
        java.util.Properties()
    }

    private fun savePrefs(p: java.util.Properties) = try {
        prefsFile.parentFile?.mkdirs()
        prefsFile.outputStream().use { p.store(it, "RiPlay desktop mini player") }
    } catch (t: Throwable) {
        // persistenza silenziosa: mai bloccare la UI per un file di preferenze
    }

    private fun readEnabled(): Boolean = try {
        loadPrefs().getProperty(KEY_ENABLED, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    private fun persistEnabled(value: Boolean) {
        savePrefs(loadPrefs().also { it.setProperty(KEY_ENABLED, value.toString()) })
    }

    /** Dimensione della mini (S/M/L), selezionabile in hover e ricordata. */
    var size: MiniSize by mutableStateOf(readSize())
        private set

    fun setSizeMiniPlayer(value: MiniSize) {
        if (size == value) return
        size = value
        savePrefs(loadPrefs().also { it.setProperty(KEY_SIZE, value.name) })
    }

    private fun readSize(): MiniSize = try {
        when (loadPrefs().getProperty(KEY_SIZE, MiniSize.MEDIUM.name).trim().uppercase()) {
            MiniSize.SMALL.name -> MiniSize.SMALL
            MiniSize.LARGE.name -> MiniSize.LARGE
            else -> MiniSize.MEDIUM
        }
    } catch (t: Throwable) {
        MiniSize.MEDIUM
    }

    /** Etichetta destra della timeline: true = tempo rimanente. */
    var showRemainingTime: Boolean by mutableStateOf(readShowRemainingTime())
        private set

    fun setShowRemainingTimeMiniPlayer(value: Boolean) {
        if (showRemainingTime == value) return
        showRemainingTime = value
        savePrefs(loadPrefs().also { it.setProperty(KEY_SHOW_REMAINING, value.toString()) })
    }

    private fun readShowRemainingTime(): Boolean = try {
        loadPrefs().getProperty(KEY_SHOW_REMAINING, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    /** Ultima posizione ricordata della mini (null = mai trascinata). */
    fun lastPosition(): WindowPosition? = try {
        val p = loadPrefs()
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
            savePrefs(
                loadPrefs().also {
                    it.setProperty(KEY_POS_X, position.x.value.toString())
                    it.setProperty(KEY_POS_Y, position.y.value.toString())
                }
            )
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
    // (o in basso a destra alla prima apertura) e la dimensione dal preset
    // corrente. rememberWindowState e' per-sito-di-composizione: a ogni
    // riapertura della mini riparte da qui.
    val preset = DesktopMiniPlayer.size
    val miniState = rememberWindowState(
        size = DpSize(preset.widthDp.dp, preset.heightDp.dp),
        position = DesktopMiniPlayer.lastPosition() ?: WindowPosition(Alignment.BottomEnd)
    )

    // Il selettore S/M/L ridimensiona la finestra al volo.
    LaunchedEffect(preset) {
        miniState.size = DpSize(preset.widthDp.dp, preset.heightDp.dp)
    }

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

    // Brano corrente (titolo/artista mostrati in hover): stesso poll lento.
    var song by remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(Unit) {
        while (isActive) {
            song = DesktopPlaybackHub.currentSong()
            delay(800)
        }
    }

    // Hover sull'intera finestra: mostra i tasti e il selettore di dimensione.
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B10))
            .miniHover { hovered = it }
            // trascinamento dalla qualunque zona libera
            .miniPlayerWindowDrag(window)
    ) {
        // ── Copertina a tutta finestra (stile PiP) ─────────────────────
        // I tasti e la timeline "galleggiano" sopra l'artwork, su un velo
        // sfumato che garantisce la leggibilita'.
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
                // ── Logo dell'app: targhetta sopra la copertina con fondo
                // semitrasparente, resta leggibile su qualsiasi artwork.
                // Solo con copertina vera: nel fallback il logo e' gia'
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
                // Al passaggio del mouse: info brano + suggerimento restore.
                MiniCoverHoverHint(
                    title = song?.title,
                    artist = song?.artistsText
                )
            }

            // ── Controlli: precedente / play-pausa / successivo ─────────
            // Al riposo restano solo tempi e timeline: i tasti compaiono
            // al passaggio del mouse e liberano lo spazio che occupano.
            AnimatedVisibility(
                visible = hovered,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
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
                    // Click = alterna tempo totale / rimanente (ricordato).
                    text = if (DesktopMiniPlayer.showRemainingTime && state.duration > 0L)
                        "-" + formatShortTime((state.duration - state.timestamp).coerceAtLeast(0L))
                    else formatShortTime(state.duration),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            DesktopMiniPlayer.setShowRemainingTimeMiniPlayer(!DesktopMiniPlayer.showRemainingTime)
                        },
                    textAlign = TextAlign.End
                )
            }
        }

        // ── Selettore di dimensione (S/M/L), solo in hover ─────────────
        // Ultimo livello della finestra: resta sopra il velo con titolo e
        // artista e i suoi tasti sono sempre raggiungibili.
        if (hovered) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .semantics { contentDescription = ds("miniplayer.size") }
                    // la pillola assorbe i passaggi del mouse: le fessure tra
                    // le lettere non devono finire sulla copertina
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent()
                        }
                    }
            ) {
                MiniSize.values().forEach { s ->
                    val active = s == DesktopMiniPlayer.size
                    Text(
                        text = s.name.first().toString(),
                        color = if (active) RiPlayAccent else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { DesktopMiniPlayer.setSizeMiniPlayer(s) }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
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
 * Velo che compare sulla copertina quando il mouse ci passa sopra: titolo e
 * artista del brano, icona "espandi" e suggerimento di restore. Pura UI:
 * NON consuma nulla, il click resta alla copertina e il trascinamento alla
 * finestra.
 */
@Composable
private fun MiniCoverHoverHint(
    title: String?,
    artist: String?
) {
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
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.42f))
                    .padding(horizontal = 10.dp)
            ) {
                Icon(
                    Icons.Filled.OpenInFull,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.height(6.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ds("miniplayer.restore"),
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Trascinamento della finestra mini senza title bar (stessa formula della
 * title bar: posizione = funzione pura del mouse sullo schermo), con due
 * differenze:
 *  - niente doppio click (il click sulla copertina e' il restore);
 *  - se un figlio sta gia' CONSUMANDO il movimento (slider durante un seek)
 *    il trascinamento non parte.
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

/** tracciamento hover dell'intera finestra (controlli extra al passaggio del mouse) */
private fun Modifier.miniHover(onHover: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Enter -> onHover(true)
                PointerEventType.Exit -> onHover(false)
            }
        }
    }
}
