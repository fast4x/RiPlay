package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.screens.localplaylist.LocalPlaylistSongs
import it.fast4x.riplay.utils.colorPalette

@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val transitionEffect by rememberPreference(transitionEffectKey, TransitionEffect.Scale)
    val playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        containerColor = colorPalette().background0,
        topBar = {
            if( UiType.RiPlay.isCurrent() )
                AppHeader( navController ).Draw()
        }
    ) {
        //**
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            Row(
                modifier = modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                val topPadding = if ( UiType.ViMusic.isCurrent() ) 30.dp else 0.dp

                AnimatedContent(
                    targetState = 0,
                    transitionSpec = {
                        when (transitionEffect) {
                            TransitionEffect.None -> EnterTransition.None togetherWith ExitTransition.None
                            TransitionEffect.Expand -> expandIn(
                                animationSpec = tween(
                                    350,
                                    easing = LinearOutSlowInEasing
                                ), expandFrom = Alignment.BottomStart
                            ).togetherWith(
                                shrinkOut(
                                    animationSpec = tween(
                                        350,
                                        easing = FastOutSlowInEasing
                                    ), shrinkTowards = Alignment.CenterStart
                                )
                            )

                            TransitionEffect.Fade -> fadeIn(animationSpec = tween(350)).togetherWith(
                                fadeOut(animationSpec = tween(350))
                            )

                            TransitionEffect.Scale -> scaleIn(animationSpec = tween(350)).togetherWith(
                                scaleOut(animationSpec = tween(350))
                            )

                            TransitionEffect.SlideHorizontal, TransitionEffect.SlideVertical -> {
                                val slideDirection = when (targetState > initialState) {
                                    true -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Left
                                        else AnimatedContentTransitionScope.SlideDirection.Up
                                    }

                                    false -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Right
                                        else AnimatedContentTransitionScope.SlideDirection.Down
                                    }
                                }

                                val animationSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = Spring.StiffnessLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )

                                slideIntoContainer(slideDirection, animationSpec) togetherWith
                                        slideOutOfContainer(slideDirection, animationSpec)
                            }
                        }
                    },
                    label = "",
                    modifier = Modifier
                        //.fillMaxWidth()
                        .fillMaxHeight()
                        .padding(top = topPadding),
                    content = content
                )
            }
            //**
            Box(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .align(if (playerPosition == PlayerPosition.Top) Alignment.TopCenter
                    else Alignment.BottomCenter)
            ) {
                miniPlayer.invoke()
            }
        }
    }
}