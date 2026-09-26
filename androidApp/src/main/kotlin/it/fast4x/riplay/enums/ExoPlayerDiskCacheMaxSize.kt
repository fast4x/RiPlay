package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ExoPlayerDiskCacheMaxSize {
    `Disabled`,
    `32MB`,
    `512MB`,
    `1GB`,
    `2GB`,
    `4GB`,
    `8GB`,
    Unlimited,
    Custom;

    val displayName: String
        @Composable
        get() = when (this) {
            ExoPlayerDiskCacheMaxSize.Disabled -> stringResource(R.string.turn_off)
            ExoPlayerDiskCacheMaxSize.Unlimited -> stringResource(R.string.unlimited)
            ExoPlayerDiskCacheMaxSize.Custom -> stringResource(R.string.custom)
            ExoPlayerDiskCacheMaxSize.`32MB` -> "32MB"
            ExoPlayerDiskCacheMaxSize.`512MB` -> "512MB"
            ExoPlayerDiskCacheMaxSize.`1GB` -> "1GB"
            ExoPlayerDiskCacheMaxSize.`2GB` -> "2GB"
            ExoPlayerDiskCacheMaxSize.`4GB` -> "4GB"
            ExoPlayerDiskCacheMaxSize.`8GB` -> "8GB"

        }

    val bytes: Long
        get() = when (this) {
            Disabled -> 1
            `32MB` -> 32
            `512MB` -> 512
            `1GB` -> 1024
            `2GB` -> 2048
            `4GB` -> 4096
            `8GB` -> 8192
            Unlimited -> 0
            Custom -> 1000000
        } * 1000 * 1000L
}
