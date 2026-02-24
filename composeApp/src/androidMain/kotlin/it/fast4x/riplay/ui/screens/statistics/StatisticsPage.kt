package it.fast4x.riplay.ui.screens.statistics

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.enums.MaxStatisticsItems
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.StatisticsCategory
import it.fast4x.riplay.enums.StatisticsType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.screens.settings.SettingsEntry
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.shimmer
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.extensions.preferences.maxStatisticsItemsKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.showStatsListeningTimeKey
import it.fast4x.riplay.extensions.preferences.statisticsCategoryKey
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.addNext
import timber.log.Timber
import kotlin.time.Duration.Companion.days

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.utils.updateOnlineAlbum
import it.fast4x.riplay.utils.updateOnlineArtist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
@UnstableApi
@Composable
fun StatisticsPage(
    navController: NavController,
    statisticsType: StatisticsType,
    onSwipeToLeft: () -> Unit = {},
    onSwipeToRight: () -> Unit = {}
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val context = LocalContext.current


    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px


    val thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)
    val showStatsListeningTime by rememberPreference(showStatsListeningTimeKey, true)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)

    var maxStatisticsItems by rememberPreference(maxStatisticsItemsKey, MaxStatisticsItems.`10`)
    var statisticsCategory by rememberPreference(statisticsCategoryKey, StatisticsCategory.Songs)

    var songs by persistList<Song>("statistics/songs")
    var allSongs by persistList<Song>("statistics/allsongs")
    var artists by persistList<Artist>("statistics/artists")
    var albums by persistList<Album>("statistics/albums")
    var playlists by persistList<PlaylistPreview>("statistics/playlists")

    val now = System.currentTimeMillis()
    val from = when (statisticsType) {
        StatisticsType.Today -> 1.days.inWholeMilliseconds
        StatisticsType.OneWeek -> 7.days.inWholeMilliseconds
        StatisticsType.OneMonth -> 30.days.inWholeMilliseconds
        StatisticsType.ThreeMonths -> 90.days.inWholeMilliseconds
        StatisticsType.SixMonths -> 180.days.inWholeMilliseconds
        StatisticsType.OneYear -> 365.days.inWholeMilliseconds
        StatisticsType.All -> 18250.days.inWholeMilliseconds // 50 years
    }


    val totalPlayTimes by remember(allSongs) {
        derivedStateOf { allSongs.sumOf { it.totalPlayTimeMs } }
    }


    LaunchedEffect(Unit, statisticsType, from) {
        if (showStatsListeningTime) {
            Database.songsMostPlayedByPeriod(from, now).collect { allSongs = it }
        }
    }
    LaunchedEffect(Unit, statisticsType, from, maxStatisticsItems) {
        Database.songsMostPlayedByPeriod(from, now, maxStatisticsItems.number).collect { songs = it }
    }
    LaunchedEffect(Unit, statisticsType, from, maxStatisticsItems) {
        Database.artistsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt())
            .collect { artists = it }
    }
    LaunchedEffect(Unit, statisticsType, from, maxStatisticsItems) {
        Database.albumsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt())
            .collect { albums = it }
    }
    LaunchedEffect(Unit, statisticsType, from, maxStatisticsItems) {
        Database.playlistsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt())
            .collect { playlists = it }
    }

    val buttonsList = listOf(
        StatisticsCategory.Songs to stringResource(R.string.songs),
        StatisticsCategory.Artists to stringResource(R.string.artists),
        StatisticsCategory.Albums to stringResource(R.string.albums),
        StatisticsCategory.Playlists to stringResource(R.string.playlists)
    )

    val lazyGridState = rememberLazyGridState()

    val localSelectedQueue = LocalSelectedQueue.current


    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Right) Dimensions.contentWidthRightBar else 1f
            )
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(minSize = if (statisticsCategory == StatisticsCategory.Songs) 200.dp else playlistThumbnailSizeDp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = windowInsets.only(WindowInsetsSides.End).asPaddingValues()
        ) {

            item(
                key = "header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                val (titleRes, iconRes) = when (statisticsType) {
                    StatisticsType.Today -> R.string.today to R.drawable.stat_today
                    StatisticsType.OneWeek -> R.string._1_week to R.drawable.stat_week
                    StatisticsType.OneMonth -> R.string._1_month to R.drawable.stat_month
                    StatisticsType.ThreeMonths -> R.string._3_month to R.drawable.stat_3months
                    StatisticsType.SixMonths -> R.string._6_month to R.drawable.stat_6months
                    StatisticsType.OneYear -> R.string._1_year to R.drawable.stat_year
                    StatisticsType.All -> R.string.all to R.drawable.calendar_clear
                }
                HeaderWithIcon(
                    title = stringResource(titleRes),
                    iconId = iconRes,
                    enabled = true,
                    showIcon = true,
                    onClick = {},
                    modifier = Modifier
                )
            }


            item(
                key = "header_tabs",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ButtonsRow(
                    buttons = buttonsList,
                    currentValue = statisticsCategory,
                    onValueUpdate = { statisticsCategory = it },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }


            if (statisticsCategory == StatisticsCategory.Songs && showStatsListeningTime) {
                item(
                    key = "headerListeningTime",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = thumbnailRoundness.shape(),
                        colors = CardDefaults.cardColors(containerColor = colorPalette().background4)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${allSongs.size} ${stringResource(R.string.statistics_songs_heard)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorPalette().text
                                )
                                Text(
                                    text = "${formatAsTime(totalPlayTimes)} ${stringResource(R.string.statistics_of_time_taken)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorPalette().textSecondary
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.musical_notes),
                                contentDescription = null,
                                tint = colorPalette().shimmer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }


            if (statisticsCategory == StatisticsCategory.Songs) {
                items(
                    items = songs,
                    key = { it.id }
                ) { song ->
                    SwipeablePlaylistItem(
                        mediaItem = song.asMediaItem,
                        onPlayNext = {
                            binder?.player?.addNext(song.asMediaItem, queue = localSelectedQueue ?: defaultQueue())
                        }
                    ) {

                        val index = songs.indexOf(song) + 1

                        SongItem(
                            song = song.asMediaItem,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            thumbnailSizePx = songThumbnailSizePx,
                            onThumbnailContent = {
                                Text(
                                    text = "$index",
                                    style = typography().s.semiBold
                                        .color(colorPalette().text)
                                        .align(androidx.compose.ui.text.style.TextAlign.Center),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .width(songThumbnailSizeDp)
                                        .align(Alignment.Center)
                                )
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
                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}")
                                                },
                                                disableScrollingText = disableScrollingText,
                                            )
                                        }
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map(Song::asMediaItem),
                                            songs.indexOf(song)
                                        )
                                    }
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }

            if (statisticsCategory == StatisticsCategory.Artists) {
                items(
                    items = artists,
                    key = { it.id }
                ) { artist ->

                    if (artist.thumbnailUrl?.toString() == "null") {
                        LaunchedEffect(artist.id) { updateOnlineArtist(artist.id) }
                    }

                    val index = artists.indexOf(artist) + 1
                    ArtistItem(
                        thumbnailUrl = artist.thumbnailUrl,
                        name = "$index. ${artist.name}",
                        showName = true,
                        subscribersCount = null,
                        thumbnailSizePx = artistThumbnailSizePx,
                        thumbnailSizeDp = artistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (artist.id.isNotEmpty()) {
                                navController.navigate("${NavRoutes.artist.name}/${artist.id}")
                            }
                        },
                        disableScrollingText = disableScrollingText
                    )
                }
            }


            if (statisticsCategory == StatisticsCategory.Albums) {
                items(
                    items = albums,
                    key = { it.id }
                ) { album ->
                    if (album.thumbnailUrl?.toString() == "null") {
                        LaunchedEffect(album.id) { updateOnlineAlbum(album.id) }
                    }

                    val index = albums.indexOf(album) + 1
                    AlbumItem(
                        thumbnailUrl = album.thumbnailUrl,
                        title = "$index. ${album.title}",
                        authors = album.authorsText,
                        year = album.year,
                        thumbnailSizePx = playlistThumbnailSizePx,
                        thumbnailSizeDp = playlistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (album.id.isNotEmpty())
                                navController.navigate("${NavRoutes.album.name}/${album.id}")
                        },
                        disableScrollingText = disableScrollingText
                    )
                }
            }


            if (statisticsCategory == StatisticsCategory.Playlists) {
                items(
                    items = playlists,
                    key = { it.playlist.id }
                ) { playlistPreview ->
                    val index = playlists.indexOf(playlistPreview) + 1


                    val thumbnails by remember {
                        Database.playlistThumbnailUrls(playlistPreview.playlist.id)
                            .distinctUntilChanged()
                            .map { list -> list.map { it.thumbnail(playlistThumbnailSizePx / 2) } }
                    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

                    PlaylistItem(
                        thumbnailContent = {
                            val thumbUrls = thumbnails.take(4)
                            if (thumbUrls.size == 1) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(thumbUrls.first())
                                        .setHeader("User-Agent", "Mozilla/5.0")
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    listOf(
                                        Alignment.TopStart, Alignment.TopEnd,
                                        Alignment.BottomStart, Alignment.BottomEnd
                                    ).forEachIndexed { idx, align ->
                                        thumbUrls.getOrNull(idx)?.let { url ->
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(url)
                                                    .setHeader("User-Agent", "Mozilla/5.0")
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .align(align)
                                                    .size(playlistThumbnailSizeDp / 2)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        songCount = playlistPreview.songCount,
                        name = "$index. ${playlistPreview.playlist.name}",
                        channelName = null,
                        thumbnailSizeDp = playlistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            val playlistId = playlistPreview.playlist.id.toString()
                            if (playlistId.isEmpty()) return@clickable

                            val pBrowseId = cleanPrefix(playlistPreview.playlist.browseId ?: "")
                            val route = if (pBrowseId.isNotEmpty()) {
                                "${NavRoutes.playlist.name}/$pBrowseId"
                            } else {
                                "${NavRoutes.localPlaylist.name}/$playlistId"
                            }
                            navController.navigate(route)
                        },
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}
