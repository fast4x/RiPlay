package it.fast4x.riplay.extensions.experimental.appearancesettings.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AppearancePreset(
    val id: String,
    val name: String,
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val author: String? = null,
    val shareUrl: String? = null,
    val source: PresetSource = PresetSource.BUILTIN,
    val settings: AppearanceSettings
)

@Serializable
data class AppearancePresetDto(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val author: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val settings: AppearanceSettings
)