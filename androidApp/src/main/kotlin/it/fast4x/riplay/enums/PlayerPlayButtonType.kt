package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class PlayerPlayButtonType {
    Disabled,
    Default,
    Rectangular,
    CircularRibbed,
    Square,
    Circle;

    val displayName: String
        @Composable
        get() = when (this) {
            PlayerPlayButtonType.Disabled -> stringResource(R.string.vt_disabled)
            PlayerPlayButtonType.Default -> stringResource(R.string._default)
            PlayerPlayButtonType.Rectangular -> stringResource(R.string.rectangular)
            PlayerPlayButtonType.Square -> stringResource(R.string.square)
            PlayerPlayButtonType.CircularRibbed -> stringResource(R.string.circular_ribbed)
            PlayerPlayButtonType.Circle -> stringResource(R.string.circle)
        }

    val height: Int
        get() = when (this) {
            Default, Disabled -> 60
            Rectangular -> 70
            CircularRibbed -> 100
            Square -> 80
            Circle -> 80
        }

    val width: Int
        get() = when (this) {
            Default, Disabled -> 60
            Rectangular -> 110
            CircularRibbed -> 100
            Square -> 80
            Circle -> 80
        }
}