package it.fast4x.riplay.ui.screens.player.common

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults.colors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXCLUDE_SONG_IF_IS_VIDEO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_LOOP_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.REORDER_IN_QUEUE_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ARROW
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SwipeableQueueItem
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.EditQueueDialog
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.PlaylistsItemMenu
import it.fast4x.riplay.ui.components.themed.QueueItemMenu
import it.fast4x.riplay.ui.components.themed.QueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.QueueItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.screens.player.unified.UnifiedMiniPlayer
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.currentWindow
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.getIconQueueLoopState
import it.fast4x.riplay.utils.getScreenDimensions
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.move
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shouldBePlaying
import it.fast4x.riplay.utils.shuffleQueue
import it.fast4x.riplay.utils.smoothScrollToTop
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.windows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date

// ─── Piccolo composable helper: icona azione con background pill quando attiva ───
@Composable
private fun ActionIconButton(
    icon: Int,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Int = 24,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
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
            .size((size + 12).dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                else
                    Modifier.clickable(enabled = enabled, onClick = onClick)
            )
    ) {
        // Usiamo Image direttamente — IconButton con onClick = {} assorbirebbe i touch
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .size(size.dp)
                .alpha(if (enabled) 1f else 0.4f)
        )
    }
}


@ExperimentalSerializationApi
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
    onDismiss: (QueueLoopType) -> Unit,
    onDiscoverClick: (Boolean) -> Unit,
) {
    val windowInsets = WindowInsets.systemBars
    val context = LocalContext.current
    val showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Essential)
    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    val binderPlayer = binder.player

    var queueLoopType by rememberPreference(QUEUE_LOOP_TYPE.key, defaultValue = QueueLoopType.Default)
    var excludeSongsIfAreVideos by rememberPreference(EXCLUDE_SONG_IF_IS_VIDEO.key, false)
    val menuState = LocalGlobalSheetState.current
    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    var mediaItemIndex by remember {
        mutableIntStateOf(if (binderPlayer.mediaItemCount == 0) -1 else binderPlayer.currentMediaItemIndex)
    }
    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    var windows by remember { mutableStateOf(binderPlayer.currentTimeline.windows) }
    var windowsFiltered by remember { mutableStateOf(windows) }
    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    binderPlayer.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex = if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                mediaItemIndex = if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val queueslist by Database.queues().collectAsState(emptyList())
    val selectedQueue = Database.selectedQueueFlow().collectAsState(defaultQueue()).let {
        if (it.value == null) defaultQueue() else it.value
    }
    val rippleIndication = ripple(bounded = false)
    val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "")
    var isReorderDisabled by rememberPreference(REORDER_IN_QUEUE_ENABLED.key, defaultValue = true)
    var listMediaItems = remember { mutableListOf<MediaItem>() }
    var listMediaItemsIndex = remember { mutableListOf<Int>() }
    var selectQueueItems by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var showConfirmDeleteAllDialog by remember { mutableStateOf(false) }

    if (showConfirmDeleteAllDialog) {
        ConfirmationDialog(
            text = "Do you really want to clean queue?",
            onDismiss = { showConfirmDeleteAllDialog = false },
            onConfirm = {
                showConfirmDeleteAllDialog = false
                CoroutineScope(Dispatchers.IO).launch {
                    Database.asyncTransaction { clearQueuedMediaItems() }
                    withContext(Dispatchers.Main) { binderPlayer.clearMediaItems() }
                }
                listMediaItems.clear()
                listMediaItemsIndex.clear()
            }
        )
    }

    var plistName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                context.applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRow("PlaylistBrowseId","PlaylistName","MediaId","Title","Artists","Duration","ThumbnailUrl","AlbumId","AlbumTitle","ArtistIds")
                        if (listMediaItems.isEmpty()) {
                            windows.forEach {
                                val artistInfos = Database.songArtistInfo(it.mediaItem.mediaId)
                                val albumInfo = Database.songAlbumInfo(it.mediaItem.mediaId)
                                writeRow("", plistName, it.mediaItem.mediaId, it.mediaItem.mediaMetadata.title,
                                    artistInfos.joinToString(",") { it.name ?: "" }, it.mediaItem.asSong.durationText,
                                    it.mediaItem.mediaMetadata.artworkUri, albumInfo?.id, albumInfo?.name,
                                    artistInfos.joinToString(",") { it.id })
                            }
                        } else {
                            listMediaItems.forEach {
                                val artistInfos = Database.songArtistInfo(it.mediaId)
                                val albumInfo = Database.songAlbumInfo(it.mediaId)
                                writeRow("", plistName, it.mediaId, it.mediaMetadata.title,
                                    artistInfos.joinToString(",") { it.name ?: "" }, it.asSong.durationText,
                                    it.mediaMetadata.artworkUri, albumInfo?.id, albumInfo?.name,
                                    artistInfos.joinToString(",") { it.id })
                            }
                        }
                    }
                }
            }
        }

    var isExporting by rememberSaveable { mutableStateOf(false) }
    if (isExporting) {
        InputTextDialog(
            onDismiss = { isExporting = false },
            title = stringResource(R.string.enter_the_playlist_name),
            value = plistName,
            placeholder = stringResource(R.string.enter_the_playlist_name),
            setValue = { text ->
                plistName = text
                try {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    exportLauncher.launch("RMPlaylist_${text.take(20)}_${dateFormat.format(Date())}")
                } catch (e: ActivityNotFoundException) {
                    SmartMessage(context.resources.getString(R.string.info_not_find_app_create_doc), type = PopupType.Warning, context = context)
                }
            }
        )
    }

    val hapticFeedback = LocalHapticFeedback.current
    val showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    var discoverIsEnabled by rememberPreference(DISCOVER.key, false)
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter: String? by rememberSaveable { mutableStateOf(null) }
    val thumbnailRoundness by rememberPreference(THUMBNAIL_ROUNDNESS.key, ThumbnailRoundness.Light)
    var showQueues by rememberSaveable { mutableStateOf(false) }
    val maxHeightQueuesList by remember { derivedStateOf { getScreenDimensions().height.dp.div(8) } }
    val heightQueues = animateDpAsState(if (showQueues) maxHeightQueuesList else 20.dp)

    var windowsInQueue by remember { mutableStateOf(windows) }
    var updateWindowsList by remember { mutableStateOf(false) }
    LaunchedEffect(Unit, selectedQueue, updateWindowsList, filter) {
        val filterCharSequence = filter.toString()
        if (!filter.isNullOrBlank())
            windowsFiltered = windows.filter {
                it.mediaItem.mediaMetadata.title?.contains(filterCharSequence, true) ?: false
                        || it.mediaItem.mediaMetadata.artist?.contains(filterCharSequence, true) ?: false
            }
        val win = if (searching) windowsFiltered else windows
        windowsInQueue = if (selectedQueue == defaultQueue()) win else win.filter {
            it.mediaItem.mediaMetadata.extras?.getLong("idQueue", defaultQueueId()) == selectedQueue?.id
        }
    }

    val filteredItemsCount = windowsInQueue.filter { item ->
        blacklisted.value?.map { it.path }?.contains(item.mediaItem.mediaId) == false
                || item.mediaItem.isVideo == !excludeSongsIfAreVideos
    }.size


    // ─── Root container ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .padding(windowInsets.only(WindowInsetsSides.Horizontal).asPaddingValues())
            .background(if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background1)
            .fillMaxSize()
    ) {
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            if (to.key != binder.player.currentWindow?.uid.toString()) {
                windowsInQueue = windowsInQueue.toMutableList().apply {
                    val fromIndex = indexOfFirst { it.uid.toString() == from.key }
                    val toIndex = indexOfFirst { it.uid.toString() == to.key }
                    val currentDragInfo = dragInfo
                    dragInfo = if (currentDragInfo == null) fromIndex to toIndex else currentDragInfo.first to toIndex
                    move(fromIndex, toIndex)
                }
            } else dragInfo = null
        }
        LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
            if (!reorderableLazyListState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    binderPlayer.moveMediaItem(from, to)
                    dragInfo = null
                }
            }
        }

        // ─── Main LazyColumn ────────────────────────────────────────────────
        LazyColumn(state = lazyListState, modifier = Modifier) {

            stickyHeader {
                var editQueue by remember { mutableStateOf(false) }
                var addQueue by remember { mutableStateOf(false) }
                var queueToEdit by remember { mutableStateOf<Queues?>(null) }

                if (editQueue || addQueue) {
                    EditQueueDialog(
                        onDismiss = { editQueue = false; addQueue = false; queueToEdit = null },
                        queue = queueToEdit,
                        setValue = { queue ->
                            CoroutineScope(Dispatchers.IO).launch {
                                Database.asyncTransaction { if (editQueue) update(queue) else insert(queue) }
                            }
                            editQueue = false; addQueue = false; queueToEdit = null
                        },
                        modifier = Modifier,
                        setValueRequireNotNull = true,
                    )
                }

                // ── BOLD HEADER ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorPalette().background1)
                        .animateContentSize(animationSpec = tween(220))
                ) {
                    // Top band: queue title left  +  track counter right
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(windowInsets.only(WindowInsetsSides.Top).asPaddingValues())
                            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)
                    ) {
                        // Left: label + selected queue name
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = stringResource(R.string.queue_queue, "").trim(),
                                style = typography().xs.semiBold.copy(
                                    color = colorPalette().textSecondary,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.W600
                                ),
                                maxLines = 1,
                            )
                            BasicText(
                                text = selectedQueue?.title.orEmpty(),
                                style = typography().s.semiBold.copy(
                                    color = colorPalette().accent,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Right: track counter badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colorPalette().accent.copy(alpha = 0.12f))
                                .border(
                                    width = 0.5.dp,
                                    color = colorPalette().accent.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = "$filteredItemsCount",
                                style = typography().s.semiBold.copy(
                                    color = colorPalette().accent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Image(
                                painter = painterResource(R.drawable.musical_notes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette().accent),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        // Queue list toggle + add
                        Spacer(Modifier.width(8.dp))
                        ActionIconButton(
                            icon = if (showQueues) R.drawable.chevron_up else R.drawable.chevron_down,
                            active = showQueues,
                            size = 20,
                            onClick = { showQueues = !showQueues }
                        )
                        ActionIconButton(
                            icon = R.drawable.addqueue,
                            size = 20,
                            onClick = { editQueue = false; addQueue = true }
                        )
                    }

                    // Accent divider under header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorPalette().accent.copy(alpha = 0.6f),
                                        colorPalette().accent.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Queue list (collapsible)
                    if (showQueues)
                        LazyColumn(
                            state = rememberLazyListState(),
                            contentPadding = windowInsets.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .height(heightQueues.value)
                                .background(colorPalette().background0)
                        ) {
                            items(items = queueslist, key = { it.id }) {
                                QueueItem(
                                    title = it.title.toString(),
                                    isSelected = it.isSelected == true,
                                    acceptSong = it.acceptSong,
                                    acceptVideo = it.acceptVideo,
                                    acceptPodcast = it.acceptPodcast,
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch { Database.toggleSelectQueue(it) }
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            QueueItemMenu(
                                                navController = navController,
                                                onDismiss = { menuState.hide() },
                                                onEdit = { queueToEdit = it; editQueue = true; addQueue = false },
                                                onRemove = {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        Database.asyncTransaction { deleteQueue(it.id) }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                    // "List of media" sub-label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(colorPalette().background1)
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 6.dp, bottom = 2.dp)
                    ) {
                        BasicText(
                            text = stringResource(R.string.queue_list_of_media).uppercase(),
                            style = typography().xxs.semiBold.copy(
                                color = colorPalette().textDisabled,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.W600
                            )
                        )
                    }

                    // Search bar
                    if (searching)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .background(colorPalette().background1)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            AnimatedVisibility(visible = searching) {
                                val focusRequester = remember { FocusRequester() }
                                val focusManager = LocalFocusManager.current
                                val keyboardController = LocalSoftwareKeyboardController.current
                                LaunchedEffect(searching) { focusRequester.requestFocus() }

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
                                    cursorBrush = SolidColor(colorPalette().accent),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                                        ) {
                                            IconButton(
                                                onClick = {},
                                                icon = R.drawable.search,
                                                color = colorPalette().accent,
                                                modifier = Modifier.align(Alignment.CenterStart).size(16.dp)
                                            )
                                        }
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier.weight(1f).padding(horizontal = 30.dp)
                                        ) {
                                            if (filter?.isEmpty() ?: true)
                                                BasicText(
                                                    text = stringResource(R.string.search),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = typography().xs.semiBold.secondary.copy(color = colorPalette().textDisabled)
                                                )

                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .fillMaxWidth()
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

                    // Bottom separator of sticky header
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = colorPalette().textDisabled.copy(alpha = 0.12f)
                    )
                }
            } // end stickyHeader


            // ─── Song items ─────────────────────────────────────────────────
            items(
                items = windowsInQueue.filter { item ->
                    blacklisted.value?.map { it.path }?.contains(item.mediaItem.mediaId) == false
                            || item.mediaItem.isVideo == !excludeSongsIfAreVideos
                },
                key = { window -> window.uid.toString() }
            ) { window ->
                ReorderableItem(reorderableLazyListState, key = window.uid.toString()) { isDragging ->

                    val interactionSource = remember { MutableInteractionSource() }
                    val currentItem by rememberUpdatedState(window)
                    val checkedState = rememberSaveable { mutableStateOf(false) }
                    val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex

                    // Spring-based scale when dragging
                    val itemScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.025f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dragScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                // Slight elevation effect while dragging via shadow alpha
                                alpha = if (isDragging) 0.97f else 1f
                            }
                    ) {
                        // ── Now playing: full left border + background tint ──
                        if (isPlayingThisMediaItem) {
                            // Thick left accent bar with gradient
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(56.dp)
                                    .align(Alignment.CenterStart)
                                    .zIndex(6f)
                                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                colorPalette().accent.copy(alpha = 0.4f),
                                                colorPalette().accent,
                                                colorPalette().accent.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                        }

                        // ── Drag handle ──────────────────────────────────────
                        if (!isReorderDisabled) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .zIndex(10f)
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-4).dp)
                                    .draggableHandle(
                                        enabled = true,
                                        interactionSource = interactionSource,
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                            ) {
                                IconButton(
                                    icon = R.drawable.reorder,
                                    color = colorPalette().accent.copy(alpha = 0.7f),
                                    indication = rippleIndication,
                                    onClick = {},
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        SwipeableQueueItem(
                            mediaItem = window.mediaItem,
                            onPlayNext = {
                                binder.player.addNext(window.mediaItem, context, selectedQueue ?: defaultQueue())
                                updateWindowsList = !updateWindowsList
                            },
                            onRemoveFromQueue = {
                                binder.player.removeMediaItem(currentItem.firstPeriodIndex)
                                SmartMessage(
                                    "${context.resources.getString(R.string.deleted)} ${currentItem.mediaItem.mediaMetadata.title}",
                                    type = PopupType.Warning, context = context
                                )
                                updateWindowsList = !updateWindowsList
                            },
                            onEnqueue = {
                                binder.player.enqueue(window.mediaItem, context, it)
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
                                                .background(Color.Black.copy(alpha = 0.25f), shape = thumbnailShape())
                                                .size(Dimensions.thumbnails.song)
                                        ) {
                                            NowPlayingSongIndicator(window.mediaItem.mediaId, binder.player)
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
                                            modifier = Modifier.scale(0.7f)
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
                                                    disableScrollingText = disableScrollingText,
                                                    onBlacklist = { insertOrUpdateBlacklist(window.mediaItem.asSong) }
                                                )
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = {
                                            if (!selectQueueItems) {
                                                if (isPlayingThisMediaItem) {
                                                    if (shouldBePlaying) binderPlayer.pause() else binderPlayer.play()
                                                } else {
                                                    binderPlayer.seekToDefaultPosition(window.firstPeriodIndex)
                                                    binderPlayer.prepare()
                                                    binderPlayer.playWhenReady = true
                                                }
                                            } else checkedState.value = !checkedState.value
                                        }
                                    )
                                    // Now playing: accent tint background; others: standard
                                    .background(
                                        color = when {
                                            isPlayingThisMediaItem -> colorPalette().accent.copy(alpha = 0.09f)
                                            queueType == QueueType.Modern -> Color.Transparent
                                            else -> colorPalette().background0
                                        }
                                    )
                                    // Left padding to clear the accent bar
                                    .padding(start = if (isPlayingThisMediaItem) 4.dp else 0.dp),
                            )
                        }
                    }
                }
            }

            item {
                if (binder.isLoadingRadio) {
                    Loader()
//                    Column(modifier = Modifier.shimmer()) {
//                        repeat(3) { index ->
//                            SongItemPlaceholder(
//                                thumbnailSizeDp = thumbnailSizeDp,
//                                modifier = Modifier.alpha(1f - index * 0.125f).fillMaxWidth()
//                            )
//                        }
//                    }
                }
            }

            item(key = "footer", contentType = 0) {
                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }
        } // end LazyColumn

        LaunchedEffect(Unit) {
            if (!lazyListState.isScrollInProgress)
                lazyListState.animateScrollToItem(windows.indexOf(binderPlayer.currentWindow), -300)
        }


        // ─── FLOATING ACTION BAR ─────────────────────────────────────────────
        val density = LocalDensity.current
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Tall gradient fade from list into bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colorPalette().background1.copy(alpha = 0.75f),
                                colorPalette().background1
                            )
                        )
                    )
            )

            // The actual bar — solid background, no click-to-dismiss on whole bar
            Box(
                modifier = Modifier
                    .background(colorPalette().background1)
                    .fillMaxWidth()
                    .height(Dimensions.navigationBarHeight + bottomInset)
                    .padding(PaddingValues(bottom = bottomInset))
            ) {
                // MiniPlayer floating above bar
                if (!isLandscape)
                    Box(
                        modifier = Modifier
                            .absoluteOffset(0.dp, -65.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        UnifiedMiniPlayer(showPlayer = { onDismiss(queueLoopType) }, hidePlayer = {})
                    }

                // ── FLOATING CAPSULE with all action icons ─────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colorPalette().background0)
                        .border(
                            width = 0.5.dp,
                            color = colorPalette().textDisabled.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Dismiss arrow — leftmost
                    ActionIconButton(
                        icon = R.drawable.chevron_down,
                        size = 22,
                        onClick = { onDismiss(queueLoopType) }
                    )

                    // Search
                    ActionIconButton(
                        icon = R.drawable.search_circle,
                        active = searching,
                        size = 22,
                        onClick = {
                            searching = !searching
                            if (searching) windowsFiltered = windows
                        }
                    )

                    // Discover (optional)
                    if (showButtonPlayerDiscover) {
                        ActionIconButton(
                            icon = R.drawable.star_brilliant,
                            active = discoverIsEnabled,
                            size = 22,
                            onClick = {
                                discoverIsEnabled = !discoverIsEnabled
                                onDiscoverClick(discoverIsEnabled)
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.discoverinfo), context = context)
                            }
                        )
                    }

                    // Reorder lock
                    ActionIconButton(
                        icon = if (isReorderDisabled) R.drawable.locked else R.drawable.unlocked,
                        active = !isReorderDisabled,
                        size = 22,
                        onClick = { isReorderDisabled = !isReorderDisabled }
                    )

                    // Loop — accent when non-default
                    ActionIconButton(
                        icon = getIconQueueLoopState(queueLoopType),
                        active = queueLoopType != QueueLoopType.Default,
                        size = 22,
                        onClick = { queueLoopType = setQueueLoopState(queueLoopType) }
                    )

                    // Shuffle
                    ActionIconButton(
                        icon = R.drawable.shuffle,
                        enabled = !reorderableLazyListState.isAnyItemDragging,
                        size = 22,
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.smoothScrollToTop()
                            }.invokeOnCompletion {
                                binderPlayer.shuffleQueue()
                                updateWindowsList = !updateWindowsList
                            }
                        }
                    )

                    // More (ellipsis) — rightmost
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(enabled = windows.isNotEmpty()) {
                                menuState.display {
                                    PlaylistsItemMenu(
                                        navController = navController,
                                        onDismiss = menuState::hide,
                                        onSelectUnselect = {
                                            selectQueueItems = !selectQueueItems
                                            if (!selectQueueItems) listMediaItems.clear()
                                        },
                                        onDelete = {
                                            if (listMediaItemsIndex.isNotEmpty()) {
                                                val mediacount = listMediaItemsIndex.size - 1
                                                listMediaItemsIndex.sort()
                                                for (i in mediacount.downTo(0)) {
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
                                            position = playlistPreview.songCount.minus(1) ?: 0
                                            if (position > 0) position++ else position = 0
                                            if (listMediaItems.isEmpty()) {
                                                if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                    windows.forEachIndexed { index, song ->
                                                        Database.asyncTransaction {
                                                            insert(song.mediaItem)
                                                            insert(SongPlaylistMap(
                                                                songId = song.mediaItem.mediaId,
                                                                playlistId = playlistPreview.playlist.id,
                                                                position = position + index
                                                            ).default())
                                                        }
                                                    }
                                                } else {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        playlistPreview.playlist.browseId.let { id ->
                                                            addToYtPlaylist(
                                                                playlistPreview.playlist.id, position,
                                                                cleanPrefix(id ?: ""),
                                                                windows.filterNot { it.mediaItem.mediaId.startsWith(LOCAL_KEY_PREFIX) }.map { it.mediaItem }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                    listMediaItems.forEachIndexed { index, song ->
                                                        Database.asyncTransaction {
                                                            insert(song)
                                                            insert(SongPlaylistMap(
                                                                songId = song.mediaId,
                                                                playlistId = playlistPreview.playlist.id,
                                                                position = position + index
                                                            ).default())
                                                        }
                                                    }
                                                } else {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        playlistPreview.playlist.browseId.let { id ->
                                                            addToYtPlaylist(
                                                                playlistPreview.playlist.id, position,
                                                                cleanPrefix(id ?: ""),
                                                                listMediaItems.filterNot { it.mediaId.startsWith(LOCAL_KEY_PREFIX) }
                                                            )
                                                        }
                                                    }
                                                }
                                                listMediaItems.clear()
                                                listMediaItemsIndex.clear()
                                                selectQueueItems = false
                                            }
                                        },
                                        onExport = { isExporting = true },
                                        onGoToPlaylist = {
                                            navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                        },
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ellipsis_horizontal),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (windows.isNotEmpty()) colorPalette().text else colorPalette().textDisabled
                            ),
                            modifier = Modifier
                                .size(22.dp)
                                .alpha(if (windows.isNotEmpty()) 1f else 0.4f)
                        )
                    }
                }
            }
        }
    } // end root Box
} // end Queue