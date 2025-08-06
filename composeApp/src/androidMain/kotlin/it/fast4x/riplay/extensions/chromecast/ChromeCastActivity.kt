package it.fast4x.riplay.extensions.chromecast

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.utils.PlayServicesUtils.checkGooglePlayServicesAvailability
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.extensions.chromecast.notifications.NotificationManager
import it.fast4x.riplay.extensions.chromecast.notifications.PlaybackControllerBroadcastReceiver
import it.fast4x.riplay.extensions.chromecast.utils.MediaRouteButtonUtils
import it.fast4x.riplay.extensions.chromecast.YouTubePlayersManager.LocalYouTubePlayerInitListener
import java.util.concurrent.Executors

/**
 * Example Activity used to showcase how to use the chromecast-youtube-library extension to cast videos to a Chromecast device.
 * See documentation here: [chromecast-youtube-player](https://github.com/PierfrancescoSoffritti/chromecast-youtube-player)
 */
class ChromeCastActivity : AppCompatActivity(), LocalYouTubePlayerInitListener,
    ChromecastConnectionListener {
    private val googlePlayServicesAvailabilityRequestCode = 1

    private var youTubePlayersManager: YouTubePlayersManager? = null
    private lateinit var mediaRouteButton: MediaRouteButton

    private var notificationManager: NotificationManager? = null
    private var playbackControllerBroadcastReceiver: PlaybackControllerBroadcastReceiver? = null

    private var youTubePlayerView: YouTubePlayerView? = null
    private var chromeCastControlsRoot: View? = null
    private var mediaRouteButtonRoot: ViewGroup? = null

    private var connectedToChromeCast = false

    private var chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext? = null

    private val chromecastPlayerUiMediaRouteButtonContainer: MediaRouteButtonContainer =
        object : MediaRouteButtonContainer {
            override fun addMediaRouteButton(mediaRouteButton: MediaRouteButton?) {
                youTubePlayersManager!!.getChromecastUiController().addView(mediaRouteButton)
            }

            override fun removeMediaRouteButton(mediaRouteButton: MediaRouteButton?) {
                youTubePlayersManager!!.getChromecastUiController().removeView(mediaRouteButton)
            }
        }

    private val localPlayerUiMediaRouteButtonContainer: MediaRouteButtonContainer =
        object : MediaRouteButtonContainer {
            override fun addMediaRouteButton(mediaRouteButton: MediaRouteButton?) {
                mediaRouteButtonRoot!!.addView(mediaRouteButton)
            }

            override fun removeMediaRouteButton(mediaRouteButton: MediaRouteButton?) {
                mediaRouteButtonRoot!!.removeView(mediaRouteButton)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chromecast_example)

        youTubePlayerView = findViewById<YouTubePlayerView?>(R.id.youtube_player_view)
        mediaRouteButtonRoot = findViewById<ViewGroup?>(R.id.media_route_button_root)
        chromeCastControlsRoot = findViewById<View?>(R.id.chromecast_controls_root)

        lifecycle.addObserver(youTubePlayerView!!)

        notificationManager = NotificationManager(this, ChromeCastActivity::class.java)
        mediaRouteButton = MediaRouteButtonUtils.initMediaRouteButton(this)
        //mediaRouteButtonRoot!!.addView(mediaRouteButton)

        youTubePlayersManager = YouTubePlayersManager(
            this,
            youTubePlayerView!!,
            chromeCastControlsRoot!!,
            notificationManager as NotificationManager,
            lifecycle
        )

        registerBroadcastReceiver()

        // can't use CastContext until I'm sure the user has GooglePlayServices
        checkGooglePlayServicesAvailability(
            this,
            googlePlayServicesAvailabilityRequestCode,
            Runnable { this.initChromeCast() }
        )
        println("ChromeCastActivity onCreate:")
    }

    public override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(playbackControllerBroadcastReceiver)
        if (chromecastYouTubePlayerContext != null) {
            chromecastYouTubePlayerContext!!.release()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // can't use CastContext until I'm sure the user has GooglePlayServices
        if (requestCode == googlePlayServicesAvailabilityRequestCode) checkGooglePlayServicesAvailability(
            this,
            googlePlayServicesAvailabilityRequestCode,
            Runnable { this.initChromeCast() })
    }

    private fun initChromeCast() {
        val castExecutor = Executors.newSingleThreadExecutor()
        var castContext: CastContext? = null
        CastContext.getSharedInstance(applicationContext, castExecutor).addOnCompleteListener {
            println("initChromecast addOnCompleteListener CastContext.getSharedInstance")
            castContext = it.result
            println("initChromecast addOnCompleteListener CastContext.getSharedInstance castContext: $castContext")
            chromecastYouTubePlayerContext = ChromecastYouTubePlayerContext(
                castContext.sessionManager,
                this,
                playbackControllerBroadcastReceiver as ChromecastConnectionListener,
                youTubePlayersManager as ChromecastConnectionListener
            )
        }

    }

    override fun onChromecastConnecting() {
    }

    override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        connectedToChromeCast = true

        updateUi(true)
        notificationManager?.showNotification()
    }

    override fun onChromecastDisconnected() {
        connectedToChromeCast = false

        updateUi(false)
        notificationManager?.dismissNotification()
    }

    override fun onLocalYouTubePlayerInit() {
        if (connectedToChromeCast) return

        MediaRouteButtonUtils.addMediaRouteButtonToPlayerUi(
            mediaRouteButton, R.color.ic_launcher_background,
            null, localPlayerUiMediaRouteButtonContainer
        )
    }

    private fun registerBroadcastReceiver() {
        playbackControllerBroadcastReceiver =
            PlaybackControllerBroadcastReceiver({ youTubePlayersManager!!.togglePlayback() })
        val filter: IntentFilter = IntentFilter(PlaybackControllerBroadcastReceiver.TOGGLE_PLAYBACK)
        filter.addAction(PlaybackControllerBroadcastReceiver.STOP_CAST_SESSION)
        ContextCompat.registerReceiver(
            applicationContext,
            playbackControllerBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateUi(connected: Boolean) {
        val disabledContainer =
            if (connected) localPlayerUiMediaRouteButtonContainer else chromecastPlayerUiMediaRouteButtonContainer
        val enabledContainer =
            if (connected) chromecastPlayerUiMediaRouteButtonContainer else localPlayerUiMediaRouteButtonContainer

        // the media route button has a single instance.
        // therefore it has to be moved from the local YouTube player Ui to the chromecast YouTube player Ui, and vice versa.
        MediaRouteButtonUtils.addMediaRouteButtonToPlayerUi(
            mediaRouteButton, R.color.ic_launcher_background,
            disabledContainer, enabledContainer
        )

        youTubePlayerView!!.visibility = if (connected) View.GONE else View.VISIBLE
        chromeCastControlsRoot!!.visibility = if (connected) View.VISIBLE else View.GONE
    }

    interface MediaRouteButtonContainer {
        fun addMediaRouteButton(mediaRouteButton: MediaRouteButton?)

        fun removeMediaRouteButton(mediaRouteButton: MediaRouteButton?)
    }
}
