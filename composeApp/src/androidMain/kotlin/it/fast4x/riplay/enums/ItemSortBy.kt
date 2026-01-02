package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ItemSortBy {
    Title,
    Year;

    val textName: String
        @Composable
        get() = when (this) {
            Title -> stringResource(R.string.sort_title)
            Year -> stringResource(R.string.sort_year)
        }

    val icon: Int
        get() = when (this) {
            Title -> R.drawable.text
            Year -> R.drawable.calendar
        }
}