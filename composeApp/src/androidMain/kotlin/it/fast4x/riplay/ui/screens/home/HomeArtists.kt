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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
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
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.BuiltInPlaylist
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.enums.SortOrder
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
import it.fast4x.riplay.extensions.preferences.songSortByKey
import it.fast4x.riplay.extensions.preferences.songSortOrderKey
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.tab.ToolbarMenuButton
import it.fast4x.riplay.ui.components.themed.ArtistsItemMenu
import it.fast4x.riplay.ui.components.themed.EnumsMenu
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.importYTMSubscribedChannels
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.typography
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

    val lazyGridState = rememberLazyGridState()
    val menuState = LocalGlobalSheetState.current
    var items by persistList<Artist>( "")

    var itemsOnDisplay by persistList<Artist>( "home/artists/on_display" )

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val search = Search.init()

    val itemSize = ItemSize.init( HOME_ARTIST_ITEM_SIZE )

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

    val coroutineScope = rememberCoroutineScope()

    var sortBy by rememberPreference(artistSortByKey, ArtistSortBy.DateAdded)
    var sortOrder by rememberPreference(artistSortOrderKey, SortOrder.Descending)
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    LaunchedEffect( Unit, sortBy, sortOrder, artistType ) {
        when( artistType ) {
            ArtistsType.Favorites -> Database.artists( sortBy, sortOrder ).collect { items = it }
            ArtistsType.Library -> Database.artistsInLibrary( sortBy, sortOrder ).collect { items = it.filter { it.isYoutubeArtist } }
            ArtistsType.OnDevice -> Database.artistsOnDevice( sortBy, sortOrder ).collect { items = it }
            ArtistsType.All -> Database.artistsWithSongsSaved( sortBy, sortOrder ).collect { items = it }
        }
    }


    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Artist.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    LaunchedEffect( items, search.input ) {
        val scrollIndex = lazyGridState.firstVisibleItemIndex
        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items
            .filter {
                it.name?.contains( search.input, true ) ?: false
            }
            .filter {item -> blacklisted.value?.map { it.path }?.contains(item.id) == false }

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

    val sortMenu: @Composable () -> Unit = {
        EnumsMenu(
            title = stringResource(R.string.sorting_order),
            onDismiss = menuState::hide,
            selectedValue = sortBy.menuItem,
            onValueSelected = { sortBy = ArtistSortBy.entries[it.ordinal] },
            values = ArtistSortBy.entries.map { it.menuItem },
            valueText = { stringResource(it.titleId) }
        )
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
                            currentValue = artistType,
                            onValueUpdate = { artistType = it },
                            modifier = Modifier.padding(end = 12.dp)
                        )

                    }
                }

                // Sticky tab's tool bar
                TabToolBar.Buttons(  sync, search, randomizer, shuffle, itemSize, viewType )

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
