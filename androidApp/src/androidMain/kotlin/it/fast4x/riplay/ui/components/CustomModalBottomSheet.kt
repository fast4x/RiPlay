package it.fast4x.riplay.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModalBottomSheet(
    showSheet: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean? = true,
    sheetGestureEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = colorPalette().background0,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = {
        BottomSheetDefaults.DragHandle()
    },
    contentWindowInsets: @Composable () -> WindowInsets = {
        //WindowInsets.ime.add(WindowInsets.navigationBars)
        WindowInsets(bottom = 0)
    },
    content: @Composable ColumnScope.() -> Unit,
) {

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = initiallyExpanded == true
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            sheetGesturesEnabled = sheetGestureEnabled,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets,
        ) {
            SetupSystemBarsForSheet(containerColor)
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeight * 0.5f)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SetupSystemBarsForSheet(sheetBackgroundColor: Color) {
    val colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    val isPitchBlack = colorPaletteMode == ColorPaletteMode.PitchBlack

    val isDarkTheme = colorPaletteMode == ColorPaletteMode.Dark ||
            isPitchBlack ||
            (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme())

    val view = LocalView.current
    (view.parent as? DialogWindowProvider)?.window?.let { window ->
        SideEffect {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }
    }
}
