package it.fast4x.androidyoutubeplayer.core.player

import it.fast4x.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener

/**
 * Use this interface to control the playback of YouTube videos and to listen to their events.
 */
interface YouTubePlayer {
  /**
   * Loads and automatically plays the video.
   * @param videoId id of the video
   * @param startSeconds the time from which the video should start playing
   */
  fun loadVideo(videoId: String, startSeconds: Float)

  /**
   * Loads the video's thumbnail and prepares the player to play the video. Does not automatically play the video.
   * @param videoId id of the video
   * @param startSeconds the time from which the video should start playing
   */
  fun cueVideo(videoId: String, startSeconds: Float)

  /**
   * Pre-cues the next video on the standby player deck so track transitions can swap decks without a cold start.
   * @param videoId id of the next video to pre-cue
   * @param startSeconds the time from which the next video should start playing
   */
  fun preloadVideo(videoId: String, startSeconds: Float = 0f) {}

  /**
   * Starts an overlapping dual-deck crossfade in the WebView: the outgoing deck fades out over
   * [durationMs] while the standby deck immediately starts playing [nextVideoId] and fades in.
   */
  fun startCrossfade(nextVideoId: String, durationMs: Int) {}

  fun play()
  fun pause()

  /** If the player is playing a playlist, play the next video. */
  fun nextVideo()
  /** If the player is playing a playlist, play the previous video. */
  fun previousVideo()
  /** If the player is playing a playlist, play the video at position [index]. */
  fun playVideoAt(index: Int)

  /** If the player is playing a playlist, enable or disable looping of the playlist. */
  fun setLoop(loop: Boolean)

  /** If the player is playing a playlist, enable or disable shuffling of the playlist. */
  fun setShuffle(shuffle: Boolean)

  fun mute()
  fun unMute()

  /** Returns true if the player is muted, false otherwise. */
  fun isMutedAsync(callback: BooleanProvider)

  /**
   * @param volumePercent Integer between 0 and 100
   */
  fun setVolume(volumePercent: Int)

  /**
   * @return The current volume in percent.
   */
  fun getVolume()

  /**
   *
   * @param time The absolute time in seconds to seek to
   */
  fun seekTo(time: Float)

  fun setPlaybackRate(playbackRate: PlayerConstants.PlaybackRate)

  fun addListener(listener: YouTubePlayerListener): Boolean
  fun removeListener(listener: YouTubePlayerListener): Boolean
}
