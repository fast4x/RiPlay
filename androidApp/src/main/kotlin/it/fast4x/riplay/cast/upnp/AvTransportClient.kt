package it.fast4x.riplay.cast.upnp

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import it.fast4x.riplay.cast.upnp.models.UpnpDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Esito di GetPositionInfo: posizione e durata in secondi + titolo del brano (se nel metadata). */
data class UpnpPositionInfo(
    val relTimeSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val title: String? = null,
)

/**
 * Client SOAP verso un renderer UPnP/DLNA: azioni AVTransport (trasporto) e
 * RenderingControl (volume).
 *
 * E' la controparte UPnP dei comandi WebSocket JSON di RiTuneCastClient:
 * ogni comando remoto finisce qui e diventa una chiamata SOAP sulla porta
 * UPnP del dispositivo (Sonos: 1400).
 */
class AvTransportClient(private val device: UpnpDevice) {

    // followRedirects = false e' una proprieta' pubblica di HttpClientConfig: disabilita i redirect
    // a livello Ktor. A livello OkHttp l'engine disabilita gia' i redirect di default
    // (OkHttpConfig: followRedirects(false) + followSslRedirects(false)).
    // NB: NON usare engine { config { followRedirects = ... } }: l'assegnazione var risolverebbe
    // sul 'var followRedirects' internal di OkHttpClient.Builder e non compila.
    private val client = HttpClient(OkHttp) {
        followRedirects = false
        expectSuccess = false // gestiamo gli errori SOAP manualmente dal body
    }

    // ─────────────────────────── Trasporto ───────────────────────────

    /**
     * SetAVTransportURI + eventuali Seek/Play sono gestiti dal chiamante:
     * qui si imposta solo la URI (il dispositivo entra in stato STOPPED/nominal).
     */
    suspend fun setAvTransportUri(url: String, title: String?, artist: String?): Result<Unit> =
        call("SetAVTransportURI") {
            arg("InstanceID", "0")
            arg("CurrentURI", SoapProtocol.xmlEscape(url))
            arg("CurrentURIMetadata", SoapProtocol.xmlEscape(SoapProtocol.buildDidlMetadata(title, artist)))
        }.map { }

    suspend fun play(): Result<Unit> =
        call("Play") {
            arg("InstanceID", "0")
            arg("Speed", "1")
        }.map { }

    suspend fun pause(): Result<Unit> =
        call("Pause") {
            arg("InstanceID", "0")
        }.map { }

    suspend fun stop(): Result<Unit> =
        call("Stop") {
            arg("InstanceID", "0")
        }.map { }

    /** Seek assoluto (REL_TIME), posizione in secondi. */
    suspend fun seek(positionSeconds: Float): Result<Unit> =
        call("Seek") {
            arg("InstanceID", "0")
            arg("Unit", "REL_TIME")
            arg("Target", SoapProtocol.formatTime(positionSeconds))
        }.map { }

    // ─────────────────────────── Volume ───────────────────────────

    suspend fun setVolume(volume: Int): Result<Unit> {
        val renderingControl = device.renderingControlControlUrl
            ?: return Result.failure(IllegalStateException("No RenderingControl endpoint on device"))
        return call("SetVolume", controlUrl = renderingControl, serviceType = SoapProtocol.RENDERING_CONTROL_SERVICE) {
            arg("InstanceID", "0")
            arg("Channel", "Master")
            arg("DesiredVolume", volume.coerceIn(0, 100).toString())
        }.map { }
    }

    // ─────────────────────────── Stato ───────────────────────────

    /** GetTransportInfo: stato di trasporto grezzo (PLAYING, PAUSED_PLAYBACK, STOPPED...). */
    suspend fun getTransportInfo(): Result<String> =
        call("GetTransportInfo") {
            arg("InstanceID", "0")
        }.mapCatching { body ->
            SoapProtocol.parseTransportState(body)
                ?: throw IllegalStateException("Missing CurrentTransportState in response")
        }

    /** GetPositionInfo: posizione/durata in secondi e titolo del brano. */
    suspend fun getPositionInfo(): Result<UpnpPositionInfo> =
        call("GetPositionInfo") {
            arg("InstanceID", "0")
        }.mapCatching { body ->
            UpnpPositionInfo(
                relTimeSeconds = SoapProtocol.parseTimeToSeconds(SoapProtocol.extractTag(body, "RelTime")) ?: 0f,
                durationSeconds = SoapProtocol.parseTimeToSeconds(SoapProtocol.extractTag(body, "TrackDuration")) ?: 0f,
                title = parseDidlTitle(SoapProtocol.extractTag(body, "TrackMetaData")),
            )
        }

    // ─────────────────────────── Motore SOAP ───────────────────────────

    private fun arg(name: String, value: String): String =
        "<$name>$value</$name>"

    private suspend fun call(
        action: String,
        controlUrl: String = device.avTransportControlUrl,
        serviceType: String = SoapProtocol.AV_TRANSPORT_SERVICE,
        args: () -> String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = SoapProtocol.buildEnvelope(serviceType, action, args())
            val response = client.post(controlUrl) {
                header("SOAPACTION", SoapProtocol.soapActionHeader(serviceType, action))
                header("Content-Type", "text/xml; charset=\"utf-8\"")
                setBody(envelope)
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val upnpError = SoapProtocol.parseUpnpError(body)
                val message = upnpError ?: "HTTP ${response.status.value}"
                Timber.w("AvTransportClient $action FAILED: $message")
                throw IllegalStateException("$action failed: $message")
            }
            body
        }.onFailure {
            Timber.w("AvTransportClient $action error: ${it.message}")
        }
    }

    /**
     * Recupera dc:title dal DIDL-Lite dentro TrackMetaData.
     * Il DIDL arriva XML-escaped dentro l'elemento: due livelli di unescape,
     * primo il wrapper (per rendere leggibile l'XML), poi il contenuto del titolo.
     */
    private fun parseDidlTitle(didl: String?): String? {
        if (didl.isNullOrBlank()) return null
        val wrapper = SoapProtocol.xmlUnescape(didl)
        val title = SoapProtocol.extractTag(wrapper, "dc:title")
            ?: SoapProtocol.extractTag(wrapper, "title")
            ?: return null
        return SoapProtocol.xmlUnescape(title)
    }
}
