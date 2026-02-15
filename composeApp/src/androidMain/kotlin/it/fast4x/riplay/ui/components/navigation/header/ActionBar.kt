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
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.EqualizerType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.equalizer.InternalEqualizerScreen
import it.fast4x.riplay.extensions.equalizer.rememberSystemEqualizerLauncher
import it.fast4x.riplay.extensions.pip.isPipSupported
import it.fast4x.riplay.extensions.pip.rememberPipHandler
import it.fast4x.riplay.extensions.preferences.castToRiTuneDeviceEnabledKey
import it.fast4x.riplay.extensions.preferences.enableMusicIdentifierKey
import it.fast4x.riplay.extensions.preferences.enablePictureInPictureKey
import it.fast4x.riplay.extensions.preferences.equalizerTypeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.ritune.improved.RiTuneSelector
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SheetBody
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.screens.events.EventsScreen
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.utils.MusicIdentifier
import it.fast4x.riplay.utils.ytAccountThumbnail
import timber.log.Timber
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import it.fast4x.riplay.utils.typography

@Composable
private fun HamburgerMenu(
    expanded: Boolean,
    onItemClick: (NavRoutes) -> Unit,
    onDismissRequest: () -> Unit
) {

    val enablePictureInPicture by rememberPreference(enablePictureInPictureKey, false)
    val pipHandler = rememberPipHandler()
    val sheet = LocalGlobalSheetState.current
    val equalizerType by rememberObservedPreference(equalizerTypeKey, EqualizerType.Internal)
    val internalEqualizer = LocalPlayerServiceBinder.current?.equalizer
    val launchSystemEqualizer by rememberSystemEqualizerLauncher(audioSessionId = {0})

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        containerColor = Color.Transparent,
        modifier = Modifier
            .width(280.dp)
            .padding(top = 8.dp)
    ) {

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    color = colorPalette().background1.copy(alpha = 0.90f),
                )
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {


                ModernMenuItem(
                    index = 0,
                    iconRes = R.drawable.music_equalizer,
                    textRes = R.string.equalizer,
                    onClick = {
                        if (equalizerType == EqualizerType.Internal) {
                            internalEqualizer?.let {
                                sheet.display {
                                    SheetBody { InternalEqualizerScreen(it) }
                                }
                            }
                        } else {
                            launchSystemEqualizer()
                        }
                        onDismissRequest()
                    }
                )


                ModernMenuItem(
                    index = 1,
                    iconRes = R.drawable.alarm,
                    textRes = R.string.events,
                    onClick = {
                        sheet.display { SheetBody { EventsScreen() } }
                    }
                )


                ModernMenuItem(
                    index = 2,
                    iconRes = R.drawable.alert_circle,
                    textRes = R.string.blacklist,
                    onClick = { onItemClick(NavRoutes.blacklist) }
                )


                ModernMenuItem(
                    index = 3,
                    iconRes = R.drawable.history,
                    textRes = R.string.history,
                    onClick = { onItemClick(NavRoutes.history) }
                )


                ModernMenuItem(
                    index = 4,
                    iconRes = R.drawable.stats_chart,
                    textRes = R.string.statistics,
                    onClick = { onItemClick(NavRoutes.statistics) }
                )


                ModernMenuItem(
                    index = 5,
                    iconRes = R.drawable.trophy,
                    textRes = R.string.listener_levels,
                    onClick = { onItemClick(NavRoutes.listenerLevel) }
                )


                ModernMenuItem(
                    index = 6,
                    iconRes = R.drawable.stat_year,
                    textRes = R.string.rewinds,
                    onClick = { onItemClick(NavRoutes.rewind) }
                )


                if (isPipSupported && enablePictureInPicture) {
                    ModernMenuItem(
                        index = 7,
                        iconRes = R.drawable.picture,
                        textRes = R.string.menu_go_to_picture_in_picture,
                        onClick = { pipHandler.enterPictureInPictureMode() }
                    )
                }


                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    color = colorPalette().accent.copy(alpha = 0.7f)
                )


                ModernMenuItem(
                    index = 8,
                    iconRes = R.drawable.settings,
                    textRes = R.string.settings,
                    onClick = { onItemClick(NavRoutes.settings) },
                    isLast = true
                )
            }
        }
    }
}


@Composable
private fun ModernMenuItem(
    index: Int,
    @androidx.annotation.DrawableRes iconRes: Int,
    @androidx.annotation.StringRes textRes: Int,
    onClick: () -> Unit,
    isLast: Boolean = false
) {

    var isVisible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 30, // Ritardo cascata (30ms per voce)
            easing = FastOutSlowInEasing
        ), label = "alpha"
    )

    val offsetX by animateIntAsState(
        targetValue = if (isVisible) 0 else -20,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 30
        ), label = "offsetX"
    )

    LaunchedEffect(Unit) { isVisible = true }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 4.dp) // Spaziatura tra le voci
            .offset { IntOffset(offsetX, 0) }
            .alpha(alpha),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, // Sfondo trasparente per usare il nostro
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp), // Bottoni con angoli smussati
        elevation = null, // Niente ombra standard
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {

            Surface(
                shape = CircleShape,
                color = colorPalette().accent.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(id = textRes),
                style = typography().s,
                color = colorPalette().text
            )
        }
    }
}

/*
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

    val equalizerType by rememberObservedPreference(equalizerTypeKey, EqualizerType.Internal)
    val internalEqualizer = LocalPlayerServiceBinder.current?.equalizer
    val launchSystemEqualizer by rememberSystemEqualizerLauncher(audioSessionId = {0})
    menu.add(
            DropdownMenu.Item(
                R.drawable.music_equalizer,
                R.string.equalizer,
            ) {
                if (equalizerType == EqualizerType.Internal) {
                    internalEqualizer?.let {
                        sheet.display {
                            SheetBody {
                                InternalEqualizerScreen(it)
                            }
                        }
                    }
                } else {
                    launchSystemEqualizer()
                }
                onDismissRequest()
            }
    )



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

 */

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
    HeaderIcon( R.drawable.search, tint = colorPalette().accent) {
        navController.navigate(NavRoutes.search.name)
    }

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
        else HeaderIcon( R.drawable.internet, tint = colorPalette().accent, size = 30.dp ) { expanded = !expanded }
    } else HeaderIcon( R.drawable.burger, tint = colorPalette().accent ) { expanded = !expanded }

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