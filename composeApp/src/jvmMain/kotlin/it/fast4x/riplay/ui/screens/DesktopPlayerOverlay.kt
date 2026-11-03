package it.fast4x.riplay.ui.screens

import it.fast4x.riplay.i18n.ds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import extension.formatTimestamp
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.player.lyrics.DesktopLyricsRepository
import it.fast4x.riplay.player.lyrics.LyricsUiState
import it.fast4x.riplay.ui.components.PremiumSlider
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.ui.components.AudioVisualizer
import it.fast4x.riplay.ui.components.LyricsPanel
import it.fast4x.riplay.ui.components.VisualizerStyle
import it.fast4x.riplay.ui.components.PlayQueuePanel
import it.fast4x.riplay.ui.QueuedSong
import it.fast4x.riplay.utils.DesktopSleepTimer
import it.fast4x.riplay.utils.hiResCoverUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import player.PlayerController
import player.PlayerState
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import riplay.composeapp.generated.resources.chevron_down
import kotlin.math.roundToLong


@Composable
fun DesktopPlayerOverlay(
    visible: Boolean,
    song: Song?,
    controller: PlayerController?,
    videoVisible: Boolean,
    engineLabel: String,
    onCollapse: () -> Unit,
    canGoPrevious: Boolean = false,
    canGoNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    queue: List<QueuedSong> = emptyList(),
    onQueuePlay: (Song) -> Unit = {},
    onQueueRemove: (Song) -> Unit = {},
    onQueueClear: () -> Unit = {},
    onQueueMove: (Long, Long) -> Unit = { _, _ -> },
    playingUid: Long? = null,
    autoRadioEnabled: Boolean = true,
    onToggleAutoRadio: () -> Unit = {},
    onAddRadio: () -> Unit = {},
    radioLoading: Boolean = false,
    radioFeedback: String? = null
) {

    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    // focus per il tasto ESC (onPreviewKeyEvent senza focus non
    // riceve mai eventi: per questo la chiusura non funzionava)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    // Pannello coda aperto/chiuso (toggle con l'icona in alto a destra)
    var queueOpen by remember { mutableStateOf(false) }

    // pannello TESTI aperto/
    // chiuso (toggle con l'icona "Testo" in alto a destra, accanto alla coda).
    var lyricsOpen by remember { mutableStateOf(false) }

    // Visualizer del player. Parte con lo
    // SPETTRO; il pulsante in alto CICLA gli stili (spettro -> livello -> off).
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM) }

    // Stato del testo del brano corrente, osservato dal repository
    // (fetch cache db -> LRCLIB -> KuGou attivato quando il pannello si apre).
    // Il flow di fallback (Idle) serve quando l'overlay e' composto senza brano.
    val lyricsFlow = remember(song?.id) {
        song?.let { DesktopLyricsRepository.stateFor(it.id) }
            ?: MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    }
    val lyricsState by lyricsFlow.collectAsState()
    val overlayScope = rememberCoroutineScope()

    // Scarica il testo quando il pannello si apre (o cambia brano):
    // niente rete finche' l'utente non lo chiede, e solo UNA volta per brano
    // (le successive letture vengono dalla cache del repository/db).
    LaunchedEffect(lyricsOpen, song?.id) {
        if (lyricsOpen && song != null) DesktopLyricsRepository.load(song)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)) +
                fadeIn(tween(280)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(280)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) +
                fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Sfumatura sottile invece del nero piatto
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF060606), Color(0xFF0E100D), Color(0xFF050505))
                    )
                )
                .focusRequester(focusRequester)
                .focusable()
                // ESC chiude l'overlay (comodo da desktop)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Escape
                    ) {
                        onCollapse()
                        true
                    } else false
                }
                // Le aree vuote CONSUMANO i tap: niente click che
                // passano alla pagina sottostante
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Barra superiore: CHIUSURA (pulsante circolare ben visibile)
                // + titolo contesto + motore attivo
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_down),
                            contentDescription = ds("overlay.close_player"),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        ds("overlay.playing"),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        engineLabel,
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    // Pulsante RADIO: aggiunge i BRANI SIMILI del
                    // brano in riproduzione in fondo alla coda (come la
                    // funzione radio di Android). Feedback inline.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { if (!radioLoading) onAddRadio() }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        if (radioLoading) {
                            CircularProgressIndicator(
                                color = RiPlayAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Radio,
                                contentDescription = ds("player.radio"),
                                tint = RiPlayAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            radioFeedback ?: "Radio",
                            color = if (radioFeedback != null) RiPlayAccent else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Toggle del pannello CODA DI RIPRODUZIONE
                    // (icona lista + contatore brani in coda)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { queueOpen = !queueOpen }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = if (queueOpen) 0.16f else 0.08f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Filled.List,
                            contentDescription = ds("overlay.queue_title"),
                            tint = if (queueOpen) RiPlayAccent else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            ds("overlay.queue_count", "n" to queue.size),
                            color = if (queueOpen) RiPlayAccent else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (queueOpen) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // STEP 5a: toggle del pannello TESTI (stessa identita' del
                    // pulsante coda: pill con icona + contatore/etichetta)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { lyricsOpen = !lyricsOpen }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = if (lyricsOpen) 0.16f else 0.08f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lyrics,
                            contentDescription = ds("overlay.lyrics_toggle"),
                            tint = if (lyricsOpen) RiPlayAccent else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            ds("overlay.lyrics_toggle"),
                            color = if (lyricsOpen) RiPlayAccent else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (lyricsOpen) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // VISUALIZER: il click CICLA gli stili (spettro ->
                    // livello -> off -> spettro). L'etichetta mostra lo stile attivo.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { visualizerStyle = visualizerStyle.next() }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = if (visualizerStyle != VisualizerStyle.NONE) 0.16f else 0.08f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = ds("overlay.visualizer_toggle"),
                            tint = if (visualizerStyle != VisualizerStyle.NONE) RiPlayAccent
                            else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            ds(
                                when (visualizerStyle) {
                                    VisualizerStyle.SPECTRUM -> "visualizer.spectrum"
                                    VisualizerStyle.LEVEL -> "visualizer.level"
                                    VisualizerStyle.NONE -> "visualizer.off"
                                }
                            ),
                            color = if (visualizerStyle != VisualizerStyle.NONE) RiPlayAccent
                            else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (visualizerStyle != VisualizerStyle.NONE) FontWeight.SemiBold
                            else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Cover grande (Android mostra la copertina quando il brano
                // non e' un video). Con il video attivo la cover resta come
                // sfondo d'arte attenuata + nota sulla posizione del video.
                if (song?.thumbnailUrl != null) {
                    val hiResCover = remember(song.id) { song.thumbnailUrl?.hiResCoverUrl() }
                    val coverScale = remember { Animatable(0.88f) }
                    LaunchedEffect(visible, song.id) {
                        if (visible) {
                            coverScale.snapTo(0.88f)
                            coverScale.animateTo(
                                1f,
                                spring(dampingRatio = 0.78f, stiffness = 340f)
                            )
                        }
                    }
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center
                    ) {
                        val coverSize = minOf(maxWidth * 0.52f, maxHeight * 0.52f)
                        AsyncImage(
                            model = hiResCover,
                            contentDescription = ds("player.song_cover"),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = coverScale.value
                                    scaleY = coverScale.value
                                }
                                .shadow(28.dp, RoundedCornerShape(22.dp))
                                .size(coverSize)
                                .clip(RoundedCornerShape(22.dp))
                                .alpha(if (videoVisible) 0.35f else 1f)
                        )
                    }
                    if (videoVisible) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = Color.Green.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                ds("overlay.video_active_note"),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        colorFilter = ColorFilter.tint(Color.Green.copy(alpha = 0.6f)),
                        contentDescription = null,
                        modifier = Modifier.size(140.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Titolo + artista
                Text(
                    song?.title ?: ds("overlay.no_song"),
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                song?.artistsText?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.weight(1f))

                // VISUALIZER tra il titolo e la timeline (si attiva e
                // cambia stile col pulsante nella barra superiore). E' pura
                // UI: non tocca il motore di riproduzione.
                if (visualizerStyle != VisualizerStyle.NONE) {
                    Spacer(Modifier.height(12.dp))
                    AudioVisualizer(
                        style = visualizerStyle,
                        isPlaying = state.isPlaying,
                        seed = song?.id ?: "riplay",
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(if (visualizerStyle == VisualizerStyle.LEVEL) 22.dp else 58.dp)
                    )
                }

                val animatedTimestamp by animateFloatAsState(state.timestamp.toFloat())

                PremiumSlider(
                    value = animatedTimestamp,
                    onValueChange = { /* durante il drag mostra solo il valore locale */ },
                    onValueChangeFinished = { controller?.seekTo(it.roundToLong()) },
                    valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                    showTimeBubble = true,
                    modifier = Modifier.fillMaxWidth(0.86f)
                )

                Row(
                    Modifier.fillMaxWidth(0.86f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        state.timestamp.formatTimestamp(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        state.duration.formatTimestamp(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Controlli: INDIETRO + play/pausa + AVANTI (cronologia dei
                // brani ascoltati in sessione) — come la PlayerScreen Android
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = canGoPrevious
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious, ds("player.previous"),
                            tint = RiPlayAccent.copy(alpha = if (canGoPrevious) 1f else 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // SPlay/pausa dentro un cerchio d'accento
                    IconButton(
                        onClick = {
                            if (state.isPlaying) controller?.pause() else controller?.play()
                        },
                        enabled = controller != null,
                        modifier = Modifier
                            .size(76.dp)
                            .background(RiPlayAccent.copy(alpha = 0.13f), CircleShape)
                    ) {
                        if (state.isPlaying) {
                            Icon(
                                Icons.Filled.Pause, ds("player.pause"),
                                tint = RiPlayAccent,
                                modifier = Modifier.size(50.dp)
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PlayArrow, ds("player.play"),
                                tint = RiPlayAccent,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        enabled = canGoNext
                    ) {
                        Icon(
                            Icons.Filled.SkipNext, ds("player.next"),
                            tint = RiPlayAccent.copy(alpha = if (canGoNext) 1f else 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Mute + volume
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { controller?.toggleSound() },
                        enabled = controller != null
                    ) {
                        when {
                            state.isMuted || state.volume == 0f ->
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeOff, ds("player.volume_off"),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            state.volume < 0.5f ->
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeDown, ds("player.volume_low"),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            else ->
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp, ds("player.volume_high"),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                        }
                    }

                    // STEP 3h: volume con lo slider premium (il volume resta
                    // continuo durante il drag: e' un comando leggero)
                    PremiumSlider(
                        value = state.volume,
                        onValueChange = { controller?.setVolume(it) },
                        modifier = Modifier.width(280.dp)
                    )

                    // TIMER DI SPEGNIMENTO
                    var sleepMenuOpen by remember { mutableStateOf(false) }
                    val sleepEndsAt = DesktopSleepTimer.endsAtMs
                    var sleepNowMs by remember { mutableStateOf(System.currentTimeMillis()) }
                    if (sleepEndsAt != null) {
                        LaunchedEffect(sleepEndsAt) {
                            while (DesktopSleepTimer.endsAtMs != null) {
                                sleepNowMs = System.currentTimeMillis()
                                delay(1_000L)
                            }
                        }
                    }
                    IconButton(onClick = { sleepMenuOpen = true }) {
                        Icon(
                            Icons.Filled.Bedtime, ds("sleeptimer.title"),
                            tint = if (sleepEndsAt != null) RiPlayAccent
                                   else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (sleepEndsAt != null) {
                        val remainingSec =
                            ((sleepEndsAt - sleepNowMs).coerceAtLeast(0L)) / 1_000L
                        Text(
                            "%d:%02d".format(remainingSec / 60, remainingSec % 60),
                            color = RiPlayAccent,
                            fontSize = 12.sp,
                            modifier = Modifier.width(46.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = sleepMenuOpen,
                        onDismissRequest = { sleepMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    ds("sleeptimer.title"),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        listOf(15, 30, 45, 60, 90).forEach { minutes ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        ds("sleeptimer.min", "n" to minutes),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    DesktopSleepTimer.start(minutes)
                                    sleepMenuOpen = false
                                }
                            )
                        }
                        if (sleepEndsAt != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        ds("sleeptimer.cancel"),
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    DesktopSleepTimer.cancel()
                                    sleepMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    ds("overlay.close_hint"),
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )

                Spacer(Modifier.weight(1f))
            }

            AnimatedVisibility(
                visible = queueOpen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {

                PlayQueuePanel(
                    queue = queue,
                    onPlay = onQueuePlay,
                    onRemove = onQueueRemove,
                    onClear = onQueueClear,
                    onMove = onQueueMove,
                    // STEP 3ac: marcatura "in riproduzione" + auto-radio
                    playingUid = playingUid,
                    autoRadioEnabled = autoRadioEnabled,
                    onToggleAutoRadio = onToggleAutoRadio,
                    onClose = { queueOpen = false },
                    topPadding = 70.dp
                )
            }

            AnimatedVisibility(
                visible = lyricsOpen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                LyricsPanel(
                    state = lyricsState,
                    timestampMs = state.timestamp,
                    isPlaying = state.isPlaying,
                    onSeek = { ms -> controller?.seekTo(ms.coerceAtLeast(0L)) },
                    onReload = {
                        song?.let { s ->
                            overlayScope.launch { DesktopLyricsRepository.load(s, force = true) }
                        }
                    },
                    onClose = { lyricsOpen = false },
                    // sotto la barra superiore, come il pannello coda
                    topPadding = 70.dp
                )
            }
        }
    }
}

