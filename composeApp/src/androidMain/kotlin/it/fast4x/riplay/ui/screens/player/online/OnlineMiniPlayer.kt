package it.fast4x.riplay.ui.screens.player.online

import android.app.PendingIntent
import android.content.Intent
import android.database.SQLException
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.getMinTimeForEvent
import it.fast4x.riplay.getPauseListenHistory
import it.fast4x.riplay.getQueueLoopType
import it.fast4x.riplay.models.Event
import it.fast4x.riplay.models.Info
import it.fast4x.riplay.service.OfflinePlayerService
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.service.BitmapProvider
import it.fast4x.riplay.thumbnailShape
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomBasePlayerUiControllerAsListener
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.ActionIntent
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.addToYtLikedSong
import it.fast4x.riplay.utils.backgroundProgressKey
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.disableClosingPlayerSwipingDownKey
import it.fast4x.riplay.utils.disableScrollingTextKey
import it.fast4x.riplay.utils.effectRotationKey
import it.fast4x.riplay.utils.getLikeState
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isInvincibilityEnabledKey
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.lastVideoIdKey
import it.fast4x.riplay.utils.lastVideoSecondsKey
import org.dailyislam.android.utilities.isNetworkConnected
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.miniPlayerTypeKey
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.semiBold
import it.fast4x.riplay.utils.setDisLikeState
import it.fast4x.riplay.utils.thumbnail
import it.fast4x.riplay.utils.unlikeYtVideoOrSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class)
@Composable
fun OnlineMiniPlayer(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    showPlayer: () -> Unit,
    hidePlayer: () -> Unit,
    navController: NavController? = null,
) {

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    var shouldBePlaying by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    binder?.player ?: return
    if (binder.player.currentTimeline?.windowCount == 0) return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

//            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
//                mediaItems = timeline.mediaItems
//                mediaItemIndex = binder.player.currentMediaItemIndex
//            }

        }
    }

    val mediaItem = nullableMediaItem ?: return

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var miniPlayerType by rememberPreference(
        miniPlayerTypeKey,
        MiniPlayerType.Modern
    )


    var updateLike by rememberSaveable { mutableStateOf(false) }
    var updateDislike by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(updateLike, updateDislike) {
        if (updateLike) {
            if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
                SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
            } else if (!isYouTubeSyncEnabled()){
                mediaItemToggleLike(mediaItem)
                if (likedAt == null || likedAt == -1L)
                    SmartMessage(context.resources.getString(R.string.added_to_favorites), context = context)
                else
                    SmartMessage(context.resources.getString(R.string.removed_from_favorites), context = context)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    addToYtLikedSong(mediaItem)
                }
            }
            updateLike = false
        }
        if (updateDislike) {
            if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
                SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
            } else if (!isYouTubeSyncEnabled()){
                Database.asyncTransaction {
                    if (like(mediaItem.mediaId, setDisLikeState(likedAt)) == 0)
                        insert(mediaItem, Song::toggleDislike)
                    }
                if (likedAt == null || likedAt!! > 0L)
                    SmartMessage(context.resources.getString(R.string.added_to_disliked), context = context)
                else
                    SmartMessage(context.resources.getString(R.string.removed_from_disliked), context = context)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    // can currently not implement dislike for sync, so unliking the song
                    unlikeYtVideoOrSong(mediaItem)
                }
            }
            updateDislike = false
        }
    }

    var positionAndDuration by remember { mutableStateOf(0f to 0f) }


    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) if (miniPlayerType == MiniPlayerType.Essential) {
                updateLike = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                binder.player.seekToPrevious()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (value == SwipeToDismissBoxValue.EndToStart) {
                binder.player.seekToNext()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            return@rememberSwipeToDismissBoxState false
        }
    )
    val backgroundProgress by rememberPreference(backgroundProgressKey, BackgroundProgress.MiniPlayer)
    val effectRotationEnabled by rememberPreference(effectRotationKey, true)
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
    val disableClosingPlayerSwipingDown by rememberPreference(disableClosingPlayerSwipingDownKey, false)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    /******* new player */
    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var updateStatistics by remember { mutableStateOf(true) }
    var updateStatisticsEverySeconds by remember { mutableIntStateOf(0) }
    val steps by remember { mutableIntStateOf(5) }
    var stepToUpdateStats by remember { mutableIntStateOf(1) }

    val inflatedView = LayoutInflater.from(context()).inflate(R.layout.youtube_player, null, false)
    val onlinePlayerView: YouTubePlayerView = inflatedView as YouTubePlayerView
    val customPLayerUi = onlinePlayerView.inflateCustomPlayerUi(R.layout.ayp_base_player_ui)
    var player = remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
    //val enableBackgroundPlayback by remember { mutableStateOf(true) }
    var enableBackgroundPlayback by rememberPreference(isInvincibilityEnabledKey, false)
    /****** */
    if (mediaItem.mediaId != lastYTVideoId) lastYTVideoSeconds = 0f
    LaunchedEffect(mediaItem) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }

        stepToUpdateStats = 1
    }

    SwipeToDismissBox(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        state = dismissState,
        backgroundContent = {
            /*
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "background"
            )
             */

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
        val colorPalette = colorPalette()
        /***** */
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .combinedClickable(
                    onLongClick = {
                        navController?.navigate(NavRoutes.queue.name);
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = {
                        player.value?.pause()
                        showPlayer()
                    }
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount < 0) showPlayer()
                            else if (dragAmount > 20) {
                                if (!disableClosingPlayerSwipingDown) {
                                    player.value?.pause()
                                    binder.player.clearMediaItems()
                                    hidePlayer()
                                    runCatching {
                                        context.stopService(context.intent<OfflinePlayerService>())
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
                .drawBehind {
                    if (backgroundProgress == BackgroundProgress.Both || backgroundProgress == BackgroundProgress.MiniPlayer) {
                        drawRect(
                            color = colorPalette.favoritesOverlay,
                            topLeft = Offset.Zero,
                            size = Size(
                                width = positionAndDuration.first.toFloat() /
                                        positionAndDuration.second.absoluteValue * size.width,
                                height = size.maxDimension
                            )
                        )
                    }
                }
        ) {

            Spacer(
                modifier = Modifier
                    .width(2.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(Dimensions.miniPlayerHeight)
            ) {
                AsyncImage(
                    model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(thumbnailShape())
                        .size(48.dp)
                )
                NowPlayingSongIndicator(mediaItem.mediaId, binder.player)
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(Dimensions.miniPlayerHeight)
                    .weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if ( mediaItem.isExplicit )
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
                        style = typography().xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

                BasicText(
                    text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                    style = typography().xxs.semiBold,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(Dimensions.miniPlayerHeight)
            ) {
               if (miniPlayerType == MiniPlayerType.Essential)
                   IconButton(
                       icon = R.drawable.play_skip_back,
                       color = colorPalette().iconButtonPlayer,
                       onClick = {
                           binder.player.playPrevious()
                           if (effectRotationEnabled) isRotated = !isRotated
                       },
                       modifier = Modifier
                           .rotate(rotationAngle)
                           .padding(horizontal = 2.dp, vertical = 8.dp)
                           .size(24.dp)
                   )

                if (positionAndDuration.second.toLong() != C.TIME_UNSET) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .clickable {
                                if (shouldBePlaying) {
                                    player.value?.pause()
                                } else {
                                    player.value?.play()
                                }
                                if (effectRotationEnabled) isRotated = !isRotated
                            }
                            .background(colorPalette().background2)
                            .size(42.dp)
                    ) {
                        Image(
                            painter = painterResource(if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette().iconButtonPlayer),
                            modifier = Modifier
                                .rotate(rotationAngle)
                                .align(Alignment.Center)
                                .size(24.dp)
                        )
                    }
                } else CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colorPalette().collapsedPlayerProgressBar
                )

               if (miniPlayerType == MiniPlayerType.Essential)
                   IconButton(
                       icon = R.drawable.play_skip_forward,
                       color = colorPalette().iconButtonPlayer,
                       onClick = {
                           binder.player.playNext()
                           if (effectRotationEnabled) isRotated = !isRotated
                       },
                       modifier = Modifier
                           .rotate(rotationAngle)
                           .padding(horizontal = 2.dp, vertical = 8.dp)
                           .size(24.dp)
                   )
                if (miniPlayerType == MiniPlayerType.Modern)
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
        /*****  */

        var albumInfo by remember {
            mutableStateOf(mediaItem.mediaMetadata.extras?.getString("albumId")?.let { albumId ->
                Info(albumId, null)
            })
        }

        var artistsInfo by remember {
            mutableStateOf(
                mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { artistNames ->
                    mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artistIds ->
                        artistNames.zip(artistIds).map { (authorName, authorId) ->
                            Info(authorId, authorName)
                        }
                    }
                }
            )
        }

        /********** NEW PLAYER */

        var mediaSession = remember { MediaSessionCompat(context(), "OnlineMiniPlayer")}
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setRatingType(RatingCompat.RATING_NONE)
        mediaSession.setSessionActivity(PendingIntent.getActivity(
            appContext(),
            0,
            Intent(appContext(), MainActivity::class.java),
            //.putExtra("expandPlayerBottomSheet", true),
            PendingIntent.FLAG_IMMUTABLE
        ))

        mediaSession.isActive = true
        val actions = remember {
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO
        }
        var stateBuilder = remember {
            PlaybackStateCompat.Builder().setActions(actions.let {
                if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
            })
        }
        var bitmapProvider = remember {
            BitmapProvider(
                bitmapSize = (512 * appContext().resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            )
        }

        LaunchedEffect(playerState.value) {
            shouldBePlaying = playerState.value == PlayerConstants.PlayerState.PLAYING

            if (playerState.value == PlayerConstants.PlayerState.ENDED) {
                // TODO Implement repeat mode in queue
                if (getQueueLoopType() != QueueLoopType.Default)
                    player.value?.seekTo(0f)

                updateStatistics = true
            }

            withContext(Dispatchers.Main) {
                mediaSession.setPlaybackState(
                    stateBuilder
                        .setState(
                            if (shouldBePlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                            (currentSecond/1000).toLong(),
                            1f
                        )
                        .build()
                )

                updateNotification(
                    mediaItem.mediaMetadata.title.toString(),
                    mediaItem.mediaMetadata.artist.toString(),
                    if(shouldBePlaying) R.drawable.pause else R.drawable.play,
                    if (shouldBePlaying) "pause" else "play",
                    if (shouldBePlaying) ActionIntent("it.fast4x.riplay.onlineplayer.pause").pendingIntent
                    else ActionIntent("it.fast4x.riplay.onlineplayer.play").pendingIntent,
                    mediaSession,
                    bitmapProvider
                )
            }

        }

        var songIsAudioOnly by rememberSaveable {
            mutableStateOf<Boolean>(true)
        }

        LaunchedEffect(mediaItem.mediaId) {
            Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
            Database.song(mediaItem.mediaId).distinctUntilChanged()
                .collect { songIsAudioOnly = it?.isVideo == false }
        }


        LaunchedEffect(Dispatchers.Main) {
            mediaSession.setCallback(
                MediaSessionCallback(
                    binder,
                    {
                        println("OnlineMiniPlayer callback play")
                        player.value?.play()
                    },
                    {
                        println("OnlineMiniPlayer callback pause")
                        player.value?.pause()
                    }
                )
            )

            mediaSession.setPlaybackState(stateBuilder.build())
        }

        LaunchedEffect(mediaItem) {
            // Ensure that the song is in database
            CoroutineScope(Dispatchers.IO).launch {
                Database.asyncTransaction {
                    insert(mediaItem.asSong)
                }

                Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }

            }
            withContext(Dispatchers.IO) {
                albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
                artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
            }

            stepToUpdateStats = 1

            println("OnlineMiniPlayer LaunchedEffect change mediaItem ${mediaItem.mediaId}")

            bitmapProvider.load(mediaItem.mediaMetadata.artworkUri, {
                println("OnlineMiniPlayer LaunchedEffect BitmapProvider ")
            })

            player.value?.loadVideo(mediaItem.mediaId, 0f)

            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaItem.mediaMetadata.title.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaItem.mediaMetadata.artist.toString())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (currentDuration/1000).toLong())
                    .build()
            )

            println("OnlineMiniPLayer LaunchedEffect change mediaItem isVideo? ${mediaItem.isVideo} song is audio only? ${songIsAudioOnly}")
        }

        LaunchedEffect(currentSecond, currentDuration) {
            positionAndDuration = currentSecond to currentDuration

            withContext(Dispatchers.Main) {
                mediaSession.setPlaybackState(
                    stateBuilder
                        .setState(
                            if (shouldBePlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                            (currentSecond/1000).toLong(),
                            1f
                        )
                        .build()
                )

                println("OnlineMiniPlayer LaunchedEffect currentSecond ${(currentSecond/1000).toLong()} shouldBePlaying $shouldBePlaying")

                updateNotification(
                    mediaItem.mediaMetadata.title.toString(),
                    mediaItem.mediaMetadata.artist.toString(),
                    if(shouldBePlaying) R.drawable.pause else R.drawable.play,
                    if (shouldBePlaying) "pause" else "play",
                    if (shouldBePlaying) ActionIntent("it.fast4x.riplay.onlineplayer.pause").pendingIntent
                    else ActionIntent("it.fast4x.riplay.onlineplayer.play").pendingIntent,
                    mediaSession,
                    bitmapProvider
                )
            }

            updateStatisticsEverySeconds = (currentDuration / steps).toInt()

            if (getPauseListenHistory()) return@LaunchedEffect

            if (currentSecond.toInt() == updateStatisticsEverySeconds * stepToUpdateStats) {
                stepToUpdateStats++
                val totalPlayTimeMs = (currentSecond * 1000).toLong()
                Database.asyncTransaction {
                    incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
                }

                val minTimeForEvent = getMinTimeForEvent().ms

                if (totalPlayTimeMs > minTimeForEvent) {

                    Database.asyncTransaction {
                        try {
                            insert(
                                Event(
                                    songId = mediaItem.mediaId,
                                    timestamp = System.currentTimeMillis(),
                                    playTime = totalPlayTimeMs
                                )
                            )
                        } catch (e: SQLException) {
                            Timber.e("PlayerServiceModern onPlaybackStatsReady SQLException ${e.stackTraceToString()}")
                        }
                    }
                }
            }

        }

        AndroidView(

            factory = {

                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

//                        val customPlayerUiController =
//                            CustomBasePlayerUiControllerAsListener(
//                                it,
//                                customPLayerUi,
//                                youTubePlayer,
//                                onlinePlayerView
//                            )
//                        youTubePlayer.addListener(customPlayerUiController)

                        // Used to show default player ui with defaultPlayerUiController as custom view
                        val customUiController =
                            CustomDefaultPlayerUiController(
                                onlinePlayerView,
                                youTubePlayer,
                                onTap = {}
                            )
                        customUiController.showUi(false) // disable all default controls and buttons
                        customUiController.showMenuButton(false)
                        customUiController.showVideoTitle(false)
                        customUiController.showPlayPauseButton(false)
                        customUiController.showDuration(false)
                        customUiController.showCurrentTime(false)
                        customUiController.showSeekBar(false)
                        customUiController.showBufferingProgress(false)
                        customUiController.showYouTubeButton(false)
                        customUiController.showFullscreenButton(false)
                        onlinePlayerView.setCustomPlayerUi(customUiController.rootView)

                        //youTubePlayer.loadOrCueVideo(lifecycleOwner.lifecycle, mediaItem.mediaId, lastYTVideoSeconds)
                        println("OnlineMiniPlayer: onReady shouldBePlaying: $shouldBePlaying")
                        youTubePlayer.cueVideo(mediaItem.mediaId, lastYTVideoSeconds)
                        //youTubePlayer.loadVideo(mediaItem.mediaId, lastYTVideoSeconds)

                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond = second
                        lastYTVideoSeconds = second
                        lastYTVideoId = mediaItem.mediaId

                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        super.onVideoDuration(youTubePlayer, duration)
                        currentDuration = duration
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        super.onStateChange(youTubePlayer, state)

                        playerState.value = state
                    }


                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false

                    if (enableBackgroundPlayback)
                        enableBackgroundPlayback(true)
                    else
                        lifecycleOwner.lifecycle.addObserver(this)

                    initialize(listener, iFramePlayerOptions)
                }

            },
            update = {
                it.enableBackgroundPlayback(enableBackgroundPlayback)
                it.layoutParams = ViewGroup.LayoutParams(
                    100,
                    100
                )

            }
        )

        /******* */


    }
}