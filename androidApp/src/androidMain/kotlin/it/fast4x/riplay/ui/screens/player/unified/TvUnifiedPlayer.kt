package it.fast4x.riplay.ui.screens.player.unified

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalPlayerServiceState
import it.fast4x.riplay.R
import it.fast4x.riplay.cast.ritune.models.RiTuneRemoteCommand
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Info
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLUR_SCALE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EFFECT_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.JUMP_PREVIOUS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_LOOP_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TEXT_OUTLINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VINYL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.services.playback.PlayerState
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.AudioCassette
import it.fast4x.riplay.ui.components.themed.PlayerMenu
import it.fast4x.riplay.ui.components.themed.RotateThumbnailCoverAnimationModern
import it.fast4x.riplay.ui.components.themed.Turntable
import it.fast4x.riplay.ui.screens.player.common.Lyrics
import it.fast4x.riplay.ui.screens.player.common.NextVisualizer
import it.fast4x.riplay.ui.screens.player.common.Queue
import it.fast4x.riplay.ui.styling.ColorPalette
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.BlurTransformation
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.LandscapeToSquareTransformation
import it.fast4x.riplay.utils.PlayerViewModel
import it.fast4x.riplay.utils.PlayerViewModelFactory
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.conditional
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getBitmapFromUrl
import it.fast4x.riplay.utils.getIconQueueLoopState
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shuffleQueue
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
@ExperimentalPermissionsApi
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun TvUnifiedPlayer(
    navController: NavController,
    onlineCore: @Composable () -> Unit,
    onDismiss: () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    if (binder.player.currentTimeline.windowCount == 0) return

    val playerState = LocalPlayerServiceState.current
    val context = LocalContext.current
    val menuState = LocalGlobalSheetState.current
    val color = colorPalette()

    // ── Track state ──────────────────────────────────────────────
    val mediaItemPolicy = object : SnapshotMutationPolicy<MediaItem?> {
        override fun equivalent(a: MediaItem?, b: MediaItem?): Boolean =
            a?.mediaId == b?.mediaId
    }

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, mediaItemPolicy)
    }
    var mediaItems by remember {
        mutableStateOf(binder.player.currentTimeline.mediaItems)
    }
    var queueLoopType by rememberPreference(QUEUE_LOOP_TYPE.key, QueueLoopType.Default)
    var isShowingLyrics by rememberSaveable { mutableStateOf(false) }
    var isShowingVisualizer by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var jumpPrevious by rememberPreference(JUMP_PREVIOUS.key, "3")
    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    val colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    val textoutline by rememberPreference(TEXT_OUTLINE.key, false)

    val visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    val showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    val thumbnailRoundness by rememberPreference(
        THUMBNAIL_ROUNDNESS.key, ThumbnailRoundness.Light
    )
    val showCoverThumbnailAnimation by rememberPreference(
        SHOW_COVER_THUMBNAIL_ANIMATION.key, false
    )
    var coverThumbnailAnimation by rememberPreference(
        COVER_THUMBNAIL_ANIMATION.key, ThumbnailCoverType.Vinyl
    )
    var imageCoverSize by rememberPreference(VINYL_SIZE.key, 50f)

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                mediaItems = timeline.mediaItems
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                queueLoopType = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> QueueLoopType.RepeatOne
                    Player.REPEAT_MODE_ALL -> QueueLoopType.RepeatAll
                    else -> QueueLoopType.Default
                }
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

    // ── ViewModel / position ─────────────────────────────────────
    val factory = remember(binder) { PlayerViewModelFactory(binder) }
    val playerViewModel: PlayerViewModel = viewModel(factory = factory)
    val positionAndDuration by playerViewModel.positionAndDuration.collectAsStateWithLifecycle()

    // ── DB info ──────────────────────────────────────────────────
    var albumInfo by remember {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getString("albumId")?.let { Info(it, null) }
        )
    }
    var artistsInfo by remember {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { names ->
                mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { ids ->
                    names.zip(ids).map { Info(it.second, it.first) }
                }
            }
        )
    }
    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            Database.asyncTransaction { insert(mediaItem.asSong) }
            Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
            albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
        }
    }

    var albumId = albumInfo?.id
        ?: mediaItem.mediaMetadata.extras?.getString("albumId")

    // ── Dynamic color ────────────────────────────────────────────
    var dynamicColorPalette by remember { mutableStateOf(color) }
    val playerBackgroundColors by rememberPreference(
        PLAYER_BACKGROUND_COLORS.key, PlayerBackgroundColors.BlurredCoverColor
    )

    LaunchedEffect(mediaItem.mediaId) {
        if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
            playerBackgroundColors == PlayerBackgroundColors.CoverColor ||
            playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor
        ) {
            try {
                val bitmap = getBitmapFromUrl(
                    context,
                    mediaItem.mediaMetadata.artworkUri.toString().toThumbnail(1200).toString()
                )
                dynamicColorPalette = dynamicColorPaletteOf(bitmap, true) ?: color
            } catch (_: Exception) {
                dynamicColorPalette = color
            }
        }
    }

    // ── Focus management ─────────────────────────────────────────
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val topRowFocusRequester = remember { FocusRequester() }
    val bottomRowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playPauseFocusRequester.requestFocus()
    }

    // ── TV Focus indicator modifier ──────────────────────────────
    val tvFocusModifier: Modifier = Modifier.tvFocusIndicator()

    BackHandler(onBack = onDismiss)

    // ── Background ───────────────────────────────────────────────
    val blurStrength by rememberPreference(BLUR_SCALE.key, 25f)
    val backgroundRequest = remember(mediaItem.mediaId) {
        ImageRequest.Builder(context)
            .data(mediaItem.mediaMetadata.artworkUri.toString().toThumbnail(1200))
            .size(1200, 1200)
            .transformations(
                listOf(
                    LandscapeToSquareTransformation(1200),
                    BlurTransformation(scale = 0.5f, radius = blurStrength.toInt())
                )
            )
            .build()
    }
    val backgroundPainter = rememberAsyncImagePainter(model = backgroundRequest)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.background1)
    ) {
        // Blurred background image
        AsyncImage(
            model = backgroundRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        )

        // Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.Black.copy(0.3f),
                        1.0f to Color.Black.copy(0.7f)
                    )
                )
        )

        // ── Main content ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT: Album Art ──────────────────────────────────
            AlbumArtSection(
                mediaItem = mediaItem,
                isPlaying = playerState.isPlaying,
                showthumbnail = showthumbnail,
                showCoverThumbnailAnimation = showCoverThumbnailAnimation,
                coverThumbnailAnimation = coverThumbnailAnimation,
                thumbnailRoundness = thumbnailRoundness,
                imageCoverSize = imageCoverSize,
                isShowingLyrics = isShowingLyrics,
                isShowingVisualizer = isShowingVisualizer,
                onToggleLyrics = {
                    if (isShowingVisualizer) isShowingVisualizer = false
                    isShowingLyrics = !isShowingLyrics
                },
                onToggleVisualizer = {
                    if (isShowingLyrics) isShowingLyrics = false
                    isShowingVisualizer = !isShowingVisualizer
                },
                modifier = Modifier.weight(0.45f)
            )

            // ── RIGHT: Info + Controls ───────────────────────────
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .padding(start = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // Song title & artist
                TrackInfoSection(
                    mediaItem = mediaItem,
                    artistIds = artistsInfo,
                    albumId = albumId,
                    navController = navController,
                    disableScrollingText = disableScrollingText,
                    colorPaletteMode = colorPaletteMode,
                    textoutline = textoutline,
                    dynamicColorPalette = dynamicColorPalette,
                    playerBackgroundColors = playerBackgroundColors,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Seek bar
                TvSeekBar(
                    position = positionAndDuration.first.toFloat(),
                    duration = positionAndDuration.second.toFloat(),
                    onSeek = { pos ->
                        if (binder.player.currentMediaItem?.isLocal == true)
                            binder.player.seekTo(pos.toLong())
                        else
                            binder.onlinePlayer?.seekTo(pos.div(1000))
                    },
                    focusRequester = seekBarFocusRequester,
                    nextFocusUp = topRowFocusRequester,
                    nextFocusDown = playPauseFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 32.dp)
                )

                // Main controls row (prev, play/pause, next)
                MainControlsRow(
                    binder = binder,
                    playerState = playerState,
                    mediaItem = mediaItem,
                    positionAndDuration = positionAndDuration,
                    jumpPrevious = jumpPrevious,
                    playPauseFocusRequester = playPauseFocusRequester,
                    seekBarFocusRequester = seekBarFocusRequester,
                    bottomRowFocusRequester = bottomRowFocusRequester,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Secondary actions row
                SecondaryActionsRow(
                    mediaItem = mediaItem,
                    isShowingLyrics = isShowingLyrics,
                    isShowingVisualizer = isShowingVisualizer,
                    visualizerEnabled = visualizerEnabled,
                    queueLoopType = queueLoopType,
                    onQueueLoopTypeChange = { queueLoopType = it },
                    onToggleLyrics = {
                        if (isShowingVisualizer) isShowingVisualizer = false
                        isShowingLyrics = !isShowingLyrics
                    },
                    onToggleVisualizer = {
                        if (isShowingLyrics) isShowingLyrics = false
                        isShowingVisualizer = !isShowingVisualizer
                    },
                    onShowQueue = { showQueue = true },
                    onShowMenu = {
                        menuState.display {
                            PlayerMenu(
                                navController = navController,
                                onDismiss = menuState::hide,
                                mediaItem = mediaItem,
                                binder = binder,
                                onClosePlayer = onDismiss,
                                onInfo = {
                                    navController.navigate(
                                        "${NavRoutes.videoOrSongInfo.name}/${mediaItem.mediaId}"
                                    )
                                },
                                disableScrollingText = disableScrollingText
                            )
                        }
                    },
                    onDismiss = onDismiss,
                    focusRequester = bottomRowFocusRequester,
                    topRowFocusRequester = topRowFocusRequester,
                    playPauseFocusRequester = playPauseFocusRequester,
                    modifier = Modifier
                )
            }
        }

        // ── Lyrics overlay ───────────────────────────────────────
        if (isShowingLyrics) {
            TvLyricsOverlay(
                mediaItem = mediaItem,
                positionAndDuration = positionAndDuration,
                isLandscape = true,
                onDismiss = { isShowingLyrics = false },
                disableScrollingText = disableScrollingText,
                clickLyricsText = true,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .padding(start = 64.dp, top = 48.dp, bottom = 48.dp)
            )
        }

        // ── Visualizer overlay ───────────────────────────────────
        if (isShowingVisualizer) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .padding(start = 64.dp, top = 48.dp, bottom = 48.dp)
            ) {
                NextVisualizer(isDisplayed = true)
            }
        }
    }

    // ── Queue sheet ──────────────────────────────────────────────
    if (showQueue) {
        CustomModalBottomSheet(
            showSheet = showQueue,
            onDismissRequest = { showQueue = false },
            containerColor = color.background2,
            contentColor = color.background2,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = {},
            shape = thumbnailRoundness.shape(),
        ) {
            Queue(
                navController = navController,
                showPlayer = {},
                hidePlayer = {},
                onDismiss = {
                    queueLoopType = it
                    showQueue = false
                },
                onDiscoverClick = {}
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Sub-composables
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun AlbumArtSection(
    mediaItem: MediaItem,
    isPlaying: Boolean,
    showthumbnail: Boolean,
    showCoverThumbnailAnimation: Boolean,
    coverThumbnailAnimation: ThumbnailCoverType,
    thumbnailRoundness: ThumbnailRoundness,
    imageCoverSize: Float,
    isShowingLyrics: Boolean,
    isShowingVisualizer: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    if (isShowingLyrics || isShowingVisualizer) return // hide art when lyrics/vis shown

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxHeight(0.7f),
        contentAlignment = Alignment.Center
    ) {
        val request = remember(mediaItem.mediaId) {
            ImageRequest.Builder(context)
                .data(mediaItem.mediaMetadata.artworkUri.toString().toThumbnail(1200))
                .size(1200, 1200)
                .transformations(LandscapeToSquareTransformation(1200))
                .build()
        }
        val coverPainter = rememberAsyncImagePainter(model = request)

        val baseModifier = Modifier
            .fillMaxSize()
            .clip(thumbnailRoundness.shape())

        if (showCoverThumbnailAnimation && !mediaItem.isVideo) {
            when (coverThumbnailAnimation) {
                ThumbnailCoverType.CD,
                ThumbnailCoverType.Vinyl,
                ThumbnailCoverType.CDWithCover -> {
//                    RotateThumbnailCoverAnimationModern(
//                        painter = coverPainter,
//                        isSongPlaying = isPlaying,
//                        modifier = baseModifier,
//                        state = null, // single item, no pager
//                        pageIndex = 0,
//                        imageCoverSize = imageCoverSize,
//                        type = coverThumbnailAnimation
//                    )
                }
                ThumbnailCoverType.AudioCassette,
                ThumbnailCoverType.AudioCassetteWithCover -> {
//                    AudioCassette(
//                        modifier = baseModifier,
//                        isPlaying = isPlaying,
//                        painter = coverPainter,
//                        playerState = null,
//                        withCover = coverThumbnailAnimation == ThumbnailCoverType.AudioCassetteWithCover
//                    )
                }
                ThumbnailCoverType.Turntable -> {
                    Turntable(
                        modifier = baseModifier,
                        isPlaying = isPlaying,
                        painter = coverPainter,
                    )
                }
            }
        } else {
            Image(
                painter = coverPainter,
                contentDescription = mediaItem.mediaMetadata.title?.toString(),
                contentScale = ContentScale.Fit,
                modifier = baseModifier
            )
        }
    }
}

@Composable
private fun TrackInfoSection(
    mediaItem: MediaItem,
    artistIds: List<Info>?,
    albumId: String?,
    navController: NavController,
    disableScrollingText: Boolean,
    colorPaletteMode: ColorPaletteMode,
    textoutline: Boolean,
    dynamicColorPalette: ColorPalette,
    playerBackgroundColors: PlayerBackgroundColors,
    modifier: Modifier = Modifier,
) {
    val color = colorPalette()
    val titleColor = when (playerBackgroundColors) {
        PlayerBackgroundColors.MidnightOdyssey -> dynamicColorPalette.background2
        else -> color.text
    }
    val subtitleColor = color.text.copy(alpha = 0.7f)

    Column(modifier = modifier) {
        // Title
        Box {
            BasicText(
                text = mediaItem.mediaMetadata.title?.toString() ?: "",
                style = typography().l.semiBold.merge(TextStyle(color = titleColor)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
            )
            if (textoutline) {
                BasicText(
                    text = mediaItem.mediaMetadata.title?.toString() ?: "",
                    style = typography().l.semiBold.merge(
                        TextStyle(
                            drawStyle = Stroke(width = 1f, join = StrokeJoin.Round),
                            color = if (colorPaletteMode == ColorPaletteMode.Light)
                                Color.White.copy(0.5f) else Color.Black
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Artist
        val artistText = mediaItem.mediaMetadata.artist?.toString() ?: ""
        Box {
            BasicText(
                text = artistText,
                style = typography().m.semiBold.merge(TextStyle(color = subtitleColor)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .conditional(!disableScrollingText) { basicMarquee() }
                    .clickable {
                        // Navigate to artist
                        artistIds?.firstOrNull()?.id?.let { artistId ->
                            navController.navigate("artist/$artistId")
                        }
                    }
            )
            if (textoutline) {
                BasicText(
                    text = artistText,
                    style = typography().m.semiBold.merge(
                        TextStyle(
                            drawStyle = Stroke(width = 0.5f, join = StrokeJoin.Round),
                            color = if (colorPaletteMode == ColorPaletteMode.Light)
                                Color.White.copy(0.5f) else Color.Black
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                )
            }
        }
    }
}

/**
 * TV-friendly seek bar: focusable, left/right D-pad steps, large thumb.
 */
/**
 * TV-friendly seek bar: focusable, left/right D-pad steps, large thumb.
 */
@Composable
private fun TvSeekBar(
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    focusRequester: FocusRequester,
    nextFocusUp: FocusRequester,
    nextFocusDown: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val color = colorPalette()
    var isFocused by remember { mutableStateOf(false) }
    val stepMs = 5000f // 5 secondi per press del D-pad

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        BasicText(
            text = formatAsDuration(position.toLong()),
            style = typography().xs.semiBold.merge(TextStyle(color = color.text)),
            modifier = Modifier.width(64.dp)
        )

        // BoxWithConstraints ci permette di conoscere la larghezza effettiva (maxWidth)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(32.dp) // Target grande per il focus da TV
                .focusRequester(focusRequester)
                .focusProperties {
                    up = nextFocusUp
                    down = nextFocusDown
                }
                .onFocusChanged { isFocused = it.isFocused }
                .focusable() // FONDAMENTALE: rende il Box capace di ricevere focus e key events
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            onSeek((position - stepMs).coerceAtLeast(0f))
                            true
                        }
                        Key.DirectionRight -> {
                            onSeek((position + stepMs).coerceAtMost(duration))
                            true
                        }
                        else -> false
                    }
                }
                .clip(RoundedCornerShape(4.dp))
                .background(color.background2.copy(alpha = 0.4f))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) color.accent else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(vertical = 12.dp)
        ) {
            val progress = if (duration > 0) position / duration else 0f

            // Barra di riempimento del progresso
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(color.accent)
            )

            // Pallino (Thumb) indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    // Ora maxWidth è disponibile come Dp in BoxWithConstraintsScope
                    .offset(x = (progress * maxWidth) - 6.dp)
                    .size(12.dp)
                    .background(Color.White, CircleShape)
            )
        }

        BasicText(
            text = formatAsDuration(duration.toLong()),
            style = typography().xs.semiBold.merge(TextStyle(color = color.text)),
            modifier = Modifier.width(64.dp)
        )
    }
}

/**
 * Main transport controls: Previous, Rewind, Play/Pause, Forward, Next
 */
@OptIn(UnstableApi::class)
@Composable
private fun MainControlsRow(
    binder: PlayerService.Binder,
    playerState: PlayerState,
    mediaItem: MediaItem,
    positionAndDuration: Pair<Long, Long>,
    jumpPrevious: String,
    playPauseFocusRequester: FocusRequester,
    seekBarFocusRequester: FocusRequester,
    bottomRowFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val color = colorPalette()
    val riTuneClient = binder.riTuneCastClient
    val scope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Previous
        TvPlayerButton(
            icon = R.drawable.play_skip_back,
            contentDescription = "Previous",
            onClick = {
                if (jumpPrevious == "") return@TvPlayerButton
                if (!binder.player.hasPreviousMediaItem() ||
                    (jumpPrevious != "0" && positionAndDuration.first > jumpPrevious.toFloat())
                ) {
                    if (binder.player.currentMediaItem?.isLocal == true)
                        binder.player.seekTo(0)
                    else binder.onlinePlayer?.seekTo(0f)
                } else {
                    binder.player.playPrevious()
                }
            },
            focusRequester = remember { FocusRequester() },
            nextFocusUp = seekBarFocusRequester,
            nextFocusDown = bottomRowFocusRequester,
        )

        // Rewind 10s
        TvPlayerButton(
            icon = R.drawable.chevron_back,
            contentDescription = "Rewind",
            onClick = {
                val newPos = (binder.player.currentPosition - 10000).coerceAtLeast(0)
                binder.player.seekTo(newPos)
            },
            focusRequester = remember { FocusRequester() },
            nextFocusUp = seekBarFocusRequester,
            nextFocusDown = bottomRowFocusRequester,
        )

        // Play / Pause
        TvPlayerButton(
            icon = if (playerState.isPlaying) R.drawable.pause else R.drawable.play,
            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
            onClick = {
                if (!GlobalSharedData.riTuneCastActive) {
                    if (playerState.isPlaying) {
                        if (binder.player.currentMediaItem?.isLocal == true)
                            binder.player.pause()
                        else binder.onlinePlayer?.pause()
                    } else {
                        if (binder.player.currentMediaItem?.isLocal == true)
                            binder.player.play()
                        else binder.onlinePlayer?.play()
                    }
                } else {
                    scope.launch {
                        riTuneClient.sendCommand(
                            RiTuneRemoteCommand(
                                if (playerState.isPlaying) "pause" else "play",
                                mediaId = mediaItem.mediaId
                            )
                        )
                    }
                }
            },
            focusRequester = playPauseFocusRequester,
            nextFocusUp = seekBarFocusRequester,
            nextFocusDown = bottomRowFocusRequester,
            isPrimary = true,
        )

        // Forward 10s
        TvPlayerButton(
            icon = R.drawable.chevron_forward,
            contentDescription = "Forward",
            onClick = {
                val newPos = (binder.player.currentPosition + 10000)
                    .coerceAtMost(binder.player.duration)
                binder.player.seekTo(newPos)
            },
            focusRequester = remember { FocusRequester() },
            nextFocusUp = seekBarFocusRequester,
            nextFocusDown = bottomRowFocusRequester,
        )

        // Next
        TvPlayerButton(
            icon = R.drawable.play_skip_forward,
            contentDescription = "Next",
            onClick = { binder.player.playNext() },
            focusRequester = remember { FocusRequester() },
            nextFocusUp = seekBarFocusRequester,
            nextFocusDown = bottomRowFocusRequester,
        )
    }
}

/**
 * Secondary actions: Loop, Shuffle, Lyrics, Visualizer, Queue, Menu, Close
 */
@OptIn(UnstableApi::class)
@Composable
private fun SecondaryActionsRow(
    mediaItem: MediaItem,
    isShowingLyrics: Boolean,
    isShowingVisualizer: Boolean,
    visualizerEnabled: Boolean,
    queueLoopType: QueueLoopType,
    onQueueLoopTypeChange: (QueueLoopType) -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleVisualizer: () -> Unit,
    onShowQueue: () -> Unit,
    onShowMenu: () -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester,
    topRowFocusRequester: FocusRequester,
    playPauseFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val color = colorPalette()
    val effectRotationEnabled by rememberPreference(EFFECT_ROTATION.key, true)
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val binder = LocalPlayerServiceBinder.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                up = playPauseFocusRequester
            }
    ) {
        // Loop
        TvPlayerButton(
            icon = getIconQueueLoopState(queueLoopType),
            contentDescription = "Repeat mode",
            tint = color.accent,
            onClick = {
                onQueueLoopTypeChange(setQueueLoopState(queueLoopType))
                if (effectRotationEnabled) isRotated = !isRotated
            },
        )

        // Shuffle
        TvPlayerButton(
            icon = R.drawable.shuffle,
            contentDescription = "Shuffle",
            tint = color.accent,
            onClick = {
                binder?.player?.shuffleQueue()
            },
        )

        // Lyrics
        TvPlayerButton(
            icon = R.drawable.song_lyrics,
            contentDescription = "Lyrics",
            tint = if (isShowingLyrics) color.accent else color.textDisabled,
            onClick = onToggleLyrics,
        )

        // Visualizer
        if (visualizerEnabled) {
            TvPlayerButton(
                icon = R.drawable.sound_effect,
                contentDescription = "Visualizer",
                tint = if (isShowingVisualizer) color.text else color.textDisabled,
                onClick = onToggleVisualizer,
            )
        }

        // Queue
        TvPlayerButton(
            icon = R.drawable.list,
            contentDescription = "Queue",
            tint = color.accent,
            onClick = onShowQueue,
        )

        // Menu
        TvPlayerButton(
            icon = R.drawable.ellipsis_horizontal,
            contentDescription = "More options",
            tint = color.accent,
            onClick = onShowMenu,
        )

        // Close player
        TvPlayerButton(
            icon = R.drawable.chevron_down,
            contentDescription = "Close player",
            tint = color.text,
            onClick = onDismiss,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Reusable TV button with focus indicator
// ═══════════════════════════════════════════════════════════════════

@Composable
fun TvPlayerButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    nextFocusUp: FocusRequester? = null,
    nextFocusDown: FocusRequester? = null,
    tint: Color = colorPalette().text,
    isPrimary: Boolean = false,
) {
    val color = colorPalette()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .focusRequester(focusRequester)
            .then(
                if (nextFocusUp != null) Modifier.focusProperties { up = nextFocusUp }
                else Modifier
            )
            .then(
                if (nextFocusDown != null) Modifier.focusProperties { down = nextFocusDown }
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .size(if (isPrimary) 72.dp else 52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isPrimary) color.accent
                else if (isFocused) color.background2.copy(alpha = 0.6f)
                else Color.Transparent
            )
            .then(
                if (isFocused && !isPrimary)
                    Modifier.border(2.dp, color.accent, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (isPrimary) color.onAccent else tint,
            modifier = Modifier.size(if (isPrimary) 32.dp else 22.dp)
        )
    }
}

/**
 * Focus indicator modifier for TV: adds a glow/border on focus.
 */
fun Modifier.tvFocusIndicator(): Modifier = this.onFocusChanged { state ->
    // The actual visual indicator is applied via border/background in composables.
    // This modifier is a hook for haptic feedback or logging if needed.
}

/**
 * Simplified lyrics overlay for TV — full-screen left panel
 */
@OptIn(UnstableApi::class)
@Composable
private fun TvLyricsOverlay(
    mediaItem: MediaItem,
    positionAndDuration: Pair<Long, Long>,
    isLandscape: Boolean,
    onDismiss: () -> Unit,
    disableScrollingText: Boolean,
    clickLyricsText: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(24.dp)
    ) {
        Lyrics(
            mediaId = mediaItem.mediaId,
            isDisplayed = true,
            onDismiss = onDismiss,
            ensureSongInserted = { Database.insert(mediaItem) },
            size = 800.dp,
            mediaMetadataProvider = mediaItem::mediaMetadata,
            durationProvider = { positionAndDuration.second },
            isLandscape = isLandscape,
            clickLyricsText = clickLyricsText,
        )
    }
}