package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager

enum class NavigationBarPosition {
    Left,
    Right,
    Top,
    Bottom;

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