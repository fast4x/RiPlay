package it.fast4x.riplay.ui.components.navigation.header

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.mobilePressFeedback
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
    val isHome = NavRoutes.home.isHere(navController)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colorPalette().background0)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = 56.dp)
            .padding(horizontal = if (isHome) 16.dp else 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isHome) {
            BasicText(
                text = greeting(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = typography().xxl.bold.copy(color = colorPalette().text),
                modifier = Modifier.weight(1f)
            )
            HeaderIcon(R.drawable.history, stringResource(R.string.history)) { navController.navigate(NavRoutes.history.name) }
            Spacer(Modifier.width(4.dp))
            HeaderIcon(R.drawable.settings, stringResource(R.string.settings)) { navController.navigate(NavRoutes.settings.name) }
        } else {
            backButton()
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderIcon(iconId: Int, description: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .mobilePressFeedback(interactionSource)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = description,
            colorFilter = ColorFilter.tint(colorPalette().text),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun greeting(): String = stringResource(when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> R.string.mobile_nav_good_morning
    in 12..18 -> R.string.mobile_nav_good_afternoon
    else -> R.string.mobile_nav_good_evening
})
