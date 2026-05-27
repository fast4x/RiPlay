package it.fast4x.riplay.ui.screens.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSITION_EFFECT
import it.fast4x.riplay.ui.components.SimpleScreenContainer

@Composable
fun WelcomeScreen(
    navController: NavController,
) {
    val transitionEffect by rememberPreference(TRANSITION_EFFECT.key, TransitionEffect.SlideHorizontal)

    var tabIndex by remember { mutableIntStateOf(0) }

    SimpleScreenContainer (
        navController,
        tabIndex,
        onTabChanged = { tabIndex = it },
        transitionEffect = transitionEffect,
        navBarContent = { Item ->
            Item(0, stringResource(R.string.home), R.drawable.sparkles, true)
            Item(1, stringResource(R.string.songs), R.drawable.disc, true)
            Item(2, stringResource(R.string.artists), R.drawable.artists, true)
            Item(3, stringResource(R.string.albums), R.drawable.album, true)
            Item(4, stringResource(R.string.playlists), R.drawable.playlist, true)
        },
        navigationBarVertical = true
    ) { currentTabIndex ->
        when (currentTabIndex) {
            0 -> WelcomePage()
        }
    }
}