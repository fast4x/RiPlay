package it.fast4x.riplay.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import it.fast4x.androidyoutubeplayer.cast.ChromecastYouTubePlayerContext
import it.fast4x.androidyoutubeplayer.cast.io.infrastructure.ChromecastConnectionListener
import timber.log.Timber

object CastHelper {
    val isCastAvailable: Boolean = true

    fun init(context: Context): CastContext =
        CastContext.getSharedInstance(context)

    fun initChromecastYouTubePlayerContext(context: Context): ChromecastYouTubePlayerContext =
        ChromecastYouTubePlayerContext(
            CastContext.getSharedInstance(context).sessionManager,
            object : ChromecastConnectionListener {
                override fun onChromecastConnecting() {
                    Timber.d("PlayerService onlinePlayerView: onChromecastConnecting")
                }

                override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
                    Timber.d("PlayerService onlinePlayerView: onChromecastConnected")
                }

                override fun onChromecastDisconnected() {
                    Timber.d("PlayerService onlinePlayerView: onChromecastDisconnected")
                }
            }
        )

}