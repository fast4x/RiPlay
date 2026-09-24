package it.fast4x.riplay.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.ui.components.PageContainer
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun PlaylistScreen(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {},
) {

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        PlaylistSongList(
            navController = navController,
            browseId = browseId,
        )
    }

}
