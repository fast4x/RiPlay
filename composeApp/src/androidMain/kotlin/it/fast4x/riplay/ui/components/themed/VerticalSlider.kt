package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.SeekBar
import it.fast4x.riplay.utils.colorPalette

@Composable
fun VerticalSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
) {

    val rangeMultiplier = 1000f

    val initialLongValue = (value * rangeMultiplier).toLong()

    var scrubbingPosition by remember { mutableLongStateOf(initialLongValue) }

    SeekBar(
        value = initialLongValue,
        minimumValue = 0,
        maximumValue = rangeMultiplier.toLong(),
        onDragStart = {
            scrubbingPosition = it
        },
        onDrag = { delta ->
            scrubbingPosition = scrubbingPosition.plus(delta).coerceIn(0, rangeMultiplier.toLong())

            val floatResult = scrubbingPosition / rangeMultiplier
            onValueChange(floatResult)
        },
        onDragEnd = {
            scrubbingPosition = 0
        },
        color = colorPalette().accent,
        backgroundColor = colorPalette().background1,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
}