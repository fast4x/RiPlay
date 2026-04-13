package it.fast4x.riplay.extensions.experimental.cast.dlna

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import timber.log.Timber


class LocalUpnpService : Service() {

    private lateinit var upnpService: UpnpService
    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: UpnpService get() = upnpService
    }

    override fun onCreate() {
        super.onCreate()

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        val ip = wifiManager.connectionInfo.ipAddress
        val androidIp = String.format("%d.%d.%d.%d",
            ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        Timber.d( "DLNA Android IP: $androidIp")

        // MulticastLock — necessario per ricevere i pacchetti SSDP di discovery
        multicastLock = wifiManager.createMulticastLock("dlna_multicast").apply {
            setReferenceCounted(true)
            acquire()
        }

        // WifiLock — evita che il WiFi vada in sleep durante lo streaming
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "dlna_wifi"
        ).apply { acquire() }

        upnpService = UpnpServiceImpl()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        upnpService.shutdown()

        multicastLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()

        super.onDestroy()
    }
}