package it.fast4x.riplay.ui.screens.moodandchip

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.data.models.Mood
import it.fast4x.riplay.ui.components.PageContainer

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun MoodListScreen(
    navController: NavController,
    mood: Mood,
    miniPlayer: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PageContainer(
        //modifier = modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        MoodList(
            navController = navController,
            mood = mood
        )
    }

//    ScreenContainer(
//        navController,
//        navBarContent = { item ->
//            item(0, stringResource(R.string.mood), R.drawable.music_album)
//        },
//        miniPlayer = miniPlayer
//    ) { currentTabIndex ->
//        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
//            when (currentTabIndex) {
//                0 -> MoodList(
//                    navController = navController,
//                    mood = mood
//                )
//            }
//        }
//    }
}
