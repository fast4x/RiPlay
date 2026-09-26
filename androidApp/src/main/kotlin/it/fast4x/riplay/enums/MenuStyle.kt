package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class MenuStyle {
    List,
    Grid;

    val displayName: String
        @Composable
        get() = when (this) {
            Grid -> stringResource(R.string.style_grid)
            List -> stringResource(R.string.style_list)
        }
}
