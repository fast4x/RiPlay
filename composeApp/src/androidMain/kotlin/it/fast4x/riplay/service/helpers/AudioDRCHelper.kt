package it.fast4x.riplay.service.helpers

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

    fun disableDRC() {
        ensureInitialized()
        try {

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