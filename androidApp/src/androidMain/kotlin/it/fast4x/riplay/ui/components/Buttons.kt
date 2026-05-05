package it.fast4x.riplay.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

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