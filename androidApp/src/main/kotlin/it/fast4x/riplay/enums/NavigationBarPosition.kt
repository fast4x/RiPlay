package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class NavigationBarPosition {
    Left,
    Right,
    Top,
    Bottom;

    val displayName: String
        @Composable
        get() = when (this) {
            NavigationBarPosition.Left -> stringResource(R.string.direction_left)
            NavigationBarPosition.Right -> stringResource(R.string.direction_right)
            NavigationBarPosition.Top -> stringResource(R.string.direction_top)
            NavigationBarPosition.Bottom -> stringResource(R.string.direction_bottom)
        }

    companion object {

        @Composable
        fun current(): NavigationBarPosition {
            // Keep legacy enum values for saved-settings compatibility. Android uses
            // one fixed bottom bar; old sidebar preferences must not narrow its pages.
            return Bottom
        }
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}
