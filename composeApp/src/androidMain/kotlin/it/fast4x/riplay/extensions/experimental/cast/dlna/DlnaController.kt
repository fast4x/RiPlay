package it.fast4x.riplay.extensions.experimental.cast.dlna

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jupnp.UpnpService
import org.jupnp.model.action.ActionInvocation
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.message.header.UDADeviceTypeHeader
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDAServiceType
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.registry.DefaultRegistryListener
import org.jupnp.registry.Registry
import org.jupnp.support.avtransport.callback.Play
import org.jupnp.support.avtransport.callback.SetAVTransportURI
import org.jupnp.support.avtransport.callback.Stop
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class DlnaController(private val context: Context) {

    private var upnpService: UpnpService? = null
    private val listeners = mutableListOf<DefaultRegistryListener>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            upnpService = (binder as DlnaUpnpService.LocalBinder).service

            // Ri-registra i listener aggiunti prima del bind
            listeners.forEach { upnpService?.registry?.addListener(it) }

            // Avvia la ricerca non appena il servizio è pronto
            upnpService?.controlPoint?.search(
                UDADeviceTypeHeader(UDADeviceType("MediaRenderer"))
            )
        }

        override fun onServiceDisconnected(name: ComponentName) {
            upnpService = null
        }
    }

    val isServiceReady: Boolean get() = upnpService != null

    init {
        context.bindService(
            Intent(context, DlnaUpnpService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun searchRenderers(
        onFound: (RemoteDevice) -> Unit,
        onLost: (RemoteDevice) -> Unit
    ) {
        val listener = object : DefaultRegistryListener() {
            override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
                if (device.type.type == "MediaRenderer") onFound(device)
            }
            override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
                onLost(device)
            }
        }

        // Salva il listener — se il servizio non è ancora connesso
        // verrà registrato in onServiceConnected
        listeners.add(listener)
        upnpService?.registry?.addListener(listener)
    }

    fun addRendererManually(ipAddress: String, onFound: (RemoteDevice) -> Unit) {
        val service = upnpService ?: run {
            Timber.e( "DlnaController UPnP service non ancora connesso, riprova tra poco")
            return
        }

        val listener = object : DefaultRegistryListener() {
            override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
                val deviceIp = device.identity.descriptorURL?.host
                if (deviceIp == ipAddress) {
                    onFound(device)
                    registry.removeListener(this)
                }
            }
        }
        service.registry?.addListener(listener)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = 5000

                val msearch = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: $ipAddress:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n" +
                        "\r\n"

                val data = msearch.toByteArray()
                val packet = DatagramPacket(
                    data,
                    data.size,
                    InetAddress.getByName(ipAddress),
                    1900
                )
                socket.send(packet)

                val responseBuffer = ByteArray(1024)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)

                val response = String(responsePacket.data, 0, responsePacket.length)
                Timber.d( "DlnaController Risposta SSDP: $response")
                socket.close()

            } catch (e: SocketTimeoutException) {
                Timber.e("DlnaController Nessuna risposta da $ipAddress — dispositivo non raggiungibile o non è un renderer DLNA")
                service.registry?.removeListener(listener)
            } catch (e: Exception) {
                Timber.e( "DlnaController Errore M-SEARCH: ${e.message}")
                service.registry?.removeListener(listener)
            }
        }
    }

    fun castTo(renderer: RemoteDevice, audioUrl: String) {
        val service = upnpService ?: run {
            Timber.e( "DlnaController UPnP service non ancora connesso")
            return
        }
        val avTransport = renderer.findService(UDAServiceType("AVTransport")) ?: return

        service.controlPoint?.execute(object : SetAVTransportURI(
            INSTANCE_ID,
            avTransport,
            audioUrl,
            buildDIDLMetadata(audioUrl)
        ) {
            override fun success(invocation: ActionInvocation<*>) {
                service.controlPoint?.execute(object : Play(INSTANCE_ID,avTransport,  "1") {
                    override fun failure(i: ActionInvocation<*>, o: UpnpResponse?, d: String?) {
                        Timber.e("DlnaController Play failed: $d")
                    }
                })
            }
            override fun failure(i: ActionInvocation<*>, o: UpnpResponse?, d: String?) {
                Timber.e( "DlnaController SetAVTransportURI failed: $d")
            }
        })
    }

    fun stop(renderer: RemoteDevice) {
        val service = upnpService ?: return
        val avTransport = renderer.findService(UDAServiceType("AVTransport")) ?: return
        service.controlPoint?.execute(object : Stop(INSTANCE_ID,avTransport) {
            override fun failure(i: ActionInvocation<*>, o: UpnpResponse?, d: String?) {
                Timber.e( "DlnaController Stop failed: $d")
            }
        })
    }

    fun destroy() {
        upnpService?.registry?.listeners?.clear()
        listeners.clear()
        try {
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.w( "DlnaController Service già scollegato")
        }
        upnpService = null
    }

    private companion object {
        val INSTANCE_ID = UnsignedIntegerFourBytes(0)
    }

    private fun buildDIDLMetadata(url: String): String {
        return """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
            <item id="1" parentID="0" restricted="1">
                <dc:title>Audio Cast</dc:title>
                <upnp:class>object.item.audioItem.musicTrack</upnp:class>
                <res protocolInfo="http-get:*:audio/wav:*">$url</res>
            </item>
        </DIDL-Lite>"""
    }
}
