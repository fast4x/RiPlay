package it.fast4x.riplay.extensions.rewind.data

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp

enum class AnimationType {
    SLIDE_AND_FADE,
    SCALE_AND_FADE,
    EXPAND_FROM_CENTER,
    SLIDE_FROM_UP,
    SPRING_SCALE_IN
}

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


//@Composable
//fun AnimatedContent(
//    isVisible: Boolean,
//    delay: Int,
//    wide: Boolean = false,
//    content: @Composable AnimatedVisibilityScope.() -> Unit
//) {
//    AnimatedVisibility(
//        visible = isVisible,
//        enter = fadeIn(
//            animationSpec = tween(1000, delayMillis = delay, easing = EaseOutQuart)
//        ) + slideInVertically(
//            initialOffsetY = { it / 4 },
//            animationSpec = tween(1000, delayMillis = delay, easing = EaseOutQuart)
//        )
//    ) {
//        Box(modifier = if (wide) Modifier else Modifier.padding(horizontal = 12.dp)) {
//            content()
//        }
//    }
//}

@Composable
fun AnimatedContent(
    isVisible: Boolean,
    delay: Int,
    wide: Boolean = false,
    animationType: AnimationType = AnimationType.SLIDE_AND_FADE,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = AnimationEffect(animationType),
        exit = fadeOut(
            animationSpec = tween(500)
        )
    ) {
        Box(modifier = if (wide) Modifier else Modifier.padding(horizontal = 12.dp)) {
            content()
        }
    }
}

@Composable
fun AnimationEffect(animationType: AnimationType) = when (animationType) {
    AnimationType.SLIDE_AND_FADE -> {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 800)
        ) + fadeIn(animationSpec = tween(durationMillis = 800))
    }
    AnimationType.SCALE_AND_FADE -> {
        scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(
            animationSpec = tween(1000)
        )
    }
    AnimationType.EXPAND_FROM_CENTER -> {
        scaleIn(
            initialScale = 0.01f,
            animationSpec = tween(durationMillis = 600),
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(
            animationSpec = tween(durationMillis = 600)
        )
    }
    AnimationType.SLIDE_FROM_UP -> {
        slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = 500,
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 100
            )
        )
    }
    AnimationType.SPRING_SCALE_IN -> {
        scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialScale = 0.1f
        ) + fadeIn()
    }
}
