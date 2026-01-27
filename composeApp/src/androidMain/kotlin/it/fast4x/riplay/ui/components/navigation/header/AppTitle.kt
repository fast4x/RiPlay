package it.fast4x.riplay.ui.components.navigation.header

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.preferences.eqEnabledKey
import it.fast4x.riplay.extensions.preferences.logDebugEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.components.themed.Button
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isDebugModeEnabled
import it.fast4x.riplay.utils.isParentalControlEnabled
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.isAtLeastAndroid7
import org.dailyislam.android.utilities.getNetworkType

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
            navController.navigate(NavRoutes.home.name)
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
                val dataTypeIcon = when (getNetworkType(context)) {
                    "WIFI" -> R.drawable.datawifi
                    "CELLULAR" -> R.drawable.datamobile
                    else -> R.drawable.alert_circle_not_filled
                }
                Image(
                    painter = painterResource(dataTypeIcon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                        .size(9.dp)
                       // .align(Alignment.TopEnd)
                )
            }

            val isEqualizerEnabled by rememberObservedPreference(eqEnabledKey, false)
            if (isEqualizerEnabled) {
                Image(
                    painter = painterResource(R.drawable.music_equalizer),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                        .size(8.dp)
                )
            }

            val isDebugModeEnabled by rememberObservedPreference(logDebugEnabledKey, false)
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