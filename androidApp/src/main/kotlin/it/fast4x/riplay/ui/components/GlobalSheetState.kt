package it.fast4x.riplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

val LocalGlobalSheetState = staticCompositionLocalOf { GlobalSheetState() }

@Stable
class GlobalSheetState {
    var isDisplayed by mutableStateOf(false)
        private set

    var content by mutableStateOf<@Composable () -> Unit>({})
        private set

    fun display(content: @Composable () -> Unit) {
        this.content = content
        isDisplayed = true
    }

    fun hide() {
        isDisplayed = false
    }
}

@Composable
inline fun SheetBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .background(colorPalette().background1)
            .padding(vertical = 8.dp)
            .navigationBarsPadding(),
        content = content
    )
}