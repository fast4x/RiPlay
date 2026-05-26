package it.fast4x.riplay.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.environment.utils.getProxy
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.config.EnvironmentConfig
import it.fast4x.riplay.enums.NetworkType
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

/*
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
*/


object CustomHttpClient {
    private val _proxy = ProxyPreferences.preference?.let { getProxy(it) }

    var clientCache: Cache? = Cache(
        directory = File(appContext().cacheDir, "http_cache"),
        maxSize = 50L * 1024L * 1024L // 50 MB
    )

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(clientCache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BASIC  // mai BODY in produzione
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .proxy(_proxy)
            .build()
    }

}
