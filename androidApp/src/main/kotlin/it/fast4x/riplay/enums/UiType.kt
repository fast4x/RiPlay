package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager

enum class UiType {
    RiPlay,
    ViMusic;

    companion object {

        @Composable
        fun current(): UiType {
            //rememberPreference(UI_TYPE.key, RiPlay).value
            val appSettingsManager = LocalAppSettingsManager.current
            val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
            return appSettings.uiType
        }
    }

    @Composable
    fun isCurrent(): Boolean = current() == this

    @Composable
    fun isNotCurrent(): Boolean = !isCurrent()
}