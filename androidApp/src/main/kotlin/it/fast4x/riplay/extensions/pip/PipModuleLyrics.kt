package it.fast4x.riplay.extensions.pip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import it.fast4x.lrclib.LrcLib
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.lyricshelper.models.LRCLyricLine
import it.fast4x.riplay.extensions.lyricshelper.parsers.LRCLyricsKaraokeParser
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.ui.screens.player.common.fetchLyricsIfNeeded
import it.fast4x.riplay.ui.screens.player.common.launchLyricsFetch
import it.fast4x.riplay.utils.LatestValueProvider
import it.fast4x.riplay.utils.SynchronizedLyricsLines
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.rememberPrecisePositionAndDurationState
import it.fast4x.riplay.utils.toLyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Modulo PiP con il testo sincronizzato del brano: riga corrente evidenziata
 * (parola per parola quando disponibile) e riga successiva in trasparenza.
 * Se il testo non e' ancora in memoria viene scaricato in silenzio.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PipModuleLyrics(
    mediaId: String?,
    binder: PlayerService.Binder?,
    isShowingSynchronizedLyrics: Boolean,
    isShowingWordByWord: Boolean,
    modifier: Modifier = Modifier
) {
    val colorPalette = colorPalette()
    val positionAndDuration by rememberPrecisePositionAndDurationState(binder)
    val positionProvider = remember { LatestValueProvider(positionAndDuration.first) }
    positionProvider.value = positionAndDuration.first

    val currentLyrics by Database.lyrics(mediaId ?: "").collectAsState(initial = null)

    val context = LocalContext.current
    LaunchedEffect(mediaId, currentLyrics?.synced == null, currentLyrics?.lrcSynced == null) {
        val player = binder?.hybridPlayer ?: return@LaunchedEffect
        val metadata = player.currentMediaItem?.mediaMetadata ?: return@LaunchedEffect
        val id = mediaId ?: return@LaunchedEffect
        launchLyricsFetch("$id|$isShowingSynchronizedLyrics|$isShowingWordByWord") {
            fetchLyricsIfNeeded(
                mediaId = id,
                artistName = metadata.artist?.toString().orEmpty(),
                title = metadata.title?.toString().orEmpty(),
                currentLyrics = currentLyrics,
                isShowingSynchronizedWordByWordLyrics = isShowingWordByWord,
                isShowingSynchronizedLyrics = isShowingSynchronizedLyrics,
                mediaMetadata = metadata,
                durationProvider = { positionAndDuration.second },
                playerEnableLyricsPopupMessage = false,
                context = context,
                onCheckedLrc = {}, onCheckedKugou = {},
                onCheckedInnertube = {}, onCheckedSimpLrc = {},
                onError = {}
            )
        }
    }

    val lyricsText = when {
        isShowingSynchronizedLyrics && isShowingWordByWord -> currentLyrics?.lrcSynced.orEmpty()
        isShowingSynchronizedLyrics -> currentLyrics?.synced.orEmpty()
        else -> currentLyrics?.fixed.orEmpty()
    }

    if (!isShowingSynchronizedLyrics || lyricsText.isBlank()) {
        PipLyricsPlaceholder(colorPalette.background0, colorPalette.text)
        return
    }

    val sentences = remember(isShowingWordByWord, lyricsText, mediaId) {
        if (!isShowingWordByWord) LrcLib.Lyrics(lyricsText).sentences.toLyricLine()
        else LRCLyricsKaraokeParser.parse(
            lyricsText,
            isOnline = binder?.hybridPlayer?.currentMediaItem?.isLocal == true
        )
    }

    val synchronizedLyrics = remember(sentences, positionProvider) {
        SynchronizedLyricsLines(sentences, positionProvider)
    }

    LaunchedEffect(synchronizedLyrics) {
        while (isActive) {
            delay(33)
            synchronizedLyrics.update()
        }
    }

    val currentLine = synchronizedLyrics.sentences.getOrNull(synchronizedLyrics.index)
    val nextLine = synchronizedLyrics.sentences.getOrNull(synchronizedLyrics.index + 1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorPalette.background0)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            PipKaraokeLine(
                line = currentLine,
                isActive = true,
                positionMs = positionProvider(),
                baseColor = colorPalette.text,
                accent = colorPalette.accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            PipKaraokeLine(
                line = nextLine,
                isActive = false,
                positionMs = 0L,
                baseColor = colorPalette.text,
                accent = colorPalette.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PipLyricsPlaceholder(background: Color, text: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♪",
            color = text.copy(alpha = 0.4f),
            fontSize = 28.sp
        )
    }
}

@Composable
private fun PipKaraokeLine(
    line: LRCLyricLine?,
    isActive: Boolean,
    positionMs: Long,
    baseColor: Color,
    accent: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight
) {
    val text = line?.text.orEmpty().trim()
    if (text.isEmpty()) return

    val annotated = if (isActive && line?.words?.isNotEmpty() == true) {
        val progress = positionMs - line.timeMs
        buildAnnotatedString {
            line.words.forEach { word ->
                val sung = progress >= word.startTimeInTheLineMs + word.durationMs
                val singing = !sung && progress >= word.startTimeInTheLineMs
                withStyle(
                    SpanStyle(
                        color = when {
                            singing -> accent
                            sung -> baseColor
                            else -> baseColor.copy(alpha = 0.45f)
                        },
                        fontWeight = if (singing) FontWeight.Bold else null
                    )
                ) {
                    append(word.text)
                    append(" ")
                }
            }
        }
    } else AnnotatedString(text)

    Text(
        text = annotated,
        color = if (isActive) accent else baseColor.copy(alpha = 0.5f),
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        lineHeight = fontSize * 1.25f,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}
