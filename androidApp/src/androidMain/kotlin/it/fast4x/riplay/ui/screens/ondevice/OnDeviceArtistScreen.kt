package it.fast4x.riplay.ui.screens.ondevice

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.ui.components.ScreenContainer

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

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

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
