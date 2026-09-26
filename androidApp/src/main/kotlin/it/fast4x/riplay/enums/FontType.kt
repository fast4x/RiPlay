package it.fast4x.riplay.enums

enum class FontType {
    Rubik,
    Poppins;

    val displayName: String
        get() = when (this) {
            FontType.Rubik -> FontType.Rubik.name
            FontType.Poppins -> FontType.Poppins.name
        }
}