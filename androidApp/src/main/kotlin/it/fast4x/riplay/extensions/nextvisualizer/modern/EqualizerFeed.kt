package it.fast4x.riplay.extensions.nextvisualizer.modern

import android.media.audiofx.Visualizer
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

interface AudioVisualConsumer {
    fun onWaveform(waveform: ByteArray)
    fun onFft(fft: ByteArray)
}

object EqualizerFeed {
    private const val SESSION_ID = 0
    private val consumers = CopyOnWriteArrayList<AudioVisualConsumer>()
    private var visualizer: Visualizer? = null

    val CAPTURE_SIZE: Int = Visualizer.getCaptureSizeRange()?.get(1) ?: 1024

    fun register(c: AudioVisualConsumer) {
        if (consumers.addIfAbsent(c) && visualizer == null) acquire()
    }

    fun unregister(c: AudioVisualConsumer) {
        if (consumers.remove(c) && consumers.isEmpty()) {
            visualizer?.release(); visualizer = null
        }
    }

    private fun acquire() {
        try {
            visualizer = Visualizer(SESSION_ID).apply {
                captureSize = this@EqualizerFeed.CAPTURE_SIZE
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, wf: ByteArray, rate: Int) {
                        consumers.forEach { it.onWaveform(wf) }
                    }
                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, rate: Int) {
                        consumers.forEach { it.onFft(fft) }
                    }
                }, Visualizer.getMaxCaptureRate(), true, true)
                enabled = true
                EqualizerState.globalSampleRateHz = samplingRate
            }
        } catch (t: Throwable) {
            Timber.e(t, "EqualizerFeed: creazione fallita")
            visualizer = null
        }
    }

    /** Query sincrona nativa: il vecchio contratto del legacy.
     *  return false = nessun visualizer attivo. */
    fun pullFft(sink: ByteArray): Boolean {
        val v = visualizer ?: return false
        return try { v.getFft(sink); true } catch (t: Throwable) { false }
    }

    fun pullWave(sink: ByteArray): Boolean {
        val v = visualizer ?: return false
        return try { v.getWaveForm(sink); true } catch (t: Throwable) { false }
    }

}

