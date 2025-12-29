package it.fast4x.riplay.ui.screens.home


import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.environment.EnvironmentExt
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.ArtistsType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.artistSortByKey
import it.fast4x.riplay.extensions.preferences.artistSortOrderKey
import it.fast4x.riplay.extensions.preferences.artistTypeKey
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.utils.getViewType
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar
import it.fast4x.riplay.ui.components.tab.ItemSize
import it.fast4x.riplay.ui.components.tab.Sort
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.tab.toolbar.Randomizer
import it.fast4x.riplay.ui.components.tab.toolbar.SongsShuffle
import it.fast4x.riplay.extensions.preferences.Preference.HOME_ARTIST_ITEM_SIZE
import it.fast4x.riplay.utils.autoSyncToolbutton
import it.fast4x.riplay.extensions.preferences.autosyncKey
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.tab.ToolbarMenuButton
import it.fast4x.riplay.ui.components.themed.ArtistsItemMenu
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.importYTMSubscribedChannels
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.viewTypeToolbutton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalMaterial3Api
@UnstableApi
@ExperimentalMaterialApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun HomeArtists(
    navController: NavController,
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val lazyGridState = rememberLazyGridState()
    val menuState = LocalGlobalSheetState.current
    var items by persistList<Artist>( "")
    //var itemsToFilter by persistList<Artist>( "home/artists" )

    var itemsOnDisplay by persistList<Artist>( "home/artists/on_display" )

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val search = Search.init()

    val sort = Sort.init(
        artistSortOrderKey,
        ArtistSortBy.entries,
        rememberPreference(artistSortByKey, ArtistSortBy.DateAdded)
    )

    val itemSize = ItemSize.init( HOME_ARTIST_ITEM_SIZE )

    val blacklistButton = ToolbarMenuButton.init(
        iconId = R.drawable.alert_circle,
        titleId = R.string.blacklisted_folders,
        onClick = {
            menuState.hide()
            navController.navigate(NavRoutes.blacklist.name)
        }
    )

    val randomizer = object: Randomizer<Artist> {
        override fun getItems(): List<Artist> = itemsOnDisplay
        override fun onClick(index: Int) = onArtistClick(itemsOnDisplay[index])

    }
    var artistType by rememberPreference(artistTypeKey, ArtistsType.Favorites )

    val shuffle = SongsShuffle.init {
        when( artistType ) {
            ArtistsType.Favorites -> {
                Database.songsInAllFollowedArtistsFiltered(itemsOnDisplay.map { it.id }).map{ it.map( Song::asMediaItem ) }
            }
            ArtistsType.Library -> {
                Database.songsInLibraryArtistsFiltered(itemsOnDisplay.map { it.id }).map{ it.map( Song::asMediaItem ) }
            }
            ArtistsType.OnDevice -> {
                Database.songsOnDeviceArtistsFiltered(itemsOnDisplay.map { it.id }).map{ it.map( Song::asMediaItem ) }
            }
            ArtistsType.All -> {
                Database.songsByArtistAsc().map { it.map { song -> song.asMediaItem } }
            }
        }
    }

    val buttonsList = ArtistsType.entries.map { it to it.textName }

    //var filterBy by rememberPreference(filterByKey, FilterBy.All)
    //val (colorPalette, typography) = LocalAppearance.current
    //val menuState = LocalGlobalSheetState.current
    val coroutineScope = rememberCoroutineScope()

//    if (!isSyncEnabled()) {
//        filterBy = FilterBy.All
//    }

    LaunchedEffect( Unit, sort.sortBy, sort.sortOrder, artistType ) {
        when( artistType ) {
            ArtistsType.Favorites -> Database.artists( sort.sortBy, sort.sortOrder ).collect { items = it }
            ArtistsType.Library -> Database.artistsInLibrary( sort.sortBy, sort.sortOrder ).collect { items = it.filter { it.isYoutubeArtist } }
            ArtistsType.OnDevice -> Database.artistsOnDevice( sort.sortBy, sort.sortOrder ).collect { items = it }
            ArtistsType.All -> Database.artistsWithSongsSaved( sort.sortBy, sort.sortOrder ).collect { items = it }
        }
    }

//    LaunchedEffect( Unit, itemsToFilter, filterBy ) {
//        items = when(filterBy) {
//            FilterBy.All -> itemsToFilter
//            FilterBy.YoutubeLibrary -> itemsToFilter.filter { it.isYoutubeArtist }
//            FilterBy.Local -> itemsToFilter.filterNot { it.isYoutubeArtist }
//        }
//
//    }
    LaunchedEffect( items, search.input ) {
        val scrollIndex = lazyGridState.firstVisibleItemIndex
        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items.filter {
            it.name?.contains( search.input, true ) ?: false
        }

        lazyGridState.scrollToItem( scrollIndex, scrollOffset )
    }
    if (items.any{it.thumbnailUrl == null}) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                items.filter { it.thumbnailUrl == null }.forEach { artist ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val artistThumbnail = EnvironmentExt.getArtistPage(artist.id).getOrNull()?.artist?.thumbnail?.url
                        Database.asyncTransaction {
                            update(artist.copy(thumbnailUrl = artistThumbnail))
                        }
                    }
                }
            }
        }
    }

    val sync = autoSyncToolbutton(R.string.autosync_channels)

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
        if (!justSynced && importYTMSubscribedChannels())
                justSynced = true
    }

    PullToRefreshBox(
        refreshing = refreshing,
        onRefresh = { refresh() }
    ) {
        Box (
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
                TabHeader( R.string.artists ) {
                    HeaderInfo(itemsOnDisplay.size.toString(), R.drawable.music_artist)
                }

                // Sticky tab's tool bar
                TabToolBar.Buttons( sort, sync, search, randomizer, shuffle, itemSize, viewType, blacklistButton )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        //.padding(vertical = 4.dp)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                ) {
                    Box {
                        ButtonsRow(
                            buttons = buttonsList,
                            currentValue = artistType,
                            onValueUpdate = { artistType = it },
                            modifier = Modifier.padding(end = 12.dp)
                        )
//                        if (isSyncEnabled()) {
//                            Row(
//                                modifier = Modifier
//                                    .align(Alignment.CenterEnd)
//                            ) {
//                                BasicText(
//                                    text = when (filterBy) {
//                                        FilterBy.All -> stringResource(R.string.all)
//                                        FilterBy.Local -> stringResource(R.string.on_device)
//                                        FilterBy.YoutubeLibrary -> stringResource(R.string.ytm_library)
//                                    },
//                                    style = typography.xs.semiBold,
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis,
//                                    modifier = Modifier
//                                        .align(Alignment.CenterVertically)
//                                        .padding(end = 5.dp)
//                                        .clickable {
//                                            menuState.display {
//                                                FilterMenu(
//                                                    title = stringResource(R.string.filter_by),
//                                                    onDismiss = menuState::hide,
//                                                    onAll = { filterBy = FilterBy.All },
//                                                    onYoutubeLibrary = {
//                                                        filterBy = FilterBy.YoutubeLibrary
//                                                    },
//                                                    onLocal = { filterBy = FilterBy.Local }
//                                                )
//                                            }
//
//                                        }
//                                )
//                                HeaderIconButton(
//                                    icon = R.drawable.playlist,
//                                    color = colorPalette.text,
//                                    onClick = {},
//                                    modifier = Modifier
//                                        .offset(0.dp, 2.5.dp)
//                                        .clickable(
//                                            interactionSource = remember { MutableInteractionSource() },
//                                            indication = null,
//                                            onClick = {}
//                                        )
//                                )
//                            }
//                        }
                    }
                }

                // Sticky search bar
                search.SearchBar( this )

                if (getViewType() == ViewType.List) {
                    val state = rememberLazyListState()
                    LazyListContainer(
                        state = state,
                    ) {
                        LazyColumn(
                            state = state,
                            modifier = Modifier
                        ) {
                            items(items = itemsOnDisplay, key = Artist::id) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizeDp = itemSize.size.dp,
                                    thumbnailSizePx = itemSize.size.px,
                                    homePage = true,
                                    iconSize = itemSize.size.dp,
                                    alternative = false,
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                search.onItemSelected()
                                                onArtistClick(artist)
                                            },
                                            onLongClick = {
                                                menuState.display {
                                                    ArtistsItemMenu(
                                                        artist = artist,
                                                        onDismiss = menuState::hide,
                                                        onBlacklist = {
                                                            menuState.hide()
                                                            insertOrUpdateBlacklist(artist)
                                                        },
                                                        disableScrollingText = disableScrollingText

                                                    )
                                                }
                                            }

                                        ),
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeArtist = artist.isYoutubeArtist
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
                            modifier = Modifier
                                .background(colorPalette().background0)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
                        ) {
                            items(items = itemsOnDisplay, key = Artist::id) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizeDp = itemSize.size.dp,
                                    thumbnailSizePx = itemSize.size.px,
                                    homePage = true,
                                    iconSize = itemSize.size.dp,
                                    alternative = true,
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                search.onItemSelected()
                                                onArtistClick(artist)
                                            },
                                            onLongClick = {
                                                menuState.display {
                                                    ArtistsItemMenu(
                                                        artist = artist,
                                                        onDismiss = menuState::hide,
                                                        onBlacklist = {
                                                            menuState.hide()
                                                            insertOrUpdateBlacklist(artist)
                                                        },
                                                        disableScrollingText = disableScrollingText

                                                    )
                                                }
                                            }

                                        ),
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeArtist = artist.isYoutubeArtist
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if( UiType.ViMusic.isCurrent() && showFloatingIcon )
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }
    }
}
