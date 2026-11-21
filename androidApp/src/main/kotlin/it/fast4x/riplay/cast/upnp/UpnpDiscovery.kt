package it.fast4x.riplay.cast.upnp

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import it.fast4x.riplay.cast.upnp.models.UpnpDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.HttpURLConnection

/**
 * Discovery UPnP/DLNA via SSDP (UDP multicast su 239.255.255.250:1900).
 *
 * Invia un M-SEARCH per i renderer (MediaRenderer v1/v2 e, come rete di sicurezza,
 * rootdevice), raccoglie le risposte con header LOCATION e scarica il device
 * description XML di ciascuno per ricavare friendlyName e controlURL dei servizi
 * AVTransport/RenderingControl. Funziona con Sonos e con qualsiasi DLNA.
 *
 * N.B. l'M-SEARCH va inviato sulla stessa rete Wi-Fi del dispositivo cercato;
 * in caso di router che filtrano il multicast, usare [buildManualDevice].
 */
class UpnpDiscovery(private val context: Context? = null) {

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val MSEARCH_TEMPLATE =
            "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: %s\r\n" +
                    "\r\n"

        private val SEARCH_TARGETS = listOf(
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "urn:schemas-upnp-org:device:MediaRenderer:2",
            // rete di sicurezza: rispondono tutti i device UPnP; i non-renderer
            // vengono scartati in fase di parsing (nessun servizio AVTransport)
            "upnp:rootdevice",
        )
    }

    // ─────────────────────────── Discovery ───────────────────────────

    /**
     * Cerca renderer UPnP/DLNA sulla rete. Ritorna la lista deduplicata dei
     * dispositivi che espongono un servizio AVTransport.
     */
    suspend fun discover(timeoutMs: Int = 4500): List<UpnpDevice> =
        withContext(Dispatchers.IO) {
            val lock = acquireMulticastLock()
            try {
                val locations = sendMSearchAndCollectLocations(timeoutMs)
                Timber.d("UpnpDiscovery: ${locations.size} risposte SSDP -> $locations")
                locations.mapNotNull { fetchDeviceDescription(it) }
                    .distinctBy { it.udn ?: it.avTransportControlUrl }
            } catch (e: Exception) {
                Timber.w("UpnpDiscovery discover error: ${e.message}")
                emptyList()
            } finally {
                releaseMulticastLock(lock)
            }
        }

    /**
     * Fallback senza SSDP (router con filtro multicast / AP isolation):
     * prova a leggere la descrizione del dispositivo all'indirizzo dato.
     * Prova prima i path convenzionali di Sonos, poi quello generico UPnP;
     * come ultima opzione costruisece il device con l'endpoint standard Sonos.
     */
    suspend fun buildManualDevice(host: String, port: Int): UpnpDevice? =
        withContext(Dispatchers.IO) {
            val base = "http://$host:$port"
            listOf("/xml/device_description.xml", "/description.xml", "/devicedescription.xml")
                .firstNotNullOfOrNull { path -> fetchDeviceDescription("$base$path") }
                ?: run {
                    Timber.d("UpnpDiscovery manual: nessuna descrizione raggiungibile, uso endpoint Sonos standard")
                    UpnpDevice(
                        name = "UPnP $host",
                        host = host,
                        port = port,
                        avTransportControlUrl = "$base/MediaRenderer/AVTransport/Control",
                        renderingControlControlUrl = "$base/MediaRenderer/RenderingControl/Control",
                    )
                }
        }

    // ─────────────────────────── SSDP ───────────────────────────

    private fun sendMSearchAndCollectLocations(timeoutMs: Int): Set<String> {
        val found = mutableSetOf<String>()
        var socket: MulticastSocket? = null
        try {
            // Porta 1900: riceve anche le risposte che alcuni renderer mandano
            // in multicast al gruppo; se occupata (reuse fallito) si va su porta effimera
            // contando sulle risposte unicast, che sono la norma (Sonos incluso).
            socket = try {
                MulticastSocket(SSDP_PORT).apply { reuseAddress = true }
            } catch (_: Exception) {
                MulticastSocket(0)
            }
            socket.soTimeout = 500
            runCatching {
                socket.joinGroup(InetAddress.getByName(SSDP_ADDRESS))
            }
            val group = InetAddress.getByName(SSDP_ADDRESS)
            SEARCH_TARGETS.forEach { st ->
                val msearch = SsdpProtocol.buildMSearch(st)
                val packet = DatagramPacket(
                    msearch.toByteArray(Charsets.UTF_8),
                    msearch.length,
                    group,
                    SSDP_PORT,
                )
                runCatching { socket.send(packet) }
            }

            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(2048)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val response = DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    SsdpProtocol.extractLocation(String(response.data, 0, response.length, Charsets.UTF_8))
                        ?.let { found.add(it) }
                } catch (_: SocketTimeoutException) {
                    // finestra di attesa: si ripete fino allo scadere del deadline
                }
            }
        } catch (e: IOException) {
            Timber.w("UpnpDiscovery SSDP error: ${e.message}")
        } finally {
            socket?.close()
        }
        return found
    }

    // ─────────────────────────── Descrizione dispositivo ───────────────────────────

    /**
     * Scarica il device description XML dalla LOCATION SSDP e lo trasforma
     * in [UpnpDevice]. Null se non raggiungibile o se non e' un renderer
     * (nessun servizio AVTransport).
     */
    internal fun fetchDeviceDescription(location: String): UpnpDevice? {
        return try {
            val xml = httpGet(location)
            UpnpDescriptionParser.parseDeviceDescription(xml, location)
        } catch (e: Exception) {
            Timber.w("UpnpDiscovery descrizione non leggibile ($location): ${e.message}")
            null
        }
    }

    /** GET HTTP minimale senza dipendenze (usato anche in fase di test JVM). */
    internal fun httpGet(url: String, timeoutMs: Int = 4000): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "GET"
            val stream = if (connection.responseCode in 200..299)
                connection.inputStream
            else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("HTTP ${connection.responseCode} without body")
        } finally {
            connection.disconnect()
        }
    }

    // ─────────────────────────── Android: MulticastLock ───────────────────────────

    /**
     * Su Android il filo WiFi filtra i pacchetti multicast: senza MulticastLock
     * le risposte SSDP spesso non arrivano. Permesso CHANGE_WIFI_MULTICAST_STATE
     * gia' presente nel manifest.
     */
    @SuppressLint("WifiManagerLeak")
    private fun acquireMulticastLock(): WifiManager.MulticastLock? = runCatching {
        val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        wifi.createMulticastLock("riplay_upnp_discovery").apply {
            setReferenceCounted(false)
            acquire()
        }
    }.getOrNull()

    private fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {
        runCatching { lock?.release() }
    }
}

/**
 * Protocollo SSDP (oggetto puro, testabile su JVM senza Android).
 */
object SsdpProtocol {

    /** M-SEARCH valido per il search target richiesto. */
    fun buildMSearch(searchTarget: String, mx: Int = 3): String =
        "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: $mx\r\n" +
                "ST: $searchTarget\r\n" +
                "\r\n"

    /** Estrae l'header LOCATION (case-insensitive) da una risposta SSDP. */
    fun extractLocation(ssdpResponse: String): String? {
        val match = Regex("(?i)^LOCATION:\\s*(\\S+)\\s*$", RegexOption.MULTILINE)
            .find(ssdpResponse) ?: return null
        return match.groupValues[1]
    }
}

/**
 * Parser del device description XML (oggetto puro, testabile su JVM).
 *
 * Il description XML UPnP e' piccolo e ben formato: usiamo estrazione mirata
 * con regex invece di introdurre una dipendenza XML — i campi cercati sono
 * friendlyName, UDN e le controlURL dei servizi AVTransport/RenderingControl.
 */
object UpnpDescriptionParser {

    fun parseDeviceDescription(xml: String, locationUrl: String): UpnpDevice? {
        val friendlyName = extractTag(xml, "friendlyName") ?: "UPnP device"
        val udn = extractTag(xml, "UDN")
        val avTransport = findControlUrl(xml, "AVTransport") ?: return null
        val renderingControl = findControlUrl(xml, "RenderingControl")

        val base = URI(locationUrl)
        val avUrl = base.resolve(avTransport).toString()
        val rcUrl = renderingControl?.let { base.resolve(it).toString() }

        return UpnpDevice(
            name = friendlyName,
            host = base.host ?: "",
            port = base.port.takeIf { it > 0 } ?: 80,
            avTransportControlUrl = avUrl,
            renderingControlControlUrl = rcUrl,
            udn = udn,
        )
    }

    /**
     * Cerca il controlURL del servizio richiesto (AVTransport / RenderingControl).
     * I nomi di serviceType possono avere versione 1/2/3: cerchiamo per nome base.
     */
    internal fun findControlUrl(xml: String, serviceBaseName: String): String? {
        val services = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
        for (service in services) {
            val body = service.groupValues[1]
            val serviceType = extractTag(body, "serviceType") ?: continue
            if (serviceType.contains(":service:$serviceBaseName")) {
                extractTag(body, "controlURL")?.let { return it }
            }
        }
        return null
    }

    private fun extractTag(body: String, tag: String): String? {
        val match = Regex("<$tag[^>]*>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
            .find(body) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }
}
