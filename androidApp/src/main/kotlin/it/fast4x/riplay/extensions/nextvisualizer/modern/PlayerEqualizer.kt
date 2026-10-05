package it.fast4x.riplay.extensions.nextvisualizer.modern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

@Composable
fun PlayerEqualizerSpectrum(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val bgState = rememberEqualizerState(barCount = 40, simulationEnabled = false)

    Box(
        modifier = modifier
    ) {
        EqualizerBars(
            modifier = Modifier.matchParentSize(),
            state = bgState,
            barColor = colorPalette().text.copy(alpha = .4f),
            peakColor = colorPalette().accent,
            barSpacing = 4.dp,
        )
        content()
    }

}

@Composable
fun PlayerEqualizerLevel(
    height: Dp = 5.dp,
    content: @Composable () -> Unit = {},
) {
    val meterState = rememberLevelMeterState(simulationEnabled = false)

    Box {
        HorizontalLevelBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            cornerRadius = 0.dp,
            state = meterState,
            colors = listOf(
                colorPalette().textDisabled,
                colorPalette().textSecondary,
                colorPalette().text,
                colorPalette().accent,

            )
        )
        content()
    }
}

@Composable
fun PlayerVisualizerHero() {
    val heroState = rememberEqualizerState(barCount = 24, peakFadeMs = 1200f)

    EqualizerBars(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp)),
        state = heroState,
        barColor = MaterialTheme.colorScheme.primary,
        peakColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
    )
}