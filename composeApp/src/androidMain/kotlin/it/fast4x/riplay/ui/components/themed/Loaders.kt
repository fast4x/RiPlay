package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun Loader(
    modifier: Modifier = Modifier.fillMaxWidth(),
    size: Dp = 32.dp,
) = Box(
    modifier = modifier,
) {
    PoligonIndicatorScreen(mini = true)
//    Image(
//        painter = painterResource(R.drawable.loader),
//        contentDescription = null,
//        colorFilter = ColorFilter.tint(colorPalette().text),
//        modifier = Modifier
//            .align(Alignment.Center)
//            .size(size)
//    )
}

@Composable
fun LoaderScreen(show: Boolean = true) {
    if (!show) return
    //RotatingLoaderScreen()
    PoligonIndicatorScreen()
}

@Composable
fun RotatingLoaderScreen() {

    val infiniteTransition = rememberInfiniteTransition(label = "infinite_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotationAngle),
            painter = painterResource(id = R.drawable.app_icon),
            colorFilter = ColorFilter.tint(colorPalette().accent),
            contentDescription = "loading..."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Loading, please wait...",
            style = typography().m
        )
    }
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PoligonIndicatorScreen(
    mini: Boolean = false,
) {
    var containerShape by remember {
        mutableStateOf(LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random())
    }

    // Animazione per il "respiro" dello sfondo
    val infiniteTransition = rememberInfiniteTransition(label = "PulseAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    LaunchedEffect(Unit) {
        while (isActive) {
            containerShape = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random()
            delay(500)
        }
    }

    Column(
        modifier = if (!mini) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Box contenitore per sovrapporre sfondo e indicatore
        Box(
            contentAlignment = Alignment.Center
        ) {
            // 1. Sfondo Pulsante (Background)
            Spacer(
                modifier = Modifier
                    .size(if (mini) 64.dp else 128.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        color = colorPalette().accent, // Usa il colore accent per coerenza
                        shape = getRoundnessShape() // Forma circolare morbida
                    )
            )

            // 2. Indicatore (Foreground)
            Crossfade(
                containerShape,
                modifier = Modifier.wrapContentSize(),
            ) { shape ->
                // Wrappo in un Box per sovrapporre i due indicatori se necessario
                //Box(contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(if (mini) 48.dp else 96.dp),
                        polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
                        containerColor = colorPalette().background0,
                        indicatorColor = colorPalette().accent,
                        containerShape = shape.toShape(),
                    )

                    ContainedLoadingIndicator(
                        modifier = Modifier.size(if (mini) 32.dp else 64.dp),
                        polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.shuffled(),
                        containerColor = colorPalette().background0,
                        indicatorColor = colorPalette().accent.copy(alpha = .6f),
                        containerShape = shape.toShape(),
                    )
                //}
            }
        }

        if (!mini) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.loading_please_wait),
                style = typography().xs
            )
        }
    }
}

@Composable
fun PlayerCircularLoader(size: Dp = 42.dp) =
    CircularProgressIndicator(
        modifier = Modifier
            .size(size)
            .padding(4.dp),
        color = colorPalette().collapsedPlayerProgressBar,
        strokeWidth = 3.dp,
        strokeCap = StrokeCap.Round,
        trackColor = colorPalette().collapsedPlayerProgressBar.copy(alpha = 0.2f)
    )

@Composable
fun PlayerRotatingLoader(
    imgSize: Dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buffer_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    val colorPalette = colorPalette()

    Canvas(
        modifier = Modifier
            .size(imgSize)
    ) {
        val strokeWidth = 3.dp.toPx()
        val arcRadius = size.width / 2 - strokeWidth / 2

        drawCircle(
            color = colorPalette.collapsedPlayerProgressBar.copy(alpha = 0.15f),
            radius = arcRadius,
            style = Stroke(width = strokeWidth)
        )

        rotate(rotation) {
            drawArc(
                color = colorPalette.collapsedPlayerProgressBar,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}