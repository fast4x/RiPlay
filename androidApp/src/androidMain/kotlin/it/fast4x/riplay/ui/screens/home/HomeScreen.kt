package it.fast4x.riplay.ui.screens.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.LocalPlayerSheetState
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.toUiChip
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.data.models.toUiMood
import it.fast4x.riplay.enums.HomePagetype
import it.fast4x.riplay.extensions.appviewmodel.rememberIsNetworkConnected
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.extensions.updater.UpdateDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.screens.home.homepages.HomePage
import it.fast4x.riplay.ui.screens.home.homepages.HomePageExtended
import it.fast4x.riplay.utils.CheckForNewVersion
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import kotlin.system.exitProcess


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalSerializationApi
@UnstableApi
@Composable
fun HomeScreen(
    navController: NavController,
    onPlaylistUrl: (String) -> Unit,
    miniPlayer: @Composable () -> Unit = {},
    openTabFromShortcut: Int
) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val recommendationService = Dependencies.application.recommendationService

    var showNewversionDialog by rememberSaveable { mutableStateOf(true) }

    val checkUpdateState = appSettings.checkUpdateState

    val saveableStateHolder = rememberSaveableStateHolder()

    //val preferences = LocalContext.current.preferences
    //val enableQuickPicksPage by rememberPreference(enableQuickPicksPageKey.key, true)

    val isNetworkConnected = rememberIsNetworkConnected()

    val coroutineScope = rememberCoroutineScope()

    val openTabFromShortcut1 by remember{
        mutableIntStateOf(openTabFromShortcut)
    }

    //Timber.d("HomeScreen appSettings.homeScreenTabIndex ${appSettings.homeScreenTabIndex}")

    val tabIndex = if (openTabFromShortcut1 == -1) appSettings.homeScreenTabIndex else openTabFromShortcut1

    val offlineModeEnabled = appSettings.offlineModeEnabled
    LaunchedEffect(Unit, offlineModeEnabled) {
        if (offlineModeEnabled) {
            appSettingsManager.updateSettings(
                appSettings.copy(
                    homeScreenTabIndex = 1
                )
            )
        }
    }

    val isEnabledMusicIdentifier = appSettings.enableMusicIdentifier

    val transitionEffect = appSettings.transitionEffect

    val homePageType = appSettings.homePageType

    if (tabIndex == -2) navController.navigate(NavRoutes.search.name)
    if (tabIndex == -3) {
        if (isEnabledMusicIdentifier)
            navController.navigate(NavRoutes.musicIdentifier.name)
        else {
            navController.navigate(NavRoutes.home.name)
            SmartMessage("Music Identifier is disabled", context = LocalContext.current)
        }
    }

    ScreenContainer(
        navController,
        tabIndex,
        {
            coroutineScope.launch {
                appSettingsManager.updateSettings(
                    appSettings.copy(homeScreenTabIndex = it)
                )
            }
        },
        miniPlayer,
        transitionEffect = transitionEffect,
        navBarContent = { Item ->

            Item(0, stringResource(R.string.home), R.drawable.home, isNetworkConnected)
            Item(1, stringResource(R.string.songs), R.drawable.musical_notes, true)
            Item(2, stringResource(R.string.artists), R.drawable.music_artist, true)
            Item(3, stringResource(R.string.albums), R.drawable.music_album, true)
            Item(4, stringResource(R.string.playlists), R.drawable.music_library, true)

        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> when(homePageType) {
                    HomePagetype.Extended -> HomePageExtended(
                        onAlbumClick = {
                            navController.navigate(route = "${NavRoutes.album.name}/$it")
                        },
                        onArtistClick = {
                            navController.navigate(route = "${NavRoutes.artist.name}/$it")
                        },
                        onPlaylistClick = {
                            navController.navigate(route = "${NavRoutes.playlist.name}/$it")
                        },
                        onSearchClick = {
                            navController.navigate(NavRoutes.search.name)
                        },
                        onMoodAndGenresClick = { mood ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "mood",
                                mood.toUiMood()
                            )
                            navController.navigate(NavRoutes.mood.name)
                        },
                        onChipClick = { chip ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "chip",
                                chip.toUiChip()
                            )
                            navController.navigate(route = NavRoutes.chip.name)
                        },
                        onSettingsClick = {
                            navController.navigate(NavRoutes.settings.name)
                        },
                        navController = navController
                    )

                    HomePagetype.Classic -> HomePage(
                        recommendationService = recommendationService,
                        onAlbumClick = {
                            navController.navigate(route = "${NavRoutes.album.name}/$it")
                        },
                        onArtistClick = {
                            navController.navigate(route = "${NavRoutes.artist.name}/$it")
                        },
                        onPlaylistClick = {
                            navController.navigate(route = "${NavRoutes.playlist.name}/$it")
                        },
                        onSearchClick = {
                            navController.navigate(NavRoutes.search.name)
                        },
                        onMoodAndGenresClick = { mood ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "mood",
                                mood.toUiMood()
                            )
                            navController.navigate(NavRoutes.mood.name)
                        },
                        onChipClick = { chip ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "chip",
                                chip.toUiChip()
                            )
                            navController.navigate(route = NavRoutes.chip.name)
                        },
                        onSettingsClick = {
                            navController.navigate(NavRoutes.settings.name)
                        },
                        navController = navController
                    )

                    /*
                    HomePagetype.Atlas -> HomePageAtlas(
                            recommendationService = recommendationService,
                            onAlbumClick = {
                                navController.navigate(route = "${NavRoutes.album.name}/$it")
                            },
                            onArtistClick = {
                                navController.navigate(route = "${NavRoutes.artist.name}/$it")
                            },
                            onPlaylistClick = {
                                navController.navigate(route = "${NavRoutes.playlist.name}/$it")
                            },
                            onSearchClick = {
                                navController.navigate(NavRoutes.search.name)
                            },
                            onMoodAndGenresClick = { mood ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "mood",
                                    mood.toUiMood()
                                )
                                navController.navigate(NavRoutes.mood.name)
                            },
                            onChipClick = { chip ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "chip",
                                    chip.toUiChip()
                                )
                                navController.navigate(route = NavRoutes.chip.name)
                            },
                            onSettingsClick = {
                                navController.navigate(NavRoutes.settings.name)
                            },
                            navController = navController
                        )
                        */

                }

                1 -> HomeSongs(
                    navController = navController,
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                2 -> HomeArtists(
                    navController = navController,
                    onArtistClick = {
                        if (!it.id.startsWith(LOCAL_KEY_PREFIX))
                            navController.navigate(route = "${NavRoutes.artist.name}/${it.id}")
                        else navController.navigate(route = "${NavRoutes.onDeviceArtist.name}/${it.id}")
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                3 -> HomeAlbums(
                    navController = navController,
                    onAlbumClick = {
                        if (!it.id.startsWith(LOCAL_KEY_PREFIX))
                            navController.navigate(route = "${NavRoutes.album.name}/${it.id}")
                        else navController.navigate(route = "${NavRoutes.onDeviceAlbum.name}/${it.id}")
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                4 -> HomePlaylists(
                    navController = navController,
                    onPlaylistClick = {
                        if (!it.isOnDevice)
                            navController.navigate(route = "${NavRoutes.localPlaylist.name}/${it.playlist.id}")
                        else
                            navController.navigate(route = "${NavRoutes.onDevicePlaylist.name}/${it.folder?.replace("/", "$")}")
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }

                )

            }
        }
    }


    if (BuildConfig.FLAVOR == "full" && showNewversionDialog) {
        if (checkUpdateState == CheckUpdateState.Enabled)
            UpdateDialog(onClose = { showNewversionDialog = false })

        if (checkUpdateState == CheckUpdateState.OnlyCheck)
            CheckForNewVersion(
                onDismiss = {},
                onClose = { showNewversionDialog = false },
                onNoUpdateAvailable = {}
            )

    }

    // Exit app when user uses back
    val context = LocalContext.current
    var confirmCount by remember { mutableIntStateOf( 0 ) }
    val playerSheetState = LocalPlayerSheetState.current
    val closeWithBackButton = appSettings.closeWithBackButton
    BackHandler(
        enabled = !playerSheetState.isExpanded
    ) {
        // Prevent this from being applied when user is not on HomeScreen
        if( NavRoutes.home.isNotHere( navController ) )  {
            if ( navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED )
                navController.popBackStack()

            return@BackHandler
        }


        if (closeWithBackButton)
            if( confirmCount == 0 ) {
                SmartMessage(
                    context.resources.getString(R.string.press_once_again_to_exit),
                    context = context
                )
                confirmCount++

                // Reset confirmCount after 5s
                CoroutineScope( Dispatchers.Default ).launch {
                    delay( 5000L )
                    confirmCount = 0
                }
            } else {
                val activity = context as? Activity
                activity?.finishAffinity()
                // Close app with exit 0 notify that no problem occurred
                exitProcess( 0 )
            }
    }
}
