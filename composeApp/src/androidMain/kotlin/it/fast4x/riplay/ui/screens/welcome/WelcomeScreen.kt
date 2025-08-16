package it.fast4x.riplay.ui.screens.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.SimpleScaffold
import it.fast4x.riplay.ui.components.Skeleton

@Composable
fun WelcomeScreen(
    navController: NavController,
) {

    var tabIndex by remember { mutableIntStateOf(0) }

    SimpleScaffold (
        navController,
        tabIndex,
        onTabChanged = { tabIndex = it },
        navBarContent = { Item ->
            Item(0, stringResource(R.string.home), R.drawable.sparkles)
            Item(1, stringResource(R.string.songs), R.drawable.disc)
            Item(2, stringResource(R.string.artists), R.drawable.artists)
            Item(3, stringResource(R.string.albums), R.drawable.album)
            Item(4, stringResource(R.string.playlists), R.drawable.playlist)
        },
        navigationBarVertical = true
    ) { currentTabIndex ->
        when (currentTabIndex) {
            0 -> WelcomePage()
        }
    }
}