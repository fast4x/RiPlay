package it.fast4x.riplay.utils

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
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

/** minimo di finestra (px) che deve restare su schermo: overlay mai irraggiungibile */
private const val KARAOKE_MIN_VISIBLE_PX = 64f

/** Stile di evidenza della riga corrente. */
enum class KaraokeMode { LINE, WORD }

/**
 * OVERLAY KARAOKE.
 *
 * Piccola finestra sempre in primo piano con la riga corrente del testo
 * (+ la successiva) sincronizzata sul timestamp del player.
 *
 *  - toggle in Impostazioni -> Generale (default OFF), effetto immediato;
 *  - riga corrente con evidenza a scelta: riga intera in accento oppure
 *    parola per parola (selettore Riga/Parola al passaggio del mouse);
 *  - dimensione del testo regolabile con i pulsanti A-/A+ (visibili al
 *    passaggio del mouse, in alto a destra) e ricordata su disco;
 *  - Ctrl+Maiusc+K (installata in main.kt) accende/spegne l'overlay;
 *  - trascinabile da qualunque zona, posizione ricordata e sempre
 *    raggiungibile (mai completamente fuori schermo);
 *  - senza testo sincronizzato mostra titolo e artista.
 */
object DesktopKaraokeOverlay {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("karaoke.properties")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_POS_X = "posX"
    private const val KEY_POS_Y = "posY"
    private const val KEY_TEXT_SCALE = "textScale"
    private const val KEY_MODE = "mode"

    /** Dimensioni base della finestra (dp), prima dell'applicazione dello scaling del testo. */
    const val BASE_WIDTH_DP = 620f
    const val BASE_HEIGHT_DP = 132f

    private const val MIN_SCALE = 0.8f
    private const val MAX_SCALE = 1.6f

    var enabled: Boolean by mutableStateOf(readEnabled())
        private set

    fun setEnabledKaraokeOverlay(value: Boolean) {
        if (enabled == value) return
        enabled = value
        savePrefs(loadPrefs().also { it.setProperty(KEY_ENABLED, value.toString()) })
    }

    /** Scala della dimensione del testo (1 = dimensione standard). */
    var textScale: Float by mutableStateOf(readTextScale())
        private set

    fun changeTextScale(delta: Float) = setTextScaleKaraokeOverlay(textScale + delta)

    fun setTextScaleKaraokeOverlay(value: Float) {
        val v = value.coerceIn(MIN_SCALE, MAX_SCALE)
        if (textScale == v) return
        textScale = v
        savePrefs(loadPrefs().also { it.setProperty(KEY_TEXT_SCALE, v.toString()) })
    }

    /** Stile di evidenza della riga corrente (riga intera o parola per parola). */
    var mode: KaraokeMode by mutableStateOf(readMode())
        private set

    fun setKaraokeMode(value: KaraokeMode) {
        if (mode == value) return
        mode = value
        savePrefs(loadPrefs().also { it.setProperty(KEY_MODE, value.name) })
    }

    private fun readMode(): KaraokeMode = try {
        when (loadPrefs().getProperty(KEY_MODE, KaraokeMode.WORD.name).trim().uppercase()) {
            KaraokeMode.LINE.name -> KaraokeMode.LINE
            else -> KaraokeMode.WORD
        }
    } catch (t: Throwable) {
        KaraokeMode.WORD
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
        prefsFile.outputStream().use { p.store(it, "RiPlay desktop karaoke overlay") }
    } catch (t: Throwable) {
        // la persistenza non deve mai bloccare la UI
    }

    private fun readEnabled(): Boolean = try {
        loadPrefs().getProperty(KEY_ENABLED, "false").trim().equals("true", ignoreCase = true)
    } catch (t: Throwable) {
        false
    }

    private fun readTextScale(): Float = try {
        loadPrefs().getProperty(KEY_TEXT_SCALE, "1.0").trim()
            .toFloatOrNull()?.coerceIn(MIN_SCALE, MAX_SCALE) ?: 1f
    } catch (t: Throwable) {
        1f
    }

    /** Ultima posizione ricordata (null = mai trascinata o non recuperabile). */
    fun lastPosition(): WindowPosition? = try {
        val p = loadPrefs()
        val x = p.getProperty(KEY_POS_X)?.toFloatOrNull()
        val y = p.getProperty(KEY_POS_Y)?.toFloatOrNull()
        if (x != null && y != null &&
            x >= -200f && y >= -200f && x <= 20000f && y <= 20000f &&
            isOnSomeScreen(x, y, BASE_WIDTH_DP, BASE_HEIGHT_DP * MAX_SCALE)
        ) WindowPosition(x.dp, y.dp) else null
    } catch (t: Throwable) {
        null
    }

    /** Salvataggio posizione (chiamato debounced mentre si trascina). */
    fun rememberPosition(position: WindowPosition) {
        try {
            if (!position.isSpecified) return
            if (!isOnSomeScreen(
                    position.x.value, position.y.value,
                    BASE_WIDTH_DP, BASE_HEIGHT_DP * MAX_SCALE
                )
            ) return
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

    /** true se il rettangolo (px) resta visibile per almeno KARAOKE_MIN_VISIBLE_PX su uno schermo. */
    fun isOnSomeScreen(x: Float, y: Float, w: Float, h: Float): Boolean = try {
        val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.screenDevices.any { dev ->
            val b = dev.defaultConfiguration.bounds
            val ix = maxOf(x, b.x.toFloat())
            val iy = maxOf(y, b.y.toFloat())
            val ix2 = minOf(x + w, (b.x + b.width).toFloat())
            val iy2 = minOf(y + h, (b.y + b.height).toFloat())
            (ix2 - ix) >= KARAOKE_MIN_VISIBLE_PX && (iy2 - iy) >= KARAOKE_MIN_VISIBLE_PX
        }
    } catch (t: Throwable) {
        true
    }

    /** Area (px) dello schermo primario: destinazione del ripristino d'emergenza. */
    fun defaultScreenBounds(): java.awt.Rectangle = try {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration.bounds
    } catch (t: Throwable) {
        java.awt.Rectangle(0, 0, 1920, 1080)
    }

    /** Bounding box (px) di tutti gli schermi: serve al clamp del drag. */
    fun screensUnion(): java.awt.Rectangle = try {
        val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        val union = java.awt.Rectangle()
        for (dev in ge.screenDevices) union.add(dev.defaultConfiguration.bounds)
        if (union.isEmpty) defaultScreenBounds() else union
    } catch (t: Throwable) {
        defaultScreenBounds()
    }
}

/** Finestra overlay karaoke: main.kt la compone solo con toggle attivo e brano caricato. */
@Composable
fun DesktopKaraokeOverlayWindow() {

    val scale = DesktopKaraokeOverlay.textScale

    val overlayState = rememberWindowState(
        size = DpSize(
            DesktopKaraokeOverlay.BASE_WIDTH_DP.dp,
            (DesktopKaraokeOverlay.BASE_HEIGHT_DP * scale).dp
        ),
        position = DesktopKaraokeOverlay.lastPosition() ?: WindowPosition(Alignment.BottomCenter)
    )

    // Il cambio di scala (A-/A+) ridimensiona la finestra al volo.
    LaunchedEffect(scale) {
        overlayState.size = DpSize(
            DesktopKaraokeOverlay.BASE_WIDTH_DP.dp,
            (DesktopKaraokeOverlay.BASE_HEIGHT_DP * scale).dp
        )
    }

    // Posizione salvata UNA sola volta a gesto fermo (debounced con collectLatest).
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
        // Niente voce in taskbar dove il sistema lo consente (come la mini player).
        LaunchedEffect(Unit) {
            runCatching {
                val frame = window as? java.awt.Frame
                if (frame != null) frame.type = java.awt.Window.Type.UTILITY
            }
        }

        // Se la finestra risultasse comunque fuori schermo al montaggio
        // (scaling o monitor cambiati tra le sessioni), torna in basso al centro.
        LaunchedEffect(Unit) {
            delay(800)
            runCatching {
                val loc = window.location
                val visible = DesktopKaraokeOverlay.isOnSomeScreen(
                    loc.x.toFloat(), loc.y.toFloat(),
                    window.width.toFloat(), window.height.toFloat()
                )
                if (!visible) {
                    val b = DesktopKaraokeOverlay.defaultScreenBounds()
                    window.setLocation(
                        b.x + (b.width - window.width) / 2,
                        b.y + b.height - window.height - 48
                    )
                }
            }
        }

        DesktopKaraokeOverlayContent(
            window = window
        )
    }
}

/** Una parola della riga corrente con il suo intervallo di tempo (ms). */
private data class KaraokeWord(val text: String, val startMs: Long, val endMs: Long)

/**
 * Pagina karaoke di un istante: parole della riga corrente + testo successivo,
 * come snapshot DENTRO la chiave di transizione (il blocco in uscita continua
 * a mostrare le proprie righe anche se brano o indice cambiano nel frattempo).
 */
private data class KaraokePage(
    val songId: String?,
    val lineIndex: Int,
    val words: List<KaraokeWord>,
    val next: String?
)

/**
 * Ripartisce le parole lungo la durata della riga, in proporzione alla
 * lunghezza: karaoke senza timestamp parola-per-parola nel sorgente LRC.
 */
private fun karaokeWords(text: String, startMs: Long, endMs: Long): List<KaraokeWord> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    if (words.size == 1) return listOf(KaraokeWord(words[0], startMs, endMs))
    val duration = (endMs - startMs).coerceAtLeast(600L).toFloat()
    val weights = words.map { it.length + 1 }
    val total = weights.sum().toFloat()
    var acc = 0f
    return words.mapIndexed { i, w ->
        val s = startMs + (acc / total * duration).toLong()
        acc += weights[i]
        KaraokeWord(w, s, startMs + (acc / total * duration).toLong())
    }
}

@Composable
private fun DesktopKaraokeOverlayContent(
    window: Window
) {

    // Brano corrente: chiusura ritardata letta a un poll lento (un tick a
    // 800ms coglie anche i passaggi di brano senza eventi espliciti).
    var song by remember { mutableStateOf(DesktopPlaybackHub.currentSong()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            song = DesktopPlaybackHub.currentSong()
            delay(800)
        }
    }

    // Controller del motore attivo (engine e' mutableStateOf: il cambio
    // motore e' osservato in composizione).
    val controller = DesktopPlaybackHub.manager?.active
    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    // Testo del brano: caricato una sola volta per brano e solo se Idle;
    // lo stato e' condiviso con il pannello testi (zero rete in piu').
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

    val lines = (lyricsState as? LyricsUiState.Synced)?.lines
    var currentIndex = -1
    if (lines != null) {
        for (i in lines.indices) {
            if (lines[i].timeMs <= state.timestamp) currentIndex = i else break
        }
    }

    val scale = DesktopKaraokeOverlay.textScale
    val wordMode = DesktopKaraokeOverlay.mode == KaraokeMode.WORD
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B10))
            .karaokeHover { hovered = it }
            // trascinamento da qualunque zona (come title bar e mini player)
            .karaokeOverlayDrag(window)
    ) {
        // Blocco testo centrato nel telaio della finestra: in alto restano
        // liberi icona e selettori, senza coprire le righe.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lines != null) {
                val currentText = (lines.getOrNull(currentIndex)?.text ?: "").ifBlank { "\u266A" }
                AnimatedContent(
                    targetState = KaraokePage(
                        songId = currentSong?.id,
                        lineIndex = currentIndex,
                        words = karaokeWords(
                            currentText,
                            lines.getOrNull(currentIndex)?.timeMs ?: 0L,
                            lines.getOrNull(currentIndex + 1)?.timeMs ?: (
                                (lines.getOrNull(currentIndex)?.timeMs ?: 0L) + 6_000L
                                )
                        ),
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
                    // Larghezza fissa, altezza avvolta: il blocco resta
                    // centrato anche quando la riga va a capo.
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrentLineText(
                            words = page.words,
                            wordMode = wordMode,
                            fontSize = (19f * scale).sp,
                            lineHeight = (26f * scale).sp,
                            playing = state.isPlaying,
                            timestampMs = state.timestamp
                        )
                        val next = page.next
                        if (next != null) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = next,
                                color = Color.White.copy(alpha = if (state.isPlaying) 0.45f else 0.30f),
                                fontSize = (13f * scale).sp,
                                lineHeight = (18f * scale).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // Senza testo sincronizzato: "now playing" compatto
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

        // Targhetta con l'icona dell'app
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

        // Selettore di stile (Riga/Parola) e dimensione del testo: visibili
        // al passaggio del mouse, come ultimo livello sopra al testo.
        if (hovered) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                OverlayChip(
                    label = ds("karaoke.mode.line"),
                    active = DesktopKaraokeOverlay.mode == KaraokeMode.LINE
                ) { DesktopKaraokeOverlay.setKaraokeMode(KaraokeMode.LINE) }
                Text(
                    text = "\u00B7",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp
                )
                OverlayChip(
                    label = ds("karaoke.mode.word"),
                    active = DesktopKaraokeOverlay.mode == KaraokeMode.WORD
                ) { DesktopKaraokeOverlay.setKaraokeMode(KaraokeMode.WORD) }
                Spacer(Modifier.width(10.dp))
                OverlayChip("A\u2212") { DesktopKaraokeOverlay.changeTextScale(-0.1f) }
                Text(
                    text = "${(DesktopKaraokeOverlay.textScale * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
                OverlayChip("A+") { DesktopKaraokeOverlay.changeTextScale(+0.1f) }
            }
        }
    }
}

/**
 * Riga corrente: in modalita' parola le parole gia' cantate sono in accento,
 * quella attiva ha lo sfondo accent, le future restano attenuate; in
 * modalita' riga tutta la riga e' in accento (idem interludi e righe con
 * una sola parola).
 */
@Composable
private fun CurrentLineText(
    words: List<KaraokeWord>,
    wordMode: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    playing: Boolean,
    timestampMs: Long
) {
    if (!wordMode || words.size <= 1) {
        Text(
            text = words.joinToString(" ") { it.text }.ifBlank { "\u266A" },
            color = RiPlayAccent.copy(alpha = if (playing) 1f else 0.7f),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = lineHeight,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        return
    }
    val accent = RiPlayAccent
    val annotated = buildAnnotatedString {
        words.forEachIndexed { i, w ->
            if (i > 0) append(" ")
            val from = length
            append(w.text)
            addStyle(
                when {
                    timestampMs >= w.endMs -> SpanStyle(color = accent)
                    timestampMs >= w.startMs -> SpanStyle(
                        color = accent,
                        background = accent.copy(alpha = 0.22f)
                    )
                    else -> SpanStyle(color = Color.White.copy(alpha = if (playing) 0.55f else 0.40f))
                },
                from, length
            )
        }
    }
    Text(
        text = annotated,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = lineHeight,
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

/** Voce dei controlli in hover; attiva = evidenziata dall'accento. */
@Composable
private fun OverlayChip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (active) RiPlayAccent else Color.White,
        fontSize = 11.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** tracciamento hover della finestra (per mostrare/nascondere i controlli) */
private fun Modifier.karaokeHover(onHover: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
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

/**
 * Trascinamento della finestra overlay senza title bar (come title bar e
 * mini player): posizione = funzione pura del mouse sullo schermo, con clamp
 * che tiene sempre una parte di finestra visibile.
 */
private fun Modifier.karaokeOverlayDrag(window: Window): Modifier = pointerInput(window) {
    awaitPointerEventScope {
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
                // un figlio sta gestendo il gesto (es. pulsanti): non spostare
                if (change.isConsumed) {
                    if (!moved) continue else break
                }
                val delta = change.position - down.position
                if (!moved) {
                    if (delta.getDistance() > slop) {
                        moved = true
                        screenStart = java.awt.MouseInfo.getPointerInfo()?.location
                        windowStart = window.location
                    }
                }
                if (moved) {
                    val mouse = java.awt.MouseInfo.getPointerInfo()?.location
                    val s0 = screenStart
                    val w0 = windowStart
                    if (mouse != null && s0 != null && w0 != null) {
                        val nx = w0.x + (mouse.x - s0.x)
                        val ny = w0.y + (mouse.y - s0.y)
                        val b = DesktopKaraokeOverlay.screensUnion()
                        val minX = (b.x - window.width + KARAOKE_MIN_VISIBLE_PX).toInt()
                        val maxX = (b.x + b.width - KARAOKE_MIN_VISIBLE_PX).toInt()
                        val minY = (b.y - window.height + KARAOKE_MIN_VISIBLE_PX).toInt()
                        val maxY = (b.y + b.height - KARAOKE_MIN_VISIBLE_PX).toInt()
                        window.setLocation(
                            if (minX < maxX) nx.coerceIn(minX, maxX) else nx,
                            if (minY < maxY) ny.coerceIn(minY, maxY) else ny
                        )
                    }
                    change.consume()
                }
            }
        }
    }
}
