package it.fast4x.riplay.cast.upnp.models

import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants

/**
 * UPnP/DLNA Cast — modelli dati.
 *
 * La struttura e' volutamente speculare a quella di RiTune
 * (cast.ritune.models.RiTuneData) perche' PlayerService, i selettori
 * di dispositivo e la UI seguono lo stesso schema di integrazione.
 */

/**
 * Un renderer UPnP/DLNA scoperto in rete (Sonos, smart TV, speaker DLNA...).
 *
 * @param name friendlyName dichiarato dal dispositivo
 * @param host indirizzo IP del dispositivo
 * @param port porta del server UPnP (Sonos: 1400)
 * @param avTransportControlUrl URL assoluto dell'endpoint SOAP AVTransport
 * @param renderingControlControlUrl URL assoluto dell'endpoint SOAP RenderingControl (volume)
 * @param udn identificativo univoco del dispositivo (se presente nella descrizione)
 * @param selected true se il dispositivo e' la destinazione del cast
 */
data class UpnpDevice(
    val name: String,
    val host: String,
    val port: Int,
    val avTransportControlUrl: String,
    val renderingControlControlUrl: String? = null,
    val udn: String? = null,
    var selected: Boolean = false,
)

/**
 * Stato riproduzione riportato dal renderer via polling SOAP
 * (GetTransportInfo + GetPositionInfo).
 */
data class UpnpPlayerState(
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val title: String? = null,
    val state: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNSTARTED
)

/**
 * Comandi che PlayerService/UI inviano al renderer.
 *
 * @param action "load", "play", "pause", "stop", "seek", "setvolume"
 * @param url URL stream diretto (solo per "load"; deve essere http/https
 *            raggiungibile dal dispositivo in rete locale)
 * @param title titolo del brano (metadata DIDL per il display del dispositivo)
 * @param artist artista del brano (metadata DIDL)
 * @param position posizione in secondi (solo per "load" e "seek")
 * @param volume volume 0..100 (solo per "setvolume")
 */
data class UpnpRemoteCommand(
    val action: String,
    val url: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val position: Float? = null,
    val volume: Int? = null,
)

sealed class UpnpConnectionStatus {
    object Disconnected : UpnpConnectionStatus()
    object Connecting : UpnpConnectionStatus()
    object Connected : UpnpConnectionStatus()
    data class Error(val message: String) : UpnpConnectionStatus()
}
