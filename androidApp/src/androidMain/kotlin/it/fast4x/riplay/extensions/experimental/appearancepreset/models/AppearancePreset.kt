package it.fast4x.riplay.extensions.experimental.appearancepreset.models

data class AppearancePreset(
    val id: String,
    val name: String,
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val author: String? = null,
    val shareUrl: String? = null,
    val source: PresetSource = PresetSource.LOCAL,
    val settings: AppearanceSettings
)