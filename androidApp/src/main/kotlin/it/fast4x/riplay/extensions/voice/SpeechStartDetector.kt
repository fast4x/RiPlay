package it.fast4x.riplay.extensions.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
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

        // The platform echo canceler runs on the voice-communication path.
        // Without communication mode some songs leak through the canceler at
        // near-speech levels, which made the detector wake the recognizer for
        // music. Only take the mode over when it is idle, and give it back as
        // soon as the canceler has converged so the volume keys keep
        // controlling media again.
        //
        // Communication mode is only needed while the song plays through the
        // built-in speaker and can bleed into the microphone. With headphones,
        // Bluetooth or USB output the mode would reroute playback and turn the
        // volume keys into voice-call keys for no benefit, so leave it alone.
        val audioManager = context.getSystemService(AudioManager::class.java)
        val externalOutput = isExternalOutputConnected(audioManager)
        val restoreMode = if (AcousticEchoCanceler.isAvailable() && !externalOutput) {
            audioManager?.mode?.takeIf { it == AudioManager.MODE_NORMAL }
        } else {
            null
        }
        if (externalOutput) {
            Timber.d("SpeechStartDetector: external output active, keeping audio mode")
        }
        if (restoreMode != null) {
            runCatching { audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION }
                .onFailure {
                    Timber.w(it, "SpeechStartDetector: could not set communication mode")
                }
        }

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
            restoreAudioMode(audioManager, restoreMode)
            return false
        }

        val echoCanceler = createEchoCanceler(record.audioSessionId)
        if (echoCanceler == null) {
            // No echo canceler, so communication mode cannot help. Give the
            // mode back immediately instead of holding it for the whole session.
            restoreAudioMode(audioManager, restoreMode)
        }
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
            var modeRestored = restoreMode == null || echoCanceler == null

            while (coroutineContext.isActive) {
                val now = SystemClock.elapsedRealtime()
                if (!modeRestored && now - startedAt >= MODE_HOLD_MS) {
                    modeRestored = true
                    restoreAudioMode(audioManager, restoreMode)
                    Timber.d("SpeechStartDetector: released communication mode")
                }
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
            restoreAudioMode(audioManager, restoreMode)
        }
    }

    private fun restoreAudioMode(audioManager: AudioManager?, restoreMode: Int?) {
        if (audioManager == null || restoreMode == null) return
        // A call or another app may have taken the mode over meanwhile.
        if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) return
        runCatching { audioManager.mode = restoreMode }
            .onFailure { Timber.w(it, "SpeechStartDetector: could not restore audio mode") }
    }

    /**
     * True when a wired, Bluetooth, USB or hearing-aid sink is connected, so
     * the song is not coming out of the built-in speaker. Communication mode
     * would not improve echo cancellation there but would still reroute audio.
     */
    private fun isExternalOutputConnected(audioManager: AudioManager?): Boolean {
        if (audioManager == null) return false
        val devices = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.onFailure {
            Timber.w(it, "SpeechStartDetector: could not query audio outputs")
        }.getOrNull() ?: return false
        return devices.any { it.isSink && it.type in EXTERNAL_OUTPUT_TYPES }
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
        const val MODE_HOLD_MS = 4_000L // let the echo canceler converge, then free the mode
        const val LOG_EVERY_FRAMES = 50 // about once per second

        val EXTERNAL_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC
        )
    }
}
