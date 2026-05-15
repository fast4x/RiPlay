package it.fast4x.riplay.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.transitionEffectKey

private val tween350 = tween<Float>( 350 )

private fun slideDirection(
    transitionEffect: TransitionEffect,
    targetState: Int,
    initialState: Int
): AnimatedContentTransitionScope.SlideDirection {
    val isSlideHorizontal = transitionEffect == TransitionEffect.SlideHorizontal

    return when ( targetState > initialState ) {
        true ->
            if ( isSlideHorizontal )
                AnimatedContentTransitionScope.SlideDirection.Left
            else
                AnimatedContentTransitionScope.SlideDirection.Up
        false ->
            if ( isSlideHorizontal )
                AnimatedContentTransitionScope.SlideDirection.Right
            else
                AnimatedContentTransitionScope.SlideDirection.Down
    }
}

private fun scale(): ContentTransform = scaleIn( tween350 ) togetherWith scaleOut( tween350 )

private fun fade(): ContentTransform = fadeIn( tween350 ) togetherWith fadeOut( tween350 )

private fun expand(): ContentTransform {
    val expandIn = expandIn(
        tween( 350, 0, LinearOutSlowInEasing ),
        Alignment.TopStart
    )
    val shrinkOut = shrinkOut(
        tween( 350, 0, LinearOutSlowInEasing ),
        Alignment.TopStart
    )
    return expandIn togetherWith shrinkOut
}

private fun none(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

@Composable
fun transition(transitionEffect: TransitionEffect = TransitionEffect.SlideHorizontal): AnimatedContentTransitionScope<Int>.() -> ContentTransform {

    return {
        when( transitionEffect ) {
            TransitionEffect.None -> EnterTransition.None togetherWith ExitTransition.None
            TransitionEffect.Fade -> fadeIn(tween(350)) togetherWith fadeOut(tween(350))
            TransitionEffect.Scale -> scaleIn(tween(350), initialScale = 0.92f) + fadeIn(tween(350)) togetherWith
                    scaleOut(tween(350), targetScale = 0.92f) + fadeOut(tween(350))
            TransitionEffect.Expand -> expandIn(tween(350, easing = LinearOutSlowInEasing), Alignment.TopStart) togetherWith
                    shrinkOut(tween(350, easing = FastOutSlowInEasing), Alignment.TopStart)
            TransitionEffect.SlideVertical -> slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up, tween(350, easing = FastOutSlowInEasing)
            ) togetherWith slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down, tween(350, easing = FastOutSlowInEasing)
            )
            TransitionEffect.SlideHorizontal -> slideIntoContainer(
                if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left
                else AnimatedContentTransitionScope.SlideDirection.Right,
                tween(350, easing = FastOutSlowInEasing)
            ) togetherWith slideOutOfContainer(
                if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left
                else AnimatedContentTransitionScope.SlideDirection.Right,
                tween(350, easing = FastOutSlowInEasing),
                targetOffset = { it / 3 }
            )
        }
    }
}