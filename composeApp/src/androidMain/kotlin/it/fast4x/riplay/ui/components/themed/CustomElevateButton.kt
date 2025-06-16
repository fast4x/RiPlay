package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.doubleShadowDrop

@Composable
fun CustomElevatedButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interSource = remember { MutableInteractionSource() }
    val isPressed by interSource.collectIsPressedAsState()

    // Animate shadow offset and blur radius
    // To create a hide shadow animation on press
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp, label = ""
    )
    val shadowBlur by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 8.dp, label = ""
    )

    Box(
        modifier = modifier
            // Apply shadow effect
            .doubleShadowDrop(shape, shadowOffset, shadowBlur)
            //.background(Color(0xFF010203), shape)
            .background(backgroundColor, shape)
            .clip(shape),
            /*
            .clickable(
                interactionSource = interSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
             */
        contentAlignment = Alignment.Center,
        content = content
    )
}

