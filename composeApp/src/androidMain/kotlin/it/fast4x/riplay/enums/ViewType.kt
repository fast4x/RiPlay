package it.fast4x.riplay.enums

enum class ViewType {
    List,
    Grid;

    val displayName: String
        get() = when (this) {
            List -> "List"
            Grid -> "Grid"
        }
}