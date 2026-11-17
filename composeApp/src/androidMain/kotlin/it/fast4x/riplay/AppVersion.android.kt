package it.fast4x.riplay

/**
 * APP UPDATE — registro del provider della versione installata per Android.
 *
 * composeApp (androidMain) non ha accesso diretto a un Context: androidApp
 * registra qui il provider (lettura PackageManager) in MainApplication.onCreate,
 * esattamente come fa AndroidDbRegistry per il builder del database.
 */
object AndroidAppVersionRegistry {

    @Volatile
    var provider: (() -> Long)? = null
}

actual fun installedVersionCode(): Long =
    AndroidAppVersionRegistry.provider?.invoke() ?: 0L
