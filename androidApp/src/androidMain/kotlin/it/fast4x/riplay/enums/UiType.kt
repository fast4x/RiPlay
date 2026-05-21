package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.extensions.preferences.PreferenceKey.UI_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference

enum class UiType {
    RiPlay,
    ViMusic;

    companion object {

        @Composable
        fun current(): UiType = rememberPreference( UI_TYPE.key, RiPlay ).value
    }

    @Composable
    fun isCurrent(): Boolean = current() == this

    @Composable
    fun isNotCurrent(): Boolean = !isCurrent()
}