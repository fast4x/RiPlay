package it.fast4x.riplay.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.environment.utils.getProxy
import it.fast4x.riplay.config.EnvironmentConfig
import it.fast4x.riplay.enums.NetworkType
import okhttp3.OkHttpClient


fun getNetworkType(context: Context): NetworkType {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Android M (API 23+) and up
    if (isAtLeastAndroid6) {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
            else -> NetworkType.UNKNOWN
        }
    } else {
        // Prior Android M
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo

        @Suppress("DEPRECATION")
        return if (networkInfo?.isConnected == true) {
            when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                ConnectivityManager.TYPE_BLUETOOTH -> NetworkType.BLUETOOTH
                else -> NetworkType.UNKNOWN
            }
        } else {
            NetworkType.NONE
        }
    }
}

fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Android M (API 23+) and up
    if (isAtLeastAndroid6) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check valid internet connection not blocked by captive portal)
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } else {
        // Prior Android M
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return networkInfo?.isConnected == true
    }
}


object CustomHttpClient {
    private val _proxy = ProxyPreferences.preference?.let { getProxy(it) }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxy(_proxy)
            .build()
    }

    val httpClient: HttpClient by lazy {
        HttpClient() {
            install(UserAgent) {
                agent = EnvironmentConfig.env_WkUFhXtC3G
            }
            engine {
                proxy?.let { _proxy }
            }
        }
    }
}

//fun okHttpClient() : OkHttpClient =
//    ProxyPreferences.preference?.let{
//        OkHttpClient.Builder()
//            .proxy(
//                getProxy(it)
//            )
//            .build()
//    } ?: OkHttpClient.Builder().build()