package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.DynamicColor
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.launch

@Composable
fun viewTypeToolbutton(messageId: Int): MenuIcon = object : MenuIcon, DynamicColor, Descriptive {
    val scope = rememberCoroutineScope()
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val viewType = appSettings.viewType

    override var isFirstColor: Boolean = true
    override var iconId: Int = if (viewType == ViewType.Grid) R.drawable.list_view else R.drawable.grid_view
    override val messageId: Int = messageId
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)

    override fun onShortClick() {
        scope.launch {
            appSettingsManager.updateSettings(
                appSettings.copy(
                    viewType = if (viewType == ViewType.Grid) ViewType.List else ViewType.Grid
                )
            )
        }
    }
}