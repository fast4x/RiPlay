package it.fast4x.riplay.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.ActionPillButton
import it.fast4x.riplay.ui.components.mobilePressFeedback

@Composable
fun HeaderIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    @DrawableRes icon: Int,
    color: Color,
    enabled: Boolean = true,
    indication: Indication? = null,
    iconSize: Dp? = 22.dp
) {
    ActionPillButton(
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        color = color,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = null,
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        // Keep the 48dp clickable container outside legacy icon-size/padding modifiers.
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .mobilePressFeedback(interactionSource)
            .clip(CircleShape)
            .combinedClickable(
                indication = indication ?: ripple(),
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(modifier)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (enabled) color else color.copy(alpha = 0.38f)),
            modifier = Modifier.size(24.dp)
        )
    }
}

