package it.fast4x.riplay.extensions.lastfm

import androidx.lifecycle.viewModelScope
import it.fast4x.lastfm.LastFmService
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.getlastFmSessionKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object LastFmClient {
    private val API_KEY = appContext().resources.getString(R.string.RiPlay_LASTFM_API_KEY)
    private val API_SECRET = appContext().resources.getString(R.string.RiPlay_LASTFM_SECRET)

    val service: LastFmService = LastFmService(apiKey = API_KEY, apiSecret = API_SECRET)
}


fun sendNowPlaying(artist: String, track: String, album: String, sessionKey: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val result = LastFmClient.service.updateNowPlaying(
            artist = artist,
            track = track,
            album = album,
            sessionKey = sessionKey
        )

        if (result.isSuccess) {
            Timber.d("LastFmClient: Now Playing updated")
        } else {
            Timber.d("LastFmClient: Error Now Playing: ${result.exceptionOrNull()?.message}")
        }
    }
}

fun sendScrobble(artist: String, track: String, album: String, sessionKey: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val timestamp = System.currentTimeMillis() / 1000

        val result = LastFmClient.service.scrobble(
            artist = artist,
            track = track,
            timestamp = timestamp,
            album = album,
            sessionKey = sessionKey
        )

        if (result.isSuccess) {
            val data = result.getOrNull()
            Timber.d("LastFmClient Scrobbled accepted: ${data?.attr?.accepted}")
        } else {
            Timber.d("LastFmClient Error Scrobble: ${result.exceptionOrNull()?.message}")
        }
    }
}

fun sendLoveTrack(artist: String, track: String) {
    val sessionKey = getlastFmSessionKey() ?: return

    CoroutineScope(Dispatchers.IO).launch {
        val result = LastFmClient.service.loveTrack(
            artist = artist,
            track = track,
            sessionKey = sessionKey
        )
        result.onSuccess {
            Timber.d("LastFmClient Love track: $it")
        }.onFailure { error ->
            Timber.d("LastFmClient Error love track: ${error.message}")
        }
    }
}

fun sendUnloveTrack(artist: String, track: String) {
    val sessionKey = getlastFmSessionKey() ?: return

    CoroutineScope(Dispatchers.IO).launch {
        val result = LastFmClient.service.unloveTrack(
            artist = artist,
            track = track,
            sessionKey = sessionKey
        )
        result.onSuccess {
            Timber.d("LastFmClient Unlove track: $it")
        }.onFailure { error ->
            Timber.d("LastFmClient Error Unlove track: ${error.message}")
        }
    }
}