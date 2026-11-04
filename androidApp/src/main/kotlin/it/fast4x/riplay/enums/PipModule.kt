package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PipModule {
    Cover, Lyrics;

    val displayName: String
        @Composable
        get() = when (this) {
            PipModule.Cover -> stringResource(R.string.pipmodule_cover)
            PipModule.Lyrics -> stringResource(R.string.pipmodule_lyrics)
        }
}