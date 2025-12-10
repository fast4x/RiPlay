package it.fast4x.riplay.extensions.rewind.data

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SequentialAnimationContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(1000, easing = EaseOutQuart)
        ),
        exit = fadeOut(
            animationSpec = tween(500)
        )
    ) {
        content()
    }
}


@Composable
fun AnimatedContent(
    isVisible: Boolean,
    delay: Int,
    wide: Boolean = false,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(1000, delayMillis = delay, easing = EaseOutQuart)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(1000, delayMillis = delay, easing = EaseOutQuart)
        )
    ) {
        Box(modifier = if (wide) Modifier else Modifier.padding(horizontal = 12.dp)) {
            content()
        }
    }
}