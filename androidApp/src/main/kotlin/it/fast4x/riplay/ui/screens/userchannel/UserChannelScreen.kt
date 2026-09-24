package it.fast4x.riplay.ui.screens.userchannel

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
import it.fast4x.riplay.ui.screens.artist.ArtistOverview
import kotlinx.serialization.ExperimentalSerializationApi


@OptIn(ExperimentalSerializationApi::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun UserChannelScreen(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {},
) {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val disableScrollingText = appearanceSettings.disableScrollingText

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        UserChannelOverview(
            navController = navController,
            browseId = browseId,
            disableScrollingText = disableScrollingText,
            onNavigateTo = {}
        )
    }

}
