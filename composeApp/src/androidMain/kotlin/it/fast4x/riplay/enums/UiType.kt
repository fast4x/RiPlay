package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.utils.UiTypeKey
import it.fast4x.riplay.utils.rememberPreference

enum class UiType {
    RiPlay,
    ViMusic;

    companion object {

        @Composable
        fun current(): UiType = rememberPreference( UiTypeKey, RiPlay ).value
    }

    @Composable
    fun isCurrent(): Boolean = current() == this

    @Composable
    fun isNotCurrent(): Boolean = !isCurrent()
}