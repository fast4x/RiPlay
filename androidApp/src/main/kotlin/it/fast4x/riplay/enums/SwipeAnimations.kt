package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class SwipeAnimationNoThumbnail {
    Sliding,
    Fade,
    Scale,
    Carousel,
    Circle;

    val displayName: String
        @Composable
        get() = when (this) {
            SwipeAnimationNoThumbnail.Sliding -> stringResource(R.string.te_slide_vertical)
            SwipeAnimationNoThumbnail.Fade -> stringResource(R.string.te_fade)
            SwipeAnimationNoThumbnail.Scale -> stringResource(R.string.te_scale)
            SwipeAnimationNoThumbnail.Carousel -> stringResource(R.string.carousel)
            SwipeAnimationNoThumbnail.Circle -> stringResource(R.string.vt_circular)
        }
}