package it.fast4x.riplay.ui.screens.player.online.components.customui

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.R

internal class CustomBasePlayerUiControllerAsListener (
    private val context: Context,
    customPlayerUi: View,
    private val youTubePlayer: YouTubePlayer,
    private val youTubePlayerView: YouTubePlayerView,
    private val onTap: () -> Unit? = {}
) : AbstractYouTubePlayerListener() {
    private val playerTracker: YouTubePlayerTracker = YouTubePlayerTracker()
    private var controlsContainer: View? = null
    private var panel: View? = null
    private var progressbar: View? = null

    init {
        controlsContainer = customPlayerUi.findViewById(R.id.controls_container)
        panel = customPlayerUi.findViewById<View?>(R.id.panel)
        progressbar = customPlayerUi.findViewById<View?>(R.id.progress)

        panel?.setOnClickListener { onTap() }

        progressbar?.visibility = View.GONE
        panel?.visibility = View.VISIBLE

        youTubePlayer.addListener(playerTracker)

    }

    override fun onReady(youTubePlayer: YouTubePlayer) {
        progressbar?.visibility = View.GONE
    }

    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
        if (state === PlayerConstants.PlayerState.PLAYING || state === PlayerConstants.PlayerState.PAUSED || state === PlayerConstants.PlayerState.VIDEO_CUED) {
            panel?.setBackgroundColor(ContextCompat.getColor(panel!!.context, android.R.color.transparent))
        } else {
            if (state === PlayerConstants.PlayerState.BUFFERING) {
                panel?.setBackgroundColor(
                    ContextCompat.getColor(
                        panel!!.context,
                        android.R.color.transparent
                    )
                )

            }

        }
    }

}