package it.fast4x.riplay.ui.styling

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("ClassName")
object Dimensions {
    val itemsVerticalPadding = 4.dp

    val navigationRailWidth = 50.dp
    val navigationRailWidthLandscape = 128.dp
    val navigationRailIconOffset = 6.dp
    val headerHeight = 140.dp
    val halfheaderHeight = 60.dp
    val miniPlayerHeight = 64.dp
    val collapsedPlayer = 84.dp
    val navigationBarHeight = 64.dp
    val contentWidthRightBar = 0.88f
    val additionalVerticalSpaceForFloatingAction = 40.dp
    val bottomSpacer = 100.dp
    val fadeSpacingTop = 30.dp
    val fadeSpacingBottom = 64.dp


    object thumbnails {
        val album = 140.dp
        val artist = 140.dp
        val song = 64.dp
        val playlist = 140.dp

        object player {
            val song: Dp
                @Composable
                get() = with(LocalConfiguration.current) {
                    minOf(screenHeightDp, screenWidthDp)
                }.dp
        }
    }

}

inline val Dp.px: Int
    @Composable
    inline get() = with(LocalDensity.current) { roundToPx() }
