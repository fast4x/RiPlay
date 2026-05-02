package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationDefaults.windowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.components.navigation.nav.HorizontalNavigationBar
import it.fast4x.riplay.ui.screens.localplaylist.LocalPlaylistSongs
import it.fast4x.riplay.utils.colorPalette
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PageContainer(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val transitionEffect by rememberPreference(transitionEffectKey, TransitionEffect.Scale)
    val playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)

    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var toolbarOffsetHeightPx by remember { mutableFloatStateOf(0f) }
    val toolbarOffset by animateFloatAsState(
        targetValue = toolbarOffsetHeightPx,
        label = "ToolbarOffset",
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = StiffnessMediumLow
        )
    )

    val appHeader: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    topBarHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            if( UiType.RiPlay.isCurrent() )
                AppHeader( navController ).Draw()

        }
    }

    val currentTopPaddingPx = (topBarHeightPx - toolbarOffset).coerceAtLeast(0f)
    val currentBottomPaddingPx = (bottomBarHeightPx - toolbarOffset).coerceAtLeast(0f)

    val density = LocalDensity.current
    val topPaddingDp = with(density) { currentTopPaddingPx.toDp() }
    val bottomPaddingDp = with(density) { currentBottomPaddingPx.toDp() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                if (available.y > 0 && toolbarOffsetHeightPx > 0) {
                    val consume = minOf(available.y, toolbarOffsetHeightPx)
                    toolbarOffsetHeightPx -= consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {

                if (consumed.y < 0) {
                    val maxOffset = maxOf(topBarHeightPx, bottomBarHeightPx)
                    val newOffset = toolbarOffsetHeightPx + abs(consumed.y)
                    toolbarOffsetHeightPx = newOffset.coerceIn(0f, maxOffset)
                }
                return Offset.Zero
            }
        }
    }

    val modifier: Modifier = Modifier.nestedScroll(nestedScrollConnection)

     Scaffold(
        modifier = modifier,
        containerColor = colorPalette().background0,
        topBar = {
            Column(
                modifier = Modifier
                    .offset { IntOffset(0, -toolbarOffset.roundToInt()) }
            ) {
                appHeader()
            }
        }
    ) { _ ->

         val paddingSides = WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
         val sidePaddingValues =
             if( NavigationBarPosition.Top.isCurrent() )
                 windowInsets.only( paddingSides ).asPaddingValues()
             else
                 PaddingValues( Dp.Hairline )
         val layoutDirection = LocalLayoutDirection.current
         val finalPadding = PaddingValues(
             start = sidePaddingValues.calculateStartPadding(layoutDirection),
             end = sidePaddingValues.calculateEndPadding(layoutDirection),
             top = topPaddingDp,
             bottom = bottomPaddingDp
         )

        //**
        Box(
            modifier = Modifier
                .padding(finalPadding)
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
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