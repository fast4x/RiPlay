package it.fast4x.riplay.ui.screens.player.controller

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri

class ExoPlayerWrapper(
    private val context: Context,
    private val viewModel: PlayerCodaViewModel
) : PlayerController {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.onPlayerStateChanged(isPlaying, exoPlayer.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                viewModel.onPlayerStateChanged(exoPlayer.isPlaying, playbackState)
            }
        })
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun caricaMedia(item: MediaItemGenerico) {
        val mediaItem = ExoMediaItem.fromUri(item.uri.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    override fun ferma() {
        exoPlayer.stop()
    }

    override fun rilascia() {
        exoPlayer.release()
    }
}