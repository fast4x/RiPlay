package it.fast4x.riplay.extensions.audiotag.models

import it.fast4x.audiotaginfo.models.SongInfo
import it.fast4x.audiotaginfo.models.Track

sealed class UiState {
    object Idle : UiState()
    object Recording : UiState()
    object Loading : UiState()
    //data class Success(val audioTagSongInfo: SongInfo?) : UiState()
    data class Success(val tracks: List<Track>?) : UiState()
    data class Error(val message: String) : UiState()
    data class Response(val message: String) : UiState()
}