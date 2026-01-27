package it.fast4x.riplay.service.helpers

import android.content.Context
import android.media.audiofx.Equalizer
import it.fast4x.riplay.extensions.preferences.preferences
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.content.edit
import it.fast4x.riplay.extensions.preferences.eqBandsKey
import it.fast4x.riplay.extensions.preferences.eqEnabledKey
import it.fast4x.riplay.extensions.preferences.eqPresetKey

class EqualizerHelper(private val context: Context) {

    private val prefs = context.preferences
    //private val eqEnabledKey = "eq_enabled"
    //private val eqPresetKey = "eq_preset"
    //private val eqBandsKey = "eq_bands"

    private var equalizer: Equalizer? = null

    fun saveSettings(isEnabled: Boolean, presetName: String, bandLevels: Map<Short, Float>) {
        try {
            prefs.edit { putBoolean(eqEnabledKey, isEnabled) }
            prefs.edit { putString(eqPresetKey, presetName) }

            val sortedBands = bandLevels.keys.sorted().map { bandLevels[it] ?: 0.5f }
            val bandsString = sortedBands.joinToString(separator = ",")

            prefs.edit { putString(eqBandsKey, bandsString) }

            Timber.d("EqualizerHelper settings saved")
        } catch (e: Exception) {
            Timber.d("EqualizerHelper EqualizerHelper Error saving prefs ${e.message}")
        }
    }

    fun loadSettings(): Triple<Boolean, String?, Map<Short, Float>>? {
        return try {
            val isEnabled = prefs.getBoolean(eqEnabledKey, false)
            val presetName = prefs.getString(eqPresetKey, "Flat")
            val bandsString = prefs.getString(eqBandsKey, null)

            if (bandsString != null) {
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

    private fun restoreState() {
        val eq = equalizer ?: return

        try {

            val isEnabled = prefs.getBoolean(eqEnabledKey, false) // Default false
            val bandsString = prefs.getString(eqBandsKey, null)

            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val bandsCount = eq.numberOfBands

            val trueFlatPercent = getZeroDbPercent(minLevel, maxLevel)

            if (bandsString != null) {

                val values = bandsString.split(",").map { it.toFloat() }
                for (i in 0 until bandsCount) {
                    val savedValue = if (i < values.size) values[i] else trueFlatPercent
                    val levelShort = percentToLevel(savedValue, minLevel, maxLevel)
                    eq.setBandLevel(i.toShort(), levelShort)
                }
                eq.enabled = isEnabled

                if (isEnabled && bandsCount > 0 && abs(values[0] - 0.5f) < 0.01f && abs(trueFlatPercent - 0.5f) > 0.1f) {
                    reset(trueFlatPercent)
                    return
                }

            } else {
                val flatLevels = List(bandsCount.toInt()) { trueFlatPercent }
                flatLevels.forEachIndexed { index, percent ->
                    val levelShort = percentToLevel(percent, minLevel, maxLevel)
                    eq.setBandLevel(index.toShort(), levelShort)
                }
                eq.enabled = false // Spento di default al primo avvio
            }

        } catch (e: Exception) {
            Timber.d("EqualizerHelper Error restoreState ${e.message}")
        }
    }

    private fun percentToLevel(percent: Float, min: Short, max: Short): Short {
        val range = (max - min).toFloat()
        val level = min + (range * percent)
        return level.roundToInt().toShort()
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

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Timber.e("EqualizerHelper Errore set banda ${e.message}")
        }
    }

    fun setBandsLevels(levels: List<Short>) {
        val eq = equalizer ?: return
        try {
            eq.enabled = true
            levels.forEachIndexed { index, level ->
                if (index < eq.numberOfBands) {
                    eq.setBandLevel(index.toShort(), level)
                }
            }
        } catch (e: Exception) {
            Timber.d("EqualizerHelper Error setupping equalizer bands: ${e.message}")
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
            saveSettings(eq.enabled, "Flat", flatMap)

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
            prefs.edit { putBoolean(eqEnabledKey, enabled) }
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