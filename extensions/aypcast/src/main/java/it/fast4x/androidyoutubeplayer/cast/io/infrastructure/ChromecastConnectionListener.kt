package it.fast4x.androidyoutubeplayer.cast.io.infrastructure

import it.fast4x.androidyoutubeplayer.cast.ChromecastYouTubePlayerContext

/**
 * Implement this interface to be notified about changes in the cast connection.
 */
interface ChromecastConnectionListener {
  fun onChromecastConnecting()
  fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext)
  fun onChromecastDisconnected()
}