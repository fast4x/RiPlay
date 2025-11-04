package it.fast4x.riplay.extensions.audiotag

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder {

    enum class OutputFormat {
        PCM,
        WAV
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 8000 // 8 kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    suspend fun startRecording(format: OutputFormat = OutputFormat.PCM): ByteArray? = withContext(Dispatchers.IO) {
        if (isRecording) return@withContext null

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            val out = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)

            val maxDurationMs = 15000
            val startTime = System.currentTimeMillis()

            while (isRecording && (System.currentTimeMillis() - startTime) < maxDurationMs) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (read > 0) {
                    out.write(buffer, 0, read)
                }
            }

            stopRecording()

            val pcmData = out.toByteArray()
            when (format) {
                OutputFormat.PCM -> pcmData
                OutputFormat.WAV -> {
                    try {
                        pcmToWav(pcmData, sampleRate, 1, 16) // 1 canale (mono), 16 bit per campione
                    } catch (e: Exception) {
                        println("AudioTag Error converting PCM to WAV: ${e.stackTraceToString()}")
                        null
                    }



                }
            }

        } catch (e: Exception) {
            stopRecording()
            null
        }
    }

    fun stopRecording() {
        if (isRecording) {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun pcmToWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val dataSize = pcmData.size
        val totalFileSize = 44 + dataSize
        val riffChunkSize = totalFileSize - 8

        val buffer = ByteBuffer.allocate(totalFileSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(riffChunkSize)
        buffer.put("WAVE".toByteArray())

        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * bitsPerSample / 8).toShort())
        buffer.putShort(bitsPerSample.toShort())

        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        buffer.put(pcmData)

        return buffer.array()
    }

}