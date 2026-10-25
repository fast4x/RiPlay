package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.components.navigation.nav.SpotifyBottomBar
import it.fast4x.riplay.ui.components.navigation.nav.SpotifyChipTabs
import it.fast4x.riplay.ui.components.navigation.nav.SpotifyGlobalBottomBar
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.transition

/** The shared Android shell keeps playback and navigation reachable while content scrolls. */
@Composable
fun ScreenContainer(
    navController: NavController,
    tabIndex: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    miniPlayer: @Composable (() -> Unit)? = null,
    transitionEffect: TransitionEffect = TransitionEffect.SlideHorizontal,
    navBarContent: @Composable (@Composable (Int, String, Int, Boolean) -> Unit) -> Unit,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val isHome = NavRoutes.home.isHere(navController)
    val chipTabs = mutableListOf<Pair<Int, String>>()
    navBarContent { index, text, _, visible ->
        if (visible) chipTabs.add(index to text)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = colorPalette().background0,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                if (!isHome || tabIndex == 0) {
                    AppHeader(navController).Draw()
                } else {
                    // Search and Library own their titles; keep only the status-bar reserve.
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
                }
                if (!isHome && chipTabs.size > 1) {
                    SpotifyChipTabs(chipTabs, tabIndex, onTabChanged)
                }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                miniPlayer?.let { player ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        player()
                    }
                }
                if (isHome) SpotifyBottomBar(tabIndex, onTabChanged)
                else SpotifyGlobalBottomBar(navController)
            }
        }
    ) { innerPadding ->
        // Scaffold reserves the measured bars and safe areas. Child pages must not
        // reserve the old player overlay a second time through this presentation local.
        CompositionLocalProvider(LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = transition(transitionEffect),
                    label = "Android navigation tab",
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
            }
        }
    }
}
