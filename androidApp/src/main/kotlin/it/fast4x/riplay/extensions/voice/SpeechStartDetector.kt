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
 * Outcome of one raw-capture session.
 */
internal sealed interface DetectOutcome {
    /** Somebody started speaking: hand the microphone to the recognizer. */
    data object Speech : DetectOutcome

    /** Nothing heard within the session window; retry with a fresh capture. */
    data object Timeout : DetectOutcome

    /** The microphone could not be captured at all. */
    data object Failed : DetectOutcome
}

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
 * The global audio mode is never touched. Setting
 * [android.media.AudioManager.MODE_IN_COMMUNICATION] while a song plays
 * reroutes/attenuates media output on many devices and the music volume
 * collapsed until the player was closed — the mode is what the volume bug was
 * made of. The [AcousticEchoCanceler] attached to the capture session alone
 * removes the song from the captured signal on devices whose
 * `VOICE_COMMUNICATION` input path ships an echo canceler; where it does not,
 * the adaptive noise floor in [SpeechTriggerGate] keeps the music from waking
 * the recognizer by itself.
 *
 * The capture stays open while hands-free waits for a voice, so the system
 * microphone indicator does not blink on and off; it is only released when a
 * trigger hands the microphone to the recognizer or hands-free is turned off.
 * The caller passes [awaitSpeech] a [shouldAbort] check that is evaluated on
 * every frame, so the capture also closes immediately when the situation
 * changes under its feet — the song was stopped, hands-free was switched off
 * or the app went to the background — and hands-free falls back to the direct
 * listening style instead of sitting on a microphone nobody is using.
 */
internal class SpeechStartDetector(private val context: Context) {

    /**
     * Suspends until speech is detected, the caller aborts or the coroutine is
     * cancelled.
     *
     * @param shouldAbort evaluated before every captured frame; when it turns
     *   true the capture is released at once and [DetectOutcome.Timeout] is
     *   returned so the caller can re-evaluate the playback state.
     * @return [DetectOutcome.Speech] when speech started,
     * [DetectOutcome.Timeout] when the wait was interrupted without speech and
     * [DetectOutcome.Failed] when the audio source could not be used.
     */
    suspend fun awaitSpeech(shouldAbort: () -> Boolean = { false }): DetectOutcome {
        if (!hasMicrophonePermission()) return DetectOutcome.Failed

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            Timber.w("SpeechStartDetector: invalid minimum buffer size %s", minBufferSize)
            return DetectOutcome.Failed
        }

        // The VOICE_COMMUNICATION input path applies platform voice processing
        // (echo cancellation, noise suppression) on most devices regardless of
        // the global audio mode, and unlike the mode it does not reroute or
        // attenuate playback output. Never set MODE_IN_COMMUNICATION here: the
        // music volume collapse users saw was caused by exactly that.
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
            return DetectOutcome.Failed
        }

        val echoCanceler = createEchoCanceler(record.audioSessionId)
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Timber.w("SpeechStartDetector: AudioRecord did not start")
                return DetectOutcome.Failed
            }

            val frame = ShortArray(FRAME_SAMPLES)
            val startedAt = SystemClock.elapsedRealtime()
            // The detector only runs while a song plays, so the gate always
            // works in its playback point (wider margin, longer voiced run).
            val gate = SpeechTriggerGate(playbackActive = true)
            gate.reset(startedAt)
            var processedFrames = 0

            // No fixed timeout: the capture stays open while hands-free waits
            // for a voice. Closing and reopening it periodically was visible
            // as the microphone indicator toggling on and off in complete
            // silence. The abort check below is what lets go of the capture
            // the moment the situation changes.
            while (coroutineContext.isActive) {
                if (shouldAbort()) {
                    Timber.d(
                        "SpeechStartDetector: capture aborted (playback/hands-free/foreground changed)"
                    )
                    return DetectOutcome.Timeout
                }

                val now = SystemClock.elapsedRealtime()
                val read = record.read(frame, 0, frame.size, AudioRecord.READ_NON_BLOCKING)
                if (read > 0) {
                    val levelDb = rmsDb(frame, read)
                    if (gate.onFrame(levelDb, now)) {
                        Timber.i(
                            "SpeechStartDetector: speech detected level=%.1f dB floor=%.1f dB aec=%s",
                            levelDb,
                            gate.peakFloorDb,
                            echoCanceler != null
                        )
                        return DetectOutcome.Speech
                    }

                    processedFrames++
                    if (processedFrames % LOG_EVERY_FRAMES == 0) {
                        Timber.d(
                            "SpeechStartDetector: level=%.1f dB floor=%.1f dB warming=%s aec=%s",
                            levelDb,
                            gate.peakFloorDb,
                            gate.isWarmingUp(now),
                            echoCanceler != null
                        )
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
            return DetectOutcome.Timeout
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            if (coroutineContext.isActive) {
                Timber.w(t, "SpeechStartDetector: capture failed")
            }
            return DetectOutcome.Failed
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
