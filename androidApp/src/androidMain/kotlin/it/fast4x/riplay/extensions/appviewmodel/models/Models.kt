package it.fast4x.riplay.extensions.appviewmodel.models

data class NetworkState(
    val isConnected: Boolean = false,
    val networkType: NetworkType? = null  // null quando disconnesso
)

enum class NetworkType {
    UNKNOWN,    // rete presente ma trasporto non riconosciuto
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH
}

sealed class NetworkConnectivity {
    object Disconnected : NetworkConnectivity()
    data class Connected(val type: NetworkType) : NetworkConnectivity()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    object SessionExpired : UiEvent()
}