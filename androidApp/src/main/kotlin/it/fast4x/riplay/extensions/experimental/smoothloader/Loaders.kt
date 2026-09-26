package it.fast4x.riplay.extensions.experimental.smoothloader

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.utils.applyIf
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Loader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isStartingUp: Boolean = false,
    expandWidth: Boolean = true
) {
    val appSettingsManager = Dependencies.application.appSettingsManager
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .applyIf(expandWidth) {
                fillMaxWidth()
            }
    ) {
        SmoothLoader(
            modifier = Modifier.align(Alignment.Center),
            style = appSettings.loaderStyle,
            size = size,
            isStartingUp = isStartingUp
        )
    }
}

@Composable
fun LoaderScreen(
    show: Boolean = true,
    mini: Boolean = false,
    isStartingUp: Boolean = false,
) {
    if (!show) return

    Column(
        modifier = if (!mini) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Loader(
                isStartingUp = isStartingUp,
                size = if (mini) 48.dp else 96.dp
            )
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


