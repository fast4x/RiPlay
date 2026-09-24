package it.fast4x.riplay.extensions.appviewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkConnectivity
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkState
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkType
import it.fast4x.riplay.extensions.appviewmodel.models.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    context: Context
) : ViewModel() {

    private val app = context.applicationContext as MainApplication
    val networkConnectivity: StateFlow<NetworkConnectivity> = app.networkConnectivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NetworkConnectivity.Disconnected
        )

    val isConnected: StateFlow<Boolean> = networkConnectivity
        .map { it is NetworkConnectivity.Connected }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val networkType: StateFlow<NetworkType?> = networkConnectivity
        .map { (it as? NetworkConnectivity.Connected)?.type }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null  // null = disconnesso
        )

    val networkState: StateFlow<NetworkState> = networkConnectivity
        .map { connectivity ->
            NetworkState(
                isConnected = connectivity is NetworkConnectivity.Connected,
                networkType = (connectivity as? NetworkConnectivity.Connected)?.type
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NetworkState()
        )

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

    fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun <T> launchWithLoading(
        onError: (Throwable) -> Unit = { sendEvent(UiEvent.Error(it.message ?: "Unknown error")) },
        block: suspend () -> T
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == AppViewModel::class.java)
                    return AppViewModel(context.applicationContext) as T
                }
            }
    }
}