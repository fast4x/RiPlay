package it.fast4x.riplay.extensions.chromecast.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import timber.log.Timber
import java.util.Objects


/**
 * This broadcast receiver is used to react to notification actions.
 */
class PlaybackControllerBroadcastReceiver(private val togglePlayback: Runnable) :
    BroadcastReceiver(), ChromecastConnectionListener {
    private var chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext? = null

    override fun onReceive(context: Context?, intent: Intent) {
        Timber.d(javaClass.simpleName, "intent received: %s", intent.action)

        when (Objects.requireNonNull<String?>(intent.action)) {
            TOGGLE_PLAYBACK -> togglePlayback.run()
            STOP_CAST_SESSION -> chromecastYouTubePlayerContext!!.endCurrentSession()
        }
    }

    override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        this.chromecastYouTubePlayerContext = chromecastYouTubePlayerContext
    }

    override fun onChromecastConnecting() {
    }

    override fun onChromecastDisconnected() {
    }

    companion object {
        const val TOGGLE_PLAYBACK: String =
            "it.fast4x.riplay.extensions.chromecast.TOGGLE_PLAYBACK"
        const val STOP_CAST_SESSION: String =
            "it.fast4x.riplay.extensions.chromecast.STOP_CAST_SESSION"
    }
}
