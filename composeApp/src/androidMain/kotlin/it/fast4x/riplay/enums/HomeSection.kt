package it.fast4x.riplay.enums

enum class HomeSection {
    Home,
    ForYou,
    Other;

    val textName: String
        get() = when(this) {
            Home -> "Home"
            ForYou -> "For You"
            Other -> "Other"
        }
}