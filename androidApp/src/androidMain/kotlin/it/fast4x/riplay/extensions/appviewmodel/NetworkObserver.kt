package it.fast4x.riplay.extensions.appviewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkConnectivity
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkType
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.isAtLeastAndroid6
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


fun observeNetworkType(context: Context): Flow<NetworkConnectivity> = callbackFlow {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    trySend(getNetworkConnectivity(context))

    if (isAtLeastAndroid6) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            private val validatedNetworks = mutableSetOf<Network>()

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                validatedNetworks.add(network)
                trySend(NetworkConnectivity.Connected(capabilities.toNetworkType()))
            }

            override fun onLost(network: Network) {
                validatedNetworks.remove(network)
                if (validatedNetworks.isEmpty()) {
                    trySend(NetworkConnectivity.Disconnected)
                } else {

                    val activeCapabilities = connectivityManager
                        .getNetworkCapabilities(validatedNetworks.last())
                    trySend(
                        if (activeCapabilities != null)
                            NetworkConnectivity.Connected(activeCapabilities.toNetworkType())
                        else
                            NetworkConnectivity.Disconnected
                    )
                }
            }

            override fun onUnavailable() {
                validatedNetworks.clear()
                trySend(NetworkConnectivity.Disconnected)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }

    } else {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(getNetworkConnectivity(ctx))
            }
        }
        @Suppress("DEPRECATION")
        context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        awaitClose { context.unregisterReceiver(receiver) }
    }
}

private fun getNetworkConnectivity(context: Context): NetworkConnectivity {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (isAtLeastAndroid6) {
        val network = connectivityManager.activeNetwork ?: return NetworkConnectivity.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkConnectivity.Disconnected
        return NetworkConnectivity.Connected(capabilities.toNetworkType())
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return if (networkInfo?.isConnected == true)
            NetworkConnectivity.Connected(NetworkType.UNKNOWN)
        else
            NetworkConnectivity.Disconnected
    }
}

fun isNetworkConnected(): Boolean {
    val app = globalContext() as MainApplication
    return app.networkConnectivity.value is NetworkConnectivity.Connected
}

private fun NetworkCapabilities.toNetworkType(): NetworkType = when {
    hasTransport(TRANSPORT_WIFI)      -> NetworkType.WIFI
    hasTransport(TRANSPORT_CELLULAR)  -> NetworkType.CELLULAR
    hasTransport(TRANSPORT_ETHERNET)  -> NetworkType.ETHERNET
    hasTransport(TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
    else                              -> NetworkType.UNKNOWN
}

fun NetworkType.toIcon(): Int = when (this) {
    NetworkType.WIFI      -> R.drawable.datawifi
    NetworkType.CELLULAR  -> R.drawable.datamobile
    NetworkType.ETHERNET  -> R.drawable.dataethernet
    NetworkType.BLUETOOTH -> R.drawable.bluetooth
    NetworkType.UNKNOWN   -> R.drawable.alert_circle_not_filled
}