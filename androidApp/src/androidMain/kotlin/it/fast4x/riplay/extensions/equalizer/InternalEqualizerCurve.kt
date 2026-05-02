package it.fast4x.riplay.extensions.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

@Composable
fun InternalEqualizerCurve(
    bandLevels: Map<Short, Float>,
    modifier: Modifier = Modifier
) {

    val sortedValues = if (bandLevels.isNotEmpty()) {
        bandLevels.keys.sorted().map { bandLevels[it] ?: 0.5f }
    } else {
        listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    }
    val colorPalette = colorPalette()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (sortedValues.isEmpty()) return@Canvas

        val padding = 8.dp.toPx()
        val availableWidth = canvasWidth - (padding * 2)
        val availableHeight = canvasHeight - (padding * 2)

        val stepX = if (sortedValues.size > 1) availableWidth / (sortedValues.size - 1) else availableWidth

        val path = Path()

        sortedValues.forEachIndexed { index, value ->
            val x = padding + (index * stepX)
            val normalizedY = 1.0f - value
            val y = padding + (normalizedY * availableHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        val fillPath = Path()
        fillPath.addPath(path)

        val lastX = padding + ((sortedValues.size - 1) * stepX)
        val bottomY = canvasHeight - padding
        fillPath.lineTo(lastX, bottomY)
        fillPath.lineTo(padding, bottomY)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    colorPalette.background1,
                    colorPalette.background2,
                )
            )
        )

        drawPath(
            path = path,
            color = colorPalette.accent,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.cornerPathEffect(20.dp.toPx()) // Angoli smussati
            )
        )

        sortedValues.forEachIndexed { index, value ->
            val x = padding + (index * stepX)
            val normalizedY = 1.0f - value
            val y = padding + (normalizedY * availableHeight)

            drawCircle(
                color = colorPalette.text,
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = colorPalette.textSecondary,
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}