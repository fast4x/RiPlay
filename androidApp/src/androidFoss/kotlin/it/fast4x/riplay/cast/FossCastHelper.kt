package it.fast4x.riplay.cast

import android.content.Context
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object CastHelper {
    var isCastAvailable: Boolean = false

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

    fun init(context: Context) {
        // Empty here but used in the flavors
    }

    fun initChromecastYouTubePlayerContext(context: Context) = null

}