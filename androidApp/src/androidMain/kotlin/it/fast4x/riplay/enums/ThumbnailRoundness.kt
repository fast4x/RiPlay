package it.fast4x.riplay.enums

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class ThumbnailRoundness {
    None,
    Light,
    Medium,
    Heavy;

    fun shape(): Shape {
        return when (this) {
            None -> RectangleShape
            Light -> RoundedCornerShape(4.dp)
            Medium -> RoundedCornerShape(8.dp)
            Heavy -> RoundedCornerShape(12.dp)
        }
    }

}
