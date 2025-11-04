package it.fast4x.riplay.ui.components.navigation.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.audiotag.AudioTagger
import it.fast4x.riplay.extensions.pip.isPipSupported
import it.fast4x.riplay.extensions.pip.rememberPipHandler
import it.fast4x.riplay.extensions.preferences.enablePictureInPictureKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.themed.DropdownMenu
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.utils.ytAccountThumbnail

@Composable
private fun HamburgerMenu(
    expanded: Boolean,
    onItemClick: (NavRoutes) -> Unit,
    onDismissRequest: () -> Unit
) {
    val enablePictureInPicture by rememberPreference(enablePictureInPictureKey, false)
    val pipHandler = rememberPipHandler()

    val menu = DropdownMenu(
        expanded = expanded,
        modifier = Modifier.background( colorPalette().background0.copy(0.90f) ),
        onDismissRequest = onDismissRequest
    )
    // History button
    menu.add(
        DropdownMenu.Item(
            R.drawable.history,
            R.string.history
        ) { onItemClick( NavRoutes.history ) }
    )
    // Statistics button
    menu.add(
        DropdownMenu.Item(
            R.drawable.stats_chart,
            R.string.statistics
        ) { onItemClick( NavRoutes.statistics ) }
    )
    // Picture in picture button
    if (isPipSupported && enablePictureInPicture)
        menu.add(
            DropdownMenu.Item(
                R.drawable.picture,
                R.string.menu_go_to_picture_in_picture
            ) { pipHandler.enterPictureInPictureMode() }
        )
    menu.add { HorizontalDivider() }
    // Settings button
    menu.add(
        DropdownMenu.Item(
            R.drawable.settings,
            R.string.settings
        ) { onItemClick( NavRoutes.settings ) }
    )

    menu.Draw()
}

// START
@Composable
fun ActionBar(
    navController: NavController,
) {
    var expanded by remember { mutableStateOf(false) }

    // todo add audio tagger
    val audioTagger = LocalAudioTagger.current
    val menuState = LocalMenuState.current

    HeaderIcon( R.drawable.mic) {
        menuState.display {
            Menu {
                AudioTagger(audioTagger, navController)
            }
        }
    }

    // Search Icon
    HeaderIcon( R.drawable.search) { navController.navigate(NavRoutes.search.name) }

    if (isLoggedIn()) {
        if (ytAccountThumbnail() != "")
            AsyncImage(
                model = ytAccountThumbnail(),
                contentDescription = null,
                modifier = Modifier.height(40.dp)
                    .padding(end = 10.dp)
                    .clip( thumbnailShape() )
                    .clickable { expanded = !expanded }
            )
        else HeaderIcon( R.drawable.internet, size = 30.dp ) { expanded = !expanded }
    } else HeaderIcon( R.drawable.burger ) { expanded = !expanded }

    // Define actions for when item inside menu clicked,
    // and when user clicks on places other than the menu (dismiss)
    val onItemClick: (NavRoutes) -> Unit = {
        expanded = false
        navController.navigate(it.name)
    }
    val onDismissRequest: () -> Unit = { expanded = false }

    // Hamburger menu
    HamburgerMenu(
        expanded = expanded,
        onItemClick = onItemClick,
        onDismissRequest = onDismissRequest
    )
// END
}