package it.fast4x.riplay.ui.screens.player.online

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.utils.FadeViewHelper
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.views.YouTubePlayerSeekBar
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.views.YouTubePlayerSeekBarListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.R

internal class CustomPlayerUiController (
    private val context: Context,
    customPlayerUi: View,
    private val youTubePlayer: YouTubePlayer,
    private val youTubePlayerView: YouTubePlayerView,
    private val onTap: () -> Unit? = {}
) : AbstractYouTubePlayerListener() {
    private val playerTracker: YouTubePlayerTracker = YouTubePlayerTracker()

    // panel is used to intercept clicks on the WebView, I don't want the user to be able to click the WebView directly.
    private var controlsContainer: View? = null
    private var fadeControlsContainer: FadeViewHelper? = null
    private var panel: View? = null
    private var progressbar: View? = null
    private var videoCurrentTimeTextView: TextView? = null
    private var videoDurationTextView: TextView? = null
    private var enterExitFullscreenButton: ImageView? = null
    private var youtubeButton: ImageView? = null
    private var youtubePlayerSeekBar: YouTubePlayerSeekBar? = null
    private var playPauseButton: ImageView? = null
    private var customActionLeft: ImageView? = null
    private var customActionRight: ImageView? = null

    private var fullscreen = false

    init {
        //initViews(customPlayerUi)
        controlsContainer = customPlayerUi.findViewById(R.id.controls_container)
        fadeControlsContainer = FadeViewHelper(controlsContainer as View)
        panel = customPlayerUi.findViewById<View?>(R.id.panel)
        progressbar = customPlayerUi.findViewById<View?>(R.id.progress)
        videoCurrentTimeTextView = customPlayerUi.findViewById<TextView?>(R.id.video_current_time)
        videoDurationTextView = customPlayerUi.findViewById<TextView?>(R.id.video_duration)
        youtubeButton = customPlayerUi.findViewById<ImageView?>(R.id.youtube_button)
        youtubeButton?.visibility = View.GONE
        youtubePlayerSeekBar = customPlayerUi.findViewById<YouTubePlayerSeekBar?>(R.id.youtube_player_seekbar)
        playPauseButton = customPlayerUi.findViewById<ImageView?>(R.id.play_pause_button)
        playPauseButton?.setColorFilter(Color.Companion.White.hashCode())
        customActionLeft = customPlayerUi.findViewById<ImageView?>(R.id.custom_action_left_button)
        customActionRight = customPlayerUi.findViewById<ImageView?>(R.id.custom_action_right_button)
        customActionLeft?.setImageResource(R.drawable.play_skip_back)
        customActionLeft?.setColorFilter(Color.Companion.White.hashCode())
        customActionRight?.setImageResource(R.drawable.play_skip_forward)
        customActionRight?.setColorFilter(Color.Companion.White.hashCode())


        enterExitFullscreenButton =
            customPlayerUi.findViewById<ImageView?>(R.id.fullscreen_button)

        playPauseButton?.setOnClickListener(View.OnClickListener { view: View? ->
            if (playerTracker.state == PlayerConstants.PlayerState.PLAYING) youTubePlayer.pause()
            else youTubePlayer.play()
        })

        customActionLeft?.setOnClickListener(View.OnClickListener { view: View? ->
            youTubePlayer.previousVideo()
        })

        customActionRight?.setOnClickListener(View.OnClickListener { view: View? ->
            youTubePlayer.nextVideo()
        })

        enterExitFullscreenButton?.setOnClickListener(View.OnClickListener { view: View? ->
            if (fullscreen) youTubePlayerView.wrapContent()
            else youTubePlayerView.matchParent()
            fullscreen = !fullscreen
        })

        youtubePlayerSeekBar?.youtubePlayerSeekBarListener = object : YouTubePlayerSeekBarListener {
            override fun seekTo(time: Float) = youTubePlayer.seekTo(time)
        }

        panel?.setOnClickListener {
            //fadeControlsContainer?.toggleVisibility()
            onTap()
        }

        //Disable controls in view
        customActionLeft?.visibility = View.GONE
        customActionRight?.visibility = View.GONE
        playPauseButton?.visibility = View.GONE
        youtubePlayerSeekBar?.visibility = View.GONE
        youtubeButton?.visibility = View.GONE
        videoCurrentTimeTextView?.visibility = View.GONE
        videoDurationTextView?.visibility = View.GONE
        progressbar?.visibility = View.GONE
        enterExitFullscreenButton?.visibility = View.GONE
        panel?.visibility = View.VISIBLE

        youTubePlayer.addListener(youtubePlayerSeekBar as YouTubePlayerListener)
        youTubePlayer.addListener(playerTracker)
        youTubePlayer.addListener(fadeControlsContainer as FadeViewHelper)

    }

    override fun onReady(youTubePlayer: YouTubePlayer) {
        progressbar?.visibility = View.GONE
    }

    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
        if (state === PlayerConstants.PlayerState.PLAYING || state === PlayerConstants.PlayerState.PAUSED || state === PlayerConstants.PlayerState.VIDEO_CUED) {
            panel?.setBackgroundColor(ContextCompat.getColor(panel!!.context, android.R.color.transparent))
//            progressbar?.visibility = View.GONE
//
//            playPauseButton?.visibility = View.VISIBLE
//            customActionLeft?.visibility = View.VISIBLE
//            customActionRight?.visibility = View.VISIBLE

            updatePlayPauseButtonIcon(state === PlayerConstants.PlayerState.PLAYING)
        }
        else {
            updatePlayPauseButtonIcon(false)

            if (state === PlayerConstants.PlayerState.BUFFERING) {
               //progressbar?.visibility = View.VISIBLE
                panel?.setBackgroundColor(
                    ContextCompat.getColor(
                        panel!!.context,
                        android.R.color.transparent
                    )
                )
//                playPauseButton?.visibility = View.INVISIBLE
//
//                customActionLeft?.visibility = View.GONE
//                customActionRight?.visibility = View.GONE
            }

//            if (state === PlayerConstants.PlayerState.UNSTARTED) {
//                progressbar?.visibility = View.GONE
//                playPauseButton?.visibility = View.VISIBLE
//            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
        videoCurrentTimeTextView?.text = second.toString() + ""
    }

    @SuppressLint("SetTextI18n")
    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
        videoDurationTextView?.text = duration.toString() + ""
    }

    private fun updatePlayPauseButtonIcon(playing: Boolean) {
        val drawable = if (playing) R.drawable.pause else R.drawable.play
        playPauseButton?.setImageResource(drawable)
    }


}