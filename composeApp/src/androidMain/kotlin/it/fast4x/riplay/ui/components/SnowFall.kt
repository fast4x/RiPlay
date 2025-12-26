package it.fast4x.riplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val SNOWFLAKES_COUNT = 300
private const val SNOWFLAKE_BASE_SPEED_DP_PER_SECOND = 70
private const val SNOWFLAKE_MAX_DEVIATION_SPEED_DP_PER_SECOND = 40
private const val SNOWFLAKE_MIN_DEVIATION_SPEED_DP_PER_SECOND = 10
private const val SNOWFLAKE_BASE_SIZE = 4
private const val SNOWFLAKE_MAX_DEVIATION_SIZE = 3

@Composable
fun Snowfall() {
    val snowflakes = remember {
        (1..SNOWFLAKES_COUNT).map {
            Snowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = (SNOWFLAKE_BASE_SPEED_DP_PER_SECOND + Random.nextInt(
                    -SNOWFLAKE_MAX_DEVIATION_SPEED_DP_PER_SECOND,
                    SNOWFLAKE_MAX_DEVIATION_SPEED_DP_PER_SECOND
                )) / 10f,
                deviation = (Random.nextInt(
                    SNOWFLAKE_MIN_DEVIATION_SPEED_DP_PER_SECOND,
                    SNOWFLAKE_MAX_DEVIATION_SPEED_DP_PER_SECOND
                ) * (if (Random.nextBoolean()) 1 else -1)) / 10f,
                size = (SNOWFLAKE_BASE_SIZE + Random.nextInt(0, SNOWFLAKE_MAX_DEVIATION_SIZE)).toFloat()
            )
        }.toMutableList()
    }

    var time by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            time = withFrameNanos { it } - startTime
            delay(300)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (snowflake in snowflakes) {
            val newY = (snowflake.y * canvasHeight + snowflake.speed * time / 100f) % canvasHeight
            val newX = (snowflake.x * canvasWidth + snowflake.deviation * time / 100f) % canvasWidth

            drawCircle(
                color = Color.White,
                radius = snowflake.size,
                center = Offset(newX, newY)
            )
        }
    }
}

private data class Snowflake(
    val x: Float,
    val y: Float,
    val speed: Float,
    val deviation: Float,
    val size: Float
)