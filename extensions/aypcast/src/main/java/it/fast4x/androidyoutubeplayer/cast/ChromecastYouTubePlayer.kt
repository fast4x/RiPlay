package it.fast4x.androidyoutubeplayer.cast

import it.fast4x.androidyoutubeplayer.cast.io.infrastructure.ChromecastCommunicationChannel
import it.fast4x.androidyoutubeplayer.cast.io.youtube.ChromecastCommunicationConstants
import it.fast4x.androidyoutubeplayer.cast.io.youtube.ChromecastYouTubeMessageDispatcher
import it.fast4x.androidyoutubeplayer.cast.utils.JSONUtils
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayerBridge
import it.fast4x.androidyoutubeplayer.core.player.BooleanProvider
import it.fast4x.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import it.fast4x.androidyoutubeplayer.core.player.toFloat

class ChromecastYouTubePlayer internal constructor(private val chromecastCommunicationChannel: ChromecastCommunicationChannel) :
  YouTubePlayer, YouTubePlayerBridge.YouTubePlayerBridgeCallbacks {

  private lateinit var youTubePlayerInitListener: (YouTubePlayer) -> Unit

  private val inputMessageDispatcher = ChromecastYouTubeMessageDispatcher(YouTubePlayerBridge(this))
  private val youTubePlayerListeners = HashSet<YouTubePlayerListener>()

  internal fun initialize(initListener: (YouTubePlayer) -> Unit) {
    youTubePlayerListeners.clear()

    youTubePlayerInitListener = initListener

    chromecastCommunicationChannel.addObserver(inputMessageDispatcher)
  }

  override fun onYouTubeIFrameAPIReady() {
    youTubePlayerInitListener(this)
  }

  override fun getInstance(): YouTubePlayer {
    return this
  }

  override fun loadVideo(videoId: String, startSeconds: Float) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.LOAD,
      "videoId" to videoId,
      "startSeconds" to startSeconds.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun cueVideo(videoId: String, startSeconds: Float) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.CUE,
      "videoId" to videoId,
      "startSeconds" to startSeconds.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun play() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.PLAY
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun pause() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.PAUSE
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun nextVideo() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.PLAY_NEXT_VIDEO
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun previousVideo() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.PLAY_PREVIOUS_VIDEO
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun playVideoAt(index: Int) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.PLAY_VIDEO_AT,
      "index" to index.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun setLoop(loop: Boolean) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.SET_LOOP,
      "loop" to loop.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun setShuffle(shuffle: Boolean) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.SET_SHUFFLE,
      "shuffle" to shuffle.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun mute() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.MUTE
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun unMute() {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.UNMUTE
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun isMutedAsync(callback: BooleanProvider) {
    throw NotImplementedError("isMutedAsync is not implemented in ChromecastYouTubePlayer")
  }

  override fun setVolume(volumePercent: Int) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.SET_VOLUME,
      "volumePercent" to volumePercent.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun seekTo(time: Float) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.SEEK_TO,
      "time" to time.toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override fun setPlaybackRate(playbackRate: PlayerConstants.PlaybackRate) {
    val message = JSONUtils.buildFlatJson(
      "command" to ChromecastCommunicationConstants.SET_PLAYBACK_RATE,
      "playbackRate" to playbackRate.toFloat().toString()
    )

    chromecastCommunicationChannel.sendMessage(message)
  }

  override val listeners: Collection<YouTubePlayerListener> get() =  youTubePlayerListeners
  override fun addListener(listener: YouTubePlayerListener): Boolean = youTubePlayerListeners.add(listener)
  override fun removeListener(listener: YouTubePlayerListener): Boolean = youTubePlayerListeners.remove(listener)
}
