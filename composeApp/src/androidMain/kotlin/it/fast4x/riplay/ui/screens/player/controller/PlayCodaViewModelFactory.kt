package it.fast4x.riplay.ui.screens.player.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class PlayerCodaViewModelFactory(
    private val codaIniziale: List<MediaItemGenerico>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerCodaViewModel::class.java)) {
            return PlayerCodaViewModel(codaIniziale) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}