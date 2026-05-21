package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.extensions.audiotag.AudioTagger
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_IDENTIFIER_PROVIDER
import it.fast4x.riplay.extensions.preferences.rememberPreference

@Composable
fun MusicIdentifier(navController: NavController) {
    val musicIdentifierProvider by rememberPreference(MUSIC_IDENTIFIER_PROVIDER.key,
        MusicIdentifierProvider.AudioTagInfo)

    when (musicIdentifierProvider) {
        MusicIdentifierProvider.AudioTagInfo -> {
            val audioTagger = LocalAudioTagger.current
            AudioTagger(audioTagger, navController)
        }
    }
}