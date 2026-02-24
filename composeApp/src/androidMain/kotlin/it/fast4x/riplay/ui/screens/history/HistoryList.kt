package it.fast4x.riplay.ui.screens.history

import androidx.annotation.OptIn
import androidx.annotation.Px
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HistoryPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.data.models.DateAgo
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenuLibrary
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.HistoryType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.preferences.historyTypeKey
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.forcePlay
import java.time.temporal.ChronoUnit
import java.util.TimeZone


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.models.EventWithSong
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.screens.history.groupByDateAgo
import kotlinx.coroutines.Dispatchers
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HistoryList(
    navController: NavController
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px


    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateSetOf<String>() }
    val selectedMediaItems = remember { mutableStateListOf<MediaItem>() }


    LaunchedEffect(selectedIds.size) {
        // External sync?
    }


    val search = Search.init()


    val eventsState = Database.events()
        .map { events ->
            if (parentalControlEnabled) events.filter { !it.song.title.startsWith(EXPLICIT_PREFIX) }
            else events
        }
        .map { events ->
            events.groupByDateAgo()
        }
        .collectAsState(initial = emptyMap(), context = Dispatchers.IO)


    val buttonsList = mutableListOf(HistoryType.History to stringResource(R.string.history))
    if (isYtLoggedIn()) buttonsList += HistoryType.OnlineHistory to stringResource(R.string.online_history)

    var historyType by rememberPreference(historyTypeKey, HistoryType.History)
    var historyPage by persist<HistoryPage>("home/historyPage")

    LaunchedEffect(Unit, historyType) {
        if (isYtLoggedIn()) {
            historyPage = EnvironmentExt.getHistory(setLogin = true).getOrNull()
        }
    }

    val colorPalette = colorPalette()

    val state = rememberLazyListState()

    Column(
        modifier = Modifier
            .background(color = colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent()) Dimensions.contentWidthRightBar else 1f
            )
    ) {
        LazyListContainer(state) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .background(color = colorPalette().background0)
                    .fillMaxSize()
            ) {
                item(key = "header") {
                    HeaderWithIcon(
                        title = stringResource(R.string.history),
                        iconId = R.drawable.history,
                        enabled = false,
                        showIcon = false,
                        modifier = Modifier,
                        onClick = {}
                    )
                }

                item(key = "tabList") {
                    ButtonsRow(
                        buttons = buttonsList,
                        currentValue = historyType,
                        onValueUpdate = { historyType = it },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    Column {
                        search.ToolBarButton()
                        search.SearchBar(this)
                    }
                }

                when (historyType) {
                    HistoryType.History -> {
                        eventsState.value.forEach { (dateAgo, events) ->
                            renderHistoryGroup(
                                headerTitle = when (dateAgo) {
                                    DateAgo.Today -> context.resources.getString((R.string.today))
                                    DateAgo.Yesterday -> context.resources.getString((R.string.yesterday))
                                    DateAgo.ThisWeek -> context.resources.getString((R.string.this_week))
                                    DateAgo.LastWeek -> context.resources.getString((R.string.last_week))
                                    is DateAgo.Other -> dateAgo.date.format(
                                        DateTimeFormatter.ofPattern(
                                            "yyyy/MM"
                                        )
                                    )
                                },
                                modifier = Modifier.background(
                                    color = colorPalette.background3,
                                    shape = thumbnailRoundness.shape()
                                )
                            ) {
                                val filteredEvents = events
                                    .filter { matchesSearch(it .song.title, it.song.artistsText, search.input) }
                                    .distinctBy { it.song.id }

                                items(filteredEvents, key = { it.event.id }) { event ->
                                    val mediaItem = event.song.asMediaItem
                                    HistoryItemRow(
                                        mediaItem = mediaItem,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        thumbnailSizePx = thumbnailSizePx,
                                        binder = binder,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = mediaItem.mediaId in selectedIds,
                                        onSelectionToggle = { selected ->
                                            if (selected) {
                                                selectedIds.add(mediaItem.mediaId)
                                                selectedMediaItems.add(mediaItem)
                                            } else {
                                                selectedIds.remove(mediaItem.mediaId)
                                                selectedMediaItems.remove(mediaItem)
                                            }
                                        },
                                        navController = navController,
                                        menuState = menuState,
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        }
                    }
                    HistoryType.OnlineHistory -> {
                        historyPage?.sections?.forEach { section ->
                            renderHistoryGroup(
                                headerTitle = section.title,
                                modifier = Modifier.background(
                                    color = colorPalette.background3,
                                    shape = thumbnailRoundness.shape()
                                )
                            ) {
                                val filteredSongs = section.songs
                                    .filter { matchesSearch(it.title, it.authors?.joinToString { it.name.toString() }, search.input) }
                                    .map { it.asMediaItem }

                                items(filteredSongs, key = { it.mediaId }) { song ->
                                    HistoryItemRow(
                                        mediaItem = song,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        thumbnailSizePx = thumbnailSizePx,
                                        binder = binder,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = song.mediaId in selectedIds,
                                        onSelectionToggle = { selected ->
                                            if (selected) {
                                                selectedIds.add(song.mediaId)
                                                selectedMediaItems.add(song)
                                            } else {
                                                selectedIds.remove(song.mediaId)
                                                selectedMediaItems.remove(song)
                                            }
                                        },
                                        navController = navController,
                                        menuState = menuState,
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.renderHistoryGroup(
    headerTitle: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    stickyHeader(key = headerTitle) {
        Title(
            title = headerTitle,
            modifier = modifier
        )
    }
    content()
}

@kotlin.OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
@UnstableApi
@Composable
fun HistoryItemRow(
    mediaItem: MediaItem,
    thumbnailSizeDp: Dp,
    thumbnailSizePx: Int,
    binder: PlayerService.Binder?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionToggle: (Boolean) -> Unit,
    navController: NavController,
    menuState: GlobalSheetState,
    disableScrollingText: Boolean
) {
    SongItem(
        song = mediaItem,
        thumbnailSizeDp = thumbnailSizeDp,
        thumbnailSizePx = thumbnailSizePx,
        onThumbnailContent = {
            NowPlayingSongIndicator(mediaItem.mediaId, binder?.player)
        },
        trailingContent = {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorPalette().accent,
                        uncheckedColor = colorPalette().text
                    )
                )
            }
        },
        modifier = Modifier
            .combinedClickable(
                onLongClick = {
                    menuState.display {
                        NonQueuedMediaItemMenuLibrary(
                            navController = navController,
                            mediaItem = mediaItem,
                            onDismiss = { menuState.hide() },
                            onInfo = {
                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${mediaItem.mediaId}")
                            },
                            disableScrollingText = disableScrollingText
                        )
                    }
                },
                onClick = {
                    if (isSelectionMode) {
                        onSelectionToggle(!isSelected)
                    } else {
                        binder?.player?.forcePlay(mediaItem)
                    }
                }
            )
            .background(color = colorPalette().background0)
            .animateContentSize()
    )
}

private fun matchesSearch(title: String?, artists: String?, query: String): Boolean {
    if (query.isEmpty()) return true
    val q = query.lowercase()
    return title?.contains(q, ignoreCase = true) == true ||
            artists?.contains(q, ignoreCase = true) == true
}


private fun List<EventWithSong>.groupByDateAgo(): Map<DateAgo, List<EventWithSong>> {
    val today = LocalDate.now()
    val thisMonday = today.with(DayOfWeek.MONDAY)
    val lastMonday = thisMonday.minusDays(7)

    return this.groupBy {
        val date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(it.event.timestamp),
            ZoneId.systemDefault()
        ).toLocalDate()

        val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
        when {
            daysAgo == 0 -> DateAgo.Today
            daysAgo == 1 -> DateAgo.Yesterday
            date >= thisMonday -> DateAgo.ThisWeek
            date >= lastMonday -> DateAgo.LastWeek
            else -> DateAgo.Other(date.withDayOfMonth(1))
        }
    }.toSortedMap(compareBy { dateAgo ->
        when (dateAgo) {
            is DateAgo.Today -> 0L
            is DateAgo.Yesterday -> 1L
            is DateAgo.ThisWeek -> 2L
            is DateAgo.LastWeek -> 3L
            is DateAgo.Other -> ChronoUnit.DAYS.between(dateAgo.date, today)
        }
    })
}

/*
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HistoryPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.data.models.DateAgo
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenuLibrary
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.HistoryType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.preferences.historyTypeKey
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.forcePlay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone

@kotlin.OptIn(ExperimentalTextApi::class)
@OptIn(UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HistoryList(
    navController: NavController
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val today = LocalDate.now()
    val thisMonday = today.with(DayOfWeek.MONDAY)
    val lastMonday = thisMonday.minusDays(7)
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val events = Database.events()
        .map { events ->
            if (parentalControlEnabled)
                events.filter { !it.song.title.startsWith(EXPLICIT_PREFIX) } else events
        }
        .map { events ->
            events.groupBy {
                val date = //it.event.timestamp.toLocalDate()
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it.event.timestamp),
                    TimeZone.getDefault().toZoneId()).toLocalDate()
                val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
                when {
                    daysAgo == 0 -> DateAgo.Today
                    daysAgo == 1 -> DateAgo.Yesterday
                    date >= thisMonday -> DateAgo.ThisWeek
                    date >= lastMonday -> DateAgo.LastWeek
                    else -> DateAgo.Other(date.withDayOfMonth(1))
                }
            }.toSortedMap(compareBy { dateAgo ->
                when (dateAgo) {
                    DateAgo.Today -> 0L
                    DateAgo.Yesterday -> 1L
                    DateAgo.ThisWeek -> 2L
                    DateAgo.LastWeek -> 3L
                    is DateAgo.Other -> ChronoUnit.DAYS.between(dateAgo.date, today)
                }
            })
        }
        .collectAsState(initial = emptyMap(), context = Dispatchers.IO)

    val buttonsList = mutableListOf(HistoryType.History to stringResource(R.string.history))

    if (isYtLoggedIn())
        buttonsList += HistoryType.OnlineHistory to stringResource(R.string.online_history)

    var historyType by rememberPreference(historyTypeKey, HistoryType.History)

    var historyPage by persist<HistoryPage>("home/historyPage")

    LaunchedEffect(Unit, historyType) {
        if (isYtLoggedIn()) {
            historyPage = EnvironmentExt.getHistory(setLogin = true).getOrNull()
        }
    }

    var listMediaItems = remember {
        mutableListOf<MediaItem>()
    }

    var selectItems by remember {
        mutableStateOf(false)
    }

    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val search = Search.init()

    Column (
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        val state = rememberLazyListState()
        LazyListContainer(
            state = state
        ) {
            LazyColumn(
                state = state,
//                contentPadding = LocalPlayerAwareWindowInsets.current
//                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {

                item(key = "header", contentType = 0) {
                    HeaderWithIcon(
                        title = stringResource(R.string.history),
                        iconId = R.drawable.history,
                        enabled = false,
                        showIcon = false,
                        modifier = Modifier,
                        onClick = {}
                    )
                }

                item(
                    key = "tabList", contentType = 0,
                ) {
                    ButtonsRow(
                        buttons = buttonsList,
                        currentValue = historyType,
                        onValueUpdate = { historyType = it },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                    )
                }

                item {
                    Column {
                        search.ToolBarButton()
                        search.SearchBar(this)
                    }
                }

                if (historyType == HistoryType.History)
                    events.value.forEach { (dateAgo, events) ->
                        stickyHeader {
                            Title(
                                title = when (dateAgo) {
                                    DateAgo.Today -> stringResource(R.string.today)
                                    DateAgo.Yesterday -> stringResource(R.string.yesterday)
                                    DateAgo.ThisWeek -> stringResource(R.string.this_week)
                                    DateAgo.LastWeek -> stringResource(R.string.last_week)
                                    is DateAgo.Other -> dateAgo.date.format(
                                        DateTimeFormatter.ofPattern(
                                            "yyyy/MM"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .background(
                                        colorPalette().background3,
                                        shape = thumbnailRoundness.shape()
                                    )


                            )
                        }

                        items(
                            items = events.map {
                                it.apply {
                                    this.event.timestamp = this.timestampDay!!
                                }
                            }
                                .filter {
                                    when{
                                        search.input.isNotEmpty() -> {
                                            it.song.title.contains(search.input, ignoreCase = true)
                                                    || it.song.artistsText?.contains(search.input, ignoreCase = true) == true
                                        }
                                        else -> true
                                    }
                                }
                                .distinctBy { it.song.id },
                            key = { it.event.id }
                        ) { event ->
                            val checkedState = rememberSaveable { mutableStateOf(false) }

                            SongItem(
                                song = event.song,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailSizePx = thumbnailSizePx,
                                onThumbnailContent = {
                                    NowPlayingSongIndicator(
                                        event.song.asMediaItem.mediaId,
                                        binder?.player
                                    )
                                },
                                trailingContent = {
                                    if (selectItems)
                                        Checkbox(
                                            checked = checkedState.value,
                                            onCheckedChange = {
                                                checkedState.value = it
                                                if (it) listMediaItems.add(event.song.asMediaItem) else
                                                    listMediaItems.remove(event.song.asMediaItem)
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
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenuLibrary(
                                                    navController = navController,
                                                    mediaItem = event.song.asMediaItem,
                                                    onDismiss = {
                                                        menuState.hide()
                                                        //forceRecompose = true
                                                    },
                                                    onInfo = {
                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${event.song.id}")
                                                    },
                                                    disableScrollingText = disableScrollingText
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.player?.forcePlay(event.song.asMediaItem)
                                            //fastPlay(event.song.asMediaItem, binder)
                                        }
                                    )
                                    .background(color = colorPalette().background0)
                                    .animateItem(),
                            )

                        }
                    }

                if (historyType == HistoryType.OnlineHistory)
                    historyPage?.sections?.forEach { section ->
                        stickyHeader {
                            Title(
                                title = section.title,
                                modifier = Modifier
                                    .background(
                                        colorPalette().background3,
                                        shape = thumbnailRoundness.shape()
                                    )


                            )
                        }
                        items(
                            items = section.songs
                                .filter {
                                    when{
                                        search.input.isNotEmpty() -> {
                                            it.title?.contains(search.input, ignoreCase = true) == true
                                                    || it.authors?.joinToString { it.name.toString() }?.contains(search.input, ignoreCase = true) == true
                                        }
                                        else -> true
                                    }
                                }
                                .map { it.asMediaItem },
                                //.filter { it.mediaId.isNotEmpty() },
                            key = { it.mediaId }
                        ) { song ->
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            SongItem(
                                song = song,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailSizePx = thumbnailSizePx,
                                onThumbnailContent = {
                                    NowPlayingSongIndicator(song.mediaId, binder?.player)
                                },
                                trailingContent = {
                                    if (selectItems)
                                        Checkbox(
                                            checked = checkedState.value,
                                            onCheckedChange = {
                                                checkedState.value = it
                                                if (it) listMediaItems.add(song) else
                                                    listMediaItems.remove(song)
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
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenuLibrary(
                                                    navController = navController,
                                                    mediaItem = song,
                                                    onDismiss = {
                                                        menuState.hide()
                                                        //forceRecompose = true
                                                    },
                                                    onInfo = {
                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.mediaId}")
                                                    },
                                                    disableScrollingText = disableScrollingText
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.player?.forcePlay(song)
                                            //fastPlay(song, binder)
                                        }
                                    )
                                    .background(color = colorPalette().background0)
                                    .animateItem(),
                            )
                        }

                    }

            }
        }

    }
}
*/
