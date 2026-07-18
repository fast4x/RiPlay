package it.fast4x.riplay.ui.screens.ondevice

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.ui.components.PageContainer

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun OnDeviceArtistScreen(
    navController: NavController,
    artistId: String,
    miniPlayer: @Composable () -> Unit = {},
) {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value


    val disableScrollingText = appearanceSettings.disableScrollingText

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        OnDeviceArtistDetails(
            navController = navController,
            artistId = artistId,
            disableScrollingText = disableScrollingText
        )

    }

}
