package it.fast4x.riplay.utils

import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.environment.utils.getProxy
import okhttp3.OkHttpClient

fun okHttpClient() : OkHttpClient =
    ProxyPreferences.preference?.let{
        OkHttpClient.Builder()
            .proxy(
                getProxy(it)
            )
            .build()
    } ?: OkHttpClient.Builder().build()

