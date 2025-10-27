package it.fast4x.riplay.ui.screens.mood

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
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.ScreenContainer

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun MoodsPageScreen(
    navController: NavController
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(tagPrefix = "playlist/$defaultBrowseId")

            ScreenContainer(
                navController,
                navBarContent = { item ->
                    item(0, stringResource(R.string.moods_and_genres), R.drawable.music_album)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> MoodsPage(
                            navController = navController
                        )
                    }
                }
            }
}
