package it.fast4x.riplay.ui.screens.ondevice

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.compose.persist.persistList
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ArtistItem
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AddToPlaylistArtistSongs
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.extensions.preferences.maxSongsInQueueKey
import it.fast4x.riplay.utils.LazyListContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun OnDeviceArtistItems(
    navController: NavController,
    artistId: String,
    artistName: String? = null,
    artistItem: ArtistItem = ArtistItem.Songs,
    disableScrollingText: Boolean,
    onDismiss: () -> Unit
) {

    if (artistId == "") {
        onDismiss()
        return
    }

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val selectedQueue = LocalSelectedQueue.current

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)


    val context = LocalContext.current


    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    val thumbnailSizeDp = Dimensions.thumbnails.album //+ 24.dp
    val thumbnailSizePx = thumbnailSizeDp.px
    val maxSongsInQueue by rememberPreference(maxSongsInQueueKey, MaxSongs.`500`)
    //var forceRecompose by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var songs by persistList<Song?>("")
    var albums by persistList<Album?>("")
    LaunchedEffect(Unit) {
        when (artistItem) {
            ArtistItem.Songs -> {
                Database.artistAllSongs(artistId).collect {
                    songs = it
                }
            }
            ArtistItem.Albums -> {
                    Database.artistAlbums(artistId).collect {
                        albums = it
                    }
                }
            }

        println("OnDeviceArtistItems songs ${songs.size} albums ${albums.size}")
    }

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

        if (artistItem == ArtistItem.Songs) {
            val state = rememberLazyListState()
            LazyListContainer(
                state = state
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
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
                            title = stringResource(id = R.string.songs),
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
                                color = if (songs.any { it?.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .any { Database.getLikedAt(it.mediaId) != -1L }
                                                ) {
                                                    songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .filter { Database.getLikedAt(it.mediaId) != -1L }
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
                                color = if (songs.any { it?.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .any { Database.getLikedAt(it.mediaId) != -1L }
                                                ) {
                                                    val filteredArtistSongs = songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .filter { Database.getLikedAt(it.mediaId) != -1L }
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
                                color = if (songs.any { it?.thumbnailUrl != "" }) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                if (songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .any { Database.getLikedAt(it.mediaId) != -1L }
                                                ) {
                                                    val filteredArtistSongs = songs
                                                        .mapNotNull { it?.asMediaItem }
                                                        .filter { Database.getLikedAt(it.mediaId) != -1L }
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
                                                        //forceRecompose = true
                                                    },
                                                    mediaItems = songs.mapNotNull { it?.asMediaItem },
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
                        }
                    }
                    items(songs.mapNotNull { it?.asMediaItem }) { item ->

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
                                                        //forceRecompose = true
                                                    },
                                                    onInfo = {
                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.mediaId}")
                                                    },
                                                    mediaItem = item,
                                                    disableScrollingText = disableScrollingText
                                                )
                                            };
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        },
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val filteredArtistSongs = songs
                                                    .mapNotNull { it?.asMediaItem }
                                                    .filter { Database.getLikedAt(it.mediaId) != -1L }
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
                                //disableScrollingText = disableScrollingText,
                                //isNowPlaying = binder?.player?.isNowPlaying(item.mediaId) ?: false,
                                //forceRecompose = forceRecompose
                            )
                        }

                    }

                }
            }
        } else {
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
                                title = stringResource(R.string.albums),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .padding(horizontal = 16.dp)
                            )
                        }

                    }
                    items(items = albums) { item ->
                        if (item == null) return@items
                        AlbumItem(
                            album = item,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            alternative = true,
                            yearCentered = true,
                            showAuthors = true,
                            modifier = Modifier.clickable(onClick = {
                                navController.navigate(route = "${NavRoutes.onDeviceAlbum.name}/${item.id}")
                            }),
                            disableScrollingText = disableScrollingText
                        )

                    }
                }
            }
        }

    }

}
