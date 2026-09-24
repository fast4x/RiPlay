package it.fast4x.riplay.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.data.models.Chip
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.StatisticsType
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.data.models.Mood
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.extensions.appviewmodel.rememberIsNetworkConnected
import it.fast4x.riplay.ui.screens.player.common.Queue
import it.fast4x.riplay.ui.screens.blacklist.BlacklistScreen
import it.fast4x.riplay.extensions.listenerlevel.ListenerLevelCharts
import it.fast4x.riplay.extensions.musicbrainz.ui.AlbumInsightsScreen
import it.fast4x.riplay.extensions.musicbrainz.ui.ArtistInsightsScreen
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.screens.history.HistoryScreen
import it.fast4x.riplay.ui.screens.home.HomeScreen
import it.fast4x.riplay.ui.screens.ondevice.OnDeviceAlbumScreen
import it.fast4x.riplay.ui.screens.localplaylist.LocalPlaylistScreen
import it.fast4x.riplay.ui.screens.moodandchip.MoodListScreen
import it.fast4x.riplay.ui.screens.moodandchip.MoodsPageScreen
import it.fast4x.riplay.ui.screens.newreleases.NewreleasesScreen
import it.fast4x.riplay.ui.screens.ondevice.OnDeviceArtistScreen
import it.fast4x.riplay.ui.screens.playlist.PlaylistScreen
import it.fast4x.riplay.ui.screens.podcast.PodcastScreen
import it.fast4x.riplay.ui.screens.search.SearchScreen
import it.fast4x.riplay.ui.screens.settings.SettingsScreen
import it.fast4x.riplay.ui.screens.statistics.StatisticsScreen
import it.fast4x.riplay.ui.screens.welcome.WelcomeScreen
import it.fast4x.riplay.utils.ShowVideoOrSongInfo
import it.fast4x.riplay.extensions.rewind.RewindListScreen
import it.fast4x.riplay.extensions.rewind.RewindScreen
import it.fast4x.riplay.ui.screens.album.AlbumScreen
import it.fast4x.riplay.ui.screens.artist.ArtistScreen
import it.fast4x.riplay.ui.screens.moodandchip.ChipListScreen
import it.fast4x.riplay.ui.screens.onboarding.OnboardingScreen
import it.fast4x.riplay.ui.screens.ondevice.OnDevicePlaylistScreen
import it.fast4x.riplay.utils.MusicIdentifier
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class, ExperimentalTextApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class
)
@UnstableApi
@KotlinCsvExperimental
@ExperimentalPermissionsApi
@Composable
fun AppNavigation(
    miniPlayer: @Composable (artworkKey: String) -> Unit = {},
    openTabFromShortcut: Int,
    onNavControllerInit: (NavHostController) -> Unit,
    playerIsExpanded: Boolean
) {
    val navController = rememberNavController()
    onNavControllerInit(navController)

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val transitionEffect = appSettings.transitionEffect
    val queueType = appearanceSettings.queueType

    @Composable
    fun modalBottomSheetPage(
        showSheet: Boolean? = true,
        content: @Composable () -> Unit
    ) {

//        val thumbnailRoundness by rememberPreference(
//            THUMBNAIL_ROUNDNESS.key,
//            ThumbnailRoundness.Light
//        )
        val thumbnailRoundness = appearanceSettings.thumbnailRoundness

        CustomModalBottomSheet(
            showSheet = showSheet == true,
            onDismissRequest = {
                //if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                    navController.popBackStack()
            },
            containerColor = if (queueType == QueueType.Modern) colorPalette().background2.copy(alpha = 0.5f) else colorPalette().background2,
            contentColor = if (queueType == QueueType.Modern) colorPalette().background2.copy(alpha = 0.5f) else colorPalette().background2,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = Color.Transparent,
                ) {}
            },
            shape = thumbnailRoundness.shape()
        ) {
            content()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    //val context = LocalContext.current
    //clearPreference(context, HOME_SCREEN_TAB_INDEX.key)

    //var showOnBoardingScreen by rememberPreference(SHOW_ON_BOARDING_SCREEN.key, true)
    val showOnBoardingScreen = appSettings.showOnboardingScreen

    val isNetworkConnected = rememberIsNetworkConnected()

    NavHost(
        navController = navController,
        startDestination = if (showOnBoardingScreen) NavRoutes.onBoarding.name else NavRoutes.home.name,
        enterTransition = {
            when (transitionEffect) {
                TransitionEffect.None -> EnterTransition.None
                TransitionEffect.Expand -> expandIn(
                    animationSpec = tween(350, easing = LinearOutSlowInEasing),
                    expandFrom = Alignment.TopStart
                )
                TransitionEffect.Fade -> fadeIn(animationSpec = tween(350))
                TransitionEffect.Scale -> scaleIn(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialScale = 0.92f
                ) + fadeIn(animationSpec = tween(350))
                TransitionEffect.SlideVertical -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
                TransitionEffect.SlideHorizontal -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            }
        },
        exitTransition = {
            when (transitionEffect) {
                TransitionEffect.None -> ExitTransition.None
                TransitionEffect.Expand -> shrinkOut(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.TopStart
                )
                TransitionEffect.Fade -> fadeOut(animationSpec = tween(350))
                TransitionEffect.Scale -> scaleOut(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetScale = 0.92f
                ) + fadeOut(animationSpec = tween(350))
                TransitionEffect.SlideVertical -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
                TransitionEffect.SlideHorizontal -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetOffset = { it / 3 }
                ) + fadeOut(animationSpec = tween(350))
            }
        },
        popEnterTransition = {
            when (transitionEffect) {
                TransitionEffect.None -> EnterTransition.None
                TransitionEffect.Expand -> expandIn(
                    animationSpec = tween(350, easing = LinearOutSlowInEasing),
                    expandFrom = Alignment.TopStart
                )
                TransitionEffect.Fade -> fadeIn(animationSpec = tween(350))
                TransitionEffect.Scale -> scaleIn(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialScale = 0.92f
                ) + fadeIn(animationSpec = tween(350))
                // Speculare rispetto a exitTransition
                TransitionEffect.SlideVertical -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
                TransitionEffect.SlideHorizontal -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialOffset = { it / 3 }
                ) + fadeIn(animationSpec = tween(350))
            }
        },
        popExitTransition = {
            when (transitionEffect) {
                TransitionEffect.None -> ExitTransition.None
                TransitionEffect.Expand -> shrinkOut(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.TopStart
                )
                TransitionEffect.Fade -> fadeOut(animationSpec = tween(350))
                TransitionEffect.Scale -> scaleOut(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetScale = 0.92f
                ) + fadeOut(animationSpec = tween(350))
                // Speculare rispetto a enterTransition
                TransitionEffect.SlideVertical -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
                TransitionEffect.SlideHorizontal -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(350))
            }
        }
    ) {
        val navigateToAlbum =
            { browseId: String -> navController.navigate(route = "${NavRoutes.album.name}/$browseId") }
        val navigateToArtist =
            { browseId: String -> navController.navigate("${NavRoutes.artist.name}/$browseId") }
        val navigateToPlaylist =
            { browseId: String -> navController.navigate("${NavRoutes.playlist.name}/$browseId") }
        val pop = {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navController.popBackStack()
        }


        composable(
            route = "${NavRoutes.home.name}?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = if (isNetworkConnected) -1 else 1
                }
            )
        ) { backStackEntry ->
            HomeScreen(
                navController = navController,
                onPlaylistUrl = navigateToPlaylist,
                miniPlayer = { miniPlayer("${NavRoutes.home.name}?tab={tab}") } ,
                openTabFromShortcut = backStackEntry.arguments?.getInt("tab") ?: openTabFromShortcut,
                playerIsExpanded = playerIsExpanded
            )
        }

        composable(route = NavRoutes.onBoarding.name) {
            OnboardingScreen{
                coroutineScope.launch {
                    appSettingsManager.updateSettings (
                        appSettingsManager.activeSettings.value.copy(showOnboardingScreen = false)
                    )
                    navController.navigate(route = NavRoutes.home.name)
                }

            }
        }

        composable(route = NavRoutes.rewind.name) {
            modalBottomSheetPage {
                RewindListScreen(navController)
            }
        }

        composable(
            route = "${NavRoutes.rewind.name}/{year}",
            arguments = listOf(
                navArgument(
                    name = "year",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val year = navBackStackEntry.arguments?.getString("year") ?: ""
            modalBottomSheetPage {
                RewindScreen(year.toIntOrNull())
            }
        }


        composable(route = NavRoutes.listenerLevel.name) {
            modalBottomSheetPage {
                ListenerLevelCharts()
            }
        }

        composable(
            route = "${NavRoutes.videoOrSongInfo.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            modalBottomSheetPage {
                ShowVideoOrSongInfo(id)
            }
        }

        composable(route = NavRoutes.queue.name) {
            var showQueue by remember { mutableStateOf(true) }
            Queue (
                showQueue = showQueue,
                navController = navController,
                onDismiss = {
                    //showModalBottomSheetPage.value = false
                    showQueue = false
                    navController.popBackStack()
                },
                onDiscoverClick = {}
            )
        }

        composable(
            route = "${NavRoutes.artist.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            ArtistScreen(
                navController = navController,
                browseId = id,
                miniPlayer = { miniPlayer("${NavRoutes.artist.name}/{id}") },
            )
        }

        composable(
            route = "${NavRoutes.artistInsights.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            val showModalBottomSheetPage = rememberSaveable { mutableStateOf(true) }
            modalBottomSheetPage(showSheet = showModalBottomSheetPage.value) {
                ArtistInsightsScreen(
                    id,
                    onArtistClick = {},
                    onAlbumClick = {},
                    onBack = {}
                )
            }
        }

        composable(
            route = "${NavRoutes.onDeviceArtist.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            OnDeviceArtistScreen(
                navController = navController,
                artistId = id,
                miniPlayer = { miniPlayer("${NavRoutes.onDeviceArtist.name}/{id}") },
            )
        }

        composable(
            route = "${NavRoutes.album.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            AlbumScreen(
                navController = navController,
                browseId = id,
                miniPlayer = { miniPlayer("${NavRoutes.album.name}/{id}") },
            )
        }

        composable(
            route = "${NavRoutes.albumInsights.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            val showModalBottomSheetPage = rememberSaveable { mutableStateOf(true) }
            modalBottomSheetPage(showSheet = showModalBottomSheetPage.value) {
                AlbumInsightsScreen(
                    id,
                    onBack = {},
                    onAlbumClick = {},
                    onArtistClick = {},
                    onSongClick = {}
                )
            }
        }



        composable(
            route = "${NavRoutes.onDeviceAlbum.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            OnDeviceAlbumScreen(
                navController = navController,
                albumId = id,
                miniPlayer = { miniPlayer("${NavRoutes.onDeviceAlbum.name}/{id}") },
            )
        }

        composable(
            route = "${NavRoutes.playlist.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            PlaylistScreen(
                navController = navController,
                browseId = id,
                miniPlayer = { miniPlayer("${NavRoutes.playlist.name}/{id}") },
            )
        }

        composable(
            route = "${NavRoutes.podcast.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id") ?: ""
            PodcastScreen(
                navController = navController,
                browseId = id,
                miniPlayer = { miniPlayer("${NavRoutes.podcast.name}/{id}") },
            )
        }

        composable(route = NavRoutes.settings.name) {
            SettingsScreen(
                navController = navController,
                miniPlayer = { miniPlayer(NavRoutes.settings.name) },
            )
        }

        composable(route = NavRoutes.statistics.name) {
            StatisticsScreen(
                navController = navController,
                statisticsType = StatisticsType.Today,
                miniPlayer = { miniPlayer(NavRoutes.statistics.name) },
            )
        }

        composable(route = NavRoutes.history.name) {
            HistoryScreen(
                navController = navController,
                miniPlayer = { miniPlayer(NavRoutes.history.name) },

                )
        }

        composable(route = NavRoutes.musicIdentifier.name) {
            modalBottomSheetPage {
                MusicIdentifier(navController)
            }
        }

        composable(route = NavRoutes.blacklist.name) {
            BlacklistScreen(navController, { miniPlayer(NavRoutes.blacklist.name) })
        }

        composable(
            route = "${NavRoutes.search.name}?text={text}",
            arguments = listOf(
                navArgument(
                    name = "text",
                    builder = {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            )
        ) { navBackStackEntry ->
            val text = navBackStackEntry.arguments?.getString("text") ?: ""
            SearchScreen(
                navController = navController,
                miniPlayer = { miniPlayer("${NavRoutes.search.name}?text={text}") },
                query = text
            )
        }

        composable(
            route = "${NavRoutes.localPlaylist.name}/{id}",
            arguments = listOf(
                navArgument(
                    name = "id",
                    builder = { type = NavType.LongType }
                )
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getLong("id") ?: 0L

            LocalPlaylistScreen(
                navController = navController,
                playlistId = id,
                miniPlayer = { miniPlayer("${NavRoutes.localPlaylist.name}/{id}") }
            )
        }

        composable(
            route = "${NavRoutes.onDevicePlaylist.name}/{folder}",
            arguments = listOf(
                navArgument(
                    name = "folder",
                    builder = { type = NavType.StringType }
                )
            )
        ) { navBackStackEntry ->
            val folder = navBackStackEntry.arguments?.getString("folder") ?: ""

            OnDevicePlaylistScreen (
                navController = navController,
                folder = folder,
                miniPlayer = { miniPlayer("${NavRoutes.onDevicePlaylist.name}/{folder}") }
            )
        }

        composable(
            route = NavRoutes.mood.name,
        ) { navBackStackEntry ->
            val mood: Mood? = navController.previousBackStackEntry?.savedStateHandle?.get("mood")
            if (mood != null) {
                MoodListScreen(
                    navController = navController,
                    mood = mood,
                    miniPlayer = { miniPlayer(NavRoutes.mood.name) },
                )
            }
        }

        composable(
            route = NavRoutes.moodsPage.name
        ) { navBackStackEntry ->
            MoodsPageScreen(
                navController = navController,
                miniPlayer = { miniPlayer(NavRoutes.moodsPage.name) },
            )

        }

        composable(
            route = NavRoutes.chip.name,
        ) { navBackStackEntry ->
            val chip: Chip? = navController.previousBackStackEntry?.savedStateHandle?.get("chip")
            if (chip != null) {
                ChipListScreen(
                    navController = navController,
                    chip = chip,
                    miniPlayer = { miniPlayer(NavRoutes.chip.name) },
                )
            }
        }

        composable(
            route = NavRoutes.newAlbums.name
        ) { navBackStackEntry ->
            NewreleasesScreen(
                navController = navController,
                miniPlayer = { miniPlayer(NavRoutes.newAlbums.name) },
            )
        }

        composable(
            route = NavRoutes.welcome.name
        ) { navBackStackEntry ->
            WelcomeScreen(
                navController = navController
            )
        }
    }
}