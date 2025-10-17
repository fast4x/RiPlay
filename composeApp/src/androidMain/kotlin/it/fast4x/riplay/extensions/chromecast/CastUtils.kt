package it.fast4x.riplay.extensions.chromecast

import com.google.android.gms.cast.framework.CastContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import it.fast4x.riplay.utils.appContext
import timber.log.Timber
import java.util.concurrent.Executors


fun initChromecast() {
    println("initChromecast")
    val castExecutor = Executors.newSingleThreadExecutor()
    var castContext: CastContext? = null
    CastContext.getSharedInstance(appContext(), castExecutor).addOnCompleteListener {
        println("initChromecast addOnCompleteListener CastContext.getSharedInstance")
        castContext = it.result
        println("initChromecast addOnCompleteListener CastContext.getSharedInstance castContext: $castContext")
        ChromecastYouTubePlayerContext(
            castContext.sessionManager,
            SimpleChromecastConnectionListener()
        )
    }

}

class SimpleChromecastConnectionListener : ChromecastConnectionListener {
    override fun onChromecastConnecting() {
        Timber.d(javaClass.simpleName, "onChromecastConnecting")
        println("onChromecastConnecting")
    }

    override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        Timber.d(javaClass.simpleName, "onChromecastConnected")
        println("onChromecastConnected")
        initializeCastPlayer(chromecastYouTubePlayerContext)
    }

    override fun onChromecastDisconnected() {
        Timber.d(javaClass.simpleName, "onChromecastDisconnected")
        println("onChromecastDisconnected")

    }

    fun initializeCastPlayer(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        println("initializeCastPlayer Chromecast")
        chromecastYouTubePlayerContext.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                println("onReady Chromecast")
                youTubePlayer.loadVideo("S0Q4gqBUs7c", 0f)
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(youTubePlayer, error)
                println("onError Chromecast $error")

            }
        })
    }
}


