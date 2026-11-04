package it.fast4x.riplay.ui.components

import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.player.lyrics.LyricLine
import it.fast4x.riplay.player.lyrics.LyricWord
import it.fast4x.riplay.player.lyrics.LyricsUiState
import it.fast4x.riplay.utils.DesktopKaraokeOverlay
import it.fast4x.riplay.utils.KaraokeMode
import it.fast4x.riplay.utils.OverlayChip
import it.fast4x.riplay.utils.karaokeWords

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun LyricsPanel(
    state: LyricsUiState,
    timestampMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
    topPadding: Dp = 0.dp
) {
    val listState = rememberLazyListState()

    Column(
        Modifier
            .fillMaxHeight()
            .width(380.dp)
            .padding(top = topPadding)
            .background(Color(0xF2101110))
            .padding(14.dp)
    ) {
        // intestazione (stessa identita' del pannello coda)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lyrics,
                contentDescription = null,
                tint = RiPlayAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                ds("lyrics.title"),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            // Selettore Riga/Parola: la stessa preferenza dell'overlay
            // karaoke (cambiarlo qui lo cambia anche li). Con testo
            // sincronizzato, l'unica modalita' che lo usa.
            if (state is LyricsUiState.Synced) {
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
                Spacer(Modifier.width(6.dp))
            }
            // ricarica: utile quando il match LRCLIB era sbagliato
            if (state is LyricsUiState.Synced || state is LyricsUiState.Fixed || state is LyricsUiState.NotFound) {
                IconButton(
                    onClick = onReload,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = ds("lyrics.reload"),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = ds("lyrics.close_panel"),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when (state) {
            LyricsUiState.Idle -> {
                Text(
                    ds("lyrics.idle"),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }

            LyricsUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = RiPlayAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ds("lyrics.loading"),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                }
            }

            LyricsUiState.NotFound -> {
                Text(
                    ds("lyrics.not_found"),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            is LyricsUiState.Fixed -> {
                // testo senza timecode: un semplice blocco scorrevole
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    val paragraphs = state.text.lines()
                    items(paragraphs.size) { index ->
                        Text(
                            paragraphs[index].ifBlank { " " },
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }

            is LyricsUiState.Synced -> {
                SyncedLyricsList(
                    lines = state.lines,
                    timestampMs = timestampMs,
                    isPlaying = isPlaying,
                    wordMode = DesktopKaraokeOverlay.mode == KaraokeMode.WORD,
                    onSeek = onSeek,
                    listState = listState
                )
            }
        }
    }
}

/**
 * Lista karaoke: evidenzia la riga corrente (ultima riga con timeMs <= now)
 * e fa l'AUTO-SCROLL a ogni cambio riga.
 */
@Composable
private fun SyncedLyricsList(
    lines: List<LyricLine>,
    timestampMs: Long,
    isPlaying: Boolean,
    wordMode: Boolean,
    onSeek: (Long) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // indice della riga corrente: l'ULTIMA con timeMs <= timestamp.
    // Calcolo DIRETTO in composizione (non derivedStateOf: il timestamp
    // arriva come parametro, non come State: il derived non lo osserverebbe
    // e l'indice resterebbe congelato). Il costo e' un ciclo O(n) di poche
    // centinaia di righe a ogni tick (250ms): trascurabile, l'overlay si
    // ricompone comunque a ogni tick per la timeline.
    var currentIndex = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= timestampMs) currentIndex = i else break
    }

    // AUTO-SCROLL: porta la riga corrente un po' sotto il centro del pannello
    // (l'utente legge sempre la riga attiva nella parte alta della vista).
    // La posizione -2 da' un margine: la riga attiva non sta mai al bordo.
    LaunchedEffect(currentIndex, lines) {
        if (currentIndex >= 0) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            runCatching { listState.animateScrollToItem(target) }
        }
    }

    LazyColumn(state = listState) {
        items(lines.size) { index ->
            val line = lines[index]
            val isCurrent = index == currentIndex
            val isPast = index < currentIndex
            val isNext = index == currentIndex + 1

            val color by animateColorAsState(
                targetValue = when {
                    isCurrent -> RiPlayAccent
                    isPast -> Color.White.copy(alpha = 0.28f)
                    isNext && isPlaying -> Color.White.copy(alpha = 0.75f)
                    else -> Color.White.copy(alpha = 0.5f)
                },
                animationSpec = tween(320)
            )

            // in modalita' parola la riga corrente mostra tempi reali per
            // parola (rich sync) se presenti, altrimenti la ripartizione
            // euristica lungo la durata della riga (come l'overlay karaoke)
            val lineWords = if (isCurrent && wordMode) {
                line.words.takeIf { it.isNotEmpty() }
                    ?: karaokeWords(
                        line.text,
                        line.timeMs,
                        lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 6_000L)
                    )
            } else emptyList()

            val lineModifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                // click sulla riga = SEEK a quell'istante
                .clickable(enabled = line.timeMs > 0) { onSeek(line.timeMs) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .alpha(if (isCurrent) 1f else 0.95f)

            if (isCurrent && lineWords.size > 1) {
                WordByWordText(
                    words = lineWords,
                    timestampMs = timestampMs,
                    playing = isPlaying,
                    modifier = lineModifier
                )
            } else {
                Text(
                    text = line.text.ifBlank { "\u266A" }, // riga vuota = interludio
                    color = color,
                    fontSize = if (isCurrent) 19.sp else 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = if (isCurrent) 26.sp else 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = lineModifier
                )
            }
        }
        // coda: respiro sotto l'ultima riga
        item { Spacer(Modifier.height(48.dp)) }
    }
}

/**
 * Riga corrente in evidenza parola per parola: le parole gia' cantate sono
 * in accento, quella attiva ha lo sfondo accent, le future restano attenuate.
 * Stessa resa dell'overlay karaoke, adattata alla lista del pannello.
 */
@Composable
private fun WordByWordText(
    words: List<LyricWord>,
    timestampMs: Long,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
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
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
