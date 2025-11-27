package it.fast4x.riplay.ui.components.themed

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar.Icon
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

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