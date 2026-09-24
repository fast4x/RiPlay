package it.fast4x.riplay.utils

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager


fun getDeviceVolume(context: Context): Float {
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    val volumeLevel: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return volumeLevel.toFloat() / maxVolume
}

fun setDeviceVolume(context: Context, volume: Float) {
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
}
