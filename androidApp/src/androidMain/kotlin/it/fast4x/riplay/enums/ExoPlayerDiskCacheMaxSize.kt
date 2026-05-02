package it.fast4x.riplay.enums

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
