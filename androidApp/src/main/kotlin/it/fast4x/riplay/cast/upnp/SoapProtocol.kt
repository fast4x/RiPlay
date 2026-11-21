package it.fast4x.riplay.cast.upnp

/**
 * Protocollo SOAP/UPnP — costruzione envelope e parsing risposte.
 *
 * Oggetto PURO (solo JVM, zero dipendenze Android): e' la logica di protocollo
 * testabile isolatamente contro un renderer fittizio o risposte registrate.
 *
 * Riferimento protocollo: AVTransport e RenderingControl sono i due servizi
 * esposti da qualunque renderer UPnP/DLNA (Sonos incluso) sulla porta 1400.
 */
object SoapProtocol {

    const val AV_TRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"
    const val RENDERING_CONTROL_SERVICE = "urn:schemas-upnp-org:service:RenderingControl:1"

    // ─────────────────────────── Envelope ───────────────────────────

    /**
     * Costruisce l'envelope SOAP per un'azione UPnP.
     * [argsXml] contiene gli argomenti gia' in forma di nodi <ArgName>value</ArgName>.
     */
    fun buildEnvelope(serviceType: String, action: String, argsXml: String): String =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body>" +
                "<u:$action xmlns:u=\"$serviceType\">$argsXml</u:$action>" +
                "</s:Body></s:Envelope>"

    /** Valore dell'header SOAPACTION per l'azione richiesta. */
    fun soapActionHeader(serviceType: String, action: String): String = "\"$serviceType#$action\""

    // ─────────────────────────── Metadata DIDL ───────────────────────────

    /**
     * Metadata DIDL-Lite per SetAVTransportURI: senza, Sonos e altri renderer
     * mostrano "Unknown source" invece del titolo del brano.
     */
    fun buildDidlMetadata(title: String?, artist: String?): String {
        val t = xmlEscape(title?.takeIf { it.isNotBlank() } ?: "RiPlay")
        val artistNode = artist?.takeIf { it.isNotBlank() }
            ?.let { "<upnp:artist>${xmlEscape(it)}</upnp:artist>" }
            ?: ""
        return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
                "<dc:title>$t</dc:title>$artistNode" +
                "<upnp:class>object.item.audioItem.musicTrack</upnp:class>" +
                "</item></DIDL-Lite>"
    }

    // ─────────────────────────── Parsing risposte ───────────────────────────

    /** Estrae <CurrentTransportState> da GetTransportInfo (es. PLAYING, STOPPED...). */
    fun parseTransportState(body: String): String? =
        extractTag(body, "CurrentTransportState")

    /** Estrae <errorCode>/<errorDescription> da una risposta SOAP fault. */
    fun parseUpnpError(body: String): String? {
        val code = extractTag(body, "errorCode")
        val description = extractTag(body, "errorDescription")
        return if (code != null || description != null)
            listOfNotNull(code?.let { "code $it" }, description).joinToString(" - ")
        else null
    }

    fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    /**
     * Unescape inverso di [xmlEscape]. Le entita' &amp; vengono risolte per ULTIME,
     * cosi' il doppio livello di escaping (DIDL dentro TrackMetaData) si apre
     * correttamente in due passate: "&amp;amp;" -&gt; "&amp;" (liv.1) -&gt; "&" (liv.2).
     */
    fun xmlUnescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    /**
     * Converte "H:MM:SS" (o "H:MM:SS.mmm", formato usato da RelTime/TrackDuration
     * nei renderer UPnP) in secondi. Accetta anche "MM:SS". Null se non interpretabile.
     */
    fun parseTimeToSeconds(value: String?): Float? {
        if (value.isNullOrBlank()) return null
        val parts = value.trim().split(":")
        if (parts.size !in 2..3) return null
        val intParts = parts.map {
            it.substringBefore('.').trim().toIntOrNull() ?: return null
        }
        val totalSeconds = when (intParts.size) {
            3 -> intParts[0] * 3600 + intParts[1] * 60 + intParts[2]
            else -> intParts[0] * 60 + intParts[1]
        }
        return totalSeconds.toFloat()
    }

    /** Formatta secondi in "H:MM:SS", formato richiesto dall'azione Seek (REL_TIME). */
    fun formatTime(seconds: Float): String {
        val s = seconds.toInt().coerceAtLeast(0)
        return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }

    // ─────────────────────────── Interni ───────────────────────────

    internal fun extractTag(body: String, tag: String): String? {
        val match = Regex("<$tag[^>]*>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
            .find(body) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }
}
