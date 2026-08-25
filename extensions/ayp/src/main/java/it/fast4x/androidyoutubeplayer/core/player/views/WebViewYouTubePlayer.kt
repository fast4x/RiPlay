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

/*
internal object FakeWebViewYouTubeListener : FullscreenListener {
  override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {}
  override fun onExitFullscreen() {}
}
 */

/**
 * WebView implementation of [YouTubePlayer]. The player runs inside the WebView, using the IFrame Player API.
 */
internal class WebViewYouTubePlayer constructor(
  context: Context,
  //private val listener: FullscreenListener,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), YouTubePlayerBridge.YouTubePlayerBridgeCallbacks {

  /** Constructor used by tools */
  //constructor(context: Context) : this(context, FakeWebViewYouTubeListener)

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

    // Forza il rendering Hardware esplicito per questa WebView
    setLayerType(LAYER_TYPE_HARDWARE, null)

    // Impedisce alla WebView di generare eventi sonori di click o focus nativi
    isSoundEffectsEnabled = false
    isHapticFeedbackEnabled = false

    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      mediaPlaybackRequiresUserGesture = false

      //cacheMode = WebSettings.LOAD_NO_CACHE

      // Ottimizzazione aggressiva della Cache
      cacheMode = WebSettings.LOAD_DEFAULT // Usa la cache se valida, altrimenti scarica

    }

    addJavascriptInterface(youTubePlayerBridge, "YouTubePlayerBridge")
    addJavascriptInterface(youTubePlayerCallbacks, "YouTubePlayerCallbacks")

    val htmlPage = readHTMLFromUTF8File(resources.openRawResource(R.raw.ayp_youtube_player))
      .replace("<<injectedVideoId>>", if (videoId != null) { "'$videoId'" } else { "undefined" })
      .replace("<<injectedPlayerVars>>", playerOptions.toString())

    loadDataWithBaseURL(playerOptions.getOrigin(), htmlPage, "text/html", "utf-8", null)

    webChromeClient = object : WebChromeClient() {

      private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
      private var focusRequest: AudioFocusRequest? = null

      // Rileva quando la WebView richiede il playback multimediale (HTML5 Video)
      override fun getDefaultVideoPoster(): Bitmap? {
        //requestAudioFocusEarly()
        return super.getDefaultVideoPoster()
      }

      private fun requestAudioFocusEarly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

          focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false) // Impedisce l'attenuazione automatica
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

          focusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
          @Suppress("DEPRECATION")
          audioManager.requestAudioFocus(
            { },
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
          )
        }
      }

    }

  }

  // Cerca il listener dell'audio focus dentro initWebView
  val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS, -1 -> {
        // Invece di subire la perdita di focus che silenzia la WebView,
        // forziamo un micro-timer per rimettere il volume dell'IFrame a 100
        Log.e("YOUTUBE_FIX", "Intercettato -1 dal sistema. Forzo lo sblocco del volume.")

        // Richiama il codice JS che hai già ottimizzato
        val triggerScript = """
                (function() {
                    if (typeof player !== 'undefined' && player && typeof player.setVolume === 'function') {
                        player.unMute();
                        player.setVolume(100);
                    }
                })();
            """.trimIndent()

        // Esegui sulla webview
        this.postDelayed({
          this.evaluateJavascript(triggerScript, null)
        }, 100)
      }
    }
  }

  override fun onWindowVisibilityChanged(visibility: Int) {
    var newVisibility = visibility
    if (isBackgroundPlaybackEnabled && (visibility == View.GONE || visibility == View.INVISIBLE)) {
      newVisibility = View.VISIBLE
    }
    super.onWindowVisibilityChanged(newVisibility)
  }

  override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
    super.onWindowFocusChanged(hasWindowFocus)
    Log.e("YOUTUBE_FORK_DEBUG", "Focus hardware ricevuto con successo!")
    // Scatta SEMPRE quando l'app torna visibile e interattiva per l'utente
    if (hasWindowFocus) {
      Log.e("YOUTUBE_FORK_DEBUG", "Focus hardware ricevuto con successo!")
      triggerVolumeRestoreOnResume()
    }
  }

  private fun triggerVolumeRestoreOnResume() {
    val triggerScript = """
        (function() {
            if (typeof window.player !== 'undefined' || typeof player !== 'undefined') {
                var activePlayer = typeof window.player !== 'undefined' ? window.player : player;
                if (activePlayer && typeof activePlayer.getPlayerState === 'function') {
                    var currentState = activePlayer.getPlayerState();
                    console.log("YOUTUBE_JS_LOG: Stato del player al focus = " + currentState);
                    
                    // Applica il fix se il video sta andando (1) o sta caricando i buffer (3)
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

    // Attendiamo 300ms per dare tempo al mixer audio di Android di ricollegarsi
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
