package it.fast4x.riplay.extensions.timeline

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.delay

data class TimelinePoint(
    val point: @Composable () -> Unit,
    val marker: Int? = null
)

@Composable
fun AnimatedVerticalTimeline(
    //points: List<@Composable () -> Unit> = emptyList()
    timelinePoints: MutableList<TimelinePoint> = mutableListOf()
) {

    val visibleItems = remember { mutableStateListOf<Boolean>().apply { repeat(timelinePoints.size) { add(false) } } }

    LaunchedEffect(Unit) {
        timelinePoints.forEachIndexed { index, _ ->
            delay(100 * index.toLong())
            visibleItems[index] = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        timelinePoints.forEachIndexed { index, point ->
            TimelineItem(
                isVisible = visibleItems[index],
                index = index,
                marker = point.marker
            ) {
                point.point.invoke()
            }
        }
    }
}

@Composable
fun TimelineItem(isVisible: Boolean, index: Int, marker: Int? = null, content: @Composable () -> Unit) {
    val animationDuration = 100
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = index * 100)
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = index * 100)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .background(
                    color = if (isVisible) colorPalette().accent else colorPalette().textDisabled,
                    shape = CircleShape
                )
                .alpha(alpha)
        ) {
            if (marker != null)
                Image(
                    modifier = Modifier.size(18.dp).align(Alignment.Center),
                    contentDescription = "marker",
                    painter = painterResource(marker)
                )
        }

        Box(
            modifier = Modifier
                .height(2.dp)
                .width(40.dp)
                .scale(scale)
                .background(
                    color = if (isVisible) colorPalette().accent else colorPalette().textDisabled,
                    shape = CircleShape
                )
                .alpha(alpha)
        )



        Box(modifier = Modifier.alpha(alpha).padding(start = 6.dp)){
            content()
        }

    }

}
