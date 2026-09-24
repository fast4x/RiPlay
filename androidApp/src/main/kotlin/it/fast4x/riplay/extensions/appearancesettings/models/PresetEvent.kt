package it.fast4x.riplay.extensions.appearancesettings.models

sealed interface PresetEvent {
    data class Applied(val presetName: String) : PresetEvent
    data class Shared(val shareUrl: String)    : PresetEvent
    data class Error(val message: String)      : PresetEvent
}