package it.fast4x.riplay.extensions.audiotag

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 8000 // 8 kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    suspend fun startRecording(): ByteArray? = withContext(Dispatchers.IO) {
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
            out.toByteArray()
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
}