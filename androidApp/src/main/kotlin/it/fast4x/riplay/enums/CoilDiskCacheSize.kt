package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class CoilDiskCacheMaxSize {
    `32MB`,
    `64MB`,
    `128MB`,
    `256MB`,
    `512MB`,
    `1GB`,
    `2GB`,
    `4GB`,
    `8GB`,
    Custom;

    val displayName: String
        @Composable
        get() = when (this) {
            CoilDiskCacheMaxSize.Custom -> stringResource(R.string.custom)
            CoilDiskCacheMaxSize.`32MB` -> "32MB"
            CoilDiskCacheMaxSize.`64MB` -> "64MB"
            CoilDiskCacheMaxSize.`128MB` -> "128MB"
            CoilDiskCacheMaxSize.`256MB`-> "256MB"
            CoilDiskCacheMaxSize.`512MB`-> "512MB"
            CoilDiskCacheMaxSize.`1GB`-> "1GB"
            CoilDiskCacheMaxSize.`2GB` -> "2GB"
            CoilDiskCacheMaxSize.`4GB` -> "4GB"
            CoilDiskCacheMaxSize.`8GB` -> "8GB"
        }

    val bytes: Long
        get() = when (this) {
            `32MB` -> 32
            `64MB` -> 64
            `128MB` -> 128
            `256MB` -> 256
            `512MB` -> 512
            `1GB` -> 1024
            `2GB` -> 2048
            `4GB` -> 4096
            `8GB` -> 8192
            Custom -> 1000000
        } * 1000 * 1000L
}
