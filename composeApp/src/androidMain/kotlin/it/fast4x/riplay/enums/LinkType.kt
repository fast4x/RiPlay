package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable

enum class LinkType {
    Main,
    Alternative;

    val textName: String
        @Composable
        get() = when( this ) {
            Main -> "Main link"
            Alternative -> "Alternative link"
        }
}