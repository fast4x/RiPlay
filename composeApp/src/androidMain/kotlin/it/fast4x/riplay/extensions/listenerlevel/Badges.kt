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
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.SonicWhisper
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.SoulNavigator
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.TheDailyWanderer
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.TheLegend
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.TheSonicOracle
import it.fast4x.riplay.extensions.listenerlevel.AnnualListenerLevel.TheSoundExplorer
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.FrequencyDominator
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.MonthlyIcon
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.SoundCheck
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.TheDJofYourDay
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.TheMonthlyExplorer
import it.fast4x.riplay.extensions.listenerlevel.MonthlyListenerLevel.VibeMaster

@Composable
fun ListenerBadge(
    size : Int = 80,
    icon: Int,
    color: Color = Color.White,
    borderBrush: Brush,
    borderWidth: Int = 8,
    shadowElevation: Int = 12
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .shadow((shadowElevation).dp, CircleShape)
            .background(Color.DarkGray)
            .border(
                width = borderWidth.dp,
                brush = borderBrush,
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "Listener Badge",
            tint = color,
            modifier = Modifier.size(size.dp / 2)
        )
    }
}


@Composable
fun IconBadge(
    size : Int = 60,
    icon: Int = R.drawable.star,
    color: Color = Color.White,
    colors: List<Color> = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA500)  // Orange
    ),
    borderWidth: Int = 8
) {
    ListenerBadge(
        size = size,
        icon = icon,
        color = color,
        borderBrush = Brush.radialGradient(
            colors = colors
        ),
        borderWidth = borderWidth
    )
}

@Composable
fun IconBadge(level: AnnualListenerLevel, size: Int = 60, borderWidth: Int = 8) {
    when (level) {
        SonicWhisper -> IconBadge(
            size = size,
            icon = R.drawable.musical_note,
            colors = listOf(
                Color(0xFFA0E0E0),
                Color(0xFF30B0B0)
            ),
            borderWidth = borderWidth
        )
        TheSoundExplorer -> IconBadge(
            size = size,
            icon = R.drawable.headphones,
            colors = listOf(
                Color(0xFFA2D5A1),
                Color(0xFF60CC4E)
            ),
            borderWidth = borderWidth
        )
        TheDailyWanderer -> IconBadge(
            size = size,
            icon = R.drawable.music,
            colors = listOf(
                Color(0xFFE0A17F),
                Color(0xFFD96334)
            ),
            borderWidth = borderWidth
        )
        SoulNavigator -> IconBadge(
            size = size,
            icon = R.drawable.music_album,
            colors = listOf(
                Color(0xFFD081DE),
                Color(0xFFA029B4)
            ),
            borderWidth = borderWidth
        )
        TheSonicOracle -> IconBadge(
            size = size,
            icon = R.drawable.music_equalizer,
            colors = listOf(
                Color(0xFF6497D5),
                Color(0xFF1689D9)
            ),
            borderWidth = borderWidth
        )
        TheLegend -> IconBadge(
            size = size,
            icon = R.drawable.trophy,
            colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFFFA500)
            ),
            borderWidth = borderWidth
        )
    }
}

@Composable
fun IconBadge(level: MonthlyListenerLevel, size: Int = 60, borderWidth: Int = 8) {
    when (level) {
        SoundCheck -> IconBadge(
            size = size,
            icon = R.drawable.play,
            colors = listOf(
                Color(0xFFABADAB),
                Color(0xFF606460)
            ),
            borderWidth = borderWidth
        )
        TheMonthlyExplorer -> IconBadge(
            size = size,
            icon = R.drawable.headset,
            colors = listOf(
                Color(0xFF92EE92),
                Color(0xFF2D6C2D)
            ),
            borderWidth = borderWidth
        )
        TheDJofYourDay -> IconBadge(
            size = size,
            icon = R.drawable.disc,
            colors = listOf(
                Color(0xFFEC5C61),
                Color(0xFFCB1126)
            ),
            borderWidth = borderWidth
        )
        FrequencyDominator -> IconBadge(
            size = size,
            icon = R.drawable.equalizer,
            colors = listOf(
                Color(0xFF7EA6EA),
                Color(0xFF0B67A2)
            ),
            borderWidth = borderWidth
        )
        VibeMaster -> IconBadge(
            size = size,
            icon = R.drawable.volume_up,
            colors = listOf(
                Color(0xFFDA92E7),
                Color(0xFFC841E0)
            ),
            borderWidth = borderWidth
        )
        MonthlyIcon -> IconBadge(
            size = size,
            icon = R.drawable.star,
            colors = listOf(
                Color(0xFFFFD700), // Gold
                Color(0xFFFFA500)  // Orange
            ),
            borderWidth = borderWidth
        )
    }
}