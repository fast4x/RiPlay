package it.fast4x.riplay.ui.screens.player.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.media3.common.Player

class PlayerCodaViewModel(
    private val codaIniziale: List<MediaItemGenerico>
) : ViewModel() {

    private var playerController: PlayerController? = null

    val codaRiproduzione: List<MediaItemGenerico> = codaIniziale

    private val _indiceCorrente = MutableStateFlow(0)
    val indiceCorrente: StateFlow<Int> = _indiceCorrente.asStateFlow()

    private val _inRiproduzione = MutableStateFlow(false)
    val inRiproduzione: StateFlow<Boolean> = _inRiproduzione.asStateFlow()

    private val _statoPlayer = MutableStateFlow(Player.STATE_IDLE)
    val statoPlayer: StateFlow<Int> = _statoPlayer.asStateFlow()

    fun associaController(controller: PlayerController) {
        this.playerController = controller

        if (codaRiproduzione.isNotEmpty()) {
            caricaBranoCorrente()
        }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, playbackState: Int) {
        _inRiproduzione.value = isPlaying
        _statoPlayer.value = playbackState

        if (playbackState == Player.STATE_ENDED) {
            vaiAlProssimo()
        }
    }

    private fun caricaBranoCorrente() {
        val controller = playerController ?: return
        if (codaRiproduzione.isNotEmpty()) {
            controller.caricaMedia(codaRiproduzione[_indiceCorrente.value])
        }
    }

    fun play() {
        playerController?.play()
    }

    fun pause() {
        playerController?.pause()
    }

    fun vaiAlProssimo() {
        if (_indiceCorrente.value < codaRiproduzione.size - 1) {
            _indiceCorrente.value += 1
            caricaBranoCorrente()
            play()
        } else {
            // Fine della coda
            _inRiproduzione.value = false
        }
    }

    fun vaiAlPrecedente() {
        if (_indiceCorrente.value > 0) {
            _indiceCorrente.value -= 1
            caricaBranoCorrente()
            play()
        }
    }

    fun vaiA(indice: Int) {
        if (indice in codaRiproduzione.indices) {
            _indiceCorrente.value = indice
            caricaBranoCorrente()
            play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController?.rilascia()
    }
}