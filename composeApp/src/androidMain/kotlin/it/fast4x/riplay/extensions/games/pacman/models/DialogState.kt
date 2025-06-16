package it.fast4x.riplay.extensions.games.pacman.models

import androidx.compose.runtime.MutableState

data class DialogState (
    val shouldShow: MutableState<Boolean>,
    val message: MutableState<String>,
)