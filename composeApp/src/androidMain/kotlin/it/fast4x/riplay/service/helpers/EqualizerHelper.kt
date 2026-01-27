package it.fast4x.riplay.service.helpers

import android.content.Context
import android.media.audiofx.Equalizer
import it.fast4x.riplay.extensions.preferences.preferences
import timber.log.Timber

class EqualizerHelper(private val context: Context) {

    private val prefs = context.preferences
    private val KEY_ENABLED = "eq_enabled"
    private val KEY_PRESET = "eq_preset"
    private val KEY_BANDS = "eq_bands"

    private var equalizer: Equalizer? = null

    fun saveSettings(isEnabled: Boolean, presetName: String, bandLevels: Map<Short, Float>) {
        try {
            prefs.edit().putBoolean(KEY_ENABLED, isEnabled).apply()
            prefs.edit().putString(KEY_PRESET, presetName).apply()

            val sortedBands = bandLevels.keys.sorted().map { bandLevels[it] ?: 0.5f }
            val bandsString = sortedBands.joinToString(separator = ",")

            prefs.edit().putString(KEY_BANDS, bandsString).apply()

            Timber.d("EqualizerHelper settings saved")
        } catch (e: Exception) {
            Timber.e("EqualizerHelper", "EqualizerHelper Error saving prefs", e)
        }
    }

    fun loadSettings(): Triple<Boolean, String?, Map<Short, Float>>? {
        return try {
            val isEnabled = prefs.getBoolean(KEY_ENABLED, false)
            val presetName = prefs.getString(KEY_PRESET, "Flat")
            val bandsString = prefs.getString(KEY_BANDS, null)

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
            Timber.e("EqualizerHelper", "Error loading prefs", e)
            null
        }
    }

    fun setup(audioSessionId: Int = 0) {
        try {
            //release previously initialized equalizer
            release()

            // Priority 0, AudioSessionId (0 = Globale)
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true

            Timber.d("EqualizerHelper Equalizer globale inizializzato. Bande disponibili: ${equalizer?.numberOfBands}")

        } catch (e: Exception) {
            Timber.e("EqualizerHelper Errore inizializzazione (il device potrebbe non supportare EQ) ${e.message}")
        }
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
            Timber.e("EqualizerHelper", "Error setupping equalizer bands:", e)
        }
    }

    fun reset() {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        val center = ((range[0] + range[1]) / 2).toShort()
        val bands = eq.numberOfBands
        val flatLevels = List(bands.toInt()) { center }
        setBandsLevels(flatLevels)
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
        } catch (e: Exception) {
            Timber.e("EqualizerHelper", "Errore set enabled", e)
        }
    }

    fun isEnabled(): Boolean {
        return try {
            equalizer?.enabled ?: false
        } catch (e: Exception) {
            false
        }
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