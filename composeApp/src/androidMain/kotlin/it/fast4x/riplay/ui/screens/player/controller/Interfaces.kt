package it.fast4x.riplay.ui.screens.player.controller

interface MediaItemGenerico {
    val uri: String
    val titolo: String?
    val artista: String?
}

interface PlayerController {
    fun play()
    fun pause()
    fun caricaMedia(item: MediaItemGenerico)
    fun ferma()
    fun rilascia()
}