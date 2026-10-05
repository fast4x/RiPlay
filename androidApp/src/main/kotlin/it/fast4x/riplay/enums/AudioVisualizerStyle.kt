package it.fast4x.riplay.enums

enum class AudioVisualizerStyle {
    NONE,
    SPECTRUM,
    LEVEL,
    ANIMATIONS;

    val displayName: String
        get() = when (this) {
            NONE -> "None"
            SPECTRUM -> "Spectrum"
            LEVEL -> "Level"
            ANIMATIONS -> "Animations"
        }
}