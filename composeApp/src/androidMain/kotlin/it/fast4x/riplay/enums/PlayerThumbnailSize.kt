package it.fast4x.riplay.enums

enum class PlayerThumbnailSize {
    Small,
    Medium,
    Big,
    Biggest,
    Expanded;

    val padding: Int
        get() = when (this) {
            Small -> 90
            Medium -> 55
            Big -> 30
            Biggest -> 20
            Expanded -> 0
        }

    val height: Int
        get() = when (this) {
            Small -> 300
            Medium -> 500
            Big -> 700
            Biggest -> 900
            Expanded -> 0
        }

}
