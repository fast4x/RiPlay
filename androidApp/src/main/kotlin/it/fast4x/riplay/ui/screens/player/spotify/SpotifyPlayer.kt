package it.fast4x.riplay.ui.screens.player.spotify

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalPlayerServiceState
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.PlayerMenu
import it.fast4x.riplay.ui.screens.player.common.Lyrics
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.addToOnlineLikedSong
import it.fast4x.riplay.utils.getBitmapFromUrl
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.rememberPlayerPositionAndDurationState
import it.fast4x.riplay.utils.removeFromOnlineLikedSong
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.absoluteValue

/**
 * Reproductor a pantalla completa, réplica del de Spotify (Android):
 *  ┌ ˅   REPRODUCIENDO DESDE / álbum   ⋮ ┐
 *  │           carátula grande          │
 *  │ Título (marquesina)            ♡   │
 *  │ Artista                            │
 *  │ ───────●──────────────────────     │
 *  │ 0:41                         3:46  │
 *  │  ⤮     ⏮     ( ▶ )     ⏭     ⟳     │
 *  │ compartir                    cola  │
 *  └ [ Tarjeta de letra ]               ┘
 */
@OptIn(
    UnstableApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalSerializationApi::class,
    androidx.compose.ui.text.ExperimentalTextApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)
@Composable
fun SpotifyPlayer(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return
    val playerState = LocalPlayerServiceState.current
    val mediaItem = playerState.mediaInfo?.mediaItem ?: return
    val menuState = LocalGlobalSheetState.current
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()

    val positionAndDuration by rememberPlayerPositionAndDurationState(binder)
    val isPlaying = playerState.isPlaying

    BackHandler(onBack = onDismiss)

    // ---------- Me gusta ----------
    var likedAt by remember(mediaItem.mediaId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
    }
    val isLiked = (likedAt ?: 0L) > 0L

    // ---------- Color dominante de la carátula ----------
    val artworkUrl = mediaItem.mediaMetadata.artworkUri.toString().toThumbnail(1200).toString()
    var dominant by remember { mutableStateOf(Encore.SurfaceElevated) }
    LaunchedEffect(mediaItem.mediaId) {
        runCatching {
            val bitmap = withContext(Dispatchers.IO) { getBitmapFromUrl(context, artworkUrl) }
            dynamicColorPaletteOf(bitmap, true)?.background2?.let { dominant = it }
        }
    }
    val topColor by animateColorAsState(dominant, tween(600), label = "topColor")

    // ---------- Shuffle / Repeat ----------
    var shuffle by remember { mutableStateOf(binder.hybridPlayer.shuffleModeEnabled) }
    val loop = appSettings.queueLoopType

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to topColor,
                    0.55f to lerp(topColor, Encore.Background, 0.75f),
                    1f to Encore.Background
                )
            )
            .pointerInput(Unit) {
                // Deslizar hacia abajo cierra (como Spotify)
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 300f) onDismiss() },
                    onVerticalDrag = { _, dy -> total += dy }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // ---------- Barra superior ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                PlayerIcon(R.drawable.chevron_down, 28) { onDismiss() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = "PLAYING FROM",
                        style = typography().xxxs.semiBold.copy(color = Color.White, letterSpacing = 1.sp)
                    )
                    BasicText(
                        text = mediaItem.mediaMetadata.albumTitle?.toString()
                            ?: mediaItem.mediaMetadata.artist?.toString() ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = typography().xxs.bold.copy(color = Color.White)
                    )
                }
                PlayerIcon(R.drawable.ellipsis_vertical, 24) {
                    menuState.display {
                        PlayerMenu(
                            navController = navController,
                            onDismiss = menuState::hide,
                            mediaItem = mediaItem,
                            binder = binder,
                            onClosePlayer = onDismiss,
                            onInfo = {
                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${mediaItem.mediaId}")
                            },
                            disableScrollingText = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ---------- Carátula (deslizar = anterior/siguiente) ----------
            var dragX by remember { mutableFloatStateOf(0f) }
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .offset { IntOffset((dragX / 3f).roundToInt(), 0) }
                    .shadow(16.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Encore.SurfaceElevated)
                    .pointerInput(mediaItem.mediaId) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    dragX < -150f -> binder.hybridPlayer.seekToNext()
                                    dragX > 150f -> binder.hybridPlayer.seekToPrevious()
                                }
                                dragX = 0f
                            },
                            onDragCancel = { dragX = 0f },
                            onHorizontalDrag = { _, dx -> dragX += dx }
                        )
                    }
            )

            Spacer(Modifier.height(36.dp))

            // ---------- Título / artista / me gusta ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = cleanPrefix(mediaItem.mediaMetadata.title?.toString() ?: ""),
                        maxLines = 1,
                        style = typography().xl.bold.copy(color = Color.White),
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = typography().s.copy(color = Encore.TextSecondary)
                    )
                }
                Spacer(Modifier.width(12.dp))
                PlayerIcon(
                    if (isLiked) R.drawable.heart else R.drawable.heart_outline,
                    26,
                    tint = if (isLiked) Encore.Green else Color.White
                ) {
                    if (!isYtSyncEnabled()) mediaItemToggleLike(mediaItem)
                    else scope.launch(Dispatchers.IO) {
                        if (isLiked) removeFromOnlineLikedSong(mediaItem) else addToOnlineLikedSong(mediaItem)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Barra de progreso ----------
            val duration = positionAndDuration.second.absoluteValue.coerceAtLeast(1L)
            var seekFraction by remember { mutableStateOf<Float?>(null) }
            val fraction = seekFraction ?: (positionAndDuration.first.toFloat() / duration).coerceIn(0f, 1f)

            BoxWithConstraints(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(duration) {
                        detectTapGestures { o ->
                            binder.hybridPlayer.seekTo(((o.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                        }
                    }
                    .pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = { o -> seekFraction = (o.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = {
                                seekFraction?.let { binder.hybridPlayer.seekTo((it * duration).toLong()) }
                                seekFraction = null
                            },
                            onDragCancel = { seekFraction = null },
                            onHorizontalDrag = { change, _ ->
                                seekFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        )
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Box(
                    Modifier
                        .offset(x = maxWidth * fraction - 6.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Row(Modifier.fillMaxWidth()) {
                BasicText(
                    text = formatMs(seekFraction?.let { (it * duration).toLong() } ?: positionAndDuration.first),
                    style = typography().xxs.copy(color = Encore.TextSecondary)
                )
                Spacer(Modifier.weight(1f))
                BasicText(
                    text = formatMs(duration),
                    style = typography().xxs.copy(color = Encore.TextSecondary)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---------- Controles ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ToggleIcon(R.drawable.shuffle, active = shuffle) {
                    shuffle = !shuffle
                    binder.hybridPlayer.shuffleModeEnabled = shuffle
                }
                PlayerIcon(R.drawable.play_skip_back, 36) { binder.hybridPlayer.seekToPrevious() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (isPlaying) binder.hybridPlayer.pause() else binder.hybridPlayer.play()
                        }
                ) {
                    Image(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Black),
                        modifier = Modifier.size(28.dp)
                    )
                }
                PlayerIcon(R.drawable.play_skip_forward, 36) { binder.hybridPlayer.seekToNext() }
                ToggleIcon(
                    if (loop == QueueLoopType.RepeatOne) R.drawable.repeatone else R.drawable.repeat,
                    active = loop != QueueLoopType.Default
                ) {
                    // Spotify: apagado → repetir todo → repetir una
                    val next = when (loop) {
                        QueueLoopType.Default -> QueueLoopType.RepeatAll
                        QueueLoopType.RepeatAll -> QueueLoopType.RepeatOne
                        QueueLoopType.RepeatOne -> QueueLoopType.Default
                    }
                    scope.launch {
                        appSettingsManager.updateSettings(
                            appSettingsManager.activeSettings.value.copy(queueLoopType = next)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Compartir / Cola ----------
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PlayerIcon(R.drawable.share_social, 22, tint = Encore.TextSecondary) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaItem.mediaId}")
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
                Spacer(Modifier.weight(1f))
                PlayerIcon(R.drawable.playlist, 22, tint = Encore.TextSecondary) {
                    onDismiss()
                    navController.navigate(NavRoutes.queue.name)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---------- Tarjeta de letra ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(lerp(topColor, Color.Black, 0.25f))
                    .padding(16.dp)
            ) {
                BasicText(
                    text = "Lyrics",
                    style = typography().m.bold.copy(color = Color.White)
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(340.dp)) {
                    Lyrics(
                        mediaId = mediaItem.mediaId,
                        isDisplayed = true,
                        onDismiss = {},
                        size = 340.dp,
                        mediaMetadataProvider = mediaItem::mediaMetadata,
                        durationProvider = { positionAndDuration.second },
                        ensureSongInserted = {
                            scope.launch(Dispatchers.IO) { Database.insert(mediaItem) }
                        },
                        clickLyricsText = true,
                        isLandscape = false
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlayerIcon(iconId: Int, sizeDp: Int, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size((sizeDp + 16).dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}

/** Icono verde con puntito debajo cuando está activo (shuffle / repeat). */
@Composable
private fun ToggleIcon(iconId: Int, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (active) Encore.Green else Color.White),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) Encore.Green else Color.Transparent)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

