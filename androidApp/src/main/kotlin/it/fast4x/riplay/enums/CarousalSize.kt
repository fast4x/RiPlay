package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class CarouselSize {
    Small,
    Medium,
    Big,
    Biggest,
    Expanded;

    val displayName: String
        @Composable
        get() = when (this) {
            CarouselSize.Small -> stringResource(R.string.small)
            CarouselSize.Medium -> stringResource(R.string.medium)
            CarouselSize.Big -> stringResource(R.string.big)
            CarouselSize.Biggest -> stringResource(R.string.biggest)
            CarouselSize.Expanded -> stringResource(R.string.expanded)
        }

    val size: Int
        get() = when (this) {
            Small -> 90
            Medium -> 55
            Big -> 30
            Biggest -> 20
            Expanded -> 0
        }

}
