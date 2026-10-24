package it.fast4x.riplay.ui.components.navigation.header

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import java.util.Calendar

/**
 * Cabecera estilo Spotify: plana, sin logo.
 *  - En Inicio: saludo según la hora ("Good afternoon") + iconos blancos (historial, ajustes).
 *  - En el resto: sólo la flecha atrás (cada pantalla dibuja su propio título).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ModernTopAppBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    backButton: @Composable () -> Unit = {},
    context: Context,
) {
    val appSettings = LocalAppSettingsManager.current.activeSettings.collectAsStateWithLifecycle().value

    val insetsPadding = if (appSettings.isEnabledFullScreen)
        TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal).asPaddingValues()
    else TopAppBarDefaults.windowInsets.asPaddingValues()

    val isHome = NavRoutes.home.isHere(navController)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colorPalette().background0)
            .padding(insetsPadding)
            .height(56.dp)
            .padding(horizontal = if (isHome) 16.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isHome) {
            BasicText(
                text = greeting(),
                style = typography().xxl.bold.copy(color = colorPalette().text),
                modifier = Modifier.weight(1f)
            )
            HeaderIcon(R.drawable.history) { navController.navigate(NavRoutes.history.name) }
            Spacer(Modifier.width(8.dp))
            HeaderIcon(R.drawable.settings) { navController.navigate(NavRoutes.settings.name) }
        } else {
            backButton()
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderIcon(iconId: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette().text),
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..18 -> "Good afternoon"
    else -> "Good evening"
}
