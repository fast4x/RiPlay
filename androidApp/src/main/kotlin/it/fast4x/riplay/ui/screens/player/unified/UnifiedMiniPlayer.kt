package it.fast4x.riplay.ui.screens.player.unified

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalPlayerServiceState
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.setDisLikeState
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.cast.ritune.models.RiTuneRemoteCommand
import it.fast4x.riplay.enums.AudioVisualizerBaseStyle
import it.fast4x.riplay.enums.AudioVisualizerStyle
import it.fast4x.riplay.enums.PlayerTransitionAnimation
import it.fast4x.riplay.extensions.appviewmodel.rememberIsNetworkConnected
import it.fast4x.riplay.extensions.nextvisualizer.modern.PlayerEqualizerSpectrum
import it.fast4x.riplay.extensions.nextvisualizer.modern.PlayerEqualizerLevel
import it.fast4x.riplay.extensions.smoothloader.Loader
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.addToOnlineLikedSong
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.emptyMediaItem
import it.fast4x.riplay.utils.getLikeState
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.playerArtworkKey
import it.fast4x.riplay.utils.rememberPlayerPositionAndDurationState
import it.fast4x.riplay.utils.removeFromOnlineLikedSong
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.absoluteValue

@OptIn(ExperimentalPermissionsApi::class)
@UnstableApi
@ExperimentalSerializationApi
@Composable
fun UnifiedMiniPlayer(
    showPlayer: () -> Unit,
    hidePlayer: () -> Unit,
    navController: NavController? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    artworkKeySuffix: String? = null,
) {

//    LaunchedEffect(Unit) {
//        Timber.d("UnifiedMiniPlayer STS=${sharedTransitionScope != null} AVS=${animatedVisibilityScope != null}")
//    }
    //LaunchedEffect(Unit) { Timber.d("UnifiedMiniPlayer LaunchedEffect artworkKeySuffix=$artworkKeySuffix") }

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    val hapticFeedback = LocalHapticFeedback.current

    binder?.hybridPlayer ?: return
    if (binder.hybridPlayer.currentTimeline.windowCount == 0) return

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val playerState = LocalPlayerServiceState.current
    val shouldBePlaying = playerState.isPlaying

    val mediaItem = playerState.mediaInfo?.mediaItem ?: return // nullableMediaItem ?: return

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(mediaItem) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
    }

    val miniPlayerType = appearanceSettings.miniPlayerType

    val positionAndDurationState = rememberPlayerPositionAndDurationState(binder)

    var updateLike by rememberSaveable { mutableStateOf(false) }
    var updateDislike by rememberSaveable { mutableStateOf(false) }
    val isNetworkConnected = rememberIsNetworkConnected()

    LaunchedEffect(updateLike, updateDislike) {
        if (updateLike) {
            if (!isNetworkConnected && isYtSyncEnabled()) {
                SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
            } else if (!isYtSyncEnabled()){
                mediaItemToggleLike(mediaItem)
                if (likedAt == null || likedAt == -1L)
                    SmartMessage(context.resources.getString(R.string.added_to_favorites), context = context)
                else
                    SmartMessage(context.resources.getString(R.string.removed_from_favorites), context = context)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    addToOnlineLikedSong(mediaItem)
                }
            }
            updateLike = false
        }
        if (updateDislike) {
            if (!isNetworkConnected && isYtSyncEnabled()) {
                SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
            } else if (!isYtSyncEnabled()){
                Database.asyncTransaction {
                    if (like(mediaItem.mediaId, setDisLikeState(likedAt)) == 0)
                        insert(mediaItem, Song::toggleDislike)
                }
                if (likedAt == null || (likedAt ?: 0) > 0L)
                    SmartMessage(context.resources.getString(R.string.added_to_disliked), context = context)
                else
                    SmartMessage(context.resources.getString(R.string.removed_from_disliked), context = context)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    removeFromOnlineLikedSong(mediaItem)
                }
            }
            updateDislike = false
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { direction ->
            when ( direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (miniPlayerType == MiniPlayerType.Essential) {
                        updateLike = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        binder.hybridPlayer.seekToPrevious()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    binder.hybridPlayer.seekToNext()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                }
                else -> {}
            }
            return@rememberSwipeToDismissBoxState false
        }
    )

    val backgroundProgress = appearanceSettings.backgroundProgress
    val shouldBePlayingTransition = updateTransition(shouldBePlaying, label = "shouldBePlaying")
    val playPauseRoundness by shouldBePlayingTransition.animateDp(
        transitionSpec = { tween(durationMillis = 100, easing = LinearEasing) },
        label = "playPauseRoundness",
        targetValueByState = { if (it) 24.dp else 12.dp }
    )

    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )
    val disableClosingPlayerSwipingDown = appSettings.disableClosingPlayerSwipingDown
    val disableScrollingText = appearanceSettings.disableScrollingText

    val colorPalette = colorPalette()

    val artworkModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = playerArtworkKey(artworkKeySuffix)),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { initial, target ->
                    PlayerTransitionAnimation.playerBoundsAnimation(
                        appearanceSettings.playerTransitionAnimation,
                        initial,
                        target
                    )
                },
                //clipInOverlayDuringTransition = OverlayClip(thumbnailShape()),
            )
        }
    } else Modifier

    val hasNextMediaItem = binder.hybridPlayer.hasNextMediaItem()
    val nextMediaItemIndex = if (hasNextMediaItem) binder.hybridPlayer.nextMediaItemIndex else -1
    val upNextMediaItem = if (nextMediaItemIndex != -1) binder.hybridPlayer.getMediaItemAt(nextMediaItemIndex) else emptyMediaItem("End")

    val micPermission = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { isGranted ->
            if (!isGranted) return@rememberPermissionState
        }
    )

    SwipeToDismissBox(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        state = dismissState,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorPalette().background1)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                    SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                    SwipeToDismissBoxValue.Settled -> Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            if (miniPlayerType == MiniPlayerType.Modern) ImageVector.vectorResource(R.drawable.play_skip_back) else
                                if (likedAt == null)
                                    ImageVector.vectorResource(R.drawable.heart_outline)
                                else if(likedAt == -1L)
                                    ImageVector.vectorResource(R.drawable.heart_dislike)
                                else ImageVector.vectorResource(R.drawable.heart)
                        }
                        SwipeToDismissBoxValue.EndToStart ->  ImageVector.vectorResource(R.drawable.play_skip_forward)
                        SwipeToDismissBoxValue.Settled ->  ImageVector.vectorResource(R.drawable.play)
                    },
                    contentDescription = null,
                    tint = colorPalette().iconButtonPlayer,
                )
            }
        }
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .combinedClickable(
                        onLongClick = {
                            navController?.navigate(NavRoutes.queue.name);
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onClick = {
                            if (sharedTransitionScope?.isTransitionActive != true) showPlayer()
                        }
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount < 0) showPlayer()
                                else if (dragAmount > 20) {
                                    if (!disableClosingPlayerSwipingDown) {
                                        if (!GlobalSharedData.riTuneCastActive)
                                            binder.hybridPlayer.pause()
                                        else
                                            CoroutineScope(Dispatchers.IO).launch {
                                                binder.riTuneCastClient.sendCommand(
                                                    RiTuneRemoteCommand(
                                                        "play"
                                                    )
                                                )
                                            }
                                        binder.hybridPlayer.clearMediaItems()
                                        hidePlayer()
                                        runCatching {
                                            context.stopService(context.intent<PlayerService>())
                                        }
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else
                                        SmartMessage(
                                            context.resources.getString(R.string.player_swiping_down_is_disabled),
                                            context = context
                                        )
                                }
                            }
                        )
                    }
                    .background(colorPalette().background2)
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .width(0.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(56.dp)
                ) {
                    AsyncImage(
                        model = mediaItem.mediaMetadata.artworkUri.toString()
                            .toThumbnail(Dimensions.thumbnails.song.px),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .then(artworkModifier)
                            .clip(RoundedCornerShape(4.dp))
                            .size(40.dp)
                    )
                    NowPlayingSongIndicator(mediaItem.mediaId, binder.hybridPlayer)
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mediaItem.isExplicit)
                            IconButton(
                                icon = R.drawable.explicit,
                                color = colorPalette().text,
                                enabled = true,
                                onClick = {},
                                modifier = Modifier
                                    .size(14.dp)
                            )
                        BasicText(
                            text = cleanPrefix(mediaItem.mediaMetadata.title?.toString() ?: ""),
                            style = typography().xs.semiBold.copy(color = colorPalette().text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                        )
                    }

                    BasicText(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        style = typography().xs.copy(color = colorPalette().textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(56.dp)
                ) {
                    // Spotify: [Me gusta] [Play/Pause]
                    IconButton(
                        icon = getLikeState(mediaItem.mediaId),
                        color = if (likedAt != null && likedAt != -1L) colorPalette().accent else colorPalette().text,
                        onClick = { updateLike = true },
                        onLongClick = { updateDislike = true },
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .size(24.dp)
                    )
                    if (false)
                        IconButton(
                            icon = R.drawable.play_skip_back,
                            color = colorPalette().iconButtonPlayer,
                            onClick = {
                                binder.hybridPlayer.playPrevious()
                            },
                            modifier = Modifier
                                .rotate(rotationAngle)
                                .padding(horizontal = 2.dp, vertical = 8.dp)
                                .size(24.dp)
                        )

                    if (!playerState.loadPending.isPending) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(playPauseRoundness))
                                .clickable {
                                    if (shouldBePlaying) {
                                        if (!GlobalSharedData.riTuneCastActive)
                                            binder.hybridPlayer.pause()
                                        else
                                            CoroutineScope(Dispatchers.IO).launch {
                                                binder.riTuneCastClient.sendCommand(
                                                    RiTuneRemoteCommand(
                                                        "pause"
                                                    )
                                                )
                                            }

                                    } else {
                                        if (!GlobalSharedData.riTuneCastActive)
                                            binder.hybridPlayer.play()
                                        else
                                            CoroutineScope(Dispatchers.IO).launch {
                                                binder.riTuneCastClient.sendCommand(
                                                    RiTuneRemoteCommand(
                                                        "play", mediaId = mediaItem.mediaId
                                                    )
                                                )
                                            }

                                    }
                                }
                                .size(42.dp)
                        ) {
                            Image(
                                painter = painterResource(if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette().text),
                                modifier = Modifier
                                    .rotate(rotationAngle)
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                    } else AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300, delayMillis = 350)),
                        exit = fadeOut(tween(150))
                    ) {
                        Loader(size = 42.dp, expandWidth = false)
                    }

                    if (false)
                        IconButton(
                            icon = R.drawable.play_skip_forward,
                            color = colorPalette().iconButtonPlayer,
                            onClick = {
                                binder.hybridPlayer.playNext()
                            },
                            modifier = Modifier
                                .rotate(rotationAngle)
                                .padding(horizontal = 2.dp, vertical = 8.dp)
                                .size(24.dp)
                        )
                    if (false)
                        IconButton(
                            icon = getLikeState(mediaItem.mediaId),
                            color = colorPalette().favoritesIcon,
                            onClick = {
                                updateLike = true
                            },
                            onLongClick = {
                                updateDislike = true
                            },
                            modifier = Modifier
                                .rotate(rotationAngle)
                                .padding(horizontal = 2.dp, vertical = 8.dp)
                                .size(24.dp)
                        )

                }

                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )

            }


            // Línea de progreso de 2dp en la base (Spotify)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colorPalette().background2)
                    .drawBehind {
                        drawRect(color = Color.White.copy(alpha = 0.25f), size = size)
                        val total = positionAndDurationState.value.second.absoluteValue
                        if (total > 0) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset.Zero,
                                size = Size(
                                    width = (positionAndDurationState.value.first.toFloat() / total * size.width)
                                        .coerceIn(0f, size.width),
                                    height = size.height
                                )
                            )
                        }
                    }
            )

            if (appSettings.showUpNextSongInMiniPlayer)
                Box(
                    modifier = Modifier
                        .background(colorPalette().background1)
                        .fillMaxWidth()
                ) {
                    BasicText(
                        text = "${stringResource(R.string.queue_up_next).uppercase()} > ${upNextMediaItem.mediaMetadata.title}",
                        style = typography().xxs.semiBold.copy(
                            color = colorPalette().accent,
                            letterSpacing = 1.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 2.dp)
                    )
                }

            if (micPermission.status.isGranted) {
                when (appearanceSettings.audioVisualizerStyleInMiniPlayer) {
                    AudioVisualizerBaseStyle.SPECTRUM -> PlayerEqualizerSpectrum(
                        modifier = Modifier
                            .height(20.dp)
                            .fillMaxWidth()
                    )

                    AudioVisualizerBaseStyle.LEVEL -> PlayerEqualizerLevel()
                    else -> {}
                }
            }



        }

    }

}