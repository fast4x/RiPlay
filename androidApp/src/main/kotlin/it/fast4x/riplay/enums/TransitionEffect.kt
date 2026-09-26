package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class TransitionEffect {
    SlideVertical,
    SlideHorizontal,
    Scale,
    Fade,
    Expand,
    None;

    val displayName: String
        @Composable
        get() = when (this) {
            TransitionEffect.None -> stringResource(R.string.none)
            TransitionEffect.Expand -> stringResource(R.string.te_expand)
            TransitionEffect.Fade -> stringResource(R.string.te_fade)
            TransitionEffect.Scale -> stringResource(R.string.te_scale)
            TransitionEffect.SlideVertical -> stringResource(R.string.te_slide_vertical)
            TransitionEffect.SlideHorizontal -> stringResource(R.string.te_slide_horizontal)
        }
}