package it.fast4x.riplay.ui.screens.blacklist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.PersistMapCleanup
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.components.SimpleScreenContainer
import it.fast4x.riplay.ui.screens.artist.ArtistOverview
import it.fast4x.riplay.ui.screens.history.HistoryList

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun BlacklistScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {

    PageContainer(
        modifier = Modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        Blacklist(
            navController = navController,
        )
    }

}
