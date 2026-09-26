package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R


enum class NavigationBarType {
    IconAndText,
    IconOnly;

    val displayName: String
        @Composable
        get() =  when (this) {
            NavigationBarType.IconAndText -> stringResource(R.string.icon_and_text)
            NavigationBarType.IconOnly -> stringResource(R.string.only_icon)
        }

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