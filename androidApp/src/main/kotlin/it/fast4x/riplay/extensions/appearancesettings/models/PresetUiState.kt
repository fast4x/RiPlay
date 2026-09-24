package it.fast4x.riplay.extensions.appearancesettings.models

sealed interface PresetUiState {
    data object Loading : PresetUiState
    data class Success(val presets: List<it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset>) : PresetUiState
    data class Error(val message: String) : PresetUiState
}