package it.fast4x.riplay.ui.screens.home

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
import it.fast4x.riplay.enums.HomeSection
import it.fast4x.riplay.enums.HomeType
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.screens.welcome.WelcomeMessage
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.homeTypeKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.playEventsTypeKey
import it.fast4x.riplay.extensions.preferences.quickPicsDiscoverPageKey
import it.fast4x.riplay.extensions.preferences.quickPicsRelatedPageKey
import it.fast4x.riplay.extensions.preferences.quickPicsTrendingSongKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.selectedCountryCodeKey
import it.fast4x.riplay.extensions.preferences.showChartsKey
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import it.fast4x.riplay.extensions.preferences.showMonthlyPlaylistInQuickPicksKey
import it.fast4x.riplay.extensions.preferences.showMoodsAndGenresKey
import it.fast4x.riplay.extensions.preferences.showNewAlbumsArtistsKey
import it.fast4x.riplay.extensions.preferences.showNewAlbumsKey
import it.fast4x.riplay.extensions.preferences.showPlaylistMightLikeKey
import it.fast4x.riplay.extensions.preferences.showRelatedAlbumsKey
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.preferences.showSimilarArtistsKey
import it.fast4x.riplay.extensions.preferences.showTipsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.preferences.quickPicsHomePageKey
import it.fast4x.riplay.extensions.preferences.showListenerLevelsKey
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.screens.home.sections.ForYouPart
import it.fast4x.riplay.ui.screens.home.sections.HomeSectionPart
import it.fast4x.riplay.ui.screens.home.sections.MoodAndGenresPart
import kotlinx.coroutines.flow.first
import timber.log.Timber


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "UnusedBoxWithConstraintsScope")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomePage(
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
    var playEventType by rememberPreference(playEventsTypeKey, PlayEventsType.MostPlayed)

    var trending by remember { mutableStateOf<Song?>(null) }
    val trendingInit by remember { mutableStateOf<Song?>(null) }
    var trendingPreference by rememberPreference(quickPicsTrendingSongKey, trendingInit)

    var relatedPageResult by remember { mutableStateOf<Result<Environment.RelatedPage?>?>(null) }
    var relatedInit by remember { mutableStateOf<Environment.RelatedPage?>(null) }
    var relatedPreference by rememberPreference(quickPicsRelatedPageKey, relatedInit)

    var discoverPageResult by remember { mutableStateOf<Result<Environment.DiscoverPage?>?>(null) }
    var discoverPageInit by remember { mutableStateOf<Environment.DiscoverPage?>(null) }
    var discoverPagePreference by rememberPreference(quickPicsDiscoverPageKey, discoverPageInit)

    var homePageResult by remember { mutableStateOf<Result<HomePage?>?>(null) }
    var homePageInit by remember { mutableStateOf<HomePage?>(null) }
    var homePagePreference by rememberPreference(quickPicsHomePageKey, homePageInit)

    var chartsPageResult by remember { mutableStateOf<Result<Environment.ChartsPage?>?>(null) }
    var chartsPageInit by remember { mutableStateOf<Environment.ChartsPage?>(null) }

    var preferitesArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    var localMonthlyPlaylists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    LaunchedEffect(Unit) {
        Database.monthlyPlaylistsPreview("").collect { localMonthlyPlaylists = it }
    }

    val showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    val showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    val showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    val showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    val showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    val showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    val showMonthlyPlaylistInQuickPicks by rememberPreference(
        showMonthlyPlaylistInQuickPicksKey,
        true
    )
    val showTips by rememberPreference(showTipsKey, true)
    val showCharts by rememberPreference(showChartsKey, true)
    val showListenerLevels by rememberPreference(showListenerLevelsKey, true)
    val refreshScope = rememberCoroutineScope()

    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)

    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    var loadedData by rememberPreference(loadedDataKey, false)

    suspend fun loadData() {

        if (showCharts)
            chartsPageResult =
                Environment.chartsPageComplete(countryCode = selectedCountryCode.name)

        if (loadedData) return

        runCatching {
            refreshScope.launch(Dispatchers.IO) {
                when (playEventType) {
                    PlayEventsType.MostPlayed -> {
                        val songs = Database.trending(3).distinctUntilChanged().first()
                        val song = songs.firstOrNull { item ->
                            blacklisted.value?.map { it.path }?.contains(item.id) == false
                        }
                        val songId = if (song?.isLocal == true) song.mediaId else song?.id
                        if (relatedPageResult == null || trending?.id != song?.id || trending?.mediaId != song?.id) {
                            relatedPageResult = Environment.relatedPage(
                                NextBody(
                                    videoId = (songId ?: "HZnNt9nnEhw")
                                )
                            )
                        }
                        trending = song

                    }

                    PlayEventsType.LastPlayed, PlayEventsType.CasualPlayed -> {
                        val numSongs = if (playEventType == PlayEventsType.LastPlayed) 3 else 50
                        val songs = Database.lastPlayed(numSongs).distinctUntilChanged().first()
                        val song = (if (playEventType == PlayEventsType.LastPlayed) songs
                            else songs.shuffled()).firstOrNull { item ->
                            blacklisted.value?.map { it.path }?.contains(item.id) == false
                        }
                        val songId = if (song?.isLocal == true) song.mediaId else song?.id
                        Timber.d("HomePage Last played song $song relatedPageResult $relatedPageResult songId $songId")
                        if (relatedPageResult == null || trending?.id != song?.id || trending?.mediaId != song?.id) {
                            relatedPageResult =
                                Environment.relatedPage(
                                    NextBody(
                                        videoId = (songId ?: "HZnNt9nnEhw")
                                    )
                                )
                        }
                        trending = song

                    }

                }
            }

            if (showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) {
                discoverPageResult = Environment.discoverPage()
            }

            homePageResult = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn())


        }.onFailure {
            loadedData = false
        }.onSuccess {
            loadedData = true
        }
    }

    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return
        loadedData = false
        relatedPageResult = null
        relatedInit = null
        trending = null
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            loadData()
            delay(500)
            refreshing = false
        }
    }

    LaunchedEffect(Unit, playEventType, selectedCountryCode) {
        loadedData = false
        loadData()
        loadedData = true
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

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val buttonsList = listOf(
        HomeSection.Home to HomeSection.Home.textName
    ).toMutableList().apply {
        add(HomeSection.ForYou to HomeSection.ForYou.textName)
        add(HomeSection.Other to HomeSection.Other.textName)
    }


    var homeSection by rememberSaveable { mutableStateOf(HomeSection.Home) }
    var homeType by rememberPreference(homeTypeKey, HomeType.Tabbed)


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

            LaunchedEffect(loadedData) {
                /*   Load data from url or from saved preference   */
                if (trendingPreference != null) {
                    when (loadedData) {
                        true -> trending = trendingPreference
                        else -> trendingPreference = trending
                    }
                } else trendingPreference = trending

                if (relatedPreference != null) {
                    when (loadedData) {
                        true -> {
                            relatedPageResult = Result.success(relatedPreference)
                            relatedInit = relatedPageResult?.getOrNull()
                        }
                        else -> {
                            relatedInit = relatedPageResult?.getOrNull()
                            relatedPreference = relatedInit
                        }
                    }
                } else {
                    relatedInit = relatedPageResult?.getOrNull()
                    relatedPreference = relatedInit
                }

                if (discoverPagePreference != null) {
                    when (loadedData) {
                        true -> {
                            discoverPageResult = Result.success(discoverPagePreference)
                            discoverPageInit = discoverPageResult?.getOrNull()
                        }
                        else -> {
                            discoverPageInit = discoverPageResult?.getOrNull()
                            discoverPagePreference = discoverPageInit
                        }

                    }
                } else {
                    discoverPageInit = discoverPageResult?.getOrNull()
                    discoverPagePreference = discoverPageInit
                }

                // Not saved/cached to preference
                chartsPageInit = chartsPageResult?.getOrNull()

                if (homePagePreference != null) {
                    when (loadedData) {
                        true -> {
                            homePageResult = Result.success(homePagePreference)
                            homePageInit = homePageResult?.getOrNull()
                        }
                        else -> {
                            homePageInit = homePageResult?.getOrNull()
                            homePagePreference = homePageInit
                        }

                    }
                } else {
                    homePageInit = homePageResult?.getOrNull()
                    homePagePreference = homePageInit
                }

                /*   Load data from url or from saved preference   */
            }

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
                        currentValue = homeSection,
                        onValueUpdate = {
                            homeSection = it
                        },
                        modifier = Modifier.padding(all = 12.dp)
                    )
                }

// START SECTION HOME



    AnimatedVisibility(
        visible = homeSection == HomeSection.Home || homeType == HomeType.Classic
    ) {
        HomeSectionPart(
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
            relatedInit = relatedInit,
            discoverPageInit = discoverPageInit,
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
        visible = homeSection == HomeSection.Other || homeType == HomeType.Classic
    ) {
        MoodAndGenresPart(
            homePageInit = homePageInit,
            chipsLazyGridState = chipsLazyGridState,
            endPaddingValues = endPaddingValues,
            onChipClick = onChipClick,
            showMoodsAndGenres = showMoodsAndGenres,
            discoverPageInit = discoverPageInit,
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
                    visible = homeSection == HomeSection.ForYou || homeType == HomeType.Classic
                ) {
                    ForYouPart(
                        homePageInit = homePageInit,
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


