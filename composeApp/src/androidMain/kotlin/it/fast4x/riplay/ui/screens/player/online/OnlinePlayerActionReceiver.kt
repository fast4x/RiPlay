package it.fast4x.riplay.ui.screens.player.online

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.MainActivity

class OnlinePlayerActionReceiver(
    private val onAction: (Boolean) -> Unit = {}
) : BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        println("OnlinePlayerActionReceiver onReceive intent.action: ${intent.action}")
        when (intent.action) {
            MainActivity.Action.pause.value -> {
                println("OnlinePlayer LauncheEffect it.fast4x.riplay.onlineplayer.pause")
                //player?.pause()
                onAction(false)
            }
            MainActivity.Action.play.value -> {
                println("OnlinePlayer LauncheEffect it.fast4x.riplay.onlineplayer.play")
                //player?.play()
                onAction(true)
            }
        }
    }
}