package it.fast4x.riplay.ui.components.navigation.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch

private val tabs = listOf(
    Triple(0, "Home", R.drawable.home),
    Triple(1, "Search", R.drawable.search),
    Triple(2, "Your Library", R.drawable.library),
)

/** Barra inferior de Spotify: 3 pestañas, degradado transparente → negro. */
@Composable
fun SpotifyBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Encore.Background.copy(alpha = 0.92f), Color.Black))
            )
            .navigationBarsPadding()
            .height(60.dp)
    ) {
        tabs.forEach { (index, label, icon) ->
            val color = if (index == selected) Color.White else Encore.TextSecondary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = label,
                    style = typography().xxxs.semiBold.copy(color = color)
                )
            }
        }
    }
}

/** Barra inferior para pantallas internas (álbum, artista, búsqueda…): vuelve a Inicio en la pestaña elegida. */
@Composable
fun SpotifyGlobalBottomBar(navController: NavController) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    SpotifyBottomBar(selected = appSettings.homeScreenTabIndex.coerceIn(0, 2)) { index ->
        scope.launch {
            appSettingsManager.updateSettings(
                appSettingsManager.activeSettings.value.copy(homeScreenTabIndex = index)
            )
        }
        navController.navigate(NavRoutes.home.name) {
            popUpTo(NavRoutes.home.name) { inclusive = false }
            launchSingleTop = true
        }
    }
}

/** Pestañas internas de una pantalla convertidas en píldoras arriba (como los filtros de Spotify). */
@Composable
fun SpotifyChipTabs(tabs: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        tabs.forEach { (index, label) ->
            val isSel = index == selected
            BasicText(
                text = label,
                style = typography().xs.semiBold.copy(color = if (isSel) Encore.OnGreen else Color.White),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSel) Encore.Green else Encore.SurfaceElevated)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}
