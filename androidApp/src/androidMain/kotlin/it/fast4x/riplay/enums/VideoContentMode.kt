package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class VideoContentMode {
    Normal, // Riproduce tutto e mostra i video in modalità normale
    AudioOnly, // Riproduce il video come fosse un audio senza mostrarlo
    Excluded; // Esclude i video dall'inserimento in coda e quindi dalla riproduzione generale

    val textName: String
        @Composable
        get() = when( this ) {
            AudioOnly -> stringResource(R.string.video_content_mode_audio_only)
            Excluded -> stringResource(R.string.video_content_mode_excluded)
            Normal -> stringResource(R.string.video_content_mode_normal)
        }

    val excluded: Boolean
        get() = this == Excluded

    val normal: Boolean
        get() = this == Normal

    val audioOnly: Boolean
        get() = this == AudioOnly

}