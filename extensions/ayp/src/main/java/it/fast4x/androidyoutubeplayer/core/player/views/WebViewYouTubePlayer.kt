package it.fast4x.androidyoutubeplayer.core.player.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.createBitmap
import it.fast4x.androidyoutubeplayer.R
import it.fast4x.androidyoutubeplayer.core.player.BooleanProvider
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayerBridge
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayerCallbacks
import it.fast4x.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import it.fast4x.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import it.fast4x.androidyoutubeplayer.core.player.toFloat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


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

  internal var customVideoPoster: Bitmap? = null
  private var base64Logo: String? = null

  internal fun initialize(initListener: (YouTubePlayer) -> Unit, playerOptions: IFramePlayerOptions?, videoId: String?) {
    youTubePlayerInitListener = initListener
    initWebView(playerOptions ?: IFramePlayerOptions.getDefault(context), videoId)
  }

  override val listeners: Collection<YouTubePlayerListener> get() = _youTubePlayer.getListeners()
  override fun getInstance(): YouTubePlayer = _youTubePlayer
  override fun onYouTubeIFrameAPIReady() {
    // Dice alla webview di iniettare la stringa Base64 del logo nel div CSS
    injectLogoIntoWebDOM()

    youTubePlayerInitListener(_youTubePlayer)
  }
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

    // Configurazione snella del client grafico
    webChromeClient = object : WebChromeClient() {
      override fun getDefaultVideoPoster(): Bitmap? {
        // Restituisce SEMPRE un pixel trasparente. La cover vera la gestisce il DOM HTML
        return try {
            createBitmap(1, 1).apply {
            eraseColor(android.graphics.Color.TRANSPARENT)
          }
        } catch (e: Exception) { super.getDefaultVideoPoster() }
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
      // Mantiene l'inganno visivo stabile. Chromium ridurrà i consumi dei frame
      // in autonomia poiché la finestra non è focalizzata, ma senza andare in pausa.
      newVisibility = View.VISIBLE
    }

    super.onWindowVisibilityChanged(newVisibility)
  }


  /**
   * Riceve una Bitmap pre-renderizzata dall'applicazione e la imposta
   * come poster grafico durante il caricamento iniziale della WebView.
   */
  fun setCustomVideoPoster(bitmap: Bitmap?) {
    Log.d("WebViewYouTubePlayer", "setCustomVideoPoster called with bitmap: $bitmap")
    //this.customVideoPoster = bitmap
    setRawLogoBitmap(bitmap)
  }

  /**
   * Riceve il logo originale grezzo dall'app, lo converte in stringa Base64
   * e lo inietta nell'HTML. Non serve ricalcolarlo alla rotazione!
   */
  fun setRawLogoBitmap(bitmap: Bitmap?) {
    if (bitmap == null) return
    try {
      val outputStream = java.io.ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
      val byteArray = outputStream.toByteArray()
      base64Logo = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e("WebViewYouTubePlayer", "Errore conversione Base64", e)
    }
  }

  // Invocato quando l'HTML notifica che le API sono pronte (sendYouTubeIFrameAPIReady o sendReady)
  fun injectLogoIntoWebDOM() {
    base64Logo?.let { base64 ->
      val jsCommand = "document.getElementById('customPoster').style.backgroundImage = 'url(data:image/png;base64,$base64)';"
      this.post {
        this.evaluateJavascript(jsCommand, null)
      }
    }
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
