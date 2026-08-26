package it.fast4x.androidyoutubeplayer.core.player.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import it.fast4x.androidyoutubeplayer.R
import it.fast4x.androidyoutubeplayer.core.player.BooleanProvider
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayerBridge
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayerCallbacks
import it.fast4x.androidyoutubeplayer.core.player.listeners.FullscreenListener
import it.fast4x.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import it.fast4x.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import it.fast4x.androidyoutubeplayer.core.player.toFloat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import androidx.core.graphics.createBitmap


private class YouTubePlayerImpl(
  private val webView: WebView,
  private val callbacks: YouTubePlayerCallbacks
) : YouTubePlayer {
  private val mainThread: Handler = Handler(Looper.getMainLooper())

  private val lock = Any()
  @GuardedBy("lock")
  private val listeners = mutableSetOf<YouTubePlayerListener>()

  override fun loadVideo(videoId: String, startSeconds: Float) = webView.invoke("loadVideo", videoId, startSeconds)
  override fun cueVideo(videoId: String, startSeconds: Float) = webView.invoke("cueVideo", videoId, startSeconds)
  override fun play() = webView.invoke("playVideo")
  override fun pause() = webView.invoke("pauseVideo")
  override fun nextVideo() = webView.invoke("nextVideo")
  override fun previousVideo() = webView.invoke("previousVideo")
  override fun playVideoAt(index: Int) = webView.invoke("playVideoAt", index)
  override fun setLoop(loop: Boolean) = webView.invoke("setLoop", loop)
  override fun setShuffle(shuffle: Boolean) = webView.invoke("setShuffle", shuffle)
  override fun mute() = webView.invoke("mute")
  override fun unMute() = webView.invoke("unMute")
  override fun isMutedAsync(callback: BooleanProvider) {
    val requestId = callbacks.registerBooleanCallback(callback)
    webView.invoke("getMuteValue", requestId)
  }
  override fun setVolume(volumePercent: Int) {
    require(volumePercent in 0..100) { "Volume must be between 0 and 100" }
    webView.invoke("setVolume", volumePercent)
  }
  override fun seekTo(time: Float) = webView.invoke("seekTo", time)
  override fun setPlaybackRate(playbackRate: PlayerConstants.PlaybackRate) = webView.invoke("setPlaybackRate", playbackRate.toFloat())
  override fun addListener(listener: YouTubePlayerListener) = synchronized(lock) { listeners.add(listener) }
  override fun removeListener(listener: YouTubePlayerListener) = synchronized(lock) { listeners.remove(listener) }

  fun getListeners(): Collection<YouTubePlayerListener> = synchronized(lock) { listeners.toList() }

  fun release() {
    synchronized(lock) { listeners.clear() }
    mainThread.removeCallbacksAndMessages(null)
  }

  private fun WebView.invoke(function: String, vararg args: Any) {
    val stringArgs = args.map {
      if (it is String) {
        "'$it'"
      }
      else {
        it.toString()
      }
    }
    mainThread.post { evaluateJavascript("$function(${stringArgs.joinToString(",")})", null) }
  }
}


/**
 * WebView implementation of [YouTubePlayer]. The player runs inside the WebView, using the IFrame Player API.
 */
internal class WebViewYouTubePlayer (
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), YouTubePlayerBridge.YouTubePlayerBridgeCallbacks {

  private val youTubePlayerCallbacks = YouTubePlayerCallbacks()
  private val _youTubePlayer = YouTubePlayerImpl(this, youTubePlayerCallbacks)
  internal val youtubePlayer: YouTubePlayer get() = _youTubePlayer

  private lateinit var youTubePlayerInitListener: (YouTubePlayer) -> Unit

  internal var isBackgroundPlaybackEnabled = false

  private val youTubePlayerBridge = YouTubePlayerBridge(this)

  internal fun initialize(initListener: (YouTubePlayer) -> Unit, playerOptions: IFramePlayerOptions?, videoId: String?) {
    youTubePlayerInitListener = initListener
    initWebView(playerOptions ?: IFramePlayerOptions.getDefault(context), videoId)
  }

  override val listeners: Collection<YouTubePlayerListener> get() = _youTubePlayer.getListeners()
  override fun getInstance(): YouTubePlayer = _youTubePlayer
  override fun onYouTubeIFrameAPIReady() = youTubePlayerInitListener(_youTubePlayer)
  fun addListener(listener: YouTubePlayerListener) = _youTubePlayer.addListener(listener)
  fun removeListener(listener: YouTubePlayerListener) = _youTubePlayer.removeListener(listener)

  override fun destroy() {
    _youTubePlayer.release()
    super.destroy()
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun initWebView(playerOptions: IFramePlayerOptions, videoId: String?) {

    // Configurazione hardware e reattività della View Android
    setLayerType(LAYER_TYPE_HARDWARE, null)
    isSoundEffectsEnabled = false
    isHapticFeedbackEnabled = false

    // Configurazione del motore Chromium (WebSettings)
    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      mediaPlaybackRequiresUserGesture = false
      cacheMode = WebSettings.LOAD_DEFAULT // Ottimizzazione aggressiva della cache

      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
      userAgentString = userAgent

    }

    // Registrazione dei Bridge (Prima del caricamento della pagina!)
    addJavascriptInterface(youTubePlayerBridge, "YouTubePlayerBridge")
    addJavascriptInterface(youTubePlayerCallbacks, "YouTubePlayerCallbacks")

    // 4. Configurazione snella del client grafico (Previene bug di rendering video in HTML5)
    webChromeClient = object : WebChromeClient() {
      override fun getDefaultVideoPoster(): Bitmap? {
        return super.getDefaultVideoPoster()
      }
    }

    // Generazione e Iniezione dell'HTML
    val htmlPage = readHTMLFromUTF8File(resources.openRawResource(R.raw.ayp_youtube_player))
      .replace("<<injectedVideoId>>", if (videoId != null) { "'$videoId'" } else { "undefined" })
      .replace("<<injectedPlayerVars>>", playerOptions.toString())

    // Caricamento definitivo impostando l'Origin corretto per bypassare le restrizioni CORS sui codec audio
    val baseUrl = playerOptions.getOrigin()
    loadDataWithBaseURL(baseUrl, htmlPage, "text/html", "utf-8", null)
  }

  // Spostato il controllo della visibilità hardware: gestisce in autonomia il resume grafico dell'app
  override fun onWindowVisibilityChanged(visibility: Int) {
    var newVisibility = visibility
    if (isBackgroundPlaybackEnabled && (visibility == View.GONE || visibility == View.INVISIBLE)) {
      newVisibility = View.VISIBLE
    }
    super.onWindowVisibilityChanged(newVisibility)
  }

  // Gestione dell'hardware focus nativo
  override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
    super.onWindowFocusChanged(hasWindowFocus)

    // Scatta quando l'utente rimette l'applicazione in primo piano sul display
    if (hasWindowFocus) {
      //Log.e("YOUTUBE_FORK_DEBUG", "Focus hardware Window recuperato. Forzo sblocco volume.")
      triggerVolumeRestoreOnResume()
    }
  }

  /**
   * Forza il riallineamento del volume dell'IFrame di YouTube quando l'app torna dal background.
   * Evita che il framework multimediale di Android mantenga l'audio attenuato (ducking).
   */
  private fun triggerVolumeRestoreOnResume() {
    val triggerScript = """
        (function() {
            if (typeof window.player !== 'undefined' || typeof player !== 'undefined') {
                var activePlayer = typeof window.player !== 'undefined' ? window.player : player;
                if (activePlayer && typeof activePlayer.getPlayerState === 'function') {
                    var currentState = activePlayer.getPlayerState();
                    console.log("YOUTUBE_JS_LOG: Stato del player al focus = " + currentState);
                    
                    // Il fix si attiva se il video è in PLAYING (1) o in BUFFERING (3)
                    if (currentState === 1 || currentState === 3) {
                        var resumeChecks = 0;
                        var resumeInterval = setInterval(function() {
                            if (activePlayer && typeof activePlayer.unMute === 'function') {
                                activePlayer.unMute();
                                activePlayer.setVolume(100);
                            }
                            resumeChecks++;
                            if (resumeChecks >= 5) {
                                clearInterval(resumeInterval);
                            }
                        }, 150);
                    }
                }
            }
        })();
    """.trimIndent()

    // Attendiamo 300ms per dare tempo al sistema operativo di ricollegare i canali audio hardware
    this.postDelayed({
      this.evaluateJavascript(triggerScript, null)
    }, 300)
  }
}


@VisibleForTesting
internal fun readHTMLFromUTF8File(inputStream: InputStream): String {
  inputStream.use { stream ->
    BufferedReader(InputStreamReader(stream, "utf-8")).use { bufferedReader ->
      try {
        return bufferedReader.readLines().joinToString("\n")
      } catch (_: Exception) {
        throw RuntimeException("Can't parse HTML file.")
      }
    }
  }
}
