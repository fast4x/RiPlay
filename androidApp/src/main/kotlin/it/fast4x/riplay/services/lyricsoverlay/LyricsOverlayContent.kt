package it.fast4x.riplay.services.lyricsoverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import it.fast4x.lrclib.LrcLib
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.lyricshelper.models.LRCLyricLine
import it.fast4x.riplay.extensions.lyricshelper.models.LRCLyricWord
import it.fast4x.riplay.extensions.lyricshelper.parsers.LRCLyricsKaraokeParser
import it.fast4x.riplay.ui.screens.player.common.fetchLyricsIfNeeded
import it.fast4x.riplay.ui.screens.player.common.launchLyricsFetch
import it.fast4x.riplay.utils.LatestValueProvider
import it.fast4x.riplay.utils.SynchronizedLyricsLines
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.toLyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Identita' visiva dell'overlay: la stessa del karaoke desktop
// (sfondo quasi nero e verde RiPlay)
private val OverlayBackgroundColor = Color(0xFF0B0B10)
private val OverlayAccentColor = Color(0xFF30D158)

/**
 * Contenuto dell'overlay testi fluttuante: riga corrente (parola per parola
 * con tempi reali quando disponibili, altrimenti ripartizione euristica)
 * + riga successiva in trasparenza; senza testo sincronizzato mostra
 * titolo e artista. Dati e recupero testo sono gli stessi del player e
 * del modulo PiP (niente catene di download parallele).
 */
@OptIn(UnstableApi::class)
@Composable
fun LyricsOverlayContent(
    song: Song,
    player: Player?
) {
    val scale = LyricsOverlayManager.textScale
    val wordMode = LyricsOverlayManager.mode == LyricsOverlayMode.WORD

    // La finestra si allarga con la dimensione del testo, senza mai superare
    // la larghezza reale dello schermo (la configurazione del contesto in PiP
    // rifletterebbe la finestra ridotta dell'app)
    val windowWidth = ((294f * scale).toInt())
        .coerceIn(240, LyricsOverlayManager.realScreenWidthDp().toInt().coerceAtLeast(240))
        .dp

    // Posizione/durata campionate a ~30Hz (cadenza del karaoke del player)
    // + stato di riproduzione per attenuare il testo in pausa
    var isPlaying by remember { mutableStateOf(false) }
    val positionAndDuration = produceState(0L to 0L, player) {
        while (isActive) {
            val p = player
            value = if (p == null) 0L to 0L
            else runCatching { p.currentPosition to p.duration }.getOrDefault(0L to 0L)
            isPlaying = if (p == null) false
            else runCatching { p.isPlaying }.getOrDefault(false)
            delay(33)
        }
    }
    val positionProvider = remember { LatestValueProvider(positionAndDuration.value.first) }
    positionProvider.value = positionAndDuration.value.first

    val currentLyrics by Database.lyrics(song.id).collectAsState(initial = null)

    // Recupero del testo in scope applicativo (deduplicato): prosegue anche
    // se l'overlay viene chiuso durante il download
    val context = LocalContext.current
    LaunchedEffect(song.id, wordMode, currentLyrics?.synced == null, currentLyrics?.lrcSynced == null) {
        val metadata = runCatching { player?.currentMediaItem?.mediaMetadata }.getOrNull()
            ?: return@LaunchedEffect
        launchLyricsFetch("${song.id}|overlay|$wordMode") {
            fetchLyricsIfNeeded(
                mediaId = song.id,
                artistName = metadata.artist?.toString().orEmpty(),
                title = metadata.title?.toString().orEmpty(),
                currentLyrics = currentLyrics,
                isShowingSynchronizedWordByWordLyrics = wordMode,
                isShowingSynchronizedLyrics = true,
                mediaMetadata = metadata,
                durationProvider = { positionAndDuration.value.second },
                playerEnableLyricsPopupMessage = false,
                context = context,
                onCheckedLrc = {}, onCheckedKugou = {},
                onCheckedInnertube = {}, onCheckedSimpLrc = {},
                onError = {}
            )
        }
    }

    val syncedText = currentLyrics?.synced.orEmpty()
    val richText = currentLyrics?.lrcSynced.orEmpty()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(OverlayBackgroundColor)
            .overlayDrag()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .width(windowWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selettore di stile (Riga/Parola) e dimensione del testo su una
            // riga propria: sta sopra il testo, mai sovrapposto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    OverlayChip(
                        label = stringResource(R.string.floating_lyrics_mode_line),
                        active = !wordMode
                    ) { LyricsOverlayManager.setModeOverlayManager(LyricsOverlayMode.LINE) }
                    Text(
                        text = "\u00B7",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 10.sp
                    )
                    OverlayChip(
                        label = stringResource(R.string.floating_lyrics_mode_word),
                        active = wordMode
                    ) { LyricsOverlayManager.setModeOverlayManager(LyricsOverlayMode.WORD) }
                    Spacer(Modifier.width(8.dp))
                    OverlayChip("A\u2212") { LyricsOverlayManager.changeTextScale(-0.1f) }
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    OverlayChip("A+") { LyricsOverlayManager.changeTextScale(+0.1f) }
                }
            }

            Spacer(Modifier.height(9.dp))

            if (syncedText.isBlank()) {
                OverlayNowPlaying(song, scale)
            } else {
                val sentences = remember(wordMode, richText, syncedText, song.id) {
                    runCatching {
                        when {
                            // Parola con rich sync: parser karaoke (stessa
                            // strada di player e modulo PiP)
                            wordMode && richText.isNotBlank() ->
                                LRCLyricsKaraokeParser.parse(
                                    richText,
                                    isOnline = player?.currentMediaItem?.isLocal == true
                                )
                            // Riga e parola senza rich sync: righe normali,
                            // i tempi delle parole sono stimati alla
                            // visualizzazione
                            syncedText.isNotBlank() -> LrcLib.Lyrics(syncedText)
                                .sentences.toLyricLine()
                            else -> emptyList()
                        }
                    }.getOrDefault(emptyList())
                }

                if (sentences.isEmpty()) {
                    OverlayNowPlaying(song, scale)
                } else {
                    val synchronizedLyrics =
                        remember(sentences, positionProvider) {
                            SynchronizedLyricsLines(sentences, positionProvider)
                        }

                    LaunchedEffect(synchronizedLyrics) {
                        while (isActive) {
                            delay(33)
                            synchronizedLyrics.update()
                        }
                    }

                    val currentLine = synchronizedLyrics.sentences
                        .getOrNull(synchronizedLyrics.index)
                    val nextLine = synchronizedLyrics.sentences
                        .getOrNull(synchronizedLyrics.index + 1)

                    OverlayKaraokeLine(
                        line = currentLine,
                        lineEndMs = nextLine?.timeMs ?: (currentLine?.timeMs ?: 0L) + 6_000L,
                        wordMode = wordMode,
                        positionMs = positionProvider(),
                        playing = isPlaying,
                        scale = scale
                    )
                    OverlayNextLine(
                        line = nextLine,
                        playing = isPlaying,
                        scale = scale
                    )
                }
            }
        }
    }
}

/**
 * Tempi delle parole stimati quando manca il rich sync: ripartizione
 * proporzionale alla lunghezza lungo la durata della riga (la stessa
 * euristica del karaoke desktop). Tempi relativi all'inizio riga.
 */
private fun estimatedWords(text: String, durationMs: Long): List<LRCLyricWord> {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    if (words.size == 1) return listOf(LRCLyricWord(words[0], 0L, durationMs))
    val duration = durationMs.coerceAtLeast(600L).toFloat()
    val weights = words.map { it.length + 1 }
    val total = weights.sum().toFloat()
    var acc = 0f
    return words.mapIndexed { i, w ->
        val start = (acc / total * duration).toLong()
        acc += weights[i]
        val end = (acc / total * duration).toLong()
        LRCLyricWord(w, start, end - start)
    }
}

/**
 * Riga corrente: in modalita' parola le parole gia' cantate sono in accento,
 * quella attiva ha lo sfondo accent, le future restano attenuate; in
 * modalita' riga tutta la riga e' in accento (idem interludi e righe con
 * una sola parola).
 */
@Composable
private fun OverlayKaraokeLine(
    line: LRCLyricLine?,
    lineEndMs: Long,
    wordMode: Boolean,
    positionMs: Long,
    playing: Boolean,
    scale: Float
) {
    val l = line
    // Con il parser karaoke il testo sta nelle parole: ricostruito qui per
    // il fallback a riga e per la stima dei tempi
    val text = l?.text.orEmpty()
        .ifBlank { l?.words?.joinToString(" ") { it.text }.orEmpty() }
        .trim()

    // Altezza riservata a due righe di testo: la finestra non collassa su
    // righe corte e non salta a ogni cambio di riga
    val minHeight = with(LocalDensity.current) { (52f * scale).sp.toDp() }

    val annotated = if (l != null && text.isNotEmpty() && wordMode) {
        // tempi reali quando ci sono, altrimenti stima proporzionale
        val words = l.words.ifEmpty {
            estimatedWords(text, (lineEndMs - l.timeMs).coerceAtLeast(1_000L))
        }
        if (words.size > 1) {
            val progress = positionMs - l.timeMs
            buildAnnotatedString {
                words.forEachIndexed { i, w ->
                    if (i > 0) append(" ")
                    val sung = progress >= w.startTimeInTheLineMs + w.durationMs
                    val singing = !sung && progress >= w.startTimeInTheLineMs
                    withStyle(
                        SpanStyle(
                            color = when {
                                singing -> OverlayAccentColor
                                sung -> OverlayAccentColor
                                else -> Color.White.copy(alpha = if (playing) 0.55f else 0.40f)
                            },
                            background = if (singing) OverlayAccentColor.copy(alpha = 0.22f)
                            else Color.Unspecified
                        )
                    ) {
                        append(w.text)
                    }
                }
            }
        } else null
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center
    ) {
        if (l == null || text.isEmpty()) {
            // interludio o nessuna riga attiva
            Text(
                text = "\u266A",
                color = OverlayAccentColor.copy(alpha = if (playing) 0.9f else 0.5f),
                fontSize = (17f * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (annotated != null) {
            Text(
                text = annotated,
                fontSize = (19f * scale).sp,
                lineHeight = (26f * scale).sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = text,
                color = OverlayAccentColor.copy(alpha = if (playing) 1f else 0.7f),
                fontSize = (19f * scale).sp,
                lineHeight = (26f * scale).sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OverlayNextLine(
    line: LRCLyricLine?,
    playing: Boolean,
    scale: Float
) {
    // Anche le righe del parser karaoke hanno il testo solo nelle parole
    val text = line?.text.orEmpty()
        .ifBlank { line?.words?.joinToString(" ") { it.text }.orEmpty() }
        .trim()
    // Una riga sempre riservata: la finestra non cambia altezza quando la
    // prossima riga non c'e' (fine del testo)
    val minHeight = with(LocalDensity.current) { (17f * scale).sp.toDp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = Color.White.copy(alpha = if (playing) 0.45f else 0.30f),
                fontSize = (13f * scale).sp,
                lineHeight = (17f * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OverlayNowPlaying(song: Song, scale: Float) {
    // Stessa area riservata del testo sincronizzato: la finestra non cambia
    // altezza mentre il testo viene recuperato
    val minHeight = with(LocalDensity.current) { (74f * scale).sp.toDp() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = song.title,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        val artists = song.artistsText
        if (!artists.isNullOrBlank()) {
            Text(
                text = artists,
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp)
            )
        }
    }
}

/** Voce dei selettori; attiva = evidenziata dall'accento. */
@Composable
private fun OverlayChip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (active) OverlayAccentColor else Color.White,
        fontSize = 10.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Trascinamento della finestra da qualunque zona (come il karaoke desktop):
 * i gesti consumati dai figli (pulsanti) non spostano la finestra.
 */
private fun Modifier.overlayDrag(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            var moved = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                if (change.isConsumed) {
                    if (!moved) continue else break
                }
                if (!moved) {
                    val delta = change.position - down.position
                    if (delta.getDistance() > slop) moved = true
                }
                if (moved) {
                    val delta = change.positionChange()
                    LyricsOverlayManager.moveBy(delta.x, delta.y)
                    change.consume()
                }
            }
            if (moved) LyricsOverlayManager.savePosition()
        }
    }
}
