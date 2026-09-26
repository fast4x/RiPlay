package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PlayerThumbnailSize {
    Small,
    Medium,
    Big,
    Biggest,
    Expanded;

    val displayName: String
        @Composable
        get() = when (this) {
            PlayerThumbnailSize.Small -> stringResource(R.string.small)
            PlayerThumbnailSize.Medium -> stringResource(R.string.medium)
            PlayerThumbnailSize.Big -> stringResource(R.string.big)
            PlayerThumbnailSize.Biggest -> stringResource(R.string.biggest)
            PlayerThumbnailSize.Expanded -> stringResource(R.string.expanded)
        }

    val padding: Int
        get() = when (this) {
            Small -> 90
            Medium -> 55
            Big -> 30
            Biggest -> 20
            Expanded -> 0
        }

    val height: Int
        get() = when (this) {
            Small -> 300
            Medium -> 500
            Big -> 700
            Biggest -> 900
            Expanded -> 0
        }

}
