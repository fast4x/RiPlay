package it.fast4x.riplay.extensions.chromecast

import android.view.View
import android.widget.Button
import androidx.lifecycle.Lifecycle
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.chromecast.ui.SimpleChromeCastUiController
import it.fast4x.riplay.extensions.chromecast.utils.VideoIdsProvider


/**
 * Class used to manage the two YouTubePlayers, local and cast.
 * The local YouTubePlayer is supposed to stop playing when the cast player stars and vice versa.
 * When one of the two players stops, the other has to resume the playback from where the previous player stopped.
 */
class YouTubePlayersManager internal constructor(
    localYouTubePlayerInitListener: LocalYouTubePlayerInitListener,
    private val youtubePlayerView: YouTubePlayerView, chromecastControls: View,
    private val chromecastPlayerListener: YouTubePlayerListener, private val lifeCycle: Lifecycle
) : ChromecastConnectionListener {
    private val chromecastUiController: SimpleChromeCastUiController
    private val chromecastPlayerStateTracker = YouTubePlayerTracker()
    private val localPlayerStateTracker = YouTubePlayerTracker()
    private var localYouTubePlayer: YouTubePlayer? = null
    private var chromecastYouTubePlayer: YouTubePlayer? = null
    private var playingOnCastPlayer = false

    init {
        val nextVideoButton = chromecastControls.findViewById<Button?>(R.id.next_video_button)
        if (nextVideoButton != null) {
            nextVideoButton.visibility = View.VISIBLE
        }
        chromecastUiController = SimpleChromeCastUiController(chromecastControls)

        initLocalYouTube(localYouTubePlayerInitListener)
        nextVideoButton?.setOnClickListener { view: View? ->
            if (chromecastYouTubePlayer != null) chromecastYouTubePlayer!!.loadVideo(
                VideoIdsProvider.nextVideoId,
                0f
            )
        }
    }

    override fun onChromecastConnecting() {
        if (localYouTubePlayer != null) localYouTubePlayer!!.pause()
    }

    override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        println("onChromecastConnected chromecastYouTubePlayerContext: $chromecastYouTubePlayerContext")
        initializeCastPlayer(chromecastYouTubePlayerContext)
        playingOnCastPlayer = true
    }

    override fun onChromecastDisconnected() {
        if (localYouTubePlayer != null && chromecastPlayerStateTracker.videoId != null) {
            if (chromecastPlayerStateTracker.state == PlayerConstants.PlayerState.PLAYING) localYouTubePlayer!!.loadOrCueVideo(
                lifeCycle,
                chromecastPlayerStateTracker.videoId!!,
                chromecastPlayerStateTracker.currentSecond
            )
            else localYouTubePlayer!!.cueVideo(
                chromecastPlayerStateTracker.videoId!!,
                chromecastPlayerStateTracker.currentSecond
            )
        }

        chromecastUiController.resetUi()
        playingOnCastPlayer = false
    }

    fun getChromecastUiController(): SimpleChromeCastUiController {
        return chromecastUiController
    }

    fun togglePlayback() {
        if (playingOnCastPlayer && chromecastYouTubePlayer != null) if (chromecastPlayerStateTracker.state == PlayerConstants.PlayerState.PLAYING) chromecastYouTubePlayer!!.pause()
        else chromecastYouTubePlayer!!.play()
        else if (localYouTubePlayer != null) if (localPlayerStateTracker.state == PlayerConstants.PlayerState.PLAYING) localYouTubePlayer!!.pause()
        else localYouTubePlayer!!.play()
    }

    private fun initLocalYouTube(localYouTubePlayerInitListener: LocalYouTubePlayerInitListener) {
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                localYouTubePlayer = youTubePlayer
                youTubePlayer.addListener(localPlayerStateTracker)

                if (!playingOnCastPlayer) youTubePlayer.loadOrCueVideo(
                    lifeCycle,
                    VideoIdsProvider.nextVideoId, chromecastPlayerStateTracker.currentSecond
                )

                localYouTubePlayerInitListener.onLocalYouTubePlayerInit()
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                if (playingOnCastPlayer && localPlayerStateTracker.state == PlayerConstants.PlayerState.PLAYING) youTubePlayer.pause()
            }
        })
    }

    private fun initializeCastPlayer(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
        println("initializeChromeCastPlayer")

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(youTubePlayer, error)
                println("initializeChromeCastPlayer onError error: $error")
            }
            override fun onApiChange(youTubePlayer: YouTubePlayer) {
                super.onApiChange(youTubePlayer)
                println("initializeChromeCastPlayer onApiChange")
            }
            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                println("initializeChromeCastPlayer onStateChange state: $state")
            }
            override fun onReady(youTubePlayer: YouTubePlayer) {
                println("initialize ChromeCastPlayer onReady")
                chromecastYouTubePlayer = youTubePlayer

                chromecastUiController.setYouTubePlayer(youTubePlayer)

                youTubePlayer.addListener(chromecastPlayerListener)
                youTubePlayer.addListener(chromecastPlayerStateTracker)
                youTubePlayer.addListener(chromecastUiController)

                println("initializeCastPlayer localPlayerStateTracker.videoId ${localPlayerStateTracker.videoId}")

                if (localPlayerStateTracker.videoId != null) youTubePlayer.loadVideo(
                    localPlayerStateTracker.videoId!!, localPlayerStateTracker.currentSecond
                )
            }
        }

        chromecastYouTubePlayerContext.initialize(listener)
    }

    /**
     * Interface used to notify its listeners than the local YouTubePlayer is ready to play videos.
     */
    internal interface LocalYouTubePlayerInitListener {
        fun onLocalYouTubePlayerInit()
    }
}
