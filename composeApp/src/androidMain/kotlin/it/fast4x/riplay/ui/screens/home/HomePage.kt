package it.fast4x.riplay.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.HomePage
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
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.listenerlevel.HomepageListenerLevelBadges
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.screens.welcome.WelcomeMessage
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.playEventsTypeKey
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
import it.fast4x.riplay.extensions.rewind.HomepageRewind
import it.fast4x.riplay.ui.components.themed.ChipItemColored
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.MoodItemColored
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.HomeDataCache
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.asVideoMediaItem
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.typography
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

    var trending by remember { mutableStateOf(HomeDataCache.trending) }
    var relatedPage by remember { mutableStateOf(HomeDataCache.relatedPage) }
    var discoverPage by remember { mutableStateOf(HomeDataCache.discoverPage) }
    var homePage by remember { mutableStateOf(HomeDataCache.homePage) }

    var preferitesArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    val showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    val showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    val showNewAlbums by rememberPreference(showNewAlbumsKey, true)

    val showTips by rememberPreference(showTipsKey, true)
    val showListenerLevels by rememberPreference(showListenerLevelsKey, true)
    val refreshScope = rememberCoroutineScope()

    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    //var loadedData by rememberPreference(loadedDataKey, false)

    suspend fun loadData() {

        runCatching {
            refreshScope.launch(Dispatchers.IO) {

                if (homePage == null) {
                    val result = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).getOrNull()
                    homePage = result
                    HomeDataCache.homePage = result
                }

                if (showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) {
                    if (discoverPage == null) {
                        val result = Environment.discoverPage().getOrNull()
                        discoverPage = result
                        HomeDataCache.discoverPage = result
                    }
                }

                when (playEventType) {
                    PlayEventsType.MostPlayed -> {
                        val songs = Database.trending(3).distinctUntilChanged().first()
                        val song = songs.firstOrNull { item ->
                            blacklisted.value?.map { it.path }?.contains(item.id) == false
                        }
                        val songId = if (song?.isLocal == true) song.mediaId else song?.id

                        if (relatedPage == null || trending?.id != song?.id || trending?.mediaId != song?.id) {
                            relatedPage = Environment.relatedPage(
                                NextBody(
                                    videoId = (songId ?: "HZnNt9nnEhw")
                                )
                            )?.getOrNull().let {
                                it?.copy(
                                    songs = it.songs?.filter { item ->
                                        blacklisted.value?.map { it.path }?.contains(item.key) == false
                                    },
                                    artists = it.artists?.filter { item ->
                                        blacklisted.value?.map { it.path }?.contains(item.key) == false
                                    },
                                    playlists = it.playlists?.filter { item ->
                                        blacklisted.value?.map { it.path }?.contains(item.key) == false
                                    },
                                    albums = it.albums?.filter { item ->
                                        blacklisted.value?.map { it.path }?.contains(item.key) == false
                                    }
                                )
                            }
                            HomeDataCache.relatedPage = relatedPage
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
                            relatedPage =
                                Environment.relatedPage(
                                    NextBody(
                                        videoId = (songId ?: "HZnNt9nnEhw")
                                    )
                                )?.getOrNull().let {
                                    it?.copy(
                                        songs = it.songs?.filter { item ->
                                            blacklisted.value?.map { it.path }?.contains(item.key) == false
                                        },
                                        artists = it.artists?.filter { item ->
                                            blacklisted.value?.map { it.path }?.contains(item.key) == false
                                        },
                                        playlists = it.playlists?.filter { item ->
                                            blacklisted.value?.map { it.path }?.contains(item.key) == false
                                        },
                                        albums = it.albums?.filter { item ->
                                            blacklisted.value?.map { it.path }?.contains(item.key) == false
                                        }
                                    )
                                }
                            HomeDataCache.relatedPage = relatedPage
                        }
                        trending = song
                        HomeDataCache.trending = trending
                    }

                }
            }
        }.onFailure {
            Timber.e("HomePage loadData failed")
        }
    }

    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return

        HomeDataCache.clear()

        homePage = null
        discoverPage = null
        relatedPage = null
        trending = null

        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            loadData()
            delay(500)
            refreshing = false
        }
    }

    LaunchedEffect(Unit, playEventType, selectedCountryCode) {

        val countryChanged = HomeDataCache.lastCountryCode != selectedCountryCode.name
        val playEventChanged = HomeDataCache.lastPlayEventType != playEventType

        if (countryChanged) {
            HomeDataCache.homePage = null
            HomeDataCache.discoverPage = null
            HomeDataCache.lastCountryCode = selectedCountryCode.name

            homePage = null
            discoverPage = null
        }

        if (playEventChanged) {
            HomeDataCache.relatedPage = null
            HomeDataCache.trending = null
            HomeDataCache.lastPlayEventType = playEventType

            relatedPage = null
            trending = null
        }

        loadData()

        if (HomeDataCache.homePage != null) homePage = HomeDataCache.homePage
        if (HomeDataCache.discoverPage != null) discoverPage = HomeDataCache.discoverPage
        if (HomeDataCache.relatedPage != null) relatedPage = HomeDataCache.relatedPage
        if (HomeDataCache.trending != null) trending = HomeDataCache.trending
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
    val chipsLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)


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

                WelcomeMessage()

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
                        title = stringResource(R.string.quick_picks),
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
                            binder?.player?.addMediaItems(relatedPage?.songs?.map { it.asMediaItem }
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




                    LazyHorizontalGrid (
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(if (relatedPage != null) 3 else 1),
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (relatedPage != null) Dimensions.itemsVerticalPadding * 3 * 9 else Dimensions.itemsVerticalPadding * 9)
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
                                                        disableScrollingText = disableScrollingText,
                                                        onBlacklist = {
                                                            insertOrUpdateBlacklist(song)
                                                        },
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

                        items(
                            items = relatedPage?.songs?.distinctBy { it.key }
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
                                                    disableScrollingText = disableScrollingText,
                                                    onBlacklist = {
                                                        insertOrUpdateBlacklist(song.asSong)
                                                    },
                                                )
                                            }
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        },
                                        onClick = {
                                            Timber.d("HomePage Clicked on song")
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

                    if (relatedPage == null) Loader()

                }

                discoverPage?.let { page ->

                    val newReleaseAlbumsFiltered = mutableListOf<Environment.AlbumItem>()
                    val preferredNames = preferitesArtists.map { it.name }.toSet()

                    page.newReleaseAlbums.forEach { album ->
                        val apiAuthorsNames = album.authors?.map { it.name } ?: emptyList()

                        val match = apiAuthorsNames.any { apiName ->

                            preferredNames.any { dbName ->
                                apiName?.contains(dbName.toString(), ignoreCase = true) == true
                            }
                        }
                        if (match) newReleaseAlbumsFiltered.add(album)
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

                homePage?.let { page ->

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
                            items(it.items.filter {item -> blacklisted.value?.map { it.path }?.contains(item?.key) == false }) { item ->
                                when (item) {
                                    is Environment.SongItem -> {
                                        Timber.d("Environment homePage SongItem: ${item.info?.name}")
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
                                        Timber.d("Environment homePage AlbumItem: ${item.info?.name}")
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
                                        Timber.d("Environment homePage ArtistItem: ${item.info?.name}")
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
                                        Timber.d("Environment homePage PlaylistItem: ${item.info?.name}")
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
                                        Timber.d("Environment homePage VideoItem: ${item.info?.name}")
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

                    if (showMoodsAndGenres) {
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
                                    items = homePage?.chips?.sortedBy { it.title } ?: emptyList(),
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




                        discoverPage?.let { page ->

                            if (page.moods.isNotEmpty()) {

                                Title(
                                    title = stringResource(R.string.genres),
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
                                            onClick = {
                                                it.endpoint.browseId?.let { _ ->
                                                    onMoodAndGenresClick(
                                                        it
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                //.width(itemWidth)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

                /****** END HOMEPAGE CONTENT *******/

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


