package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R

enum class NavigationBarPosition {
    Left,
    Right,
    Top,
    Bottom;

    val displayName: String
        @Composable
        get() = when (this) {
            NavigationBarPosition.Left -> stringResource(R.string.direction_left)
            NavigationBarPosition.Right -> stringResource(R.string.direction_right)
            NavigationBarPosition.Top -> stringResource(R.string.direction_top)
            NavigationBarPosition.Bottom -> stringResource(R.string.direction_bottom)
        }

    companion object {

        @Composable
        fun current(): NavigationBarPosition {
            //rememberPreference(NAVIGATION_BAR_POSITION.key, Bottom).value
            val appSettingsManager = LocalAppSettingsManager.current
            val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
            return appSettings.navigationBarPosition
        }
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}