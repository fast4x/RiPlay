package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class MusicIdentifierProvider {
    AudioTagInfo;

    val title: String
        @Composable
        get() = when(this) {
            AudioTagInfo -> stringResource(R.string.mi_name_audiotag)
        }

    val subtitle: String
        @Composable
        get() = when(this) {
            AudioTagInfo -> stringResource(R.string.get_your_api_key)
        }


    val website: String
        get() = when(this) {
            AudioTagInfo -> "https://audiotag.info/apisection"
        }

    val info: String
        @Composable
        get() = when(this) {
            AudioTagInfo -> stringResource(R.string.mi_info_audiotag_recognizes_music_using_its_own_proprietary_patented_acoustic_fingerprinting_technology)
        }

}