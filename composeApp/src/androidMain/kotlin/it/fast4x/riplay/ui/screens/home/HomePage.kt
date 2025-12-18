package it.fast4x.riplay.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.HomePage
import it.fast4x.environment.requests.chartsPageComplete
import it.fast4x.environment.requests.discoverPage
import it.fast4x.environment.requests.relatedPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.Countries
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.HomeSection
import it.fast4x.riplay.extensions.listenerlevel.HomepageListenerLevelBadges
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.screens.welcome.WelcomeMessage
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.playEventsTypeKey
import it.fast4x.riplay.extensions.preferences.quickPicsDiscoverPageKey
import it.fast4x.riplay.extensions.preferences.quickPicsRelatedPageKey
import it.fast4x.riplay.extensions.preferences.quickPicsTrendingSongKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.extensions.preferences.selectedCountryCodeKey
import it.fast4x.riplay.ui.styling.semiBold
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
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.utils.asVideoMediaItem
import it.fast4x.riplay.extensions.preferences.quickPicsHomePageKey
import it.fast4x.riplay.extensions.preferences.showListenerLevelsKey
import it.fast4x.riplay.extensions.rewind.HomepageRewind
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.themed.ChipItemColored
import it.fast4x.riplay.ui.components.themed.MoodItemColored
import it.fast4x.riplay.utils.forcePlay
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days


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

    //var trending by persist<Song?>("home/trending")
    var trending by remember { mutableStateOf<Song?>(null) }
    //val trendingInit by persist<Song?>(tag = "home/trending")
    val trendingInit by remember { mutableStateOf<Song?>(null) }
    var trendingPreference by rememberPreference(quickPicsTrendingSongKey, trendingInit)

    //var relatedPageResult by persist<Result<Environment.RelatedPage?>?>(tag = "home/relatedPageResult")
    var relatedPageResult by remember { mutableStateOf<Result<Environment.RelatedPage?>?>(null) }
    //var relatedInit by persist<Environment.RelatedPage?>(tag = "home/relatedPage")
    var relatedInit by remember { mutableStateOf<Environment.RelatedPage?>(null) }
    var relatedPreference by rememberPreference(quickPicsRelatedPageKey, relatedInit)

    //var discoverPageResult by persist<Result<Environment.DiscoverPage?>>("home/discoveryAlbums")
    var discoverPageResult by remember { mutableStateOf<Result<Environment.DiscoverPage?>?>(null) }
    //var discoverPageInit by persist<Environment.DiscoverPage>("home/discoveryAlbums")
    var discoverPageInit by remember { mutableStateOf<Environment.DiscoverPage?>(null) }
    var discoverPagePreference by rememberPreference(quickPicsDiscoverPageKey, discoverPageInit)

    //var homePageResult by persist<Result<HomePage?>>("home/homePage")
    var homePageResult by remember { mutableStateOf<Result<HomePage?>?>(null) }
    //var homePageInit by persist<HomePage?>("home/homePage")
    var homePageInit by remember { mutableStateOf<HomePage?>(null) }
    var homePagePreference by rememberPreference(quickPicsHomePageKey, homePageInit)

    //var chartsPageResult by persist<Result<Environment.ChartsPage?>>("home/chartsPage")
    var chartsPageResult by remember { mutableStateOf<Result<Environment.ChartsPage?>?>(null) }
    //var chartsPageInit by persist<Environment.ChartsPage>("home/chartsPage")
    var chartsPageInit by remember { mutableStateOf<Environment.ChartsPage?>(null) }
//    var chartsPagePreference by rememberPreference(quickPicsChartsPageKey, chartsPageInit)



    //var preferitesArtists by persistList<Artist>("home/artists")
    var preferitesArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    //var localMonthlyPlaylists by persistList<PlaylistPreview>("home/monthlyPlaylists")
    var localMonthlyPlaylists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    LaunchedEffect(Unit) {
        Database.monthlyPlaylistsPreview("").collect { localMonthlyPlaylists = it }
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current


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
    val now = System.currentTimeMillis()
    val last50Year: Duration = 18250.days
    val from = last50Year.inWholeMilliseconds

    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)

    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    //var loadedData by rememberSaveable { mutableStateOf(false) }
    var loadedData by rememberPreference(loadedDataKey, false)

    suspend fun loadData() {

        //Used to refresh chart when country change
        if (showCharts)
            chartsPageResult =
                Environment.chartsPageComplete(countryCode = selectedCountryCode.name)

        if (loadedData) return

        runCatching {
            refreshScope.launch(Dispatchers.IO) {
                when (playEventType) {
                    PlayEventsType.MostPlayed ->
                        //Database.songsMostPlayedByPeriod(from, now, 1).distinctUntilChanged()
                        Database.trending(1).distinctUntilChanged()
                            .collect { songs ->
                                val song = songs.firstOrNull()
                                if (relatedPageResult == null || trending?.id != song?.id) {
                                    relatedPageResult = Environment.relatedPage(
                                        NextBody(
                                            videoId = (song?.id ?: "HZnNt9nnEhw")
                                        )
                                    )
                                }
                                trending = song
                            }

                    PlayEventsType.LastPlayed, PlayEventsType.CasualPlayed -> {
                        val numSongs = if (playEventType == PlayEventsType.LastPlayed) 1 else 50
                        Database.lastPlayed(numSongs).distinctUntilChanged().collect { songs ->
                            val song =
                                if (playEventType == PlayEventsType.LastPlayed) songs.firstOrNull()
                                else songs.shuffled().firstOrNull()
                            if (relatedPageResult == null || trending?.id != song?.id) {
                                relatedPageResult =
                                    Environment.relatedPage(
                                        NextBody(
                                            videoId = (song?.id ?: "HZnNt9nnEhw")
                                        )
                                    )
                            }
                            trending = song
                        }
                    }

                }
            }

            if (showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) {
                discoverPageResult = Environment.discoverPage()
            }

            if (isLoggedIn()) {
                homePageResult = EnvironmentExt.getHomePage()
                //todo implement chips
                //Timber.d("Homepage ${homePageResult?.getOrNull()?.chips?.map { it.title }}")
            }

        }.onFailure {
            //Timber.e("Failed loadData  ${it.stackTraceToString()}")
            loadedData = false
        }.onSuccess {
            //Timber.d("Success loadData ")
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

//    val cachedSongs = remember {
//        try {
//            binder?.cache?.keys?.toMutableList()
//        } catch (e: Exception) {
//            null
//        }
//    }


    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val buttonsList = listOf(
        HomeSection.Home to HomeSection.Home.textName
    ).toMutableList().apply {
        if (isLoggedIn()) add(HomeSection.ForYou to HomeSection.ForYou.textName)
        add(HomeSection.Other to HomeSection.Other.textName)
    }


    var homeSection by rememberSaveable { mutableStateOf(HomeSection.Home) }


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


                if (UiType.ViMusic.isCurrent())
                    HeaderWithIcon(
                        title = if (!isLoggedIn()) stringResource(R.string.quick_picks)
                        else stringResource(R.string.home),
                        iconId = R.drawable.search,
                        enabled = true,
                        showIcon = !showSearchTab,
                        modifier = Modifier,
                        onClick = onSearchClick,
                        navController = navController
                    )




                WelcomeMessage()

                ButtonsRow(
                    buttons = buttonsList,
                    currentValue = homeSection,
                    onValueUpdate = {
                        homeSection = it
                    },
                    modifier = Modifier.padding(all = 12.dp)
                )

// START SECTION HOME

if (homeSection == HomeSection.Home) {

    if (showListenerLevels)
        HomepageListenerLevelBadges(navController)

    HomepageRewind(
        showIfEndOfYear = true,
        navController = navController,
        playlistThumbnailSizeDp = playlistThumbnailSizeDp,
        endPaddingValues = endPaddingValues,
        disableScrollingText = disableScrollingText
    )

    if (showTips) {
        Title2Actions(
            title = stringResource(R.string.tips),
            onClick1 = {
                menuState.display {
                    Menu {
                        MenuEntry(
                            icon = R.drawable.chevron_up,
                            text = stringResource(R.string.by_most_played_song),
                            onClick = {
                                playEventType = PlayEventsType.MostPlayed
                                menuState.hide()
                            }
                        )
                        MenuEntry(
                            icon = R.drawable.chevron_down,
                            text = stringResource(R.string.by_last_played_song),
                            onClick = {
                                playEventType = PlayEventsType.LastPlayed
                                menuState.hide()
                            }
                        )
                        MenuEntry(
                            icon = R.drawable.random,
                            text = stringResource(R.string.by_casual_played_song),
                            onClick = {
                                playEventType = PlayEventsType.CasualPlayed
                                menuState.hide()
                            }
                        )
                    }
                }
            },
            icon2 = R.drawable.play_now,
            onClick2 = {
                //trending?.let { fastPlay(it.asMediaItem, binder, relatedInit?.songs?.map { it.asMediaItem }) }
                binder?.stopRadio()
                trending?.let { binder?.player?.forcePlay(it.asMediaItem) }
                binder?.player?.addMediaItems(relatedInit?.songs?.map { it.asMediaItem }
                    ?: emptyList())
            }

            //modifier = Modifier.fillMaxWidth(0.7f)
        )

        BasicText(
            text = when (playEventType) {
                PlayEventsType.MostPlayed -> stringResource(R.string.by_most_played_song)
                PlayEventsType.LastPlayed -> stringResource(R.string.by_last_played_song)
                PlayEventsType.CasualPlayed -> stringResource(R.string.by_casual_played_song)
            },
            style = typography().xxs.secondary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        )




        LazyHorizontalGrid(
            state = quickPicksLazyGridState,
            rows = GridCells.Fixed(if (relatedInit != null) 3 else 1),
            flingBehavior = ScrollableDefaults.flingBehavior(),
            contentPadding = endPaddingValues,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (relatedInit != null) Dimensions.itemsVerticalPadding * 3 * 9 else Dimensions.itemsVerticalPadding * 9)
            //.height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
        ) {
            trending?.let { song ->
                item {
                    //val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                    //var forceRecompose by remember { mutableStateOf(false) }
                    SongItem(
                        song = song,
                        thumbnailSizePx = songThumbnailSizePx,
                        thumbnailSizeDp = songThumbnailSizeDp,
                        trailingContent = {
                            Image(
                                painter = painterResource(R.drawable.star),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette().accent),
                                modifier = Modifier
                                    .size(16.dp)
                            )
                        },
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
                                            mediaItem = song.asMediaItem,
                                            onRemoveFromQuickPicks = {
                                                Database.asyncTransaction {
                                                    clearEventsFor(song.id)
                                                }
                                            },
                                            onInfo = {
                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}")
                                            },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                },
                                onClick = {

                                    val mediaItem = if (song.isAudioOnly == 1)
                                        song.asMediaItem
                                    else
                                        song.asVideoMediaItem

                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(mediaItem)
                                    //binder?.player?.playOnline(mediaItem)
                                    //fastPlay(mediaItem, binder)
                                    binder?.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                    )
                                }
                            )
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .width(itemInHorizontalGridWidth),

                        )
                }
            }

            if (relatedInit != null) {
                items(
                    items = relatedInit?.songs?.distinctBy { it.key }
                        ?.dropLast(if (trending == null) 0 else 1)
                        ?: emptyList(),
                    key = Environment.SongItem::key
                ) { song ->
                    Timber.d("HomePage RELATED Environment.SongItem duration ${song.durationText}")
                    SongItem(
                        song = song,
                        thumbnailSizePx = songThumbnailSizePx,
                        thumbnailSizeDp = songThumbnailSizeDp,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .width(itemInHorizontalGridWidth)
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            navController = navController,
                                            onDismiss = {
                                                menuState.hide()
                                                //forceRecompose = true
                                            },
                                            mediaItem = song.asMediaItem,
                                            onInfo = {
                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.key}")
                                            },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                },
                                onClick = {
                                    println("HomePage Clicked on song")
                                    val mediaItem = if (song.isAudioOnly)
                                        song.asMediaItem
                                    else
                                        song.asVideoMediaItem

                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(mediaItem)
                                    //fastPlay(mediaItem, binder)
                                    binder?.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                    )
                                }
                            ),

                        )
                }
            }
        }

        if (relatedInit == null) Loader()

    }


    discoverPageInit?.let { page ->

        //var newReleaseAlbumsFiltered by persistList<Environment.AlbumItem>("discovery/newalbumsartist")
        var newReleaseAlbumsFiltered by remember { mutableStateOf(emptyList<Environment.AlbumItem>()) }
        page.newReleaseAlbums.forEach { album ->
            preferitesArtists.forEach { artist ->
                if (artist.name == album.authors?.first()?.name) {
                    newReleaseAlbumsFiltered += album
                }
            }
        }

        if (showNewAlbumsArtists)
            if (newReleaseAlbumsFiltered.isNotEmpty() && preferitesArtists.isNotEmpty()) {

                BasicText(
                    text = stringResource(R.string.new_albums_of_your_artists),
                    style = typography().l.semiBold,
                    modifier = sectionTextModifier
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = newReleaseAlbumsFiltered.distinctBy { it.key },
                        key = { it.key }) {
                        AlbumItem(
                            album = it,
                            thumbnailSizePx = albumThumbnailSizePx,
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier.clickable(onClick = {
                                onAlbumClick(it.key)
                            }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }

            }

        if (showNewAlbums) {
            Title(
                title = stringResource(R.string.new_albums),
                onClick = { navController.navigate(NavRoutes.newAlbums.name) },
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    items = page.newReleaseAlbums.distinctBy { it.key },
                    key = { it.key }) {
                    AlbumItem(
                        album = it,
                        thumbnailSizePx = albumThumbnailSizePx,
                        thumbnailSizeDp = albumThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable(onClick = {
                            onAlbumClick(it.key)
                        }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }
    }

    if (showRelatedAlbums)
        relatedInit?.albums?.let { albums ->
            BasicText(
                text = stringResource(R.string.related_albums),
                style = typography().l.semiBold,
                modifier = sectionTextModifier
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    items = albums.distinctBy { it.key },
                    key = Environment.AlbumItem::key
                ) { album ->
                    AlbumItem(
                        album = album,
                        thumbnailSizePx = albumThumbnailSizePx,
                        thumbnailSizeDp = albumThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = { onAlbumClick(album.key) }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

    if (showSimilarArtists)
        relatedInit?.artists?.let { artists ->
            BasicText(
                text = stringResource(R.string.similar_artists),
                style = typography().l.semiBold,
                modifier = sectionTextModifier
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    items = artists.distinctBy { it.key },
                    key = Environment.ArtistItem::key,
                ) { artist ->
                    ArtistItem(
                        artist = artist,
                        thumbnailSizePx = artistThumbnailSizePx,
                        thumbnailSizeDp = artistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = { onArtistClick(artist.key) }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

    if (showPlaylistMightLike)
        relatedInit?.playlists?.let { playlists ->
            BasicText(
                text = stringResource(R.string.playlists_you_might_like),
                style = typography().l.semiBold,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 8.dp)
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    items = playlists.distinctBy { it.key },
                    key = Environment.PlaylistItem::key,
                ) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        thumbnailSizePx = playlistThumbnailSizePx,
                        thumbnailSizeDp = playlistThumbnailSizeDp,
                        alternative = true,
                        showSongsCount = false,
                        modifier = Modifier
                            .clickable(onClick = { onPlaylistClick(playlist.key) }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

}
// END SECTION HOME

//START SECTION MOOD AND GENRES
if (homeSection == HomeSection.Other) {

    homePageInit?.let { page ->

        if (page.chips?.isNotEmpty() == true) {
            Title(
                title = stringResource(R.string.mood),
                //onClick = { navController.navigate(NavRoutes.moodsPage.name) },
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyHorizontalGrid(
                state = chipsLazyGridState,
                rows = GridCells.Fixed(4),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                //flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                contentPadding = endPaddingValues,
                modifier = Modifier
                    .fillMaxWidth()
                    //.height((thumbnailSizeDp + Dimensions.itemsVerticalPadding * 8) * 8)
                    .height(Dimensions.itemsVerticalPadding * 4 * 8)
            ) {
                items(
                    items = homePageInit?.chips?.sortedBy { it.title } ?: emptyList(),
                    key = { it.endpoint?.params!! }
                ) {
                    ChipItemColored(
                        chip = it,
                        onClick = { it.endpoint?.browseId?.let { _ -> onChipClick(it) } },
                        modifier = Modifier
                            //.width(itemWidth)
                            .padding(4.dp)
                    )
                }
            }
        }
    }


    if (showMoodsAndGenres)
        discoverPageInit?.let { page ->

            if (page.moods.isNotEmpty()) {

                Title(
                    title = stringResource(R.string.moods_and_genres),
                    onClick = { navController.navigate(NavRoutes.moodsPage.name) },
                    //modifier = Modifier.fillMaxWidth(0.7f)
                )

                LazyHorizontalGrid(
                    state = moodAngGenresLazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    //flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        //.height((thumbnailSizeDp + Dimensions.itemsVerticalPadding * 8) * 8)
                        .height(Dimensions.itemsVerticalPadding * 4 * 8)
                ) {
                    items(
                        items = page.moods.sortedBy { it.title },
                        key = { it.endpoint.params ?: it.title }
                    ) {
                        MoodItemColored(
                            mood = it,
                            onClick = { it.endpoint.browseId?.let { _ -> onMoodAndGenresClick(it) } },
                            modifier = Modifier
                                //.width(itemWidth)
                                .padding(4.dp)
                        )
                    }
                }

            }
        }

    HomepageRewind(
        navController = navController,
        playlistThumbnailSizeDp = playlistThumbnailSizeDp,
        endPaddingValues = endPaddingValues,
        disableScrollingText = disableScrollingText
    )

    if (showMonthlyPlaylistInQuickPicks)
        localMonthlyPlaylists.let { playlists ->
            if (playlists.isNotEmpty()) {
                BasicText(
                    text = stringResource(R.string.monthly_playlists),
                    style = typography().l.semiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp, bottom = 8.dp)
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = playlists.distinctBy { it.playlist.id },
                        key = { it.playlist.id }
                    ) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            thumbnailSizeDp = playlistThumbnailSizeDp,
                            thumbnailSizePx = playlistThumbnailSizePx,
                            alternative = true,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                )
                                .fillMaxSize()
                                .clickable(onClick = { navController.navigate(route = "${NavRoutes.localPlaylist.name}/${playlist.playlist.id}") }),
                            disableScrollingText = disableScrollingText,
                            isYoutubePlaylist = playlist.playlist.isYoutubePlaylist,
                            isEditable = playlist.playlist.isEditable
                        )
                    }
                }
            }
        }


    if (showCharts) {

        chartsPageInit?.let { page ->

            Title(
                title = "${stringResource(R.string.charts)} (${selectedCountryCode.countryName})",
                onClick = {
                    menuState.display {
                        Menu {
                            Countries.entries.forEach { country ->
                                MenuEntry(
                                    icon = R.drawable.arrow_right,
                                    text = country.countryName,
                                    onClick = {
                                        selectedCountryCode = country
                                        menuState.hide()
                                    }
                                )
                            }
                        }
                    }
                },
            )

            page.playlists?.let { playlists ->
                /*
                           BasicText(
                               text = stringResource(R.string.playlists),
                               style = typography().l.semiBold,
                               modifier = Modifier
                                   .padding(horizontal = 16.dp)
                                   .padding(top = 24.dp, bottom = 8.dp)
                           )
                             */

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = playlists.distinctBy { it.key },
                        key = Environment.PlaylistItem::key,
                    ) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            thumbnailSizePx = playlistThumbnailSizePx,
                            thumbnailSizeDp = playlistThumbnailSizeDp,
                            alternative = true,
                            showSongsCount = false,
                            modifier = Modifier
                                .clickable(onClick = { onPlaylistClick(playlist.key) }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            }

            page.songs?.let { songs ->
                if (songs.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.chart_top_songs),
                        style = typography().l.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )


                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(),
                        state = chartsPageSongLazyGridState,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                    ) {
                        itemsIndexed(
                            items = if (parentalControlEnabled)
                                songs.filter {
                                    !it.asSong.title.startsWith(
                                        EXPLICIT_PREFIX
                                    )
                                }.distinctBy { it.key }
                            else songs.distinctBy { it.key },
                            key = { _, song -> song.key }
                        ) { index, song ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                BasicText(
                                    text = "${index + 1}",
                                    style = typography().l.bold.center.color(
                                        colorPalette().text
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                SongItem(
                                    song = song,
                                    thumbnailSizePx = songThumbnailSizePx,
                                    thumbnailSizeDp = songThumbnailSizeDp,
                                    modifier = Modifier
                                        .clickable(onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            //fastPlay(mediaItem, binder)
                                            binder?.player?.addMediaItems(songs.map { it.asMediaItem })
                                        })
                                        .width(itemWidth),
                                    //disableScrollingText = disableScrollingText,
                                    //isNowPlaying = binder?.player?.isNowPlaying(song.key) ?: false
                                )
                            }
                        }
                    }
                }
            }

            page.artists?.let { artists ->
                if (artists.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.chart_top_artists),
                        style = typography().l.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )


                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(),
                        state = chartsPageArtistLazyGridState,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                    ) {
                        itemsIndexed(
                            items = artists.distinctBy { it.key },
                            key = { _, artist -> artist.key }
                        ) { index, artist ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                BasicText(
                                    text = "${index + 1}",
                                    style = typography().l.bold.center.color(
                                        colorPalette().text
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizePx = songThumbnailSizePx,
                                    thumbnailSizeDp = songThumbnailSizeDp,
                                    alternative = false,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .clickable(onClick = { onArtistClick(artist.key) }),
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// END SECTION MOOD AND GENRES

// START SECTION FOR YOU
if (homeSection == HomeSection.ForYou) {
    homePageInit?.let { page ->

        page.sections.forEach {
            if (it.items.isEmpty() || it.items.firstOrNull()?.key == null) return@forEach

            TitleMiniSection(
                it.label ?: "", modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
            )

            BasicText(
                text = it.title,
                style = typography().l.semiBold.color(colorPalette().text),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 4.dp)
            )
            LazyRow(contentPadding = endPaddingValues) {
                items(it.items) { item ->
                    when (item) {
                        is Environment.SongItem -> {
                            println("Innertube homePage SongItem: ${item.info?.name}")
                            SongItem(
                                song = item,
                                thumbnailSizePx = albumThumbnailSizePx,
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                //disableScrollingText = disableScrollingText,
                                //isNowPlaying = false,
                                modifier = Modifier.clickable(onClick = {
                                    binder?.player?.forcePlay(item.asMediaItem)
                                    //fastPlay(item.asMediaItem, binder)
                                })
                            )
                        }

                        is Environment.AlbumItem -> {
                            println("Innertube homePage AlbumItem: ${item.info?.name}")
                            AlbumItem(
                                album = item,
                                alternative = true,
                                thumbnailSizePx = albumThumbnailSizePx,
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                disableScrollingText = disableScrollingText,
                                modifier = Modifier.clickable(onClick = {
                                    navController.navigate("${NavRoutes.album.name}/${item.key}")
                                })

                            )
                        }

                        is Environment.ArtistItem -> {
                            println("Innertube homePage ArtistItem: ${item.info?.name}")
                            ArtistItem(
                                artist = item,
                                thumbnailSizePx = artistThumbnailSizePx,
                                thumbnailSizeDp = artistThumbnailSizeDp,
                                disableScrollingText = disableScrollingText,
                                modifier = Modifier.clickable(onClick = {
                                    navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                })
                            )
                        }

                        is Environment.PlaylistItem -> {
                            println("Innertube homePage PlaylistItem: ${item.info?.name}")
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
                            println("Innertube homePage VideoItem: ${item.info?.name}")
                            VideoItem(
                                video = item,
                                thumbnailHeightDp = playlistThumbnailSizeDp,
                                thumbnailWidthDp = playlistThumbnailSizeDp,
                                disableScrollingText = disableScrollingText,
                                modifier = Modifier.clickable(onClick = {
                                    binder?.stopRadio()
//                                                if (isVideoEnabled())
//                                                    binder?.player?.playOnline(item.asMediaItem)
//                                                else
                                    binder?.player?.forcePlay(item.asMediaItem)
                                    //fastPlay(item.asMediaItem, binder)
                                })
                            )
                        }

                        null -> {}
                    }

                }
            }
        }
    }
//                ?:
//                if (!isYouTubeLoggedIn())
//                    BasicText(
//                        text = "Log in to your YTM account for more content",
//                        style = typography().xs.center,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier
//                            .padding(vertical = 32.dp)
//                            .fillMaxWidth()
//                            .clickable {
//                                navController.navigate(NavRoutes.settings.name)
//                            }
//                    )
//                else
//                {
//                    ShimmerHost {
//                        repeat(3) {
//                            SongItemPlaceholder(
//                                thumbnailSizeDp = songThumbnailSizeDp,
//                            )
//                        }
//
//                        TextPlaceholder(modifier = sectionTextModifier)
//
//                        Row {
//                            repeat(2) {
//                                AlbumItemPlaceholder(
//                                    thumbnailSizeDp = albumThumbnailSizeDp,
//                                    alternative = true
//                                )
//                            }
//                        }
//
//                        TextPlaceholder(modifier = sectionTextModifier)
//
//                        Row {
//                            repeat(2) {
//                                PlaylistItemPlaceholder(
//                                    thumbnailSizeDp = albumThumbnailSizeDp,
//                                    alternative = true
//                                )
//                            }
//                        }
//                    }
//                }





    //} ?:

    relatedPageResult?.exceptionOrNull()?.let {
        BasicText(
            text = stringResource(R.string.page_not_been_loaded),
            style = typography().s.secondary.center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(all = 16.dp)
        )
    }

    /*
                if (related == null)
                    ShimmerHost {
                        repeat(3) {
                            SongItemPlaceholder(
                                thumbnailSizeDp = songThumbnailSizeDp,
                            )
                        }

                        TextPlaceholder(modifier = sectionTextModifier)

                        Row {
                            repeat(2) {
                                AlbumItemPlaceholder(
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }

                        TextPlaceholder(modifier = sectionTextModifier)

                        Row {
                            repeat(2) {
                                ArtistItemPlaceholder(
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }

                        TextPlaceholder(modifier = sectionTextModifier)

                        Row {
                            repeat(2) {
                                PlaylistItemPlaceholder(
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }
                    }
                 */
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


