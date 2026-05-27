package it.fast4x.riplay.ui.components.navigation.header

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.appviewmodel.LocalAppViewModel
import it.fast4x.riplay.extensions.appviewmodel.toIcon
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EQ_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LOG_DEBUG_ENABLED
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.components.themed.Button
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isParentalControlEnabled
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.isAtLeastAndroid7

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppLogo(
    navController: NavController,
    context: Context
) {
    val modifier = Modifier.combinedClickable(
        onClick = {},
        onLongClick = {}
    )

    Button(
        iconId = R.drawable.app_icon,
        color = colorPalette().favoritesIcon,
        padding = 0.dp,
        size = 36.dp,
        modifier = modifier
    ).Draw()
}

@Composable
private fun AppLogoText( navController: NavController ) {
    val iconTextClick: () -> Unit = {
        if ( NavRoutes.home.isNotHere( navController ) )
            navController.popBackStack(route = NavRoutes.home.name, inclusive = false)
    }


    Text(
        text = "Play",
        style = typography().xxl.copy(
            color = colorPalette().text
        ),
        modifier = Modifier.clickable {
            iconTextClick()
        }
    )

//    Button(
//        iconId = R.drawable.app_logo_text,
//        color = AppBar.contentColor(),
//        padding = 0.dp,
//        size = 36.dp,
//        forceWidth = 100.dp,
//        modifier = Modifier.clickable { iconTextClick() }
//    ).Draw()
}


@Composable
fun AppTitle(
    navController: NavController,
    context: Context
) {
    val appViewModel = LocalAppViewModel.current
    val networkState by appViewModel.networkState.collectAsStateWithLifecycle()
    var offlineModeEnabled by rememberPreference(PreferenceKey.OFFLINE_MODE_ENABLED.key, false)

    Row(
        horizontalArrangement = Arrangement.spacedBy( 5.dp ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            AppLogo(navController, context)
        }
        Column {
            AppLogoText(navController)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {

            if (isAtLeastAndroid7) {
                Image(
                    painter = if (offlineModeEnabled) painterResource(R.drawable.airplane)
                        else painterResource(networkState.networkType?.toIcon() ?: R.drawable.alert_circle_not_filled),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                    .size(9.dp)
                // .align(Alignment.TopEnd)
                )
            }



            val isEqualizerEnabled by rememberPreference(EQ_ENABLED.key, false)
            if (isEqualizerEnabled) {
                Image(
                    painter = painterResource(R.drawable.music_equalizer),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                        .size(8.dp)
                )
            }

            val isDebugModeEnabled by rememberPreference(LOG_DEBUG_ENABLED.key, false)
            if (isDebugModeEnabled)
                Image(
                    painter = painterResource(R.drawable.maintenance),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().red),
                    modifier = Modifier
                        .size(8.dp)
                       // .align(Alignment.BottomEnd)
                )
        }

        if(isParentalControlEnabled())
            Button(
                iconId = R.drawable.shield_checkmark,
                color = AppBar.contentColor(),
                padding = 0.dp,
                size = 20.dp
            ).Draw()
    }

}