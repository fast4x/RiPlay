package it.fast4x.riplay.extensions.players

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import it.fast4x.riplay.utils.globalContext

fun audioPlayer(uri: String) {
    val exoPlayer by lazy { ExoPlayer.Builder(globalContext()).build() }
    val mediaItem = MediaItem.fromUri(uri)
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
}