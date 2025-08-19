package it.fast4x.riplay.ui.screens.player.online

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.SQLException
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.audiofx.AudioEffect
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.IceReflection
import com.mikepenz.hypnoticcanvas.shaders.InkFlow
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid
import com.mikepenz.hypnoticcanvas.shaders.Stage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalLinkDevices
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.appRunningInBackground
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.AnimatedGradient
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.SongsNumber
import it.fast4x.riplay.enums.SwipeAnimationNoThumbnail
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.extensions.link.LINKWEB_COMMAND_PAUSE
import it.fast4x.riplay.extensions.link.LinkDevice
import it.fast4x.riplay.extensions.link.LinkDevicesSelected
import it.fast4x.riplay.extensions.link.linkServiceClientSend
import it.fast4x.riplay.extensions.link.toCommand
import it.fast4x.riplay.extensions.link.toCommandLoad
import it.fast4x.riplay.extensions.link.toCommandPlay
import it.fast4x.riplay.extensions.link.toCommandPlayAt
import it.fast4x.riplay.extensions.link.toLinkDevice
import it.fast4x.riplay.getMinTimeForEvent
import it.fast4x.riplay.getPauseListenHistory
import it.fast4x.riplay.getQueueLoopType
import it.fast4x.riplay.models.Event
import it.fast4x.riplay.models.Info
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.models.ui.toUiMedia
import it.fast4x.riplay.thumbnailShape
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.BottomSheetState
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.themed.AddToPlaylistPlayerMenu
import it.fast4x.riplay.ui.components.themed.BlurParamsDialog
import it.fast4x.riplay.ui.components.themed.CircularSlider
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.PlayerMenu
import it.fast4x.riplay.ui.components.themed.RotateThumbnailCoverAnimationModern
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.ThumbnailOffsetDialog
import it.fast4x.riplay.ui.components.themed.animateBrushRotation
import it.fast4x.riplay.ui.screens.player.offline.Lyrics
import it.fast4x.riplay.ui.screens.player.offline.NextVisualizer
import it.fast4x.riplay.ui.screens.player.offline.Queue
import it.fast4x.riplay.ui.screens.player.offline.StatsForNerds
import it.fast4x.riplay.ui.screens.player.offline.animatedGradient
import it.fast4x.riplay.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.BlurTransformation
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.SearchYoutubeEntity
import it.fast4x.riplay.utils.VerticalfadingEdge2
import it.fast4x.riplay.extensions.preferences.VinylSizeKey
import it.fast4x.riplay.extensions.preferences.actionExpandedKey
import it.fast4x.riplay.extensions.preferences.actionspacedevenlyKey
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtLikedSong
import it.fast4x.riplay.extensions.preferences.albumCoverRotationKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.extensions.preferences.backgroundProgressKey
import it.fast4x.riplay.extensions.preferences.blackgradientKey
import it.fast4x.riplay.extensions.preferences.blurDarkenFactorKey
import it.fast4x.riplay.extensions.preferences.blurStrengthKey
import it.fast4x.riplay.extensions.preferences.bottomgradientKey
import it.fast4x.riplay.extensions.preferences.carouselKey
import it.fast4x.riplay.extensions.preferences.carouselSizeKey
import it.fast4x.riplay.extensions.preferences.castToLinkDeviceEnabledKey
import it.fast4x.riplay.extensions.preferences.clickOnLyricsTextKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.controlsExpandedKey
import it.fast4x.riplay.extensions.preferences.coverThumbnailAnimationKey
import it.fast4x.riplay.utils.currentWindow
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.discoverKey
import it.fast4x.riplay.utils.doubleShadowDrop
import it.fast4x.riplay.utils.durationTextToMillis
import it.fast4x.riplay.extensions.preferences.effectRotationKey
import it.fast4x.riplay.extensions.preferences.expandedplayerKey
import it.fast4x.riplay.extensions.preferences.expandedplayertoggleKey
import it.fast4x.riplay.extensions.preferences.extraspaceKey
import it.fast4x.riplay.extensions.preferences.fadingedgeKey
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.utils.getBitmapFromUrl
import it.fast4x.riplay.utils.getIconQueueLoopState
import it.fast4x.riplay.utils.getLikeState
import it.fast4x.riplay.utils.horizontalFadingEdge
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.extensions.preferences.lastVideoIdKey
import it.fast4x.riplay.extensions.preferences.lastVideoSecondsKey
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.extensions.preferences.miniQueueExpandedKey
import it.fast4x.riplay.extensions.preferences.noblurKey
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeLKey
import it.fast4x.riplay.extensions.preferences.playerTypeKey
import it.fast4x.riplay.extensions.preferences.playlistindicatorKey
import it.fast4x.riplay.extensions.preferences.queueDurationExpandedKey
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.queueTypeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.seamlessPlay
import it.fast4x.riplay.utils.semiBold
import it.fast4x.riplay.utils.setDisLikeState
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.extensions.preferences.showButtonPlayerAddToPlaylistKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerArrowKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerDiscoverKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerDownloadKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLoopKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLyricsKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerMenuKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerShuffleKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSleepTimerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerStartRadioKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSystemEqualizerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerVideoKey
import it.fast4x.riplay.extensions.preferences.showCoverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.showNextSongsInPlayerKey
import it.fast4x.riplay.extensions.preferences.showTopActionsBarKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.showalbumcoverKey
import it.fast4x.riplay.extensions.preferences.showlyricsthumbnailKey
import it.fast4x.riplay.extensions.preferences.showsongsKey
import it.fast4x.riplay.extensions.preferences.showthumbnailKey
import it.fast4x.riplay.extensions.preferences.showvisthumbnailKey
import it.fast4x.riplay.utils.shuffleQueue
import it.fast4x.riplay.extensions.preferences.statsExpandedKey
import it.fast4x.riplay.extensions.preferences.statsfornerdsKey
import it.fast4x.riplay.extensions.preferences.swipeAnimationsNoThumbnailKey
import it.fast4x.riplay.extensions.preferences.swipeUpQueueKey
import it.fast4x.riplay.extensions.preferences.tapqueueKey
import it.fast4x.riplay.extensions.preferences.textoutlineKey
import it.fast4x.riplay.utils.thumbnail
import it.fast4x.riplay.extensions.preferences.thumbnailFadeExKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.thumbnailSpacingKey
import it.fast4x.riplay.extensions.preferences.thumbnailSpacingLKey
import it.fast4x.riplay.extensions.preferences.thumbnailTapEnabledKey
import it.fast4x.riplay.extensions.preferences.thumbnailTypeKey
import it.fast4x.riplay.extensions.preferences.timelineExpandedKey
import it.fast4x.riplay.extensions.preferences.titleExpandedKey
import it.fast4x.riplay.extensions.preferences.topPaddingKey
import it.fast4x.riplay.extensions.preferences.transparentBackgroundPlayerActionBarKey
import it.fast4x.riplay.utils.unlikeYtVideoOrSong
import it.fast4x.riplay.extensions.preferences.visualizerEnabledKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dailyislam.android.utilities.isNetworkConnected
import timber.log.Timber
import kotlin.Float.Companion.POSITIVE_INFINITY
import kotlin.math.absoluteValue
import kotlin.math.sqrt




@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@ExperimentalTextApi
@SuppressLint(
    "SuspiciousIndentation", "RememberReturnType", "NewApi",
    "UnusedBoxWithConstraintsScope"
)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun OnlinePlayer(
    navController: NavController,
    playFromSecond: Float = 0f,
    onlineCore: @Composable () -> Unit,
    player: MutableState<YouTubePlayer?>,
    playerState: MutableState<PlayerConstants.PlayerState>,
    currentDuration: Float,
    currentSecond: Float,
    showControls: Boolean,
    playerSheetState: BottomSheetState,
    onDismiss: () -> Unit,
) {

    BackHandler(
        enabled = playerSheetState.isExpanded,
        onBack = onDismiss
    )

    val menuState = LocalMenuState.current

    val effectRotationEnabled by rememberObservedPreference(effectRotationKey, true)

    val playerThumbnailSize by rememberObservedPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )
    val playerThumbnailSizeL by rememberObservedPreference(
        playerThumbnailSizeLKey,
        PlayerThumbnailSize.Biggest
    )

    val disablePlayerHorizontalSwipe by rememberObservedPreference(disablePlayerHorizontalSwipeKey, false)
    val showlyricsthumbnail by rememberObservedPreference(showlyricsthumbnailKey, false)
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return
    if (binder.player.currentTimeline.windowCount == 0) return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    var shouldBePlaying by remember { mutableStateOf(false) }

    var castToLinkDevice by rememberPreference(castToLinkDeviceEnabledKey, false )

    var isRotated by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )

    val visualizerEnabled by rememberObservedPreference(visualizerEnabledKey, false)

    val defaultStrength = 25f
    val defaultDarkenFactor = 0.2f
    val defaultOffset = 0f
    val defaultSpacing = 0f
    val defaultFade = 5f
    val defaultImageCoverSize = 50f
    var blurStrength by rememberPreference(blurStrengthKey, defaultStrength)
    var thumbnailSpacing by rememberPreference(thumbnailSpacingKey, defaultSpacing)
    var thumbnailSpacingL by rememberPreference(thumbnailSpacingLKey, defaultSpacing)
    var thumbnailFade by rememberPreference(thumbnailFadeKey, defaultFade)
    var thumbnailFadeEx by rememberPreference(thumbnailFadeExKey, defaultFade)
    var imageCoverSize by rememberPreference(VinylSizeKey, defaultImageCoverSize)
    var blurDarkenFactor by rememberPreference(blurDarkenFactorKey, defaultDarkenFactor)
    var showBlurPlayerDialog by remember {
        mutableStateOf(false)
    }
    var showThumbnailOffsetDialog by remember {
        mutableStateOf(false)
    }
    var isShowingLyrics by remember {
        mutableStateOf(false)
    }
    val showvisthumbnail by rememberObservedPreference(showvisthumbnailKey, false)
    var isShowingVisualizer by remember {
        mutableStateOf(false)
    }

    if (showBlurPlayerDialog) {

        BlurParamsDialog(
            onDismiss = { showBlurPlayerDialog = false },
            scaleValue = { blurStrength = it },
            darkenFactorValue = { blurDarkenFactor = it }
        )

    }

    if (showThumbnailOffsetDialog) {

        ThumbnailOffsetDialog(
            onDismiss = { showThumbnailOffsetDialog = false },
            spacingValue = { thumbnailSpacing = it },
            spacingValueL = { thumbnailSpacingL = it },
            fadeValue = { thumbnailFade = it },
            fadeValueEx = { thumbnailFadeEx = it },
            imageCoverSizeValue = { imageCoverSize = it }
        )
    }


    val context = LocalContext.current
    val selectedQueue = LocalSelectedQueue.current
    var mediaItems by remember {
        mutableStateOf(binder.player.currentTimeline.mediaItems)
    }
    var mediaItemIndex by remember {
        mutableIntStateOf(if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex)
    }

    var playerError by remember {
        mutableStateOf<PlaybackException?>(binder.player.playerError)
    }

    val queueDurationExpanded by rememberObservedPreference(queueDurationExpandedKey, true)
    val miniQueueExpanded by rememberObservedPreference(miniQueueExpandedKey, true)
    val statsExpanded by rememberObservedPreference(statsExpandedKey, true)
    val actionExpanded by rememberObservedPreference(actionExpandedKey, true)
    val colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)

    fun PagerState.offsetForPage(page: Int) = (currentPage - page) + currentPageOffsetFraction

    fun PagerState.startOffsetForPage(page: Int): Float {
        return offsetForPage(page).coerceAtLeast(0f)
    }

    fun PagerState.endOffsetForPage(page: Int): Float {
        return offsetForPage(page).coerceAtMost(0f)
    }

    class CirclePath(private val progress: Float, private val origin: Offset = Offset(0f, 0f)) :
        Shape {
        override fun createOutline(
            size: Size, layoutDirection: LayoutDirection, density: Density
        ): Outline {

            val center = Offset(
                x = size.center.x - ((size.center.x - origin.x) * (1f - progress)),
                y = size.center.y - ((size.center.y - origin.y) * (1f - progress)),
            )
            val radius = (sqrt(
                size.height * size.height + size.width * size.width
            ) * .5f) * progress

            return Outline.Generic(Path().apply {
                addOval(
                    Rect(
                        center = center,
                        radius = radius,
                    )
                )
            })
        }
    }

    val linkDevices = LocalLinkDevices.current

    val linkDevicesSavedProvider = MutableStateFlow<LinkDevicesSelected>(LinkDevicesSelected(context))

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val linkDevicesSavedAsState = linkDevicesSavedProvider.collectAsState()

    val linkDevicesSelected = remember { mutableListOf<LinkDevice>() }
    LaunchedEffect(Unit) {
        linkDevicesSelected.addAll(linkDevicesSavedAsState.value.devices())
    }


    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem

                mediaItem?.let {
                    linkServiceClientSend(it.mediaId.toCommandLoad(), castToLinkDevice, linkDevicesSelected)
                }


//        }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                mediaItems = timeline.mediaItems
                mediaItemIndex = binder.player.currentMediaItemIndex
            }

        }
    }

    val mediaItem = nullableMediaItem ?: return

    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val pagerStateFS = rememberPagerState(pageCount = { mediaItems.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val isDraggedFS by pagerStateFS.interactionSource.collectIsDraggedAsState()

    var isShowingSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var delayedSleepTimer by remember {
        mutableStateOf(false)
    }

    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft
        ?: flowOf(null))
        .collectAsState(initial = null)

    var positionAndDuration by remember { mutableStateOf(0f to 0f) }
    var timeRemaining by remember { mutableIntStateOf(0) }


    if (sleepTimerMillisLeft != null)
        if (sleepTimerMillisLeft!! < timeRemaining.toLong() && !delayedSleepTimer) {
            binder.cancelSleepTimer()
            binder.startSleepTimer(timeRemaining.toLong())
            delayedSleepTimer = true
            SmartMessage(
                stringResource(R.string.info_sleep_timer_delayed_at_end_of_song),
                context = context
            )
        }

    val windowInsets = WindowInsets.systemBars

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
    val actionspacedevenly by rememberObservedPreference(actionspacedevenlyKey, false)
    var expandedplayer by rememberObservedPreference(expandedplayerKey, false)

    var updateBrush by remember { mutableStateOf(false) }

    if (showlyricsthumbnail) expandedplayer = false

    LaunchedEffect(mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
        }
        updateBrush = true
    }


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

    var likedAt by remember {
        mutableStateOf<Long?>(null)
    }
    var songIsAudioOnly by remember {
        mutableStateOf<Boolean>(true)
    }

    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
        Database.song(mediaItem.mediaId).distinctUntilChanged()
            .collect { songIsAudioOnly = it?.isVideo == false }
        updateBrush = true
    }

    var showthumbnail by rememberObservedPreference(showthumbnailKey, true)

    val showButtonPlayerAddToPlaylist by rememberObservedPreference(showButtonPlayerAddToPlaylistKey, true)
    val showButtonPlayerArrow by rememberObservedPreference(showButtonPlayerArrowKey, true)
    val showButtonPlayerDownload by rememberObservedPreference(showButtonPlayerDownloadKey, true)
    val showButtonPlayerLoop by rememberObservedPreference(showButtonPlayerLoopKey, true)
    val showButtonPlayerLyrics by rememberObservedPreference(showButtonPlayerLyricsKey, true)
    val expandedplayertoggle by rememberObservedPreference(expandedplayertoggleKey, true)
    val showButtonPlayerShuffle by rememberObservedPreference(showButtonPlayerShuffleKey, true)
    val showButtonPlayerSleepTimer by rememberObservedPreference(showButtonPlayerSleepTimerKey, false)
    val showButtonPlayerMenu by rememberObservedPreference(showButtonPlayerMenuKey, false)
    val showButtonPlayerStartRadio by rememberObservedPreference(showButtonPlayerStartRadioKey, false)
    val showButtonPlayerSystemEqualizer by rememberObservedPreference(
        showButtonPlayerSystemEqualizerKey,
        false
    )
    val showButtonPlayerVideo by rememberObservedPreference(showButtonPlayerVideoKey, false)

    val showTotalTimeQueue by rememberObservedPreference(showTotalTimeQueueKey, true)
    val backgroundProgress by rememberObservedPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )

    var queueLoopType by rememberObservedPreference(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
    var showCircularSlider by remember {
        mutableStateOf(false)
    }
    val showsongs by rememberObservedPreference(showsongsKey, SongsNumber.`2`)
    val showalbumcover by rememberObservedPreference(showalbumcoverKey, true)
    val tapqueue by rememberObservedPreference(tapqueueKey, true)
    val swipeUpQueue by rememberObservedPreference(swipeUpQueueKey, true)
    val playerType by rememberObservedPreference(playerTypeKey, PlayerType.Essential)
    val queueType by rememberObservedPreference(queueTypeKey, QueueType.Essential)
    val noblur by rememberObservedPreference(noblurKey, true)
    val fadingedge by rememberObservedPreference(fadingedgeKey, false)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    if (isShowingSleepTimerDialog) {
        if (sleepTimerMillisLeft != null) {
            ConfirmationDialog(
                text = stringResource(R.string.stop_sleep_timer),
                cancelText = stringResource(R.string.no),
                confirmText = stringResource(R.string.stop),
                onDismiss = { isShowingSleepTimerDialog = false },
                onConfirm = {
                    binder.cancelSleepTimer()
                    delayedSleepTimer = false
                    //onDismiss()
                }
            )
        } else {
            DefaultDialog(
                onDismiss = { isShowingSleepTimerDialog = false }
            ) {
                var amount by remember {
                    mutableStateOf(1)
                }

                BasicText(
                    text = stringResource(R.string.set_sleep_timer),
                    style = typography().s.semiBold,
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 24.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                ) {
                    if (!showCircularSlider) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .alpha(if (amount <= 1) 0.5f else 1f)
                                .clip(CircleShape)
                                .clickable(enabled = amount > 1) { amount-- }
                                .size(48.dp)
                                .background(colorPalette().background0)
                        ) {
                            BasicText(
                                text = "-",
                                style = typography().xs.semiBold
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            BasicText(
                                text = stringResource(
                                    R.string.left,
                                    formatAsDuration(amount * 5 * 60 * 1000L)
                                ),
                                style = typography().s.semiBold,
                                modifier = Modifier
                                    .clickable {
                                        showCircularSlider = !showCircularSlider
                                    }
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .alpha(if (amount >= 60) 0.5f else 1f)
                                .clip(CircleShape)
                                .clickable(enabled = amount < 60) { amount++ }
                                .size(48.dp)
                                .background(colorPalette().background0)
                        ) {
                            BasicText(
                                text = "+",
                                style = typography().xs.semiBold
                            )
                        }

                    } else {
                        CircularSlider(
                            stroke = 40f,
                            thumbColor = colorPalette().accent,
                            text = formatAsDuration(amount * 5 * 60 * 1000L),
                            modifier = Modifier
                                .size(300.dp),
                            onChange = {
                                amount = (it * 120).toInt()
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .fillMaxWidth()
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.set_to) + " "
                                + formatAsDuration(timeRemaining.toLong())
                                + " " + stringResource(R.string.end_of_song),
                        onClick = {
                            binder.startSleepTimer(timeRemaining.toLong())
                            isShowingSleepTimerDialog = false
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    IconButton(
                        onClick = { showCircularSlider = !showCircularSlider },
                        icon = R.drawable.time,
                        color = colorPalette().text
                    )
                    IconButton(
                        onClick = { isShowingSleepTimerDialog = false },
                        icon = R.drawable.close,
                        color = colorPalette().text
                    )
                    IconButton(
                        enabled = amount > 0,
                        onClick = {
                            binder.startSleepTimer(amount * 5 * 60 * 1000L)
                            isShowingSleepTimerDialog = false
                        },
                        icon = R.drawable.checkmark,
                        color = colorPalette().accent
                    )
                }
            }
        }
    }

    val color = colorPalette()
    var dynamicColorPalette by remember { mutableStateOf(color) }
    var dominant by remember { mutableStateOf(0) }
    var vibrant by remember { mutableStateOf(0) }
    var lightVibrant by remember { mutableStateOf(0) }
    var darkVibrant by remember { mutableStateOf(0) }
    var muted by remember { mutableStateOf(0) }
    var lightMuted by remember { mutableStateOf(0) }
    var darkMuted by remember { mutableStateOf(0) }


    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

    @Composable
    fun saturate(color: Int): Color {
        val colorHSL by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
        val lightTheme =
            colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
        colorToHSL(color, colorHSL)
        colorHSL[1] =
            (colorHSL[1] + if (lightTheme || colorHSL[1] < 0.1f) 0f else 0.35f).coerceIn(0f, 1f)
        colorHSL[2] = if (lightTheme) {
            colorHSL[2].coerceIn(0.5f, 1f)
        } else colorHSL[2]
        return Color.hsl(colorHSL[0], colorHSL[1], colorHSL[2])
    }

    var lightTheme =
        colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
    var ratio = if (lightTheme) 1f else 0.5f

    fun Color.darkenBy(): Color {
        return copy(
            red = red * ratio,
            green = green * ratio,
            blue = blue * ratio,
            alpha = alpha
        )
    }

    val playerBackgroundColors by rememberObservedPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )
    val animatedGradient by rememberObservedPreference(
        animatedGradientKey,
        AnimatedGradient.Linear
    )
    val isGradientBackgroundEnabled =
        playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient ||
                playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
                playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient

    LaunchedEffect(mediaItem.mediaId, updateBrush) {
        if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
            playerBackgroundColors == PlayerBackgroundColors.CoverColor ||
            playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient || updateBrush
        ) {
            try {
                val bitmap = getBitmapFromUrl(
                    context,
                    binder.player.currentWindow?.mediaItem?.mediaMetadata?.artworkUri.thumbnail(1200)
                        .toString()
                )

                dynamicColorPalette = dynamicColorPaletteOf(
                    bitmap,
                    !lightTheme
                ) ?: color

                val palette = Palette.from(bitmap).generate()

                dominant = palette.getDominantColor(dynamicColorPalette.accent.toArgb())
                vibrant = palette.getVibrantColor(dynamicColorPalette.accent.toArgb())
                lightVibrant = palette.getLightVibrantColor(dynamicColorPalette.accent.toArgb())
                darkVibrant = palette.getDarkVibrantColor(dynamicColorPalette.accent.toArgb())
                muted = palette.getMutedColor(dynamicColorPalette.accent.toArgb())
                lightMuted = palette.getLightMutedColor(dynamicColorPalette.accent.toArgb())
                darkMuted = palette.getDarkMutedColor(dynamicColorPalette.accent.toArgb())

            } catch (e: Exception) {
                dynamicColorPalette = color
            }

        }
    }

    var sizeShader by remember { mutableStateOf(Size.Zero) }

    val shaderA = LinearGradientShader(
        Offset(sizeShader.width / 2f, 0f),
        Offset(sizeShader.width / 2f, sizeShader.height),
        listOf(
            dynamicColorPalette.background2,
            colorPalette().background2,
        ),
        listOf(0f, 1f)
    )

    val shaderB = LinearGradientShader(
        Offset(sizeShader.width / 2f, 0f),
        Offset(sizeShader.width / 2f, sizeShader.height),
        listOf(
            colorPalette().background1,
            dynamicColorPalette.accent,
        ),
        listOf(0f, 1f)
    )

    val shaderMask = LinearGradientShader(
        Offset(sizeShader.width / 2f, 0f),
        Offset(sizeShader.width / 2f, sizeShader.height),
        listOf(
            //Color.White,
            colorPalette().background2,
            Color.Transparent,
        ),
        listOf(0f, 1f)
    )

    val brushA by animateBrushRotation(shaderA, sizeShader, 20_000, true)
    val brushB by animateBrushRotation(shaderB, sizeShader, 12_000, false)
    val brushMask by animateBrushRotation(shaderMask, sizeShader, 15_000, true)
    /*  */

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(
                mediaItem.mediaMetadata.artworkUri.thumbnail(1200)
            )
            .size(coil.size.Size.ORIGINAL)
            .transformations(
                listOf(
                    if (showthumbnail) {
                        BlurTransformation(
                            scale = 0.5f,
                            radius = blurStrength.toInt(),
                            //darkenFactor = blurDarkenFactor
                        )

                    } else
                        BlurTransformation(
                            scale = 0.5f,
                            //radius = blurStrength2.toInt(),
                            radius = if ((isShowingLyrics && !isShowingVisualizer) || !noblur) blurStrength.toInt() else 0,
                            //darkenFactor = blurDarkenFactor
                        )
                )
            )
            .build()
    )


    var totalPlayTimes = 0L
    mediaItems.forEach {
        totalPlayTimes += it.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
            durationTextToMillis(it1)
        } ?: 0
    }



//    var isShowingStatsForNerds by remember {
//        mutableStateOf(false)
//    }

    val thumbnailTapEnabled by rememberObservedPreference(thumbnailTapEnabledKey, true)
    val showNextSongsInPlayer by rememberObservedPreference(showNextSongsInPlayerKey, false)

    var showQueue by remember { mutableStateOf(false) }
    var showSearchEntity by remember { mutableStateOf(false) }

    val transparentBackgroundActionBarPlayer by rememberPreference(
        transparentBackgroundPlayerActionBarKey,
        false
    )
    val showTopActionsBar by rememberObservedPreference(showTopActionsBarKey, true)

    var containerModifier = Modifier
        //.padding(bottom = bottomDp)
        .padding(bottom = 0.dp)
    var deltaX by remember { mutableStateOf(0f) }
    val blackgradient by rememberObservedPreference(blackgradientKey, false)
    val bottomgradient by rememberObservedPreference(bottomgradientKey, false)
    val disableScrollingText by rememberObservedPreference(disableScrollingTextKey, false)

    var discoverIsEnabled by rememberObservedPreference(discoverKey, false)
    val titleExpanded by rememberObservedPreference(titleExpandedKey, true)
    val timelineExpanded by rememberObservedPreference(timelineExpandedKey, true)
    val controlsExpanded by rememberObservedPreference(controlsExpandedKey, true)

    val showCoverThumbnailAnimation by rememberObservedPreference(showCoverThumbnailAnimationKey, false)
    var coverThumbnailAnimation by rememberObservedPreference(
        coverThumbnailAnimationKey,
        ThumbnailCoverType.Vinyl
    )

    var valueGrad by remember { mutableStateOf(2) }
    val gradients = enumValues<AnimatedGradient>()
    var tempGradient by remember { mutableStateOf(AnimatedGradient.Linear) }
    var albumCoverRotation by rememberObservedPreference(albumCoverRotationKey, false)
    var circleOffsetY by remember { mutableStateOf(0f) }

    @Composable
    fun Modifier.conditional(
        condition: Boolean,
        modifier: @Composable Modifier.() -> Modifier
    ): Modifier {
        return if (condition) {
            then(modifier(Modifier))
        } else {
            this
        }
    }

    // This hide androidview when media is not video
    fun Modifier.hidePlayer(): Modifier {
        return this.size(0.dp)
    }

    if (animatedGradient == AnimatedGradient.Random) {
        LaunchedEffect(mediaItem.mediaId) {
            valueGrad = (2..14).random()
        }
        tempGradient = gradients[valueGrad]
    }


    if (playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey) {
        containerModifier = containerModifier
            .background(dynamicColorPalette.accent.copy(0.8f).compositeOver(Color.Black))
    } else if (!isGradientBackgroundEnabled) {
        if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor && (playerType == PlayerType.Essential || (showthumbnail && (!albumCoverRotation)))) {
            containerModifier = containerModifier
                .background(dynamicColorPalette.background1)
                .paint(
                    painter = painter,
                    contentScale = ContentScale.Crop,
                    sizeToIntrinsics = false
                )
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to if (bottomgradient) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(
                            if (isLandscape) 0.8f else 0.75f
                        ) else Color.Black.copy(if (isLandscape) 0.8f else 0.75f) else Color.Transparent,
                        startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                        endY = POSITIVE_INFINITY
                    )
                )
                .background(
                    if (bottomgradient) if (isLandscape) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(
                        0.25f
                    ) else Color.Black.copy(0.25f) else Color.Transparent else Color.Transparent
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (thumbnailTapEnabled && !showthumbnail) {
                            if (isShowingVisualizer) isShowingVisualizer = false
                            isShowingLyrics = !isShowingLyrics
                        }
                    },
                    onDoubleClick = {
                        if (!showlyricsthumbnail && !showvisthumbnail)
                            showthumbnail = !showthumbnail
                    },
                    onLongClick = {
                        if (showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur)
                            showBlurPlayerDialog = true
                    }
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            deltaX = dragAmount
                        },
                        onDragStart = {
                            //Log.d("mediaItemGesture","ondragStart offset ${it}")
                        },
                        onDragEnd = {
                            if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                if (deltaX > 5) {
                                    binder.player.playPrevious()
                                    //Log.d("mediaItem","Swipe to LEFT")
                                } else if (deltaX < -5) {
                                    binder.player.playNext()
                                    //Log.d("mediaItem","Swipe to RIGHT")
                                }

                            }

                        }

                    )
                }

        } else if (playerBackgroundColors == PlayerBackgroundColors.ColorPalette) {
            containerModifier = containerModifier
                .drawBehind {
                    val colors = listOf(
                        Color(dominant),
                        Color(vibrant),
                        Color(lightVibrant),
                        Color(darkVibrant),
                        Color(muted),
                        Color(lightMuted),
                        Color(darkMuted)
                    )
                    val boxheight = (size.height) / 7
                    colors.forEachIndexed { i, color ->
                        drawRect(
                            color = colors[i],
                            topLeft = Offset(0f, i * boxheight),
                            size = Size(size.width, boxheight)
                        )
                    }
                }
        } else if (playerBackgroundColors == PlayerBackgroundColors.CoverColor) {
            containerModifier = containerModifier
                .background(dynamicColorPalette.background1)
        } else if (playerBackgroundColors == PlayerBackgroundColors.ThemeColor) {
            containerModifier = containerModifier
                .background(color.background1)
        }
    } else {
        when (playerBackgroundColors) {

            PlayerBackgroundColors.AnimatedGradient -> {
                if (animatedGradient == AnimatedGradient.FluidCoverColorGradient ||
                    animatedGradient == AnimatedGradient.FluidThemeColorGradient
                ) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .drawBehind {
                            drawRect(brush = brushA)
                            drawRect(brush = brushMask, blendMode = BlendMode.DstOut)
                            drawRect(brush = brushB, blendMode = BlendMode.DstAtop)
                        }
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[2]) || animatedGradient == AnimatedGradient.Linear) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .animatedGradient(
                            binder.player.isPlaying,
                            saturate(dominant).darkenBy(),
                            saturate(vibrant).darkenBy(),
                            saturate(lightVibrant).darkenBy(),
                            saturate(darkVibrant).darkenBy(),
                            saturate(muted).darkenBy(),
                            saturate(lightMuted).darkenBy(),
                            saturate(darkMuted).darkenBy()
                        )
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[3]) || animatedGradient == AnimatedGradient.Mesh) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .conditional(!appRunningInBackground) {
                            shaderBackground(
                                MeshGradient(
                                    arrayOf(
                                        saturate(vibrant).darkenBy(),
                                        saturate(lightVibrant).darkenBy(),
                                        saturate(darkVibrant).darkenBy(),
                                        saturate(muted).darkenBy(),
                                        saturate(lightMuted).darkenBy(),
                                        saturate(darkMuted).darkenBy(),
                                        saturate(dominant).darkenBy()
                                    ),
                                    scale = 1f
                                )
                            )
                        }
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[4]) || animatedGradient == AnimatedGradient.MesmerizingLens) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            MesmerizingLens
                        )
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[5]) || animatedGradient == AnimatedGradient.GlossyGradients) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            GlossyGradients
                        )
                        .background(if (!lightTheme) Color.Black.copy(0.2f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[6]) || animatedGradient == AnimatedGradient.GradientFlow) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            GradientFlow
                        )
                        .background(if (!lightTheme) Color.Black.copy(0.2f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[7]) || animatedGradient == AnimatedGradient.PurpleLiquid) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            PurpleLiquid
                        )
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[8]) || animatedGradient == AnimatedGradient.InkFlow) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            InkFlow
                        )
                        .background(if (lightTheme) Color.White.copy(0.4f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[9]) || animatedGradient == AnimatedGradient.OilFlow) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            OilFlow
                        )
                        .background(if (lightTheme) Color.White.copy(0.4f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[10]) || animatedGradient == AnimatedGradient.IceReflection) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            IceReflection
                        )
                        .background(if (!lightTheme) Color.Black.copy(0.3f) else Color.Transparent)
                        .background(if (lightTheme) Color.White.copy(0.4f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[11]) || animatedGradient == AnimatedGradient.Stage) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            Stage
                        )
                        .background(if (!lightTheme) Color.Black.copy(0.3f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[12]) || animatedGradient == AnimatedGradient.GoldenMagma) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            GoldenMagma
                        )
                        .background(if (!lightTheme) Color.Black.copy(0.2f) else Color.Transparent)
                        .background(if (lightTheme) Color.White.copy(0.3f) else Color.Transparent)
                } else if ((animatedGradient == AnimatedGradient.Random && tempGradient == gradients[13]) || animatedGradient == AnimatedGradient.BlackCherryCosmos) {
                    containerModifier = containerModifier
                        .onSizeChanged {
                            sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .shaderBackground(
                            BlackCherryCosmos
                        )
                        .background(if (lightTheme) Color.White.copy(0.35f) else Color.Transparent)
                }
            }

            else -> {
                containerModifier = containerModifier
                    .background(
                        Brush.verticalGradient(
                            0.5f to if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) dynamicColorPalette.background1 else colorPalette().background1,
                            1.0f to if (blackgradient) Color.Black
                            else if ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) && transparentBackgroundActionBarPlayer) dynamicColorPalette.background2
                            else colorPalette().background2,
                            //0.0f to colorPalette().background0,
                            //1.0f to colorPalette().background2,
                            startY = 0.0f,
                            endY = 1500.0f
                        )
                    )

            }
        }

    }

    /***** NEW PLAYER *****/

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    var updateStatisticsEverySeconds by remember { mutableIntStateOf(0) }
    val steps by remember { mutableIntStateOf(5) }
    var stepToUpdateStats by remember { mutableIntStateOf(1) }

    val isLandscape = isLandscape

    LaunchedEffect(mediaItem) {
        positionAndDuration = 0f to 0f

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
        timeRemaining = positionAndDuration.second.toInt() - positionAndDuration.first.toInt()

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

    LaunchedEffect(playerState.value) {

        shouldBePlaying = playerState.value == PlayerConstants.PlayerState.PLAYING

        if (playerState.value == PlayerConstants.PlayerState.ENDED) {
            // TODO Implement repeat mode in queue
            if (getQueueLoopType() != QueueLoopType.Default)
                player.value?.seekTo(0f)

            if (binder.player.hasNextMediaItem())
                binder.player.playNext()

        }

        linkServiceClientSend(
            if (shouldBePlaying) mediaItem.mediaId.toCommandPlay() else LINKWEB_COMMAND_PAUSE.toCommand(),
            castToLinkDevice,
            linkDevicesSelected
        )

    }

    val thumbnailRoundness by rememberObservedPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
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
            mediaItem = mediaItem,
            onPlay = {
                player.value?.play()
                //println("LinkClient OnLinePlayer Controls play")
                linkServiceClientSend(
                    mediaItem.mediaId.toCommandPlayAt(currentSecond.toInt()),
                    castToLinkDevice,
                    linkDevicesSelected
                )

            },
            onPause = {
                player.value?.pause()

                        linkServiceClientSend(
                            LINKWEB_COMMAND_PAUSE.toCommand(),
                            castToLinkDevice,
                            linkDevicesSelected
                        )

            },
            onSeekTo = { player.value?.seekTo(it) },
            onNext = { binder.player.playNext() },
            onPrevious = { binder.player.playPrevious() },
            playerState = playerState.value,
        )
    }



    /***** NEW PLAYER *****/

    val thumbnailContent: @Composable (
        modifier: Modifier,
    ) -> Unit = { innerModifier ->

            Box(
                modifier = innerModifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                AnimatedVisibility(
                    modifier = Modifier
                        .zIndex(1f)
                        .align(Alignment.Center),
                    visible = castToLinkDevice,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .background(colorPalette().background0)
                            .fillMaxSize()
                    ) {

                        LazyColumn(
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(all = 10.dp),
                            modifier = Modifier
                                .background(
                                    colorPalette().background0
                                )
                                .fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "Link Devices",
                                    color = colorPalette().text,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )


                            }
                            items(
                                items = linkDevices.distinct()
                            ) { device ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .height(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        icon = if (linkDevicesSelected.contains(device.toLinkDevice())) R.drawable.cast_connected else R.drawable.cast_disconnected,
                                        color = colorPalette().text,
                                        enabled = true,
                                        onClick = {
                                            if (linkDevicesSelected.contains(device.toLinkDevice()))
                                                linkDevicesSelected.remove(device.toLinkDevice())
                                            else
                                                linkDevicesSelected.add(device.toLinkDevice())

                                            linkDevicesSavedProvider.value.saveDevices(linkDevicesSelected)

                                            //println("LinkClient OnLinePlayer Controls cast selected -${device.toLinkDevice()}- devices after ${linkDevicesSelected}")
                                            player.value?.pause()
                                        },
                                        modifier = Modifier
                                            .size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = device.serviceName.toString(),
                                        color = colorPalette().text,
                                        modifier = Modifier.border(BorderStroke(1.dp, Color.Red))
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth().height(1.dp).align(Alignment.BottomCenter),
                        )

                    }
                }
                AnimatedVisibility(
                    modifier = Modifier
                        .zIndex(1f)
                        .align(Alignment.Center),
                    visible = showControls && it.fast4x.riplay.utils.isLandscape && !castToLinkDevice,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = .4f), thumbnailRoundness.shape())
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                    ) {
                        controlsContent(Modifier.padding(top = 20.dp))
                    }
                }

                onlineCore()

            }

    }


    val textoutline by rememberObservedPreference(textoutlineKey, false)

    var songPlaylist by remember {
        mutableStateOf(0)
    }
    LaunchedEffect(Unit, mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            songPlaylist = Database.songUsedInPlaylists(mediaItem.mediaId)
        }
    }
    val playlistindicator by rememberObservedPreference(playlistindicatorKey, false)
    val carousel by rememberObservedPreference(carouselKey, true)
    val carouselSize by rememberObservedPreference(carouselSizeKey, CarouselSize.Biggest)

    var showButtonPlayerDiscover by rememberObservedPreference(showButtonPlayerDiscoverKey, false)
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val actionsBarContent: @Composable () -> Unit = {
            if ((!showButtonPlayerDownload &&
                        !showButtonPlayerAddToPlaylist &&
                        !showButtonPlayerLoop &&
                        !showButtonPlayerShuffle &&
                        !showButtonPlayerLyrics &&
                        !showButtonPlayerSleepTimer &&
                        !showButtonPlayerSystemEqualizer &&
                        !showButtonPlayerArrow &&
                        !showButtonPlayerMenu &&
                        !showButtonPlayerStartRadio &&
                        !expandedplayertoggle &&
                        !showButtonPlayerDiscover &&
                        !showButtonPlayerVideo) ||
                        (!showlyricsthumbnail && isShowingLyrics && !actionExpanded) ||
                        (mediaItem.isVideo && it.fast4x.riplay.utils.isLandscape)
            ) {
                Row {}
            } else
                Row(
                    modifier = Modifier
                        .align(if (isLandscape) Alignment.BottomEnd else Alignment.BottomCenter)
                        .requiredHeight(if (showNextSongsInPlayer && (showlyricsthumbnail || (!isShowingLyrics || miniQueueExpanded))) 90.dp else 50.dp)
                        .fillMaxWidth(if (isLandscape) 0.8f else 1f)
                        .conditional(tapqueue) { clickable { showQueue = true } }
                        .background(
                            colorPalette().background2.copy(
                                alpha = if ((transparentBackgroundActionBarPlayer) || ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient)) && blackgradient) 0.0f else 0.7f // 0.0 > 0.1
                            )
                        )
                        .pointerInput(Unit) {
                            if (swipeUpQueue)
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        if (dragAmount < 0) showQueue = true
                                    }
                                )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (showNextSongsInPlayer) {
                            if (showlyricsthumbnail || !isShowingLyrics || miniQueueExpanded) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        //.background(colorPalette().background2.copy(alpha = 0.3f))
                                        .background(
                                            colorPalette().background2.copy(
                                                alpha = if (transparentBackgroundActionBarPlayer) 0.0f else 0.3f
                                            )
                                        )
                                        .padding(horizontal = 12.dp)
                                        .fillMaxWidth()
                                ) {
                                    val nextMediaItemIndex = binder.player.nextMediaItemIndex
                                    val pagerStateQueue =
                                        rememberPagerState(pageCount = { mediaItems.size })
                                    val scope = rememberCoroutineScope()
                                    val fling = PagerDefaults.flingBehavior(
                                        state = pagerStateQueue,
                                        snapPositionalThreshold = 0.15f,
                                        pagerSnapDistance = PagerSnapDistance.atMost(showsongs.number)
                                    )
                                    pagerStateQueue.LaunchedEffectScrollToPage(binder.player.currentMediaItemIndex + 1)

                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 7.5.dp)
                                            .weight(0.07f)
                                            .conditional(pagerStateQueue.currentPage == binder.player.currentMediaItemIndex) {
                                                padding(
                                                    horizontal = 3.dp
                                                )
                                            }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (pagerStateQueue.currentPage > binder.player.currentMediaItemIndex) R.drawable.chevron_forward
                                                else if (pagerStateQueue.currentPage == binder.player.currentMediaItemIndex) R.drawable.play
                                                else R.drawable.chevron_back
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(25.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    indication = ripple(bounded = false),
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    onClick = {
                                                        scope.launch {
                                                            if (!appRunningInBackground) {
                                                                pagerStateQueue.animateScrollToPage(
                                                                    binder.player.currentMediaItemIndex + 1
                                                                )
                                                            } else {
                                                                pagerStateQueue.scrollToPage(binder.player.currentMediaItemIndex + 1)
                                                            }
                                                        }
                                                    }
                                                ),
                                            tint = colorPalette().accent
                                        )
                                    }

                                    val threePagesPerViewport = object : PageSize {
                                        override fun Density.calculateMainAxisPageSize(
                                            availableSpace: Int,
                                            pageSpacing: Int
                                        ): Int {
                                            return if (showsongs == SongsNumber.`1`) availableSpace else ((availableSpace - 2 * pageSpacing) / (showsongs.number))
                                        }
                                    }

                                    HorizontalPager(
                                        state = pagerStateQueue,
                                        pageSize = threePagesPerViewport,
                                        pageSpacing = 10.dp,
                                        flingBehavior = fling,
                                        modifier = Modifier.weight(1f)
                                    ) { index ->
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        binder.player.playAtIndex(index)
                                                    },
                                                    onLongClick = {
                                                        if (index < mediaItems.size) {
                                                            binder.player.addNext(
                                                                binder.player.getMediaItemAt(index),
                                                                queue = selectedQueue ?: defaultQueue()
                                                            )
                                                            scope.launch {
                                                                if (!appRunningInBackground) {
                                                                    pagerStateQueue.animateScrollToPage(
                                                                        binder.player.currentMediaItemIndex + 1
                                                                    )
                                                                } else {
                                                                    pagerStateQueue.scrollToPage(
                                                                        binder.player.currentMediaItemIndex + 1
                                                                    )
                                                                }
                                                            }
                                                            SmartMessage(
                                                                context.resources.getString(R.string.addednext),
                                                                type = PopupType.Info,
                                                                context = context
                                                            )
//                                                        hapticFeedback.performHapticFeedback(
//                                                            HapticFeedbackType.LongPress
//                                                        )
                                                        }
                                                    }
                                                )
                                            //.width(IntrinsicSize.Min)
                                        ) {
                                            if (showalbumcover) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterVertically)
                                                ) {
                                                    AsyncImage(
                                                        model = binder.player.getMediaItemAt(
                                                            index
                                                            //if (it + 1 <= mediaItems.size - 1) it + 1 else it
                                                        ).mediaMetadata.artworkUri.thumbnail(1200),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .padding(end = 5.dp)
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .size(30.dp)
                                                    )
                                                }
                                            }
                                            Column(
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .height(40.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                Box(

                                                ) {
                                                    BasicText(
                                                        text = cleanPrefix(
                                                            binder.player.getMediaItemAt(
                                                                index
                                                                //if (it + 1 <= mediaItems.size - 1) it + 1 else it
                                                            ).mediaMetadata.title?.toString()
                                                                ?: ""
                                                        ),
                                                        style = TextStyle(
                                                            color = colorPalette().text,
                                                            fontSize = typography().xxxs.semiBold.fontSize,
                                                        ),
                                                        maxLines = 1,
                                                        //overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                                    )
                                                    BasicText(
                                                        text = cleanPrefix(
                                                            binder.player.getMediaItemAt(
                                                                index
                                                                //if (it + 1 <= mediaItems.size - 1) it + 1 else it
                                                            ).mediaMetadata.title?.toString()
                                                                ?: ""
                                                        ),
                                                        style = TextStyle(
                                                            drawStyle = Stroke(
                                                                width = 0.25f,
                                                                join = StrokeJoin.Round
                                                            ),
                                                            color = if (!textoutline) Color.Transparent
                                                            else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(
                                                                0.65f
                                                            )
                                                            else Color.Black,
                                                            fontSize = typography().xxxs.semiBold.fontSize,
                                                        ),
                                                        maxLines = 1,
                                                        //overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                                    )
                                                }

                                                Box(

                                                ) {
                                                    BasicText(
                                                        text = binder.player.getMediaItemAt(
                                                            index
                                                            //if (it + 1 <= mediaItems.size - 1) it + 1 else it
                                                        ).mediaMetadata.artist?.toString()
                                                            ?: "",
                                                        style = TextStyle(
                                                            color = colorPalette().text,
                                                            fontSize = typography().xxxs.semiBold.fontSize,
                                                        ),
                                                        maxLines = 1,
                                                        //overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                                    )
                                                    BasicText(
                                                        text = binder.player.getMediaItemAt(
                                                            index
                                                            //if (it + 1 <= mediaItems.size - 1) it + 1 else it
                                                        ).mediaMetadata.artist?.toString()
                                                            ?: "",
                                                        style = TextStyle(
                                                            drawStyle = Stroke(
                                                                width = 0.25f,
                                                                join = StrokeJoin.Round
                                                            ),
                                                            color = if (!textoutline) Color.Transparent
                                                            else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(
                                                                0.65f
                                                            )
                                                            else Color.Black,
                                                            fontSize = typography().xxxs.semiBold.fontSize,
                                                        ),
                                                        maxLines = 1,
                                                        //overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (showsongs == SongsNumber.`1`) {
                                        IconButton(
                                            icon = R.drawable.trash,
                                            color = Color.White,
                                            enabled = true,
                                            onClick = {
                                                binder.player.removeMediaItem(nextMediaItemIndex)
                                            },
                                            modifier = Modifier
                                                .weight(0.07f)
                                                .size(40.dp)
                                                .padding(vertical = 7.5.dp),
                                        )
                                    }

                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (actionspacedevenly) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                        ) {
                            // TODO Implement RiPlay Connect
                            IconButton(
                                icon = if (castToLinkDevice) R.drawable.cast_connected else R.drawable.cast_disconnected,
                                color = colorPalette().accent,
                                enabled = true,
                                onClick = {
                                    castToLinkDevice = !castToLinkDevice
                                    player.value?.pause()
                                    if (castToLinkDevice) player.value?.mute() else player.value?.unMute()
                                    if (!castToLinkDevice) linkServiceClientSend(
                                        LINKWEB_COMMAND_PAUSE.toCommand(),
                                        true,
                                        linkDevicesSelected
                                    )
                                    //println("CastToLinkDevice: $castToLinkDevice")
                                },
                                modifier = Modifier
                                    .size(24.dp),
                            )

                            if (showButtonPlayerVideo)
                                IconButton(
                                    icon = R.drawable.video,
                                    color = colorPalette().accent,
                                    enabled = true,
                                    onClick = {
                                        binder.callPause {}
                                        showSearchEntity = true
                                    },
                                    modifier = Modifier
                                        .size(24.dp),
                                )

                            if (showButtonPlayerDiscover)
                                IconButton(
                                    icon = R.drawable.star_brilliant,
                                    color = if (discoverIsEnabled) colorPalette().text else colorPalette().textDisabled,
                                    onClick = {},
                                    modifier = Modifier
                                        .size(24.dp)
                                        .combinedClickable(
                                            onClick = { discoverIsEnabled = !discoverIsEnabled },
                                            onLongClick = {
                                                SmartMessage(
                                                    context.resources.getString(R.string.discoverinfo),
                                                    context = context
                                                )
                                            }

                                        )
                                )


                            if (showButtonPlayerAddToPlaylist)
                                IconButton(
                                    icon = R.drawable.add_in_playlist,
                                    color = if (songPlaylist > 0 && playlistindicator)
                                        if (colorPaletteName == ColorPaletteName.PureBlack) Color.Black else colorPalette().text
                                    else colorPalette().accent,
                                    onClick = {
                                        menuState.display {
                                            AddToPlaylistPlayerMenu(
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.hide()
                                                    Database.asyncTransaction {
                                                        songPlaylist =
                                                            songUsedInPlaylists(mediaItem.mediaId)
                                                    }
                                                },
                                                mediaItem = mediaItem,
                                                binder = binder,
                                                onClosePlayer = {
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        //.padding(horizontal = 4.dp)
                                        .size(24.dp)
                                        .conditional(songPlaylist > 0 && playlistindicator) {
                                            background(
                                                color.accent,
                                                CircleShape
                                            )
                                        }
                                        .conditional(songPlaylist > 0 && playlistindicator) {
                                            padding(
                                                all = 5.dp
                                            )
                                        }
                                )



                            if (showButtonPlayerLoop)
                                IconButton(
                                    icon = getIconQueueLoopState(queueLoopType),
                                    color = colorPalette().accent,
                                    onClick = {
                                        queueLoopType = setQueueLoopState(queueLoopType)
                                        if (effectRotationEnabled) isRotated = !isRotated
                                    },
                                    modifier = Modifier
                                        //.padding(horizontal = 4.dp)
                                        .size(24.dp)
                                )

                            if (showButtonPlayerShuffle)
                                IconButton(
                                    icon = R.drawable.shuffle,
                                    color = colorPalette().accent,
                                    enabled = true,
                                    onClick = {
                                        binder.player.shuffleQueue()
                                    },
                                    modifier = Modifier
                                        .size(24.dp),
                                )

                            if (showButtonPlayerLyrics)
                                IconButton(
                                    icon = R.drawable.song_lyrics,
                                    color = if (isShowingLyrics) colorPalette().accent else Color.Gray,
                                    enabled = true,
                                    onClick = {
                                        if (isShowingVisualizer) isShowingVisualizer =
                                            !isShowingVisualizer
                                        isShowingLyrics = !isShowingLyrics
                                    },
                                    modifier = Modifier
                                        .size(24.dp),
                                )
                            if (!isLandscape || ((playerType == PlayerType.Essential) && !showthumbnail))
                                if (expandedplayertoggle && !showlyricsthumbnail)
                                    IconButton(
                                        icon = R.drawable.minmax,
                                        color = if (expandedplayer) colorPalette().accent else Color.Gray,
                                        enabled = true,
                                        onClick = {
                                            expandedplayer = !expandedplayer
                                        },
                                        modifier = Modifier
                                            .size(20.dp),
                                    )


                            if (visualizerEnabled)
                                IconButton(
                                    icon = R.drawable.sound_effect,
                                    color = if (isShowingVisualizer) colorPalette().text else colorPalette().textDisabled,
                                    enabled = true,
                                    onClick = {
                                        if (isShowingLyrics) isShowingLyrics = !isShowingLyrics
                                        isShowingVisualizer = !isShowingVisualizer
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                )


                            if (showButtonPlayerSleepTimer)
                                if (sleepTimerMillisLeft == null) {
                                    IconButton(
                                        icon = R.drawable.sleep,
                                        color = Color.Gray,
                                        enabled = true,
                                        onClick = {
                                            isShowingSleepTimerDialog = true
                                        },
                                        modifier = Modifier
                                            .size(24.dp),
                                    )
                                } else {
                                    BasicText(
                                        text = formatAsDuration(sleepTimerMillisLeft!!),
                                        style = typography().l.semiBold,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                isShowingSleepTimerDialog = true
                                            })
                                    )
                                }

                            if (showButtonPlayerSystemEqualizer) {
                                val activityResultLauncher =
                                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

                                IconButton(
                                    icon = R.drawable.equalizer,
                                    color = colorPalette().accent,
                                    enabled = true,
                                    onClick = {
                                        try {
                                            activityResultLauncher.launch(
                                                Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                                    putExtra(
                                                        AudioEffect.EXTRA_AUDIO_SESSION,
                                                        binder.player.audioSessionId
                                                    )
                                                    putExtra(
                                                        AudioEffect.EXTRA_PACKAGE_NAME,
                                                        context.packageName
                                                    )
                                                    putExtra(
                                                        AudioEffect.EXTRA_CONTENT_TYPE,
                                                        AudioEffect.CONTENT_TYPE_MUSIC
                                                    )
                                                }
                                            )
                                        } catch (e: ActivityNotFoundException) {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_not_find_application_audio),
                                                type = PopupType.Warning, context = context
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(20.dp),
                                )
                            }

                            if (showButtonPlayerStartRadio)
                                IconButton(
                                    icon = R.drawable.radio,
                                    color = colorPalette().accent,
                                    enabled = true,
                                    onClick = {
                                        binder.stopRadio()
                                        binder.player.seamlessPlay(mediaItem)
                                        binder.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    },
                                    modifier = Modifier
                                        .size(24.dp),
                                )

                            if (showButtonPlayerArrow)
                                IconButton(
                                    icon = R.drawable.chevron_up,
                                    color = colorPalette().accent,
                                    enabled = true,
                                    onClick = {
                                        showQueue = true
                                    },
                                    modifier = Modifier
                                        //.padding(end = 12.dp)
                                        .size(24.dp),
                                )

                            if (showButtonPlayerMenu && !isLandscape)
                                IconButton(
                                    icon = R.drawable.ellipsis_vertical,
                                    color = colorPalette().accent,
                                    onClick = {
                                        menuState.display {
                                            PlayerMenu(
                                                navController = navController,
                                                onDismiss = menuState::hide,
                                                mediaItem = mediaItem,
                                                binder = binder,
                                                onClosePlayer = {
                                                    onDismiss()
                                                },
                                                onInfo = {
                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${mediaItem.mediaId}")
                                                },
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        //.padding(end = 12.dp)
                                        .size(24.dp)
                                )


                            if (isLandscape) {
                                IconButton(
                                    icon = R.drawable.ellipsis_horizontal,
                                    color = colorPalette().accent,
                                    onClick = {
                                        menuState.display {
                                            PlayerMenu(
                                                navController = navController,
                                                onDismiss = menuState::hide,
                                                mediaItem = mediaItem,
                                                binder = binder,
                                                onClosePlayer = {
                                                    onDismiss()
                                                },
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                        }


                    }
                }
        }

        val binderPlayer = binder.player ?: return
        val clickLyricsText by rememberObservedPreference(clickOnLyricsTextKey, true)
        var extraspace by rememberObservedPreference(extraspaceKey, false)

        val nextmedia = if (binder.player.mediaItemCount > 1
            && binder.player.currentMediaItemIndex + 1 < binder.player.mediaItemCount
        )
            binder.player.getMediaItemAt(binder.player.currentMediaItemIndex + 1) else MediaItem.EMPTY

        var songPlaylist1 by remember {
            mutableStateOf(0)
        }
        LaunchedEffect(Unit, nextmedia.mediaId) {
            withContext(Dispatchers.IO) {
                songPlaylist1 = Database.songUsedInPlaylists(nextmedia.mediaId)
            }
        }

        var songLiked by remember {
            mutableStateOf(0)
        }

        LaunchedEffect(Unit, nextmedia.mediaId) {
            withContext(Dispatchers.IO) {
                songLiked = Database.songliked(nextmedia.mediaId)
            }
        }


        val thumbnailType by rememberObservedPreference(thumbnailTypeKey, ThumbnailType.Modern)
        val statsfornerds by rememberObservedPreference(statsfornerdsKey, false)
        val topPadding by rememberObservedPreference(topPaddingKey, true)
        var swipeAnimationNoThumbnail by rememberObservedPreference(
            swipeAnimationsNoThumbnailKey,
            SwipeAnimationNoThumbnail.Sliding
        )

        if (isLandscape && !mediaItem.isVideo) {
            Box(
                modifier = Modifier
                    .conditional(queueType == QueueType.Modern) {
                        haze(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = if (lightTheme) Color.White.copy(0.5f) else Color.Black.copy(
                                    0.5f
                                ),
                                blurRadius = 8.dp
                            )
                        )
                    }
            ) {
                if ((playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor && playerType == PlayerType.Modern && (!showthumbnail || albumCoverRotation)) || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) {
                    val fling = PagerDefaults.flingBehavior(
                        state = pagerStateFS,
                        snapPositionalThreshold = 0.20f
                    )
                    pagerStateFS.LaunchedEffectScrollToPage(binder.player.currentMediaItemIndex)

                    if (!showQueue) {
                        LaunchedEffect(pagerStateFS) {
                            var previousPage = pagerStateFS.settledPage
                            snapshotFlow { pagerStateFS.settledPage }.distinctUntilChanged()
                                .collect {
                                    if (previousPage != it) {
                                        if (it != binder.player.currentMediaItemIndex) binder.player.playAtIndex(
                                            it
                                        )
                                    }
                                    previousPage = it
                                }
                        }
                    }

                    HorizontalPager(
                        state = pagerStateFS,
                        beyondViewportPageCount = 1,
                        flingBehavior = fling,
                        userScrollEnabled = !((albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) && (isShowingLyrics || showthumbnail)),
                        modifier = Modifier
                    ) { it ->

                        var currentRotation by remember {
                            mutableFloatStateOf(0f)
                        }

                        val rotation = remember {
                            Animatable(currentRotation)
                        }

                        LaunchedEffect(binderPlayer.isPlaying, pagerStateFS.settledPage) {
                            if (binderPlayer.isPlaying && it == pagerStateFS.settledPage) {
                                rotation.animateTo(
                                    targetValue = currentRotation + 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(30000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                ) {
                                    currentRotation = value
                                }
                            } else {
                                if (currentRotation > 0f && it == pagerStateFS.settledPage) {
                                    rotation.animateTo(
                                        targetValue = currentRotation + 10,
                                        animationSpec = tween(
                                            1250,
                                            easing = LinearOutSlowInEasing
                                        )
                                    ) {
                                        currentRotation = value
                                    }
                                }
                            }
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(
                                    binder.player.getMediaItemAt(it).mediaMetadata.artworkUri.thumbnail(
                                        1200
                                    )
                                )
                                .transformations(
                                    listOf(
                                        if (showthumbnail) {
                                            BlurTransformation(
                                                scale = 0.5f,
                                                radius = blurStrength.toInt(),
                                                //darkenFactor = blurDarkenFactor
                                            )
                                        } else
                                            BlurTransformation(
                                                scale = 0.5f,
                                                //radius = blurStrength2.toInt(),
                                                radius = if ((isShowingLyrics && !isShowingVisualizer) || !noblur) blurStrength.toInt() else 0,
                                                //darkenFactor = blurDarkenFactor
                                            )
                                    )
                                )
                                .build(),
                            contentDescription = "",
                            contentScale = if ((albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) && (isShowingLyrics || showthumbnail)) ContentScale.Fit else ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (it == pagerStateFS.currentPage) 1f else 0.9f)
                                .conditional(albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) {
                                    graphicsLayer {
                                        scaleX =
                                            if (isShowingLyrics || showthumbnail) (screenWidth / screenHeight) + 0.5f else 1f
                                        scaleY =
                                            if (isShowingLyrics || showthumbnail) (screenWidth / screenHeight) + 0.5f else 1f
                                        rotationZ =
                                            if ((it == pagerStateFS.settledPage) && (isShowingLyrics || showthumbnail)) rotation.value else 0f
                                    }
                                }
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (thumbnailTapEnabled && !showthumbnail) {
                                            if (isShowingVisualizer) isShowingVisualizer = false
                                            isShowingLyrics = !isShowingLyrics
                                        }
                                    },
                                    onDoubleClick = {
                                        if (!showlyricsthumbnail && !showvisthumbnail)
                                            showthumbnail = !showthumbnail
                                    },
                                    onLongClick = {
                                        if (showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur)
                                            showBlurPlayerDialog = true
                                    }
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    1.0f to if (bottomgradient) if (lightTheme) Color.White.copy(
                                        if (isLandscape) 0.8f else 0.75f
                                    ) else Color.Black.copy(if (isLandscape) 0.8f else 0.75f) else Color.Transparent,
                                    startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                                    endY = POSITIVE_INFINITY
                                )
                            )
                            .background(
                                if (bottomgradient) if (isLandscape) if (lightTheme) Color.White.copy(
                                    0.25f
                                ) else Color.Black.copy(0.25f) else Color.Transparent else Color.Transparent
                            )
                    ) {}
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = containerModifier
                        .padding(top = if (playerType == PlayerType.Essential) 40.dp else 20.dp)
                        .padding(top = if (extraspace) 10.dp else 0.dp)
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
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .animateContentSize()
                        // .border(BorderStroke(1.dp, Color.Blue))
                    ) {

                        if (isShowingVisualizer && !showvisthumbnail && playerType == PlayerType.Essential) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, dragAmount ->
                                                deltaX = dragAmount
                                            },
                                            onDragStart = {
                                            },
                                            onDragEnd = {
                                                if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                                    if (deltaX > 5) {
                                                        binder.player.playPrevious()
                                                    } else if (deltaX < -5) {
                                                        binder.player.playNext()
                                                    }

                                                }

                                            }

                                        )
                                    }
                            ) {
                                NextVisualizer(
                                    isDisplayed = isShowingVisualizer
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .navigationBarsPadding()
                        ) {
                            if (!showlyricsthumbnail) {
                                Lyrics(
                                    mediaId = mediaItem.mediaId,
                                    isDisplayed = isShowingLyrics,
                                    onDismiss = {
                                        isShowingLyrics = false
                                    },
                                    ensureSongInserted = { Database.insert(mediaItem) },
                                    size = 1000.dp,
                                    mediaMetadataProvider = mediaItem::mediaMetadata,
                                    durationProvider = { positionAndDuration.second.toLong() * 1000 },
                                    positionProvider = { positionAndDuration.first.toLong() * 1000 },
                                    isLandscape = isLandscape,
                                    clickLyricsText = clickLyricsText,
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onHorizontalDrag = { change, dragAmount ->
                                                    deltaX = dragAmount
                                                },
                                                onDragStart = {
                                                },
                                                onDragEnd = {
                                                    if (!disablePlayerHorizontalSwipe) {
                                                        if (deltaX > 5) {
                                                            binder.player.playPrevious()
                                                        } else if (deltaX < -5) {
                                                            binder.player.playNext()
                                                        }

                                                    }

                                                }

                                            )
                                        }
                                )
                            }
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (playerType == PlayerType.Modern) {
                            BoxWithConstraints(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(.5f)
                                    //.border(BorderStroke(2.dp, Color.Blue))
                                /*modifier = Modifier
                               .weight(1f)*/
                                //.padding(vertical = 10.dp)
                            ) {
                                if (showthumbnail) {
                                    if (!isShowingVisualizer) {
                                        val fling = PagerDefaults.flingBehavior(
                                            state = pagerState,
                                            snapPositionalThreshold = 0.25f
                                        )
                                        val pageSpacing =
                                            thumbnailSpacingL.toInt() * 0.01 * (screenWidth) - (2.5 * playerThumbnailSizeL.padding.dp)

                                        LaunchedEffect(
                                            pagerState,
                                            binder.player.currentMediaItemIndex
                                        ) {
                                            if (appRunningInBackground || isShowingLyrics) {
                                                pagerState.scrollToPage(binder.player.currentMediaItemIndex)
                                            } else {
                                                pagerState.animateScrollToPage(binder.player.currentMediaItemIndex)
                                            }
                                        }

                                        if (!showQueue) {
                                            LaunchedEffect(pagerState) {
                                                var previousPage = pagerState.settledPage
                                                snapshotFlow { pagerState.settledPage }.distinctUntilChanged()
                                                    .collect {
                                                        if (previousPage != it) {
                                                            if (it != binder.player.currentMediaItemIndex) binder.player.playAtIndex(
                                                                it
                                                            )
                                                        }
                                                        previousPage = it
                                                    }
                                            }
                                        }
                                        HorizontalPager(
                                            state = pagerState,
                                            pageSize = PageSize.Fixed(thumbnailSizeDp),
                                            pageSpacing = thumbnailSpacingL.toInt() * 0.01 * (screenWidth) - (2.5 * playerThumbnailSizeL.padding.dp),
                                            contentPadding = PaddingValues(
                                                start = ((maxWidth - maxHeight) / 2).coerceAtLeast(
                                                    0.dp
                                                ),
                                                end = ((maxWidth - maxHeight) / 2 + if (pageSpacing < 0.dp) (-(pageSpacing)) else 0.dp).coerceAtLeast(
                                                    0.dp
                                                )
                                            ),
                                            beyondViewportPageCount = 3,
                                            flingBehavior = fling,
                                            userScrollEnabled = !disablePlayerHorizontalSwipe,
                                            modifier = Modifier
                                                .padding(
                                                    all = (if (thumbnailType == ThumbnailType.Modern) -(10.dp) else 0.dp).coerceAtLeast(
                                                        0.dp
                                                    )
                                                )
                                                .conditional(fadingedge) { horizontalFadingEdge() }
                                        ) { it ->

                                            val coverPainter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(
                                                        binder.player.getMediaItemAt(it).mediaMetadata.artworkUri.thumbnail(
                                                            1200
                                                        )
                                                    )
                                                    .build()
                                            )

                                            val coverModifier = Modifier
                                                .aspectRatio(1f)
                                                .padding(all = playerThumbnailSizeL.padding.dp)
                                                .graphicsLayer {
                                                    val pageOffSet =
                                                        ((pagerState.currentPage - it) + pagerState.currentPageOffsetFraction).absoluteValue
                                                    alpha = lerp(
                                                        start = 0.9f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 1f)
                                                    )
                                                    scaleY = lerp(
                                                        start = 0.85f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 5f)
                                                    )
                                                    scaleX = lerp(
                                                        start = 0.85f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 5f)
                                                    )
                                                }
                                                .conditional(thumbnailType == ThumbnailType.Modern) {
                                                    padding(
                                                        all = 10.dp
                                                    )
                                                }
                                                .conditional(thumbnailType == ThumbnailType.Modern) {
                                                    doubleShadowDrop(
                                                        if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape(),
                                                        4.dp,
                                                        8.dp
                                                    )
                                                }
                                                .clip(thumbnailRoundness.shape())
                                                .combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        if (it == pagerState.settledPage && thumbnailTapEnabled) {
                                                            if (isShowingVisualizer) isShowingVisualizer =
                                                                false
                                                            isShowingLyrics = !isShowingLyrics
                                                        }
                                                        if (it != pagerState.settledPage) {
                                                            binder.player.playAtIndex(it)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (it == pagerState.settledPage)
                                                            showThumbnailOffsetDialog = true
                                                    }
                                                )

                                            if (showCoverThumbnailAnimation)
                                                RotateThumbnailCoverAnimationModern(
                                                    painter = coverPainter,
                                                    isSongPlaying = binderPlayer.isPlaying || shouldBePlaying,
                                                    modifier = coverModifier
                                                        .zIndex(
                                                            if (it == pagerState.currentPage) 1f
                                                            else if (it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1)) 0.85f
                                                            else if (it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2)) 0.78f
                                                            else if (it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3)) 0.73f
                                                            else if (it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4)) 0.68f
                                                            else if (it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5)) 0.63f
                                                            else 0.57f
                                                        ),
                                                    state = pagerState,
                                                    it = it,
                                                    imageCoverSize = imageCoverSize,
                                                    type = coverThumbnailAnimation
                                                )
                                            else
                                                Box(
                                                    modifier = Modifier
                                                        .zIndex(
                                                            if (it == pagerState.currentPage) 1f
                                                            else if (it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1)) 0.85f
                                                            else if (it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2)) 0.78f
                                                            else if (it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3)) 0.73f
                                                            else if (it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4)) 0.68f
                                                            else if (it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5)) 0.63f
                                                            else 0.57f
                                                        )
                                                ) {
                                                    Image(
                                                        painter = coverPainter,
                                                        contentDescription = "",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = coverModifier
                                                    )
                                                    if (isDragged && it == binder.player.currentMediaItemIndex) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.Center)
                                                                .matchParentSize()
                                                        ) {
                                                            NowPlayingSongIndicator(
                                                                binder.player.getMediaItemAt(
                                                                    binder.player.currentMediaItemIndex
                                                                ).mediaId, binder.player,
                                                                Dimensions.thumbnails.album
                                                            )
                                                        }
                                                    }
                                                }
                                        }


                                    }
                                }
                                if (isShowingVisualizer) {
                                    Box(
                                        modifier = Modifier
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        deltaX = dragAmount
                                                    },
                                                    onDragStart = {
                                                    },
                                                    onDragEnd = {
                                                        if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                                            if (deltaX > 5) {
                                                                binder.player.playPrevious()
                                                            } else if (deltaX < -5) {
                                                                binder.player.playNext()
                                                            }

                                                        }

                                                    }

                                                )
                                            }
                                    ) {
                                        NextVisualizer(
                                            isDisplayed = isShowingVisualizer
                                        )
                                    }
                                }
                            }
                        }
                        if (playerType == PlayerType.Essential || isShowingVisualizer) {
                            controlsContent(
                                Modifier
                                    .padding(vertical = 8.dp)
                                    .conditional(playerType == PlayerType.Essential) { fillMaxHeight() }
                                    .conditional(playerType == PlayerType.Essential) { weight(1f) }

                            )
                        } else {

                            val index = (if (!showthumbnail) {
                                if (pagerStateFS.currentPage > binder.player.currentTimeline.windowCount) 0 else pagerStateFS.currentPage
                            } else if (pagerState.currentPage > binder.player.currentTimeline.windowCount) 0 else pagerState.currentPage).coerceIn(
                                0,
                                (binderPlayer.mediaItemCount) - 1
                            )

                            Controls(
                                navController = navController,
                                onCollapse = onDismiss,
                                onBlurScaleChange = { blurStrength = it },
                                expandedplayer = expandedplayer,
                                titleExpanded = titleExpanded,
                                timelineExpanded = timelineExpanded,
                                controlsExpanded = controlsExpanded,
                                isShowingLyrics = isShowingLyrics,
                                media = binderPlayer.getMediaItemAt(index)
                                    .toUiMedia(positionAndDuration.second.toLong()),
                                mediaItem = binderPlayer.getMediaItemAt(index),
                                title = binderPlayer.getMediaItemAt(index).mediaMetadata.title?.toString(),
                                artist = binderPlayer.getMediaItemAt(index).mediaMetadata.artist?.toString(),
                                artistIds = artistsInfo,
                                albumId = albumId,
                                shouldBePlaying = shouldBePlaying,
                                position = positionAndDuration.first.toLong(),
                                duration = positionAndDuration.second.toLong(),
                                isExplicit = binderPlayer.getMediaItemAt(index).isExplicit,
                                modifier = Modifier
                                    .padding(vertical = 8.dp),
                                onPlay = {
                                    player.value?.play()
                                    //println("LinkClient OnLinePlayer Controls play")
                                    linkServiceClientSend(
                                        binderPlayer.getMediaItemAt(index).mediaId.toCommandPlayAt(currentSecond.toInt()),
                                        castToLinkDevice,
                                        linkDevicesSelected
                                    )
                                },
                                onPause = {
                                    player.value?.pause()
                                    //println("LinkClient OnLinePlayer Controls pause 2")
                                    //CoroutineScope(Dispatchers.IO).launch {
                                        //if (linkClient != null)
                                            linkServiceClientSend(
                                                LINKWEB_COMMAND_PAUSE.toCommand(),
                                                castToLinkDevice,
                                                linkDevicesSelected
                                            )
                                            //linkClient?.send("pause|")
                                    //}
                                },
                                onSeekTo = { player.value?.seekTo(it) },
                                onNext = { binder.player.playNext() },
                                onPrevious = { binder.player.playPrevious() },
                                playerState = playerState.value,
                            )

                        }
                        if (!showthumbnail || playerType == PlayerType.Modern) {
                            StatsForNerds(
                                mediaId = mediaItem.mediaId,
                                isDisplayed = statsfornerds,
                                onDismiss = {}
                            )
                        }
                        actionsBarContent()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .conditional(queueType == QueueType.Modern) {
                        haze(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = if (lightTheme) Color.White.copy(0.5f) else Color.Black.copy(
                                    0.5f
                                ),
                                blurRadius = 8.dp
                            )
                        )
                    }
            ) {
                if ((playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor && playerType == PlayerType.Modern && (!showthumbnail || albumCoverRotation)) || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) {
                    val fling = PagerDefaults.flingBehavior(
                        state = pagerStateFS,
                        snapPositionalThreshold = 0.30f
                    )
                    val scaleAnimationFloat by animateFloatAsState(
                        if (isDraggedFS) 0.85f else 1f, label = ""
                    )
                    pagerStateFS.LaunchedEffectScrollToPage(binder.player.currentMediaItemIndex)

                    if (!showQueue) {
                        LaunchedEffect(pagerStateFS) {
                            var previousPage = pagerStateFS.settledPage
                            snapshotFlow { pagerStateFS.settledPage }.distinctUntilChanged()
                                .collect {
                                    if (previousPage != it) {
                                        delay(if (swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Fade) 0 else 400)
                                        if (it != binder.player.currentMediaItemIndex) binder.player.playAtIndex(
                                            it
                                        )
                                    }
                                    previousPage = it
                                }
                        }
                    }
                    HorizontalPager(
                        state = pagerStateFS,
                        beyondViewportPageCount = if (swipeAnimationNoThumbnail != SwipeAnimationNoThumbnail.Circle || albumCoverRotation && (isShowingLyrics || showthumbnail)) 1 else 0,
                        flingBehavior = fling,
                        userScrollEnabled = !((albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) && (isShowingLyrics || showthumbnail)),
                        modifier = Modifier
                            .background(colorPalette().background1)
                            .pointerInteropFilter {
                                circleOffsetY = it.y
                                false
                            }
                    ) { it ->

                        var currentRotation by remember {
                            mutableFloatStateOf(0f)
                        }

                        val rotation = remember {
                            Animatable(currentRotation)
                        }

                        LaunchedEffect(binderPlayer.isPlaying, pagerStateFS.settledPage) {
                            if (binderPlayer.isPlaying && it == pagerStateFS.settledPage) {
                                rotation.animateTo(
                                    targetValue = currentRotation + 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(30000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                ) {
                                    currentRotation = value
                                }
                            } else {
                                if (currentRotation > 0f && it == pagerStateFS.settledPage) {
                                    rotation.animateTo(
                                        targetValue = currentRotation + 10,
                                        animationSpec = tween(
                                            1250,
                                            easing = LinearOutSlowInEasing
                                        )
                                    ) {
                                        currentRotation = value
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .conditional((albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) && (isShowingLyrics || showthumbnail)) {
                                    zIndex(if (it == pagerStateFS.currentPage) 1f else 0.9f)
                                }
                                .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale && isDraggedFS) {
                                    graphicsLayer {
                                        scaleY = scaleAnimationFloat
                                        scaleX = scaleAnimationFloat
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(
                                        binder.player.getMediaItemAt(it).mediaMetadata.artworkUri.thumbnail(
                                            1200
                                        )
                                    )
                                    .transformations(
                                        listOf(
                                            if (showthumbnail) {
                                                BlurTransformation(
                                                    scale = 0.5f,
                                                    radius = blurStrength.toInt(),
                                                    //darkenFactor = blurDarkenFactor
                                                )

                                            } else
                                                BlurTransformation(
                                                    scale = 0.5f,
                                                    //radius = blurStrength2.toInt(),
                                                    radius = if ((isShowingLyrics && !isShowingVisualizer) || !noblur) blurStrength.toInt() else 0,
                                                    //darkenFactor = blurDarkenFactor
                                                )
                                        )
                                    )
                                    .build(),
                                contentDescription = "",
                                contentScale = if ((albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) && (isShowingLyrics || showthumbnail)) ContentScale.Fit else ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .conditional(albumCoverRotation || (animatedGradient == AnimatedGradient.Random && tempGradient == gradients[14])) {
                                        graphicsLayer {
                                            scaleX =
                                                if (isShowingLyrics || showthumbnail) (screenHeight / screenWidth) + 0.5f else 1f
                                            scaleY =
                                                if (isShowingLyrics || showthumbnail) (screenHeight / screenWidth) + 0.5f else 1f
                                            rotationZ =
                                                if ((it == pagerStateFS.settledPage) && (isShowingLyrics || showthumbnail)) rotation.value else 0f
                                        }
                                    }
                                    .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Fade && !showthumbnail) {
                                        graphicsLayer {
                                            val pageOffset = pagerStateFS.currentPageOffsetFraction
                                            translationX = pageOffset * size.width
                                            alpha = 1 - pageOffset.absoluteValue
                                        }
                                    }
                                    .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Carousel && isDraggedFS) { //by sinasamaki
                                        graphicsLayer {
                                            val startOffset = pagerStateFS.startOffsetForPage(it)
                                            translationX = size.width * (startOffset * .99f)
                                            alpha = (2f - startOffset) / 2f
                                            val blur = (startOffset * 20f).coerceAtLeast(0.1f)
                                            renderEffect = RenderEffect
                                                .createBlurEffect(
                                                    blur, blur, Shader.TileMode.DECAL
                                                ).asComposeRenderEffect()
                                            val scale = 1f - (startOffset * .1f)
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                    }
                                    .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Circle && !showthumbnail) { //by sinasamaki
                                        graphicsLayer {
                                            val pageOffset = pagerStateFS.offsetForPage(it)
                                            translationX = size.width * pageOffset

                                            val endOffset = pagerStateFS.endOffsetForPage(it)
                                            shadowElevation = 20f

                                            shape = CirclePath(
                                                progress = 1f - endOffset.absoluteValue,
                                                origin = Offset(
                                                    size.width,
                                                    circleOffsetY,
                                                )
                                            )

                                            clip = true

                                            val absoluteOffset =
                                                pagerStateFS.offsetForPage(it).absoluteValue
                                            val scale = 1f + (absoluteOffset.absoluteValue * .4f)

                                            scaleX = scale
                                            scaleY = scale

                                            val startOffset = pagerStateFS.startOffsetForPage(it)
                                            alpha = (2f - startOffset) / 2f
                                        }
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (thumbnailTapEnabled && !showthumbnail) {
                                                if (isShowingVisualizer) isShowingVisualizer = false
                                                isShowingLyrics = !isShowingLyrics
                                            }
                                        },
                                        onDoubleClick = {
                                            if (!showlyricsthumbnail && !showvisthumbnail)
                                                showthumbnail = !showthumbnail
                                        },
                                        onLongClick = {
                                            if (showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur)
                                                showBlurPlayerDialog = true
                                        }
                                    )
                            )
                            if ((swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale) && isDraggedFS) {
                                Column {
                                    Spacer(
                                        modifier = Modifier
                                            .conditional((screenWidth <= (screenHeight / 2)) && (showlyricsthumbnail || (!expandedplayer && !isShowingLyrics))) {
                                                height(screenWidth)
                                            }
                                            .conditional((screenWidth > (screenHeight / 2)) || expandedplayer || (isShowingLyrics && !showlyricsthumbnail)) {
                                                weight(
                                                    1f
                                                )
                                            })

                                    Box(
                                        modifier = Modifier
                                            .conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) {
                                                weight(
                                                    1f
                                                )
                                            }
                                    ) {
                                        Controls(
                                            navController = navController,
                                            onCollapse = onDismiss,
                                            onBlurScaleChange = { blurStrength = it },
                                            expandedplayer = expandedplayer,
                                            titleExpanded = titleExpanded,
                                            timelineExpanded = timelineExpanded,
                                            controlsExpanded = controlsExpanded,
                                            isShowingLyrics = isShowingLyrics,
                                            media = binderPlayer.getMediaItemAt(it)
                                                .toUiMedia(positionAndDuration.second.toLong()),
                                            mediaItem = binderPlayer.getMediaItemAt(it),
                                            title = binderPlayer.getMediaItemAt(it).mediaMetadata.title?.toString(),
                                            artist = binderPlayer.getMediaItemAt(it).mediaMetadata.artist?.toString(),
                                            artistIds = artistsInfo,
                                            albumId = albumId,
                                            shouldBePlaying = shouldBePlaying,
                                            position = positionAndDuration.first.toLong(),
                                            duration = positionAndDuration.second.toLong(),
                                            isExplicit = binderPlayer.getMediaItemAt(it).isExplicit,
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .fillMaxWidth(),
                                            onPlay = {
                                                player.value?.play()
                                                //println("LinkClient OnLinePlayer Controls pause 3")
                                                linkServiceClientSend(
                                                    binderPlayer.getMediaItemAt(it).mediaId.toCommandPlayAt(currentSecond.toInt()),
                                                    castToLinkDevice,
                                                    linkDevicesSelected
                                                )

                                            },
                                            onPause = {
                                                player.value?.pause()
                                                //println("LinkClient OnLinePlayer Controls pause 4")
                                                linkServiceClientSend(
                                                    LINKWEB_COMMAND_PAUSE.toCommand(),
                                                    castToLinkDevice,
                                                    linkDevicesSelected
                                                )

                                            },
                                            onSeekTo = { player.value?.seekTo(it) },
                                            onNext = { binder.player.playNext() },
                                            onPrevious = { binder.player.playPrevious() },
                                            playerState = playerState.value,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    1.0f to if (bottomgradient) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(
                                        if (isLandscape) 0.8f else 0.75f
                                    ) else Color.Black.copy(if (isLandscape) 0.8f else 0.75f) else Color.Transparent,
                                    startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                                    endY = POSITIVE_INFINITY
                                )
                            )
                            .background(
                                if (bottomgradient) if (isLandscape) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(
                                    0.25f
                                ) else Color.Black.copy(0.25f) else Color.Transparent else Color.Transparent
                            )
                    ) {}
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = containerModifier
                        //.padding(top = 10.dp)
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


                    if (showTopActionsBar && !it.fast4x.riplay.utils.isLandscape) {
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
                                                    binder = binder,
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

                    if (topPadding && !showTopActionsBar) {
                        Spacer(
                            modifier = Modifier
                                .padding(
                                    windowInsets
                                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                                        .asPaddingValues()
                                )
                                .height(35.dp)
                        )
                    }

                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .conditional(
                                !it.fast4x.riplay.utils.isLandscape &&
                                        (screenWidth <= (screenHeight / 2)) && (showlyricsthumbnail || (!expandedplayer && !isShowingLyrics))
                            ) {
                                height(
                                    screenWidth
                                )
                            }
                            .conditional(
                                !it.fast4x.riplay.utils.isLandscape &&
                                        (screenWidth > (screenHeight / 2)) || expandedplayer || (isShowingLyrics && !showlyricsthumbnail)
                            ) {
                                weight(
                                    1f
                                )
                            }
                            .conditional(it.fast4x.riplay.utils.isLandscape && mediaItem.isVideo) {
                                height(screenHeight)
                                width(screenWidth)
                            }
                            //.border(BorderStroke(2.dp, colorPalette().collapsedPlayerProgressBar))
                    ) {

                        if (showthumbnail) {
                            if ((!isShowingLyrics && !isShowingVisualizer) || (isShowingVisualizer && showvisthumbnail) || (isShowingLyrics && showlyricsthumbnail)) {
                                if (playerType == PlayerType.Modern) {
                                    val fling = PagerDefaults.flingBehavior(
                                        state = pagerState,
                                        snapPositionalThreshold = 0.25f
                                    )

                                    pagerState.LaunchedEffectScrollToPage(binder.player.currentMediaItemIndex)

                                    if (!showQueue) {
                                        LaunchedEffect(pagerState) {
                                            var previousPage = pagerState.settledPage
                                            snapshotFlow { pagerState.settledPage }.distinctUntilChanged()
                                                .collect {
                                                    if (previousPage != it) {
                                                        if (it != binder.player.currentMediaItemIndex) binder.player.playAtIndex(
                                                            it
                                                        )
                                                    }
                                                    previousPage = it
                                                }
                                        }
                                    }

                                    val pageSpacing =
                                        (thumbnailSpacing.toInt() * 0.01 * (screenHeight) - if (carousel) (3 * carouselSize.size.dp) else (2 * playerThumbnailSize.padding.dp))
                                    val animatePageSpacing by animateDpAsState(
                                        if (expandedplayer) (thumbnailSpacing.toInt() * 0.01 * (screenHeight) - if (carousel) (3 * carouselSize.size.dp) else (2 * carouselSize.size.dp)) else 10.dp,
                                        label = ""
                                    )

                                    val animatePadding by animateDpAsState(
                                        if (expandedplayer) carouselSize.size.dp else playerThumbnailSize.padding.dp
                                    )
                                    VerticalPager(
                                        state = pagerState,
                                        pageSize = PageSize.Fixed(if (maxWidth < maxHeight) maxWidth else maxHeight),
                                        contentPadding = PaddingValues(
                                            top = (maxHeight - (if (maxWidth < maxHeight) maxWidth else maxHeight)) / 2,
                                            bottom = (maxHeight - (if (maxWidth < maxHeight) maxWidth else maxHeight)) / 2 + if (pageSpacing < 0.dp) (-(pageSpacing)) else 0.dp
                                        ),
                                        pageSpacing = animatePageSpacing,
                                        beyondViewportPageCount = 2,
                                        flingBehavior = fling,
                                        userScrollEnabled = expandedplayer || !disablePlayerHorizontalSwipe,
                                        modifier = Modifier
                                            .padding(
                                                all = (if (expandedplayer) 0.dp else if (thumbnailType == ThumbnailType.Modern) -(10.dp) else 0.dp).coerceAtLeast(
                                                    0.dp
                                                )
                                            )
                                            .conditional(fadingedge) {
                                                VerticalfadingEdge2(
                                                    fade = (if (expandedplayer) thumbnailFadeEx else thumbnailFade) * 0.05f,
                                                    showTopActionsBar,
                                                    topPadding,
                                                    expandedplayer
                                                )
                                            }
                                    ) { it ->

                                        val coverPainter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(
                                                    binder.player.getMediaItemAt(it).mediaMetadata.artworkUri.thumbnail(
                                                        1200
                                                    )
                                                )
                                                .build()
                                        )

                                        val coverModifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(all = animatePadding)
                                            .conditional(carousel)
                                            {
                                                graphicsLayer {
                                                    val pageOffSet =
                                                        ((pagerState.currentPage - it) + pagerState.currentPageOffsetFraction).absoluteValue
                                                    alpha = lerp(
                                                        start = 0.9f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 1f)
                                                    )
                                                    scaleY = lerp(
                                                        start = 0.9f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 5f)
                                                    )
                                                    scaleX = lerp(
                                                        start = 0.9f,
                                                        stop = 1f,
                                                        fraction = 1f - pageOffSet.coerceIn(0f, 5f)
                                                    )
                                                }
                                            }
                                            .conditional(thumbnailType == ThumbnailType.Modern) {
                                                padding(
                                                    all = 10.dp
                                                )
                                            }
                                            .conditional(thumbnailType == ThumbnailType.Modern) {
                                                doubleShadowDrop(
                                                    if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape(),
                                                    4.dp,
                                                    8.dp
                                                )
                                            }
                                            .clip(thumbnailRoundness.shape())
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (it == pagerState.settledPage && thumbnailTapEnabled) {
                                                        if (isShowingVisualizer) isShowingVisualizer =
                                                            false
                                                        isShowingLyrics = !isShowingLyrics
                                                    }
                                                    if (it != pagerState.settledPage) {
                                                        binder.player.playAtIndex(it)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (it == pagerState.settledPage && (expandedplayer || fadingedge))
                                                        showThumbnailOffsetDialog = true
                                                }
                                            )

                                        if (showCoverThumbnailAnimation)
                                            RotateThumbnailCoverAnimationModern(
                                                painter = coverPainter,
                                                isSongPlaying = binderPlayer.isPlaying  || shouldBePlaying,
                                                modifier = coverModifier
                                                    .zIndex(
                                                        if (it == pagerState.currentPage) 1f
                                                        else if (it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1)) 0.85f
                                                        else if (it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2)) 0.78f
                                                        else if (it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3)) 0.73f
                                                        else if (it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4)) 0.68f
                                                        else if (it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5)) 0.63f
                                                        else 0.57f
                                                    ),
                                                state = pagerState,
                                                it = it,
                                                imageCoverSize = imageCoverSize,
                                                type = coverThumbnailAnimation
                                            )
                                        else
                                            Box(
                                                modifier = Modifier
                                                    .zIndex(
                                                        if (it == pagerState.currentPage) 1f
                                                        else if (it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1)) 0.85f
                                                        else if (it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2)) 0.78f
                                                        else if (it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3)) 0.73f
                                                        else if (it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4)) 0.68f
                                                        else if (it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5)) 0.63f
                                                        else 0.57f
                                                    )
                                            ) {

                                                val isVideo =
                                                    remember { binder.player.getMediaItemAt(it).isVideo }
                                                if (!isVideo)
                                                    Image(
                                                        painter = coverPainter,
                                                        contentDescription = "",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = coverModifier
                                                    )

                                                if (isDragged && expandedplayer && it == binder.player.currentMediaItemIndex) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .matchParentSize()
                                                    ) {
                                                        NowPlayingSongIndicator(
                                                            binder.player.getMediaItemAt(
                                                                binder.player.currentMediaItemIndex
                                                            ).mediaId, binder.player,
                                                            Dimensions.thumbnails.album
                                                        )
                                                    }
                                                }
                                            }
                                    }

                                } else {
                                    val animatePadding by animateDpAsState(
                                        if (expandedplayer) carouselSize.size.dp else playerThumbnailSize.padding.dp
                                    )

                                    val coverPainter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(mediaItem.mediaMetadata.artworkUri.thumbnail(1200))
                                            .build()
                                   )

                                     val coverModifier = Modifier
                                        .conditional(thumbnailType == ThumbnailType.Modern) {
                                            padding(
                                                all = 10.dp
                                            )
                                        }
                                         .conditional(thumbnailType == ThumbnailType.Modern) {
                                             doubleShadowDrop(
                                                 if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape(),
                                                 4.dp,
                                                 8.dp
                                             )
                                         }
                                         .clip(thumbnailRoundness.shape())

                                    if (!mediaItem.isVideo)
                                        Image(
                                            painter = coverPainter,
                                           contentDescription = "",
                                            contentScale = ContentScale.Fit,
                                            modifier = coverModifier
                                        )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            deltaX = dragAmount
                                        },
                                        onDragStart = {
                                        },
                                        onDragEnd = {
                                            if (!disablePlayerHorizontalSwipe) {
                                                if (deltaX > 5) {
                                                    binder.player.playPrevious()
                                                } else if (deltaX < -5) {
                                                    binder.player.playNext()
                                                }

                                            }

                                        }

                                    )
                                }
                        ) {
                            if (!showlyricsthumbnail)
                                Lyrics(
                                    mediaId = mediaItem.mediaId,
                                    isDisplayed = isShowingLyrics,
                                    onDismiss = {
                                        isShowingLyrics = false
                                    },
                                    ensureSongInserted = { Database.insert(mediaItem) },
                                    size = 1000.dp,
                                    mediaMetadataProvider = mediaItem::mediaMetadata,
                                    durationProvider = { positionAndDuration.second.toLong() * 1000 },
                                    positionProvider = { positionAndDuration.first.toLong() * 1000 },
                                    isLandscape = isLandscape,
                                    clickLyricsText = clickLyricsText,
                                )
                            if (!showvisthumbnail)
                                NextVisualizer(
                                    isDisplayed = isShowingVisualizer
                                )
                        }

                        val animatePadding by animateDpAsState(
                            if (expandedplayer) carouselSize.size.dp else playerThumbnailSize.padding.dp
                        )

                        val coverModifier = Modifier
                            .conditional(!it.fast4x.riplay.utils.isLandscape && !mediaItem.isVideo) {
                                aspectRatio(1f)
                            }
                            .conditional(!it.fast4x.riplay.utils.isLandscape) {
                                padding(all = animatePadding)
                            }
                            //.padding(all = animatePadding)
                            .conditional(thumbnailType == ThumbnailType.Modern) {
                                padding(
                                    all = 10.dp
                                )
                                doubleShadowDrop(
                                    if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape(),
                                    4.dp,
                                    8.dp
                                )
                            }
                            .clip(thumbnailRoundness.shape())

                        //use online player
                        thumbnailContent(
                            if ((!mediaItem.isVideo || isShowingVisualizer) && !castToLinkDevice)
                                Modifier.hidePlayer()
                            else
                                coverModifier
                        )

                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) {
                                weight(
                                    1f
                                )
                            }
                            .conditional(playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey) {
                                background(
                                    Brush.verticalGradient(
                                        0.0f to Color(0xff141414),
                                        1.0f to Color.Black,
                                        startY = 0f,
                                        endY = POSITIVE_INFINITY
                                    ),
                                    RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                                )
                            }
                    ) {
                        if (playerBackgroundColors == PlayerBackgroundColors.MidnightOdyssey && !isShowingLyrics) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(0.dp, (-15).dp)
                                        .size(30.dp)
                                        .background(Color.DarkGray.copy(0.5f), CircleShape)
                                ) {
                                    IconButton(
                                        color = colorPalette().favoritesIcon,
                                        icon = getLikeState(mediaItem.mediaId),
                                        onClick = {
                                            if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
                                                SmartMessage(
                                                    appContext().resources.getString(R.string.no_connection),
                                                    context = appContext(),
                                                    type = PopupType.Error
                                                )
                                            } else if (!isYouTubeSyncEnabled()) {
                                                Database.asyncTransaction {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        mediaItem.takeIf { it.mediaId == mediaItem.mediaId }
                                                            ?.let { mediaItem ->
                                                                mediaItemToggleLike(mediaItem)
                                                            }
                                                    }
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    addToYtLikedSong(mediaItem)
                                                }
                                            }
                                            if (effectRotationEnabled) isRotated = !isRotated
                                        },
                                        onLongClick = {
                                            if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
                                                SmartMessage(
                                                    appContext().resources.getString(R.string.no_connection),
                                                    context = appContext(),
                                                    type = PopupType.Error
                                                )
                                            } else if (!isYouTubeSyncEnabled()) {
                                                Database.asyncTransaction {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        mediaItem.takeIf { it.mediaId == mediaItem.mediaId }
                                                            ?.let { mediaItem ->
                                                                if (like(
                                                                        mediaItem.mediaId,
                                                                        setDisLikeState(likedAt)
                                                                    ) == 0
                                                                ) {
                                                                    insert(
                                                                        mediaItem,
                                                                        Song::toggleDislike
                                                                    )
                                                                }
                                                            }
                                                    }
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    unlikeYtVideoOrSong(mediaItem)
                                                }
                                            }
                                            if (effectRotationEnabled) isRotated = !isRotated
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        if (!expandedplayer || !isShowingLyrics || queueDurationExpanded) {
                            if (showTotalTimeQueue)
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.time),
                                        colorFilter = ColorFilter.tint(colorPalette().accent),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(horizontal = 5.dp),
                                        contentDescription = "Background Image",
                                        contentScale = ContentScale.Fit
                                    )

                                    Box {
                                        BasicText(
                                            text = " ${formatAsTime(totalPlayTimes)}",
                                            style = typography().xxs.semiBold.merge(
                                                TextStyle(
                                                    textAlign = TextAlign.Center,
                                                    color = colorPalette().text,
                                                )
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        BasicText(
                                            text = " ${formatAsTime(totalPlayTimes)}",
                                            style = typography().xxs.semiBold.merge(
                                                TextStyle(
                                                    textAlign = TextAlign.Center,
                                                    drawStyle = Stroke(
                                                        width = 1f,
                                                        join = StrokeJoin.Round
                                                    ),
                                                    color = if (!textoutline) Color.Transparent
                                                    else if (colorPaletteMode == ColorPaletteMode.Light ||
                                                        (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
                                                    )
                                                        Color.White.copy(0.5f)
                                                    else Color.Black,
                                                )
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }


                            Spacer(
                                modifier = Modifier
                                    .height(10.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) {
                                    weight(
                                        1f
                                    )
                                }) {
                            if (playerType == PlayerType.Essential || isShowingLyrics || isShowingVisualizer) {
                                controlsContent(
                                    Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth(),
                                    //.weight(1f)

                                )
                            } else if (!(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale && isDraggedFS)) {
                                val index = (if (!showthumbnail) {
                                    if (pagerStateFS.currentPage > binder.player.currentTimeline.windowCount) 0 else pagerStateFS.currentPage
                                } else if (pagerState.currentPage > binder.player.currentTimeline.windowCount) 0 else pagerState.currentPage).coerceIn(
                                    0,
                                    (binderPlayer.mediaItemCount) - 1
                                )

                                Controls(
                                    navController = navController,
                                    onCollapse = onDismiss,
                                    onBlurScaleChange = { blurStrength = it },
                                    expandedplayer = expandedplayer,
                                    titleExpanded = titleExpanded,
                                    timelineExpanded = timelineExpanded,
                                    controlsExpanded = controlsExpanded,
                                    isShowingLyrics = isShowingLyrics,
                                    media = binderPlayer.getMediaItemAt(index)
                                        .toUiMedia(positionAndDuration.second.toLong()),
                                    mediaItem = binderPlayer.getMediaItemAt(index),
                                    title = binderPlayer.getMediaItemAt(index).mediaMetadata.title?.toString(),
                                    artist = binderPlayer.getMediaItemAt(index).mediaMetadata.artist?.toString(),
                                    artistIds = artistsInfo,
                                    albumId = albumId,
                                    shouldBePlaying = shouldBePlaying,
                                    position = positionAndDuration.first.toLong(),
                                    //.weight(1f),
                                    duration = positionAndDuration.second.toLong(),
                                    isExplicit = binderPlayer.getMediaItemAt(index).isExplicit,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth(),
                                    onPlay = {
                                        player.value?.play()
                                        //println("LinkClient OnLinePlayer Controls play")
                                        linkServiceClientSend(
                                            binderPlayer.getMediaItemAt(index).mediaId.toCommandPlayAt(currentSecond.toInt()),
                                            castToLinkDevice,
                                            linkDevicesSelected
                                        )

                                    },
                                    onPause = {
                                        player.value?.pause()
                                        //println("LinkClient OnLinePlayer Controls pause 5")
                                        //CoroutineScope(Dispatchers.IO).launch {
                                            //if (linkClient != null)
                                                linkServiceClientSend(
                                                    LINKWEB_COMMAND_PAUSE.toCommand(),
                                                    castToLinkDevice,
                                                    linkDevicesSelected
                                                )
                                                //linkClient?.send("pause|")
                                        //}
                                    },
                                    onSeekTo = { player.value?.seekTo(it) },
                                    onNext = { binder.player.playNext() },
                                    onPrevious = { binder.player.playPrevious() },
                                    playerState = playerState.value,
                                )

                            }
                        }

                        if (!showthumbnail || playerType == PlayerType.Modern) {
                            if (!isShowingLyrics || statsExpanded) {
                                StatsForNerds(
                                    mediaId = mediaItem.mediaId,
                                    isDisplayed = statsfornerds,
                                    onDismiss = {}
                                )
                            }
                        }

                        actionsBarContent()
                    }
                }
            }
        }

        CustomModalBottomSheet(
            showSheet = showQueue,
            onDismissRequest = { showQueue = false },
            containerColor = if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background2,
            contentColor = if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background2,
            modifier = Modifier
                .fillMaxWidth()
                .conditional(queueType == QueueType.Modern) { hazeChild(state = hazeState) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape()
        ) {
            Queue(
                navController = navController,
                showPlayer = {},
                hidePlayer = {},
                player = player,
                playerState = playerState,
                currentDuration = currentDuration,
                currentSecond = currentSecond,
                onDismiss = {
                    queueLoopType = it
                    showQueue = false
                },
                onDiscoverClick = {
                    discoverIsEnabled = it
                }
            )
        }

        CustomModalBottomSheet(
            showSheet = showSearchEntity,
            onDismissRequest = { showSearchEntity = false },
            containerColor = if (playerType == PlayerType.Modern) Color.Transparent else colorPalette().background2,
            contentColor = if (playerType == PlayerType.Modern) Color.Transparent else colorPalette().background2,
            modifier = Modifier
                .fillMaxWidth()
                .conditional(queueType == QueueType.Modern) { hazeChild(state = hazeState) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape()
        ) {
            SearchYoutubeEntity(
                navController = navController,
                onDismiss = { showSearchEntity = false },
                query = "${mediaItem.mediaMetadata.artist.toString()} - ${mediaItem.mediaMetadata.title.toString()}",
                disableScrollingText = disableScrollingText
            )
        }

    }

}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun PagerState.LaunchedEffectScrollToPage(
    index: Int
) {
    val pagerState = this
    LaunchedEffect(pagerState, index) {
        if (!appRunningInBackground) {
            pagerState.animateScrollToPage(index)
        } else {
            pagerState.scrollToPage(index)
        }
    }
}

