package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.DynamicColor
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon

@Composable
fun viewTypeToolbutton(messageId: Int): MenuIcon = object : MenuIcon, DynamicColor, Descriptive {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    var viewType = appSettings.viewType
    override var isFirstColor: Boolean = true
    override var iconId: Int = if (viewType == ViewType.Grid) R.drawable.list_view else R.drawable.grid_view
    override val messageId: Int = messageId
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)

    override fun onShortClick() {
        viewType = when (viewType) {
            ViewType.Grid -> ViewType.List
            ViewType.List -> ViewType.Grid
        }
    }
}