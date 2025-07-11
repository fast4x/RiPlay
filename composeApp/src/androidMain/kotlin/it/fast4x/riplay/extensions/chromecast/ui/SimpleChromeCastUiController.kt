package it.fast4x.riplay.extensions.chromecast.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.utils.TimeUtilities.formatTime
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import it.fast4x.riplay.R

/**
 * Class used to control a simple Ui for the cast player.
 */
class SimpleChromeCastUiController(private val controls_view: View) :
    AbstractYouTubePlayerListener(), OnSeekBarChangeListener {
    private val progressBar: View
    private val playPauseButton: ImageView
    private val currentTimeTextView: TextView
    private val totalTimeTextView: TextView
    private val seekBar: SeekBar
    private val youTubeButton: ImageView
    private val newViewsContainer: FrameLayout
    private var youTubePlayer: YouTubePlayer? = null
    private var isPlaying = false
    private var seekBarTouchStarted = false

    // I need this variable because onCurrentSecond gets called every 100 mill, so without the proper checks on this variable in onCurrentSeconds the seek bar glitches when touched.
    private var newSeekBarProgress = -1

    init {
        progressBar = controls_view.findViewById<View?>(R.id.progress_bar)
        playPauseButton = controls_view.findViewById<ImageView?>(R.id.play_pause_button)
        currentTimeTextView = controls_view.findViewById<TextView?>(R.id.current_time_text_view)
        totalTimeTextView = controls_view.findViewById<TextView?>(R.id.total_time_text_view)
        seekBar = controls_view.findViewById<SeekBar?>(R.id.seek_bar)
        youTubeButton = controls_view.findViewById<ImageView?>(R.id.youtube_button)
        newViewsContainer = controls_view.findViewById<FrameLayout?>(R.id.cast_button_container)

        seekBar.setOnSeekBarChangeListener(this)
        playPauseButton.setOnClickListener(View.OnClickListener { view: View? -> onPlayButtonPressed() })
    }

    fun setYouTubePlayer(youTubePlayer: YouTubePlayer?) {
        this.youTubePlayer = youTubePlayer
    }

    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
        newSeekBarProgress = -1

        updateControlsState(state)

        if (state == PlayerConstants.PlayerState.PLAYING || state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.VIDEO_CUED || state == PlayerConstants.PlayerState.UNSTARTED) {
            progressBar.visibility = View.INVISIBLE
            playPauseButton.visibility = View.VISIBLE
        } else if (state == PlayerConstants.PlayerState.BUFFERING) {
            progressBar.visibility = View.VISIBLE
            playPauseButton.visibility = View.INVISIBLE
        }

        val playing = state == PlayerConstants.PlayerState.PLAYING
        updatePlayPauseButtonIcon(playing)
    }

    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
        totalTimeTextView.text = formatTime(duration)
        seekBar.max = duration.toInt()
    }

    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, currentSecond: Float) {
        if (seekBarTouchStarted) return

        // ignore if the current time is older than what the user selected with the SeekBar
        if (newSeekBarProgress > 0 && formatTime(currentSecond) != formatTime(newSeekBarProgress.toFloat())) return

        newSeekBarProgress = -1
        seekBar.progress = currentSecond.toInt()
    }

    override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {
        seekBar.secondaryProgress = loadedFraction.toInt()
    }

    override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
        youTubeButton.setOnClickListener(View.OnClickListener { view: View? ->
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=\$videoId"))
            controls_view.context.startActivity(intent)
        })
    }

    fun addView(view: View?) {
        newViewsContainer.addView(view)
    }

    fun removeView(view: View?) {
        newViewsContainer.removeView(view)
    }

    private fun updateControlsState(state: PlayerConstants.PlayerState) {
        when (state) {
            PlayerConstants.PlayerState.ENDED -> isPlaying = false
            PlayerConstants.PlayerState.PAUSED -> isPlaying = false
            PlayerConstants.PlayerState.PLAYING -> isPlaying = true
            PlayerConstants.PlayerState.BUFFERING -> isPlaying = false
            PlayerConstants.PlayerState.UNSTARTED -> resetUi()
            PlayerConstants.PlayerState.UNKNOWN -> isPlaying = false
            PlayerConstants.PlayerState.VIDEO_CUED -> isPlaying = false
        }

        updatePlayPauseButtonIcon(!isPlaying)
    }

    fun resetUi() {
        seekBar.progress = 0
        seekBar.max = 0
        playPauseButton.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        currentTimeTextView.post(Runnable { currentTimeTextView.text = "" })
        totalTimeTextView.post(Runnable { totalTimeTextView.text = "" })
    }

    // -- SeekBar, this code will be refactored
    private fun updatePlayPauseButtonIcon(playing: Boolean) {
        val img: Int = if (playing) R.drawable.ayp_ic_pause_36dp else R.drawable.ayp_ic_play_36dp
        playPauseButton.setImageResource(img)
    }

    private fun onPlayButtonPressed() {
        if (youTubePlayer == null) return

        if (isPlaying) youTubePlayer!!.pause()
        else youTubePlayer!!.play()
    }

    override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
        currentTimeTextView.text = formatTime(i.toFloat())
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        seekBarTouchStarted = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (isPlaying) newSeekBarProgress = seekBar.progress

        if (youTubePlayer != null) youTubePlayer!!.seekTo(seekBar.progress.toFloat())
        seekBarTouchStarted = false
    }
}
