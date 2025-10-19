package it.fast4x.riplay.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.screens.localplaylist.LocalPlaylistSongs

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
        modifier = Modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        PlaylistSongList(
            navController = navController,
            browseId = browseId,
        )
    }

}
