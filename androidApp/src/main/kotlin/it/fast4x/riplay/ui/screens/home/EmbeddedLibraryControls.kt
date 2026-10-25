package it.fast4x.riplay.ui.screens.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SheetBody
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

/** Presentation only: the embedded library shares its title with the parent tab. */
internal val LocalEmbeddedLibrary = staticCompositionLocalOf { false }

@Composable
internal fun ColumnScope.EmbeddedLibraryControls(
    itemCount: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!LocalEmbeddedLibrary.current) {
        content()
        return
    }

    val sheet = LocalGlobalSheetState.current
    val currentContent by rememberUpdatedState(content)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimensions.touchTarget).padding(horizontal = 16.dp)
    ) {
        BasicText(
            text = stringResource(R.string.mobile_home_item_count, itemCount),
            style = typography().xs.copy(color = colorPalette().textSecondary),
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = { sheet.display { SheetBody { currentContent() } } },
            modifier = Modifier.heightIn(min = Dimensions.touchTarget)
        ) {
            BasicText(
                text = stringResource(R.string.mobile_home_library_options),
                style = typography().xs.semiBold.copy(color = colorPalette().text)
            )
        }
    }
}
