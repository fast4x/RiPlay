package it.fast4x.riplay.ui.screens.player.online

import android.database.SQLException
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.getMinTimeForEvent
import it.fast4x.riplay.getPauseListenHistory
import it.fast4x.riplay.getQueueLoopType
import it.fast4x.riplay.models.Event
import it.fast4x.riplay.models.Info
import it.fast4x.riplay.models.ui.toUiMedia
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.themed.PlayerMenu
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.backgroundProgressKey
import it.fast4x.riplay.utils.blurStrengthKey
import it.fast4x.riplay.utils.colorPaletteModeKey
import it.fast4x.riplay.utils.controlsExpandedKey
import it.fast4x.riplay.utils.disableScrollingTextKey
import it.fast4x.riplay.utils.effectRotationKey
import it.fast4x.riplay.utils.expandedplayerKey
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isShowingLyricsKey
import it.fast4x.riplay.utils.lastVideoIdKey
import it.fast4x.riplay.utils.lastVideoSecondsKey
import it.fast4x.riplay.utils.playerBackgroundColorsKey
import it.fast4x.riplay.utils.playerThumbnailSizeKey
import it.fast4x.riplay.utils.playerTypeKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.showButtonPlayerMenuKey
import it.fast4x.riplay.utils.showTopActionsBarKey
import it.fast4x.riplay.utils.timelineExpandedKey
import it.fast4x.riplay.utils.titleExpandedKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.absoluteValue


@UnstableApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalTextApi
@Composable
fun OnlinePlayer(
    lifecycleOwner: LifecycleOwner,
    showPlayer: Boolean = true,
    onCurrentSecond: (second: Float) -> Unit,
    onVideoDuration: (duration: Float) -> Unit,
    onVideoEnded: () -> Unit,
    onSwitchToAudioPlayer: () -> Unit,
    onDismiss: () -> Unit,
    navController: NavController,
    mediaItem: MediaItem,
) {

    if (!showPlayer) return

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var positionAndDuration by remember { mutableStateOf(0f to 0f) }

    var isShowingVisualizer by remember { mutableStateOf(false) }
    val playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    var expandedplayer by rememberPreference(expandedplayerKey, false)
    var titleExpanded by rememberPreference(titleExpandedKey, false)
    var timelineExpanded by rememberPreference(timelineExpandedKey, false)
    var controlsExpanded by rememberPreference(controlsExpandedKey, false)
    var isShowingLyrics by rememberPreference(isShowingLyricsKey, false)
    val binder = LocalPlayerServiceBinder.current
    var nullableMediaItem by remember {
        mutableStateOf(binder?.player?.currentMediaItem, neverEqualPolicy())
    }
    val mediaItem = nullableMediaItem ?: return
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

    var updateBrush by remember { mutableStateOf(false) }

    val ExistIdsExtras =
        mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.size.toString()
    val ExistAlbumIdExtras = mediaItem.mediaMetadata.extras?.getString("albumId")

    var albumId = albumInfo?.id
    if (albumId == null) albumId = ExistAlbumIdExtras

    var artistIds = arrayListOf<String>()
    var artistNames = arrayListOf<String>()


    artistsInfo?.forEach { (id) -> artistIds = arrayListOf(id) }
    if (ExistIdsExtras.equals(0)
            .not()
    ) mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.toCollection(artistIds)

    artistsInfo?.forEach { (name) -> artistNames = arrayListOf(name) }
    if (ExistIdsExtras.equals(0)
            .not()
    ) mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.toCollection(artistNames)

    if (artistsInfo?.isEmpty() == true && ExistIdsExtras.equals(0).not()) {
        artistsInfo = artistNames.let { artistNames ->
            artistIds.let { artistIds ->
                artistNames.zip(artistIds).map {
                    Info(it.second, it.first)
                }
            }
        }
    }

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    val defaultStrength = 25f
    var blurStrength by rememberPreference(blurStrengthKey, defaultStrength)

//    val currentYTVideoId by remember { mutableStateOf(ytVideoId) }
//    println("mediaItem youtubePlayer called currentYTVideoId $currentYTVideoId ytVideoId $ytVideoId lastYTVideoId $lastYTVideoId")

    if (mediaItem.mediaId != lastYTVideoId) lastYTVideoSeconds = 0f

    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

    val showTopActionsBar by rememberPreference(showTopActionsBarKey, true)
    val windowInsets = WindowInsets.systemBars
    val playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )
    val color = colorPalette()
    var dynamicColorPalette by remember { mutableStateOf( color ) }
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )
    val effectRotationEnabled by rememberPreference(effectRotationKey, true)
    val showButtonPlayerMenu by rememberPreference(showButtonPlayerMenuKey, false)
    val menuState = LocalMenuState.current
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val backgroundProgress by rememberPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )
    val playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )
    var updateStatistics by remember { mutableStateOf(true) }
    var updateStatisticsEverySeconds by remember { mutableIntStateOf(0) }
    val steps by remember { mutableIntStateOf(5) }
    var stepToUpdateStats by remember { mutableIntStateOf(1) }


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
        updateBrush = true

        stepToUpdateStats = 1
    }

    LaunchedEffect(currentSecond, currentDuration) {
        positionAndDuration = currentSecond to currentDuration

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


    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                top = WindowInsets.systemBars
                    .asPaddingValues()
                    .calculateTopPadding(),
                bottom = WindowInsets.systemBars
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
            .fillMaxSize()
            .drawBehind {
                if (backgroundProgress == BackgroundProgress.Both || backgroundProgress == BackgroundProgress.Player) {
                    drawRect(
                        color = color.favoritesOverlay,
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

        if (showTopActionsBar) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(
                        windowInsets
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                    )
                    //.padding(top = 5.dp)
                    .fillMaxWidth(0.9f)
                    .height(30.dp)
            ) {

                Image(
                    painter = painterResource(R.drawable.chevron_down),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey) dynamicColorPalette.background2 else colorPalette().collapsedPlayerProgressBar),
                    modifier = Modifier
                        .clickable {
                            onDismiss()
                        }
                        .rotate(rotationAngle)
                        //.padding(10.dp)
                        .size(24.dp)
                )


                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey) dynamicColorPalette.background2 else colorPalette().collapsedPlayerProgressBar),
                    modifier = Modifier
                        .clickable {
                            onDismiss()
                            navController.navigate(NavRoutes.home.name)
                        }
                        .rotate(rotationAngle)
                        //.padding(10.dp)
                        .size(24.dp)

                )

                if (!showButtonPlayerMenu)
                    Image(
                        painter = painterResource(R.drawable.ellipsis_vertical),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey) dynamicColorPalette.background2 else colorPalette().collapsedPlayerProgressBar),
                        modifier = Modifier
                            .clickable {
                                menuState.display {
                                    PlayerMenu(
                                        navController = navController,
                                        onDismiss = menuState::hide,
                                        mediaItem = mediaItem,
                                        binder = binder ?: return@display,
                                        onClosePlayer = {
                                            onDismiss()
                                        },
                                        onInfo = {
                                            navController.navigate("${NavRoutes.videoOrSongInfo.name}/${mediaItem.mediaId}")
                                        },
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                            .rotate(rotationAngle)
                            //.padding(10.dp)
                            .size(24.dp)

                    )

            }
            Spacer(
                modifier = Modifier
                    .height(5.dp)
                    .padding(
                        windowInsets
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                    )
            )
        }

        //val onlinePlayerView = YouTubePlayerView(context = context())
        val inflatedView = LayoutInflater.from(context()).inflate(R.layout.youtube_player, null, false)
        val onlinePlayerView: YouTubePlayerView = inflatedView as YouTubePlayerView
        val customPLayerUi = onlinePlayerView.inflateCustomPlayerUi(R.layout.ayp_default_player_ui)
        var player = remember { mutableStateOf<YouTubePlayer?>(null) }
        val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
        var shouldBePlaying by remember { mutableStateOf(false) }
        val enableBackgroundPlayback by remember { mutableStateOf(false) }


        LaunchedEffect(playerState.value) {
            shouldBePlaying = playerState.value == PlayerConstants.PlayerState.PLAYING

            if (playerState.value == PlayerConstants.PlayerState.ENDED) {
                // TODO Implement repeat mode in queue
                if (getQueueLoopType() != QueueLoopType.Default)
                    player.value?.seekTo(0f)

                updateStatistics = true
            }

        }


        val isLandscape = isLandscape

        AndroidView(
            modifier = Modifier
                .applyIf(!isLandscape) {
                    padding(horizontal = playerThumbnailSize.padding.dp)
                },
            factory = {

//                val iFramePlayerOptions = IFramePlayerOptions.Builder()
//                    .controls(1) // show/hide controls
//                    .rel(0) // related video at the end
//                    .ivLoadPolicy(0) // show/hide annotations
//                    .ccLoadPolicy(0) // show/hide captions
//                    // Play a playlist by id
//                    //.listType("playlist")
//                    //.list(PLAYLIST_ID)
//                    .build()

                // Disable default view controls to set custom view
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

                        val customPlayerUiController = CustomPlayerUiController(
                            it,
                            customPLayerUi,
                            youTubePlayer,
                            onlinePlayerView
                        )
                        youTubePlayer.addListener(customPlayerUiController)

                        youTubePlayer.loadVideo(mediaItem.mediaId, lastYTVideoSeconds)

                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond = second
                        onCurrentSecond(second)
                        lastYTVideoSeconds = second
                        lastYTVideoId = mediaItem.mediaId

                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        super.onVideoDuration(youTubePlayer, duration)
                        currentDuration = duration
                        onVideoDuration(duration)
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        super.onStateChange(youTubePlayer, state)
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            onVideoEnded()
                        }
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
                it.layoutParams =  if (!isLandscape) {
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        if (playerThumbnailSize == PlayerThumbnailSize.Expanded)
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        else playerThumbnailSize.height
                    )
                } else {
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        )

        val controlsContent: @Composable (
            modifier: Modifier
        ) -> Unit = { modifierValue ->
            Controls(
                navController = navController,
                onCollapse = onDismiss,
                expandedplayer = expandedplayer,
                titleExpanded = titleExpanded,
                timelineExpanded = timelineExpanded,
                controlsExpanded = controlsExpanded,
                isShowingLyrics = isShowingLyrics,
                media = mediaItem.toUiMedia(positionAndDuration.second.toLong()),
                mediaItem = mediaItem,
                title = mediaItem.mediaMetadata.title?.toString() ?: "",
                artist = mediaItem.mediaMetadata.artist?.toString(),
                artistIds = artistsInfo,
                albumId = albumId,
                shouldBePlaying = shouldBePlaying,
                position = positionAndDuration.first.toLong(),
                duration = positionAndDuration.second.toLong(),
                modifier = modifierValue,
                onBlurScaleChange = { blurStrength = it },
                isExplicit = mediaItem.isExplicit,
                onPlay = { player.value?.play() },
                onPause = { player.value?.pause() },
                onSeekTo = { player.value?.seekTo(it) },
                onNext = { },
                onPrevious = { },
                onToggleShuffleMode = { },
                onToggleLike = { }
            )
        }



        if (!isLandscape)
            Row {
                controlsContent(
                    Modifier
                        .padding(vertical = 30.dp)
                        .border(BorderStroke(1.dp, colorPalette().red))
                )
            }




    }

}
