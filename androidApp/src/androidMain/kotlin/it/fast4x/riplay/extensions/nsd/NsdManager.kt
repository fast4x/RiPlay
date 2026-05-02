package it.fast4x.riplay.extensions.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import it.fast4x.riplay.utils.appContext
import timber.log.Timber
import java.net.Socket
import java.net.UnknownHostException

fun initializeNsd(): NsdManager {
    val context = appContext()
    val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    return nsdManager
}

fun registerNsdService() {

    val nsdManager = initializeNsd()

    val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            Timber.d("NsdManager.Registration Service registered")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Timber.d("NsdManager.Registration Registration failed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Timber.d("NsdManager.Registration Service unregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Timber.d("NsdManager.Registration Unregistration failed")
        }
    }

    // Create the NsdServiceInfo object, and populate it.
    val serviceInfo = NsdServiceInfo().apply {
        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceName = "RiPlayLinkApp"
        serviceType = "_RiPlayLinkApp._tcp."
        port = 8000
    }

// Register the service for discovery
    nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

}

fun discoverNsdServices(
    onServiceFound: (List<NsdServiceInfo>) -> Unit
) {

    val nsdManager = initializeNsd()

    var socket: Socket? = null

    var servicesDiscovered: List<NsdServiceInfo> = emptyList()

    val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Timber.e("NsdManager.resolveListener Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Timber.i("NsdManager.resolveListener Resolve Succeeded. $serviceInfo")

            socket?.let {
                Timber.i("NsdManager.resolveListener Socket already connected $it")
                return
            }

            try {
                // Connect to the host
                //socket = Socket(serviceInfo.host, serviceInfo.port)
                servicesDiscovered = servicesDiscovered + serviceInfo
                onServiceFound(servicesDiscovered)
                Timber.i("NsdManager.resolveListener Socket host ${serviceInfo.host}, port ${serviceInfo.port}")
            } catch (e: UnknownHostException) {
                Timber.e("NsdManager.resolveListener Unknown host. ${e.localizedMessage}")
            }
        }
    }

    val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Timber.d("NsdManager.discoveryListener Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Timber.d("NsdManager.discoveryListener Service found ${service.serviceName} try to resolve")
            nsdManager.resolveService(service, resolveListener)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Timber.e("NsdManager.discoveryListener Service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Timber.i("NsdManager.discoveryListener Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("NsdManager.discoveryListener Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("NsdManager.discoveryListener Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    nsdManager.discoverServices("_RiPlayLinkApp._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

}