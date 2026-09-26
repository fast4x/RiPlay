package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ExoPlayerCacheLocation {
    System,
    Private;

    val displayName: String
        @Composable
        get() = when (this) {
            ExoPlayerCacheLocation.Private -> stringResource(R.string.cache_location_private)
            ExoPlayerCacheLocation.System -> stringResource(R.string.cache_location_system)
        }
}