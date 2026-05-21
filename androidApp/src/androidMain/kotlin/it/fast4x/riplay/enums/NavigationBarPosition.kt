package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_POSITION
import it.fast4x.riplay.extensions.preferences.rememberPreference

enum class NavigationBarPosition {
    Left,
    Right,
    Top,
    Bottom;

    companion object {

        @Composable
        fun current() = rememberPreference( NAVIGATION_BAR_POSITION.key, Bottom ).value
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}