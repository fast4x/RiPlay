package it.fast4x.riplay.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.ActionPillButton
import kotlinx.coroutines.launch

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
        modifier = modifier, //.padding(end = 5.dp),
        icon = icon,
        //iconSize = iconSize,
        enabled = enabled,
        color = color,
        onClick = onClick,
        onLongClick = onLongClick
    )
    /*
    IconButton(
        icon = icon,
        color = color,
        onClick = onClick,
        enabled = enabled,
        indication = indication,
        modifier = modifier
            .padding(all = 2.dp)
            .size(iconSize ?: 18.dp)
    )

     */
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
    indication: Indication? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .combinedClickable(
                indication = indication ?: ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled,
//                onClick = onClick,
//                onLongClick = onLongClick

                onClick = {
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
                    onClick()
                },
                onLongClick = {
                    coroutineScope.launch {
                        // Stesso rimbalzo anche per il long click
                        scale.animateTo(0.85f, tween(80, easing = FastOutLinearInEasing))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                    }
                    onLongClick?.invoke()
                }
            )
            .then(modifier)
    )
}

