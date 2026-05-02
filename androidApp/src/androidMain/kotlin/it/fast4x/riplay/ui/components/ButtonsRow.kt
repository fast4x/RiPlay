package it.fast4x.riplay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun <E> ButtonsRow(
    buttons: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    val scrollState = rememberScrollState()

    val backgroundColor = colorPalette().background0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(8.dp))

            buttons.forEach { (value, label) ->
                FilterChip(
                    label = {
                        Text(
                            label,
                            style = typography().xs,
                            maxLines = 1
                        )
                    },
                    selected = currentValue == value,
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = colorPalette().textSecondary,
                        selectedLabelColor = colorPalette().text,
                        selectedContainerColor = when (colorPaletteMode) {
                            ColorPaletteMode.Dark, ColorPaletteMode.PitchBlack
                                -> colorPalette().textDisabled
                            else -> colorPalette().background3
                        } ,
                    ),
                    onClick = { onValueUpdate(value) }
                )

                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.width(8.dp))
        }


        AnimatedVisibility(
            visible = scrollState.canScrollBackward,
            enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(200)),
            exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clip(CircleShape)
                        .background(colorPalette().background1.copy(alpha = 0.8f))
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_back),
                        contentDescription = "Scroll Indietro",
                        tint = colorPalette().text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = scrollState.canScrollForward,
            enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(200)),
            exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = 0.95f)
                            )
                        )
                    ),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(CircleShape)
                        .background(colorPalette().background1.copy(alpha = 0.8f))
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_forward),
                        contentDescription = "Scroll Avanti",
                        tint = colorPalette().text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


/*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette

@Composable
fun <E> ButtonsRow(
    buttons: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.width(12.dp))

        buttons.forEach { (value, label) ->
            FilterChip(
                label = { Text(label) },
                selected = currentValue == value,
                colors = FilterChipDefaults
                    .filterChipColors(
                        containerColor = colorPalette().background1,
                        labelColor = colorPalette().text,
                        selectedContainerColor = when (colorPaletteMode) {
                            ColorPaletteMode.Dark, ColorPaletteMode.PitchBlack
                                -> colorPalette().textDisabled
                            else -> colorPalette().background3
                        } ,
                        selectedLabelColor = colorPalette().text,
                    ),
                onClick = { onValueUpdate(value) }
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}
*/