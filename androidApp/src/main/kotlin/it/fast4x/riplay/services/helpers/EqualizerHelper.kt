package it.fast4x.riplay.services.helpers

import android.content.Context
import android.media.audiofx.Equalizer
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EqualizerHelper(private val context: Context) {

    val scope = CoroutineScope(Dispatchers.IO)

    val appSettingsManager = (appContext() as MainApplication).appSettingsManager

    private fun currentSettings() = appSettingsManager.activeSettings.value

    private val _eqReady = MutableStateFlow(false)
    val eqReady: StateFlow<Boolean> = _eqReady

    private val _preampDb = MutableStateFlow(0f)   // offset in dB, negativo
    val preampDb: StateFlow<Float> = _preampDb // Lo usiamo per l'indicatore di pre-amplificazione nella UI

    fun percentToMb(percent: Float): Int = ((percent - 0.5f) * 1600).roundToInt()  // ±800

    private var equalizer: Equalizer? = null

    fun loadSettings(): Triple<Boolean, String?, Map<Short, Float>>? {
        return try {
            val s = currentSettings()
            val isEnabled = s.eqEnabled
            val presetName = s.eqPreset
            val bandsString = s.eqBands

            Timber.d("EqualizerHelper EQ-LOAD inst=${System.identityHashCode(this)} enabled=${s.eqEnabled} bands='${s.eqBands.take(20)}'")

            if (bandsString.isNotEmpty()) {
                val values = bandsString.split(",").map { it.toFloat() }
                val bandsMap = mutableMapOf<Short, Float>()
                values.forEachIndexed { index, fl ->
                    bandsMap[index.toShort()] = fl
                }
                Triple(isEnabled, presetName, bandsMap)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.d("EqualizerHelper Error loading prefs ${e.message}")
            null
        }
    }

    fun setup(audioSessionId: Int = 0) {
        try {
            //release previously initialized equalizer
            release()

            // Priority 0, AudioSessionId (0 = Globale)
            equalizer = Equalizer(0, audioSessionId)
            restoreState()

            Timber.d("EqualizerHelper Equalizer globale inizializzato. Bande disponibili: ${equalizer?.numberOfBands}")

        } catch (e: Exception) {
            Timber.e("EqualizerHelper Errore inizializzazione (il device potrebbe non supportare EQ) ${e.message}")
        }
    }

    fun setupWithRetry(audioSessionId: Int = 0, maxAttempts: Int = 15) {
        scope.launch {
            repeat(maxAttempts) { attempt ->
                if (trySetup(audioSessionId)) {
                    Timber.d("EqualizerHelper setup OK al tentativo ${attempt + 1}")
                    return@launch
                }
                delay(1000.milliseconds)
            }
            Timber.e("EqualizerHelper setup fallito dopo $maxAttempts tentativi")
        }
    }

    private fun trySetup(audioSessionId: Int): Boolean = try {
        release()
        val eq = Equalizer(0, audioSessionId)
        Timber.d("EqualizerHelper EQ-SETUP inst=${System.identityHashCode(this)}")

        val range = eq.bandLevelRange
        Timber.d("EqualizerHelper setup EQ range: min=${range[0]} max=${range[1]} zeroDbPercent=${getZeroDbPercent(range[0], range[1])}")
        eq.setBandLevel(0, ((range[0] + range[1]) / 2).toShort())  // lancia se l'effetto è busto

        equalizer = eq
        restoreState()
        _eqReady.value = true
        true
    } catch (e: Exception) {
        Timber.d("EqualizerHelper setup non pronto (${e.message}) → retry")
        false
    }

    fun applyAndSave(enabled: Boolean, presetName: String, cfg: EqualizerConfig, bandLevels: Map<Short, Float>) {
        try {
            equalizer?.enabled = enabled

            Timber.d("EqualizerHelper EQ-SAVE inst=${System.identityHashCode(this)} enabled=$enabled")

            if (enabled) {
                applyCurve(bandLevels)
            }
        } catch (e: Exception) {
            Timber.e("EqualizerHelper applyAndSave: ${e.message}")
        }

        scope.launch {
            appSettingsManager.updateSettings(
                currentSettings().copy(
                    eqEnabled = enabled,
                    eqPreset = presetName,
                    eqBands = bandLevels.keys.sorted()
                        .map { bandLevels[it] ?: 0.5f }
                        .joinToString(",")
                )
            )
        }
    }

    fun applyCurve(percents: Map<Short, Float>) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val min = range[0]; val max = range[1]

            // 1. forma logica: percent → mB
            val levels = (0 until eq.numberOfBands).map { i ->
                percentToLevel(percents[i.toShort()] ?: 0.5f, min, max)
            }

            // 2. headroom: il boost massimo determina l'offset uniforme
            val maxBoost = levels.max()
            val offset = if (maxBoost > 0) -maxBoost else 0

            // 3. forma fisica: livelli shiftati nell'effetto
            levels.forEachIndexed { i, level ->
                eq.setBandLevel(i.toShort(),
                    (level + offset).coerceIn(min.toInt(), max.toInt()).toShort())
            }

            // 4. pubblica l'offset per la UI
            _preampDb.value = offset / 100f

            Timber.d("EqualizerHelper EQ-CURVE inst=${System.identityHashCode(this)} offset=$offset levels=${levels.joinToString()}")
        } catch (e: Exception) {
            Timber.e("EqualizerHelper applyCurve: ${e.message}")
        }
    }

    fun saveBands(presetName: String, bandLevels: Map<Short, Float>) {
        scope.launch {
            appSettingsManager.updateSettings(
                currentSettings().copy(eqPreset = presetName,
                    eqBands = bandLevels.keys.sorted().map { bandLevels[it] ?: 0.5f }.joinToString(","))
            )
        }
    }

    private fun restoreState() {
        val eq = equalizer ?: return
        val s = currentSettings()
        val range = eq.bandLevelRange
        val bandsCount = eq.numberOfBands
        val zero = getZeroDbPercent(range[0], range[1])

        val saved = s.eqBands.split(",").mapNotNull { it.toFloatOrNull() }
        val map = (0 until bandsCount).associate { i ->
            i.toShort() to (saved.getOrNull(i) ?: zero)
        }
        applyCurve(map)
        eq.enabled = s.eqEnabled
    }

    private fun percentToLevel(percent: Float, minLevel: Short, maxLevel: Short): Short {
        //val range = (max - min).toFloat()
        //val level = min + (range * percent)
        val shapeMaxMb = 800
        val levelMb = ((percent - 0.5f) * 2 * shapeMaxMb).roundToInt()
        val level = (levelMb + 0).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
        return level //.roundToInt().toShort()
    }

    fun getEqualizerConfig(): EqualizerConfig? {
        val eq = equalizer ?: return null
        return try {
            val bands = eq.numberOfBands
            val range = eq.bandLevelRange
            val bandConfigs = (0 until bands).map { index ->
                BandConfig(
                    index = index.toShort(),
                    centerFreq = eq.getCenterFreq(index.toShort()),
                    currentLevel = eq.getBandLevel(index.toShort())
                )
            }
            EqualizerConfig(minLevel = range[0], maxLevel = range[1], bands = bandConfigs)
        } catch (e: Exception) {
            null
        }
    }

    fun reset(customFlatPercent: Float? = null) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val bandsCount = eq.numberOfBands

            val trueFlatPercent = customFlatPercent ?: getZeroDbPercent(minLevel, maxLevel)

            for (i in 0 until bandsCount) {
                val levelShort = percentToLevel(trueFlatPercent, minLevel, maxLevel)
                eq.setBandLevel(i.toShort(), levelShort)
            }

            val flatMap = (0 until bandsCount).associate { i -> i.toShort() to trueFlatPercent }
            val cfg = getEqualizerConfig() ?: return
            applyAndSave(eq.enabled, "Flat", cfg, flatMap)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            scope.launch {
                appSettingsManager.updateSettings(currentSettings().copy(eqEnabled = enabled))
            }
        } catch (e: Exception) {
            Timber.d("EqualizerHelper Errore set enabled ${e.message}")
        }
    }

    fun isEnabled(): Boolean {
        return try {
            equalizer?.enabled ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getZeroDbPercent(min: Short, max: Short): Float {
        val minF = min.toFloat()
        val maxF = max.toFloat()
        val range = maxF - minF

        if (range == 0f) return 0.5f

        val zeroPercent = (0f - minF) / range

        return zeroPercent.coerceIn(0f, 1f)
    }

}

data class EqualizerConfig(
    val minLevel: Short,
    val maxLevel: Short,
    val bands: List<BandConfig>
)

data class BandConfig(
    val index: Short,
    val centerFreq: Int, // in milliHertz
    val currentLevel: Short
)