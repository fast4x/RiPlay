package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager

/** Detail pages use the same measured bars and safe areas as the main tabs. */
@Composable
fun PageContainer(
    navController: NavController,
    miniPlayer: @Composable (() -> Unit)? = null,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val appSettings = LocalAppSettingsManager.current.activeSettings.collectAsStateWithLifecycle().value
    ScreenContainer(
        navController = navController,
        miniPlayer = miniPlayer,
        transitionEffect = appSettings.transitionEffect,
        navBarContent = {},
        content = content
    )
}
