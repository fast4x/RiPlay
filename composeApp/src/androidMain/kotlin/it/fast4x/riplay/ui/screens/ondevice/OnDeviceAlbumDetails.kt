package it.fast4x.riplay.ui.screens.ondevice

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.compose.persist.persist
import it.fast4x.compose.persist.persistList
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.riplay.Database
import it.fast4x.riplay.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.MODIFIED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.Info
import it.fast4x.riplay.models.Playlist
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.SongPlaylistMap
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AlbumsItemMenu
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.SelectorDialog
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.align
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.center
import it.fast4x.riplay.utils.color
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.disableScrollingTextKey
import it.fast4x.riplay.utils.durationTextToMillis
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsTime
import org.dailyislam.android.utilities.getHttpClient
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isNowPlaying
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.medium
import it.fast4x.riplay.utils.parentalControlEnabledKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.semiBold
import it.fast4x.riplay.utils.showFloatingIconKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bush.translator.Translator
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.themed.QueuesDialog
import it.fast4x.riplay.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.riplay.utils.addToYtLikedSongs
import org.dailyislam.android.utilities.isNetworkConnected
import it.fast4x.riplay.utils.mediaItemSetLiked

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun OnDeviceAlbumDetails(
    navController: NavController,
    albumId: String,
    //albumPage: AlbumPage?,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {

    //if (albumPage == null) return

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val selectedQueue = LocalSelectedQueue.current
    var songs by persistList<Song>("album/$albumId/songs")
    var album by persist<Album?>("album/$albumId")
    //val albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    var songPlaylist by remember {
        mutableIntStateOf(0)
    }
    var playlistsList by remember { mutableStateOf<List<Database.PlayListIdPosition>?>(null) }
    var songExists by remember { mutableStateOf(false) }
    var likedAt by remember {
        mutableStateOf<Long?>(null)
    }
    var playTime by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(Unit) {
        Database.albumSongs(albumId).collect {
            songs = if (parentalControlEnabled)
                it.filter { !it.title.startsWith(EXPLICIT_PREFIX) } else it
        }
    }

    LaunchedEffect(Unit) {
        Database.album(albumId).collect { album = it }
    }

    var showConfirmDeleteDownloadDialog by remember {
        mutableStateOf(false)
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailAlbumSizeDp = Dimensions.thumbnails.album

    val thumbnailAlbumSizePx = thumbnailAlbumSizeDp.px

    val lazyListState = rememberLazyListState()

    var listMediaItems = remember {
        mutableListOf<MediaItem>()
    }

    var selectItems by remember {
        mutableStateOf(false)
    }

    var showSelectDialog by remember {
        mutableStateOf(false)
    }

    var showDialogChangeAlbumTitle by remember {
        mutableStateOf(false)
    }
    var showDialogChangeAlbumAuthors by remember {
        mutableStateOf(false)
    }
    var showDialogChangeAlbumCover by remember {
        mutableStateOf(false)
    }
    var isCreatingNewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }
    var totalPlayTimes = 0L
    songs.forEach {
        totalPlayTimes += it.durationText?.let { it1 ->
            durationTextToMillis(it1)
        }?.toLong() ?: 0
    }
    var position by remember {
        mutableIntStateOf(0)
    }

    var scrollToNowPlaying by remember {
        mutableStateOf(false)
    }

    var nowPlayingItem by remember {
        mutableStateOf(-1)
    }
    val hapticFeedback = LocalHapticFeedback.current

    if (showDialogChangeAlbumTitle)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumTitle = false },
            title = stringResource(R.string.update_title),
            value = album?.title.toString(),
            placeholder = stringResource(R.string.title),
            setValue = {
                if (it.isNotEmpty()) {
                    Database.asyncTransaction {
                        updateAlbumTitle(albumId, it)
                    }
                }
            },
            prefix = MODIFIED_PREFIX
        )
    if (showDialogChangeAlbumAuthors)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumAuthors = false },
            title = stringResource(R.string.update_authors),
            value = album?.authorsText.toString(),
            placeholder = stringResource(R.string.authors),
            setValue = {
                if (it.isNotEmpty()) {
                    Database.asyncTransaction {
                        updateAlbumAuthors(albumId, it)
                    }
                    //context.toast("Album Saved $it")
                }
            },
            prefix = MODIFIED_PREFIX
        )

    if (showDialogChangeAlbumCover)
        InputTextDialog(
            onDismiss = { showDialogChangeAlbumCover = false },
            title = stringResource(R.string.update_cover),
            value = album?.thumbnailUrl.toString(),
            placeholder = stringResource(R.string.cover),
            setValue = {
                if (it.isNotEmpty()) {
                    Database.asyncTransaction {
                        updateAlbumCover(albumId, it)
                    }
                    //context.toast("Album Saved $it")
                }
            },
            prefix = MODIFIED_PREFIX
        )

    if (isCreatingNewPlaylist)
        InputTextDialog(
            onDismiss = { isCreatingNewPlaylist = false },
            title = stringResource(R.string.new_playlist),
            value = "",
            placeholder = stringResource(R.string.new_playlist),
            setValue = {
                if (it.isNotEmpty()) {
                    Database.asyncTransaction {
                        insert(Playlist(name = it))
                    }
                }
            }
        )

//    if (showConfirmDeleteDownloadDialog) {
//        ConfirmationDialog(
//            text = stringResource(R.string.do_you_really_want_to_delete_download),
//            onDismiss = { showConfirmDeleteDownloadDialog = false },
//            onConfirm = {}
//        )
//    }

    var isViewingQueues by remember { mutableStateOf(false) }
    if (isViewingQueues) {
        QueuesDialog(
            onSelect = {
                binder?.player?.enqueue(songs.map(Song::asMediaItem))
            },
            onDismiss = { isViewingQueues = false }
        )
    }

    if (showSelectDialog)
        SelectorDialog(
            title = stringResource(R.string.enqueue),
            onDismiss = { showSelectDialog = false },
            values = listOf(
                Info("a", stringResource(R.string.enqueue_all)),
                Info("s", stringResource(R.string.enqueue_selected))
            ),
            onValueSelected = {
                if (it == "a") {
                    isViewingQueues = true
                } else selectItems = true

                showSelectDialog = false
            }
        )

    LaunchedEffect(scrollToNowPlaying) {
        if (scrollToNowPlaying)
            lazyListState.scrollToItem(nowPlayingItem, 1)
        scrollToNowPlaying = false
    }

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)

    var translateEnabled by remember {
        mutableStateOf(false)
    }

    val translator = Translator(getHttpClient())
    val languageDestination = languageDestination()

    var readMore by remember { mutableStateOf(false) }

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {

            Box(
                modifier = Modifier
                    .background(
                        colorPalette().background0
                    )
                    //.fillMaxSize()
                    .fillMaxHeight()
                    //.fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left) 1f else contentWidth)
                    .fillMaxWidth()
            ) {

                LazyColumn(
                    state = lazyListState,
                    //contentPadding = LocalPlayerAwareWindowInsets.current
                    //    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                    modifier = Modifier
                        .background(
                            colorPalette().background0
                        )
                        .fillMaxSize()
                ) {
                    item(
                        key = "header"
                    ) {

                        val modifierArt = Modifier.fillMaxWidth()

                        Box(
                            modifier = modifierArt
                        ) {
                            if (album != null) {
                                if (!isLandscape)
                                    Box {
                                        AsyncImage(
                                            model = album?.thumbnailUrl?.resize(1200, 1200),
                                            contentDescription = "loading...",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.Center)
                                                .fadingEdge(
                                                    top = WindowInsets.systemBars
                                                        .asPaddingValues()
                                                        .calculateTopPadding() + Dimensions.fadeSpacingTop,
                                                    bottom = Dimensions.fadeSpacingBottom
                                                )
                                        )
                                        if (album?.isYoutubeAlbum == true) {
                                            Image(
                                                painter = painterResource(R.drawable.ytmusic),
                                                colorFilter = ColorFilter.tint(
                                                    Color.Red.copy(0.75f).compositeOver(Color.White)
                                                ),
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .padding(all = 5.dp)
                                                    .offset(10.dp, 10.dp),
                                                contentDescription = "Background Image",
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                AutoResizeText(
                                    text = cleanPrefix(album?.title ?: ""),
                                    style = typography().l.semiBold,
                                    fontSizeRange = FontSizeRange(32.sp, 38.sp),
                                    fontWeight = typography().l.semiBold.fontWeight,
                                    fontFamily = typography().l.semiBold.fontFamily,
                                    color = typography().l.semiBold.color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 30.dp)
                                        .applyIf(!disableScrollingText) {
                                            basicMarquee(
                                                iterations = Int.MAX_VALUE
                                            )
                                        }
                                    //.padding(bottom = 20.dp)
                                )

//                                HeaderIconButton(
//                                    icon = R.drawable.share_social,
//                                    color = colorPalette().text,
//                                    iconSize = 24.dp,
//                                    modifier = Modifier
//                                        .align(Alignment.TopEnd)
//                                        .padding(top = 5.dp, end = 5.dp),
//                                    onClick = {
//                                        album?.shareUrl?.let { url ->
//                                            val sendIntent = Intent().apply {
//                                                Intent.setAction = Intent.ACTION_SEND
//                                                Intent.setType = "text/plain"
//                                                putExtra(Intent.EXTRA_TEXT, url)
//                                            }
//
//                                            context.startActivity(
//                                                Intent.createChooser(
//                                                    sendIntent,
//                                                    null
//                                                )
//                                            )
//                                        }
//                                    }
//                                )

                            } else {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3)
                                ) {
                                    ShimmerHost {
                                        AlbumItemPlaceholder(
                                            thumbnailSizeDp = 200.dp,
                                            alternative = true
                                        )
                                        BasicText(
                                            text = stringResource(R.string.info_wait_it_may_take_a_few_minutes),
                                            style = typography().xs.medium,
                                            maxLines = 1,
                                            modifier = Modifier
                                            //.padding(top = 10.dp)

                                        )
                                    }
                                }
                            }
                        }

                    }

                    if (album?.year != null && songs.isNotEmpty())
                        item(
                            key = "infoAlbum"
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    //.padding(top = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                BasicText(
                                    text = "${album?.year} - " + songs.size.toString() + " "
                                            + stringResource(R.string.songs)
                                            + " - " + formatAsTime(totalPlayTimes),
                                    style = typography().xs.medium,
                                    maxLines = 1
                                )
                            }
                        }

                    item(
                        key = "actions",
                        contentType = 0
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                        ) {

                            HeaderIconButton(
                                icon = R.drawable.shuffle,
                                enabled = songs.any { it.likedAt != -1L },
                                color = if (songs.any { it.likedAt != -1L }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (songs.any { it.likedAt != -1L }) {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(
                                                    songs.filter { it.likedAt != -1L }
                                                        .shuffled()
                                                        .map(Song::asMediaItem)
                                                )
                                            } else {
                                                SmartMessage(
                                                    context.resources.getString(R.string.disliked_this_collection),
                                                    type = PopupType.Error,
                                                    context = context
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_shuffle),
                                                context = context
                                            )
                                        }
                                    )
                            )

                            HeaderIconButton(
                                icon = R.drawable.radio,
                                enabled = true,
                                color = if (songs.any { it.likedAt != -1L }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (songs.any { it.likedAt != -1L }) {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayFromBeginning(songs.filter { it.likedAt != -1L }
                                                    .map(Song::asMediaItem))
                                                binder?.setupRadio(
                                                    NavigationEndpoint.Endpoint.Watch(
                                                        videoId = songs.first { it.likedAt != -1L }.id
                                                    )
                                                )
                                            } else {
                                                SmartMessage(
                                                    context.resources.getString(R.string.disliked_this_collection),
                                                    type = PopupType.Error,
                                                    context = context
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_start_radio),
                                                context = context
                                            )
                                        }
                                    )
                            )

                            HeaderIconButton(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            nowPlayingItem = -1
                                            scrollToNowPlaying = false
                                            songs
                                                .forEachIndexed { index, song ->
                                                    if (song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId)
                                                        nowPlayingItem = index
                                                }

                                            if (nowPlayingItem > -1)
                                                scrollToNowPlaying = true
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_find_the_song_that_is_playing),
                                                context = context
                                            )
                                        }
                                    ),
                                icon = R.drawable.locate,
                                enabled = songs.isNotEmpty(),
                                color = if (songs.isNotEmpty()) colorPalette().text else colorPalette().textDisabled,
                                onClick = {}


                            )


                            HeaderIconButton(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp),
                                icon = R.drawable.ellipsis_horizontal,
                                enabled = songs.isNotEmpty(),
                                color = if (songs.isNotEmpty()) colorPalette().text else colorPalette().textDisabled,
                                onClick = {
                                    menuState.display {
                                        album?.let { it ->
                                            AlbumsItemMenu(
                                                navController = navController,
                                                onDismiss = menuState::hide,
                                                onSelectUnselect = {
                                                    selectItems = !selectItems
                                                    if (!selectItems) {
                                                        listMediaItems.clear()
                                                    }
                                                },
                                                /*
                                        onSelect = { selectItems = true },
                                        onUncheck = {
                                            selectItems = false
                                            listMediaItems.clear()
                                        },
                                         */
                                                onChangeAlbumTitle = {
                                                    if (album?.isYoutubeAlbum == true) {
                                                        SmartMessage(
                                                            context.resources.getString(R.string.cant_rename_Saved_albums),
                                                            type = PopupType.Error,
                                                            context = context
                                                        )
                                                    } else
                                                        showDialogChangeAlbumTitle = true
                                                },
                                                onChangeAlbumAuthors = {
                                                    showDialogChangeAlbumAuthors = true
                                                },
                                                onChangeAlbumCover = {
                                                    showDialogChangeAlbumCover = true
                                                },
                                                onPlayNext = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (songs.any { it.likedAt != -1L }) {
                                                            binder?.player?.addNext(
                                                                songs.filter { it.likedAt != -1L }
                                                                    .map(Song::asMediaItem),
                                                                context,
                                                                selectedQueue ?: defaultQueue()
                                                            )
                                                        } else {
                                                            SmartMessage(
                                                                context.resources.getString(
                                                                    R.string.disliked_this_collection
                                                                ),
                                                                type = PopupType.Error,
                                                                context = context
                                                            )
                                                        }
                                                    } else {
                                                        binder?.player?.addNext(
                                                            listMediaItems,
                                                            context,
                                                            selectedQueue ?: defaultQueue()
                                                        )
                                                        listMediaItems.clear()
                                                        selectItems = false
                                                    }
                                                },
                                                onEnqueue = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (songs.any { it.likedAt != -1L }) {
                                                            binder?.player?.enqueue(
                                                                songs.filter { it.likedAt != -1L }
                                                                    .map(Song::asMediaItem), context
                                                            )
                                                        } else {
                                                            SmartMessage(
                                                                context.resources.getString(
                                                                    R.string.disliked_this_collection
                                                                ),
                                                                type = PopupType.Error,
                                                                context = context
                                                            )
                                                        }
                                                    } else {
                                                        binder?.player?.enqueue(
                                                            listMediaItems,
                                                            context
                                                        )
                                                        listMediaItems.clear()
                                                        selectItems = false
                                                    }
                                                },
                                                album = it,
                                                onAddToPlaylist = { playlistPreview ->
                                                    position =
                                                        playlistPreview.songCount.minus(1) ?: 0
                                                    //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                    if (position > 0) position++ else position =
                                                        0
                                                    //Log.d("mediaItem", "next initial pos ${position}")
                                                    if (listMediaItems.isEmpty()) {
                                                        if (!isYouTubeSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                            songs.forEachIndexed { index, song ->
                                                                Database.asyncTransaction {
                                                                    insert(song.asMediaItem)
                                                                    insert(
                                                                        SongPlaylistMap(
                                                                            songId = song.asMediaItem.mediaId,
                                                                            playlistId = playlistPreview.playlist.id,
                                                                            position = position + index
                                                                        ).default()
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        if (!isYouTubeSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                            listMediaItems.forEachIndexed { index, song ->
                                                                //Log.d("mediaItemMaxPos", position.toString())
                                                                Database.asyncTransaction {
                                                                    insert(song)
                                                                    insert(
                                                                        SongPlaylistMap(
                                                                            songId = song.mediaId,
                                                                            playlistId = playlistPreview.playlist.id,
                                                                            position = position + index
                                                                        ).default()
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        listMediaItems.clear()
                                                        selectItems = false
                                                    }
                                                },
                                                onAddToFavourites = {
                                                    if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
                                                        SmartMessage(
                                                            appContext().resources.getString(
                                                                R.string.no_connection
                                                            ),
                                                            context = appContext(),
                                                            type = PopupType.Error
                                                        )
                                                    } else if (!isYouTubeSyncEnabled()) {
                                                        songs.forEach { song ->
                                                            mediaItemSetLiked(song.asMediaItem)
                                                        }
                                                    } else {
                                                        val totalSongsToLike = songs.filter {
                                                            it.likedAt in listOf(-1L, null)
                                                        }
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            addToYtLikedSongs(totalSongsToLike.map { it.asMediaItem })
                                                        }
                                                    }
                                                },
                                                onGoToPlaylist = {
                                                    navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                                },
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                    }

                                }
                            )

                        }
                    }

                    item(
                        key = "songsTitle"
                    ) {
                        BasicText(
                            text = stringResource(R.string.songs),
                            style = typography().m.semiBold.align(TextAlign.Start),
                            modifier = sectionTextModifier
                                .fillMaxWidth()
                        )
                    }
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }

                        SwipeablePlaylistItem(
                            mediaItem = song.asMediaItem,
                            onPlayNext = {
                                binder?.player?.addNext(song.asMediaItem, queue = selectedQueue ?: defaultQueue())
                            },
                            onDownload = {},
                            onEnqueue = {
                                binder?.player?.enqueue(song.asMediaItem, queue = it)
                            }
                        ) {
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            var forceRecompose by remember { mutableStateOf(false) }
                            SongItem(
                                mediaItem = song.asMediaItem,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailContent = {
                                    /*
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .clip(LocalAppearance.current.thumbnailShape)
                                    .fillMaxSize()
                            )
                             */
                                    BasicText(
                                        text = "${index + 1}",
                                        style = typography().s.semiBold.center.color(
                                            colorPalette().textDisabled
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(thumbnailSizeDp)
                                            .align(Alignment.Center)
                                    )


                                    NowPlayingSongIndicator(
                                        song.asMediaItem.mediaId,
                                        binder?.player
                                    )
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.hide()
                                                        forceRecompose = true
                                                    },
                                                    onInfo = {
                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}")
                                                    },
                                                    mediaItem = song.asMediaItem,
                                                    disableScrollingText = disableScrollingText
                                                )
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = {
                                            if (!selectItems) {
                                                if (song.likedAt != -1L) {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlayAtIndex(
                                                        songs.filter { it.likedAt != -1L }
                                                            .map(Song::asMediaItem),
                                                        songs.filter { it.likedAt != -1L }
                                                            .map(Song::asMediaItem)
                                                            .indexOf(song.asMediaItem)
                                                    )
                                                } else {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.disliked_this_song),
                                                        type = PopupType.Error,
                                                        context = context
                                                    )
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
                                                if (it) listMediaItems.add(song.asMediaItem) else
                                                    listMediaItems.remove(song.asMediaItem)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = colorPalette().accent,
                                                uncheckedColor = colorPalette().text
                                            ),
                                            modifier = Modifier
                                                .scale(0.7f)
                                        )
                                    else checkedState.value = false
                                },
                                isLocal = isLocal,
                                disableScrollingText = disableScrollingText,
                                isNowPlaying = binder?.player?.isNowPlaying(song.id) ?: false,
                                forceRecompose = forceRecompose
                            )
                        }
                    }

                    item(key = "bottom") {
                        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                    }

                    if (songs.isEmpty()) {
                        item(key = "loading") {
                            ShimmerHost(
                                modifier = Modifier
                                    .fillParentMaxSize()
                            ) {
                                repeat(1) {
                                    AlbumItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.album)
                                }
                                repeat(4) {
                                    SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song)
                                }
                            }
                        }
                    }


                }


                val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
                if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                    MultiFloatingActionsContainer(
                        iconId = R.drawable.shuffle,
                        onClick = {
                            if (songs.any { it.likedAt != -1L }) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    songs.filter { it.likedAt != -1L }
                                        .shuffled()
                                        .map(Song::asMediaItem)
                                )
                            } else {
                                SmartMessage(
                                    context.resources.getString(R.string.disliked_this_collection),
                                    type = PopupType.Error,
                                    context = context
                                )
                            }
                        },
                        onClickSettings = onSettingsClick,
                        onClickSearch = onSearchClick
                    )

                /*
            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            )

             */


            }
        //}


    }


}
