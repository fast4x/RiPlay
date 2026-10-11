package it.fast4x.riplay.ui.screens.home.homepages

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.discoverPage
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.RecommendationService
import it.fast4x.riplay.extensions.listenerlevel.HomepageListenerLevelBadges
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.isLandscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.extensions.rewind.HomepageRewind
import it.fast4x.riplay.extensions.smoothloader.Loader
import it.fast4x.riplay.ui.components.themed.ChipItemColored
import it.fast4x.riplay.ui.components.themed.MoodItemColored
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.HomeDataCache
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.asVideoMediaItem
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.toMediaItem
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

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
fun HomePageOnline(
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
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsIsInitialized = appSettingsManager.isInitialized
    //Timber.d("HOME-GATE loaded=${appSettingsIsInitialized}")
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val playEventType = appSettings.playEventsType

    // usata per i mood ed i generi
    var discoverPage by remember { mutableStateOf(HomeDataCache.discoverPage) }
    var homePage by remember { mutableStateOf(HomeDataCache.homePage) }

    val showMoodsAndGenres = appSettings.showMoodsAndGenres
    val showListenerLevels = appSettings.showListenerLevels
    val refreshScope = rememberCoroutineScope()

    val blacklisted = remember {
        Database.blacklisted(listOf(BlacklistType.Song.name, BlacklistType.Video.name))
    }.collectAsState(initial = null, context = Dispatchers.IO)

    val loadDataMutex = Mutex()

    suspend fun loadData() {
        loadDataMutex.withLock {
            //Timber.d("HOME-LOAD start: cache home=${HomeDataCache.homePage != null} discover=${HomeDataCache.discoverPage != null}")
            try {
                withContext(Dispatchers.IO) {

                    //  Leggo  dalla cache, POI la chiamata
                    if (homePage == null && HomeDataCache.homePage != null) {
                        homePage = HomeDataCache.homePage
                    }

                    if (homePage == null) {

                        var attempt = 0
                        while (homePage == null && attempt < 3) {
//                            Timber.d(
//                                "HOME-ATTEMPT scope=${coroutineContext[CoroutineName]?.name} " +
//                                        "active=${coroutineContext[Job]?.isActive}"
//                            )

                            try {
                                homePage = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn())
//                                .onFailure {
//                                    Timber.e(it, "HOME-LOAD homePage attempt $attempt failed")
//                                }
                                    .getOrNull()
                            } catch (e: CancellationException) {
                                // SE questo scope era serviceScope e il service è stato ricreato nel frattempo,
                                // IL SERVIZIO NUOVO ha uno scope nuovo: logga l'identità del service corrente:
                                Timber.d("HOME-CANCELLATION — serviceScope.isActive=${binder?.serviceScope?.coroutineContext?.isActive}")
                                throw e
                            }
                            attempt++
                            if (homePage == null && attempt < 3) {
                                //Timber.d("HOME-LOAD empty (attempt $attempt), retrying")
                                delay((500L * attempt).milliseconds)   // backoff: 500, 1000, 2000ms
                            }
                        }
                        HomeDataCache.homePage = homePage


//                        Timber.d("HOME-LOAD isYtLoggedIn=${isYtLoggedIn()}")
//                        val result = EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).getOrNull()
//                        homePage = result
//                        HomeDataCache.homePage = result
//                        Timber.d("HOME-LOAD api result: ${result != null}, err=${EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).exceptionOrNull()?.message}")
                    }

                    if (showMoodsAndGenres) {
                        if (discoverPage == null && HomeDataCache.discoverPage != null) {
                            discoverPage = HomeDataCache.discoverPage
                        }
                        if (discoverPage == null) {
                            val result = Environment.discoverPage(appSettings.languageApp.code, appSettings.contentCountry.code).getOrNull()
                            //Timber.d("HOME-LOAD discoverPage api result: ${result != null}, err=${Environment.discoverPage(appSettings.languageApp.code, appSettings.contentCountry.code).exceptionOrNull()?.message}")
                            discoverPage = result
                            if (result != null) {
                                HomeDataCache.discoverPage = result
                            }
                        }
                    }
                }
            } catch (e: Exception ) {
                Timber.e(e, "HOME-LOAD FAILED")   // ← l'eccezione INTERA, non solo il messaggio!
            }
            //Timber.d("HOME-LOAD end: home=${homePage != null} discover=${discoverPage != null} related=${relatedPage != null}")
        }
    }

    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return

        HomeDataCache.clear()

        homePage = null
        discoverPage = null

        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            loadData()
            delay(500.milliseconds)
            refreshing = false
        }
    }

    LaunchedEffect( appSettingsIsInitialized, playEventType) {
        //Timber.d("HOME-EFFECT start loaded=$appSettingsIsInitialized")
        if (!appSettingsIsInitialized) return@LaunchedEffect

        val playEventChanged = HomeDataCache.lastPlayEventType != playEventType

        if (playEventChanged) {
            HomeDataCache.relatedPage = null
            HomeDataCache.trending = null
            HomeDataCache.lastPlayEventType = playEventType

        }

        loadData()

        if (HomeDataCache.homePage != null) homePage = HomeDataCache.homePage
        if (HomeDataCache.discoverPage != null) discoverPage = HomeDataCache.discoverPage

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
    //val quickPicksLazyGridState = rememberLazyGridState()
    val moodAngGenresLazyGridState = rememberLazyGridState()
    val chipsLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val showSearchTab = appSettings.showSearchTab

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText = appearanceSettings.disableScrollingText


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

                if (showListenerLevels)
                    HomepageListenerLevelBadges(navController)

                HomepageRewind(
                    showIfEndOfYear = true,
                    navController = navController,
                    playlistThumbnailSizeDp = playlistThumbnailSizeDp,
                    endPaddingValues = endPaddingValues,
                    disableScrollingText = disableScrollingText
                )

                homePage?.let { page ->

                    page.sections.forEach { section ->
                        if (section.items.isEmpty() || section.items.firstOrNull()?.key == null) return@forEach

                        val isSongSection = section.items.any { it is Environment.SongItem }

                        TitleMiniSection(
                            section.label ?: "", modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 4.dp)
                        )

                        Title(
                            title = section.title,
                        )

                        if (isSongSection) {
                            val density = LocalDensity.current
                            val fontSize = typography().xs.semiBold.fontSize
                            val textHeightDp = with(density) { fontSize.toDp() } * 2.5f
                            val singleRowHeight = songThumbnailSizeDp + (Dimensions.itemsVerticalPadding * 2) + (textHeightDp * .2f)
                            val totalGridHeight = singleRowHeight * (if (section.items.isNotEmpty()) 3 else 1)

                            LazyHorizontalGrid (
                                state = rememberLazyGridState(),
                                rows = GridCells.Fixed(if (section.items.isNotEmpty()) 3 else 1),
                                flingBehavior = ScrollableDefaults.flingBehavior(),
                                contentPadding = endPaddingValues,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalGridHeight)
                            ) {
                                items(
                                    items = section.items
                                ) { item ->
                                    when (item) {
                                        is Environment.SongItem -> {
                                            SongItem(
                                                song = item,
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
                                                                    },
                                                                    mediaItem = item.asMediaItem,
                                                                    onInfo = {
                                                                        navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.key}")
                                                                    },
                                                                    disableScrollingText = disableScrollingText,
                                                                    onBlacklist = {
                                                                        insertOrUpdateBlacklist(item.asSong)
                                                                    },
                                                                )
                                                            }
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                        },
                                                        onClick = {
                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                val mediaItem = if (item.isAudioOnly)

                                                                    item.toMediaItem()
                                                                else
                                                                    item.asVideoMediaItem

                                                                binder?.stopRadio()
                                                                withContext(Dispatchers.Main) {
                                                                    binder?.hybridPlayer?.forcePlay(mediaItem)
                                                                }
                                                                binder?.setupRadio(
                                                                    NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                                                )
                                                            }

                                                        }
                                                    ),

                                                )
                                        }
                                        is Environment.AlbumItem -> {
                                            //Timber.d("Environment homePage AlbumItem: ${item.info?.name}")
                                            AlbumItem(
                                                album = item,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.album.name}/${item.key}")
                                                })

                                            )
                                        }

                                        is Environment.ArtistItem -> {
                                            //Timber.d("Environment homePage ArtistItem: ${item.info?.name}")
                                            ArtistItem(
                                                artist = item,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                                })
                                            )
                                        }

                                        is Environment.PlaylistItem -> {
                                            //Timber.d("Environment homePage PlaylistItem: ${item.info?.name}")
                                            PlaylistItem(
                                                playlist = item,
                                                alternative = true,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                                })
                                            )
                                        }

                                        is Environment.VideoItem -> {
                                            //Timber.d("Environment homePage VideoItem: ${item.info?.name}")
                                            VideoItem(
                                                video = item,
                                                thumbnailHeightDp = songThumbnailSizeDp,
                                                thumbnailWidthDp = songThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    binder?.stopRadio()
                                                    binder?.hybridPlayer?.forcePlay(item.asMediaItem)
                                                })
                                            )
                                        }
                                        else -> {}
                                    }


                                }
                            }
                        } else {
                            LazyRow(contentPadding = endPaddingValues) {
                                items(section.items.filter { item ->
                                    blacklisted.value?.map { it.path }?.contains(item?.key) == false
                                }) { item ->
                                    when (item) {
                                        is Environment.SongItem -> {
                                            //Timber.d("HomePage LOOP SongItem: ${item.info?.name}")
                                            SongItem(
                                                song = item,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                modifier = Modifier.width(300.dp)
                                                    .clickable(onClick = {
                                                        binder?.hybridPlayer?.forcePlay(item.asMediaItem)
                                                    })
                                            )
                                        }

                                        is Environment.AlbumItem -> {
                                            //Timber.d("Environment homePage AlbumItem: ${item.info?.name}")
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
                                            //Timber.d("Environment homePage ArtistItem: ${item.info?.name}")
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
                                            //Timber.d("Environment homePage PlaylistItem: ${item.info?.name}")
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
                                            //Timber.d("Environment homePage VideoItem: ${item.info?.name}")
                                            VideoItem(
                                                video = item,
                                                thumbnailHeightDp = playlistThumbnailSizeDp,
                                                thumbnailWidthDp = playlistThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    binder?.stopRadio()
                                                    binder?.hybridPlayer?.forcePlay(item.asMediaItem)
                                                })
                                            )
                                        }

                                        null -> {}
                                    }

                                }
                            }
                        }
                    }

                    if (showMoodsAndGenres) {
                        if (page.chips?.isNotEmpty() == true) {
                            Title(
                                title = stringResource(R.string.mood),
                            )

                            LazyHorizontalGrid(
                                state = chipsLazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = ScrollableDefaults.flingBehavior(),
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
                            //Timber.d("HomePage discoverPage: genres ${page.moods.size}")
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
                } ?: Loader() // Se homepage è vuota visualizziamo il loader

                /****** END HOMEPAGE CONTENT *******/

                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }





            val showFloatingIcon = appSettings.showFloatingIcon
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


