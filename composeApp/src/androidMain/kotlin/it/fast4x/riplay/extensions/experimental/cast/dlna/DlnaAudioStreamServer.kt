package it.fast4x.riplay.extensions.experimental.cast.dlna

import fi.iki.elonen.NanoHTTPD
import kotlinx.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DlnaAudioStreamServer(port: Int) : NanoHTTPD(port) {

    // Buffer thread-safe tra produttore (AudioRecord) e consumatori HTTP
    private val audioQueue = LinkedBlockingQueue<ByteArray>(200)

    fun pushAudio(data: ByteArray) {
        // Droppa se il client non sta consumando abbastanza veloce
        if (audioQueue.size < 190) audioQueue.offer(data)
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.uri != "/stream.wav") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
        }

        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream, 65536)

        // Thread dedicato per scrivere il WAV
        Thread {
            try {
                // Scrivi l'header WAV con chunk size "infinito"
                outputStream.write(buildWavHeader(
                    sampleRate = 44100,
                    channels = 2,
                    bitsPerSample = 16
                ))

                // Stream continuo dei dati PCM
                while (true) {
                    val chunk = audioQueue.poll(2, TimeUnit.SECONDS) ?: break
                    outputStream.write(chunk)
                    outputStream.flush()
                }
            } catch (e: IOException) {
                // Il client ha chiuso la connessione — normale
            } finally {
                outputStream.close()
            }
        }.apply { isDaemon = true }.start()

        return newChunkedResponse(
            Response.Status.OK,
            "audio/wav",
            inputStream
        ).apply {
            addHeader("Connection", "keep-alive")
            addHeader("Cache-Control", "no-cache")
            // Fondamentale per alcuni renderer DLNA
            addHeader("transferMode.dlna.org", "Streaming")
            addHeader("contentFeatures.dlna.org",
                "DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000")
        }
    }

    private fun buildWavHeader(sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        // Usiamo 0xFFFFFFFF per indicare stream di lunghezza indefinita
        val dataSize = 0xFFFFFFFFL

        return ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt((dataSize + 36).toInt())       // chunk size
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                             // subchunk size
            putShort(1)                            // PCM format
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray())
            putInt(dataSize.toInt())               // 0xFFFFFFFF = stream
        }.array()
    }
}