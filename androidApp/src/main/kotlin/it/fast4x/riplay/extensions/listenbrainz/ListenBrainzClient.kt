package it.fast4x.riplay.extensions.listenbrainz

import it.fast4x.listenbrainz.ListenBrainzService
import it.fast4x.riplay.utils.getListenBrainzServerUrl
import it.fast4x.riplay.utils.getListenBrainzToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object ListenBrainzClient {

    @Volatile
    private var currentServerUrl: String = ListenBrainzService.DEFAULT_SERVER_URL

    @Volatile
    private var serviceInstance: ListenBrainzService = ListenBrainzService()

    /**
     * Restituisce il service puntando al server indicato.
     *
     * Il service viene ricreato automaticamente quando l'URL del server cambia
     * (es. quando l'utente imposta o rimuove un server self-hosted).
     *
     * @param serverUrl indirizzo del server ListenBrainz; vuoto = server ufficiale
     */
    fun service(serverUrl: String = ListenBrainzService.DEFAULT_SERVER_URL): ListenBrainzService {
        val normalized = ListenBrainzService.normalizeServerUrl(serverUrl)

        if (normalized != currentServerUrl) {
            synchronized(this) {
                if (normalized != currentServerUrl) {
                    currentServerUrl = normalized
                    serviceInstance = ListenBrainzService(normalized)
                    Timber.d("ListenBrainzClient: service rebuilt, server = $normalized")
                }
            }
        }

        return serviceInstance
    }
}


fun sendListenBrainzNowPlaying(artist: String, track: String, album: String, token: String) {
    val serverUrl = getListenBrainzServerUrl()

    CoroutineScope(Dispatchers.IO).launch {
        val result = ListenBrainzClient.service(serverUrl).submitPlayingNow(
            artist = artist,
            track = track,
            album = album,
            token = token
        )

        if (result.isSuccess) {
            Timber.d("ListenBrainzClient: Now Playing updated")
        } else {
            Timber.d("ListenBrainzClient: Error Now Playing: ${result.exceptionOrNull()?.message}")
        }
    }
}

fun sendListenBrainzScrobble(artist: String, track: String, album: String, token: String) {
    val serverUrl = getListenBrainzServerUrl()

    CoroutineScope(Dispatchers.IO).launch {
        val timestamp = System.currentTimeMillis() / 1000

        val result = ListenBrainzClient.service(serverUrl).submitListen(
            artist = artist,
            track = track,
            listenedAt = timestamp,
            album = album,
            token = token
        )

        if (result.isSuccess) {
            val data = result.getOrNull()
            Timber.d("ListenBrainzClient Scrobbled accepted: ${data?.status}")
        } else {
            Timber.d("ListenBrainzClient Error Scrobble: ${result.exceptionOrNull()?.message}")
        }
    }
}

fun sendListenBrainzLoveTrack(artist: String, track: String) {
    val token = getListenBrainzToken()
    if (token.isEmpty()) return

    val serverUrl = getListenBrainzServerUrl()

    CoroutineScope(Dispatchers.IO).launch {
        val result = ListenBrainzClient.service(serverUrl).loveTrack(
            artist = artist,
            track = track,
            token = token
        )
        result.onSuccess {
            Timber.d("ListenBrainzClient Love track: $it")
        }.onFailure { error ->
            Timber.d("ListenBrainzClient Error love track: ${error.message}")
        }
    }
}

fun sendListenBrainzUnloveTrack(artist: String, track: String) {
    val token = getListenBrainzToken()
    if (token.isEmpty()) return

    val serverUrl = getListenBrainzServerUrl()

    CoroutineScope(Dispatchers.IO).launch {
        val result = ListenBrainzClient.service(serverUrl).unloveTrack(
            artist = artist,
            track = track,
            token = token
        )
        result.onSuccess {
            Timber.d("ListenBrainzClient Unlove track: $it")
        }.onFailure { error ->
            Timber.d("ListenBrainzClient Error Unlove track: ${error.message}")
        }
    }
}
