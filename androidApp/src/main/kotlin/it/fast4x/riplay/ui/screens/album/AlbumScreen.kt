package it.fast4x.riplay.ui.screens.album

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.PageContainer
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "SimpleDateFormat")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun AlbumScreen(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {}
) {

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        AlbumDetails(
            navController = navController,
            browseId = browseId,
            onSearchClick = {
                navController.navigate(NavRoutes.search.name)
            },
            onSettingsClick = {
                navController.navigate(NavRoutes.settings.name)
            },
            onNavigateTo = {}
        )
    }

}




