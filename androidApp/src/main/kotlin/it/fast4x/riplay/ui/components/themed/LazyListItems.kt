package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape


fun LazyListScope.settingsItem(
    isHeader: Boolean = false,
    content: @Composable () -> Unit
) {
    if (isHeader)
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background0)
            ) {
                content()
            }
        }
    else
        item {
            Column( modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .background(colorPalette().background1.copy(alpha = .5f), shape = getRoundnessShape())
                .fillMaxWidth()

            ) {
                content()
            }
        }
}

fun LazyListScope.settingsSearchBarItem(
    content: @Composable (ColumnScope.() -> Unit)
) {
    item {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                //.background(colorPalette().background0)
        ) {
            content()
        }
    }
}
