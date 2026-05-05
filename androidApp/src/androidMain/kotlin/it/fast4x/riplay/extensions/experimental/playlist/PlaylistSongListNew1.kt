package it.fast4x.riplay.extensions.experimental.playlist

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.PlaylistPage
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.commonutils.setLikeState
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.Database.Companion.like
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.PlaylistsItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.adaptiveThumbnailContent
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtLikedSongs
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asPlaylist
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isNetworkConnected
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.mediaItemSetLiked
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

// ── Helper: pill icon button ─────────────────────────────────────────────────
@Composable
private fun PlaylistPillIcon(
    icon: Int,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Int = 22,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = colorPalette().accent
    val tint = when {
        !enabled -> colorPalette().textDisabled
        active   -> accent
        else     -> colorPalette().text
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((size + 14).dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                else
                    Modifier.clickable(enabled = enabled, onClick = onClick)
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size.dp).alpha(if (enabled) 1f else 0.4f)
        )
    }
}


@ExperimentalSerializationApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun PlaylistSongListNew1(
    navController: NavController,
    browseId: String,
) {
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalGlobalSheetState.current
    val selectedQueue = LocalSelectedQueue.current

    var playlistPage by persist<PlaylistPage?>("playlist/$browseId/playlistPage")
    var playlistSongs by persistList<Environment.SongItem>("playlist/$browseId/songs")

    var filter: String? by rememberSaveable { mutableStateOf(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    var isLiked by remember { mutableStateOf(0) }
    var saveCheck by remember { mutableStateOf(false) }
    var translateEnabled by remember { mutableStateOf(false) }
    val translator = Translator(CustomHttpClient.okHttpClient)
    val languageDestination = languageDestination()
    var localPlaylist by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(saveCheck) {
        Database.asyncTransaction {
            localPlaylist = Database.playlistWithBrowseId(browseId.substringAfter("VL"))
        }
    }

    @Composable
    fun checkLike(mediaId: String, song: Environment.SongItem): Boolean {
        LaunchedEffect(Unit, mediaId) {
            withContext(Dispatchers.IO) {
                isLiked = like(mediaId, setLikeState(song.asSong.likedAt))
            }
        }
        return true
    }

    LoaderScreen(show = playlistPage == null)

    LaunchedEffect(Unit, browseId) {
        EnvironmentExt.getPlaylist(browseId).completed()
            .onSuccess {
                playlistPage = it
                playlistSongs = it.songs
                playlistSongs = if (parentalControlEnabled) it.songs.filter { !it.explicit }
                else playlistPage?.songs ?: emptyList()
            }.onFailure {
                println("PlaylistSongList error: ${it.stackTraceToString()}")
            }
    }

    var filterCharSequence: CharSequence = filter.toString()
    if (!filter.isNullOrBlank()) {
        playlistPage?.songs = playlistPage?.songs?.filter { songItem ->
            songItem.asMediaItem.mediaMetadata.title?.contains(filterCharSequence, true) ?: false
                    || songItem.asMediaItem.mediaMetadata.artist?.contains(filterCharSequence, true) ?: false
                    || songItem.asMediaItem.mediaMetadata.albumTitle?.contains(filterCharSequence, true) ?: false
        } ?: emptyList()
    } else playlistPage?.songs = playlistSongs

    var playlistNotLikedSongs by persistList<Environment.SongItem>("")
    var searching by rememberSaveable { mutableStateOf(false) }
    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }
    var thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Light)
    var showYoutubeLikeConfirmDialog by remember { mutableStateOf(false) }
    var totalMinutesToLike by remember { mutableStateOf("") }

    if (showYoutubeLikeConfirmDialog) {
        Database.asyncTransaction {
            playlistNotLikedSongs = playlistSongs.filter { getLikedAt(it.asMediaItem.mediaId) in listOf(-1L, null) }
        }
        totalMinutesToLike = formatAsDuration(playlistNotLikedSongs.size.toLong() * 1000)
        ConfirmationDialog(
            text = "$totalMinutesToLike " + stringResource(R.string.do_you_really_want_to_like_all),
            onDismiss = { showYoutubeLikeConfirmDialog = false },
            onConfirm = {
                showYoutubeLikeConfirmDialog = false
                CoroutineScope(Dispatchers.IO).launch { addToYtLikedSongs(playlistNotLikedSongs.map { it.asMediaItem }) }
            }
        )
    }

    var totalPlayTimes = 0L
    playlistPage?.songs?.forEach { totalPlayTimes += it.durationText?.let { durationTextToMillis(it) }?.toLong() ?: 0 }

    var dislikedSongs by persistList<String>("")
    LaunchedEffect(Unit) { Database.dislikedSongsById().filterNotNull().collect { dislikedSongs = it } }

    if (isImportingPlaylist)
        InputTextDialog(
            onDismiss = { isImportingPlaylist = false },
            title = stringResource(R.string.enter_the_playlist_name),
            value = playlistPage?.playlist?.title ?: "",
            placeholder = "https://........",
            setValue = { text ->
                Database.asyncTransaction {
                    val playlistId = insert(Playlist(name = text, browseId = browseId))
                    playlistPage?.songs
                        ?.map(Environment.SongItem::asMediaItem)
                        ?.onEach(::insert)
                        ?.mapIndexed { index, mediaItem ->
                            SongPlaylistMap(songId = mediaItem.mediaId, playlistId = playlistId, position = index).default()
                        }?.onEach(::insert)
                }
                SmartMessage(context.resources.getString(R.string.done), PopupType.Success, context = context)
            }
        )

    var position by remember { mutableIntStateOf(0) }
    val thumbnailContent = adaptiveThumbnailContent(playlistPage == null, playlistPage?.playlist?.thumbnail?.url)
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showStickyTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val hasSongs = playlistPage?.songs?.any { it.asMediaItem.mediaId !in dislikedSongs } == true

    var showFastShare by remember { mutableStateOf(false) }
    var showDirectFastShare by remember { mutableStateOf(false) }

    FastShare(
        showFastShare, showLinks = !showDirectFastShare, showShareWith = !showDirectFastShare,
        onDismissRequest = { showFastShare = false; showDirectFastShare = false },
        content = playlistPage?.playlist?.asPlaylist ?: return
    )


    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxHeight()
                .fillMaxWidth(if (NavigationBarPosition.Right.isCurrent()) Dimensions.contentWidthRightBar else 1f)
        ) {

            // ── Sticky title header ──────────────────────────────────────────
            AnimatedVisibility(
                visible = showStickyTitle, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(colorPalette().background0, colorPalette().background0.copy(alpha = 0.92f))))
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(), bottom = 8.dp)
                ) {
                    BasicText(
                        text = playlistPage?.playlist?.title ?: "",
                        style = typography().m.semiBold.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp)
                    )
                    PlaylistPillIcon(
                        icon = R.drawable.share_social, size = 20,
                        onClick = { showFastShare = true },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    )
                }
            }

            LazyListContainer(state = lazyListState) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.background(colorPalette().background0).fillMaxSize()
                ) {

                    // ── HERO ────────────────────────────────────────────────
                    item(key = "header") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (playlistPage != null) {
                                if (!isLandscape) {
                                    Box {
                                        AsyncImage(
                                            model = playlistPage?.playlist?.thumbnail?.url?.resize(1200, 1200),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(380.dp)
                                        )
                                        // Gradiente drammatico bottom-heavy
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth().height(380.dp)
                                                .background(Brush.verticalGradient(
                                                    colorStops = arrayOf(
                                                        0f to Color.Transparent,
                                                        0.45f to Color.Transparent,
                                                        0.78f to colorPalette().background0.copy(alpha = 0.7f),
                                                        1f to colorPalette().background0
                                                    )
                                                ))
                                        )
                                        // Badge YouTube playlist
                                        if (localPlaylist?.isYoutubePlaylist == true) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .align(Alignment.TopStart)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Image(
                                                        painter = painterResource(R.drawable.internet),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(Color.Red.copy(0.85f).compositeOver(Color.White)),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    BasicText(text = "YouTube", style = typography().xxs.semiBold.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Share button (scompare con sticky)
                                AnimatedVisibility(
                                    visible = !showStickyTitle, enter = fadeIn(), exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 4.dp, end = 8.dp)
                                ) {
                                    PlaylistPillIcon(
                                        icon = R.drawable.share_social, size = 22,
                                        onClick = { showFastShare = true },
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                    )
                                }

                                // Titolo + info pill + FastPlay
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 12.dp)
                                ) {
                                    AutoResizeText(
                                        text = playlistPage?.playlist?.title ?: "",
                                        style = typography().l.semiBold.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp, color = colorPalette().text),
                                        fontSizeRange = FontSizeRange(34.sp, 48.sp),
                                        fontWeight = FontWeight.Black,
                                        fontFamily = typography().l.semiBold.fontFamily,
                                        color = colorPalette().text,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    FastPlayActionsBar(
                                        modifier = Modifier.fillMaxWidth(0.5f).padding(top = 10.dp),
                                        onPlayNowClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(playlistSongs.map { it.asMediaItem })
                                        },
                                        onShufflePlayClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(playlistSongs.shuffled().map { it.asMediaItem })
                                        }
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3)
                                ) {
                                    ShimmerHost { AlbumItemPlaceholder(thumbnailSizeDp = 200.dp, alternative = true) }
                                }
                            }
                        }
                    }

                    // ── INFO PILL + AZIONI ───────────────────────────────────
                    item(key = "actions", contentType = 0) {
                        if (playlistPage != null) {
                            // Info pill: brani · durata
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
                                        text = "${playlistPage?.songs?.size} ${stringResource(R.string.songs)}  ·  ${formatAsTime(totalPlayTimes)}",
                                        style = typography().xs.medium.copy(color = colorPalette().textSecondary),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Floating capsule con tutte le azioni
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(colorPalette().background1)
                                    .border(0.5.dp, colorPalette().textDisabled.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                // Search — diventa accent quando attivo
                                PlaylistPillIcon(
                                    icon = R.drawable.search_circle, active = searching,
                                    onClick = { searching = !searching }
                                )
                                Spacer(Modifier.weight(1f))
                                // Enqueue
                                PlaylistPillIcon(
                                    icon = R.drawable.enqueue, enabled = hasSongs,
                                    onClick = {
                                        if (hasSongs) playlistPage?.songs?.filter { it.asMediaItem.mediaId !in dislikedSongs }
                                            ?.map(Environment.SongItem::asMediaItem)
                                            ?.let { binder?.player?.enqueue(it, context) }
                                        else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                    },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.info_enqueue_songs), context = context) }
                                )
                                // Shuffle
                                PlaylistPillIcon(
                                    icon = R.drawable.shuffle, enabled = hasSongs,
                                    onClick = {
                                        if (hasSongs) { binder?.stopRadio(); playlistPage?.songs?.filter { it.asMediaItem.mediaId !in dislikedSongs }?.shuffled()?.map(Environment.SongItem::asMediaItem)?.let { binder?.player?.forcePlayFromBeginning(it) } }
                                        else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                    },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.info_shuffle), context = context) }
                                )
                                // Radio
                                PlaylistPillIcon(
                                    icon = R.drawable.radio, enabled = hasSongs,
                                    onClick = {
                                        if (binder != null && hasSongs) {
                                            binder.stopRadio()
                                            binder.playRadio(NavigationEndpoint.Endpoint.Watch(
                                                videoId = if (binder.player.currentMediaItem?.mediaId != null) binder.player.currentMediaItem?.mediaId
                                                else playlistPage?.songs?.first { it.asMediaItem.mediaId !in dislikedSongs }?.asMediaItem?.mediaId
                                            ))
                                        } else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                                    },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.info_start_radio), context = context) }
                                )
                                // Add to playlist
                                PlaylistPillIcon(
                                    icon = R.drawable.add_in_playlist,
                                    onClick = {
                                        menuState.display {
                                            PlaylistsItemMenu(
                                                navController = navController,
                                                modifier = Modifier.fillMaxHeight(0.4f),
                                                onDismiss = menuState::hide,
                                                onImportOnlinePlaylist = { isImportingPlaylist = true },
                                                onAddToPlaylist = { playlistPreview ->
                                                    position = playlistPreview.songCount.minus(1) ?: 0
                                                    if (position > 0) position++ else position = 0
                                                    val playlistSize = playlistPage?.songs?.size ?: 0
                                                    if ((playlistSize + playlistPreview.songCount) > 5000 && playlistPreview.playlist.isYoutubePlaylist && isYtSyncEnabled()) {
                                                        SmartMessage(context.resources.getString(R.string.yt_playlist_limited), context = context, type = PopupType.Error)
                                                    } else if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                        playlistPage?.songs?.forEachIndexed { index, song ->
                                                            runCatching {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.insert(song.asSong)
                                                                    Database.insert(SongPlaylistMap(songId = song.asMediaItem.mediaId, playlistId = playlistPreview.playlist.id, position = position + index).default())
                                                                }
                                                            }.onFailure { Timber.e("Failed onAddToPlaylist ${it.stackTraceToString()}") }
                                                        }
                                                    } else {
                                                        CoroutineScope(Dispatchers.IO).launch { EnvironmentExt.addPlaylistToPlaylist(cleanPrefix(playlistPreview.playlist.browseId ?: ""), browseId.substringAfter("VL")) }
                                                    }
                                                    CoroutineScope(Dispatchers.Main).launch { SmartMessage(context.resources.getString(R.string.done), type = PopupType.Success, context = context) }
                                                },
                                                onGoToPlaylist = { navController.navigate("${NavRoutes.localPlaylist.name}/$it") },
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                    },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.info_add_in_playlist), context = context) }
                                )
                                // Heart / like all
                                PlaylistPillIcon(
                                    icon = R.drawable.heart,
                                    enabled = playlistPage?.songs?.isNotEmpty() == true,
                                    onClick = {
                                        if (!isNetworkConnected(appContext()) && isYtSyncEnabled())
                                            SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
                                        else if (!isYtSyncEnabled()) {
                                            Database.asyncTransaction {
                                                playlistPage?.songs?.filter { getLikedAt(it.asMediaItem.mediaId) in listOf(-1L, null) }
                                                    ?.forEach { mediaItemSetLiked(it.asMediaItem) }
                                                SmartMessage(context.resources.getString(R.string.done), context = context)
                                            }
                                        } else showYoutubeLikeConfirmDialog = true
                                    },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.add_to_favorites), context = context) }
                                )
                                // Share/download
                                PlaylistPillIcon(
                                    icon = R.drawable.get_app,
                                    enabled = playlistPage?.songs?.isNotEmpty() == true,
                                    onClick = { showFastShare = true; showDirectFastShare = true },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.share_with_external_app), context = context) }
                                )
                                // Bookmark YouTube
                                if (isYtSyncEnabled()) {
                                    val isYtSaved = localPlaylist?.isYoutubePlaylist == true
                                    PlaylistPillIcon(
                                        icon = if (isYtSaved) R.drawable.bookmark else R.drawable.bookmark_outline,
                                        active = isYtSaved,
                                        onClick = {
                                            if (isNetworkConnected(context)) {
                                                if (isYtSaved) {
                                                    CoroutineScope(Dispatchers.IO).launch { EnvironmentExt.removelikePlaylistOrAlbum(browseId.substringAfter("VL")) }
                                                    Database.asyncTransaction { Database.playlistWithBrowseId(browseId.substringAfter("VL"))?.let { delete(it) } }
                                                } else {
                                                    CoroutineScope(Dispatchers.IO).launch { EnvironmentExt.likePlaylistOrAlbum(browseId.substringAfter("VL")) }
                                                    Database.asyncTransaction {
                                                        val playlistId = insert(Playlist(name = playlistPage?.playlist?.title ?: "", browseId = browseId.substringAfter("VL"), isYoutubePlaylist = true, isEditable = false))
                                                        playlistPage?.songs?.map(Environment.SongItem::asMediaItem)?.onEach(::insert)
                                                            ?.mapIndexed { index, mediaItem -> SongPlaylistMap(songId = mediaItem.mediaId, playlistId = playlistId, position = index).default() }
                                                            ?.onEach { Database.insert(it) }
                                                    }
                                                }
                                                SmartMessage(context.resources.getString(R.string.done), context = context)
                                                saveCheck = !saveCheck
                                            } else SmartMessage(context.resources.getString(R.string.no_connection), context = context, type = PopupType.Error)
                                        },
                                        onLongClick = { SmartMessage(context.resources.getString(R.string.save_youtube_library), context = context) }
                                    )
                                }
                            }
                        } else {
                            BasicText(text = stringResource(R.string.info_wait_it_may_take_a_few_minutes), style = typography().xxs.medium, maxLines = 1)
                        }

                        // Search bar — pill style, appare con animazione
                        AnimatedVisibility(
                            visible = searching,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(150))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                            ) {
                                val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                                val focusManager = LocalFocusManager.current
                                val keyboardController = LocalSoftwareKeyboardController.current
                                LaunchedEffect(searching) { focusRequester.requestFocus() }

                                BasicTextField(
                                    value = filter ?: "",
                                    onValueChange = { filter = it },
                                    textStyle = typography().xs.semiBold,
                                    singleLine = true, maxLines = 1,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (filter.isNullOrBlank()) filter = ""
                                        focusManager.clearFocus()
                                    }),
                                    cursorBrush = SolidColor(colorPalette().accent),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                            IconButton(onClick = {}, icon = R.drawable.search, color = colorPalette().accent, modifier = Modifier.align(Alignment.CenterStart).size(16.dp))
                                        }

                                        AnimatedVisibility(visible = filter?.isEmpty() ?: true, enter = fadeIn(tween(100)), exit = fadeOut(tween(100))) {
                                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f).padding(horizontal = 30.dp)) {
                                                BasicText(text = stringResource(R.string.search), maxLines = 1, overflow = TextOverflow.Ellipsis, style = typography().xs.semiBold.secondary.copy(color = colorPalette().textDisabled))
                                            }
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .height(34.dp).fillMaxWidth()
                                        .clip(RoundedCornerShape(50))
                                        .background(colorPalette().background0)
                                        .border(0.5.dp, colorPalette().accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                                        .focusRequester(focusRequester)
                                        .onFocusChanged {
                                            if (!it.hasFocus) {
                                                keyboardController?.hide()
                                                if (filter?.isBlank() == true) { filter = null; searching = false }
                                            }
                                        }
                                )
                            }
                        }
                    }

                    // ── DESCRIPTION CARD ────────────────────────────────────
                    playlistPage?.description?.let { description ->
                        item(key = "playlistInfo") {
                            val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), thickness = 0.5.dp, color = colorPalette().textDisabled.copy(alpha = 0.15f))

                            // Section title con accent bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(colorPalette().accent))
                                Spacer(Modifier.width(10.dp))
                                BasicText(
                                    text = stringResource(R.string.information).uppercase(),
                                    style = typography().xs.semiBold.copy(color = colorPalette().text, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                                    modifier = Modifier.weight(1f)
                                )
                                // Translate toggle
                                PlaylistPillIcon(
                                    icon = R.drawable.translate, active = translateEnabled, size = 18,
                                    onClick = { translateEnabled = !translateEnabled },
                                    onLongClick = { SmartMessage(context.resources.getString(R.string.info_translation), context = context) }
                                )
                            }

                            // Card con bordo accent sinistro
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp).padding(bottom = 8.dp).fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorPalette().background1)
                                    .border(0.5.dp, colorPalette().textDisabled.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                Box(modifier = Modifier.width(3.dp).fillMaxHeight().align(Alignment.CenterStart)
                                    .background(Brush.verticalGradient(listOf(colorPalette().accent.copy(alpha = 0.4f), colorPalette().accent, colorPalette().accent.copy(alpha = 0.4f)))))

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

                                    BasicText(
                                        text = translatedText,
                                        style = typography().xxs.secondary.align(TextAlign.Justify).copy(lineHeight = 18.sp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (attributionsIndex != -1) {
                                        Spacer(Modifier.height(8.dp))
                                        BasicText(text = stringResource(R.string.from_wikipedia_cca), style = typography().xxs.color(colorPalette().textDisabled).align(TextAlign.Start))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // ── SONG ITEMS ───────────────────────────────────────────
                    itemsIndexed(items = playlistPage?.songs ?: emptyList()) { index, song ->
                        val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                        val isNowPlaying = song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId

                        SwipeablePlaylistItem(
                            mediaItem = song.asMediaItem,
                            onPlayNext = { binder?.player?.addNext(song.asMediaItem, queue = selectedQueue ?: defaultQueue()) },
                            onEnqueue = { binder?.player?.enqueue(song.asMediaItem, queue = it) }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Accent bar now playing
                                if (isNowPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp).height(48.dp).align(Alignment.CenterStart).zIndex(5f)
                                            .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                            .background(Brush.verticalGradient(listOf(colorPalette().accent.copy(alpha = 0.4f), colorPalette().accent, colorPalette().accent.copy(alpha = 0.4f))))
                                    )
                                }
                                SongItem(
                                    song = song,
                                    thumbnailSizePx = songThumbnailSizePx,
                                    thumbnailSizeDp = songThumbnailSizeDp,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        navController = navController,
                                                        onDismiss = { menuState.hide() },
                                                        mediaItem = song.asMediaItem,
                                                        onInfo = { navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.key}") },
                                                        disableScrollingText = disableScrollingText,
                                                    )
                                                }
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onClick = {
                                                if (song.asMediaItem.mediaId !in dislikedSongs) {
                                                    searching = false; filter = null
                                                    playlistPage?.songs?.filter { it.asMediaItem.mediaId !in dislikedSongs }
                                                        ?.map(Environment.SongItem::asMediaItem)
                                                        ?.let { mediaItems ->
                                                            binder?.stopRadio()
                                                            binder?.player?.forcePlayAtIndex(mediaItems, mediaItems.indexOf(song.asMediaItem))
                                                        }
                                                } else SmartMessage(context.resources.getString(R.string.disliked_this_song), type = PopupType.Error, context = context)
                                            }
                                        )
                                        .background(if (isNowPlaying) colorPalette().accent.copy(alpha = 0.07f) else colorPalette().background0)
                                        .padding(start = if (isNowPlaying) 4.dp else 0.dp)
                                )
                            }
                        }
                    }

                    item(key = "footer", contentType = 0) { Spacer(modifier = Modifier.height(Dimensions.bottomSpacer)) }

                    if (playlistPage == null) {
                        item(key = "loading") {
                            ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                                repeat(4) { SongItemPlaceholder(thumbnailSizeDp = songThumbnailSizeDp) }
                            }
                        }
                    }
                } // end LazyColumn
            } // end LazyListContainer

            // ── FAB ──────────────────────────────────────────────────────────
            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    iconId = R.drawable.shuffle,
                    onClick = {
                        if (hasSongs) {
                            binder?.stopRadio()
                            playlistPage?.songs?.filter { it.asMediaItem.mediaId !in dislikedSongs }
                                ?.shuffled()?.map(Environment.SongItem::asMediaItem)
                                ?.let { binder?.player?.forcePlayFromBeginning(it) }
                        } else SmartMessage(context.resources.getString(R.string.disliked_this_collection), type = PopupType.Error, context = context)
                    }
                )
        } // end Box
    } // end LayoutWithAdaptiveThumbnail
} // end PlaylistSongList