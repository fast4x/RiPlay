package it.fast4x.riplay.extensions.experimental.cast.dlna

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jupnp.model.meta.RemoteDevice
import timber.log.Timber

class DlnaCastViewModel : ViewModel() {

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices: StateFlow<List<RemoteDevice>> = _devices.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castingTo = MutableStateFlow<RemoteDevice?>(null)
    val castingTo: StateFlow<RemoteDevice?> = _castingTo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var dlnaController: DlnaController? = null

    private val _requestProjectionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestProjectionEvent: SharedFlow<Unit> = _requestProjectionEvent.asSharedFlow()

    val isServiceReady: StateFlow<Boolean> = flow {
        while (true) {
            emit(dlnaController?.isServiceReady ?: false)
            delay(500)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Chiamato dalla UI quando l'utente preme "Cast"
    fun onCastRequested(device: RemoteDevice) {
        _castingTo.value = device  // salva il device in attesa del grant
        _requestProjectionEvent.tryEmit(Unit)
    }

    // Chiamato dalla CastScreen dopo che l'utente ha accettato il dialog
    fun onProjectionGranted(context: Context, resultCode: Int, data: Intent) {
        val device = _castingTo.value ?: return
        viewModelScope.launch {
            try {
                _error.value = null

                val intent = Intent(context, DlnaCastService::class.java).apply {
                    putExtra("projection_data", data)
                    putExtra("projection_code", resultCode)
                }
                context.startForegroundService(intent)

                val localIp = getLocalIpAddress(context)
                    ?: run { _error.value = "WiFi non disponibile"; return@launch }

                delay(300)
                dlnaController?.castTo(device, "http://$localIp:8765/stream.wav")

                _isCasting.value = true

            } catch (e: Exception) {
                _error.value = "Errore cast: ${e.message}"
                _isCasting.value = false
                _castingTo.value = null
            }
        }
    }

    // Chiamato una volta sola dal Composable tramite LaunchedEffect
    fun init(context: Context) {
        dlnaController = DlnaController(context).also { controller ->
            controller.searchRenderers(
                onFound = { device ->
                    _devices.update { current ->
                        if (current.none { it.identity.udn == device.identity.udn })
                            current + device
                        else current
                    }
                },
                onLost = { device ->
                    _devices.update { current ->
                        current.filter { it.identity.udn != device.identity.udn }
                    }
                    // Se stava facendo cast su quel dispositivo, resetta lo stato
                    if (_castingTo.value?.identity?.udn == device.identity.udn) {
                        _isCasting.value = false
                        _castingTo.value = null
                    }
                }
            )
        }
    }

    // CastViewModel
    fun addRendererManually(ipAddress: String) {
        dlnaController?.addRendererManually(ipAddress) { device ->
            _devices.update { current ->
                if (current.none { it.identity.udn == device.identity.udn })
                    current + device
                else current
            }
        }
    }

    fun startCast(context: Context, device: RemoteDevice) {
        if (_isCasting.value) stopCast()

        viewModelScope.launch {
            try {
                _error.value = null

                // Avvia il servizio foreground di cattura audio
                val intent = Intent(context, DlnaCastService::class.java)
                context.startForegroundService(intent)

                // Ottieni l'IP locale e costruisci l'URL
                val localIp = getLocalIpAddress(context)
                    ?: run {
                        _error.value = "WiFi non disponibile"
                        return@launch
                    }
                val audioUrl = "http://$localIp:8765/stream.wav"

                // Dai il tempo al server HTTP di avviarsi
                delay(300)

                dlnaController?.castTo(device, audioUrl)

                _castingTo.value = device
                _isCasting.value = true

            } catch (e: Exception) {
                _error.value = "Errore cast: ${e.message}"
                _isCasting.value = false
                _castingTo.value = null
            }
        }
    }

    fun stopCast() {
        viewModelScope.launch {
            try {
                _castingTo.value?.let { dlnaController?.stop(it) }
            } catch (e: Exception) {
                Timber.e( "CastViewModel Stop error $e", )
            } finally {
                _isCasting.value = false
                _castingTo.value = null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        if (ip == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    override fun onCleared() {
        stopCast()
        dlnaController?.destroy()
        super.onCleared()
    }
}