package it.fast4x.musicbrainz.utils

import java.net.InetSocketAddress
import java.net.Proxy

object ProxyPreferences {
    var preference: ProxyPreferenceItem? = null
}

data class ProxyPreferenceItem(
    var proxyHost: String,
    var proxyPort: Int,
    var proxyMode: java.net.Proxy.Type
)

fun getProxy(proxyPreference: ProxyPreferenceItem): java.net.Proxy {
    return if(proxyPreference.proxyMode == java.net.Proxy.Type.DIRECT) {
        java.net.Proxy.NO_PROXY
    } else {
        java.net.Proxy(
            proxyPreference.proxyMode,
            java.net.InetSocketAddress(proxyPreference.proxyHost, proxyPreference.proxyPort)
        )
    }
}
