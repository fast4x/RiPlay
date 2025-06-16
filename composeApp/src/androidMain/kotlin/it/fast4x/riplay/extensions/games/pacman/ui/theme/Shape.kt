package it.fast4x.riplay.extensions.games.pacman.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

@Composable
fun MyStarTrek() {
    val myStarTrekShape = GenericShape { size, _ ->
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height)
        quadraticBezierTo(
            size.width * 0.6f,
            size.height * 0.4f,
            0f,
            size.height
        )
        close()
    }
    Surface(
        shape = myStarTrekShape,
        color = Color.Yellow,
        border = BorderStroke(3.dp, Color.Black),
        modifier = Modifier.size(100.dp)
    ) { }
}