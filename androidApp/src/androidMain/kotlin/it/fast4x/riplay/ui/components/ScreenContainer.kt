package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.components.navigation.nav.AbstractNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.HorizontalNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.VerticalNavigationBar
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.transition
import kotlin.math.abs
import kotlin.math.roundToInt


@Composable
fun ScreenContainer(
    navController: NavController,
    tabIndex: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    miniPlayer: @Composable (() -> Unit)? = null,
    navBarContent: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val navigationBar: AbstractNavigationBar =
        when( NavigationBarPosition.current() ) {
            NavigationBarPosition.Left, NavigationBarPosition.Right ->
                VerticalNavigationBar( tabIndex, onTabChanged, navController )
            NavigationBarPosition.Top, NavigationBarPosition.Bottom ->
                HorizontalNavigationBar( tabIndex, onTabChanged, navController )
        }
    navigationBar.add( navBarContent )

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

    val currentTopPaddingPx = (topBarHeightPx - toolbarOffset).coerceAtLeast(0f)
    val currentBottomPaddingPx = (bottomBarHeightPx - toolbarOffset).coerceAtLeast(0f)

    val density = LocalDensity.current
    val topPaddingDp = with(density) { currentTopPaddingPx.toDp() }
    val bottomPaddingDp = with(density) { currentBottomPaddingPx.toDp() }

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

            if ( NavigationBarPosition.Top.isCurrent() )
                navigationBar.Draw()
        }
    }

    val bottomBar: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    bottomBarHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            if ( NavigationBarPosition.Bottom.isCurrent() )
                navigationBar.Draw()
        }
    }

    val modifier: Modifier =
        if( UiType.ViMusic.isCurrent() && navigationBar is HorizontalNavigationBar)
            Modifier
        else
            Modifier.nestedScroll(nestedScrollConnection)

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
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, toolbarOffset.roundToInt()) }
            ) {
                bottomBar()
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

        Box(
            Modifier
                .padding(finalPadding)
                .fillMaxSize()
        ) {
            Row(
                Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                if( NavigationBarPosition.Left.isCurrent() )
                    navigationBar.Draw()

                val topPadding = if ( UiType.ViMusic.isCurrent() ) 30.dp else 0.dp
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = transition(),
                    content = content,
                    label = "",
                    modifier = Modifier.fillMaxHeight().padding( top = topPadding )
                )

                if( NavigationBarPosition.Right.isCurrent() )
                    navigationBar.Draw()
            }

            val playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)
            val playerAlignment =
                if (playerPosition == PlayerPosition.Top)
                    Alignment.TopCenter
                else
                    Alignment.BottomCenter

            Box(
                Modifier
                    .padding( vertical = 5.dp )
                    .align( playerAlignment ),
                content = { miniPlayer?.invoke() }
            )
        }
    }
}

/*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.components.navigation.nav.AbstractNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.HorizontalNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.VerticalNavigationBar
import it.fast4x.riplay.utils.transition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContainer(
    navController: NavController,
    tabIndex: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    miniPlayer: @Composable (() -> Unit)? = null,
    navBarContent: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val navigationBar: AbstractNavigationBar =
        when( NavigationBarPosition.current() ) {
            NavigationBarPosition.Left, NavigationBarPosition.Right ->
                VerticalNavigationBar( tabIndex, onTabChanged, navController )
            NavigationBarPosition.Top, NavigationBarPosition.Bottom ->
                HorizontalNavigationBar( tabIndex, onTabChanged, navController )
        }
    navigationBar.add( navBarContent )

    val appHeader: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            if( UiType.RiPlay.isCurrent() )
                AppHeader( navController ).Draw()


            if ( NavigationBarPosition.Top.isCurrent() )
                navigationBar.Draw()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val modifier: Modifier =
        if( UiType.ViMusic.isCurrent() && navigationBar is HorizontalNavigationBar)
            Modifier
        else
            Modifier.nestedScroll( scrollBehavior.nestedScrollConnection )

    Scaffold(
        modifier = modifier,
        containerColor = colorPalette().background0,
        topBar = appHeader,
        bottomBar = {
            if ( NavigationBarPosition.Bottom.isCurrent() )
                navigationBar.Draw()
        }
    ) {
        val paddingSides = WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
        val innerPadding =
            if( NavigationBarPosition.Top.isCurrent() )
                windowInsets.only( paddingSides ).asPaddingValues()
            else
                PaddingValues( Dp.Hairline )

        Box(
            Modifier
                .padding(it)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                if( NavigationBarPosition.Left.isCurrent() )
                    navigationBar.Draw()

                val topPadding = if ( UiType.ViMusic.isCurrent() ) 30.dp else 0.dp
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = transition(),
                    content = content,
                    label = "",
                    modifier = Modifier.fillMaxHeight().padding( top = topPadding )
                )

                if( NavigationBarPosition.Right.isCurrent() )
                    navigationBar.Draw()
            }

            val playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)
            val playerAlignment =
                if (playerPosition == PlayerPosition.Top)
                    Alignment.TopCenter
                else
                    Alignment.BottomCenter

            Box(
                Modifier
                    .padding( vertical = 5.dp )
                    .align( playerAlignment ),
                content = { miniPlayer?.invoke() }
            )
        }
    }
}

 */