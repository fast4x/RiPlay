package it.fast4x.riplay.ui.screens.home.homepages

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.HomePage
import it.fast4x.environment.requests.chartsPageComplete
import it.fast4x.environment.requests.discoverPage
import it.fast4x.environment.requests.relatedPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.Countries
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.HomePageSection
import it.fast4x.riplay.enums.HomeType
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.screens.welcome.WelcomeMessage
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.HOME_TYPE
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAY_EVENTS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUICK_PICS_DISCOVER_PAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUICK_PICS_RELATED_PAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUICK_PICS_TRENDING_SONG
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SELECTED_COUNTRY_CODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_CHARTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MONTHLY_PLAYLIST_IN_QUICK_PICKS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MOODS_AND_GENRES
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS_ARTISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PLAYLIST_MIGHT_LIKE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_RELATED_ALBUMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SEARCH_TAB
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SIMILAR_ARTISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TIPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUICK_PICS_HOME_PAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LISTENER_LEVELS
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.utils.HomeDataCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

@ExperimentalSerializationApi
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "UnusedBoxWithConstraintsScope")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomePageExtended(
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodAndGenresClick: (mood: Environment.Mood.Item) -> Unit,
    onChipClick: (chip: Environment.Chip) -> Unit,
    onSettingsClick: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    var playEventType by rememberPreference(PLAY_EVENTS_TYPE.key, PlayEventsType.MostPlayed)

    var trending by remember { mutableStateOf<Song?>(null) }

    var relatedPage by remember { mutableStateOf<Environment.RelatedPage?>(null) }

    var discoverPage by remember { mutableStateOf<Environment.DiscoverPage?>(null) }

    var homePage by remember { mutableStateOf<HomePage?>(null) }

    var chartsPage by remember { mutableStateOf<Environment.ChartsPage?>(null) }
    var chartsPageInit by remember { mutableStateOf<Environment.ChartsPage?>(null) }

    var preferitesArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    var localMonthlyPlaylists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    LaunchedEffect(Unit) {
        Database.monthlyPlaylistsPreview("").collect { localMonthlyPlaylists = it }
    }

    val showRelatedAlbums by rememberPreference(SHOW_RELATED_ALBUMS.key, true)
    val showSimilarArtists by rememberPreference(SHOW_SIMILAR_ARTISTS.key, true)
    val showNewAlbumsArtists by rememberPreference(SHOW_NEW_ALBUMS_ARTISTS.key, true)
    val showPlaylistMightLike by rememberPreference(SHOW_PLAYLIST_MIGHT_LIKE.key, true)
    val showMoodsAndGenres by rememberPreference(SHOW_MOODS_AND_GENRES.key, true)
    val showNewAlbums by rememberPreference(SHOW_NEW_ALBUMS.key, true)
    val showMonthlyPlaylistInQuickPicks by rememberPreference(
        SHOW_MONTHLY_PLAYLIST_IN_QUICK_PICKS.key,
        true
    )
    val showTips by rememberPreference(SHOW_TIPS.key, true)
    val showCharts by rememberPreference(SHOW_CHARTS.key, true)
    val showListenerLevels by rememberPreference(SHOW_LISTENER_LEVELS.key, true)
    val refreshScope = rememberCoroutineScope()

    var selectedCountryCode by rememberPreference(SELECTED_COUNTRY_CODE.key, Countries.ZZ)

    val parentalControlEnabled by rememberPreference(PARENTAL_CONTROL_ENABLED.key, false)

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    val loadDataMutex = Mutex()

    suspend fun loadData() {
        loadDataMutex.withLock {
            try {
                withContext(Dispatchers.IO) {

                    // 1. Charts
                    if (showCharts) {
                        val countryChanged = HomeDataCache.lastCountryCode != selectedCountryCode.name

                        if (chartsPage == null || countryChanged) {
                            // Provo a leggere dalla cache se il paese non è cambiato
                            if (!countryChanged && HomeDataCache.chartsPage != null) {
                                chartsPage = HomeDataCache.chartsPage
                            } else {
                                // Chiamata API con .getOrNull()
                                val result = Environment.chartsPageComplete(countryCode = selectedCountryCode.name).getOrNull()
                                chartsPage = result
                                // Salvo in cache solo se la chiamata ha avuto successo
                                if (result != null) {
                                    HomeDataCache.chartsPage = result
                                    HomeDataCache.lastCountryCode = selectedCountryCode.name
                                }
                            }
                        }
                    }

                    // 2. Controllo se è necessario caricare i dati
                    val needsLoading = homePage == null ||
                            ((showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) && discoverPage == null) ||
                            relatedPage == null

                    if (!needsLoading) return@withContext

                    // 3. Play Events
                    when (playEventType) {
                        PlayEventsType.MostPlayed -> {
                            val songs = Database.trending(3).distinctUntilChanged().first()
                            val song = songs.firstOrNull { item ->
                                blacklisted.value?.map { it.path }?.contains(item.id) == false
                            }
                            val songId = if (song?.isLocal == true) song.mediaId else song?.id

                            if (relatedPage == null || trending?.id != song?.id || trending?.mediaId != song?.id) {
                                // Leggo dalla cache se la canzone corrisponde
                                if (HomeDataCache.relatedPage != null && HomeDataCache.trending?.id == song?.id) {
                                    relatedPage = HomeDataCache.relatedPage
                                } else {
                                    // Chiamata API con .getOrNull()
                                    val result = Environment.relatedPage(
                                        NextBody(videoId = (songId ?: "HZnNt9nnEhw"))
                                    )?.getOrNull()
                                    relatedPage = result
                                    if (result != null) {
                                        HomeDataCache.relatedPage = result
                                    }
                                }
                            }
                            trending = song
                            HomeDataCache.trending = trending
                        }

                        PlayEventsType.LastPlayed, PlayEventsType.CasualPlayed -> {
                            val numSongs = if (playEventType == PlayEventsType.LastPlayed) 3 else 50
                            val songs = Database.lastPlayed(numSongs).distinctUntilChanged().first()
                            val song = (if (playEventType == PlayEventsType.LastPlayed) songs
                            else songs.shuffled()).firstOrNull { item ->
                                blacklisted.value?.map { it.path }?.contains(item.id) == false
                            }
                            val songId = if (song?.isLocal == true) song.mediaId else song?.id

                            if (relatedPage == null || trending?.id != song?.id || trending?.mediaId != song?.id) {
                                // Leggo dalla cache se la canzone corrisponde
                                if (HomeDataCache.relatedPage != null && HomeDataCache.trending?.id == song?.id) {
                                    relatedPage = HomeDataCache.relatedPage
                                } else {
                                    // Chiamata API con .getOrNull()
                                    val result = Environment.relatedPage(
                                        NextBody(videoId = (songId ?: "HZnNt9nnEhw"))
                                    )?.getOrNull()
                                    relatedPage = result
                                    if (result != null) {
                                        HomeDataCache.relatedPage = result
                                    }
                                }
                            }
                            trending = song
                            HomeDataCache.trending = trending
                        }
                    }

                    // 4. Discover Page
                    if (showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) {
                        if (discoverPage == null && HomeDataCache.discoverPage != null) {
                            discoverPage = HomeDataCache.discoverPage
                        } else if (discoverPage == null) {
                            // Chiamata API con .getOrNull()
                            val result = Environment.discoverPage().getOrNull()
                            discoverPage = result
                            if (result != null) {
                                HomeDataCache.discoverPage = result
                            }
                        }
                    }

                    // 5. Home Page
                    if (homePage == null && HomeDataCache.homePage != null) {
                        homePage = HomeDataCache.homePage
                    } else if (homePage == null) {
                        // Chiamata API con .getOrNull()
                        val result = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).getOrNull()
                        homePage = result
                        if (result != null) {
                            HomeDataCache.homePage = result
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("Explore loadData failed: ${e.message}")
            }
        }
    }

    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return
        trending = null
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            loadData()
            delay(500)
            refreshing = false
        }
    }

    LaunchedEffect(Unit, playEventType, selectedCountryCode) {
        loadData()
    }


    LaunchedEffect(Unit) {
        Database.preferitesArtistsByName().collect { preferitesArtists = it }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()
    val moodAngGenresLazyGridState = rememberLazyGridState()
    val chartsPageSongLazyGridState = rememberLazyGridState()
    val chartsPageArtistLazyGridState = rememberLazyGridState()
    val chipsLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val showSearchTab by rememberPreference(SHOW_SEARCH_TAB.key, false)

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)

    val buttonsList = listOf(
        HomePageSection.Home to HomePageSection.Home.textName
    ).toMutableList().apply {
        add(HomePageSection.ForYou to HomePageSection.ForYou.textName)
        add(HomePageSection.Other to HomePageSection.Other.textName)
    }


    var homePageSection by rememberSaveable { mutableStateOf(HomePageSection.Home) }
    var homeType by rememberPreference(HOME_TYPE.key, HomeType.Tabbed)


    PullToRefreshBox(
        refreshing = refreshing,
        onRefresh = { refresh() }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(
                    if (NavigationBarPosition.Right.isCurrent())
                        Dimensions.contentWidthRightBar
                    else
                        1f
                )

        ) {
            val quickPicksLazyGridItemWidthFactor =
                if (isLandscape && maxWidth * 0.475f >= 320.dp) {
                    0.475f
                } else {
                    0.9f
                }
            val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

            val moodItemWidthFactor =
                if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val itemWidth = maxWidth * moodItemWidthFactor


            Column(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {




                if (UiType.ViMusic.isCurrent())
                    HeaderWithIcon(
                        title = if (!isYtLoggedIn()) stringResource(R.string.quick_picks)
                        else stringResource(R.string.home),
                        iconId = R.drawable.search,
                        enabled = true,
                        showIcon = !showSearchTab,
                        modifier = Modifier,
                        onClick = onSearchClick,
                        navController = navController
                    )

                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WelcomeMessage()

                    IconButton(
                        modifier = Modifier.size(24.dp),
                        icon = when (homeType) {
                            HomeType.Tabbed -> R.drawable.singlepage
                            else -> R.drawable.multipage
                        },
                        onClick = { homeType = when (homeType) {
                                HomeType.Tabbed -> HomeType.Classic
                                else ->  HomeType.Tabbed
                            }
                        },
                        color = colorPalette().accent,
                    )
                }

                if (homeType == HomeType.Tabbed) {
                    ButtonsRow(
                        buttons = buttonsList,
                        currentValue = homePageSection,
                        onValueUpdate = {
                            homePageSection = it
                        },
                        modifier = Modifier.padding(all = 12.dp)
                    )
                }

// START SECTION HOME



    AnimatedVisibility(
        visible = homePageSection == HomePageSection.Home || homeType == HomeType.Classic
    ) {
        HomePageExtendedSections(
            navController = navController,
            showListenerLevels = showListenerLevels,
            showTips = showTips,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onPlaylistClick = onPlaylistClick,
            playlistThumbnailSizeDp = playlistThumbnailSizeDp,
            playlistThumbnailSizePx = playlistThumbnailSizePx,
            disableScrollingText = disableScrollingText,
            endPaddingValues = endPaddingValues,
            menuState = menuState,
            onPlayEventTypeClick = { playEventType = it },
            binder = binder,
            trending = trending,
            relatedInit = relatedPage,
            discoverPageInit = discoverPage,
            playEventType = playEventType,
            quickPicksLazyGridState = quickPicksLazyGridState,
            songThumbnailSizeDp = songThumbnailSizeDp,
            songThumbnailSizePx = songThumbnailSizePx,
            hapticFeedback = hapticFeedback,
            itemInHorizontalGridWidth = itemInHorizontalGridWidth,
            preferitesArtists = preferitesArtists,
            showNewAlbumsArtists = showNewAlbumsArtists,
            showNewAlbums = showNewAlbums,
            sectionTextModifier = sectionTextModifier,
            albumThumbnailSizeDp = albumThumbnailSizeDp,
            albumThumbnailSizePx = albumThumbnailSizePx,
            showRelatedAlbums = showRelatedAlbums,
            showSimilarArtists = showSimilarArtists,
            artistThumbnailSizeDp = artistThumbnailSizeDp,
            artistThumbnailSizePx = artistThumbnailSizePx,
            showPlaylistMightLike = showPlaylistMightLike,
            blacklisted = blacklisted
        )
    }

// END SECTION HOME

//START SECTION MOOD AND GENRES

    AnimatedVisibility(
        visible = homePageSection == HomePageSection.Other || homeType == HomeType.Classic
    ) {
        MoodAndGenresPart(
            homePageInit = homePage,
            chipsLazyGridState = chipsLazyGridState,
            endPaddingValues = endPaddingValues,
            onChipClick = onChipClick,
            showMoodsAndGenres = showMoodsAndGenres,
            discoverPageInit = discoverPage,
            navController = navController,
            moodAndGenresLazyGridState = moodAngGenresLazyGridState,
            onMoodAndGenresClick = onMoodAndGenresClick,
            playlistThumbnailSizeDp = playlistThumbnailSizeDp,
            playlistThumbnailSizePx = playlistThumbnailSizePx,
            disableScrollingText = disableScrollingText,
            showMonthlyPlaylistInQuickPicks = showMonthlyPlaylistInQuickPicks,
            localMonthlyPlaylists = localMonthlyPlaylists,
            moodAngGenresLazyGridState = moodAngGenresLazyGridState,
            showCharts = showCharts,
            chartsPageInit = chartsPageInit,
            selectedCountryCode = selectedCountryCode,
            menuState = menuState,
            onSelectCountryCode = { selectedCountryCode = it },
            onPlaylistClick = onPlaylistClick,
            chartsPageSongLazyGridState = chartsPageSongLazyGridState,
            parentalControlEnabled = parentalControlEnabled,
            songThumbnailSizeDp = songThumbnailSizeDp,
            songThumbnailSizePx = songThumbnailSizePx,
            binder = binder,
            itemWidth = itemWidth,
            chartsPageArtistLazyGridState = chartsPageArtistLazyGridState,
            onArtistClick = onArtistClick,
            blacklisted = blacklisted
        )
    }

// END SECTION MOOD AND GENRES

// START SECTION FOR YOU

                AnimatedVisibility(
                    visible = homePageSection == HomePageSection.ForYou || homeType == HomeType.Classic
                ) {
                    ForYouPart(
                        homePageInit = homePage,
                        endPaddingValues = endPaddingValues,
                        disableScrollingText = disableScrollingText,
                        navController = navController,
                        albumThumbnailSizeDp = albumThumbnailSizeDp,
                        albumThumbnailSizePx = albumThumbnailSizePx,
                        binder = binder,
                        artistThumbnailSizeDp = artistThumbnailSizeDp,
                        artistThumbnailSizePx = artistThumbnailSizePx,
                        playlistThumbnailSizeDp = playlistThumbnailSizeDp,
                        playlistThumbnailSizePx = playlistThumbnailSizePx,
                        blacklisted = blacklisted,
                        //relatedPageResult = relatedPageResult,
                    )
                }
// END SECTION FOR YOU

                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }


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


