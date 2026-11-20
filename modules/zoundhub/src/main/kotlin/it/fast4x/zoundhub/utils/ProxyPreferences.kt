package it.fast4x.zoundhub.utils

import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Preferenze proxy del modulo ZoundHub (stesso pattern degli altri moduli:
 * lastfm, listenbrainz, spotifymeta). L'app assegna `preference` all'avvio
 * (o quando cambia il proxy) e il service lo applica al client Ktor.
 */
object ProxyPreferences {
    var preference: ProxyPreferenceItem? = null
}

data class ProxyPreferenceItem(
    var proxyHost: String,
    var proxyPort: Int,
    var proxyMode: Proxy.Type
)

fun getProxy(proxyPreference: ProxyPreferenceItem): Proxy {
    return if (proxyPreference.proxyMode == Proxy.Type.DIRECT) {
        Proxy.NO_PROXY
    } else {
        Proxy(
            proxyPreference.proxyMode,
            InetSocketAddress(proxyPreference.proxyHost, proxyPreference.proxyPort)
        )
    }
}
