package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.ui.components.navigation.nav.AbstractNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.HorizontalNavigationBar
import it.fast4x.riplay.ui.components.navigation.nav.VerticalNavigationBar
import it.fast4x.riplay.utils.transition


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScaffold(
    navController: NavController,
    tabIndex: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    navBarContent: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    navigationBarVertical: Boolean = false,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val navigationBar: AbstractNavigationBar =
        if( navigationBarVertical )
            VerticalNavigationBar( tabIndex, onTabChanged, navController )
        else
         HorizontalNavigationBar( tabIndex, onTabChanged, navController )

    navigationBar.add( navBarContent )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val modifier: Modifier =
            Modifier.nestedScroll( scrollBehavior.nestedScrollConnection )

    Scaffold(
        modifier = modifier,
        containerColor = colorPalette().background0,
        topBar = {},
        bottomBar = {
            if (!navigationBarVertical)
                navigationBar.Draw()
        }
    ) {

        val innerPadding = PaddingValues( Dp.Hairline )

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

                // Left Navigation Bar
                if (navigationBarVertical)
                    navigationBar.Draw()

                val topPadding = 0.dp
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = transition(),
                    content = content,
                    label = "",
                    modifier = Modifier.fillMaxHeight().padding( top = topPadding )
                )

            }
        }
    }
}