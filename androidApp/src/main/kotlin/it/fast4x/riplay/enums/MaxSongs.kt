package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class MaxSongs {
    `50`,
    `100`,
    `200`,
    `300`,
    `500`,
    `1000`,
    `2000`,
    `3000`,
    Unlimited;

    val displayName: String
        @Composable
        get() = when (this) {
            MaxSongs.Unlimited -> stringResource(R.string.unlimited)
            MaxSongs.`50` -> MaxSongs.`50`.name
            MaxSongs.`100` -> MaxSongs.`100`.name
            MaxSongs.`200` -> MaxSongs.`200`.name
            MaxSongs.`300` -> MaxSongs.`300`.name
            MaxSongs.`500` -> MaxSongs.`500`.name
            MaxSongs.`1000` -> MaxSongs.`1000`.name
            MaxSongs.`2000` -> MaxSongs.`2000`.name
            MaxSongs.`3000` -> MaxSongs.`3000`.name
        }

    val number: Long
        get() = when (this) {
            `50` -> 50
            `100` -> 100
            `200` -> 200
            `300` -> 300
            `500` -> 500
            `1000` -> 1000
            `2000` -> 2000
            `3000` -> 3000
            Unlimited -> 1000000
        } * 1L
}