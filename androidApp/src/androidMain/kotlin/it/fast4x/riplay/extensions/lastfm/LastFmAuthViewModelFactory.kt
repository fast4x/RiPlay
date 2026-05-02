package it.fast4x.riplay.extensions.lastfm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.fast4x.lastfm.LastFmService

class LastFmAuthViewModelFactory(
    private val lastFmService: LastFmService,
    private val onSaveSessionKey: (String) -> Unit
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LastFmAuthViewModel::class.java)) {
            return LastFmAuthViewModel(
                lastFmService = lastFmService,
                onSaveSessionKey = onSaveSessionKey
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}