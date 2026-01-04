package it.fast4x.riplay.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.requests.ArtistItemsPage
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AddToPlaylistArtistSongs
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.addToYtLikedSongs
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.isExplicit
import org.dailyislam.android.utilities.isNetworkConnected
import it.fast4x.riplay.extensions.preferences.maxSongsInQueueKey
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.commonutils.setLikeState
import it.fast4x.riplay.enums.ItemSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.preferences.songSortOrderKey
import it.fast4x.riplay.ui.components.themed.SortMenu
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ArtistOverviewItems(
    navController: NavController,
    browseId: String,
    params: String? = null,
    artistName: String? = null,
    sectionName: String? = null,
    disableScrollingText: Boolean,
    onDismiss: () -> Unit
) {

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val selectedQueue = LocalSelectedQueue.current

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)

    val context = LocalContext.current

    var showYoutubeLikeConfirmDialog by remember {
        mutableStateOf(false)
    }

    var notLikedSongs by persistList<MediaItem>("")
    var totalMinutesToLike by remember { mutableStateOf("") }

    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    var artistItemsPage by remember { mutableStateOf<ArtistItemsPage?>(null) }


    val thumbnailSizeDp = Dimensions.thumbnails.album //+ 24.dp
    val thumbnailSizePx = thumbnailSizeDp.px
    val maxSongsInQueue by rememberPreference(maxSongsInQueueKey, MaxSongs.`500`)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        artistItemsPage = EnvironmentExt.getArtistItemsPage(
            BrowseEndpoint(
                browseId = browseId,
                params = params
            )
        ).completed().getOrNull()

        println("ArtistOverviewItems artistItemsPage size: ${artistItemsPage?.items}")
    }

    if (artistItemsPage == null) return

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {

        if (artistItemsPage?.items?.firstOrNull() is Environment.SongItem) {
            val artistSongs = artistItemsPage!!.items
                .map{it as Environment.SongItem}
                .map { it.asMediaItem }




            if (showYoutubeLikeConfirmDialog) {
                Database.asyncTransaction {
                    notLikedSongs = artistSongs.filter { getLikedAt(it.mediaId) in listOf(-1L,null)}
                }
                totalMinutesToLike = formatAsDuration(notLikedSongs.size.toLong()*1000)
                ConfirmationDialog(
                    text = "$totalMinutesToLike "+stringResource(R.string.do_you_really_want_to_like_all),
                    onDismiss = { showYoutubeLikeConfirmDialog = false },
                    onConfirm = {
                        showYoutubeLikeConfirmDialog = false
                        CoroutineScope(Dispatchers.IO).launch {
                            addToYtLikedSongs(notLikedSongs)
                        }
                    }
                )
            }

            val listState = rememberLazyListState()
            LazyListContainer(
                state = listState,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = WindowInsets.systemBars.asPaddingValues()
                ) {
                    item {
                        Title(
                            title = artistName ?: "",
                            modifier = sectionTextModifier,
                            icon = R.drawable.chevron_down,
                            onClick = onDismiss
                        )
                        TitleSection(
                            title = sectionName ?: "",
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .padding(horizontal = 16.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                        ) {
                            HeaderIconButton(
                                icon = R.drawable.shuffle,
                                color = if (artistSongs.any { it.asSong.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (artistSongs.any { Database.getLikedAt(it.mediaId) != -1L }) {
                                                    artistSongs.filter { Database.getLikedAt(it.mediaId) != -1L }
                                                        .let { songs ->
                                                            if (songs.isNotEmpty()) {
                                                                val itemsLimited =
                                                                    if (songs.size > maxSongsInQueue.number) songs.shuffled()
                                                                        .take(maxSongsInQueue.number.toInt()) else songs
                                                                withContext(Dispatchers.Main) {
                                                                    binder?.stopRadio()
                                                                    binder?.player?.forcePlayFromBeginning(
                                                                        itemsLimited.shuffled()
                                                                    )
                                                                }
                                                            }
                                                        }
                                                } else {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.disliked_this_collection),
                                                        type = PopupType.Error,
                                                        context = context
                                                    )
                                                }
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
                                icon = R.drawable.enqueue,
                                //enabled = artistSongs.any { it.mediaMetadata.artworkUri.toString() != "" && it.song.likedAt != -1L },
                                color = if (artistSongs.any { it.asSong.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (artistSongs.any { Database.getLikedAt(it.mediaId) != -1L }) {
                                                    val filteredArtistSongs =
                                                        artistSongs.filter { Database.getLikedAt(it.mediaId) != -1L }
                                                    withContext(Dispatchers.Main) {
                                                        binder?.player?.enqueue(
                                                            filteredArtistSongs,
                                                            context
                                                        )
                                                    }
                                                } else {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.disliked_this_collection),
                                                        type = PopupType.Error,
                                                        context = context
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_enqueue_songs),
                                                context = context
                                            )
                                        }
                                    )
                            )
                            HeaderIconButton(
                                icon = R.drawable.play_skip_forward,
                                //enabled = artistSongs.any { it.mediaMetadata.artworkUri.toString() != "" && it.song.likedAt != -1L },
                                color = if (artistSongs.any { it.asSong.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (artistSongs.any { Database.getLikedAt(it.mediaId) != -1L }) {
                                                    val filteredArtistSongs =
                                                        artistSongs.filter { Database.getLikedAt(it.mediaId) != -1L }
                                                    withContext(Dispatchers.Main) {
                                                        binder?.player?.addNext(
                                                            filteredArtistSongs, context,
                                                            selectedQueue ?: defaultQueue()
                                                        )
                                                    }
                                                } else {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.disliked_this_collection),
                                                        type = PopupType.Error,
                                                        context = context
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.play_next),
                                                context = context
                                            )
                                        }
                                    )
                            )

                            HeaderIconButton(
                                icon = R.drawable.add_in_playlist,
                                color = colorPalette().text,
                                onClick = {},
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            menuState.display {
                                                AddToPlaylistArtistSongs(
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.hide()

                                                    },
                                                    mediaItems = artistSongs,
                                                    onClosePlayer = {
                                                        onDismiss()
                                                    },
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_add_in_playlist),
                                                context = context
                                            )
                                        }
                                    )
                            )
                            HeaderIconButton(
                                icon = R.drawable.heart,
                                enabled = artistSongs.isNotEmpty(),
                                color = colorPalette().text,
                                onClick = {},
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (!isNetworkConnected(appContext()) && isYtSyncEnabled()) {
                                                SmartMessage(
                                                    appContext().resources.getString(R.string.no_connection),
                                                    context = appContext(),
                                                    type = PopupType.Error
                                                )
                                            } else if (!isYtSyncEnabled()) {
                                                artistSongs.forEachIndexed { _, song ->
                                                    Database.asyncTransaction {
                                                        if (like(
                                                                song.mediaId,
                                                                setLikeState(song.asSong.likedAt)
                                                            ) == 0
                                                        ) {
                                                            insert(song, Song::toggleLike)
                                                        }
                                                    }
                                                }
                                                SmartMessage(
                                                    context.resources.getString(R.string.done),
                                                    context = context
                                                )
                                            } else {
                                                showYoutubeLikeConfirmDialog = true
                                            }
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.add_to_favorites),
                                                context = context
                                            )
                                        }
                                    )
                            )
                        }
                    }
                    items(artistSongs) { item ->

                        println("ArtistOverviewItems item: ${item}")

                        if (parentalControlEnabled && item.isExplicit) return@items

                        SwipeablePlaylistItem(
                            mediaItem = item,
                            onPlayNext = {
                                binder?.player?.addNext(
                                    item,
                                    queue = selectedQueue ?: defaultQueue()
                                )
                            },
                            onEnqueue = {
                                binder?.player?.enqueue(item, queue = it)
                            }
                        ) {
                            SongItem(
                                song = item,
                                onThumbnailContent = {
                                    NowPlayingSongIndicator(item.mediaId, binder?.player)
                                },
                                thumbnailSizeDp = songThumbnailSizeDp,
                                thumbnailSizePx = songThumbnailSizePx,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.hide()

                                                    },
                                                    mediaItem = item,
                                                    onInfo = {
                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.mediaId}")
                                                    },
                                                    disableScrollingText = disableScrollingText,
                                                )
                                            };
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        },
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val filteredArtistSongs =
                                                    artistSongs.filter { Database.getLikedAt(it.mediaId) != -1L }
                                                if (item in filteredArtistSongs) {
                                                    withContext(Dispatchers.Main) {
                                                        binder?.player?.forcePlayAtIndex(
                                                            filteredArtistSongs,
                                                            filteredArtistSongs.indexOf(item)
                                                        )
                                                    }
                                                } else {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.disliked_this_song),
                                                        type = PopupType.Error,
                                                        context = context
                                                    )
                                                }

                                            }
                                        }
                                    ),
                            )
                        }

                    }

                }
            }
        } else {
            var sortBy by remember{ mutableStateOf(ItemSortBy.Year)}
            var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)
            val sortOrderIconRotation by animateFloatAsState(
                targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
            )
            var items by remember (artistItemsPage) {
                mutableStateOf(artistItemsPage?.items)
            }
            LaunchedEffect(Unit, sortOrder, sortBy) {
                when (sortOrder) {
                    SortOrder.Ascending -> {
                        items = items?.sortedBy {
                            when (sortBy) {
                                ItemSortBy.Year -> (it as Environment.AlbumItem).year
                                ItemSortBy.Title -> it.title
                            }
                        }
                    }
                    SortOrder.Descending -> {
                        items = items?.sortedByDescending {
                            when (sortBy) {
                                ItemSortBy.Year -> (it as Environment.AlbumItem).year
                                ItemSortBy.Title -> it.title
                            }
                        }
                    }
                }
            }

            val sortMenu: @Composable () -> Unit = {
                SortMenu(
                    title = stringResource(R.string.sorting_order),
                    onDismiss = menuState::hide,
                    onTitle = { sortBy = ItemSortBy.Title},
                    onYear = { sortBy = ItemSortBy.Year}
                )
            }


            val gridState = rememberLazyGridState()
            LazyListContainer(
                state = gridState
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(Dimensions.thumbnails.album + 24.dp),
                    modifier = Modifier
                        .background(colorPalette().background0)
                        .fillMaxSize(),
                    contentPadding = WindowInsets.systemBars.asPaddingValues()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Title(
                                title = artistName ?: "",
                                modifier = sectionTextModifier,
                                icon = R.drawable.chevron_down,
                                onClick = onDismiss
                            )
                            TitleSection(
                                title = sectionName ?: "",
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .padding(horizontal = 16.dp)
                            )
                        }

                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                        ) {
                            HeaderIconButton(
                                icon = R.drawable.arrow_up,
                                color = colorPalette().text,
                                onClick = {},
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .graphicsLayer {
                                        rotationZ =
                                            sortOrderIconRotation
                                    }
                                    .combinedClickable(
                                        onClick = { sortOrder = !sortOrder },
                                        onLongClick = {
                                            menuState.display {
                                                sortMenu()
                                            }
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = sortBy.textName,
                                style = typography().s,
                                color = colorPalette().text,
                                modifier = Modifier.clickable {
                                    menuState.display {
                                        sortMenu()
                                    }
                                }
                            )

                        }
                    }

                    items(
                        items = items!!
                    ) { item ->
                        when (item) {

                            is Environment.AlbumItem -> {
                                var albumById by remember { mutableStateOf<Album?>(null) }
                                LaunchedEffect(item) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        albumById = Database.album(item.key).firstOrNull()
                                    }
                                }
                                AlbumItem(
                                    album = item,
                                    thumbnailSizePx = thumbnailSizePx,
                                    thumbnailSizeDp = thumbnailSizeDp,
                                    alternative = true,
                                    yearCentered = true,
                                    showAuthors = true,
                                    isYoutubeAlbum = albumById?.isYoutubeAlbum == true,
                                    modifier = Modifier.clickable(onClick = {
                                        navController.navigate(route = "${NavRoutes.album.name}/${item.key}")
                                    }),
                                    disableScrollingText = disableScrollingText
                                )
                            }

                            is Environment.PlaylistItem -> {
                                PlaylistItem(
                                    playlist = item,
                                    alternative = true,
                                    thumbnailSizePx = playlistThumbnailSizePx,
                                    thumbnailSizeDp = playlistThumbnailSizeDp,
                                    disableScrollingText = disableScrollingText,
                                    modifier = Modifier.clickable(onClick = {
                                        navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                    })
                                )
                            }

                            is Environment.VideoItem -> {
                                VideoItem(
                                    video = item,
                                    thumbnailHeightDp = playlistThumbnailSizeDp,
                                    thumbnailWidthDp = playlistThumbnailSizeDp,
                                    disableScrollingText = disableScrollingText,
                                    modifier = Modifier.clickable(onClick = {
                                        binder?.stopRadio()
//                                    if (isVideoEnabled())
//                                        binder?.player?.playOnline(item.asMediaItem)
//                                    else
                                        binder?.player?.forcePlay(item.asMediaItem)
                                        //fastPlay(item.asMediaItem, binder)
                                    })
                                )
                            }

                            is Environment.ArtistItem -> {
                                ArtistItem(
                                    artist = item,
                                    alternative = true,
                                    thumbnailSizePx = artistThumbnailSizePx,
                                    thumbnailSizeDp = artistThumbnailSizeDp,
                                    disableScrollingText = disableScrollingText,
                                    modifier = Modifier.clickable(onClick = {
                                        navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                    })
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }

    }

}
