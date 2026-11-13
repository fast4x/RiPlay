package it.fast4x.riplay.extensions.lastfm

import it.fast4x.lastfm.LastFmService
import it.fast4x.riplay.config.EnvironmentConfig
import it.fast4x.riplay.utils.DesktopAccountsStore
import it.fast4x.riplay.utils.DesktopSecureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PORTING DESKTOP di LastFmClient.kt (androidApp): client Last.fm per la
 * tab Account delle impostazioni e per lo scrobbling automatico.
 *
 * Le chiavi API arrivano dalle STESSE costanti cifrate generate per tutto
 * composeApp (EnvironmentConfig, composeApp/build.gradle.kts) e vengono
 * decifrate con DesktopSecureConfig (stessa logica di SecureUtils Android):
 * nessun valore segreto e' duplicato tra le due piattaforme.
 *
 * Il modulo :lastfm e' un modulo kotlin("jvm") puro (ktor/okhttp) gia'
 * incluso nelle dipendenze commonMain di composeApp: funziona su desktop
 * senza modifiche (LastFmService: getAuthToken/getSession/updateNowPlaying/
 * scrobble/loveTrack).
 */
object DesktopLastFmClient {
    private val API_KEY =
        DesktopSecureConfig.getApiKey(EnvironmentConfig.RiPlay_LASTFM_API_KEY)
    private val API_SECRET =
        DesktopSecureConfig.getApiKey(EnvironmentConfig.RiPlay_LASTFM_SECRET)

    val service: LastFmService by lazy {
        LastFmService(apiKey = API_KEY, apiSecret = API_SECRET)
    }

    /** URL della pagina di autorizzazione Last.fm per un token richiesto. */
    fun authUrl(token: String): String =
        "https://www.last.fm/api/auth/?api_key=$API_KEY&token=$token"
}

// ── INVIO SCROBBLE (fire-and-forget, come gli helper Android) ───────────────

/** Registra il brano come scrobble (track.scrobble, timestamp = adesso). */
fun sendLastFmScrobble(artist: String, track: String, album: String) {
    val sessionKey = DesktopAccountsStore.lastFmSessionKeyOrNull() ?: return
    CoroutineScope(Dispatchers.IO).launch {
        val timestamp = System.currentTimeMillis() / 1000
        val result = DesktopLastFmClient.service.scrobble(
            artist = artist,
            track = track,
            timestamp = timestamp,
            album = album,
            sessionKey = sessionKey
        )
        if (result.isSuccess) {
            println("[RiPlay-LastFM] scrobble accettato: ${result.getOrNull()?.attr?.accepted}")
        } else {
            println("[RiPlay-LastFM] errore scrobble: ${result.exceptionOrNull()?.message}")
        }
    }
}

/** Pubblica il brano come "in ascolto ora" (track.updateNowPlaying). */
fun sendLastFmNowPlaying(artist: String, track: String, album: String) {
    val sessionKey = DesktopAccountsStore.lastFmSessionKeyOrNull() ?: return
    CoroutineScope(Dispatchers.IO).launch {
        val result = DesktopLastFmClient.service.updateNowPlaying(
            artist = artist,
            track = track,
            album = album,
            sessionKey = sessionKey
        )
        if (result.isSuccess) {
            println("[RiPlay-LastFM] now playing aggiornato")
        } else {
            println("[RiPlay-LastFM] errore now playing: ${result.exceptionOrNull()?.message}")
        }
    }
}
