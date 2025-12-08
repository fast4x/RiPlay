package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R

@Composable
fun ListenerBadge(
    size : Int = 80,
    icon: Int,
    iconTint: Color = Color.White,
    borderBrush: Brush,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .shadow(8.dp, CircleShape)
            .background(Color.DarkGray)
            .border(
                width = 3.dp,
                brush = borderBrush,
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "Listener Badge",
            tint = iconTint,
            modifier = Modifier.size(size.dp / 2)
        )
    }
}


@Composable
fun MonthlyIconBadge(
    icon: Int = R.drawable.star,
    colors: List<Color> = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA500)  // Orange
    )
) {
    ListenerBadge(
        icon = icon,
        borderBrush = Brush.radialGradient(
            colors = colors
        )
    )
}