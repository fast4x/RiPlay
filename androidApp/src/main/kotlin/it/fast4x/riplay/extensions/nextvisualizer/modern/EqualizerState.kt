package it.fast4x.riplay.extensions.nextvisualizer.modern

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class EqualizerState(
    barCount: Int = 32,
    var inputGain: Float = 1.8f,
    var peakHoldMs: Float = 220f,
    var peakFadeMs: Float = 900f,
    var peakGravity: Float = 0.55f,
    var simulationEnabled: Boolean = false,
): AudioVisualConsumer {

    override fun onWaveform(waveform: ByteArray) = submitWaveform(waveform)
    override fun onFft(fft: ByteArray) = submitFft(fft)
    var barCount: Int = 0
        private set

    // Buffer a dimensione fissa: zero allocazioni né in step() né in draw,
    // nemmeno quando cambia barCount.
    internal val levels = FloatArray(MAX_BARS)
    internal val targets = FloatArray(MAX_BARS)
    internal val peaks = FloatArray(MAX_BARS)
    internal val peakVel = FloatArray(MAX_BARS)
    internal val peakAgeSec = FloatArray(MAX_BARS)
    private val nextChangeMs = LongArray(MAX_BARS)

    private var lastExternalDataMs = 0L
    private var lastStepNanos = 0L
    private val random = Random.Default

    var spectrumFMinHz: Float = 35f       // sotto: solo rumore
        set(value) { field = value; spectrumDirty = true }

    var spectrumFMaxHz: Float = 15_000f   // sopra: quasi solo sibilo
        set(value) { field = value; spectrumDirty = true }

    /** Compensazione del roll-off spettrale: dB aggiunti per ottava sopra fMin. */
    var spectrumTiltDbPerOctave: Float = 3.5f
        set(value) { field = value; spectrumDirty = true }

    var spectrumGain: Float = 1.1f

    var levelAggregation: Aggregation = Aggregation.MAX
        set(value) { field = value; spectrumDirty = true }

    /** Peso del picco nel mix HYBRID (0 = puro mean, 1 = puro max). */
    var hybridPeakMix: Float = 0.35f

    // ---- precalcolo bande ----
    private var bandEdges = IntArray(0)   // bin di inizio per banda (size = barCount+1)
    private var bandGain = FloatArray(0)  // tilt lineare per banda
    private var cachedFftBins = -1
    private var spectrumDirty = true
    var fftFrames = 0
        internal set
    var wfFrames = 0
        internal set
    internal var lastFftMs = 0L

    init {
        this.barCount = barCount.coerceIn(1, MAX_BARS)
    }

    /** Normalizza la dinamica osservata sull'intera scala (auto-gain lento). */
    var autoRange: Boolean = false

    /** Velocità di decadimento del massimo di riferimento (frazione/s). */
    var autoRangeDecay: Float = 0.08f

    /** Sotto questa soglia (post auto-range) il livello è 0: taglia il "sempre acceso". */
    var noiseFloor: Float = 0f

    /** Esponente di risposta: >1 spinge giù la media e accentua i salite. */
    var gamma: Float = 1f

    private val rangeMax = FloatArray(MAX_BARS)

    private fun shape(raw: Float, i: Int): Float {
        var t = raw
        if (autoRange) t = (t / rangeMax[i]).coerceIn(0f, 1f)
        if (noiseFloor > 0f) t = ((t - noiseFloor) / (1f - noiseFloor)).coerceIn(0f, 1f)
        return if (gamma != 1f) t.pow(gamma) else t
    }

    /**
     * Buffer FFT del Visualizer (byte signed interleaved re/im).
     * Numera i bin: frequenza del bin k = k * sampleRate / fft.size.
     * Chiamato dal thread del Visualizer: nessuna allocazione qui dentro.
     */
    fun submitFft(fft: ByteArray) {
        val numBins = fft.size shr 1
        if (numBins < 2) return
        fftFrames++
        lastFftMs = System.currentTimeMillis()
        if (spectrumDirty || cachedFftBins != numBins) {
            rebuildSpectrum(numBins)
            cachedFftBins = numBins
            spectrumDirty = false
        }

        val gain = spectrumGain

        for (band in 0 until barCount) {
            var idx = bandEdges[band] shl 1       // byte index del bin di inizio
            val stop = bandEdges[band + 1] shl 1  // esclusivo
            var peak = 0f
            var sum = 0f
            var count = 0
            while (idx < stop) {
                val re = fft[idx].toInt()
                val im = fft[idx + 1].toInt()
                val mag = sqrt((re * re + im * im).toFloat()) * (1f / 128f)
                if (mag > peak) peak = mag
                sum += mag
                count++
                idx += 2
            }
            val mean = if (count > 0) sum / count else 0f
            val agg = when (levelAggregation) {
                Aggregation.MAX -> peak
                Aggregation.MEAN -> mean
                Aggregation.HYBRID -> mean * (1f - hybridPeakMix) + peak * hybridPeakMix
            }
            targets[band] = (agg * bandGain[band] * gain).coerceIn(0f, 1f)
        }
        lastExternalDataMs = System.currentTimeMillis()
    }

    private fun rebuildSpectrum(numBins: Int) {
        val n = barCount
        if (bandEdges.size != n + 1) bandEdges = IntArray(n + 1)
        if (bandGain.size != n) bandGain = FloatArray(n)

        val fMin = spectrumFMinHz.coerceAtLeast(1f)
        val fMax = spectrumFMaxHz.coerceIn(fMin * 2f, globalSampleRateHz / 2f * 0.95f)
        val logRatio = ln(fMax / fMin)
        val binHz = globalSampleRateHz / (numBins * 2).toFloat()

        // Confini log-spaced; ogni banda prende almeno un bin (le basse si dividono i primi bin)
        var prev = 1 // bin 0 = DC, inutile e fuorviante
        bandEdges[0] = prev
        for (i in 1..n) {
            val f = fMin * exp(logRatio * i / n)
            var bin = ceil(f / binHz).toInt()
            if (bin <= prev) bin = prev + 1
            bandEdges[i] = bin.coerceAtMost(numBins)
            prev = bandEdges[i]
        }
        bandEdges[n] = numBins // l'ultima banda chiude fino in fondo

        // Gain di tilt: lineare in dB sull'altezza in ottave della banda
        for (i in 0 until n) {
            val fc = fMin * exp(logRatio * (i + 0.5f) / n)
            val octaves = ln(fc / fMin) / ln(2f)
            bandGain[i] = 10f.pow(spectrumTiltDbPerOctave * octaves / 20f)
        }
    }

    fun setBarCount(count: Int) {
        val n = count.coerceIn(1, MAX_BARS)
        if (n == barCount) return
        val old = barCount
        barCount = n
        if (n > old) { // azzera la gamma appena esposta
            levels.fill(0f, old, n); targets.fill(0f, old, n); peaks.fill(0f, old, n)
            peakVel.fill(0f, old, n); peakAgeSec.fill(0f, old, n); nextChangeMs.fill(0L, old, n)
        }
    }

    /** Livelli già normalizzati 0..1, uno per barra. */
    fun submitLevels(values: FloatArray) {
        val n = minOf(values.size, barCount)
        for (i in 0 until n) targets[i] = values[i].coerceIn(0f, 1f)
        lastExternalDataMs = System.currentTimeMillis()
    }

    /** Waveform del Visualizer: RMS per segmento, rimozione DC per-segmento. */
    fun submitWaveform(waveform: ByteArray) {
        wfFrames++                                             // diagnostica: conta TUTTI i callback
        if (System.currentTimeMillis() - lastFftMs < 500L) return  // FFT fresca: tiene il timone
        val n = barCount
        val seg = waveform.size / n
        if (seg <= 0) return
        var idx = 0
        for (i in 0 until n) {
            val start = idx
            var sum = 0
            for (j in 0 until seg) sum += waveform[idx++].toInt()
            val mean = sum.toFloat() / seg
            var acc = 0.0
            for (j in start until start + seg) {
                val v = waveform[j].toInt() - mean
                acc += (v * v).toDouble()
            }
            val rms = sqrt(acc / seg) / 128.0
            targets[i] = (rms * inputGain).toFloat().coerceIn(0f, 1f)
        }
        lastExternalDataMs = System.currentTimeMillis()
    }

    internal fun step(frameNanos: Long) {
        val previous = lastStepNanos
        lastStepNanos = frameNanos
        // Guardia per stato condiviso tra più renderer: 1 step per frame max
        if (previous == 0L || frameNanos <= previous) return

        val dt = ((frameNanos - previous) / 1_000_000_000f).coerceIn(0f, 0.05f)
        val nowMs = System.currentTimeMillis()
        val hasFreshData = nowMs - lastExternalDataMs < EXTERNAL_DATA_TIMEOUT_MS
        val holdSec = peakHoldMs / 1000f
        val denom = (barCount - 1).coerceAtLeast(1).toFloat()
        val attackK = 1f - exp(-ATTACK_RATE * dt)
        val releaseK = 1f - exp(-RELEASE_RATE * dt)
        val idleK = exp(-IDLE_DECAY * dt)

        for (i in 0 until barCount) {
            when {
                hasFreshData -> Unit
                simulationEnabled -> {
                    if (nowMs >= nextChangeMs[i]) {
                        nextChangeMs[i] = nowMs + 90L + random.nextLong(320L)
                        val tilt = 1f - (i / denom) * 0.55f
                        targets[i] = ((0.25f + random.nextFloat() * 0.75f) * tilt).coerceIn(0.05f, 1f)
                    }
                }
                else -> targets[i] *= idleK
            }

            if (autoRange) {
                rangeMax[i] = maxOf(targets[i], rangeMax[i] * exp(-autoRangeDecay * dt))
                    .coerceAtLeast(0.2f)
            }
            val shaped = shape(targets[i], i)
            val k = if (shaped > levels[i]) attackK else releaseK
            levels[i] += (shaped - levels[i]) * k

            if (levels[i] >= peaks[i]) {
                peaks[i] = levels[i]; peakVel[i] = 0f; peakAgeSec[i] = 0f
            } else {
                peakAgeSec[i] += dt
                if (peakAgeSec[i] > holdSec) {
                    peakVel[i] += peakGravity * dt
                    peaks[i] = (peaks[i] - peakVel[i] * dt).coerceAtLeast(levels[i])
                }
            }
        }
    }

    companion object {
        private const val MAX_BARS = 64
        private const val EXTERNAL_DATA_TIMEOUT_MS = 250L
        private const val ATTACK_RATE = 22f
        private const val RELEASE_RATE = 6f
        private const val IDLE_DECAY = 4f

        internal var globalSampleRateHz: Int = 44_100
    }
}