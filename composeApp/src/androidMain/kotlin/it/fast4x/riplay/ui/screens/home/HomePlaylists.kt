package it.fast4x.riplay.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.environment.EnvironmentExt
import it.fast4x.riplay.LocalOnDeviceViewModel
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.YTP_PREFIX
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.PlaylistType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.enableCreateMonthlyPlaylistsKey
import it.fast4x.riplay.extensions.preferences.playlistSortByKey
import it.fast4x.riplay.extensions.preferences.playlistSortOrderKey
import it.fast4x.riplay.extensions.preferences.playlistTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import it.fast4x.riplay.extensions.preferences.showMonthlyPlaylistsKey
import it.fast4x.riplay.extensions.preferences.showPinnedPlaylistsKey
import it.fast4x.riplay.extensions.preferences.showPipedPlaylistsKey
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.utils.getViewType
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.IDialog
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar
import it.fast4x.riplay.ui.components.tab.ImportSongsFromCSV
import it.fast4x.riplay.ui.components.tab.ItemSize
import it.fast4x.riplay.ui.components.tab.Sort
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon
import it.fast4x.riplay.ui.components.tab.toolbar.SongsShuffle
import it.fast4x.riplay.ui.screens.settings.isSyncEnabled
import it.fast4x.riplay.utils.importYTMPrivatePlaylists
import it.fast4x.riplay.extensions.preferences.Preference.HOME_LIBRARY_ITEM_SIZE
import it.fast4x.riplay.utils.autoSyncToolbutton
import it.fast4x.riplay.extensions.preferences.autosyncKey
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.ondevice.blackListedPathsFilename
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.tab.ToolbarMenuButton
import it.fast4x.riplay.ui.components.themed.PlaylistsItemMenu
import it.fast4x.riplay.ui.components.themed.StringListDialog
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.CheckAndCreateMonthlyPlaylist
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.viewTypeToolbutton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.map


@OptIn(ExperimentalTextApi::class)
@ExperimentalMaterial3Api
@UnstableApi
@ExperimentalMaterialApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomePlaylists(
    onPlaylistClick: (PlaylistPreview) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    navController: NavController
) {
    // Essentials
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState()
    val menuState = LocalGlobalSheetState.current
    //val navController = rememberNavController()
    val binder = LocalPlayerServiceBinder.current
    val selectedQueue = LocalSelectedQueue.current

    // Non-vital
    var plistId by remember { mutableLongStateOf( 0L ) }
    var playlistType by rememberPreference(playlistTypeKey, PlaylistType.Playlist)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var items by persistList<PlaylistPreview>("home/playlists")

    var itemsOnDisplay by persistList<PlaylistPreview>("home/playlists/on_display")

    val playlistThumbnailSizeDp = Dimensions.thumbnails.playlist
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    // Dialog states
    val newPlaylistToggleState = remember { mutableStateOf( false ) }

    val search = Search.init()

    val sort = Sort.init(
        playlistSortOrderKey,
        PlaylistSortBy.entries,
        rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
    )

    val itemSize = ItemSize.init( HOME_LIBRARY_ITEM_SIZE )

    val shuffle = SongsShuffle.init {
        when( playlistType ) {
            PlaylistType.Playlist -> Database.songsInAllPlaylists()
            PlaylistType.PinnedPlaylist -> Database.songsInAllPinnedPlaylists()
            PlaylistType.MonthlyPlaylist -> Database.songsInAllMonthlyPlaylists()
            PlaylistType.PodcastPlaylist -> Database.songsInAllPodcastPlaylists()
            PlaylistType.YTPlaylist -> Database.songsInAllYTPrivatePlaylists()
            PlaylistType.OnDevicePlaylist -> Database.songsOnDevice()
        }.map { it.map( Song::asMediaItem ) }
    }

    val newPlaylistDialog = object: IDialog, Descriptive, MenuIcon {

        override val messageId: Int = R.string.create_new_playlist
        override val iconId: Int = R.drawable.add_in_playlist
        override val dialogTitle: String
            @Composable
            get() = stringResource( R.string.enter_the_playlist_name )
        override val menuIconTitle: String
            @Composable
            get() = stringResource( messageId )

        override var isActive: Boolean = newPlaylistToggleState.value
            set(value) {
                newPlaylistToggleState.value = value
                field = value
            }

        override var value: String = ""

        override fun onShortClick() = super.onShortClick()

        override fun onSet(newValue: String) {
            if (isSyncEnabled()) {
                CoroutineScope(Dispatchers.IO).launch {
                    EnvironmentExt.createPlaylist(newValue).getOrNull()
                        .also {
                            println("Innertube YtMusic createPlaylist: $it")
                            Database.asyncTransaction {
                                insert(Playlist(name = newValue, browseId = it, isYoutubePlaylist = true, isEditable = true))
                            }
                        }

                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    Database.asyncTransaction {
                        insert(Playlist(name = newValue))
                    }
                }
            }

            onDismiss()
        }

    }
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss")
    var time by remember {mutableStateOf("")}
    val formattedDate = currentDateTime.format(formatter)

    val importPlaylistDialog = ImportSongsFromCSV.init(
        beforeTransaction = { _, row ->
            time = formattedDate
            val playlistName = row["PlaylistName"] ?: "New Playlist $time"
            plistId = playlistName.let {
                Database.playlistExistByName( it )
            }

            if (plistId == 0L)
                plistId = playlistName.let {
                    Database.insert( Playlist( plistId, it, row["PlaylistBrowseId"] ) )
                }
        },
        afterTransaction = { index, song, album, artists ->
            if (song.id.isBlank()) return@init

            Database.insert(song)
            Database.insert(
                SongPlaylistMap(
                    songId = song.id,
                    playlistId = plistId,
                    position = index
                ).default()
            )

            if(album.id !=""){
                Database.insert(
                    album,
                    SongAlbumMap(
                        songId = song.id,
                        albumId = album.id,
                        position = null
                    )
                )
            }
            if(artists.isNotEmpty()){
                Database.insert(
                    artists,
                    artists.map{ artist->
                        SongArtistMap(
                            songId = song.id,
                            artistId = artist.id
                        )
                    }
                )
            }
        }
    )
    val sync = autoSyncToolbutton(R.string.autosync)

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

    // START: Import YTM private playlists
    LaunchedEffect(justSynced, doAutoSync) {
        if ((!justSynced) && importYTMPrivatePlaylists())
            justSynced = true
    }

    // get playlists list

    val onDeviceViewModel = LocalOnDeviceViewModel.current

    LaunchedEffect( sort.sortBy, sort.sortOrder, playlistType ) {
        if (playlistType == PlaylistType.OnDevicePlaylist) {
            onDeviceViewModel.audioFoldersAsPlaylists().collect { folders ->
                items = when (sort.sortBy) {
                    PlaylistSortBy.Name -> when (sort.sortOrder) {
                        SortOrder.Ascending -> folders.sortedBy { it.playlist.name }
                        SortOrder.Descending -> folders.sortedByDescending { it.playlist.name }
                    }
                    PlaylistSortBy.DateAdded -> when (sort.sortOrder) {
                        SortOrder.Ascending -> folders.sortedBy { it.totalPlayTimeMs }
                        SortOrder.Descending -> folders.sortedByDescending { it.totalPlayTimeMs }
                    }
                    PlaylistSortBy.SongCount -> when (sort.sortOrder) {
                        SortOrder.Ascending -> folders.sortedBy { it.songCount }
                        SortOrder.Descending -> folders.sortedByDescending { it.songCount }
                    }
                    PlaylistSortBy.MostPlayed -> when (sort.sortOrder) {
                        SortOrder.Ascending -> folders.sortedBy { it.totalPlayTimeMs }
                        SortOrder.Descending -> folders.sortedByDescending { it.totalPlayTimeMs }
                    }
                }
            }
        } else
            Database.playlistPreviews(sort.sortBy, sort.sortOrder).collect { items = it }
    }

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Playlist.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    LaunchedEffect( items, search.input ) {
        val scrollIndex = lazyGridState.firstVisibleItemIndex
        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items
            .filter {
                it.playlist.name.contains( search.input, true )
            }
            .filter {item -> blacklisted.value?.map { it.path }?.contains(item.playlist.id.toString()) == false }

        lazyGridState.scrollToItem( scrollIndex, scrollOffset )
    }

    // START: Additional playlists
    val showPinnedPlaylists by rememberPreference(showPinnedPlaylistsKey, true)
    val showMonthlyPlaylists by rememberPreference(showMonthlyPlaylistsKey, true)
    val showPipedPlaylists by rememberPreference(showPipedPlaylistsKey, true)

    val buttonsList = mutableListOf(PlaylistType.Playlist to stringResource(R.string.playlists))
    buttonsList += PlaylistType.YTPlaylist to stringResource(R.string.library)
    //if (showPipedPlaylists)
        buttonsList +=
        PlaylistType.PodcastPlaylist to stringResource(R.string.podcasts)
    if (showPinnedPlaylists) buttonsList +=
        PlaylistType.PinnedPlaylist to stringResource(R.string.pinned_playlists)
    if (showMonthlyPlaylists) buttonsList +=
        PlaylistType.MonthlyPlaylist to stringResource(R.string.monthly_playlists)

    buttonsList += PlaylistType.OnDevicePlaylist to stringResource(R.string.on_device)


    // END - Additional playlists


    // START - New playlist
    newPlaylistDialog.Render()
    // END - New playlist

    // START - Monthly playlist
    val enableCreateMonthlyPlaylists by rememberPreference(enableCreateMonthlyPlaylistsKey, true)
    if (enableCreateMonthlyPlaylists)
        CheckAndCreateMonthlyPlaylist()
    // END - Monthly playlist

    PullToRefreshBox(
        refreshing = refreshing,
        onRefresh = { refresh() }
    ) {
        Box(
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
            Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
                TabHeader( R.string.playlists ) {
                    HeaderInfo( itemsOnDisplay.size.toString(), R.drawable.playlist )
                }

                // Sticky tab's tool bar
                val buttons = mutableListOf(
                    sort, sync, search, shuffle, newPlaylistDialog, importPlaylistDialog, itemSize, viewType)
                TabToolBar.Buttons(buttons)

                // Sticky search bar
                search.SearchBar( this )

                if (getViewType() == ViewType.List) {
                    val state = rememberLazyListState()
                    LazyListContainer(
                        state = state
                    ) {
                        LazyColumn(
                            state = state,
                            modifier = Modifier

                        ) {
                            item(
                                key = "separator",
                                contentType = 0
                            ) {
                                ButtonsRow(
                                    buttons = buttonsList,
                                    currentValue = playlistType,
                                    onValueUpdate = { playlistType = it },
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                                )
                            }

                            val listPrefix =
                                when (playlistType) {
                                    PlaylistType.Playlist, PlaylistType.OnDevicePlaylist -> ""    // Matches everything
                                    PlaylistType.PinnedPlaylist -> PINNED_PREFIX
                                    PlaylistType.MonthlyPlaylist -> MONTHLY_PREFIX
                                    PlaylistType.PodcastPlaylist -> ""
                                    PlaylistType.YTPlaylist -> YTP_PREFIX
                                }
                            val condition: (PlaylistPreview) -> Boolean = {
                                when (playlistType) {
                                    PlaylistType.YTPlaylist -> it.playlist.isYoutubePlaylist
                                    PlaylistType.PodcastPlaylist -> it.playlist.isPodcast
                                    PlaylistType.OnDevicePlaylist -> it.isOnDevice
                                    else -> it.playlist.name.startsWith(listPrefix, true)
                                }
                            }
                            items(
                                items = itemsOnDisplay.filter(condition),
                                key = { it.playlist.id }
                            ) { preview ->
                                if (!preview.isOnDevice)
                                    PlaylistItem(
                                        playlist = preview,
                                        thumbnailSizeDp = itemSize.size.dp,
                                        thumbnailSizePx = itemSize.size.px,
                                        homepage = true,
                                        iconSize = itemSize.size.dp,
                                        alternative = false,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            .combinedClickable(
                                                onClick = {
                                                    search.onItemSelected()
                                                    onPlaylistClick(preview)
                                                },
                                                onLongClick = {
                                                    menuState.display {
                                                        PlaylistsItemMenu(
                                                            navController = navController,
                                                            onDismiss = menuState::hide,
                                                            playlist = preview,
                                                            disableScrollingText = disableScrollingText,
                                                            onPlayNext = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                binder?.player?.addNext(
                                                                                    it ?: emptyList(),
                                                                                    appContext(),
                                                                                    selectedQueue
                                                                                        ?: defaultQueue()
                                                                                )
                                                                            }
                                                                        }
                                                                        .collect()
                                                                }

                                                            },
                                                            onBlacklist = {
                                                                insertOrUpdateBlacklist(PlaylistType.Playlist, preview)
                                                            },
                                                        )
                                                    }
                                                }

                                            ),
                                        disableScrollingText = disableScrollingText,
                                        isYoutubePlaylist = preview.playlist.isYoutubePlaylist,
                                        isEditable = preview.playlist.isEditable
                                    )
                                else {
                                    var songs by persistList<SongEntity>("playlist${preview.playlist.id}/songsThumbnails")
                                    LaunchedEffect(Unit) {
                                        onDeviceViewModel.audioFilesFromFolder(preview.folder ?: "").collect {
                                            songs = it
                                        }
                                    }
                                    val thumbnails = songs.map { song -> song.song }
                                        .takeWhile { it.thumbnailUrl?.isNotEmpty() ?: false }
                                        .take(4)
                                        .map { it.thumbnailUrl.thumbnail(playlistThumbnailSizePx / 2) }

                                    PlaylistItem(
                                        thumbnailContent = {
                                            if (thumbnails.toSet().size == 1) {
                                                AsyncImage(
                                                    model = thumbnails.first()
                                                        .thumbnail(playlistThumbnailSizePx),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                ) {
                                                    listOf(
                                                        Alignment.TopStart,
                                                        Alignment.TopEnd,
                                                        Alignment.BottomStart,
                                                        Alignment.BottomEnd
                                                    ).forEachIndexed { index, alignment ->
                                                        AsyncImage(
                                                            model = thumbnails.getOrNull(index),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .align(alignment)
                                                                .size(playlistThumbnailSizeDp / 2)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        songCount = preview.songCount,
                                        thumbnailSizeDp = playlistThumbnailSizeDp,
                                        name = preview.playlist.name,
                                        channelName = null,
                                        alternative = false,
                                        showName = true,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            .combinedClickable(
                                                onClick = {
                                                    search.onItemSelected()
                                                    onPlaylistClick(preview)
                                                },
                                                onLongClick = {
                                                    menuState.display {
                                                        PlaylistsItemMenu(
                                                            navController = navController,
                                                            onDismiss = menuState::hide,
                                                            playlist = preview,
                                                            disableScrollingText = disableScrollingText,
                                                            onPlayNext = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                binder?.player?.addNext(
                                                                                    it ?: emptyList(),
                                                                                    appContext(),
                                                                                    selectedQueue
                                                                                        ?: defaultQueue()
                                                                                )
                                                                            }
                                                                        }
                                                                        .collect()
                                                                }

                                                            },
                                                            onPlayNow = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            if (it != null)
                                                                                binder?.player?.forcePlayFromBeginning(
                                                                                    it
                                                                                )
                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onShufflePlay = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                if (it != null)
                                                                                    binder?.player?.forcePlayFromBeginning(
                                                                                        it.shuffled()
                                                                                    )
                                                                            }

                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onBlacklist = {
                                                                if (preview.folder == null) return@PlaylistsItemMenu
                                                                insertOrUpdateBlacklist(PlaylistType.OnDevicePlaylist, preview)
                                                            },
                                                        )
                                                    }
                                                }

                                            ),
                                        disableScrollingText = disableScrollingText,

                                        )
                                }
                            }

                            item(
                                key = "footer",
                                contentType = 0
                            ) {
                                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                            }

                        }
                    }
                } else {
                    LazyListContainer(
                        state = lazyGridState
                    ) {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Adaptive(itemSize.size.dp),
                            modifier = Modifier
                                .background(colorPalette().background0)
                        ) {
                            item(
                                key = "separator",
                                contentType = 0,
                                span = { GridItemSpan(maxLineSpan) }) {
                                ButtonsRow(
                                    buttons = buttonsList,
                                    currentValue = playlistType,
                                    onValueUpdate = { playlistType = it },
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                                )
                            }

                            val listPrefix =
                                when (playlistType) {
                                    PlaylistType.Playlist, PlaylistType.OnDevicePlaylist -> ""    // Matches everything
                                    PlaylistType.PinnedPlaylist -> PINNED_PREFIX
                                    PlaylistType.MonthlyPlaylist -> MONTHLY_PREFIX
                                    PlaylistType.PodcastPlaylist -> ""
                                    PlaylistType.YTPlaylist -> YTP_PREFIX
                                }
                            val condition: (PlaylistPreview) -> Boolean = {
                                when (playlistType) {
                                    PlaylistType.YTPlaylist -> it.playlist.isYoutubePlaylist
                                    PlaylistType.PodcastPlaylist -> it.playlist.isPodcast
                                    PlaylistType.OnDevicePlaylist -> it.isOnDevice
                                    PlaylistType.Playlist -> {
                                        when {
                                            !showPinnedPlaylists -> !it.playlist.isPinned
                                            !showMonthlyPlaylists -> !it.playlist.isMonthly
                                            else -> true
                                        }
                                    }
                                    else -> it.playlist.name.startsWith(listPrefix, true)
                                }

                            }
                            items(
                                items = itemsOnDisplay.filter(condition),
                                key = { it.playlist.id }
                            ) { preview ->
                                if (!preview.isOnDevice)
                                    PlaylistItem(
                                        playlist = preview,
                                        thumbnailSizeDp = itemSize.size.dp,
                                        thumbnailSizePx = itemSize.size.px,
                                        alternative = true,
                                        homepage = true,
                                        iconSize = itemSize.size.dp,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            .combinedClickable(
                                                onClick = {
                                                    search.onItemSelected()
                                                    onPlaylistClick(preview)
                                                },
                                                onLongClick = {
                                                    menuState.display {
                                                        PlaylistsItemMenu(
                                                            navController = navController,
                                                            onDismiss = menuState::hide,
                                                            playlist = preview,
                                                            disableScrollingText = disableScrollingText,
                                                            onPlayNext = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                binder?.player?.addNext(
                                                                                    it
                                                                                        ?: emptyList(),
                                                                                    appContext(),
                                                                                    selectedQueue
                                                                                        ?: defaultQueue()
                                                                                )
                                                                            }
                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onPlayNow = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            if (it != null)
                                                                                binder?.player?.forcePlayFromBeginning(
                                                                                    it
                                                                                )
                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onShufflePlay = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                if (it != null)
                                                                                    binder?.player?.forcePlayFromBeginning(
                                                                                        it.shuffled()
                                                                                    )
                                                                            }

                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onBlacklist = {
                                                                insertOrUpdateBlacklist(PlaylistType.Playlist, preview)
                                                            },
                                                        )
                                                    }
                                                }
                                            ),
                                        disableScrollingText = disableScrollingText,
                                        isYoutubePlaylist = preview.playlist.isYoutubePlaylist,
                                        isEditable = preview.playlist.isEditable,
                                    )
                                else {
                                    var songs by persistList<SongEntity>("playlist${preview.playlist.id}/songsThumbnails")
                                    LaunchedEffect(Unit) {
                                        onDeviceViewModel.audioFilesFromFolder(preview.folder ?: "").collect {
                                            songs = it
                                        }
                                    }
                                    val thumbnails = songs.map { song -> song.song }
                                        .takeWhile { it.thumbnailUrl?.isNotEmpty() ?: false }
                                        .take(4)
                                        .map { it.thumbnailUrl.thumbnail(playlistThumbnailSizePx / 2) }

                                    PlaylistItem(
                                        thumbnailContent = {
                                            if (thumbnails.toSet().size == 1) {
                                                AsyncImage(
                                                    model = thumbnails.first()
                                                        .thumbnail(playlistThumbnailSizePx),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                ) {
                                                    listOf(
                                                        Alignment.TopStart,
                                                        Alignment.TopEnd,
                                                        Alignment.BottomStart,
                                                        Alignment.BottomEnd
                                                    ).forEachIndexed { index, alignment ->
                                                        AsyncImage(
                                                            model = thumbnails.getOrNull(index),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .align(alignment)
                                                                .size(playlistThumbnailSizeDp / 2)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        songCount = preview.songCount,
                                        thumbnailSizeDp = playlistThumbnailSizeDp,
                                        name = preview.playlist.name,
                                        channelName = null,
                                        alternative = true,
                                        showName = true,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            .combinedClickable(
                                                onClick = {
                                                    search.onItemSelected()
                                                    onPlaylistClick(preview)
                                                },
                                                onLongClick = {
                                                    menuState.display {
                                                        PlaylistsItemMenu(
                                                            navController = navController,
                                                            onDismiss = menuState::hide,
                                                            playlist = preview,
                                                            disableScrollingText = disableScrollingText,
                                                            onPlayNext = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                binder?.player?.addNext(
                                                                                    it
                                                                                        ?: emptyList(),
                                                                                    appContext(),
                                                                                    selectedQueue
                                                                                        ?: defaultQueue()
                                                                                )
                                                                            }
                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onPlayNow = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            if (it != null)
                                                                                binder?.player?.forcePlayFromBeginning(
                                                                                    it
                                                                                )
                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onShufflePlay = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    Database.playlistSongs(preview.playlist.id)
                                                                        .distinctUntilChanged()
                                                                        .map { it?.map(Song::asMediaItem) }
                                                                        .onEach {
                                                                            withContext(Dispatchers.Main) {
                                                                                if (it != null)
                                                                                    binder?.player?.forcePlayFromBeginning(
                                                                                        it.shuffled()
                                                                                    )
                                                                            }

                                                                        }
                                                                        .collect()
                                                                }
                                                            },
                                                            onBlacklist = {
                                                                if (preview.folder == null) return@PlaylistsItemMenu
                                                                insertOrUpdateBlacklist(PlaylistType.OnDevicePlaylist, preview)
                                                            },
                                                        )
                                                    }
                                                }
                                            ),
                                        disableScrollingText = disableScrollingText,

                                        )
                                }
                            }

                            item(
                                key = "footer",
                                contentType = 0,
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                            }

                        }
                    }
                }

            }

            FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }
    }
}