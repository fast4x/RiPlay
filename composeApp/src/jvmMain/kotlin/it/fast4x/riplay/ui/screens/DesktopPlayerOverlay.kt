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
// STEP 3i: icone del pannello coda di riproduzione
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
// STEP 3j: maniglia di trascinamento + icona RADIO (brani simili in coda)
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import it.fast4x.riplay.ui.components.PremiumSlider
import it.fast4x.riplay.ui.components.RiPlayAccent
// STEP 3m: pannello coda estratto in ui/components (ora usato anche nella
// colonna destra della finestra principale, senza aprire il player full)
import it.fast4x.riplay.ui.components.PlayQueuePanel
// STEP 3j: wrapper del brano in coda con uid stabile (serve al drag&drop)
import it.fast4x.riplay.ui.QueuedSong
// STEP 3n: cover alla risoluzione alta (richiesta utente n.7)
import it.fast4x.riplay.utils.hiResCoverUrl
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import player.PlayerController
import player.PlayerState
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import riplay.composeapp.generated.resources.chevron_down
import kotlin.math.roundToLong

/**
 * STEP 3c: PLAYER ESPANDIBILE in stile Android (l'equivalente desktop della
 * PlayerScreen Android che si apre dal mini player). Si apre a FINESTRA
 * INTERA sopra tutto (scaffold, barre incluse) con animazione dal basso e
 * contiene cio' che il pannello player non puo' mostrare comodamente:
 *  - la COVER GRANDE del brano (o attenuata quando il video e' attivo, con
 *    l'indicazione che il video resta visibile nel pannello player);
 *  - titolo e artista;
 *  - la TIMELINA con seek (slider) e i due timestamp;
 *  - i pulsanti AVANTI/INDIETRO (STEP 3d: cronologia dei brani ascoltati
 *    nella sessione), play/pausa, mute e volume;
 *  - l'interruttore video/cover (stessa semantica del db isAudioOnly);
 *  - la chiusura: pulsante in alto a sinistra oppure il tasto ESC.
 *
 * STEP 3d (correzioni dalla prova utente):
 *  - CHIUSURA AFFIDABILE: il chevron ora e' un pulsante circolare ben
 *    visibile (prima un'immagine 24dp difficile da colpire) e il tasto ESC
 *    funziona davvero: l'overlay RICHIEDE il focus quando si apre
 *    (prima onPreviewKeyEvent non riceveva mai gli eventi, senza focus);
 *  - I CLICK NON PASSANO PIU' ATTRAVERSO: le aree vuote dell'overlay
 *    consumano i tocchi (prima un click accanto alla cover finiva sulla
 *    pagina sottostante e faceva partire un altro brano);
 *  - PULSANTI AVANTI/INDIETRO visibili accanto a play/pausa.
 *
 * STEP 3g (dalla prova utente: "aggiungere uno chevron down in alto nel
 * player full per chiuderlo"): il chevron c'era gia' (in alto a sinistra)
 * ma era piccolo e poco contrastato (cerchio bianco al 14% su nero) e la
 * parte bassa restava coperta dal pannello bianco del browser (fix nel
 * KcefVideoSurface). Ora e' un bersaglio chiaro: cerchio 48dp al 22%,
 * icona 28dp, e il suggerimento in basso lo nomina esplicitamente.
 *
 * Il motore di riproduzione NON viene toccato: l'audio prosegue con
 * l'overlay aperto (il livello Swing del pannello player viene solo
 * nascosto sotto di esso, vedi KcefVideoSurface.overlayOpen).
 *
 * STEP 3h (migliorie visive, richiesta utente "slider orizzontali della
 * timeline e del volume piu' premium" + polish):
 *  - SLIDER PREMIUM (PremiumSlider): track sottile che si ingrandisce
 *    al passaggio del mouse, riempimento a gradiente, thumb bianco con
 *    alone, bolla del tempo sull'anteprima del seek; la timeline fa UN
 *    solo seekTo al rilascio (prima lo Slider material chiamava seekTo
 *    continuamente durante il drag);
 *  - ENTRATA PIU' MORBIDA: slide + fade + leggero scale; la cover grande
 *    appare con una piccola animazione di scala a molla e angoli
 *    arrotondati con ombra;
 *  - SFONDO con sfumatura sottile (non piu' nero piatto);
 *  - PLAY/PAUSA dentro un cerchio d'accento (come i player chic);
 *  - accento unificato (RiPlayAccent) su icone e cursori.
 */
@Composable
fun DesktopPlayerOverlay(
    visible: Boolean,
    song: Song?,
    controller: PlayerController?,
    videoVisible: Boolean,
    // STEP 3k (richiesta utente n.3): il pulsante audio/video e' RIMOSSO
    // dal player full (per guardare il video si deve comunque tornare alla
    // home: il toggle vive solo nella barra del player di sinistra).
    // [videoVisible] resta: decide se la cover appare attenuata con la nota
    // "video attivo nel pannello player".
    engineLabel: String,
    onCollapse: () -> Unit,
    // STEP 3d: navigazione tra i brani ascoltati (cronologia di sessione)
    canGoPrevious: Boolean = false,
    canGoNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    // STEP 3i: CODA DI RIPRODUZIONE. Il pannello (icona in alto a destra)
    // elenca i brani in coda: click = riproduci subito, X = rimuovi,
    // cestino = svuota, MANIGLIA = trascina per riordinare (STEP 3j).
    // L'AVANZAMENTO AUTOMATICO alla fine del brano e' gestito dall'app
    // (ThreeColumnsApp via PlayerController.onEnded).
    // STEP 3j: la coda ora e' List<QueuedSong> (uid stabile per item: serve
    // alle chiavi del drag&drop anche con brani duplicati).
    queue: List<QueuedSong> = emptyList(),
    onQueuePlay: (Song) -> Unit = {},
    onQueueRemove: (Song) -> Unit = {},
    onQueueClear: () -> Unit = {},
    // STEP 3j: riordino (from.uid -> to.uid, gestito dall'app sulla lista)
    onQueueMove: (Long, Long) -> Unit = { _, _ -> },
    // STEP 3j: RADIO (richiesta utente n.4): aggiunge i BRANI SIMILI in coda
    // (Environment.nextPage, la stessa funzione della radio Android)
    onAddRadio: () -> Unit = {},
    radioLoading: Boolean = false,
    radioFeedback: String? = null
) {

    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    // STEP 3d: focus per il tasto ESC (onPreviewKeyEvent senza focus non
    // riceve mai eventi: per questo la chiusura non funzionava)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    // STEP 3i: pannello coda aperto/chiuso (toggle con l'icona in alto a destra)
    var queueOpen by remember { mutableStateOf(false) }

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
                // STEP 3h: sfumatura sottile invece del nero piatto
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
                // STEP 3d: le aree vuote CONSUMANO i tap: niente click che
                // passano alla pagina sottostante (prima un click accanto
                // alla cover faceva partire il brano di un item sotto)
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
                    // STEP 3j: pulsante RADIO: aggiunge i BRANI SIMILI del
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
                    // STEP 3i: toggle del pannello CODA DI RIPRODUZIONE
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
                }

                Spacer(Modifier.weight(1f))

                // Cover grande (Android mostra la copertina quando il brano
                // non e' un video). Con il video attivo la cover resta come
                // sfondo d'arte attenuata + nota sulla posizione del video.
                // STEP 3h: ingresso con scala a molla + angoli arrotondati
                // e ombra (profondita', stile player chic).
                // STEP 3n (richiesta utente n.7): copertina alla RISOLUZIONE
                // ALTA (la cover qui arriva a >50% della finestra: la
                // miniatura 120px era ancora piu' visibilmente sfocata)
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

                // TIMELINA (seek) + timestamp, come la PlayerScreen Android
                // STEP 3h: SLIDER PREMIUM con bolla del tempo sull'anteprima
                // e UN solo seekTo al RILASCIO (prima lo Slider material
                // chiamava seekTo a ogni pixel di drag: decine di comandi
                // JS al bridge KCEF e cursor che saltava).
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

                    // STEP 3h: play/pausa dentro un cerchio d'accento
                    // (pulsante principale, come nei player chic)
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
                }

                Spacer(Modifier.height(10.dp))

                // STEP 3k (richiesta utente n.3): l'interruttore video/cover
                // e' stato RIMOSSO dal player full: la copertina attenuata con
                // la nota qui sopra resta come stato informativo, mentre lo
                // switch vero si usa nella barra del player di sinistra
                // (per guardare il video si deve comunque tornare alla home).

                // Suggerimento di chiusura (dalla prova: la via d'uscita
                // non era evidente)
                Text(
                    ds("overlay.close_hint"),
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )

                Spacer(Modifier.weight(1f))
            }

            // STEP 3i: PANNELLO CODA DI RIPRODUZIONE, agganciato al bordo
            // destro del player a tutto schermo (si apre/chiude con l'icona
            // "Coda" in alto a destra). Click su un brano = riproduci ora,
            // X = rimuovi dalla coda, cestino = svuota tutto.
            // STEP 3k (richiesta utente n.5): una volta aperto NON si poteva
            // piu' chiudere: il pannello copriva l'intera altezza INCLUSA
            // la barra in alto a destra col pulsante "Coda". Ora (a) il
            // pannello inizia SOTTO la barra superiore (il pulsante Coda
            // resta cliccabile) e (b) ha un pulsante X proprio nell'intestazione.
            androidx.compose.animation.AnimatedVisibility(
                visible = queueOpen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // STEP 3m: il pannello e' il componente PUBBLICO condiviso con
                // la colonna coda della finestra principale (topPadding 70.dp:
                // inizia sotto la barra superiore, fix step3k n.5)
                PlayQueuePanel(
                    queue = queue,
                    onPlay = onQueuePlay,
                    onRemove = onQueueRemove,
                    onClear = onQueueClear,
                    onMove = onQueueMove,
                    onClose = { queueOpen = false },
                    topPadding = 70.dp
                )
            }
        }
    }
}

