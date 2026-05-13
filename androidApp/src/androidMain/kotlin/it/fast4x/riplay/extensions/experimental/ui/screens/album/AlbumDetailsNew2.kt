package it.fast4x.riplay.extensions.experimental.ui.screens.album

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.AlbumPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Info
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AlbumsItemMenu
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.ItemsList
import it.fast4x.riplay.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.SelectorDialog
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.ui.components.ActionPillButton
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.components.themed.QueuesDialog
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.addToYtLikedSongs
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.isNetworkConnected
import it.fast4x.riplay.utils.mediaItemSetLiked
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

// ─────────────────────────────────────────────────────────────────────────────
// Composable helper: animated icon-pill button for the actions bar
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────

@ExperimentalSerializationApi
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun AlbumDetailsNew2(
    navController: NavController,
    browseId: String,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateTo: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val context = LocalContext.current
    val selectedQueue = LocalSelectedQueue.current
    var songs by persistList<Song>("album/$browseId/songs")
    var album by persist<Album?>("album/$browseId")
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    LoaderScreen(show = songs.isEmpty())

    var albumPage by persist<AlbumPage?>("album/$browseId/albumPage")

    LaunchedEffect(Unit) {
        Database.album(browseId).collect { currentAlbum ->
            album = currentAlbum
            CoroutineScope(Dispatchers.IO).launch {
                if (albumPage == null)
                    EnvironmentExt.getAlbum(browseId)
                        .onSuccess { currentAlbumPage ->
                            albumPage = currentAlbumPage
                            Database.upsert(
                                Album(
                                    id = browseId,
                                    title = album?.title ?: currentAlbumPage.album.title,
                                    thumbnailUrl = if (album?.thumbnailUrl?.startsWith(MODIFIED_PREFIX) == true)
                                        album?.thumbnailUrl else currentAlbumPage.album.thumbnail?.url,
                                    year = currentAlbumPage.album.year,
                                    authorsText = if (album?.authorsText?.startsWith(MODIFIED_PREFIX) == true)
                                        album?.authorsText
                                    else currentAlbumPage.album.authors?.joinToString(", ") { it.name ?: "" },
                                    shareUrl = currentAlbumPage.url,
                                    timestamp = System.currentTimeMillis(),
                                    bookmarkedAt = album?.bookmarkedAt,
                                    isYoutubeAlbum = album?.isYoutubeAlbum == true
                                ),
                                currentAlbumPage.songs.distinct()
                                    .map(Environment.SongItem::asMediaItem)
                                    .onEach(Database::insert)
                                    .mapIndexed { position, mediaItem ->
                                        SongAlbumMap(songId = mediaItem.mediaId, albumId = browseId, position = position)
                                    }
                            )
                        }
                        .onFailure { Timber.e("AlbumScreen error ${it.stackTraceToString()}") }
            }
        }
    }

    fun update() {
        if (!isNetworkConnected(context)) return
        runBlocking(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                Database.asyncTransaction {
                    Database.upsert(
                        Album(
                            id = browseId,
                            title = if (album?.title?.startsWith(MODIFIED_PREFIX) == true) album?.title else albumPage?.album?.title,
                            thumbnailUrl = if (album?.thumbnailUrl?.startsWith(MODIFIED_PREFIX) == true) album?.thumbnailUrl else albumPage?.album?.thumbnail?.url,
                            year = albumPage?.album?.year,
                            authorsText = if (album?.authorsText?.startsWith(MODIFIED_PREFIX) == true) album?.authorsText
                            else albumPage?.album?.authors?.joinToString("") { it.name ?: "" },
                            shareUrl = albumPage?.url,
                            timestamp = System.currentTimeMillis(),
                            bookmarkedAt = album?.bookmarkedAt,
                            isYoutubeAlbum = album?.isYoutubeAlbum == true
                        ),
                        albumPage?.songs?.distinct()
                            ?.map(Environment.SongItem::asMediaItem)
                            ?.onEach(Database::insert)
                            ?.mapIndexed { position, mediaItem ->
                                SongAlbumMap(songId = mediaItem.mediaId, albumId = browseId, position = position)
                            } ?: emptyList()
                    )
                }
            }
        }
    }

    var refreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()
    fun refresh() {
        if (refreshing) return
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            update()
            delay(500)
            refreshing = false
        }
    }

    LaunchedEffect(Unit) {
        Database.albumSongs(browseId).collect {
            songs = if (parentalControlEnabled)
                it.filter { s -> !s.title.startsWith(EXPLICIT_PREFIX) || s.mediaId?.isNotEmpty() == true }
            else it
        }
    }
    LaunchedEffect(Unit) { Database.album(browseId).collect { album = it } }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailAlbumSizeDp = Dimensions.thumbnails.album
    val thumbnailAlbumSizePx = thumbnailAlbumSizeDp.px
    val lazyListState = rememberLazyListState()

    // Parallax: track scroll offset for the header image
    val headerScrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0)
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            else Float.MAX_VALUE
        }
    }

    var listMediaItems = remember { mutableListOf<MediaItem>() }
    var selectItems by remember { mutableStateOf(false) }
    var showSelectDialog by remember { mutableStateOf(false) }
    var showDialogChangeAlbumTitle by remember { mutableStateOf(false) }
    var showDialogChangeAlbumAuthors by remember { mutableStateOf(false) }
    var showDialogChangeAlbumCover by remember { mutableStateOf(false) }
    var isCreatingNewPlaylist by rememberSaveable { mutableStateOf(false) }

    var totalPlayTimes = 0L
    songs.forEach { totalPlayTimes += it.durationText?.let { durationTextToMillis(it) }?.toLong() ?: 0 }

    var position by remember { mutableIntStateOf(0) }
    var scrollToNowPlaying by remember { mutableStateOf(false) }
    var nowPlayingItem by remember { mutableIntStateOf(-1) }
    val hapticFeedback = LocalHapticFeedback.current

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showDialogChangeAlbumTitle)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumTitle = false },
            title = stringResource(R.string.update_title),
            value = album?.title.toString(),
            placeholder = stringResource(R.string.title),
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumTitle(browseId, it) } },
            prefix = MODIFIED_PREFIX
        )
    if (showDialogChangeAlbumAuthors)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumAuthors = false },
            title = stringResource(R.string.update_authors),
            value = album?.authorsText.toString(),
            placeholder = stringResource(R.string.authors),
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumAuthors(browseId, it) } },
            prefix = MODIFIED_PREFIX
        )
    if (showDialogChangeAlbumCover)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumCover = false },
            title = stringResource(R.string.update_cover),
            value = album?.thumbnailUrl.toString(),
            placeholder = stringResource(R.string.cover),
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumCover(browseId, it) } },
            prefix = MODIFIED_PREFIX
        )
    if (isCreatingNewPlaylist)
        InputTextDialog(
            onDismiss = { isCreatingNewPlaylist = false },
            title = stringResource(R.string.new_playlist),
            value = "",
            placeholder = stringResource(R.string.new_playlist),
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { insert(Playlist(name = it)) } }
        )

    var isViewingQueues by remember { mutableStateOf(false) }
    if (isViewingQueues)
        QueuesDialog(
            onSelect = { binder?.player?.enqueue(songs.map(Song::asMediaItem)) },
            onDismiss = { isViewingQueues = false }
        )
    if (showSelectDialog)
        SelectorDialog(
            title = stringResource(R.string.enqueue),
            onDismiss = { showSelectDialog = false },
            values = listOf(
                Info("a", stringResource(R.string.enqueue_all)),
                Info("s", stringResource(R.string.enqueue_selected))
            ),
            onValueSelected = {
                if (it == "a") isViewingQueues = true else selectItems = true
                showSelectDialog = false
            }
        )

    LaunchedEffect(scrollToNowPlaying) {
        if (scrollToNowPlaying) lazyListState.animateScrollToItem(nowPlayingItem, 1)
        scrollToNowPlaying = false
    }

    var translateEnabled by remember { mutableStateOf(false) }
    val translator = Translator(CustomHttpClient.okHttpClient)
    val languageDestination = languageDestination()
    var readMore by remember { mutableStateOf(false) }
    var showFastShare by remember { mutableStateOf(false) }
    var showDirectFastShare by remember { mutableStateOf(false) }

    FastShare(
        showFastShare,
        showLinks = !showDirectFastShare,
        showShareWith = !showDirectFastShare,
        onDismissRequest = { showFastShare = false; showDirectFastShare = false },
        content = album ?: return
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────────────────────────────────
    LayoutWithAdaptiveThumbnail(
        thumbnailLandscapeContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(.4f)
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = album?.thumbnailUrl?.toThumbnail(1200),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(
                            top = WindowInsets.systemBars.asPaddingValues()
                                .calculateTopPadding() + Dimensions.fadeSpacingTop,
                            bottom = Dimensions.fadeSpacingBottom
                        )
                )
                if (album?.isYoutubeAlbum == true)
                    Image(
                        painter = painterResource(R.drawable.internet),
                        colorFilter = ColorFilter.tint(Color.Red.copy(0.75f).compositeOver(Color.White)),
                        modifier = Modifier.size(40.dp).padding(5.dp).offset(10.dp, 10.dp),
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
            }
        }
    ) {
        PullToRefreshBox(refreshing = refreshing, onRefresh = { refresh() }) {
            Box(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                LazyListContainer(state = lazyListState) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .background(colorPalette().background0)
                            .fillMaxSize()
                    ) {

                        // ── HEADER ────────────────────────────────────────────
                        item(key = "header") {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (album != null) {
                                    if (!isLandscape) {
                                        // Blurred background layer (colour extracted from art)
                                        Box {
                                            AsyncImage(
                                                model = album?.thumbnailUrl?.toThumbnail(120),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .blur(60.dp)
                                                    .graphicsLayer { alpha = 0.55f }
                                            )
                                            // Gradient overlay: blurred art fades into background
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                colorPalette().background0.copy(alpha = 0.6f),
                                                                colorPalette().background0
                                                            ),
                                                            startY = 0f,
                                                            endY = Float.POSITIVE_INFINITY
                                                        )
                                                    )
                                            )
                                        }

                                        // Sharp album art (centered, rounded, with parallax + shadow)
                                        val parallaxOffset = if (headerScrollOffset != Float.MAX_VALUE)
                                            headerScrollOffset * 0.25f else 0f

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 56.dp, bottom = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Glow shadow behind art
                                            Box(
                                                modifier = Modifier
                                                    .size(220.dp)
                                                    .graphicsLayer { translationY = parallaxOffset }
                                                    .drawBehind {
                                                        drawCircle(
                                                            color = Color.Black.copy(alpha = 0.45f),
                                                            radius = size.minDimension * 0.52f,
                                                            center = center.copy(y = center.y + 18.dp.toPx())
                                                        )
                                                    }
                                            )
//                                            AsyncImage(
//                                                model = album?.thumbnailUrl?.resize(1200, 1200),
//                                                contentDescription = null,
//                                                contentScale = ContentScale.Crop,
//                                                modifier = Modifier
//                                                    .graphicsLayer { translationY = parallaxOffset }
//                                                    .clip(RoundedCornerShape(16.dp))
//                                            )
                                            if (album?.isYoutubeAlbum == true) {
                                                Image(
                                                    painter = painterResource(R.drawable.internet),
                                                    colorFilter = ColorFilter.tint(
                                                        Color.Red.copy(0.9f).compositeOver(Color.White)
                                                    ),
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset((-8).dp, 8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(0.4f))
                                                        .padding(4.dp),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }

                                    // Share button (top-right)
                                    HeaderIconButton(
                                        icon = R.drawable.share_social,
                                        color = colorPalette().text,
                                        iconSize = 24.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 8.dp, end = 8.dp),
                                        onClick = { showFastShare = true }
                                    )

                                    // Album title — animated entrance
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .padding(top = if (isLandscape) 0.dp else 0.dp)
                                            .align(Alignment.BottomCenter)
                                    ) {
                                        AutoResizeText(
                                            text = cleanPrefix(album?.title ?: ""),
                                            style = typography().l.semiBold,
                                            fontSizeRange = FontSizeRange(26.sp, 34.sp),
                                            fontWeight = typography().l.semiBold.fontWeight,
                                            fontFamily = typography().l.semiBold.fontFamily,
                                            color = typography().l.semiBold.color,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .applyIf(!disableScrollingText) {
                                                    basicMarquee(iterations = Int.MAX_VALUE)
                                                }
                                        )

                                        // Author text
                                        album?.authorsText?.let { authors ->
                                            BasicText(
                                                text = cleanPrefix(authors),
                                                style = typography().s.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                            )
                                        }

                                        // FastPlay buttons just below title
                                        FastPlayActionsBar(
                                            modifier = Modifier
                                                .fillMaxWidth(0.55f)
                                                .padding(top = 16.dp, bottom = 8.dp),
                                            onPlayNowClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(
                                                    songs.filter { it.likedAt != -1L }.map(Song::asMediaItem)
                                                )
                                            },
                                            onShufflePlayClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(
                                                    songs.filter { it.likedAt != -1L }.shuffled().map(Song::asMediaItem)
                                                )
                                            }
                                        )
                                    }

                                } else {
                                    // Loading shimmer
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4f / 3)
                                    ) {
                                        ShimmerHost {
                                            AlbumItemPlaceholder(thumbnailSizeDp = 200.dp, alternative = true)
                                            BasicText(
                                                text = stringResource(R.string.info_wait_it_may_take_a_few_minutes),
                                                style = typography().xs.medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── ALBUM META (year · songs · duration) ──────────────
                        if (album?.year != null && songs.isNotEmpty())
                            item(key = "infoAlbum") {
                                AnimatedVisibility(
                                    visible = songs.isNotEmpty(),
                                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = colorPalette().background1,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            BasicText(
                                                text = "${album?.year}  ·  ${songs.size} ${stringResource(R.string.songs)}  ·  ${formatAsTime(totalPlayTimes)}",
                                                style = typography().xs.medium,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                        // ── ACTIONS BAR ───────────────────────────────────────
                        item(key = "actions", contentType = 0) {
                            AnimatedVisibility(
                                visible = album != null,
                                enter = fadeIn(tween(500, delayMillis = 100)) +
                                        slideInVertically(tween(500, delayMillis = 100)) { it / 2 }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 12.dp)
                                ) {
                                    // Bookmark
                                    ActionPillButton(
                                        icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline else R.drawable.bookmark,
                                        active = album?.bookmarkedAt != null,
                                        activeColor = colorPalette().accent,
                                        tint = colorPalette().accent,
                                        onClick = {
                                            if (isYtSyncEnabled() && !isNetworkConnected(context)) {
                                                SmartMessage(context.resources.getString(R.string.no_connection), context = context, type = PopupType.Error)
                                            } else {
                                                val bookmarkedAt = if (album?.bookmarkedAt == null) System.currentTimeMillis() else null
                                                Database.asyncTransaction { album?.copy(bookmarkedAt = bookmarkedAt)?.let(::update) }
                                                if (isYtSyncEnabled())
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        if (bookmarkedAt == null)
                                                            albumPage?.album?.playlistId?.let {
                                                                EnvironmentExt.removelikePlaylistOrAlbum(it)
                                                                Database.asyncTransaction { album?.let { update(it.copy(isYoutubeAlbum = false)) } }
                                                            }
                                                        else
                                                            albumPage?.album?.playlistId?.let {
                                                                EnvironmentExt.likePlaylistOrAlbum(it)
                                                                if (album != null)
                                                                    Database.asyncTransaction { album?.let { update(it.copy(isYoutubeAlbum = false)) } }
                                                            }
                                                    }
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(context.resources.getString(R.string.info_bookmark_album), context = context)
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // Shuffle
                                    ActionPillButton(
                                        icon = R.drawable.shuffle,
                                        enabled = songs.any { it.likedAt != -1L },
                                        tint = colorPalette().text,
                                        onClick = {
                                            if (songs.any { it.likedAt != -1L }) {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(
                                                    songs.filter { it.likedAt != -1L }.shuffled().map(Song::asMediaItem)
                                                )
                                            } else {
                                                SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                            }
                                        },
                                        onLongClick = { SmartMessage(context.resources.getString(R.string.info_shuffle), context = context) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // Radio
                                    ActionPillButton(
                                        icon = R.drawable.radio,
                                        enabled = songs.any { it.likedAt != -1L },
                                        tint = colorPalette().text,
                                        onClick = {
                                            if (songs.any { it.likedAt != -1L }) {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem))
                                                binder?.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = songs.first { it.likedAt != -1L }.id))
                                            } else {
                                                SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                            }
                                        },
                                        onLongClick = { SmartMessage(context.resources.getString(R.string.info_start_radio), context = context) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // Locate now playing
                                    ActionPillButton(
                                        icon = R.drawable.locate,
                                        enabled = songs.isNotEmpty(),
                                        tint = colorPalette().text,
                                        onClick = {
                                            nowPlayingItem = -1
                                            scrollToNowPlaying = false
                                            songs.forEachIndexed { index, song ->
                                                if (song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId)
                                                    nowPlayingItem = index
                                            }
                                            if (nowPlayingItem > -1) scrollToNowPlaying = true
                                        },
                                        onLongClick = { SmartMessage(context.resources.getString(R.string.info_find_the_song_that_is_playing), context = context) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // Download / share
                                    ActionPillButton(
                                        icon = R.drawable.get_app,
                                        enabled = songs.isNotEmpty(),
                                        tint = colorPalette().text,
                                        onClick = { showFastShare = true; showDirectFastShare = true },
                                        onLongClick = { SmartMessage(context.resources.getString(R.string.share_with_external_app), context = context) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // More (ellipsis)
                                    ActionPillButton(
                                        icon = R.drawable.ellipsis_horizontal,
                                        enabled = songs.isNotEmpty(),
                                        tint = colorPalette().text,
                                        onClick = {
                                            menuState.display {
                                                album?.let { it ->
                                                    AlbumsItemMenu(
                                                        navController = navController,
                                                        onDismiss = menuState::hide,
                                                        onSelectUnselect = {
                                                            selectItems = !selectItems
                                                            if (!selectItems) listMediaItems.clear()
                                                        },
                                                        onChangeAlbumTitle = {
                                                            if (album?.isYoutubeAlbum == true)
                                                                SmartMessage(context.resources.getString(R.string.cant_rename_Saved_albums), type = PopupType.Error, context = context)
                                                            else showDialogChangeAlbumTitle = true
                                                        },
                                                        onChangeAlbumAuthors = { showDialogChangeAlbumAuthors = true },
                                                        onChangeAlbumCover = { showDialogChangeAlbumCover = true },
                                                        album = it,
                                                        onPlayNext = {
                                                            if (listMediaItems.isEmpty()) {
                                                                if (songs.any { it.likedAt != -1L })
                                                                    binder?.player?.addNext(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem), context, selectedQueue ?: defaultQueue())
                                                                else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                                            } else {
                                                                binder?.player?.addNext(listMediaItems, context, selectedQueue ?: defaultQueue())
                                                                listMediaItems.clear(); selectItems = false
                                                            }
                                                        },
                                                        onEnqueue = {
                                                            if (listMediaItems.isEmpty()) {
                                                                if (songs.any { it.likedAt != -1L })
                                                                    binder?.player?.enqueue(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem), context)
                                                                else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                                            } else {
                                                                binder?.player?.enqueue(listMediaItems, context)
                                                                listMediaItems.clear(); selectItems = false
                                                            }
                                                        },
                                                        onAddToPlaylist = { playlistPreview ->
                                                            position = playlistPreview.songCount.minus(1) ?: 0
                                                            if (position > 0) position++ else position = 0
                                                            if (listMediaItems.isEmpty()) {
                                                                if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                                    songs.forEachIndexed { index, song ->
                                                                        Database.asyncTransaction {
                                                                            insert(song.asMediaItem)
                                                                            insert(SongPlaylistMap(songId = song.asMediaItem.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default())
                                                                        }
                                                                    }
                                                                } else {
                                                                    CoroutineScope(Dispatchers.IO).launch {
                                                                        EnvironmentExt.addPlaylistToPlaylist(cleanPrefix(playlistPreview.playlist.browseId ?: ""), cleanPrefix(albumPage?.album?.playlistId ?: ""))
                                                                            .onSuccess {
                                                                                songs.forEachIndexed { index, song ->
                                                                                    Database.asyncTransaction {
                                                                                        insert(song.asMediaItem)
                                                                                        insert(SongPlaylistMap(songId = song.asMediaItem.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default())
                                                                                    }
                                                                                }
                                                                            }
                                                                    }
                                                                }
                                                            } else {
                                                                if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                                    listMediaItems.forEachIndexed { index, song ->
                                                                        Database.asyncTransaction {
                                                                            insert(song)
                                                                            insert(SongPlaylistMap(songId = song.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default())
                                                                        }
                                                                    }
                                                                } else {
                                                                    CoroutineScope(Dispatchers.IO).launch {
                                                                        addToYtPlaylist(playlistPreview.playlist.id, position, cleanPrefix(playlistPreview.playlist.browseId ?: ""), listMediaItems)
                                                                    }
                                                                }
                                                                listMediaItems.clear(); selectItems = false
                                                            }
                                                        },
                                                        onGoToPlaylist = {
                                                            onNavigateTo()
                                                            navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                                        },
                                                        onAddToFavourites = {
                                                            if (!isNetworkConnected(appContext()) && isYtSyncEnabled()) {
                                                                SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
                                                            } else if (!isYtSyncEnabled()) {
                                                                songs.forEach { song -> mediaItemSetLiked(song.asMediaItem) }
                                                            } else {
                                                                val totalSongsToLike = songs.filter { it.likedAt in listOf(-1L, null) }
                                                                CoroutineScope(Dispatchers.IO).launch { addToYtLikedSongs(totalSongsToLike.map { it.asMediaItem }) }
                                                            }
                                                        },
                                                        disableScrollingText = disableScrollingText,
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        // ── DESCRIPTION ───────────────────────────────────────
                        albumPage?.description?.let { description ->
                            item(key = "albumInfo") {
                                val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                                Title(
                                    title = stringResource(R.string.information),
                                    icon = if (readMore) R.drawable.chevron_up else R.drawable.chevron_down,
                                    onClick = { readMore = !readMore },
                                )

                                // Animated expand/collapse with gradient fade when collapsed
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 8.dp)
                                ) {
                                    var translatedText by remember { mutableStateOf("") }
                                    val nonTranslatedText by remember {
                                        mutableStateOf(
                                            if (attributionsIndex == -1) description
                                            else description.substring(0, attributionsIndex)
                                        )
                                    }
                                    if (translateEnabled) {
                                        LaunchedEffect(Unit) {
                                            val result = withContext(Dispatchers.IO) {
                                                try { translator.translate(nonTranslatedText, languageDestination, Language.AUTO).translatedText }
                                                catch (e: Exception) { e.printStackTrace() }
                                            }
                                            translatedText = if (result.toString() == "kotlin.Unit") "" else result.toString()
                                        }
                                    } else translatedText = nonTranslatedText

                                    val displayText = if (readMore) translatedText
                                    else translatedText.take(minOf(translatedText.length, 120)).plus("…")

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { readMore = !readMore }
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            BasicText(
                                                text = "\u201C",
                                                style = typography().xxl.semiBold,
                                                modifier = Modifier
                                                    .offset(y = (-8).dp)
                                                    .align(Alignment.Top)
                                            )
                                            BasicText(
                                                text = displayText,
                                                style = typography().xxs.secondary.align(TextAlign.Justify),
                                                modifier = Modifier
                                                    .padding(horizontal = 8.dp)
                                                    .weight(1f)
                                            )
                                            BasicText(
                                                text = "\u201E",
                                                style = typography().xxl.semiBold,
                                                modifier = Modifier
                                                    .offset(y = 4.dp)
                                                    .align(Alignment.Bottom)
                                            )
                                        }
                                        // Gradient fade when collapsed
                                        if (!readMore) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                colorPalette().background0
                                                            )
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                }

                                if (attributionsIndex != -1) {
                                    BasicText(
                                        text = stringResource(R.string.from_wikipedia_cca),
                                        style = typography().xxs.color(colorPalette().textDisabled).align(TextAlign.Start),
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 16.dp)
                                    )
                                }
                            }
                        }

                        // ── SONGS TITLE ───────────────────────────────────────
                        item(key = "songsTitle") {
                            Title(title = stringResource(R.string.songs))
                        }

                        // ── SONG ITEMS (staggered entrance) ───────────────────
                        itemsIndexed(
                            items = songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            // Staggered slide-in animation per item
                            val visibleState = remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(minOf(index * 40L, 400L))
                                visibleState.value = true
                            }
                            AnimatedVisibility(
                                visible = visibleState.value,
                                enter = fadeIn(tween(300)) +
                                        slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 3 }
                            ) {
                                SwipeablePlaylistItem(
                                    mediaItem = song.asMediaItem,
                                    onPlayNext = { binder?.player?.addNext(song.asMediaItem, queue = selectedQueue ?: defaultQueue()) },
                                    onEnqueue = { binder?.player?.enqueue(song.asMediaItem, queue = it) }
                                ) {
                                    val checkedState = rememberSaveable { mutableStateOf(false) }
                                    SongItem(
                                        mediaItem = song.asMediaItem,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        thumbnailContent = {
                                            BasicText(
                                                text = "${index + 1}",
                                                style = typography().s.semiBold.center.color(colorPalette().textDisabled),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .width(thumbnailSizeDp)
                                                    .align(Alignment.Center)
                                            )
                                            NowPlayingSongIndicator(song.asMediaItem.mediaId, binder?.player)
                                        },
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            navController = navController,
                                                            onDismiss = { menuState.hide() },
                                                            mediaItem = song.asMediaItem,
                                                            onInfo = {
                                                                onNavigateTo()
                                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}")
                                                            },
                                                            disableScrollingText = disableScrollingText,
                                                        )
                                                    }
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onClick = {
                                                    if (!selectItems) {
                                                        if (song.likedAt != -1L) {
                                                            val mediaItems = songs.filter { it.likedAt != -1L }.map(Song::asMediaItem)
                                                            binder?.stopRadio()
                                                            binder?.player?.forcePlayAtIndex(mediaItems, mediaItems.indexOfFirst { it.mediaId == song.id })
                                                        } else {
                                                            SmartMessage(globalContext().resources.getString(R.string.disliked_this_song), type = PopupType.Error, context = context)
                                                        }
                                                    } else checkedState.value = !checkedState.value
                                                }
                                            ),
                                        trailingContent = {
                                            if (selectItems)
                                                Checkbox(
                                                    checked = checkedState.value,
                                                    onCheckedChange = {
                                                        checkedState.value = it
                                                        if (it) listMediaItems.add(song.asMediaItem)
                                                        else listMediaItems.remove(song.asMediaItem)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = colorPalette().accent,
                                                        uncheckedColor = colorPalette().text
                                                    ),
                                                    modifier = Modifier.scale(0.7f)
                                                )
                                            else checkedState.value = false
                                        },
                                    )
                                }
                            }
                        }

                        // ── ALTERNATIVE VERSIONS ──────────────────────────────
                        item(key = "alternateVersionsTitle") {
                            Title(title = stringResource(R.string.album_alternative_versions))
                        }
                        item(key = "alternateVersions") {
                            ItemsList(
                                tag = "album/$browseId/alternatives",
                                headerContent = {},
                                initialPlaceholderCount = 1,
                                continuationPlaceholderCount = 1,
                                emptyItemsText = stringResource(R.string.album_no_alternative_version),
                                itemsPageProvider = albumPage?.let {
                                    ({
                                        Result.success(Environment.ItemsPage(items = albumPage?.otherVersions, continuation = null))
                                    })
                                } ?: {
                                    Result.success(Environment.ItemsPage(items = emptyList(), continuation = null))
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        alternative = true,
                                        album = album,
                                        thumbnailSizePx = thumbnailAlbumSizePx,
                                        thumbnailSizeDp = thumbnailAlbumSizeDp,
                                        modifier = Modifier.clickable {
                                            onNavigateTo()
                                            navController.navigate(route = "${NavRoutes.album.name}/${album.key}")
                                        },
                                        disableScrollingText = disableScrollingText
                                    )
                                },
                                itemPlaceholderContent = { AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp) }
                            )
                        }

                        item(key = "bottom") {
                            Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                        }

                        if (songs.isEmpty()) {
                            item(key = "loading") {
                                ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                                    repeat(1) { AlbumItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.album) }
                                    repeat(4) { SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song) }
                                }
                            }
                        }
                    }
                }

                // ── FAB ───────────────────────────────────────────────────────
                val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
                if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                    MultiFloatingActionsContainer(
                        iconId = R.drawable.shuffle,
                        onClick = {
                            if (songs.any { it.likedAt != -1L }) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    songs.filter { it.likedAt != -1L }.shuffled().map(Song::asMediaItem)
                                )
                            } else {
                                SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                            }
                        },
                        onClickSettings = onSettingsClick,
                        onClickSearch = onSearchClick
                    )
            }
        }
    }
}
