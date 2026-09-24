package it.fast4x.riplay.extensions.lyricshelper.parsers

import java.io.BufferedInputStream
import java.io.File



enum class OggCodec { VORBIS, OPUS, UNKNOWN }

fun readOpusVorbisComments(file: File): Map<String, String> {
    return try {
        file.inputStream().buffered().use { stream ->
            // Salta il primo pacchetto OGG (OpusHead)
            skipOggPacket(stream) ?: return emptyMap()

            // Leggi il secondo pacchetto (OpusTags)
            val packet = readOggPacket(stream) ?: return emptyMap()

            parseVorbisComments(packet, magicBytes = "OpusTags".toByteArray())
        }
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun skipOggPacket(stream: BufferedInputStream) =
    readOggPacket(stream)



private fun readOggPacket(stream: BufferedInputStream): ByteArray? {
    val packetBytes = mutableListOf<Byte>()

    while (true) {
        // Header fisso OGG page: 27 byte
        val pageHeader = ByteArray(27)
        if (stream.read(pageHeader) < 27) return null

        // Verifica "OggS"
        if (pageHeader[0] != 0x4F.toByte() ||
            pageHeader[1] != 0x67.toByte() ||
            pageHeader[2] != 0x67.toByte() ||
            pageHeader[3] != 0x53.toByte()
        ) return null

        val headerType  = pageHeader[5].toInt() and 0xFF
        val numSegments = pageHeader[26].toInt() and 0xFF

        // Segment table: ogni byte indica la dimensione di un segmento
        val segmentTable = ByteArray(numSegments)
        if (stream.read(segmentTable) < numSegments) return null

        // Leggi tutti i segmenti della pagina
        for (segSize in segmentTable) {
            val size = segSize.toInt() and 0xFF
            val segment = ByteArray(size)
            if (stream.read(segment) < size) return null
            packetBytes.addAll(segment.toList())
        }

        // Se l'ultimo segmento è < 255 byte, il pacchetto è completo
        val lastSegment = segmentTable.last().toInt() and 0xFF
        if (lastSegment < 255) break

        // Se headerType indica continuation (bit 0 = 1), continua a leggere
        if (headerType and 0x01 == 0) break
    }

    return packetBytes.toByteArray()
}


private fun parseVorbisComments(packet: ByteArray, magicBytes: ByteArray): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var pos = magicBytes.size // Salta il magic header

    if (pos + 4 > packet.size) return result

    // Lunghezza vendor string (little-endian)
    val vendorLen = readInt32LE(packet, pos); pos += 4
    pos += vendorLen // Salta vendor string

    if (pos + 4 > packet.size) return result

    // Numero di commenti
    val commentCount = readInt32LE(packet, pos); pos += 4

    repeat(commentCount) {
        if (pos + 4 > packet.size) return result

        val commentLen = readInt32LE(packet, pos); pos += 4
        if (pos + commentLen > packet.size) return result

        val comment = String(packet, pos, commentLen, Charsets.UTF_8)
        pos += commentLen

        val eqIndex = comment.indexOf('=')
        if (eqIndex > 0) {
            val key   = comment.substring(0, eqIndex).uppercase()
            val value = comment.substring(eqIndex + 1)
            result[key] = value
        }
    }

    return result
}

private fun readInt32LE(data: ByteArray, offset: Int): Int =
    (data[offset].toInt()     and 0xFF)        or
            ((data[offset + 1].toInt() and 0xFF) shl 8)  or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
fun detectOggCodec(file: File): OggCodec {
    return try {
        file.inputStream().use { stream ->
            val header = ByteArray(64)
            if (stream.read(header) < 64) return OggCodec.UNKNOWN

            // Verifica "OggS"
            if (header[0] != 0x4F.toByte() || // O
                header[1] != 0x67.toByte() || // g
                header[2] != 0x67.toByte() || // g
                header[3] != 0x53.toByte()    // S
            ) return OggCodec.UNKNOWN

            // Salta l'header OGG page fino ai dati del pacchetto
            // offset 26: numero di entries nella segment table
            val numSegments = header[26].toInt() and 0xFF
            // I dati del pacchetto iniziano a: 27 + numSegments
            val packetStart = 27 + numSegments
            if (packetStart + 8 > header.size) return OggCodec.UNKNOWN

            val packet = header.copyOfRange(packetStart, header.size)
            val packetStr = String(packet, Charsets.ISO_8859_1)

            when {
                packetStr.startsWith("\u0001vorbis") -> OggCodec.VORBIS
                packetStr.startsWith("OpusHead")     -> OggCodec.OPUS
                else                                 -> OggCodec.UNKNOWN
            }
        }
    } catch (e: Exception) {
        OggCodec.UNKNOWN
    }
}

