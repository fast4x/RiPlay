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
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.equalizer.EqualizerScreen
import it.fast4x.riplay.extensions.pip.isPipSupported
import it.fast4x.riplay.extensions.pip.rememberPipHandler
import it.fast4x.riplay.extensions.preferences.castToRiTuneDeviceEnabledKey
import it.fast4x.riplay.extensions.preferences.enableMusicIdentifierKey
import it.fast4x.riplay.extensions.preferences.enablePictureInPictureKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.ritune.improved.RiTuneSelector
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SheetBody
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.themed.DropdownMenu
import it.fast4x.riplay.ui.screens.events.EventsScreen
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.MusicIdentifier
import it.fast4x.riplay.utils.ytAccountThumbnail
import timber.log.Timber

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

//    menu.add(
//        DropdownMenu.Item(
//            R.drawable.cast_connected,
//            R.string.blacklist,
//        ) { onItemClick( NavRoutes.ritunecontroller ) }
//    )

    val sheet = LocalGlobalSheetState.current

    // Equalizer button
    LocalPlayerServiceBinder.current?.equalizer?.let {
        menu.add(
            DropdownMenu.Item(
                R.drawable.music_equalizer,
                R.string.equalizer,
            ) {
                sheet.display {
                    SheetBody {
                        EqualizerScreen(it)
                    }
                }
                onDismissRequest()
            }
        )
    }



    // Events button
    menu.add(
        DropdownMenu.Item(
            R.drawable.alarm,
            R.string.events,
        ) {
            sheet.display {
                SheetBody {
                    EventsScreen()
                }
            }
        }
    )

    // Blacklist button
    menu.add(
        DropdownMenu.Item(
            R.drawable.alert_circle,
            R.string.blacklist,
        ) { onItemClick( NavRoutes.blacklist ) }
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
    // Listener levels button
    menu.add(
        DropdownMenu.Item(
            R.drawable.trophy,
            R.string.listener_levels
        ) { onItemClick( NavRoutes.listenerLevel ) }
    )
    // Rewinds button
    menu.add(
        DropdownMenu.Item(
            R.drawable.stat_year,
            R.string.rewinds,
        ) { onItemClick( NavRoutes.rewind ) }
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
    val sheet = LocalGlobalSheetState.current

    var castToRiTuneDeviceEnabled by rememberPreference(castToRiTuneDeviceEnabledKey, false )
    var showRiTuneSelector by remember { mutableStateOf(false) }

    if (showRiTuneSelector) {
        RiTuneSelector(
            onDismiss = {
                showRiTuneSelector = false
            },
            onSelect = {
                Timber.d("RiTuneSelector: $it")
                //castToRiTuneDeviceActive = it.any { device -> device.selected }
            }
        )
    }

    /* todo maybe nor right place
    val equalizer = LocalPlayerServiceBinder.current?.equalizer
    equalizer?.let {
        HeaderIcon(R.drawable.equalizer) {
            sheet.display {
                SheetBody {
                    EqualizerScreen(it)
                }
            }
        }
    }
     */

    // todo cast to complete
//    if (castToRiTuneDeviceEnabled)
//        HeaderIcon(if (GlobalSharedData.riTuneCastActive) R.drawable.cast_connected else R.drawable.cast_disconnected) {
//            showRiTuneSelector = true
//            //navController.navigate(NavRoutes.ritunecontroller.name)
//        }

    val isEnabledMusicIdentifier by rememberPreference(
        enableMusicIdentifierKey,
        false
    )
    if (isEnabledMusicIdentifier) {
        HeaderIcon(R.drawable.soundwave) {
            sheet.display {
                SheetBody {
                    MusicIdentifier(navController)
                }
            }
        }
    }

    // Search Icon
    HeaderIcon( R.drawable.search) { navController.navigate(NavRoutes.search.name) }

    if (isYtLoggedIn()) {
        if (ytAccountThumbnail() != "")
            AsyncImage(
                model = ytAccountThumbnail(),
                contentDescription = null,
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = 10.dp)
                    .clip(thumbnailShape())
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