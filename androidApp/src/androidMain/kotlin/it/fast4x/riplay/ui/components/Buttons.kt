package it.fast4x.riplay.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import kotlinx.coroutines.launch

@Composable
fun ActionPillButton(
    modifier: Modifier = Modifier,
    icon: Int,
    iconSize: Dp? = 22.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    activeColor: Color = Color.Unspecified,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val bgColor by animateColorAsState(
        targetValue = if (active && activeColor != Color.Unspecified)
            activeColor.copy(alpha = 0.18f)
        else
            tint.copy(alpha = 0.08f),
        animationSpec = tween(300),
        label = "pillBg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(getRoundnessShape())
            .background(bgColor)
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        coroutineScope.launch {
                            // SCALE IN (Rimpicciolisce velocemente)
                            scale.animateTo(
                                targetValue = 0.85f,
                                animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing)
                            )
                            // SCALE OUT (Rimbalza indietro con la molla)
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                    onClick()
                },
                onLongClick = {
                    if (enabled) {
                        coroutineScope.launch {
                            // Stesso rimbalzo anche per il long click
                            scale.animateTo(0.85f, tween(80, easing = FastOutLinearInEasing))
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                    }
                    onLongClick?.invoke()
                }
            )
            .padding(8.dp)

    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (enabled) tint else tint.copy(alpha = 0.35f)
            ),
            modifier = Modifier.size(iconSize ?: 22.dp)
        )
    }
}

@Composable
fun ActionPillButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    iconSize: Dp? = 22.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    activeColor: Color = Color.Unspecified,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val bgColor by animateColorAsState(
        targetValue = if (active && activeColor != Color.Unspecified)
            activeColor.copy(alpha = 0.18f)
        else
            tint.copy(alpha = 0.08f),
        animationSpec = tween(300),
        label = "pillBg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(getRoundnessShape())
            .background(bgColor)
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        coroutineScope.launch {
                            // SCALE IN (Rimpicciolisce velocemente)
                            scale.animateTo(
                                targetValue = 0.85f,
                                animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing)
                            )
                            // SCALE OUT (Rimbalza indietro con la molla)
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                    onClick()
                },
                onLongClick = {
                    if (enabled) {
                        coroutineScope.launch {
                            // Stesso rimbalzo anche per il long click
                            scale.animateTo(0.85f, tween(80, easing = FastOutLinearInEasing))
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                    }
                    onLongClick?.invoke()
                }
            )
            .padding(8.dp)

    ) {
        Image(
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (enabled) tint else tint.copy(alpha = 0.35f)
            ),
            modifier = Modifier.size(iconSize ?: 22.dp)
        )
    }
}

@Composable
fun PillIconButton(
    icon: Int,
    active: Boolean = false,
    size: Int = 22,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = colorPalette().accent
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((size + 14).dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else
                    Modifier.clickable(onClick = onClick)
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (active) accent else colorPalette().text),
            modifier = Modifier.size(size.dp)
        )
    }
}


@Composable
fun AlbumPillIconButton(
    icon: Int,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Int = 22,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = colorPalette().accent
    val tint = when {
        !enabled -> colorPalette().textDisabled
        active   -> accent
        else     -> colorPalette().text
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((size + 14).dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                else
                    Modifier.clickable(enabled = enabled, onClick = onClick)
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .size(size.dp)
                .alpha(if (enabled) 1f else 0.4f)
        )
    }
}