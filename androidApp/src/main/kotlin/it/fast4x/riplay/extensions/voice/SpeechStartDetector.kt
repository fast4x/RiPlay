package it.fast4x.riplay.extensions.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * Waits for somebody to start speaking without requesting audio focus.
 *
 * Hands-free voice control used to keep a [android.speech.SpeechRecognizer]
 * open all the time. The system recognizer asks for exclusive audio focus, so
 * Chromium (YouTube) paused the song for every listening session and the music
 * stopped again and again. This detector only records raw PCM with
 * [AudioRecord], which takes no audio focus, and reports the moment speech is
 * loud enough to wake the recognizer.
 *
 * [AcousticEchoCanceler] removes the song that is playing from the captured
 * signal when the device supports it. Without echo cancellation the detector
 * still works, but the music has to stay well below the adaptive noise floor.
 *
 * The detector never changes the global audio mode. Taking
 * `MODE_IN_COMMUNICATION` while a song was playing rerouted playback and made
 * the YouTube player pause before the user had said anything, which is exactly
 * the "enabling hands-free stops the song" bug this class must avoid.
 */
internal class SpeechStartDetector(private val context: Context) {

    /**
     * Suspends until speech is detected, the coroutine is cancelled or the
     * microphone cannot be opened.
     *
     * @return true when speech started, false when the audio source could not
     * be used.
     */
    suspend fun awaitSpeech(): Boolean {
        if (!hasMicrophonePermission()) return false

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            Timber.w("SpeechStartDetector: invalid minimum buffer size %s", minBufferSize)
            return false
        }

        // The platform echo canceler runs on the voice-communication capture
        // path. Do not switch the device to communication mode for it: that
        // rerouted playback on real devices and made the app look paused.
        val record = runCatching {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize * 2, FRAME_SAMPLES * 2 * 4))
                .build()
        }.onFailure {
            Timber.e(it, "SpeechStartDetector: could not create AudioRecord")
        }.getOrNull()

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            return false
        }

        val echoCanceler = createEchoCanceler(record.audioSessionId)
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Timber.w("SpeechStartDetector: AudioRecord did not start")
                return false
            }

            val frame = ShortArray(FRAME_SAMPLES)
            val startedAt = SystemClock.elapsedRealtime()
            val gate = SpeechTriggerGate()
            gate.reset(startedAt)
            var processedFrames = 0

            while (coroutineContext.isActive) {
                val now = SystemClock.elapsedRealtime()
                val read = record.read(frame, 0, frame.size, AudioRecord.READ_NON_BLOCKING)
                if (read > 0) {
                    val levelDb = rmsDb(frame, read)
                    if (gate.onFrame(levelDb, now)) {
                        Timber.i(
                            "SpeechStartDetector: speech detected level=%.1f dB noise=%.1f dB aec=%s",
                            levelDb,
                            gate.noiseFloorDb,
                            echoCanceler != null
                        )
                        return true
                    }

                    processedFrames++
                    if (processedFrames % LOG_EVERY_FRAMES == 0) {
                        Timber.d(
                            "SpeechStartDetector: level=%.1f dB noise=%.1f dB warming=%s settled=%s aec=%s",
                            levelDb,
                            gate.noiseFloorDb,
                            gate.isWarmingUp(now),
                            gate.isSettled,
                            echoCanceler != null
                        )
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
            return false
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            if (coroutineContext.isActive) {
                Timber.w(t, "SpeechStartDetector: capture failed")
            }
            return false
        } finally {
            runCatching { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() }
            runCatching { record.release() }
            runCatching { echoCanceler?.release() }
        }
    }

    private fun createEchoCanceler(audioSessionId: Int): AcousticEchoCanceler? {
        if (!AcousticEchoCanceler.isAvailable()) return null
        val canceler = runCatching { AcousticEchoCanceler.create(audioSessionId) }
            .onFailure { Timber.w(it, "SpeechStartDetector: could not create echo canceler") }
            .getOrNull() ?: return null
        val enabled = runCatching {
            canceler.enabled = true
            canceler.enabled
        }.getOrDefault(false)
        if (!enabled) {
            runCatching { canceler.release() }
            return null
        }
        return canceler
    }

    private fun rmsDb(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (index in 0 until length) {
            val sample = buffer[index] / 32768.0
            sum += sample * sample
        }
        return 10.0 * log10(sum / length + 1e-12)
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = SAMPLE_RATE / 50 // 20 ms
        const val POLL_INTERVAL_MS = 20L
        const val LOG_EVERY_FRAMES = 50 // about once per second
    }
}
