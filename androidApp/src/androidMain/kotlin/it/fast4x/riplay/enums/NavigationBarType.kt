package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager


enum class NavigationBarType {
    IconAndText,
    IconOnly;

    companion object {

        @Composable
        fun current(): NavigationBarType {
            //rememberPreference(NAVIGATION_BAR_TYPE.key, NavigationBarType.IconAndText).value
            val appSettingsManager = LocalAppSettingsManager.current
            val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
            return appSettings.navigationBarType
        }
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}