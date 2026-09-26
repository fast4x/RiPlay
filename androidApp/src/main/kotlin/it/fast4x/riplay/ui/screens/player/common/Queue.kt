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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
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
import it.fast4x.riplay.enums.ExportType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.extensions.exporter.Exporter
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SwipeableQueueItem
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.EditQueueDialog
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.extensions.experimental.smoothloader.Loader
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
import it.fast4x.riplay.utils.conditional
import it.fast4x.riplay.utils.currentWindow
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.getIconQueueLoopState
import it.fast4x.riplay.utils.getScreenDimensions
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shouldBePlaying
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.windows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds


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
                    Modifier.combinedClickable(
                        enabled = enabled,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
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
    onDismiss: (QueueLoopType) -> Unit,
    onDiscoverClick: (Boolean) -> Unit,
    showQueue: Boolean,
) {

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings =
        appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val windowInsets = WindowInsets.systemBars
    val context = LocalContext.current
    val queueType = appearanceSettings.queueType
    val disableScrollingText = appearanceSettings.disableScrollingText
    val binder = LocalPlayerServiceBinder.current
    binder?.hybridPlayer ?: return
    val binderPlayer = binder.hybridPlayer

    val queueLoopType = appSettings.queueLoopType
    val excludeSongsIfAreVideos = appSettings.videoContentMode.excluded
    val menuState = LocalGlobalSheetState.current
    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    var mediaItemIndex by remember {
        mutableIntStateOf(
            (if (binderPlayer.mediaItemCount == 0) -1 else binderPlayer.currentMediaItemIndex)
                ?: 0
        )
    }
    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    var windows by remember { mutableStateOf(binderPlayer.currentTimeline.windows) }
    var windowsFiltered by remember { mutableStateOf(windows) }
    var shouldBePlaying by remember { mutableStateOf(binder.hybridPlayer.shouldBePlaying) }

    binderPlayer.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex =
                    (if (binder.hybridPlayer.mediaItemCount == 0) -1 else binder.hybridPlayer.currentMediaItemIndex)
                        ?: 0
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                Timber.d("REODER - TIMELINE-CHANGED count=${windows.size} firstUid=${windows.firstOrNull()?.uid}")
                mediaItemIndex =
                    (if (binder.hybridPlayer?.mediaItemCount == 0) -1 else binder.hybridPlayer?.currentMediaItemIndex)
                        ?: 0
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.hybridPlayer.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.hybridPlayer.shouldBePlaying
            }
        }
    }

    val queueslist by Database.queues().collectAsState(initial = emptyList())
    val selectedQueue = Database.selectedQueue().collectAsState(defaultQueue()).let {
        if (it.value == null) defaultQueue() else it.value
    }
    val rippleIndication = ripple(bounded = false)
    val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "")
    //val isReorderDisabled = appSettings.reorderInQueueEnabled
    var listMediaItems = remember { mutableListOf<MediaItem>() }
    var listMediaItemsIndex = remember { mutableListOf<Int>() }
    var selectQueueItems by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var showConfirmDeleteAllDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val thumbnailRoundness = appearanceSettings.thumbnailRoundness


    if (showConfirmDeleteAllDialog) {
        ConfirmationDialog(
            text = "Do you really want to clean queue?",
            onDismiss = { showConfirmDeleteAllDialog = false },
            onConfirm = {
                showConfirmDeleteAllDialog = false

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            Database.clearQueuedMediaItems()
                        } catch (e: Exception) {
                            Timber.e("Queue UI: Errore durante la pulizia del DB: ${e.message}")
                        }
                    }
                    // Torniamo sul thread principale per svuotare il player multimediale
                    binderPlayer.clearMediaItems()
                    listMediaItems.clear()
                    listMediaItemsIndex.clear()
                }
            }
        )
    }


    var plistName by remember { mutableStateOf("") }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportType.CSV.mimeExport)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            coroutineScope.launch(Dispatchers.IO) {
                val songs =
                    if (listMediaItems.isEmpty()) windows?.map { it.mediaItem.asSong } else listMediaItems.map { it.asSong }
                Exporter.exportTo(
                    ExportType.CSV,
                    context,
                    uri,
                    songs ?: return@launch,
                    "",
                    plistName
                )
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
                    SmartMessage(
                        context.resources.getString(R.string.info_not_find_app_create_doc),
                        type = PopupType.Warning,
                        context = context
                    )
                }
            }
        )
    }

    val hapticFeedback = LocalHapticFeedback.current
    val showButtonPlayerDiscover = appearanceSettings.showButtonPlayerDiscover
    val discoverIsEnabled = appSettings.discoverIsEnabled
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter: String? by rememberSaveable { mutableStateOf(null) }
    var showQueues by rememberSaveable { mutableStateOf(false) }
    val maxHeightQueuesList by remember { derivedStateOf { getScreenDimensions().height.dp.div(8) } }
    val heightQueues = animateDpAsState(if (showQueues) maxHeightQueuesList else 20.dp)

    var windowsInQueue by remember { mutableStateOf(windows) }
    var updateWindowsList by remember { mutableStateOf(false) }

    var finalFilteredWindows by remember { mutableStateOf<List<Timeline.Window>>(emptyList()) }


    val filteredItemsCount = finalFilteredWindows.size

    val isLoadingRadio by binder.isLoadingRadio.collectAsStateWithLifecycle()

    CustomModalBottomSheet(
        showSheet = showQueue,
        onDismissRequest = { onDismiss(queueLoopType) },
        containerColor = if (queueType == QueueType.Modern) colorPalette().background2.copy(alpha = 0.5f) else colorPalette().background2,
        contentColor = if (queueType == QueueType.Modern) colorPalette().background2.copy(alpha = 0.5f) else colorPalette().background2,
        modifier = Modifier
            .fillMaxWidth()
            .conditional(queueType == QueueType.Modern) { hazeEffect(state = hazeState) },
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 0.dp),
                color = colorPalette().background0,
                shape = thumbnailShape()
            ) {}
        },
        shape = thumbnailRoundness.shape(),
    ) {


        // ─── Root container ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(windowInsets.only(WindowInsetsSides.Horizontal).asPaddingValues())
                .background(if (queueType == QueueType.Modern) colorPalette().background1.copy(alpha = 0.7f) else colorPalette().background1)
                .fillMaxSize()
        ) {
            val lazyListState = rememberLazyListState()
            // Lista di appoggio che esiste SOLO durante il drag, altrimenti null


            // La snapshot dell'ordine ALL'INIZIO del drag (per calcolare from al commit)
            var dragStartSnapshot by remember { mutableStateOf<List<Timeline.Window>?>(null) }
            // La lista visiva DURANTE il drag (contratto libreria: mutazione immediata)
            var dragList by remember { mutableStateOf<List<Timeline.Window>?>(null) }
            // La chiave del brano trascinato (per lo scroller e il commit)
            var draggedKey by remember { mutableStateOf<String?>(null) }

            val displayList = dragList ?: finalFilteredWindows
            val isQueueReady = finalFilteredWindows.isNotEmpty()
            var initialScrollDone by remember { mutableStateOf(false) }

            // Verifico se il corrente è visibile nel viewport
            val isCurrentVisible by remember {
                derivedStateOf {
                    lazyListState.layoutInfo.visibleItemsInfo.any {
                        it.key == binderPlayer.currentWindow?.uid.toString()
                    }
                }
            }
            val showNowPlayingBar = !isCurrentVisible && !searching

            key(isQueueReady) {
                val reorderableLazyListState = rememberReorderableLazyListState(
                    lazyListState = lazyListState,
                    scrollThreshold = 56.dp,
                    scrollThresholdPadding = PaddingValues(96.dp),
                ) { from, to ->
                    Timber.d("REORDER - ONMOVE from=${from.key} to=${to.key}")
                    val currentUid = binderPlayer.currentWindow?.uid.toString()
                    if (from.key == currentUid) return@rememberReorderableLazyListState

                    val base = dragList ?: finalFilteredWindows.also { dragList = it }
                    draggedKey = from.key.toString()
                    val fromI = base.indexOfFirst { it.uid.toString() == from.key }
                    val toI = base.indexOfFirst { it.uid.toString() == to.key }
                    if (fromI >= 0 && toI >= 0 && fromI != toI) {
                        dragList = base.toMutableList().apply { add(toI, removeAt(fromI)) }  // stesso frame, subito
                    }
                }

                LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
                    if (reorderableLazyListState.isAnyItemDragging) {
                        Timber.d("REORDER - COMMIT waiting=true")
                        return@LaunchedEffect
                    }
                    val key = draggedKey
                    val start = dragStartSnapshot
                    val shadow = dragList
                    Timber.d("REORDER - COMMIT eval key=$key start=${start?.size} shadow=${shadow?.size}")
                    if (key == null || start == null || shadow == null) {
                        Timber.d("REORDER - COMMIT ABORT: null field")
                        return@LaunchedEffect
                    }
                    val from = start.indexOfFirst { it.uid.toString() == key }
                    val to = shadow.indexOfFirst { it.uid.toString() == key }
                    Timber.d("REORDER - COMMIT from=$from to=$to")
                    if (from in start.indices && to in shadow.indices && from != to) {
                        binderPlayer.moveMediaItem(from, to)
                        Timber.d("REORDER - COMMIT EXECUTED")
                    }
                    // l'assorbimento (non il reset qui) fa il resto
                }

                // Aggiungiamo 'windows' tra le chiavi del LaunchedEffect, così ogni volta che ExoPlayer cambia brano,
                // riordina la coda o aggiunge una canzone, l'elenco visivo si aggiorna all'istante!
                LaunchedEffect(
                    Unit,
                    selectedQueue,
                    updateWindowsList,
                    filter,
                    windows,
                    blacklisted.value,
                    excludeSongsIfAreVideos
                ) {
                    val filterCharSequence = filter.toString()

                    // 1. GESTIONE FILTRO RICERCA (Con ramo else di riallineamento!)
                    if (!filter.isNullOrBlank()) {
                        windowsFiltered = windows.filter {
                            it.mediaItem.mediaMetadata.title?.contains(filterCharSequence, true) ?: false
                                    || it.mediaItem.mediaMetadata.artist?.contains(
                                filterCharSequence,
                                true
                            ) ?: false
                        }
                    } else {
                        windowsFiltered =
                            windows // Se non cerco nulla, la lista filtrata coincide con la timeline reale!
                    }

                    val win = if (searching) windowsFiltered else windows

                    // 2. GESTIONE FILTRO CODE PERSONALIZZATE
                    val queueFiltered = if (selectedQueue == defaultQueue()) win else win.filter {
                        it.mediaItem.mediaMetadata.extras?.getLong(
                            "idQueue",
                            defaultQueueId()
                        ) == selectedQueue?.id
                    }

                    // 3. CALCOLO STRUTTURALE DELLA BLACKLIST A MONTE
                    val blacklistedPaths = blacklisted.value?.map { it.path }?.toSet().orEmpty()
                    finalFilteredWindows = queueFiltered.filter { item ->
                        (item.mediaItem.mediaId !in blacklistedPaths) ||
                                (item.mediaItem.isVideo == !excludeSongsIfAreVideos)
                    }
//        finalFilteredWindows = queueFiltered.filter { item ->
//            val isNotBlacklisted =
//                blacklisted.value?.map { it.path }?.contains(item.mediaItem.mediaId) == false
//            val isVideoMatch = item.mediaItem.isVideo == !excludeSongsIfAreVideos
//            isNotBlacklisted || isVideoMatch
//        }

                    // ── Absorb: la shadow muore solo quando la timeline riproduce il suo ordine ──
                    if (dragList != null && !reorderableLazyListState.isAnyItemDragging) {
                        val shadowUids = dragList?.map { it.uid.toString() }
                        val freshUids = finalFilteredWindows.map { it.uid.toString() }
                        if (shadowUids == freshUids) {
                            dragList = null
                            dragStartSnapshot = null
                            draggedKey = null
                        }
                        Timber.d("REORDER - ABSORB shadow=${dragList?.size} fresh=${finalFilteredWindows.size} match=${shadowUids == freshUids}")
                    }

                }

                LaunchedEffect(dragList) {
                    if (dragList != null) {
                        delay(800.milliseconds)
                        if (dragList != null) {   // l'assorbimento non è avvenuto: ripristino dalla realtà
                            dragList = null
                            dragStartSnapshot = null
                            draggedKey = null
                        }
                    }
                }

                // Calcolo il prossimo brano e mostrare l'etichetta
                val currentDisplayIndex = displayList.indexOfFirst {
                    it.uid == binderPlayer.currentWindow?.uid
                }
                val upNextWindow = displayList.getOrNull(currentDisplayIndex + 1)
                val upNextKey = upNextWindow?.uid?.toString()

                // ─── Main LazyColumn ────────────────────────────────────────────────
                LazyColumn(state = lazyListState, modifier = Modifier) {

                    stickyHeader {
                        var editQueue by remember { mutableStateOf(false) }
                        var addQueue by remember { mutableStateOf(false) }
                        var queueToEdit by remember { mutableStateOf<Queues?>(null) }

                        if (editQueue || addQueue) {
                            EditQueueDialog(
                                onDismiss = {
                                    editQueue = false; addQueue = false; queueToEdit = null
                                },
                                queue = queueToEdit,
                                setValue = { queue ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                if (editQueue) Database.update(queue) else Database.insert(
                                                    queue
                                                )
                                            } catch (e: Exception) {
                                                Timber.e("Queue UI: Errore salvataggio coda custom: ${e.message}")
                                            }
                                        }
                                        editQueue = false; addQueue = false; queueToEdit = null
                                    }
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
                                    .padding(
                                        windowInsets.only(WindowInsetsSides.Top).asPaddingValues()
                                    )
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
                                    contentPadding = windowInsets.only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
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
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    Database.toggleSelectQueue(
                                                        it
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                menuState.display {
                                                    QueueItemMenu(
                                                        navController = navController,
                                                        onDismiss = { menuState.hide() },
                                                        onEdit = {
                                                            queueToEdit = it; editQueue =
                                                            true; addQueue = false
                                                        },
                                                        onRemove = {
                                                            coroutineScope.launch {
                                                                withContext(Dispatchers.IO) {
                                                                    try {
                                                                        Database.deleteQueue(it.id)
                                                                    } catch (e: Exception) {
                                                                        Timber.e("Queue UI: Errore rimozione coda custom: ${e.message}")
                                                                    }
                                                                }
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
                                        val keyboardController =
                                            LocalSoftwareKeyboardController.current
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
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 10.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {},
                                                        icon = R.drawable.search,
                                                        color = colorPalette().accent,
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .size(16.dp)
                                                    )
                                                }
                                                Box(
                                                    contentAlignment = Alignment.CenterStart,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 30.dp)
                                                ) {
                                                    if (filter?.isEmpty() ?: true)
                                                        BasicText(
                                                            text = stringResource(R.string.search),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = typography().xs.semiBold.secondary.copy(
                                                                color = colorPalette().textDisabled
                                                            )
                                                        )

                                                    innerTextField()
                                                }
                                            },
                                            modifier = Modifier
                                                .height(34.dp)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(50))
                                                .background(colorPalette().background0)
                                                .border(
                                                    0.5.dp,
                                                    colorPalette().accent.copy(alpha = 0.4f),
                                                    RoundedCornerShape(50)
                                                )
                                                .focusRequester(focusRequester)
                                                .onFocusChanged {
                                                    if (!it.hasFocus) {
                                                        keyboardController?.hide()
                                                        if (filter?.isBlank() == true) {
                                                            filter = null; searching = false
                                                        }
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

                    // ─── Se la coda è vuota ─────────────────────────────────────────────────
                    if (displayList.isEmpty()) {
                        item(key = "empty-state") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (filter.isNullOrBlank()) R.drawable.musical_notes
                                        else R.drawable.search
                                    ),
                                    contentDescription = null,
                                    tint = colorPalette().textDisabled,
                                    modifier = Modifier.size(48.dp)
                                )
                                BasicText(
                                    text = when {
                                        filter.isNullOrBlank() -> stringResource(R.string.queue_empty)
                                        else -> stringResource(R.string.queue_no_results)
                                    },
                                    style = typography().s.semiBold.copy(color = colorPalette().textSecondary)
                                )
                                // e, per la coda vuota, un invito ad ascoltare qualcosa
                                if (filter.isNullOrBlank()) {
                                    BasicText(
                                        text = stringResource(R.string.queue_empty_hint),
                                        style = typography().xs.copy(color = colorPalette().textDisabled)
                                    )
                                }
                            }
                        }
                    }

                    // ─── Song items ─────────────────────────────────────────────────
                    items(
                        displayList,
                        key = { window -> window.uid.toString() }
                    ) { window ->

                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = window.uid.toString()
                        ) { isDragging ->

                            val interactionSource = remember { MutableInteractionSource() }
                            val currentItem by rememberUpdatedState(window)
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            val isPlayingThisMediaItem =
                                binderPlayer.currentMediaItemIndex == window.firstPeriodIndex


                            // Spring-based scale when dragging
                            val itemScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.025f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "dragScale"
                            )

                            Column(                                              // ← L'IMPILATORE
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .graphicsLayer {
                                        scaleX = itemScale; scaleY = itemScale
                                        shadowElevation = if (isDragging) 24f else 0f
                                        shape = thumbnailRoundness.shape()
                                    }
                            ) {

                                // Etichetta prossimo brano
                                if (window.uid.toString() == upNextKey && !isDragging) {
                                    Timber.d("REORDER - NEXT UP: ${window.mediaItem.mediaMetadata.title}")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .padding(start = 16.dp, bottom = 2.dp)
                                    ) {
                                        BasicText(
                                            text = "${stringResource(R.string.queue_up_next).uppercase()} > ${window.mediaItem.mediaMetadata.title}",
                                            style = typography().xxs.semiBold.copy(
                                                color = colorPalette().accent,
                                                letterSpacing = 1.5.sp
                                            )
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .graphicsLayer {
                                            scaleX = itemScale
                                            scaleY = itemScale
                                            shadowElevation = if (isDragging) 24f else 0f
                                            shape = thumbnailRoundness.shape()
                                            clip = false
                                            alpha = 1f
                                        }
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .graphicsLayer {
                                                scaleX = itemScale; scaleY = itemScale
                                            }
                                            .background(colorPalette().background0)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            SwipeableQueueItem(
                                                mediaItem = window.mediaItem,
                                                onPlayNext = {
                                                    binder.hybridPlayer?.addNext(
                                                        window.mediaItem,
                                                        context,
                                                        selectedQueue ?: defaultQueue()
                                                    )
                                                    updateWindowsList = !updateWindowsList
                                                },
                                                onRemoveFromQueue = {
                                                    binder.hybridPlayer?.removeMediaItem(currentItem.firstPeriodIndex)
                                                    SmartMessage(
                                                        "${context.resources.getString(R.string.deleted)} ${currentItem.mediaItem.mediaMetadata.title}",
                                                        type = PopupType.Warning, context = context
                                                    )
                                                    updateWindowsList = !updateWindowsList
                                                },
                                                onEnqueue = {
                                                    binder.hybridPlayer?.enqueue(
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
                                                                        Color.Black.copy(alpha = 0.25f),
                                                                        shape = thumbnailShape()
                                                                    )
                                                                    .size(Dimensions.thumbnails.song)
                                                            ) {
                                                                NowPlayingSongIndicator(
                                                                    window.mediaItem.mediaId,
                                                                    binder.hybridPlayer
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
                                                                        listMediaItemsIndex.add(
                                                                            window.firstPeriodIndex
                                                                        )
                                                                    } else {
                                                                        listMediaItems.remove(window.mediaItem)
                                                                        listMediaItemsIndex.remove(
                                                                            window.firstPeriodIndex
                                                                        )
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
                                                                            updateWindowsList =
                                                                                !updateWindowsList
                                                                        },
                                                                        onInfo = {},
                                                                        disableScrollingText = disableScrollingText,
                                                                        onBlacklist = {
                                                                            insertOrUpdateBlacklist(
                                                                                window.mediaItem.asSong
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                                hapticFeedback.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                            },
                                                            onClick = {
                                                                if (!selectQueueItems) {
                                                                    if (isPlayingThisMediaItem) {
                                                                        if (shouldBePlaying == true) binderPlayer?.pause() else binderPlayer?.play()
                                                                    } else {
                                                                        binderPlayer?.seekToDefaultPosition(
                                                                            window.firstPeriodIndex
                                                                        )
                                                                        binderPlayer?.prepare()
                                                                        binderPlayer?.playWhenReady =
                                                                            true
                                                                    }
                                                                } else checkedState.value =
                                                                    !checkedState.value
                                                            }
                                                        )
                                                        // Now playing: accent tint background; others: standard
                                                        .background(
                                                            color = when {
                                                                isPlayingThisMediaItem -> colorPalette().accent.copy(
                                                                    alpha = 0.09f
                                                                )

                                                                queueType == QueueType.Modern -> Color.Transparent
                                                                else -> colorPalette().background0
                                                            }
                                                        )
                                                        // Left padding to clear the accent bar
                                                        .padding(start = if (isPlayingThisMediaItem) 4.dp else 0.dp)

                                                )
                                            }
                                        }

                                        // ── Drag handle ──────────────────────────────────────
                                        if (!isPlayingThisMediaItem) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    //.border(2.dp, Color.Magenta)
                                                    .background(colorPalette().background0)
                                                    .width(32.dp)
                                                    .height(56.dp)
                                                    .draggableHandle(
                                                        enabled = true,
                                                        interactionSource = interactionSource,
                                                        onDragStarted = {
                                                            Timber.d("REORDER - DRAG-START snapshot=${dragStartSnapshot?.size} key=$draggedKey")
                                                            dragStartSnapshot =
                                                                finalFilteredWindows   // ← QUESTA è la variabile che prima non esisteva
                                                            dragList =
                                                                finalFilteredWindows // creiamo la lista shadow
                                                            draggedKey =
                                                                window.uid.toString()         // ← serve allo scroller dal primo frame
                                                            Timber.d("REORDER - DRAG-START (post) snapshot=${dragStartSnapshot?.size} key=$draggedKey")
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                        },
                                                        onDragStopped = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                        }
                                                    )
                                            ) {
                                                IconButton(
                                                    icon = R.drawable.reorder,
                                                    color = colorPalette().textDisabled,
                                                    indication = rippleIndication,
                                                    onClick = {},
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }

                    item {
                        if (isLoadingRadio) {
                            Loader()
                        }
                    }

                    item(key = "footer", contentType = 0) {
                        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                    }
                } // end LazyColumn

            }


            val currentListIndex = finalFilteredWindows
                .indexOfFirst { it.uid == binderPlayer.currentWindow?.uid }

            // apertura: scroll alla corrente
            LaunchedEffect(
                Unit
                //finalFilteredWindows
            ) {
                if (!lazyListState.isScrollInProgress && currentListIndex >= 0) {
                    lazyListState.scrollToItem(currentListIndex.coerceAtMost(0), 0)
                    // oppure animateScrollToItem(-300)
                }
            }

            // cambio brano a coda aperta: segue SOLO se il brano non è visibile
            LaunchedEffect(mediaItemIndex) {
                if (initialScrollDone) return@LaunchedEffect
                if (lazyListState.isScrollInProgress || currentListIndex < 0) return@LaunchedEffect
                val visible = lazyListState.layoutInfo.visibleItemsInfo.any {
                    it.key == binderPlayer.currentWindow?.uid.toString()
                }
                if (!visible) lazyListState.animateScrollToItem(currentListIndex, -300)
                initialScrollDone = true
            }

//            LaunchedEffect(Unit) {
//                if (!lazyListState.isScrollInProgress)
//                    windows?.indexOf(binderPlayer?.currentWindow)?.let {
//                        lazyListState.animateScrollToItem(it, -300)
//                    }
//
//            }


            // ─── FLOATING ACTION BAR ─────────────────────────────────────────────
            val density = LocalDensity.current
            val bottomInset =
                with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

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
                            UnifiedMiniPlayer(
                                showPlayer = { onDismiss(queueLoopType) },
                                hidePlayer = {})
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
                                    coroutineScope.launch {
                                        val new =
                                            appSettingsManager.activeSettings.value.copy(
                                                discoverIsEnabled = !discoverIsEnabled
                                            )
                                        appSettingsManager.updateSettings(new)
                                    }
                                    onDiscoverClick(discoverIsEnabled)
                                },
                                onLongClick = {
                                    SmartMessage(
                                        context.resources.getString(R.string.discoverinfo),
                                        context = context
                                    )
                                }
                            )
                        }

                        /*
                        // Reorder lock
                        ActionIconButton(
                            icon = if (isReorderDisabled) R.drawable.locked else R.drawable.unlocked,
                            size = 22,
                            onClick = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            reorderInQueueEnabled = !isReorderDisabled
                                        )
                                    )
                                }
                            }
                        )

                         */

                        // Loop — accent when non-default
                        ActionIconButton(
                            icon = getIconQueueLoopState(queueLoopType),
                            active = queueLoopType != QueueLoopType.Default,
                            size = 22,
                            onClick = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(
                                            queueLoopType = setQueueLoopState(
                                                queueLoopType
                                            )
                                        )
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )

                        /*
                        // Shuffle
                        ActionIconButton(
                            icon = R.drawable.shuffle,
                            enabled = !reorderableLazyListState.isAnyItemDragging,
                            size = 22,
                            onClick = {
                                coroutineScope.launch {
                                    lazyListState.smoothScrollToTop()
                                }.invokeOnCompletion {
                                    binderPlayer?.shuffleQueue()
                                    updateWindowsList = !updateWindowsList
                                }
                            }
                        )

                         */

                        // More (ellipsis) — rightmost
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = windows?.isNotEmpty() == true) {
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
                                                        binder.hybridPlayer?.removeMediaItem(
                                                            listMediaItemsIndex[i]
                                                        )
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
                                                        windows?.forEachIndexed { index, song ->
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
                                                                    cleanPrefix(id ?: ""),
                                                                    windows?.filterNot {
                                                                        it.mediaItem.mediaId.startsWith(
                                                                            LOCAL_KEY_PREFIX
                                                                        )
                                                                    }?.map { it.mediaItem }
                                                                        ?: emptyList()
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
                                                        listMediaItems.forEachIndexed { index, song ->
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
                                                    } else {
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            playlistPreview.playlist.browseId.let { id ->
                                                                addToYtPlaylist(
                                                                    playlistPreview.playlist.id,
                                                                    position,
                                                                    cleanPrefix(id ?: ""),
                                                                    listMediaItems.filterNot {
                                                                        it.mediaId.startsWith(
                                                                            LOCAL_KEY_PREFIX
                                                                        )
                                                                    }
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
                                    if (windows?.isNotEmpty() == true) colorPalette().text else colorPalette().textDisabled
                                ),
                                modifier = Modifier
                                    .size(22.dp)
                                    .alpha(if (windows?.isNotEmpty() == true) 1f else 0.4f)
                            )
                        }

                        // Dismiss arrow — leftmost
                        ActionIconButton(
                            icon = R.drawable.chevron_down,
                            size = 22,
                            onClick = { onDismiss(queueLoopType) }
                        )
                    }
                }

            }

            // E' una idea per un barra now playing visibile
            /*
            Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                val currentMedia = binderPlayer.currentMediaItem ?: return@Row
                AnimatedVisibility(
                    visible = showNowPlayingBar,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .padding(bottom = (Dimensions.miniPlayerHeight * 2) + bottomInset + 8.dp)
                        .zIndex(5f)
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .background(colorPalette().background1, thumbnailRoundness.shape())
                    ) {
                        // thumbnail micro con l'indicatore
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = currentMedia.mediaMetadata.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            NowPlayingSongIndicator(
                                currentMedia.mediaId,
                                binder.hybridPlayer
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.queue_now_playing).uppercase(),
                                style = typography().xxs.semiBold.copy(
                                    color = colorPalette().accent,
                                    letterSpacing = 1.sp
                                )
                            )
                            BasicText(
                                text = cleanPrefix(
                                    currentMedia.mediaMetadata.title?.toString() ?: ""
                                ),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            */

        } // end root Box
    } // end Queue

}