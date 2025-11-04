package it.fast4x.riplay.ui.screens.player.common


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults.colors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.valentinilk.shimmer.shimmer
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LOCAL_KEY_PREFIX
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.SwipeableQueueItem
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.PlaylistsItemMenu
import it.fast4x.riplay.ui.components.themed.QueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.screens.settings.isSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.currentWindow
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.discoverKey
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.getIconQueueLoopState
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.queueTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.reorderInQueueEnabledKey
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shouldBePlaying
import it.fast4x.riplay.extensions.preferences.showButtonPlayerArrowKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerDiscoverKey
import it.fast4x.riplay.utils.shuffleQueue
import it.fast4x.riplay.utils.smoothScrollToTop
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.utils.windows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.ui.components.themed.EditQueueDialog
import it.fast4x.riplay.ui.components.themed.QueueItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.items.QueueItem
import it.fast4x.riplay.ui.screens.player.local.LocalMiniPlayer
import it.fast4x.riplay.ui.screens.player.online.OnlineMiniPlayer
import it.fast4x.riplay.utils.getScreenDimensions
import it.fast4x.riplay.utils.move
import kotlinx.coroutines.withContext
import timber.log.Timber

@ExperimentalMaterial3Api
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun Queue(
    navController: NavController,
    showPlayer: () -> Unit? = {},
    hidePlayer: () -> Unit? = {},
    player: MutableState<YouTubePlayer?>,
    playerState: MutableState<PlayerConstants.PlayerState>,
    currentDuration: Float,
    currentSecond: Float,
    onDismiss: (QueueLoopType) -> Unit,
    onDiscoverClick: (Boolean) -> Unit,
) {
    val windowInsets = WindowInsets.systemBars

    val context = LocalContext.current
    val showButtonPlayerArrow by rememberPreference(showButtonPlayerArrowKey, true)
    var queueType by rememberPreference(queueTypeKey, QueueType.Essential)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    val binderPlayer = binder.player
    //val player = binder.onlinePlayer

    var queueLoopType by rememberPreference(queueLoopTypeKey, defaultValue = QueueLoopType.Default)

    val menuState = LocalMenuState.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    var mediaItemIndex by remember {
        mutableIntStateOf(if (binderPlayer.mediaItemCount == 0) -1 else binderPlayer.currentMediaItemIndex)
    }

    var windows by remember {
        mutableStateOf(binderPlayer.currentTimeline.windows)
    }
    var windowsFiltered by remember {
        mutableStateOf(windows)
    }

    var shouldBePlaying by remember {
        mutableStateOf(binder.player.shouldBePlaying)
    }

    binderPlayer.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                //mediaItemIndex = binderPlayer.currentMediaItemIndex
                mediaItemIndex =
                    if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                //mediaItemIndex = binderPlayer.currentMediaItemIndex
                mediaItemIndex =
                    if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val queueslist by Database.queues().collectAsState( emptyList())
    val selectedQueue = Database.selectedQueueFlow().collectAsState( defaultQueue()).let {
        if (it.value == null) defaultQueue() else it.value
    }

    val rippleIndication = ripple(bounded = false)

    val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "")

    var isReorderDisabled by rememberPreference(reorderInQueueEnabledKey, defaultValue = true)

    var listMediaItems = remember {
        mutableListOf<MediaItem>()
    }
    var listMediaItemsIndex = remember {
        mutableListOf<Int>()
    }

    var selectQueueItems by remember {
        mutableStateOf(false)
    }

    var position by remember {
        mutableIntStateOf(0)
    }

    var showConfirmDeleteAllDialog by remember {
        mutableStateOf(false)
    }

    if (showConfirmDeleteAllDialog) {
        ConfirmationDialog(
            text = "Do you really want to clean queue?",
            onDismiss = { showConfirmDeleteAllDialog = false },
            onConfirm = {
                showConfirmDeleteAllDialog = false
                CoroutineScope(Dispatchers.IO).launch {
                    Database.asyncTransaction {
                        clearQueuedMediaItems()
                    }
                    withContext(Dispatchers.Main) {
                        binderPlayer.clearMediaItems()
                    }
                }
//                val mediacount = binder.player.mediaItemCount - 1
//                for (i in mediacount.downTo(0)) {
//                    if (i == mediaItemIndex) null else binder.player.removeMediaItem(i)
//                }
                listMediaItems.clear()
                listMediaItemsIndex.clear()

            }
        )
    }

    var plistName by remember {
        mutableStateOf("")
    }

    val coroutineScope = rememberCoroutineScope()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        csvWriter().open(outputStream) {
                            writeRow(
                                "PlaylistBrowseId",
                                "PlaylistName",
                                "MediaId",
                                "Title",
                                "Artists",
                                "Duration",
                                "ThumbnailUrl",
                                "AlbumId",
                                "AlbumTitle",
                                "ArtistIds"
                            )
                            if (listMediaItems.isEmpty()) {
                                windows.forEach {
                                    val artistInfos = Database.songArtistInfo(it.mediaItem.mediaId)
                                    val albumInfo = Database.songAlbumInfo(it.mediaItem.mediaId)
                                    writeRow(
                                        "",
                                        plistName,
                                        it.mediaItem.mediaId,
                                        it.mediaItem.mediaMetadata.title,
                                        artistInfos.joinToString(",") { it.name ?: "" },
                                        it.mediaItem.asSong.durationText,
                                        it.mediaItem.mediaMetadata.artworkUri,
                                        albumInfo?.id,
                                        albumInfo?.name,
                                        artistInfos.joinToString(",") { it.id }
                                    )
                                }
                            } else {
                                listMediaItems.forEach {
                                    val artistInfos = Database.songArtistInfo(it.mediaId)
                                    val albumInfo = Database.songAlbumInfo(it.mediaId)
                                    writeRow(
                                        "",
                                        plistName,
                                        it.mediaId,
                                        it.mediaMetadata.title,
                                        artistInfos.joinToString(",") { it.name ?: "" },
                                        it.asSong.durationText,
                                        it.mediaMetadata.artworkUri,
                                        albumInfo?.id,
                                        albumInfo?.name,
                                        artistInfos.joinToString(",") { it.id }
                                    )
                                }
                            }
                        }
                    }
            }
        }


    var isExporting by rememberSaveable {
        mutableStateOf(false)
    }

    if (isExporting) {
        InputTextDialog(
            onDismiss = {
                isExporting = false
            },
            title = stringResource(R.string.enter_the_playlist_name),
            value = plistName,
            placeholder = stringResource(R.string.enter_the_playlist_name),
            setValue = { text ->
                plistName = text
                try {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    exportLauncher.launch(
                        "RMPlaylist_${text.take(20)}_${
                            dateFormat.format(
                                Date()
                            )
                        }"
                    )
                } catch (e: ActivityNotFoundException) {
                    SmartMessage(
                        context.resources.getString(R.string.info_not_find_app_create_doc),
                        type = PopupType.Warning, context = context
                    )
                }
            }
        )
    }

    val hapticFeedback = LocalHapticFeedback.current
    val showButtonPlayerDiscover by rememberPreference(showButtonPlayerDiscoverKey, false)
    var discoverIsEnabled by rememberPreference(discoverKey, false)

    var searching by rememberSaveable { mutableStateOf(false) }
    var filter: String? by rememberSaveable { mutableStateOf(null) }
    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    var showQueues by rememberSaveable { mutableStateOf(false) }
    val maxHeightQueuesList by remember { derivedStateOf { getScreenDimensions().height.dp.div(8) } }
    println("maxHeightQueuesList: $maxHeightQueuesList")
    val heightQueues = animateDpAsState(if (showQueues) maxHeightQueuesList else 20.dp)

    var filterCharSequence: CharSequence
    filterCharSequence = filter.toString()
    if (!filter.isNullOrBlank())
        windowsFiltered = windowsFiltered
            .filter {
                it.mediaItem.mediaMetadata.title?.contains(filterCharSequence, true) ?: false ||
                        it.mediaItem.mediaMetadata.artist?.contains(
                            filterCharSequence,
                            true
                        ) ?: false ||
                        it.mediaItem.mediaMetadata.albumTitle?.contains(
                            filterCharSequence,
                            true
                        ) ?: false ||
                        it.mediaItem.mediaMetadata.albumArtist?.contains(
                            filterCharSequence,
                            true
                        ) ?: false
            }
    var windowsInQueue by remember { mutableStateOf(windows) }
    var updateWindowsList by remember { mutableStateOf(false) }
    LaunchedEffect(selectedQueue, updateWindowsList) {
        val win = if (searching) windowsFiltered else windows
        windowsInQueue = if (selectedQueue == defaultQueue()) win else win.filter {
                it.mediaItem.mediaMetadata.extras
                    ?.getLong("idQueue", defaultQueueId()) == selectedQueue?.id
        }
        //binderPlayer.setMediaItems(windowsInQueue.map { it.mediaItem })
        println("windowsInQueue changed: ${windowsInQueue.size}")
    }

    Box(
        modifier = Modifier
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .asPaddingValues()
            )
            .background(if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background1)
            .fillMaxSize()
    ) {

        var dragInfo by remember {
            mutableStateOf<Pair<Int, Int>?>(null)
        }
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            //scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
        ) { from, to ->
            // based on uid as key
            if (to.key != binder.player.currentWindow?.uid.toString()) {

                windowsInQueue = windowsInQueue.toMutableList().apply {
                    val fromIndex = indexOfFirst { it.uid.toString() == from.key }
                    val toIndex = indexOfFirst { it.uid.toString() == to.key }

                    val currentDragInfo = dragInfo
                    dragInfo = if (currentDragInfo == null)
                        fromIndex to toIndex
                    else currentDragInfo.first to toIndex

                    move(fromIndex, toIndex)
                    println("reorderableLazyListState dragInfo from ${fromIndex} to ${toIndex}")
                }

            } else dragInfo = null

        }

        LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
            if (!reorderableLazyListState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val fromIndex = from
                    val toIndex = to
                    binderPlayer.moveMediaItem(fromIndex, toIndex)
                    println("reorderableLazyListState.isAnyItemDragging moved from ${fromIndex} to ${toIndex}")
                    dragInfo = null
                }
            }
        }

    LazyColumn(
        state = lazyListState,
//        contentPadding = windowInsets
//            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
//            .asPaddingValues(),
//        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier


    ) {

            stickyHeader {

                var editQueue by remember { mutableStateOf(false) }
                var addQueue by remember { mutableStateOf(false) }
                var queueToEdit by remember { mutableStateOf<Queues?>(null) }
                if (editQueue || addQueue) {
                    EditQueueDialog(
                        onDismiss = {
                            editQueue = false
                            addQueue = false
                            queueToEdit = null
                        },
                        queue = queueToEdit,
                        setValue = { queue ->
                            CoroutineScope(Dispatchers.IO).launch {
                                Database.asyncTransaction {
                                    if (editQueue)
                                        update(queue)
                                    else insert(queue)
                                }
                            }
                            editQueue = false
                            addQueue = false
                            queueToEdit = null
                        },
                        modifier = Modifier,
                        setValueRequireNotNull = true,
                    )
                }

                Title2Actions(
                    title = "Queue: ${selectedQueue?.title}",
                    icon1 = if (showQueues) R.drawable.chevron_up else R.drawable.chevron_down,
                    icon2 = R.drawable.addqueue,
                    onClick1 = {
                        showQueues = !showQueues
                    },
                    onClick2 = {
                        editQueue = false
                        addQueue = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorPalette().background1)
                )

                if (showQueues)
                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = windowInsets
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(heightQueues.value)
                        .background(colorPalette().background0)
                ) {

                        items(
                            items = queueslist,
                            key = { it.id }
                        ) {
                            QueueItem(
                                title = it.title.toString(),
                                isSelected = it.isSelected == true,
                                acceptSong = it.acceptSong,
                                acceptVideo = it.acceptVideo,
                                acceptPodcast = it.acceptPodcast,
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        Database.toggleSelectQueue(it)
                                    }
                                    println("Queue selected")
                                },
                                onLongClick = {
                                    menuState.display {
                                        QueueItemMenu(
                                            navController = navController,
                                            onDismiss = { menuState.hide() },
                                            onEdit = {
                                                queueToEdit = it
                                                editQueue = true
                                                addQueue = false
                                                println("Queue selected for editing")
                                            },
                                            onRemove = {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    Database.asyncTransaction {
                                                        deleteQueue(it.id)
                                                    }
                                                }
                                            }
                                        )

                                    }
                                }
                            )
                        }

                }

                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(colorPalette().background1)
                        .fillMaxWidth()
                ) {
                    Title("List of media")
                }

                if (searching)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .background(colorPalette().background1)
                            .padding(all = 10.dp)
                            .fillMaxWidth()
                    ) {
                        AnimatedVisibility(visible = searching) {
                            val focusRequester = remember { FocusRequester() }
                            val focusManager = LocalFocusManager.current
                            val keyboardController = LocalSoftwareKeyboardController.current

                            LaunchedEffect(searching) {
                                focusRequester.requestFocus()
                            }

                            BasicTextField(
                                value = filter ?: "",
                                onValueChange = { filter = it },
                                textStyle = typography().xs.semiBold,
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (filter.isNullOrBlank()) filter = ""
                                    focusManager.clearFocus()
                                }),
                                cursorBrush = SolidColor(colorPalette().text),
                                decorationBox = { innerTextField ->
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        IconButton(
                                            onClick = {},
                                            icon = R.drawable.search,
                                            color = colorPalette().favoritesIcon,
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .size(16.dp)
                                        )
                                    }
    //                                Box(
    //                                    contentAlignment = Alignment.CenterStart,
    //                                    modifier = Modifier
    //                                        .weight(1f)
    //                                        .padding(horizontal = 30.dp)
    //                                ) {
    //                                    AnimatedVisibility(
    //                                        visible = filter?.isEmpty() ?: true,
    //                                        enter = fadeIn(tween(100)),
    //                                        exit = fadeOut(tween(100)),
    //                                    ) {
    //                                        BasicText(
    //                                            text = stringResource(R.string.search),
    //                                            maxLines = 1,
    //                                            overflow = TextOverflow.Ellipsis,
    //                                            style = typography().xs.semiBold.secondary.copy(color = colorPalette().textDisabled)
    //                                        )
    //                                    }
    //
    //                                    innerTextField()
    //                                }
                                },
                                modifier = Modifier
                                    .height(30.dp)
                                    .fillMaxWidth()
                                    .background(
                                        colorPalette().background4,
                                        shape = thumbnailRoundness.shape()
                                    )
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (!it.hasFocus) {
                                            keyboardController?.hide()
                                            if (filter?.isBlank() == true) {
                                                filter = null
                                                searching = false
                                            }
                                        }
                                    }
                            )
                        }

                    }
            }


        items(
            items = windowsInQueue,
            key =  { window -> window.uid.toString() }
        ) { window ->
            ReorderableItem(
                reorderableLazyListState,
                key = window.uid.toString()
            ) { isDragging ->

                val interactionSource = remember { MutableInteractionSource() }

                val currentItem by rememberUpdatedState(window)
                val checkedState = rememberSaveable { mutableStateOf(false) }

                //var deltaX by remember { mutableStateOf(0f) }
                val isPlayingThisMediaItem =
                    mediaItemIndex == window.firstPeriodIndex
                //val currentItem by rememberUpdatedState(window)
                //val isLocal by remember { derivedStateOf { window.mediaItem.isLocal } }
                //var forceRecompose by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .zIndex(10f)
                            .align(Alignment.TopCenter)
                            .offset(y = (-5).dp)
                            .draggableHandle(
                                enabled = !isReorderDisabled,
                                interactionSource = interactionSource,
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )

                    ) {

                        if (!isReorderDisabled) {
                            IconButton(
                                icon = R.drawable.reorder,
                                color = colorPalette().accent,
                                indication = rippleIndication,
                                onClick = {},
//                                modifier = Modifier
//                                    .draggableHandle()
                            )
                        }
                    }

                    SwipeableQueueItem (
                        mediaItem = window.mediaItem,
                        onPlayNext = {
                            binder.player.addNext(
                                window.mediaItem,
                                context,
                                selectedQueue ?: defaultQueue()
                            )
                            updateWindowsList = !updateWindowsList
                        },
                        onRemoveFromQueue = {
                            binder.player.removeMediaItem(currentItem.firstPeriodIndex)
                            //Timber.d("QueueItem: index ${currentItem.firstPeriodIndex}")
                            SmartMessage(
                                "${context.resources.getString(R.string.deleted)} ${currentItem.mediaItem.mediaMetadata.title}",
                                type = PopupType.Warning,
                                context = context
                            )
                            updateWindowsList = !updateWindowsList
                        },
                        onEnqueue = {
                            binder.player.enqueue(
                                window.mediaItem,
                                context,
                                it
                            )
                            updateWindowsList = !updateWindowsList
                        }
                    ) {
                        SongItem(
                            song = window.mediaItem,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            onThumbnailContent = {
                                musicBarsTransition.AnimatedVisibility(
                                    visible = { it == window.firstPeriodIndex },
                                    enter = fadeIn(tween(800)),
                                    exit = fadeOut(tween(800)),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.25f),
                                                shape = thumbnailShape()
                                            )
                                            .size(Dimensions.thumbnails.song)
                                    ) {
                                        NowPlayingSongIndicator(
                                            window.mediaItem.mediaId,
                                            binder.player
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                if (selectQueueItems)
                                    Checkbox(
                                        checked = checkedState.value,
                                        onCheckedChange = {
                                            checkedState.value = it
                                            if (it) {
                                                listMediaItems.add(window.mediaItem)
                                                listMediaItemsIndex.add(window.firstPeriodIndex)
                                            } else {
                                                listMediaItems.remove(window.mediaItem)
                                                listMediaItemsIndex.remove(window.firstPeriodIndex)
                                            }
                                        },
                                        colors = colors(
                                            checkedColor = colorPalette().accent,
                                            uncheckedColor = colorPalette().text
                                        ),
                                        modifier = Modifier
                                            .scale(0.7f)
                                    )
                                else checkedState.value = false

                            },
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            QueuedMediaItemMenu(
                                                navController = navController,
                                                mediaItem = window.mediaItem,
                                                indexInQueue = if (isPlayingThisMediaItem) null else window.firstPeriodIndex,
                                                onDismiss = {
                                                    menuState.hide()
                                                    updateWindowsList = !updateWindowsList
                                                },
                                                onInfo = {},
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    },
                                    onClick = {
                                        if (!selectQueueItems) {
                                            if (isPlayingThisMediaItem) {
                                                if (shouldBePlaying) {
                                                    binderPlayer.pause()
                                                } else {
                                                    binderPlayer.play()
                                                }
                                            } else {
                                                binderPlayer.seekToDefaultPosition(window.firstPeriodIndex)
                                                binderPlayer.prepare()
                                                binderPlayer.playWhenReady = true
                                            }
                                        } else checkedState.value = !checkedState.value
                                    }
                                )
                                .background(color = if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background0),
                            //disableScrollingText = disableScrollingText,
                            //isNowPlaying = binder.player.isNowPlaying(window.mediaItem.mediaId),
                            //forceRecompose = forceRecompose
                        )
                    }
                }
            }
        }

        item {
            if (binder.isLoadingRadio) {
                Column(
                    modifier = Modifier
                        .shimmer()
                ) {
                    repeat(3) { index ->
                        SongItemPlaceholder(
                            thumbnailSizeDp = thumbnailSizeDp,
                            modifier = Modifier
                                .alpha(1f - index * 0.125f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        item(
            key = "footer",
            contentType = 0
        ) {
            Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
        }
    }

    LaunchedEffect(Unit) {
        if (!lazyListState.isScrollInProgress)
            lazyListState.animateScrollToItem (
                windows.indexOf(binderPlayer.currentWindow),
                -300
            )
    }

//    val backgroundProgress by rememberPreference(backgroundProgressKey, BackgroundProgress.MiniPlayer)
//    val positionAndDuration by binder.player.positionAndDurationState()
            Box(
                modifier = Modifier
                    .clickable(onClick = { onDismiss(queueLoopType) })
                    .background(colorPalette().background1)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(60.dp) //bottom bar queue
//                    .drawBehind {
//                        if (backgroundProgress == BackgroundProgress.Both || backgroundProgress == BackgroundProgress.MiniPlayer) {
//                            drawRect(
//                                color = colorPalette().favoritesOverlay,
//                                topLeft = Offset.Zero,
//                                size = Size(
//                                    width = positionAndDuration.first.toFloat() /
//                                            positionAndDuration.second.absoluteValue * size.width,
//                                    height = size.maxDimension
//                                )
//                            )
//                        }
//                    }

            ) {

                if (!isLandscape)
                    Box(
                        modifier = Modifier
                            .absoluteOffset(0.dp, -65.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (binderPlayer.currentMediaItem?.isLocal == true)
                            LocalMiniPlayer(
                                showPlayer = {
                                    onDismiss(queueLoopType)
                                },
                                hidePlayer = {}
                            )
                        else
                            OnlineMiniPlayer(
                                showPlayer = {
                                    onDismiss(queueLoopType)
                                },
                                hidePlayer = { hidePlayer() },
                                navController = navController,
                                player = player,
                                playerState = playerState,
                                currentDuration = currentDuration,
                                currentSecond = currentSecond,
                            )
                    }


                if (!showButtonPlayerArrow)
                    Image(
                        painter = painterResource(R.drawable.horizontal_bold_line_rounded),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .absoluteOffset(0.dp, -10.dp)
                            .align(Alignment.TopCenter)
                            .size(30.dp)
                    )


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.CenterStart)

                ) {

                    BasicText(
                        text = "${binder.player.mediaItemCount} ",
                        style = typography().xxs.medium,
                    )
                    Image(
                        painter = painterResource(R.drawable.musical_notes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().textSecondary),
                        modifier = Modifier
                            .size(12.dp)
                    )

                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 4.dp)

                ) {
                    IconButton(
                        icon = R.drawable.search_circle,
                        color = colorPalette().text,
                        onClick = {
                            searching = !searching
                            if (searching)
                                windowsFiltered = windows
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )

                    if (showButtonPlayerDiscover) {
                        IconButton(
                            icon = R.drawable.star_brilliant,
                            color = if (discoverIsEnabled) colorPalette().text else colorPalette().textDisabled,
                            onClick = {},
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(24.dp)
                                .combinedClickable(
                                    onClick = {
                                        discoverIsEnabled = !discoverIsEnabled
                                        onDiscoverClick(discoverIsEnabled)
                                    },
                                    onLongClick = {
                                        SmartMessage(
                                            context.resources.getString(R.string.discoverinfo),
                                            context = context
                                        )
                                    }

                                )
                        )

                        Spacer(
                            modifier = Modifier
                                .width(12.dp)
                        )

                    }

                    IconButton(
                        icon = if (isReorderDisabled) R.drawable.locked else R.drawable.unlocked,
                        color = if (isReorderDisabled) colorPalette().text else colorPalette().accent,
                        onClick = { isReorderDisabled = !isReorderDisabled },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )
                    IconButton(
                        icon = getIconQueueLoopState(queueLoopType),
                        color = colorPalette().text,
                        onClick = {
                            queueLoopType = setQueueLoopState(queueLoopType)
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )

                    IconButton(
                        icon = R.drawable.shuffle,
                        color = colorPalette().text,
                        enabled = !reorderableLazyListState.isAnyItemDragging,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp),
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.smoothScrollToTop()
                            }.invokeOnCompletion {
                                binderPlayer.shuffleQueue()
                                updateWindowsList = !updateWindowsList
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )
                    HeaderIconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = if (windows.isNotEmpty() == true) colorPalette().text else colorPalette().textDisabled,
                        enabled = windows.isNotEmpty() == true,
                        modifier = Modifier
                            .padding(end = 4.dp),
                        onClick = {
                            menuState.display {
                                PlaylistsItemMenu(
                                    navController = navController,
                                    onDismiss = menuState::hide,
                                    onSelectUnselect = {
                                        selectQueueItems = !selectQueueItems
                                        if (!selectQueueItems) {
                                            listMediaItems.clear()
                                        }
                                    },
                                    /*
                                    onSelect = { selectQueueItems = true },
                                    onUncheck = {
                                        selectQueueItems = false
                                        listMediaItems.clear()
                                        listMediaItemsIndex.clear()
                                    },
                                     */
                                    onDelete = {
                                        if (listMediaItemsIndex.isNotEmpty())
                                        //showSelectTypeClearQueue = true else
                                        {
                                            val mediacount = listMediaItemsIndex.size - 1
                                            listMediaItemsIndex.sort()
                                            for (i in mediacount.downTo(0)) {
                                                //if (i == mediaItemIndex) null else
                                                binder.player.removeMediaItem(listMediaItemsIndex[i])
                                            }
                                            listMediaItemsIndex.clear()
                                            listMediaItems.clear()
                                            selectQueueItems = false
                                        } else {
                                            showConfirmDeleteAllDialog = true
                                        }
                                    },
                                    onAddToPlaylist = { playlistPreview ->
                                        position =
                                            playlistPreview.songCount.minus(1) ?: 0
                                        //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                        if (position > 0) position++ else position = 0
                                        //Log.d("mediaItem", "next initial pos ${position}")
                                        if (listMediaItems.isEmpty()) {
                                            if (!isSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                windows.forEachIndexed { index, song ->
                                                    Database.asyncTransaction {
                                                        insert(song.mediaItem)
                                                        insert(
                                                            SongPlaylistMap(
                                                                songId = song.mediaItem.mediaId,
                                                                playlistId = playlistPreview.playlist.id,
                                                                position = position + index
                                                            ).default()
                                                        )
                                                    }
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    playlistPreview.playlist.browseId.let { id ->
                                                        addToYtPlaylist(
                                                            playlistPreview.playlist.id,
                                                            position,
                                                            cleanPrefix(id ?: ""),windows
                                                                .filterNot {it.mediaItem.mediaId.startsWith(LOCAL_KEY_PREFIX)}
                                                                .map { it.mediaItem })
                                                    }
                                                }
                                            }
                                        } else {
                                            if (!isSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
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
                                                    //Log.d("mediaItemPos", "add position $position")
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    playlistPreview.playlist.browseId.let { id ->
                                                        addToYtPlaylist(
                                                            playlistPreview.playlist.id,
                                                            position,
                                                            cleanPrefix(id ?: ""),
                                                            listMediaItems.filterNot {it.mediaId.startsWith(LOCAL_KEY_PREFIX)})
                                                    }
                                                }
                                            }
                                            listMediaItems.clear()
                                            listMediaItemsIndex.clear()
                                            selectQueueItems = false
                                        }
                                    },
                                    onExport = {
                                        isExporting = true
                                    },
                                    onGoToPlaylist = {
                                        navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                    },
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    )


                    if (showButtonPlayerArrow) {
                        Spacer(
                            modifier = Modifier
                                .width(12.dp)
                        )
                        IconButton(
                            icon = R.drawable.chevron_down,
                            color = colorPalette().text,
                            onClick = { onDismiss(queueLoopType) },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(24.dp)
                        )
                    }


                }
            }
    }

}
