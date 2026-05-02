package it.fast4x.riplay.extensions.rewind.data

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import it.fast4x.riplay.R

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
    year: Int,
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
        Box() {
            Box(modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.BottomCenter)
                .zIndex(2f)) {
                Text(
                    text = stringResource(R.string.rw_riplay_rewind),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    lineHeight = 60.sp,
                    modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues())
                )
            }
            content()
        }
    }
}

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
        enter = animationEffect(animationType),
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
fun animationEffect(animationType: AnimationType) = when (animationType) {
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
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialScale = 0.1f
        ) + fadeIn()
    }
}
