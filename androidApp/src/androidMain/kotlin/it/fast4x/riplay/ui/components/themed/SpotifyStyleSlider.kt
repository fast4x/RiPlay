package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

@Composable
fun SpotifyStyleSlider(
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animazione morbida del thumb
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumbScale"
    )

    // Colore dinamico (es. basato sull'accent della palette)
    val accentColor = colorPalette().accent

    Slider(
        value = position,
        onValueChange = onSeek,
        valueRange = 0f..duration,
        interactionSource = interactionSource,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = accentColor.copy(alpha = 0.3f)
        ),
        thumb = {
            // Thumb personalizzato animato
            Spacer(
                modifier = Modifier
                    .size(16.dp * thumbScale) // Dimensione base 16, si ingrandisce
                    .background(accentColor, CircleShape)
                    // Aggiungiamo un'ombra morbida quando è premuto (stile Spotify)
                    .shadow(if (isPressed) 8.dp else 0.dp, CircleShape)
            )
        }
    )
}