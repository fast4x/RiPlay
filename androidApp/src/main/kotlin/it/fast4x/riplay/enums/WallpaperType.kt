package it.fast4x.riplay.enums

enum class WallpaperType {
    Home,
    Lockscreen,
    Both;

    val displayName: String
    get() = when (this) {
        Home -> "Home"
        Lockscreen -> "Lockscreen"
        Both -> "Both"
    }
}