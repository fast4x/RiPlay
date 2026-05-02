package it.fast4x.riplay.ui.components.navigation.header

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getColorTheme

@Composable
internal fun HeaderIcon(
    iconId: Int,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    onClick: () -> Unit
) {

    IconButton( onClick ) {
        Icon(
            imageVector = ImageVector.vectorResource( iconId ),
            contentDescription = null,
            modifier = Modifier.size( size ),
            tint = tint
        )
    }
}

internal class AppBar {

    internal companion object {

        @Composable
        fun contentColor(): Color =
             when(getColorTheme()) {
                ColorPaletteMode.Light, ColorPaletteMode.System -> colorPalette().text
                else -> Color.White
            }
    }
}