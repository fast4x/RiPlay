package it.fast4x.riplay.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import it.fast4x.riplay.enums.AlbumsType
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ARTIST_SORT_BY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ARTIST_SORT_ORDER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ARTIST_TYPE
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.extensions.appviewmodel.rememberIsNetworkConnected
import it.fast4x.riplay.utils.getViewType
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar
import it.fast4x.riplay.ui.components.tab.ItemSize
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.tab.toolbar.Randomizer
import it.fast4x.riplay.ui.components.tab.toolbar.SongsShuffle
import it.fast4x.riplay.extensions.preferences.Preference.HOME_ARTIST_ITEM_SIZE
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.ArtistsItemMenu
import it.fast4x.riplay.ui.components.themed.EnumsMenu
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.importYTMSubscribedChannels
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.viewTypeToolbutton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
@ExperimentalMaterial3Api
@UnstableApi
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
    val lazyListState = rememberLazyListState() // Stato per la vista lista
    val menuState = LocalGlobalSheetState.current

    var items by persistList<Artist>("")
    var itemsOnDisplay by persistList<Artist>("home/artists/on_display")

    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    val search = Search.init()
    val itemSize = ItemSize.init(HOME_ARTIST_ITEM_SIZE)

    // Configurazione Randomizer
    val randomizer = object : Randomizer<Artist> {
        override fun getItems(): List<Artist> = itemsOnDisplay
        override fun onClick(index: Int) = onArtistClick(itemsOnDisplay[index])
    }

    var artistType by rememberPreference(ARTIST_TYPE.key, ArtistsType.Favorites)

    // Configurazione Shuffle
    val shuffle = SongsShuffle.init {
        when (artistType) {
            ArtistsType.Favorites -> Database.songsInAllFollowedArtistsFiltered(itemsOnDisplay.map { it.id }).map { it.map(Song::asMediaItem) }
            ArtistsType.Library -> Database.songsInLibraryArtistsFiltered(itemsOnDisplay.map { it.id }).map { it.map(Song::asMediaItem) }
            ArtistsType.OnDevice -> Database.songsOnDeviceArtistsFiltered(itemsOnDisplay.map { it.id }).map { it.map(Song::asMediaItem) }
            ArtistsType.All -> Database.songsByArtistAsc().map { it.map { song -> song.asMediaItem } }
        }
    }

    val isNetworkConnected = rememberIsNetworkConnected()

    val buttonsList = ArtistsType.entries.map { it to it.textName }.filter {
        if (isNetworkConnected) true else it.first.availableWhenOffline
    }

    val coroutineScope = rememberCoroutineScope()

    // Gestione Ordinamento
    var sortBy by rememberPreference(ARTIST_SORT_BY.key, ArtistSortBy.DateAdded)
    var sortOrder by rememberPreference(ARTIST_SORT_ORDER.key, SortOrder.Descending)
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    LaunchedEffect(isNetworkConnected, sortBy, sortOrder, artistType) {
        // 1. Calcolo del tipo effettivo in base alla rete
        val targetArtistType = if (!isNetworkConnected && artistType !in listOf(ArtistsType.OnDevice)) {
            ArtistsType.OnDevice
        } else {
            artistType
        }

        // 2. Aggiorno lo stato esterno (se necessario) per far riflettere
        // la forzatura sull'UI (es. cambiare tab selezionato), senza causare loop infiniti
//        if (artistType != targetArtistType) {
//            artistType = targetArtistType
//        }

        // 3. Caricamento dati usando il tipo effettivo calcolato
        when (targetArtistType) {
            ArtistsType.Favorites -> Database.artists(sortBy, sortOrder).collect { items = it }
            ArtistsType.Library -> Database.artistsInLibrary(sortBy, sortOrder).collect { items = it.filter { it.isYoutubeArtist } }
            ArtistsType.OnDevice -> Database.artistsOnDevice(sortBy, sortOrder).collect { items = it }
            ArtistsType.All -> Database.artistsWithSongsSaved(sortBy, sortOrder).collect { items = it }
        }
    }

    // Filtro Blacklist e Ricerca
    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Artist.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    LaunchedEffect(items, search.input) {
        // Salvataggio posizione scroll per evitare salti durante il filtro
        val scrollIndex = if (getViewType() == ViewType.List) lazyListState.firstVisibleItemIndex else lazyGridState.firstVisibleItemIndex
        val scrollOffset = if (getViewType() == ViewType.List) lazyListState.firstVisibleItemScrollOffset else lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items
            .filter {
                it.name?.contains(search.input, true) ?: false
            }
            .filter { item -> blacklisted.value?.map { it.path }?.contains(item.id) == false }

        // Ripristino scroll
        if (getViewType() == ViewType.List) lazyListState.scrollToItem(scrollIndex, scrollOffset)
        else lazyGridState.scrollToItem(scrollIndex, scrollOffset)
    }

    // Caricamento Thumbnail mancanti
    if (items.any { it.thumbnailUrl == null }) {
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

    // Sync Logic
    //val sync = autoSyncToolbutton(R.string.autosync_channels)
    var justSynced by rememberSaveable { mutableStateOf(false) }
    val viewType = viewTypeToolbutton(R.string.viewType)

    // Pull to Refresh
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

    LaunchedEffect(Unit, justSynced) {
        if (!justSynced && importYTMSubscribedChannels())
            justSynced = true
    }

    // Menu Ordinamento
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
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxHeight()
                .fillMaxWidth(
                    if (NavigationBarPosition.Right.isCurrent()) Dimensions.contentWidthRightBar else 1f
                )
        ) {
            Column(Modifier.fillMaxSize()) {
                // 1. Header Pulito: Solo Titolo e Conteggio
                TabHeader(R.string.artists) {
                    HeaderInfo(itemsOnDisplay.size.toString(), R.drawable.music_artist)
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 2. Control Bar Unificata: Tabs + Sort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FIX:
                    // ButtonsRow è sicuramente un LazyRow. Non serve (e causa crash)
                    // avvolgerlo in un altro horizontalScroll.
                    // La Box con weight(1f) fornisce il vincolo di larghezza necessario.
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        ButtonsRow(
                            buttons = buttonsList,
                            currentValue = artistType,
                            onValueUpdate = { artistType = it },
                            // FillMaxWidth assicura che il LazyRow interno occupi tutto lo spazio disponibile
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // SORT CONTROLS
                    var isSortExpanded by remember { mutableStateOf(false) }

                    // Timer per auto-chiusura dopo 3 secondi di inattività
                    LaunchedEffect(isSortExpanded) {
                        if (isSortExpanded) {
                            delay(3000)
                            isSortExpanded = false
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            // Animazione della larghezza quando si espande/contrae
                            .animateContentSize(animationSpec = tween(durationMillis = 300))
                            .clip(getRoundnessShape())
                            .background(colorPalette().background1.copy(alpha = 0.5f))
                            .clickable {
                                // Se è espanso -> Apre il menu ordinamento
                                // Se è chiuso -> Espande il chip
                                if (isSortExpanded) {
                                    menuState.display { sortMenu() }
                                } else {
                                    isSortExpanded = true
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        // TESTO dell'ordinamento (Animato)
                        AnimatedVisibility(
                            visible = isSortExpanded,
                            enter = fadeIn(tween(200)) + expandHorizontally(expandFrom = Alignment.Start),
                            exit = fadeOut(tween(200)) + shrinkHorizontally(shrinkTowards = Alignment.Start)
                        ) {
                            Text(
                                text = stringResource(sortBy.textId),
                                style = typography().xs,
                                color = colorPalette().textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        HeaderIconButton(
                            icon = R.drawable.arrow_up,
                            color = colorPalette().text,
                            onClick = {
                                // Cliccando la freccia:
                                // Se espanso -> Inverte ordine (e resetta timer)
                                // Se chiuso -> Espande il chip
                                if (isSortExpanded) {
                                    sortOrder = if (sortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
                                } else {
                                    isSortExpanded = true
                                }
                            },
                            onLongClick = { menuState.display { sortMenu() } },
                            modifier = Modifier
                                .graphicsLayer { rotationZ = sortOrderIconRotation }

                        )
                    }
                }

                // 3. Toolbar delle azioni (Sync, Random, Shuffle, etc.)
                TabToolBar.Buttons(search, randomizer, shuffle, itemSize, viewType)

                // 4. Search Bar
                search.SearchBar(this)

                // 5. Contenuto (Lista o Griglia)
                if (itemsOnDisplay.isEmpty()) {
                    // Empty State Moderno
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.music_artist),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colorPalette().textDisabled
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_artists),
                                style = typography().m,
                                color = colorPalette().textSecondary
                            )
                        }
                    }
                } else {
                    if (getViewType() == ViewType.List) {
                        LazyListContainer(state = lazyListState) {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize()
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
                                            .animateItem( // Abilitata animazione fluida
                                                fadeInSpec = tween(durationMillis = 200),
                                                fadeOutSpec = tween(durationMillis = 200),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                                )
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
                        LazyListContainer(state = lazyGridState) {
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
                                            .animateItem( // Abilitata animazione fluida
                                                fadeInSpec = tween(durationMillis = 200),
                                                fadeOutSpec = tween(durationMillis = 200),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                                )
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
            }

            FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

            val showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
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

