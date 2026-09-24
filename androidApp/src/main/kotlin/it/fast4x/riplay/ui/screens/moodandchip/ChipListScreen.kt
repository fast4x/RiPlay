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
import it.fast4x.riplay.data.models.Chip
import it.fast4x.riplay.ui.components.PageContainer
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun ChipListScreen(
    navController: NavController,
    chip: Chip,
    miniPlayer: @Composable () -> Unit = {},
) {
    PageContainer(
        //modifier = modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        ChipList(
            navController = navController,
            chip = chip
        )
    }
}
