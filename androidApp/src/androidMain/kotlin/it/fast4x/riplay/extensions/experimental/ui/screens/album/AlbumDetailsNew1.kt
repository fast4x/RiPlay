package it.fast4x.riplay.extensions.experimental.ui.screens.album

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.AlbumPage
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Info
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.appviewmodel.isNetworkConnected
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import it.fast4x.riplay.ui.components.AlbumPillIconButton
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AlbumSectionTitle
import it.fast4x.riplay.ui.components.themed.AlbumsItemMenu
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.ItemsList
import it.fast4x.riplay.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.QueuesDialog
import it.fast4x.riplay.ui.components.themed.SelectorDialog
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtLikedSongs
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.mediaItemSetLiked
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

@ExperimentalSerializationApi
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun AlbumDetailsNew1(
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
    val parentalControlEnabled by rememberPreference(PARENTAL_CONTROL_ENABLED.key, false)
    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)

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

    // Sticky header trigger
    val showStickyTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

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
    var nowPlayingItem by remember { mutableStateOf(-1) }
    val hapticFeedback = LocalHapticFeedback.current
    var translateEnabled by remember { mutableStateOf(false) }
    val translator = Translator(CustomHttpClient.okHttpClient)
    val languageDestination = languageDestination()
    var readMore by remember { mutableStateOf(false) }
    var showFastShare by remember { mutableStateOf(false) }
    var showDirectFastShare by remember { mutableStateOf(false) }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showDialogChangeAlbumTitle)
        InputTextDialog(onDismiss = { showDialogChangeAlbumTitle = false },
            title = stringResource(R.string.update_title), value = album?.title.toString(),
            placeholder = stringResource(R.string.title), prefix = MODIFIED_PREFIX,
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumTitle(browseId, it) } })
    if (showDialogChangeAlbumAuthors)
        InputTextDialog(onDismiss = { showDialogChangeAlbumAuthors = false },
            title = stringResource(R.string.update_authors), value = album?.authorsText.toString(),
            placeholder = stringResource(R.string.authors), prefix = MODIFIED_PREFIX,
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumAuthors(browseId, it) } })
    if (showDialogChangeAlbumCover)
        InputTextDialog(onDismiss = { showDialogChangeAlbumCover = false },
            title = stringResource(R.string.update_cover), value = album?.thumbnailUrl.toString(),
            placeholder = stringResource(R.string.cover), prefix = MODIFIED_PREFIX,
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { updateAlbumCover(browseId, it) } })
    if (isCreatingNewPlaylist)
        InputTextDialog(onDismiss = { isCreatingNewPlaylist = false },
            title = stringResource(R.string.new_playlist), value = "",
            placeholder = stringResource(R.string.new_playlist),
            setValue = { if (it.isNotEmpty()) Database.asyncTransaction { insert(Playlist(name = it)) } })

    var isViewingQueues by remember { mutableStateOf(false) }
    if (isViewingQueues)
        QueuesDialog(onSelect = { binder?.player?.enqueue(songs.map(Song::asMediaItem)) },
            onDismiss = { isViewingQueues = false })

    if (showSelectDialog)
        SelectorDialog(title = stringResource(R.string.enqueue),
            onDismiss = { showSelectDialog = false },
            values = listOf(Info("a", stringResource(R.string.enqueue_all)), Info("s", stringResource(R.string.enqueue_selected))),
            onValueSelected = { if (it == "a") isViewingQueues = true else selectItems = true; showSelectDialog = false })

    LaunchedEffect(scrollToNowPlaying) {
        if (scrollToNowPlaying) lazyListState.scrollToItem(nowPlayingItem, 1)
        scrollToNowPlaying = false
    }

    FastShare(showFastShare, showLinks = !showDirectFastShare, showShareWith = !showDirectFastShare,
        onDismissRequest = { showFastShare = false; showDirectFastShare = false },
        content = album ?: return)


    LayoutWithAdaptiveThumbnail(thumbnailLandscapeContent = {}) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {

            // ── Sticky title header ──────────────────────────────────────────
            AnimatedVisibility(
                visible = showStickyTitle,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorPalette().background0,
                                    colorPalette().background0.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(
                            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                            bottom = 8.dp
                        )
                ) {
                    BasicText(
                        text = cleanPrefix(album?.title ?: ""),
                        style = typography().m.semiBold.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 56.dp)
                    )
                    AlbumPillIconButton(
                        icon = R.drawable.share_social,
                        size = 20,
                        onClick = { showFastShare = true },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    )
                }
            }

            LazyListContainer(state = lazyListState) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .background(colorPalette().background0)
                        .fillMaxSize()
                ) {

                    // ── HERO HEADER ─────────────────────────────────────────
                    item(key = "header") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (album != null) {
                                if (!isLandscape) {
                                    Box {
                                        AsyncImage(
                                            model = album?.thumbnailUrl?.toThumbnail(1200),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(380.dp)
                                        )
                                        // Gradiente drammatico bottom-heavy
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(380.dp)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colorStops = arrayOf(
                                                            0f to Color.Transparent,
                                                            0.45f to Color.Transparent,
                                                            0.78f to colorPalette().background0.copy(alpha = 0.7f),
                                                            1f to colorPalette().background0
                                                        )
                                                    )
                                                )
                                        )
                                        // Badge YouTube album
                                        if (album?.isYoutubeAlbum == true) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .align(Alignment.TopStart)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.internet),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(
                                                            Color.Red.copy(0.85f).compositeOver(Color.White)
                                                        ),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    BasicText(
                                                        text = "YouTube",
                                                        style = typography().xxs.semiBold.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Share button — scompare quando sticky attivo
                                AnimatedVisibility(
                                    visible = !showStickyTitle,
                                    enter = fadeIn(), exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(
                                            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 4.dp,
                                            end = 8.dp
                                        )
                                ) {
                                    AlbumPillIconButton(
                                        icon = R.drawable.share_social,
                                        size = 22,
                                        onClick = { showFastShare = true },
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                    )
                                }

                                // Titolo + autore + FastPlay sopra il gradiente
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                ) {
                                    AutoResizeText(
                                        text = cleanPrefix(album?.title ?: ""),
                                        style = typography().l.semiBold.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1).sp,
                                            color = colorPalette().text
                                        ),
                                        fontSizeRange = FontSizeRange(34.sp, 48.sp),
                                        fontWeight = FontWeight.Black,
                                        fontFamily = typography().l.semiBold.fontFamily,
                                        color = colorPalette().text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(horizontal = 24.dp)
                                            .applyIf(!disableScrollingText) {
                                                basicMarquee(iterations = Int.MAX_VALUE)
                                            }
                                    )
                                    // Autore in accent color
                                    album?.authorsText?.let { authors ->
                                        BasicText(
                                            text = cleanPrefix(authors),
                                            style = typography().xs.semiBold.copy(
                                                color = colorPalette().accent,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(horizontal = 24.dp)
                                                .padding(top = 2.dp)
                                        )
                                    }
                                    FastPlayActionsBar(
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .padding(top = 10.dp),
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
                                // Placeholder shimmer
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3)
                                ) {
                                    ShimmerHost {
                                        AlbumItemPlaceholder(thumbnailSizeDp = 200.dp, alternative = true)
                                        BasicText(
                                            text = stringResource(R.string.info_wait_it_may_take_a_few_minutes),
                                            style = typography().xs.medium, maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── INFO PILL: anno · brani · durata ────────────────────
                    if (album?.year != null && songs.isNotEmpty())
                        item(key = "infoAlbum") {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(colorPalette().background1)
                                        .border(0.5.dp, colorPalette().textDisabled.copy(alpha = 0.2f), RoundedCornerShape(50))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    BasicText(
                                        text = "${album?.year}  ·  ${songs.size} ${stringResource(R.string.songs)}  ·  ${formatAsTime(totalPlayTimes)}",
                                        style = typography().xs.medium.copy(color = colorPalette().textSecondary),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                    // ── AZIONI: capsula flottante ────────────────────────────
                    item(key = "actions", contentType = 0) {
                        val hasSongs = songs.any { it.likedAt != -1L }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(colorPalette().background1)
                                .border(0.5.dp, colorPalette().textDisabled.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            // Bookmark
                            val isBookmarked = album?.bookmarkedAt != null
                            AlbumPillIconButton(
                                icon = if (isBookmarked) R.drawable.bookmark else R.drawable.bookmark_outline,
                                active = isBookmarked,
                                onClick = {
                                    if (isYtSyncEnabled() && !isNetworkConnected()) {
                                        SmartMessage(context.resources.getString(R.string.no_connection), context = context, type = PopupType.Error)
                                    } else {
                                        val bookmarkedAt = if (album?.bookmarkedAt == null) System.currentTimeMillis() else null
                                        Database.asyncTransaction { album?.copy(bookmarkedAt = bookmarkedAt)?.let(::update) }
                                        if (isYtSyncEnabled()) CoroutineScope(Dispatchers.IO).launch {
                                            if (bookmarkedAt == null)
                                                albumPage?.album?.playlistId?.let {
                                                    EnvironmentExt.removelikePlaylistOrAlbum(it)
                                                    Database.asyncTransaction { album?.let { update(it.copy(isYoutubeAlbum = false)) } }
                                                }
                                            else albumPage?.album?.playlistId?.let {
                                                EnvironmentExt.likePlaylistOrAlbum(it)
                                                if (album != null) Database.asyncTransaction { album?.let { update(it.copy(isYoutubeAlbum = false)) } }
                                            }
                                        }
                                    }
                                },
                                onLongClick = { SmartMessage(context.resources.getString(R.string.info_bookmark_album), context = context) }
                            )
                            Spacer(Modifier.weight(1f))
                            // Shuffle
                            AlbumPillIconButton(
                                icon = R.drawable.shuffle, enabled = hasSongs,
                                onClick = {
                                    if (hasSongs) { binder?.stopRadio(); binder?.player?.forcePlayFromBeginning(songs.filter { it.likedAt != -1L }.shuffled().map(Song::asMediaItem)) }
                                    else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                },
                                onLongClick = { SmartMessage(context.resources.getString(R.string.info_shuffle), context = context) }
                            )
                            // Radio
                            AlbumPillIconButton(
                                icon = R.drawable.radio, enabled = hasSongs,
                                onClick = {
                                    if (hasSongs) {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem))
                                        binder?.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = songs.first { it.likedAt != -1L }.id))
                                    } else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                },
                                onLongClick = { SmartMessage(context.resources.getString(R.string.info_start_radio), context = context) }
                            )
                            // Locate now playing
                            AlbumPillIconButton(
                                icon = R.drawable.locate, enabled = songs.isNotEmpty(),
                                onClick = {
                                    nowPlayingItem = -1; scrollToNowPlaying = false
                                    songs.forEachIndexed { index, song ->
                                        if (song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId) nowPlayingItem = index
                                    }
                                    if (nowPlayingItem > -1) scrollToNowPlaying = true
                                },
                                onLongClick = { SmartMessage(context.resources.getString(R.string.info_find_the_song_that_is_playing), context = context) }
                            )
                            // Share/download
                            AlbumPillIconButton(
                                icon = R.drawable.get_app, enabled = songs.isNotEmpty(),
                                onClick = { showFastShare = true; showDirectFastShare = true },
                                onLongClick = { SmartMessage(context.resources.getString(R.string.share_with_external_app), context = context) }
                            )
                            // More
                            AlbumPillIconButton(
                                icon = R.drawable.ellipsis_horizontal, enabled = songs.isNotEmpty(),
                                onClick = {
                                    menuState.display {
                                        album?.let { it ->
                                            AlbumsItemMenu(
                                                navController = navController,
                                                onDismiss = menuState::hide,
                                                onSelectUnselect = { selectItems = !selectItems; if (!selectItems) listMediaItems.clear() },
                                                onChangeAlbumTitle = {
                                                    if (album?.isYoutubeAlbum == true) SmartMessage(context.resources.getString(R.string.cant_rename_Saved_albums), type = PopupType.Error, context = context)
                                                    else showDialogChangeAlbumTitle = true
                                                },
                                                onChangeAlbumAuthors = { showDialogChangeAlbumAuthors = true },
                                                onChangeAlbumCover = { showDialogChangeAlbumCover = true },
                                                album = it,
                                                onPlayNext = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (hasSongs) binder?.player?.addNext(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem), context, selectedQueue ?: defaultQueue())
                                                        else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                                    } else { binder?.player?.addNext(listMediaItems, context, selectedQueue ?: defaultQueue()); listMediaItems.clear(); selectItems = false }
                                                },
                                                onEnqueue = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (hasSongs) binder?.player?.enqueue(songs.filter { it.likedAt != -1L }.map(Song::asMediaItem), context)
                                                        else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                                    } else { binder?.player?.enqueue(listMediaItems, context); listMediaItems.clear(); selectItems = false }
                                                },
                                                onAddToPlaylist = { playlistPreview ->
                                                    position = playlistPreview.songCount.minus(1) ?: 0
                                                    if (position > 0) position++ else position = 0
                                                    if (listMediaItems.isEmpty()) {
                                                        if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                            songs.forEachIndexed { index, song ->
                                                                Database.asyncTransaction { insert(song.asMediaItem); insert(SongPlaylistMap(songId = song.asMediaItem.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default()) }
                                                            }
                                                        } else {
                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                EnvironmentExt.addPlaylistToPlaylist(cleanPrefix(playlistPreview.playlist.browseId ?: ""), cleanPrefix(albumPage?.album?.playlistId ?: ""))
                                                                    .onSuccess { songs.forEachIndexed { index, song -> Database.asyncTransaction { insert(song.asMediaItem); insert(SongPlaylistMap(songId = song.asMediaItem.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default()) } } }
                                                            }
                                                        }
                                                    } else {
                                                        if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                            listMediaItems.forEachIndexed { index, song -> Database.asyncTransaction { insert(song); insert(SongPlaylistMap(songId = song.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default()) } }
                                                        } else {
                                                            CoroutineScope(Dispatchers.IO).launch { addToYtPlaylist(playlistPreview.playlist.id, position, cleanPrefix(playlistPreview.playlist.browseId ?: ""), listMediaItems) }
                                                        }
                                                        listMediaItems.clear(); selectItems = false
                                                    }
                                                },
                                                onGoToPlaylist = { onNavigateTo(); navController.navigate("${NavRoutes.localPlaylist.name}/$it") },
                                                onAddToFavourites = {
                                                    if (!isNetworkConnected() && isYtSyncEnabled()) SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
                                                    else if (!isYtSyncEnabled()) songs.forEach { song -> mediaItemSetLiked(song.asMediaItem) }
                                                    else { val totalSongsToLike = songs.filter { it.likedAt in listOf(-1L, null) }; CoroutineScope(Dispatchers.IO).launch { addToYtLikedSongs(totalSongsToLike.map { it.asMediaItem }) } }
                                                },
                                                disableScrollingText = disableScrollingText,
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // ── DESCRIPTION CARD ─────────────────────────────────────
                    albumPage?.description?.let { description ->
                        item(key = "albumInfo") {
                            val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), thickness = 0.5.dp, color = colorPalette().textDisabled.copy(alpha = 0.15f))
                            AlbumSectionTitle(
                                title = stringResource(R.string.information),
                                trailingContent = {
                                    AlbumPillIconButton(icon = if (readMore) R.drawable.chevron_up else R.drawable.chevron_down, size = 18, onClick = { readMore = !readMore })
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp).padding(bottom = 8.dp).fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorPalette().background1)
                                    .border(0.5.dp, colorPalette().textDisabled.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                Box(modifier = Modifier.width(3.dp).fillMaxHeight().align(Alignment.CenterStart)
                                    .background(brush = Brush.verticalGradient(colors = listOf(colorPalette().accent.copy(alpha = 0.4f), colorPalette().accent, colorPalette().accent.copy(alpha = 0.4f)))))

                                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
                                    BasicText(text = "\u201C", style = typography().xxl.semiBold.copy(color = colorPalette().accent.copy(alpha = 0.5f), fontWeight = FontWeight.Black, fontSize = 32.sp), modifier = Modifier.offset(y = (-4).dp))

                                    var translatedText by remember { mutableStateOf("") }
                                    val nonTranslatedText by remember {
                                        mutableStateOf(if (attributionsIndex == -1) description else description.substring(0, attributionsIndex))
                                    }
                                    if (translateEnabled) {
                                        LaunchedEffect(Unit) {
                                            val result = withContext(Dispatchers.IO) {
                                                try { translator.translate(nonTranslatedText, languageDestination, Language.AUTO).translatedText } catch (e: Exception) { e.printStackTrace() }
                                            }
                                            translatedText = if (result.toString() == "kotlin.Unit") "" else result.toString()
                                        }
                                    } else translatedText = nonTranslatedText

                                    val displayText = if (!readMore)
                                        translatedText.take(minOf(100, translatedText.length)).plus("…")
                                    else translatedText

                                    BasicText(
                                        text = displayText,
                                        style = typography().xxs.secondary.align(TextAlign.Justify).copy(lineHeight = 18.sp),
                                        modifier = Modifier.fillMaxWidth().clickable { readMore = !readMore }
                                    )
                                    if (readMore && attributionsIndex != -1) {
                                        Spacer(Modifier.height(8.dp))
                                        BasicText(text = stringResource(R.string.from_wikipedia_cca), style = typography().xxs.color(colorPalette().textDisabled).align(TextAlign.Start))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // ── SONGS SECTION ─────────────────────────────────────────
                    item(key = "songsTitle") {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), thickness = 0.5.dp, color = colorPalette().textDisabled.copy(alpha = 0.12f))
                        AlbumSectionTitle(title = stringResource(R.string.songs))
                    }

                    itemsIndexed(items = songs, key = { _, song -> song.id }) { index, song ->
                        SwipeablePlaylistItem(
                            mediaItem = song.asMediaItem,
                            onPlayNext = { binder?.player?.addNext(song.asMediaItem, queue = selectedQueue ?: defaultQueue()) },
                            onEnqueue = { binder?.player?.enqueue(song.asMediaItem, queue = it) }
                        ) {
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            val isNowPlaying = song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Accent bar sinistra per la traccia in riproduzione
                                if (isNowPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp).height(48.dp).align(Alignment.CenterStart).zIndex(5f)
                                            .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                            .background(brush = Brush.verticalGradient(colors = listOf(colorPalette().accent.copy(alpha = 0.4f), colorPalette().accent, colorPalette().accent.copy(alpha = 0.4f))))
                                    )
                                }
                                SongItem(
                                    mediaItem = song.asMediaItem,
                                    thumbnailSizeDp = thumbnailSizeDp,
                                    thumbnailContent = {
                                        BasicText(
                                            text = "${index + 1}",
                                            style = typography().s.semiBold.center.color(colorPalette().textDisabled),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(thumbnailSizeDp).align(Alignment.Center)
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
                                                        onInfo = { onNavigateTo(); navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}") },
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
                                                    } else SmartMessage(globalContext().resources.getString(R.string.disliked_this_song), type = PopupType.Error, context = context)
                                                } else checkedState.value = !checkedState.value
                                            }
                                        )
                                        .background(if (isNowPlaying) colorPalette().accent.copy(alpha = 0.07f) else colorPalette().background0)
                                        .padding(start = if (isNowPlaying) 4.dp else 0.dp),
                                    trailingContent = {
                                        if (selectItems)
                                            Checkbox(
                                                checked = checkedState.value,
                                                onCheckedChange = { checkedState.value = it; if (it) listMediaItems.add(song.asMediaItem) else listMediaItems.remove(song.asMediaItem) },
                                                colors = CheckboxDefaults.colors(checkedColor = colorPalette().accent, uncheckedColor = colorPalette().text),
                                                modifier = Modifier.scale(0.7f)
                                            )
                                        else checkedState.value = false
                                    }
                                )
                            }
                        }
                    }

                    // ── VERSIONI ALTERNATIVE ──────────────────────────────────
                    item(key = "alternateVersionsTitle") {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), thickness = 0.5.dp, color = colorPalette().textDisabled.copy(alpha = 0.12f))
                        AlbumSectionTitle(title = stringResource(R.string.album_alternative_versions))
                    }

                    item(key = "alternateVersions") {
                        ItemsList(
                            tag = "album/$browseId/alternatives",
                            headerContent = {},
                            initialPlaceholderCount = 1,
                            continuationPlaceholderCount = 1,
                            emptyItemsText = stringResource(R.string.album_no_alternative_version),
                            itemsPageProvider = albumPage?.let {
                                { Result.success(Environment.ItemsPage(items = albumPage?.otherVersions, continuation = null)) }
                            } ?: { Result.success(Environment.ItemsPage(items = emptyList(), continuation = null)) },
                            itemContent = { album ->
                                AlbumItem(
                                    alternative = true, album = album,
                                    thumbnailSizePx = thumbnailAlbumSizePx, thumbnailSizeDp = thumbnailAlbumSizeDp,
                                    modifier = Modifier.clickable { onNavigateTo(); navController.navigate("${NavRoutes.album.name}/${album.key}") },
                                    disableScrollingText = disableScrollingText
                                )
                            },
                            itemPlaceholderContent = { AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp) }
                        )
                    }

                    item(key = "bottom") { Spacer(modifier = Modifier.height(Dimensions.bottomSpacer)) }

                    if (songs.isEmpty()) {
                        item(key = "loading") {
                            ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                                repeat(1) { AlbumItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.album) }
                                repeat(4) { SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song) }
                            }
                        }
                    }
                } // end LazyColumn
            } // end LazyListContainer

            // ── FAB ──────────────────────────────────────────────────────────
            val showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
            if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                MultiFloatingActionsContainer(
                    iconId = R.drawable.shuffle,
                    onClick = {
                        if (songs.any { it.likedAt != -1L }) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(songs.filter { it.likedAt != -1L }.shuffled().map(Song::asMediaItem))
                        } else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                    },
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )

        } // end Box
    } // end LayoutWithAdaptiveThumbnail
} // end AlbumDetails