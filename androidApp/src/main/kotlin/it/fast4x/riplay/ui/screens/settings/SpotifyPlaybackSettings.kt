package it.fast4x.riplay.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.enums.CrossfadeDuration
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch

/** Sección "Reproducción" con sólo las opciones que tiene Spotify. */
@Composable
fun SpotifyPlaybackSettings() {
    val manager = LocalAppSettingsManager.current
    val settings = manager.activeSettings.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    fun update(block: (AppSettings) -> AppSettings) =
        scope.launch { manager.updateSettings(block(manager.activeSettings.value)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        BasicText(
            text = "Playback",
            style = typography().l.bold.copy(color = colorPalette().text),
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
        )

        EnumValueSelectorSettingsEntry(
            title = "Crossfade",
            text = "Lets songs fade into each other",
            selectedValue = settings.crossfadeDuration,
            onValueSelected = { v -> update { it.copy(crossfadeDuration = v) } },
            valueText = { it.title }
        )

        SwitchSettingEntry(
            title = "Autoplay",
            text = "Enjoy nonstop listening. When your audio ends, we'll play you something similar.",
            isChecked = settings.autoLoadSongsInQueue,
            onCheckedChange = { v -> update { it.copy(autoLoadSongsInQueue = v) } }
        )

        SwitchSettingEntry(
            title = "Show artwork on lock screen",
            text = "Display the cover of the current song on your lock screen",
            isChecked = settings.isShowingThumbnailInLockscreen,
            onCheckedChange = { v -> update { it.copy(isShowingThumbnailInLockscreen = v) } }
        )

        Spacer(Modifier.height(Dimensions.bottomSpacer))
    }
}
