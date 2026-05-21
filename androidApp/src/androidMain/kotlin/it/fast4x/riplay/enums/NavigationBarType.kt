package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference


enum class NavigationBarType {
    IconAndText,
    IconOnly;

    companion object {

        @Composable
        fun current(): NavigationBarType = rememberPreference( NAVIGATION_BAR_TYPE.key, NavigationBarType.IconAndText ).value
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}