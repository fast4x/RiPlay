package it.fast4x.riplay.extensions.experimental.appearancepreset.models

sealed interface PresetUiState {
    data object Loading : PresetUiState
    data class Success(val presets: List<AppearancePreset>) : PresetUiState
    data class Error(val message: String) : PresetUiState
}