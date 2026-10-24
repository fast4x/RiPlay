package it.fast4x.riplay.ui.screens.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
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
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.data.models.toUiMood
import it.fast4x.riplay.enums.HomePageMode
import it.fast4x.riplay.extensions.appviewmodel.rememberIsNetworkConnected
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.extensions.updater.UpdateDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.extensions.spotifreeupdater.SpotifreeUpdater
import it.fast4x.riplay.ui.screens.home.homepages.HomePageLocal
import it.fast4x.riplay.ui.screens.home.homepages.HomePageOnline
import it.fast4x.riplay.ui.screens.home.homepages.HomePageMixed
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
    openTabFromShortcut: Int,
    playerIsExpanded: Boolean
) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsState().value

    val recommendationService = Dependencies.application.recommendationService

    var showNewversionDialog by rememberSaveable { mutableStateOf(true) }

    val checkUpdateState = appSettings.checkUpdateState

    val isNetworkConnected = rememberIsNetworkConnected()

    val coroutineScope = rememberCoroutineScope()

    val openTabFromShortcut1 by remember{
        mutableIntStateOf(openTabFromShortcut)
    }

    Timber.d("HomeScreen appSettings.homeScreenTabIndex ${appSettings.homeScreenTabIndex} openTabFromShortcut1 $openTabFromShortcut1")

    // Sólo 3 pestañas (Inicio / Buscar / Tu Biblioteca); índices antiguos (3, 4) van a Biblioteca
    val tabIndex = appSettings.homeScreenTabIndex.let { if (it > 2) 2 else it }

    val offlineModeEnabled = appSettings.offlineModeEnabled
    LaunchedEffect(Unit, offlineModeEnabled) {
        if (offlineModeEnabled) {
            appSettingsManager.updateSettings(
                appSettingsManager.activeSettings.value.copy(
                    homeScreenTabIndex = 2
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
                    appSettingsManager.activeSettings.value.copy(homeScreenTabIndex = it)
                )
            }
            Timber.d("HomeScreen clicked  homeScreenTabIndex = $it in db = ${appSettingsManager.activeSettings.value.homeScreenTabIndex}")
        },
        miniPlayer = miniPlayer,
        transitionEffect = transitionEffect,
        navBarContent = { Item ->

            Item(0, "Home", R.drawable.home, true)
            Item(1, "Search", R.drawable.search, true)
            Item(2, "Your Library", R.drawable.library, true)

        }
    ) { currentTabIndex ->
        //saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> SpotifyHome(navController = navController)

                1 -> SpotifySearchTab(navController = navController)

                2 -> SpotifyLibraryTab(navController = navController)

            }
        //}
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

    // Spotifree: check GitHub for a newer automatic build
    SpotifreeUpdater.Check()

    // Exit app when user uses back
    val context = LocalContext.current
    var confirmCount by remember { mutableIntStateOf( 0 ) }
    val playerSheetState = LocalPlayerSheetState.current
    val closeWithBackButton = appSettings.closeWithBackButton
    val isOnHome = NavRoutes.home.isHere(navController)
    BackHandler(
        enabled = isOnHome && (!playerSheetState.isExpanded || !playerIsExpanded)
    ) {

        // Como Spotify: atrás en Inicio minimiza la app; la música sigue sonando.
        if (tabIndex != 0) {
            coroutineScope.launch {
                appSettingsManager.updateSettings(
                    appSettingsManager.activeSettings.value.copy(homeScreenTabIndex = 0)
                )
            }
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }
}
