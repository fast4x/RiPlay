package it.fast4x.riplay.ui.screens.searchresult

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.compose.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.albumPage
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.components.SwipeableAlbumItem
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.ArtistItemPlaceholder
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.PlaylistItemPlaceholder
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.items.VideoItemPlaceholder
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.searchResultScreenTabIndexKey
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.utils.forcePlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchResultScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    query: String,
    onSearchAgain: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val selectedQueue = LocalSelectedQueue.current

    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabIndexChanges) = rememberPreference(searchResultScreenTabIndexKey, 0)

    val hapticFeedback = LocalHapticFeedback.current

    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val menuState = LocalMenuState.current
    var filterContentType by remember { mutableStateOf(ContentType.All) }

                val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                    Title(
                        title = stringResource(R.string.search_results_for),
                        verticalPadding = 4.dp
                    )
                    Title(
                        title = query,
                        icon = R.drawable.pencil,
                        onClick = {
                            /*
                                    context.persistMap?.keys?.removeAll {
                                       it.startsWith("searchResults/$query/")
                                    }
                                    onSearchAgain()
                                    */
                            navController.navigate("searchScreenRoute/${query}")
                        },
                        verticalPadding = 4.dp
                    )

                    Column(
                        modifier = Modifier.background(colorPalette().accent.copy(alpha = 0.15f))
                    ) {
                        Title2Actions(
                            title = "Filter content type",
                            onClick1 = {
                                menuState.display {
                                    Menu {
                                        ContentType.entries.forEach {
                                            MenuEntry(
                                                icon = it.icon,
                                                text = it.textName,
                                                onClick = {
                                                    filterContentType = it
                                                    menuState.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        BasicText(
                            text = when (filterContentType) {
                                ContentType.All -> "All"
                                ContentType.Official -> "Official"
                                ContentType.UserGenerated -> "User Generated"

                            },
                            style = typography().xxs.secondary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        )
                    }


                    /*
                    Header(
                        title = query,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    /*
                                    context.persistMap?.keys?.removeAll {
                                       it.startsWith("searchResults/$query/")
                                    }
                                    onSearchAgain()
                                    */
                                    navController.navigate("searchScreenRoute/${query}")
                                }
                            }
                    )
                     */
            }

            val emptyItemsText = stringResource(R.string.no_results_found)

            ScreenContainer(
                navController,
                tabIndex,
                onTabIndexChanges,
                miniPlayer,
                navBarContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(1, stringResource(R.string.albums), R.drawable.music_album)
                    item(2, stringResource(R.string.artists), R.drawable.music_artist)
                    item(3, stringResource(R.string.videos), R.drawable.video)
                    item(4, stringResource(R.string.playlists), R.drawable.playlist)
                    item(5, stringResource(R.string.featured), R.drawable.featured_playlist)
                    item(6, stringResource(R.string.podcasts), R.drawable.podcast)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when ( currentTabIndex ) {
                        0 -> {
                            val localBinder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailSizeDp = Dimensions.thumbnails.song
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/songs",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Environment.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Environment.SearchFilter.Song.value
                                            ),
                                            fromMusicShelfRendererContent = Environment.SongItem.Companion::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.SongItem.Companion::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { song ->
                                    //Log.d("mediaItem",song.toString())
                                    if (parentalControlEnabled && song.explicit)
                                        return@ItemsPage

                                    SwipeablePlaylistItem(
                                        mediaItem = song.asMediaItem,
                                        onPlayNext = {
                                            localBinder?.player?.addNext(song.asMediaItem, queue = selectedQueue ?: defaultQueue())
                                        },
                                        onEnqueue = {
                                            localBinder?.player?.enqueue(song.asMediaItem, queue = it)
                                        }
                                    ) {
                                        //var forceRecompose by remember { mutableStateOf(false) }
                                        SongItem(
                                            song = song,
                                            thumbnailContent = {
                                                NowPlayingSongIndicator(song.asMediaItem.mediaId, binder?.player)
                                            },
                                            thumbnailSizePx = thumbnailSizePx,
                                            thumbnailSizeDp = thumbnailSizeDp,
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
                                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.key}")
                                                                },
                                                                mediaItem = song.asMediaItem,
                                                                disableScrollingText = disableScrollingText
                                                            )
                                                        };
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                    },
                                                    onClick = {
                                                        localBinder?.stopRadio()
                                                        localBinder?.player?.forcePlay(song.asMediaItem)
                                                        //fastPlay(song.asMediaItem, localBinder)
                                                        //forceRecompose = true
                                                        localBinder?.setupRadio(song.info?.endpoint)
                                                    }
                                                ),
                                            //disableScrollingText = disableScrollingText,
                                            //isNowPlaying = binder?.player?.isNowPlaying(song.key) ?: false,
                                            //forceRecompose = forceRecompose
                                        )
                                    }
                                },
                                itemPlaceholderContent = {
                                    SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                },
                                filterContentType = filterContentType
                            )
                        }

                        1 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/albums",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Environment.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Environment.SearchFilter.Album.value
                                            ),
                                            fromMusicShelfRendererContent = Environment.AlbumItem::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.AlbumItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { album ->
                                    var albumPage by persist<Environment.PlaylistOrAlbumPage?>("album/${album.key}/albumPage")
                                    SwipeableAlbumItem(
                                        albumItem = album,
                                        onPlayNext = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                Database
                                                    .album(album.key)
                                                    .combine(snapshotFlow { currentTabIndex }) { album, tabIndex -> album to tabIndex }
                                                    .collect {
                                                        if (albumPage == null)
                                                            withContext(Dispatchers.IO) {
                                                                Environment.albumPage(
                                                                    BrowseBody(
                                                                        browseId = album.key
                                                                    )
                                                                )
                                                                    ?.onSuccess { currentAlbumPage ->
                                                                        albumPage =
                                                                            currentAlbumPage

                                                                        println("mediaItem success home album songsPage ${currentAlbumPage.songsPage} description ${currentAlbumPage.description} year ${currentAlbumPage.year}")

                                                                        albumPage
                                                                            ?.songsPage
                                                                            ?.items
                                                                            ?.map(
                                                                                Environment.SongItem::asMediaItem
                                                                            )
                                                                            ?.let { it1 ->
                                                                                withContext(Dispatchers.Main) {
                                                                                    binder?.player?.addNext(
                                                                                        it1,
                                                                                        context,
                                                                                        selectedQueue ?: defaultQueue()
                                                                                    )
                                                                                }
                                                                            }
                                                                        println("mediaItem success add in queue album songsPage ${albumPage
                                                                            ?.songsPage
                                                                            ?.items?.size}")

                                                                    }
                                                                    ?.onFailure {
                                                                        println("mediaItem error searchResultScreen album ${it.stackTraceToString()}")
                                                                    }

                                                            }

                                                        //}
                                                    }

                                            }

                                        },
                                        onEnqueue = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                Database
                                                    .album(album.key)
                                                    .combine(snapshotFlow { currentTabIndex }) { album, tabIndex -> album to tabIndex }
                                                    .collect {
                                                        if (albumPage == null)
                                                            withContext(Dispatchers.IO) {
                                                                Environment.albumPage(
                                                                    BrowseBody(
                                                                        browseId = album.key
                                                                    )
                                                                )
                                                                    ?.onSuccess { currentAlbumPage ->
                                                                        albumPage =
                                                                            currentAlbumPage

                                                                        println("mediaItem success home album songsPage ${currentAlbumPage.songsPage} description ${currentAlbumPage.description} year ${currentAlbumPage.year}")

                                                                        albumPage
                                                                            ?.songsPage
                                                                            ?.items
                                                                            ?.map(
                                                                                Environment.SongItem::asMediaItem
                                                                            )
                                                                            ?.let { it1 ->
                                                                                withContext(Dispatchers.Main) {
                                                                                    binder?.player?.enqueue(
                                                                                        it1,
                                                                                        context
                                                                                    )
                                                                                }
                                                                            }
                                                                        println("mediaItem success add in queue album songsPage ${albumPage
                                                                            ?.songsPage
                                                                            ?.items?.size}")

                                                                    }
                                                                    ?.onFailure {
                                                                        println("mediaItem error searchResultScreen album ${it.stackTraceToString()}")
                                                                    }

                                                            }

                                                        //}
                                                    }

                                            }

                                        },
                                        onBookmark = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                Database
                                                    .album(album.key)
                                                    .combine(snapshotFlow { currentTabIndex }) { album, tabIndex -> album to tabIndex }
                                                    .collect {
                                                        if (albumPage == null)
                                                            withContext(Dispatchers.IO) {
                                                                Environment.albumPage(
                                                                    BrowseBody(
                                                                        browseId = album.key
                                                                    )
                                                                )
                                                                    ?.onSuccess { currentAlbumPage ->
                                                                        albumPage =
                                                                            currentAlbumPage

                                                                        println("mediaItem success home album songsPage ${currentAlbumPage.songsPage} description ${currentAlbumPage.description} year ${currentAlbumPage.year}")

                                                                        Database.upsert(
                                                                            Album(
                                                                                id = album.key,
                                                                                title = currentAlbumPage.title,
                                                                                thumbnailUrl = currentAlbumPage.thumbnail?.url,
                                                                                year = currentAlbumPage.year,
                                                                                authorsText = currentAlbumPage.authors
                                                                                    ?.joinToString(
                                                                                        ""
                                                                                    ) {
                                                                                        it.name
                                                                                            ?: ""
                                                                                    },
                                                                                shareUrl = currentAlbumPage.url,
                                                                                timestamp = System.currentTimeMillis(),
                                                                                bookmarkedAt = System.currentTimeMillis()
                                                                            ),
                                                                            currentAlbumPage
                                                                                .songsPage
                                                                                ?.items
                                                                                ?.map(
                                                                                    Environment.SongItem::asMediaItem
                                                                                )
                                                                                ?.onEach(
                                                                                    Database::insert
                                                                                )
                                                                                ?.mapIndexed { position, mediaItem ->
                                                                                    SongAlbumMap(
                                                                                        songId = mediaItem.mediaId,
                                                                                        albumId = album.key,
                                                                                        position = position
                                                                                    )
                                                                                }
                                                                                ?: emptyList()
                                                                        )

                                                                    }
                                                                    ?.onFailure {
                                                                        println("mediaItem error searchResultScreen album ${it.stackTraceToString()}")
                                                                    }

                                                            }
                                                    }
                                            }
                                        }
                                    ) {
                                        var albumById by remember { mutableStateOf<Album?>(null) }
                                        LaunchedEffect(album) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                albumById = Database.album(album.key).firstOrNull()
                                            }
                                        }
                                        AlbumItem(
                                            yearCentered = false,
                                            album = album,
                                            thumbnailSizePx = thumbnailSizePx,
                                            thumbnailSizeDp = thumbnailSizeDp,
                                            isYoutubeAlbum = albumById?.isYoutubeAlbum == true,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        navController.navigate("${NavRoutes.album.name}/${album.key}")
                                                    },
                                                    onLongClick = {}

                                                ),
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                },
                                filterContentType = filterContentType
                            )
                        }

                        2 -> {
                            val thumbnailSizeDp = 64.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/artists",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Environment.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Environment.SearchFilter.Artist.value
                                            ),
                                            fromMusicShelfRendererContent = Environment.ArtistItem::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.ArtistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { artist ->
                                    var artistById by remember { mutableStateOf<Artist?>(null) }
                                    LaunchedEffect(artist) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            artistById = Database.artist(artist.key).firstOrNull()
                                        }
                                    }
                                    ArtistItem(
                                        artist = artist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        isYoutubeArtist = artistById?.isYoutubeArtist == true,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                navController.navigate("${NavRoutes.artist.name}/${artist.key}")
                                            }),
                                        disableScrollingText = disableScrollingText,
                                        smallThumbnail = true
                                    )
                                },
                                itemPlaceholderContent = {
                                    ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                },
                                filterContentType = filterContentType
                            )
                        }

                        3 -> {
                            val localBinder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailHeightDp = 72.dp
                            val thumbnailWidthDp = 128.dp

                            ItemsPage(
                                tag = "searchResults/$query/videos",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Environment.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Environment.SearchFilter.Video.value
                                            ),
                                            fromMusicShelfRendererContent = Environment.VideoItem::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.VideoItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { video ->
                                    SwipeablePlaylistItem(
                                        mediaItem = video.asMediaItem,
                                        onPlayNext = {
                                            localBinder?.player?.addNext(video.asMediaItem, queue = selectedQueue ?: defaultQueue())
                                        },
                                        onEnqueue = {
                                            localBinder?.player?.enqueue(video.asMediaItem, queue = it)
                                        }
                                    ) {
                                        VideoItem(
                                            video = video,
                                            thumbnailWidthDp = thumbnailWidthDp,
                                            thumbnailHeightDp = thumbnailHeightDp,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onLongClick = {
                                                        menuState.display {
                                                            NonQueuedMediaItemMenu(
                                                                navController = navController,
                                                                mediaItem = video.asMediaItem,
                                                                onDismiss = menuState::hide,
                                                                onInfo = {
                                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${video.key}")
                                                                },
                                                                disableScrollingText = disableScrollingText
                                                            )
                                                        };
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                    },
                                                    onClick = {
                                                        localBinder?.stopRadio()
//                                                        if (isVideoEnabled)
//                                                            localBinder?.player?.playOnline(video.asMediaItem)
//                                                        else
                                                        localBinder?.player?.forcePlay(video.asMediaItem)
                                                        //binder?.setupRadio(video.info?.endpoint)
                                                        //fastPlay(video.asMediaItem, localBinder)
                                                        localBinder?.setupRadio(video.info?.endpoint)
                                                    }
                                                ),
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                },
                                itemPlaceholderContent = {
                                    VideoItemPlaceholder(
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        thumbnailWidthDp = thumbnailWidthDp
                                    )
                                },
                                filterContentType = filterContentType
                            )
                        }

                        4, 5 -> {
                            val thumbnailSizeDp = Dimensions.thumbnails.playlist
                            val thumbnailSizePx = thumbnailSizeDp.px
                            //val thumbnailSizeDp = 108.dp
                            //val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/${
                                    when (currentTabIndex) {
                                        4 -> "playlists"
                                        else -> "featured"
                                    }
                                }",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        val filter = when (currentTabIndex) {
                                            4 -> Environment.SearchFilter.CommunityPlaylist
                                            else -> Environment.SearchFilter.FeaturedPlaylist
                                        }

                                        Environment.searchPage(
                                            body = SearchBody(query = query, params = filter.value),
                                            fromMusicShelfRendererContent = Environment.PlaylistItem::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.PlaylistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { playlist ->
                                    var playlistById by remember { mutableStateOf<Playlist?>(null) }
                                    LaunchedEffect(playlist) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            playlistById = Database.playlist(playlist.key.substringAfter("VL")).firstOrNull()
                                        }
                                    }
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        showSongsCount = false,
                                        isYoutubePlaylist = playlistById?.isYoutubePlaylist == true,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                //playlistRoute(playlist.key)
                                                navController.navigate("${NavRoutes.playlist.name}/${playlist.key}")
                                            }),
                                        disableScrollingText = disableScrollingText
                                    )
                                },
                                itemPlaceholderContent = {
                                    PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                },
                                filterContentType = filterContentType
                            )
                        }

                        6 -> {
                            val thumbnailSizeDp = Dimensions.thumbnails.playlist
                            val thumbnailSizePx = thumbnailSizeDp.px
                            //val thumbnailSizeDp = 108.dp
                            //val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/podcasts",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        val filter = Environment.SearchFilter.Podcast

                                        Environment.searchPage(
                                            body = SearchBody(query = query, params = filter.value),
                                            fromMusicShelfRendererContent = Environment.PlaylistItem::from
                                        )
                                    } else {
                                        Environment.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Environment.PlaylistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        showSongsCount = false,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                //playlistRoute(playlist.key)
                                                println("mediaItem searchResultScreen playlist key ${playlist.key}")
                                                navController.navigate("${NavRoutes.podcast.name}/${playlist.key}")
                                            }),
                                        disableScrollingText = disableScrollingText
                                    )
                                },
                                itemPlaceholderContent = {
                                    PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                },
                                filterContentType = filterContentType
                            )
                        }
                    }
                }
            }
}
