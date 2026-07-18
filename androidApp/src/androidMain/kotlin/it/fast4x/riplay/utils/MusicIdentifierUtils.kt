package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.extensions.audiotag.AudioTagger

@Composable
fun MusicIdentifier(navController: NavController) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val musicIdentifierProvider = appSettings.musicIdentifierProvider

    when (musicIdentifierProvider) {
        MusicIdentifierProvider.AudioTagInfo -> {
            val audioTagger = LocalAudioTagger.current
            AudioTagger(audioTagger, navController)
        }
    }
}