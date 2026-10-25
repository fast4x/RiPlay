package it.fast4x.riplay.ui.components.navigation.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.mobilePressFeedback
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch

private val tabs = listOf(
    Triple(0, R.string.mobile_nav_home, R.drawable.home),
    Triple(1, R.string.mobile_nav_search, R.drawable.search),
    Triple(2, R.string.mobile_nav_library, R.drawable.library),
)

/** Fixed Android navigation; labels can grow with the system font size. */
@Composable
fun SpotifyBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Encore.Background.copy(alpha = 0.96f), Color.Black)))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .height(IntrinsicSize.Min)
            .selectableGroup()
    ) {
        tabs.forEach { (index, labelId, icon) ->
            val label = stringResource(labelId)
            val isSelected = index == selected
            val color = if (isSelected) Color.White else Encore.TextSecondary
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 64.dp)
                    .mobilePressFeedback(interactionSource)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = { onSelect(index) }
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = typography().xxxs.semiBold.copy(color = color, fontSize = 12.sp, lineHeight = 16.sp)
                )
            }
        }
    }
}

/** Detail pages reuse the existing tab-selection and navigation callbacks. */
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

/** Secondary sections stay reachable as horizontally scrollable touch targets. */
@Composable
fun SpotifyChipTabs(tabs: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        tabs.forEach { (index, label) ->
            val isSelected = index == selected
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .mobilePressFeedback(interactionSource)
                    .clip(CircleShape)
                    .background(if (isSelected) Encore.Green else Encore.SurfaceElevated)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = { onSelect(index) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = label,
                    style = typography().xs.semiBold.copy(color = if (isSelected) Encore.OnGreen else Color.White)
                )
            }
        }
    }
}
