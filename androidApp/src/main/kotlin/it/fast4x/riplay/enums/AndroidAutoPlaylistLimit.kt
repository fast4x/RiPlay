package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class AndroidAutoPlaylistLimit {
    Unlimited,
    `100`,
    `200`,
    `300`,
    `400`,
    `500`;

    val displayName: String
        @Composable
        get() = this.number?.toString()
            ?: stringResource(R.string.aa_playlist_song_limit_unlimited)

    val number: Int?
        get() = when (this) {
            Unlimited -> null
            `100` -> 100
            `200` -> 200
            `300` -> 300
            `400` -> 400
            `500` -> 500
        }
}
