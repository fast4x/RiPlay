package it.fast4x.riplay.services.helpers

import android.content.Context
import android.media.AudioManager
import android.os.Build
import timber.log.Timber

object AudioDRCHelper {

    private const val TAG = "AudioDRCHelper"
    private var isInitialized = false
    private lateinit var audioManager: AudioManager

    fun init(context: Context) {
        if (isInitialized) return
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        isInitialized = true
    }

    fun hasDRCSupport(): Boolean {
        ensureInitialized()
        if (hasDRCSupportViaParameters()) return true

        return hasDRCSupportHeuristic()
    }

    private fun hasDRCSupportViaParameters(): Boolean {
        val keys = buildList {
            add("speaker_drc_enabled")
            add("drc_enabled")
            add("dolby_drc_mode")
            add("dts_drc_mode")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add("audiostream_drc")
            }
        }
        return keys.any { key ->
            val raw = audioManager.getParameters(key)
            val supported = !raw.isNullOrBlank()
            if (supported) Timber.d( "$TAG DRC founded by parameters — $key: '$raw'")
            supported
        }
    }

    private fun hasDRCSupportHeuristic(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val socModel = getSoCModel().lowercase()

        val likelyHasDRC = when {
            manufacturer == "samsung" -> true
            socModel.startsWith("sm") -> true   // Snapdragon modern
            socModel.startsWith("sdm") -> true  // Snapdragon legacy
            socModel.startsWith("exynos") -> true
            hardware.contains("exynos") -> true
            socModel.startsWith("mt") -> false  // MediaTek: DRC off by default
            else -> false
        }

        Timber.d( "$TAG DRC heuristic — manufacturer=$manufacturer, " +
                "soc=$socModel, hardware=$hardware, likelyHasDRC=$likelyHasDRC")

        return likelyHasDRC
    }

    private fun getSoCModel(): String {
        return try {
            val cpuInfo = java.io.File("/proc/cpuinfo").readText()
            val hardwareLine = cpuInfo.lines()
                .firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
            hardwareLine?.substringAfter(":")?.trim() ?: Build.HARDWARE
        } catch (e: Exception) {
            Build.HARDWARE
        }
    }

    private fun isDRCEnabledViaParameters(): Boolean {
        val fallbackKeys = listOf(
            "speaker_drc_enabled",
            "dolby_drc_mode",
            "dts_drc_mode",
            "audiostream_drc"
        )
        return fallbackKeys.any { key ->
            val raw = audioManager.getParameters(key)
            val value = parseBooleanParameter(raw, key)
            Timber.d( "$TAG Fallback DRC check — $key: $raw")
            value == true
        }
    }

    private fun parseBooleanParameter(raw: String?, key: String): Boolean? {
        if (raw.isNullOrBlank()) return null
        val value = raw
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?: return null

        return when (value.lowercase()) {
            "true", "1", "on"   -> true
            "false", "0", "off" -> false
            else -> null
        }
    }

    fun disableDRC() {
        ensureInitialized()
        try {

            audioManager.setParameters("speaker_drc_enabled=false")

            audioManager.setParameters("drc_enabled=false")

            audioManager.setParameters("dolby_drc_mode=0")

            audioManager.setParameters("dts_drc_mode=0")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioManager.setParameters("audiostream_drc=false")
            }

            Timber.d("$TAG DRC disabled")
        } catch (e: Exception) {
            Timber.w( "$TAG Error during disabling DRC: ${e.message}")
        }
    }

    fun restoreDRC() {
        ensureInitialized()
        try {
            audioManager.setParameters("speaker_drc_enabled=true")
            audioManager.setParameters("drc_enabled=true")
            audioManager.setParameters("dolby_drc_mode=1")
            audioManager.setParameters("dts_drc_mode=1")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioManager.setParameters("audiostream_drc=true")
            }
            Timber.d( "$TAG DRC enabled")
        } catch (e: Exception) {
            Timber.w( "$TAG Error during enabling DRC: ${e.message}")
        }
    }

    private fun ensureInitialized() {
        check(isInitialized) { "AudioDRCHelper not initialized. Call init(context) before." }
    }
}