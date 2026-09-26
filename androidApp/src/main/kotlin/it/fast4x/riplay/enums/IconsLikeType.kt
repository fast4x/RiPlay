package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class IconLikeType {
    Apple,
    Breaked,
    Brilliant,
    Essential,
    Gift,
    Shape,
    Striped;

    val displayName: String
        @Composable
        get() = when (this) {
            IconLikeType.Essential -> stringResource(R.string.pcontrols_essential)
            IconLikeType.Apple -> stringResource(R.string.icon_like_apple)
            IconLikeType.Breaked -> stringResource(R.string.icon_like_breaked)
            IconLikeType.Gift -> stringResource(R.string.icon_like_gift)
            IconLikeType.Shape -> stringResource(R.string.icon_like_shape)
            IconLikeType.Striped -> stringResource(R.string.icon_like_striped)
            IconLikeType.Brilliant -> stringResource(R.string.icon_like_brilliant)
        }
}