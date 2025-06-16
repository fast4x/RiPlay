package it.fast4x.riplay.enums

enum class SearchType {
    Online,
    Library,
    Gotolink;

    val index: Int
        get() = when (this) {
            Online -> 0
            Library -> 1
            Gotolink -> 2
        }
}
