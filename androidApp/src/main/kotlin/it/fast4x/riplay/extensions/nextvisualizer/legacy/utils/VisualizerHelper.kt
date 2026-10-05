package it.fast4x.riplay.extensions.nextvisualizer.legacy.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import it.fast4x.riplay.extensions.nextvisualizer.modern.AudioVisualConsumer
import it.fast4x.riplay.extensions.nextvisualizer.modern.EqualizerFeed

@Composable
fun rememberVisualizerHelper(): VisualizerHelper {
    val helper = remember { VisualizerHelper() }
    DisposableEffect(helper) { onDispose { helper.release() } }
    return helper
}

class VisualizerHelper : AudioVisualConsumer {

    private val fftBuff = ByteArray(EqualizerFeed.CAPTURE_SIZE)
    private val waveBuff = ByteArray(EqualizerFeed.CAPTURE_SIZE)

    @Volatile private var lastWave = ByteArray(EqualizerFeed.CAPTURE_SIZE)
    @Volatile private var lastFft = ByteArray(EqualizerFeed.CAPTURE_SIZE)
    @Volatile internal var fftPushes = 0; private set
    private var testPhase = 0

    override fun onWaveform(waveform: ByteArray) { lastWave = waveform.copyOf() }
    override fun onFft(fft: ByteArray) {
        testPhase++
        if (testPhase % 120 == 0) {
            fft.copyInto(lastFft)
            lastFft.fill(100.toByte(), 0, lastFft.size / 3)
            lastFft.fill(0, lastFft.size / 3)
        } else {
            fft.copyInto(lastFft)
        }
        fftPushes++
    }


    fun getFft(): ByteArray {
        val ret = if (EqualizerFeed.pullFft(fftBuff)) fftBuff
        else lastFft
        return ret
    }

    fun getWave(): ByteArray =
        if (EqualizerFeed.pullWave(waveBuff)) waveBuff else lastWave

    fun getFftMagnitude(): DoubleArray {
        val fft = getFft()
        val n = fft.size / 2 - 1
        if (n <= 0) return DoubleArray(0)
        val m = DoubleArray(n)
        for (k in m.indices) {
            val i = (k + 1) * 2
            m[k] = Math.hypot(fft[i].toDouble(), fft[i + 1].toDouble())
        }

        return m
    }
    fun getFftMagnitudeRange(startHz: Int, endHz: Int): DoubleArray {
        val s = hzToFftIndex(startHz)
        val e = hzToFftIndex(endHz)
        return getFftMagnitude().copyOfRange(minOf(s, e), maxOf(s, e))
    }

    fun hzToFftIndex(hz: Int): Int =
        ((hz * 1024) / (44100 * 2)).coerceIn(0, 255)

    fun release() = EqualizerFeed.unregister(this)

    init { EqualizerFeed.register(this) }
}

