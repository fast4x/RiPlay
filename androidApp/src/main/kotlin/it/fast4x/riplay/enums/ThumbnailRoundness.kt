package it.fast4x.riplay.enums

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R

enum class ThumbnailRoundness {
    None,
    Light,
    Medium,
    Heavy;

    val displayName: String
        @Composable
        get() = when (this) {
            ThumbnailRoundness.None -> stringResource(R.string.none)
            ThumbnailRoundness.Light -> stringResource(R.string.light)
            ThumbnailRoundness.Heavy -> stringResource(R.string.heavy)
            ThumbnailRoundness.Medium -> stringResource(R.string.medium)
        }

    fun shape(): Shape {
        return when (this) {
            None -> RectangleShape
            Light -> RoundedCornerShape(4.dp)
            Medium -> RoundedCornerShape(8.dp)
            Heavy -> RoundedCornerShape(12.dp)
        }
    }

}
