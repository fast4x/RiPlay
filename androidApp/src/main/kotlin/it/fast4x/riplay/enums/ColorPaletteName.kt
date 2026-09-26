package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ColorPaletteName {
    Default,
    Dynamic,
    PureBlack,
    ModernBlack,
    MaterialYou,
    Customized,
    CustomColor;

    val displayName: String
        @Composable
        get() = when (this) {
            ColorPaletteName.Default -> stringResource(R.string._default)
            ColorPaletteName.Dynamic -> stringResource(R.string.dynamic)
            ColorPaletteName.PureBlack -> stringResource(R.string.theme_pure_black)
            ColorPaletteName.ModernBlack -> stringResource(R.string.theme_modern_black)
            ColorPaletteName.MaterialYou -> stringResource(R.string.theme_material_you)
            ColorPaletteName.Customized -> stringResource(R.string.theme_customized)
            ColorPaletteName.CustomColor -> stringResource(R.string.customcolor)
        }
}
