package it.fast4x.riplay.cast.upnp

import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.riplay.cast.upnp.models.UpnpConnectionStatus
import it.fast4x.riplay.cast.upnp.models.UpnpDevice
import it.fast4x.riplay.cast.upnp.models.UpnpPlayerState
import it.fast4x.riplay.cast.upnp.models.UpnpRemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Client cast UPnP/DLNA — controparte di RiTuneCastClient per dispositivi
 * Sonos/DLNA: stessa interfaccia (connectionStatus, state, sendCommand,
 * disconnect), ma il trasporto e' SOAP/AVTransport invece di WebSocket JSON.
 *
 * - startConnection valida il dispositivo con un GetTransportInfo e apre
 *   il ciclo comandi + il loop di polling dello stato (1,2s)
 * - sendCommand accoda i comandi ("load", "play", "pause", "seek", "setvolume")
 * - il loop di polling aggiorna [state] da GetTransportInfo/GetPositionInfo;
 *   3 fallimenti SOAP consecutivi = dispositivo perso -> Error
 */
class UpnpCastClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandChannel = Channel<UpnpRemoteCommand>(Channel.UNLIMITED)

    private var device: UpnpDevice? = null
    private var transport: AvTransportClient? = null
    private var commandJob: Job? = null
    private var pollJob: Job? = null

    private val _state = MutableStateFlow<UpnpPlayerState?>(null)
    val state: StateFlow<UpnpPlayerState?> = _state.asStateFlow()

    private val _connectionStatus = MutableStateFlow<UpnpConnectionStatus>(UpnpConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<UpnpConnectionStatus> = _connectionStatus.asStateFlow()

    // ─────────────────────────── Ciclo di vita ───────────────────────────

    /** Connette il client al renderer. Il polling continua finche' non arriva disconnect(). */
    suspend fun startConnection(target: UpnpDevice) {
        _connectionStatus.value = UpnpConnectionStatus.Connecting
        Timber.d("UpnpCastClient startConnection -> ${target.name} (${target.avTransportControlUrl})")

        device = target
        transport = AvTransportClient(target)

        // Validazione: un GetTransportInfo che risponde = dispositivo vivo e raggiungibile
        val handshake = transport!!.getTransportInfo()
        if (handshake.isFailure) {
            val message = handshake.exceptionOrNull()?.message ?: "Device not responding"
            Timber.w("UpnpCastClient startConnection FAILED: $message")
            _connectionStatus.value = UpnpConnectionStatus.Error(message)
            transport = null
            return
        }

        _connectionStatus.value = UpnpConnectionStatus.Connected
        _state.value = UpnpPlayerState()

        commandJob?.cancel()
        commandJob = scope.launch { processCommands() }
        startPolling()
        Timber.d("UpnpCastClient connected to ${target.name}")
    }

    suspend fun sendCommand(cmd: UpnpRemoteCommand) {
        commandChannel.send(cmd)
    }

    /**
     * Chiude la sessione di cast. Il contenuto resta in riproduzione sul
     * dispositivo (comportamento atteso da un cast), come per RiTune.
     */
    suspend fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        commandJob?.cancel()
        commandJob = null
        transport = null
        _connectionStatus.value = UpnpConnectionStatus.Disconnected
        Timber.d("UpnpCastClient disconnected")
    }

    // ─────────────────────────── Comandi ───────────────────────────

    private suspend fun processCommands() {
        val client = transport ?: return
        for (cmd in commandChannel) {
            try {
                when (cmd.action) {
                    "load" -> {
                        val url = cmd.url
                        if (url.isNullOrBlank()) {
                            Timber.w("UpnpCastClient load without url, ignored")
                            continue
                        }
                        client.setAvTransportUri(url, cmd.title, cmd.artist)
                            .onSuccess {
                                val at = cmd.position ?: 0f
                                if (at > 1f) client.seek(at)
                                client.play()
                            }
                            .onFailure {
                                Timber.w("UpnpCastClient load failed: ${it.message}")
                            }
                    }

                    "play" -> {
                        client.play().onFailure { Timber.w("UpnpCastClient play failed: ${it.message}") }
                        val at = cmd.position ?: 0f
                        if (at > 1f) client.seek(at)
                    }

                    "pause" -> client.pause().onFailure { Timber.w("UpnpCastClient pause failed: ${it.message}") }
                    "stop" -> client.stop().onFailure { Timber.w("UpnpCastClient stop failed: ${it.message}") }
                    "seek" -> cmd.position?.let { client.seek(it).onFailure { e -> Timber.w("UpnpCastClient seek failed: ${e.message}") } }
                    "setvolume" -> cmd.volume?.let { client.setVolume(it).onFailure { e -> Timber.w("UpnpCastClient setVolume failed: ${e.message}") } }
                    else -> Timber.w("UpnpCastClient unknown action ${cmd.action}")
                }
            } catch (e: Exception) {
                Timber.w("UpnpCastClient command ${cmd.action} error: ${e.message}")
            }
        }
    }

    // ─────────────────────────── Polling stato ───────────────────────────

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var consecutiveFailures = 0
            while (true) {
                val client = transport ?: break
                val transportResult = client.getTransportInfo()
                val positionResult = client.getPositionInfo()

                if (transportResult.isFailure && positionResult.isFailure) {
                    consecutiveFailures++
                    Timber.w("UpnpCastClient poll failure $consecutiveFailures/3")
                    if (consecutiveFailures >= 3) {
                        _connectionStatus.value = UpnpConnectionStatus.Error("Device lost")
                        Timber.w("UpnpCastClient device lost after repeated poll failures")
                        break
                    }
                } else {
                    consecutiveFailures = 0
                    val rawTransport = transportResult.getOrNull() ?: "STOPPED"
                    val position = positionResult.getOrNull() ?: UpnpPositionInfo()
                    val mapped = mapTransportState(rawTransport, position.relTimeSeconds, position.durationSeconds)
                    _state.value = UpnpPlayerState(
                        isPlaying = rawTransport.equals("PLAYING", ignoreCase = true),
                        currentTime = position.relTimeSeconds,
                        duration = position.durationSeconds,
                        title = position.title,
                        state = mapped,
                    )
                }
                delay(1200)
            }
        }
    }

    /**
     * Mappa lo stato di trasporto UPnP su PlayerState (ayp), lo stesso tipo che
     * PlayerService consuma per RiTune. Attenzione al caso STOPPED: da Sonos
     * significa o "pausa da stop manuale" o "brano finito"; se la posizione
     * e' vicina alla durata lo interpretiamo come fine brano per far
     * avanzare la coda (stesso effetto dell'ENDED dell'iframe player).
     */
    private fun mapTransportState(raw: String, relTime: Float, duration: Float): PlayerConstants.PlayerState =
        when (raw.uppercase().trim()) {
            "PLAYING" -> PlayerConstants.PlayerState.PLAYING
            "TRANSITIONING" -> PlayerConstants.PlayerState.BUFFERING
            "PAUSED_PLAYBACK" -> PlayerConstants.PlayerState.PAUSED
            "STOPPED" ->
                if (duration > 0f && relTime > 0f && relTime >= duration - 2f)
                    PlayerConstants.PlayerState.ENDED
                else
                    PlayerConstants.PlayerState.PAUSED
            "NO_MEDIA_PRESENT" -> PlayerConstants.PlayerState.UNSTARTED
            else -> PlayerConstants.PlayerState.UNKNOWN
        }
}
