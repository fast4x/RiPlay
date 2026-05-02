package it.fast4x.riplay.cast

import android.content.Context
import android.widget.TextView
import com.google.android.gms.cast.framework.CastContext
import it.fast4x.androidyoutubeplayer.cast.ChromecastYouTubePlayerContext
import it.fast4x.androidyoutubeplayer.cast.io.infrastructure.ChromecastConnectionListener
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CastHelper {
    val isCastAvailable: Boolean = true
    var mediaId = ""

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _internalCastOnlinePlayer = MutableStateFlow<YouTubePlayer?>(null)
    val internalCastOnlinePlayer: StateFlow<YouTubePlayer?> = _internalCastOnlinePlayer

    private val _internalBufferedFraction = MutableStateFlow(0f)
    val internalBufferedFraction: StateFlow<Float> = _internalBufferedFraction

    private var _currentSecond = MutableStateFlow(0f)
    var currentSecond: StateFlow<Float> = _currentSecond

    private var _currentDuration = MutableStateFlow(0f)
    var currentDuration: StateFlow<Float> = _currentDuration

    private val _playerState = MutableStateFlow<PlayerConstants.PlayerState>(PlayerConstants.PlayerState.UNSTARTED)
    val playerState: StateFlow<PlayerConstants.PlayerState> = _playerState

    fun init(context: Context): CastContext =
        CastContext.getSharedInstance(context)

    fun initChromecastYouTubePlayerContext(context: Context) {

            CoroutineScope(Dispatchers.Main).launch {
                ChromecastYouTubePlayerContext(
                    CastContext.getSharedInstance(context).sessionManager,
                    object : ChromecastConnectionListener {
                        override fun onChromecastConnecting() {
                            Timber.d("CastHelper: onChromecastConnecting")
                        }

                        override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
                            Timber.d("CastHelper: onChromecastConnected")
                            _connected.value = true
                            CoroutineScope(Dispatchers.Main.immediate).launch {
                                try {
                                    val player =
                                        initializeChromecastPlayer(chromecastYouTubePlayerContext)
                                    _internalCastOnlinePlayer.value = player
                                } catch (e: Exception) {
                                    Timber.e("CastHelper: Errore nel preparare il player: $e")
                                }
                            }

                        }

                        override fun onChromecastDisconnected() {
                            Timber.d("CastHelper  onChromecastDisconnected")
                            _connected.value = false
                        }
                    }
                )
            }

    }

    private suspend fun initializeChromecastPlayer(
        chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext
    ): YouTubePlayer {
        Timber.d("CastHelper: initializeChromecastPlayer")

        return suspendCancellableCoroutine { continuation ->
            // Initialize Chromecast playerchromecastYouTubePlayerContext.initialize(listener)
            chromecastYouTubePlayerContext.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    _internalCastOnlinePlayer.value = youTubePlayer
                    Timber.d("CastHelper: onReady")
                    if (continuation.isActive) {
                        continuation.resume(youTubePlayer) { cause, _, _ -> {} }
                    }
                }

                override fun onPlaybackRateChange(
                    youTubePlayer: YouTubePlayer,
                    playbackRate: PlayerConstants.PlaybackRate
                ) {
                    //Timber.d("CastHelper: onPlaybackRateChange $playbackRate")
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    _currentSecond.value = second
                    //Timber.d("CastHelper: onCurrentSecond $second")
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    _currentDuration.value = duration
                    //Timber.d("CastHelper: onVideoDuration $duration")
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    Timber.d("CastHelper: onError $error")

                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Errore inizializzazione Cast Player: $error"))
                    }
                }

                override fun onVideoLoadedFraction(
                    youTubePlayer: YouTubePlayer,
                    loadedFraction: Float
                ) {
                    _internalBufferedFraction.value = loadedFraction
                }

                override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
                    Timber.d("CastHelper: onVideoId $videoId")
                    mediaId = videoId
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    Timber.d("CastHelper: onStateChange $state")
                    _playerState.value = state
                    when(state) {
                        PlayerConstants.PlayerState.VIDEO_CUED -> {
                            youTubePlayer.play()
                        }
                        else -> {}
                    }

                }

            })

            continuation.invokeOnCancellation {
                Timber.d("CastHelper: Inizializzazione annullata dall'utente")
            }
        }
    }

    fun load(mediaId: String, position: Float = 0f) {
        _internalCastOnlinePlayer.value?.loadVideo(mediaId, position)
    }

    fun cue(mediaId: String, position: Float = 0f) {
        _internalCastOnlinePlayer.value?.cueVideo(mediaId, position)
    }

    fun play() = _internalCastOnlinePlayer.value?.play()
    fun pause() = _internalCastOnlinePlayer.value?.pause()

}