package it.fast4x.riplay.service.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class NoisyAudioReceiver(
    private val context: Context,
    private val onAudioBecomingNoisy: () -> Unit
) {

    private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onAudioBecomingNoisy()
            }
        }
    }

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            context.registerReceiver(receiver, noisyIntentFilter)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(receiver)
            isRegistered = false
        }
    }
}