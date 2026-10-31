package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class SwipeToNext {
    RightToLeft, // Swipe from right to left normal case
    LeftToRight;

    val displayName: String
        @Composable
        get() = when (this) {
            SwipeToNext.RightToLeft -> stringResource(R.string.swipe_to_next_settings_right_to_left)
            SwipeToNext.LeftToRight -> stringResource(R.string.swipe_to_next_settings_left_to_right)
        }
}