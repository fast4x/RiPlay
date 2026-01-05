package it.fast4x.riplay.ui.screens.home

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.AlbumsType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.AlbumsItemMenu
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.extensions.preferences.albumSortByKey
import it.fast4x.riplay.extensions.preferences.albumSortOrderKey
import it.fast4x.riplay.extensions.preferences.albumTypeKey
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.utils.getViewType
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar
import it.fast4x.riplay.ui.components.tab.ItemSize
import it.fast4x.riplay.ui.components.tab.Sort
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.tab.toolbar.Randomizer
import it.fast4x.riplay.ui.components.tab.toolbar.SongsShuffle
import it.fast4x.riplay.extensions.preferences.Preference.HOME_ALBUM_ITEM_SIZE
import it.fast4x.riplay.extensions.preferences.artistSortByKey
import it.fast4x.riplay.extensions.preferences.artistSortOrderKey
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.autoSyncToolbutton
import it.fast4x.riplay.extensions.preferences.autosyncKey
import it.fast4x.riplay.ui.components.themed.EnumsMenu
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.importYTMLikedAlbums
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.viewTypeToolbutton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.map

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    navController: NavController,
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val menuState = LocalGlobalSheetState.current
    //val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val selectedQueue = LocalSelectedQueue.current
    val lazyGridState = rememberLazyGridState()

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var items by persistList<Album>( "home/albums" )
    //var itemsToFilter by persistList<Album>( "home/artists" )
    //var filterBy by rememberPreference(filterByKey, FilterBy.All)
    //val (colorPalette, typography) = LocalAppearance.current

    var itemsOnDisplay by persistList<Album>( "home/albums/on_display" )

    val search = Search.init()

//    val sort = Sort.init(
//        albumSortOrderKey,
//        AlbumSortBy.entries,
//        rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
//    )

    val itemSize = ItemSize.init( HOME_ALBUM_ITEM_SIZE )

    val randomizer = object: Randomizer<Album> {
        override fun getItems(): List<Album> = itemsOnDisplay
        override fun onClick(index: Int) = onAlbumClick( itemsOnDisplay[index] )
    }
    val shuffle = SongsShuffle.init {
        Database.songsInAllBookmarkedAlbums().map { it.map( Song::asMediaItem ) }
    }

    var albumType by rememberPreference(albumTypeKey, AlbumsType.Favorites )
    val buttonsList = AlbumsType.entries.map { it to it.textName }
    val coroutineScope = rememberCoroutineScope()

    var sortBy by rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
    var sortOrder by rememberPreference(albumSortOrderKey, SortOrder.Descending)
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    LaunchedEffect( sortBy, sortOrder, albumType ) {
        when ( albumType ) {
            AlbumsType.Favorites -> Database.albums( sortBy, sortOrder ).collect { items = it }
            AlbumsType.Library -> Database.albumsInLibrary( sortBy, sortOrder ).collect { items = it.filter { it.isYoutubeAlbum } }
            AlbumsType.OnDevice -> Database.albumsOnDevice( sortBy, sortOrder ).collect { items = it }
            AlbumsType.All -> Database.albumsWithSongsSaved( sortBy, sortOrder ).collect { items = it }

        }
    }

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Album.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    LaunchedEffect( items, search.input ) {
        val scrollIndex = lazyGridState.firstVisibleItemIndex
        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items.filter {
            it.title?.contains( search.input, true) ?: false
                    || it.year?.contains( search.input, true) ?: false
                    || it.authorsText?.contains( search.input, true) ?: false
            }
            .filter {item -> blacklisted.value?.map { it.path }?.contains(item.id) == false }

        lazyGridState.scrollToItem( scrollIndex, scrollOffset )
    }

    if (albumType == AlbumsType.Library || albumType == AlbumsType.OnDevice) {
        if (items.any{it.thumbnailUrl == null}) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    items.filter { it.thumbnailUrl == null }.forEach { album ->
                        coroutineScope.launch(Dispatchers.IO) {
                            Database.asyncTransaction {
                                val albumThumbnail = albumThumbnailFromSong(album.id)
                                update(album.copy(thumbnailUrl = albumThumbnail))
                            }
                        }
                    }
                }
            }
        }
    }

    val sync = autoSyncToolbutton(R.string.autosync_albums)

    val doAutoSync by rememberPreference(autosyncKey, false)
    var justSynced by rememberSaveable { mutableStateOf(!doAutoSync) }

    val viewType = viewTypeToolbutton(R.string.viewType)

    var refreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    fun refresh() {
        if (refreshing) return
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            justSynced = false
            delay(500)
            refreshing = false
        }
    }

    // START: Import YTM subscribed channels
    LaunchedEffect(justSynced, doAutoSync) {
        if (!justSynced && importYTMLikedAlbums())
            justSynced = true
    }

    val sortMenu: @Composable () -> Unit = {
        EnumsMenu(
            title = stringResource(R.string.sorting_order),
            onDismiss = menuState::hide,
            selectedValue = sortBy.menuItem,
            onValueSelected = { sortBy = AlbumSortBy.entries[it.ordinal] },
            values = AlbumSortBy.entries.map { it.menuItem },
            valueText = { stringResource(it.titleId) }
        )
    }

    PullToRefreshBox(
        refreshing = refreshing,
        onRefresh = { refresh() }
    ) {
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
            Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
                TabHeader(R.string.albums) {
                    HeaderInfo(itemsOnDisplay.size.toString(), R.drawable.music_album)

                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )
                    Text(
                        text = stringResource(sortBy.textId),
                        style = typography().s,
                        color = colorPalette().text,
                        modifier = Modifier.clickable {
                            menuState.display {
                                sortMenu()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                                onClick = {
                                    sortOrder =
                                        if (sortOrder == SortOrder.Ascending)
                                            SortOrder.Descending
                                        else SortOrder.Ascending
                                },
                                onLongClick = {
                                    menuState.display {
                                        sortMenu()
                                    }
                                }

                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
                        //.padding(bottom = 8.dp)
                        .fillMaxWidth()
                ) {
                    Box {
                        ButtonsRow(
                            buttons = buttonsList,
                            currentValue = albumType,
                            onValueUpdate = { albumType = it },
                            modifier = Modifier.padding(end = 12.dp)
                        )

                    }
                }

                // Sticky tab's tool bar
                TabToolBar.Buttons( sync, search, randomizer, shuffle, itemSize, viewType )

                // Sticky search bar
                search.SearchBar( this )

                /*
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
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
                                onClick = {
                                    sortOrder =
                                        if (sortOrder == SortOrder.Ascending)
                                            SortOrder.Descending
                                        else SortOrder.Ascending
                                },
                                onLongClick = {
                                    menuState.display {
                                        sortMenu()
                                    }
                                }

                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(sortBy.textId),
                        style = typography().s,
                        color = colorPalette().text,
                        modifier = Modifier.clickable {
                            menuState.display {
                                sortMenu()
                            }
                        }
                    )
                }
                */

                if (getViewType() == ViewType.List) {
                    val state = rememberLazyListState()
                    LazyListContainer(
                        state = state,
                    ) {
                        LazyColumn(
                            state = state,
                            modifier = Modifier
                        ) {
                            items(
                                items = itemsOnDisplay,
                                key = Album::id
                            ) { album ->
                                var songs = remember { listOf<Song>() }
                                Database.asyncQuery {
                                    songs = albumSongsList(album.id)
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

                                var onDismiss: () -> Unit = {}
                                var titleId = 0
                                var defValue = ""
                                var placeholderTextId: Int = 0
                                var queryBlock: (Database, String, String) -> Int = { _, _, _ -> 0 }

                                if (showDialogChangeAlbumCover) {
                                    onDismiss = { showDialogChangeAlbumCover = false }
                                    titleId = R.string.update_cover
                                    defValue = album.thumbnailUrl.toString()
                                    placeholderTextId = R.string.cover
                                    queryBlock = Database::updateAlbumCover
                                } else if (showDialogChangeAlbumTitle) {
                                    onDismiss = { showDialogChangeAlbumTitle = false }
                                    titleId = R.string.update_title
                                    defValue = album.title.toString()
                                    placeholderTextId = R.string.title
                                    queryBlock = Database::updateAlbumTitle
                                } else if (showDialogChangeAlbumAuthors) {
                                    onDismiss = { showDialogChangeAlbumAuthors = false }
                                    titleId = R.string.update_authors
                                    defValue = album.authorsText.toString()
                                    placeholderTextId = R.string.authors
                                    queryBlock = Database::updateAlbumAuthors
                                }

                                if (showDialogChangeAlbumTitle || showDialogChangeAlbumAuthors || showDialogChangeAlbumCover)
                                    InputTextDialog(
                                        onDismiss = onDismiss,
                                        title = stringResource(titleId),
                                        value = defValue,
                                        placeholder = stringResource(placeholderTextId),
                                        setValue = {
                                            if (it.isNotEmpty())
                                                Database.asyncTransaction {
                                                    queryBlock(
                                                        this,
                                                        album.id,
                                                        it
                                                    )
                                                }
                                        },
                                        prefix = MODIFIED_PREFIX
                                    )

                                var position by remember {
                                    mutableIntStateOf(0)
                                }
                                val context = LocalContext.current

                                AlbumItem(
                                    alternative = false,
                                    yearCentered = false,
                                    showAuthors = true,
                                    album = album,
                                    homePage = true,
                                    iconSize = itemSize.size.dp,
                                    thumbnailSizeDp = itemSize.size.dp,
                                    thumbnailSizePx = itemSize.size.px,
                                    modifier = Modifier
                                        .combinedClickable(

                                            onLongClick = {
                                                menuState.display {
                                                    AlbumsItemMenu(
                                                        navController = navController,
                                                        onDismiss = menuState::hide,
                                                        onChangeAlbumTitle = {
                                                            showDialogChangeAlbumTitle = true
                                                        },
                                                        onChangeAlbumAuthors = {
                                                            showDialogChangeAlbumAuthors = true
                                                        },
                                                        onChangeAlbumCover = {
                                                            showDialogChangeAlbumCover = true
                                                        },
                                                        album = album,
                                                        onPlayNext = {
                                                            println("mediaItem ${songs}")
                                                            binder?.player?.addNext(
                                                                songs.map(Song::asMediaItem),
                                                                context,
                                                                selectedQueue ?: defaultQueue()
                                                            )

                                                        },
                                                        onEnqueue = {
                                                            println("mediaItem ${songs}")
                                                            binder?.player?.enqueue(
                                                                songs.map(Song::asMediaItem),
                                                                context
                                                            )

                                                        },
                                                        onAddToPlaylist = { playlistPreview ->
                                                            position =
                                                                playlistPreview.songCount.minus(1)
                                                                    ?: 0
                                                            //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                            if (position > 0) position++ else position =
                                                                0

                                                            if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
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
                                                            } else {
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    addToYtPlaylist(
                                                                        playlistPreview.playlist.id,
                                                                        position,
                                                                        playlistPreview.playlist.browseId
                                                                            ?: "",
                                                                        songs.map { it.asMediaItem })
                                                                }
                                                            }


                                                        },
                                                        onGoToPlaylist = {
                                                            navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                                        },
                                                        disableScrollingText = disableScrollingText,
                                                        onBlacklist = {
                                                            insertOrUpdateBlacklist(album)
                                                        },
                                                    )
                                                }
                                            },
                                            onClick = {
                                                search.onItemSelected()
                                                onAlbumClick(album)
                                            }
                                        )
                                        .clip(thumbnailShape()),
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeAlbum = album.isYoutubeAlbum
                                )
                            }
                        }
                    }
                } else {
                    LazyListContainer(
                        state = lazyGridState,
                    ) {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Adaptive(itemSize.size.dp),
                            //contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                            modifier = Modifier
                                .background(colorPalette().background0)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
                        ) {
                            items(
                                items = itemsOnDisplay,
                                key = Album::id
                            ) { album ->
                                var songs = remember { listOf<Song>() }
                                Database.asyncQuery {
                                    songs = albumSongsList(album.id)
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

                                var onDismiss: () -> Unit = {}
                                var titleId = 0
                                var defValue = ""
                                var placeholderTextId: Int = 0
                                var queryBlock: (Database, String, String) -> Int = { _, _, _ -> 0 }

                                if (showDialogChangeAlbumCover) {
                                    onDismiss = { showDialogChangeAlbumCover = false }
                                    titleId = R.string.update_cover
                                    defValue = album.thumbnailUrl.toString()
                                    placeholderTextId = R.string.cover
                                    queryBlock = Database::updateAlbumCover
                                } else if (showDialogChangeAlbumTitle) {
                                    onDismiss = { showDialogChangeAlbumTitle = false }
                                    titleId = R.string.update_title
                                    defValue = album.title.toString()
                                    placeholderTextId = R.string.title
                                    queryBlock = Database::updateAlbumTitle
                                } else if (showDialogChangeAlbumAuthors) {
                                    onDismiss = { showDialogChangeAlbumAuthors = false }
                                    titleId = R.string.update_authors
                                    defValue = album.authorsText.toString()
                                    placeholderTextId = R.string.authors
                                    queryBlock = Database::updateAlbumAuthors
                                }

                                if (showDialogChangeAlbumTitle || showDialogChangeAlbumAuthors || showDialogChangeAlbumCover)
                                    InputTextDialog(
                                        onDismiss = onDismiss,
                                        title = stringResource(titleId),
                                        value = defValue,
                                        placeholder = stringResource(placeholderTextId),
                                        setValue = {
                                            if (it.isNotEmpty())
                                                Database.asyncTransaction {
                                                    queryBlock(
                                                        this,
                                                        album.id,
                                                        it
                                                    )
                                                }
                                        },
                                        prefix = MODIFIED_PREFIX
                                    )

                                var position by remember {
                                    mutableIntStateOf(0)
                                }
                                val context = LocalContext.current

                                AlbumItem(
                                    alternative = true,
                                    showAuthors = true,
                                    album = album,
                                    homePage = true,
                                    iconSize = itemSize.size.dp,
                                    thumbnailSizeDp = itemSize.size.dp,
                                    thumbnailSizePx = itemSize.size.px,
                                    modifier = Modifier
                                        .combinedClickable(

                                            onLongClick = {
                                                menuState.display {
                                                    AlbumsItemMenu(
                                                        navController = navController,
                                                        onDismiss = menuState::hide,
                                                        album = album,
                                                        onChangeAlbumTitle = {
                                                            showDialogChangeAlbumTitle = true
                                                        },
                                                        onChangeAlbumAuthors = {
                                                            showDialogChangeAlbumAuthors = true
                                                        },
                                                        onChangeAlbumCover = {
                                                            showDialogChangeAlbumCover = true
                                                        },
                                                        onPlayNext = {
                                                            println("mediaItem ${songs}")
                                                            binder?.player?.addNext(
                                                                songs.map(Song::asMediaItem),
                                                                context,
                                                                selectedQueue ?: defaultQueue()
                                                            )

                                                        },
                                                        onEnqueue = {
                                                            println("mediaItem ${songs}")
                                                            binder?.player?.enqueue(
                                                                songs.map(Song::asMediaItem),
                                                                context
                                                            )

                                                        },
                                                        onAddToPlaylist = { playlistPreview ->
                                                            position =
                                                                playlistPreview.songCount.minus(1)
                                                                    ?: 0
                                                            //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                            if (position > 0) position++ else position =
                                                                0

                                                            if (!isYtSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist) {
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
                                                            } else {
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    addToYtPlaylist(
                                                                        playlistPreview.playlist.id,
                                                                        position,
                                                                        playlistPreview.playlist.browseId
                                                                            ?: "",
                                                                        songs.map { it.asMediaItem })
                                                                }
                                                            }


                                                        },
                                                        onGoToPlaylist = {
                                                            navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                                        },
                                                        disableScrollingText = disableScrollingText,
                                                        onBlacklist = {
                                                            insertOrUpdateBlacklist(album)
                                                        },
                                                    )
                                                }
                                            },
                                            onClick = {
                                                search.onItemSelected()
                                                onAlbumClick(album)
                                            }
                                        )
                                        .clip(thumbnailShape()),
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeAlbum = album.isYoutubeAlbum
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop( lazyGridState )

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }
    }
}
