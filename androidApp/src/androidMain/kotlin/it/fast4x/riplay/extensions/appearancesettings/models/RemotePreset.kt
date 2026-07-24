package it.fast4x.riplay.extensions.appearancesettings.models

import kotlinx.serialization.Serializable

data class RemotePreset(
    val id: String,
    val name: String,
    val author: String,
    val imageUrl: String,
    val shareString: String
)

@Serializable
data class RemoteThemesResponse(
    // Se la chiave "themes" non esiste nel JSON, tornerà una lista vuota di default. Zero crash.
    val themes: List<it.fast4x.riplay.extensions.appearancesettings.models.AppearancePresetDto> = emptyList()
)