package it.fast4x.riplay.ui.screens.home.homepages

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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalDensity
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
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui.RecommendationsBlock
import it.fast4x.riplay.extensions.listenerlevel.HomepageListenerLevelBadges
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.screens.welcome.WelcomeMessage
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAY_EVENTS_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SELECTED_COUNTRY_CODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MOODS_AND_GENRES
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS_ARTISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SEARCH_TAB
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TIPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LISTENER_LEVELS
import it.fast4x.riplay.extensions.rewind.HomepageRewind
import it.fast4x.riplay.ui.components.themed.ChipItemColored
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.MoodItemColored
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.HomeDataCache
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.asVideoMediaItem
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.toMediaItem
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
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
fun HomePage(
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodAndGenresClick: (mood: Environment.Mood.Item) -> Unit,
    onChipClick: (chip: Environment.Chip) -> Unit,
    onSettingsClick: () -> Unit,
    recommendationService: RecommendationService
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    var playEventType by rememberPreference(PLAY_EVENTS_TYPE.key, PlayEventsType.MostPlayed)

    val scope = rememberCoroutineScope()

    var trending by remember { mutableStateOf(HomeDataCache.trending) }
    var relatedPage by remember { mutableStateOf(HomeDataCache.relatedPage) }
    var discoverPage by remember { mutableStateOf(HomeDataCache.discoverPage) }
    var homePage by remember { mutableStateOf(HomeDataCache.homePage) }

    var preferitesArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    val showNewAlbumsArtists by rememberPreference(SHOW_NEW_ALBUMS_ARTISTS.key, true)
    val showMoodsAndGenres by rememberPreference(SHOW_MOODS_AND_GENRES.key, true)
    val showNewAlbums by rememberPreference(SHOW_NEW_ALBUMS.key, true)

    val showTips by rememberPreference(SHOW_TIPS.key, true)
    val showListenerLevels by rememberPreference(SHOW_LISTENER_LEVELS.key, true)
    val refreshScope = rememberCoroutineScope()

    var selectedCountryCode by rememberPreference(SELECTED_COUNTRY_CODE.key, Countries.ZZ)

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    val loadDataMutex = Mutex()

    fun Environment.RelatedPage?.copyFiltered(blacklist: List<Blacklist>?): Environment.RelatedPage? {
        val blacklistedPaths = blacklist?.map { it.path }
        return this?.copy(
            songs = songs?.filter { blacklistedPaths?.contains(it.key) == false },
            artists = artists?.filter { blacklistedPaths?.contains(it.key) == false },
            playlists = playlists?.filter { blacklistedPaths?.contains(it.key) == false },
            albums = albums?.filter { blacklistedPaths?.contains(it.key) == false }
        )
    }

    suspend fun loadData() {
        loadDataMutex.withLock {
            try {
                withContext(Dispatchers.IO) {

                    //  Leggo  dalla cache, POI la chiamata
                    if (homePage == null && HomeDataCache.homePage != null) {
                        homePage = HomeDataCache.homePage
                    }
                    if (homePage == null) {
                        val result = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).getOrNull()
                        homePage = result
                        HomeDataCache.homePage = result
                    }

                    if (showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) {
                        if (discoverPage == null && HomeDataCache.discoverPage != null) {
                            discoverPage = HomeDataCache.discoverPage
                        }
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
                                // Leggo relatedPage dalla cache se disponibile
                                if (relatedPage == null && HomeDataCache.relatedPage != null
                                    && trending?.id == HomeDataCache.trending?.id) {
                                    relatedPage = HomeDataCache.relatedPage
                                    trending = HomeDataCache.trending
                                } else {
                                    relatedPage = Environment.relatedPage(
                                        NextBody(videoId = (songId ?: "HZnNt9nnEhw"))
                                    )?.getOrNull().let { result ->
                                        result?.copyFiltered(blacklisted.value)
                                    }
                                    HomeDataCache.relatedPage = relatedPage
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
                                if (relatedPage == null && HomeDataCache.relatedPage != null
                                    && trending?.id == HomeDataCache.trending?.id) {
                                    relatedPage = HomeDataCache.relatedPage
                                    trending = HomeDataCache.trending
                                } else {
                                    relatedPage = Environment.relatedPage(
                                        NextBody(videoId = (songId ?: "HZnNt9nnEhw"))
                                    )?.getOrNull().let { result ->
                                        result?.copyFiltered(blacklisted.value)
                                    }
                                    HomeDataCache.relatedPage = relatedPage
                                }
                            }
                            trending = song
                            HomeDataCache.trending = trending
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("HomePage loadData failed: ${e.message}")
            }
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

    val songThumbnailSizeDp = Dimensions.thumbnails.song - 10.dp
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = Dimensions.thumbnails.album
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = Dimensions.thumbnails.artist
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = Dimensions.thumbnails.playlist
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

    val showSearchTab by rememberPreference(SHOW_SEARCH_TAB.key, false)

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)


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

                BasicText(
                    text = "Recommendations",
                    style = typography().l.bold,
                    modifier = sectionTextModifier
                )
                RecommendationsBlock(
                    recommendationService,
                    onPlayItem = { item ->
                        // Play del brano + marca come consumato
                        scope.launch {
                            item.song?.let { song ->
                                binder?.player?.forcePlay(song.asMediaItem)
                                recommendationService.markConsumed(item.strategyId, song.id)
                            }
                        }
                    },
                    onRejectItem = { item ->
                        // Long-press: mostra dialog "non interessato"
                        item.song?.let { song ->
                            scope.launch {
                                recommendationService.markRejected(song.id)
                            }
                        }
                        SmartMessage("Non interessato", context = appContext())
                    }
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

                    val density = LocalDensity.current
                    val fontSize = typography().xs.semiBold.fontSize
                    val textHeightDp = with(density) { fontSize.toDp() } * 2.5f
                    val singleRowHeight = songThumbnailSizeDp + (Dimensions.itemsVerticalPadding * 2) + (textHeightDp * .2f)
                    val totalGridHeight = singleRowHeight * (if (relatedPage != null) 3 else 1)

                    LazyHorizontalGrid (
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(if (relatedPage != null) 3 else 1),
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalGridHeight)
                            //.height(if (relatedPage != null) Dimensions.itemsVerticalPadding * 3 * 9 else Dimensions.itemsVerticalPadding * 9)
                            //.height((songThumbnailSizeDp + (Dimensions.itemsVerticalPadding * 2)) * 3)
                    ) {
                        trending?.let { song ->
                            item {
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
                                            CoroutineScope(Dispatchers.IO).launch {
                                                Timber.d("HomePage Clicked on song")
                                                val mediaItem = if (song.isAudioOnly)
                                                //song.asMediaItem
                                                    song.toMediaItem()
                                                else
                                                    song.asVideoMediaItem

                                                binder?.stopRadio()
                                                withContext(Dispatchers.Main) {
                                                    binder?.player?.forcePlay(mediaItem)
                                                }
                                                binder?.setupRadio(
                                                    NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                                )
                                            }

                                        }
                                    ),

                                )
                        }

                    }

                    if (relatedPage == null) Loader()

                }

                discoverPage?.let { page ->

                    if (showNewAlbumsArtists) {
                        val newReleaseAlbumsFiltered = remember { mutableListOf<Environment.AlbumItem>() }
                        val preferredNames = remember { preferitesArtists.map { it.name }.toSet() }

                        LaunchedEffect(Unit, preferredNames) {
                            page.newReleaseAlbums.forEach { album ->
                                val apiAuthorsNames = album.authors?.map { it.name } ?: emptyList()
                                Timber.d("HomePage newReleaseAlbums Author ${album.title} $apiAuthorsNames")
                                val match = apiAuthorsNames.any { apiName ->

                                    preferredNames.any { dbName ->
                                        apiName?.contains(
                                            dbName.toString(),
                                            ignoreCase = true
                                        ) == true
                                    }
                                }
                                if (match) newReleaseAlbumsFiltered.add(album)
                            }

                            Timber.d("HomePage newReleaseAlbums preferredNames $preferredNames")
                            Timber.d("HomePage newReleaseAlbums newReleaseAlbumsFiltered $newReleaseAlbumsFiltered")
                        }



                        if (newReleaseAlbumsFiltered.isNotEmpty() && preferitesArtists.isNotEmpty()) {

                            BasicText(
                                text = stringResource(R.string.new_albums_of_your_artists),
                                style = typography().l.bold,
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
                                .padding(top = 8.dp, bottom = 4.dp)
                        )

                        Title(
                            title = it.title,
                        )
//                        BasicText(
//                            text = it.title,
//                            style = typography().l.bold.color(colorPalette().text),
//                            modifier = Modifier
//                                .padding(horizontal = 16.dp)
//                                .padding(vertical = 4.dp)
//                        )
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
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding) * 4)
                            ) {
                                items(
                                    items = homePage?.chips?.sortedBy { it.title } ?: emptyList(),
                                    key = { it.endpoint?.params.toString() }
                                ) {
                                    ChipItemColored(
                                        chip = it,
                                        onClick = { it.endpoint?.browseId?.let { _ -> onChipClick(it) } },
                                        modifier = Modifier
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
                                )

                                LazyHorizontalGrid(
                                    state = moodAngGenresLazyGridState,
                                    rows = GridCells.Fixed(4),
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding) * 4)
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


