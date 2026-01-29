package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun Loader(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) = Box(
    modifier = modifier,
) {
    Image(
        painter = painterResource(R.drawable.loader),
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorPalette().text),
        modifier = Modifier
            .align(Alignment.Center)
            .size(size)
    )
}

@Composable
fun LoaderScreen(show: Boolean = true) {
    if (!show) return
    //RotatingLoaderScreen()
    PoligonIndicatorScreen(Modifier)
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
fun PoligonIndicatorScreen(modifier: Modifier) {
    var containerShape by remember {
        mutableStateOf(LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            containerShape = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random()
            delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(
            containerShape,
            modifier = Modifier
                    .wrapContentSize(),
        ) { shape ->
            ContainedLoadingIndicator(
                modifier =
                    Modifier
                        .size(96.dp),
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
                containerColor = colorPalette().background0,
                indicatorColor = colorPalette().accent,
                containerShape = shape.toShape(),
            )

            ContainedLoadingIndicator(
                modifier =
                    Modifier
                        .size(64.dp),
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.shuffled(),
                containerColor = colorPalette().background0,
                indicatorColor = colorPalette().accent.copy(alpha = .6f),
                containerShape = shape.toShape(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.loading_please_wait),
            style = typography().xs
        )

    }
}