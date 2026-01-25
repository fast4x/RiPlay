package it.fast4x.riplay.ui.screens.history

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.PersistMapCleanup
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.ui.components.ScreenContainer

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun HistoryScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) { currentTabIndex ->
        when (currentTabIndex) {
            0 -> HistoryList(
                navController = navController,
            )
        }
    }

}
