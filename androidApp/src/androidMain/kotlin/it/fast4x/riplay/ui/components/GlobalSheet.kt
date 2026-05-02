package it.fast4x.riplay.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput



@Composable
fun GlobalSheet(
    state: GlobalSheetState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler(onBack = state::hide)

        Spacer(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        state.hide()
                    }
                }
                .background(Color.Black.copy(alpha = 0.5f))
                .fillMaxSize()
        )
    }

    AnimatedVisibility(
        visible = state.isDisplayed,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        state.content()
    }
}
