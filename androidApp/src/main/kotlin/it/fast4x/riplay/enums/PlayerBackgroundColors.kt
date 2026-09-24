package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PlayerBackgroundColors {
    CoverColorGradient,
    ThemeColorGradient,
    CoverColor,
    BlurredCoverColor,
    ThemeColor,
    ColorPalette,
    AnimatedGradient,
    MidnightOdyssey,
    BlurredCoverMix;
    
    
    val textName: String
        @Composable
        get() = when (this) {
            CoverColor -> stringResource(R.string.bg_colors_background_from_cover)
            ThemeColor -> stringResource(R.string.bg_colors_background_from_theme)
            CoverColorGradient -> stringResource(R.string.bg_colors_gradient_background_from_cover)
            ThemeColorGradient -> stringResource(R.string.bg_colors_gradient_background_from_theme)
            BlurredCoverColor -> stringResource(R.string.bg_colors_blurred_cover_background)
            ColorPalette -> stringResource(R.string.colorpalette)
            AnimatedGradient -> stringResource(R.string.animatedgradient)
            MidnightOdyssey -> stringResource(R.string.midnightodyssey)
            BlurredCoverMix -> stringResource(R.string.bg_colors_blurred_cover_mix)
        }
    
}