package it.fast4x.riplay.ui.screens.history

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
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HistoryPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.EXPLICIT_PREFIX
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
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.extensions.preferences.historyTypeKey
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

    if (isLoggedIn())
        buttonsList += HistoryType.YTMHistory to stringResource(R.string.yt_history)

    var historyType by rememberPreference(historyTypeKey, HistoryType.History)

    var historyPage by persist<Result<HistoryPage>>("home/historyPage")
    LaunchedEffect(Unit, historyType) {
        if (isLoggedIn())
            historyPage = EnvironmentExt.getHistory()
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
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



    Column (
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
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
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
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
                        chips = buttonsList,
                        currentValue = historyType,
                        onValueUpdate = { historyType = it },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                    )
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
                            }.distinctBy { it.song.id },
                            key = { it.event.id }
                        ) { event ->
                            //val isLocal by remember { derivedStateOf { event.song.asMediaItem.isLocal } }
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            //var forceRecompose by remember { mutableStateOf(false) }

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
                                //disableScrollingText = disableScrollingText,
                                //isNowPlaying = binder?.player?.isNowPlaying(event.song.id) ?: false,
                                //forceRecompose = forceRecompose
                            )

                        }
                    }

                if (historyType == HistoryType.YTMHistory)
                    historyPage?.getOrNull()?.sections?.forEach { section ->
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
                            items = section.songs.map { it.asMediaItem }
                                .filter { it.mediaId.isNotEmpty() },
                            key = { it.mediaId }
                        ) { song ->
                            //val isLocal by remember { derivedStateOf { song.isLocal } }
                            val checkedState = rememberSaveable { mutableStateOf(false) }
                            //var forceRecompose by remember { mutableStateOf(false) }
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
                                //disableScrollingText = disableScrollingText,
                                //isNowPlaying = binder?.player?.isNowPlaying(song.mediaId) ?: false,
                                //forceRecompose = forceRecompose
                            )
                        }

                    }

            }
        }

    }
}

