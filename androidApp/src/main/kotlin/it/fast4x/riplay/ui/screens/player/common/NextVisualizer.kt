package it.fast4x.riplay.ui.screens.player.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import it.fast4x.riplay.extensions.nextvisualizer.NextVisualizer

@ExperimentalPermissionsApi
@UnstableApi
@Composable
fun NextVisualizer(
    isDisplayed: Boolean
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(500)),
    ) {
        NextVisualizer()
    }
}
