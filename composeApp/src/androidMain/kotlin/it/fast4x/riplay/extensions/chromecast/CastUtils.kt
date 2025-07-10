package it.fast4x.riplay.extensions.chromecast

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.compose.ui.graphics.Color
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.tasks.Task
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import it.fast4x.riplay.appContext
import it.fast4x.riplay.context
import it.fast4x.riplay.typography
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.Executors


fun initChromecast() {
    println("initChromecast")
    val castExecutor = Executors.newSingleThreadExecutor()
//    val castContext = CastContext.getSharedInstance(appContext(), castExecutor).result
//    val sessionManager = castContext.sessionManager
//    ChromecastYouTubePlayerContext(
//        sessionManager,
//        SimpleChromecastConnectionListener()
//    )

    //val castContext = getCastContext(appContext())
//    val sessionManager = castContext.sessionManager
//    ChromecastYouTubePlayerContext(
//        sessionManager,
//        SimpleChromecastConnectionListener()
//    )

//   val mCastStateListener = CastStateListener { newState ->
//        if (newState != CastState.NO_DEVICES_AVAILABLE) {
//            showIntroductoryOverlay()
//        }
//    }
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


