package it.fast4x.riplay.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import it.fast4x.riplay.ui.components.PageContainer

@OptIn(KotlinCsvExperimental::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    playlistId: Long,
    modifier: Modifier = Modifier,
    miniPlayer: @Composable () -> Unit = {}
) {

    PageContainer(
        modifier = modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        LocalPlaylistSongs(
            navController = navController,
            playlistId = playlistId,
            onDelete = {}
        )
    }

}
