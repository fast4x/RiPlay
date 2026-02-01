package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable

enum class EqualizerType {
    Internal,
    System;

    val textName: String
        @Composable
        get() = when(this) {
            Internal -> "Internal"
            System -> "System"
        }
}