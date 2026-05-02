package it.fast4x.riplay.extensions.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidConnectivityObserverLegacy(context: Context) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    val networkStatus = _networkStatus.receiveAsFlow()

    private val internetNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _networkStatus.trySend(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _networkStatus.trySend(false)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            _networkStatus.trySend(false)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            // add Internet capability to request
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, internetNetworkCallback)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(internetNetworkCallback)
    }
}

