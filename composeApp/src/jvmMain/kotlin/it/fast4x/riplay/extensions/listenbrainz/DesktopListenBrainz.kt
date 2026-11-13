package it.fast4x.riplay.extensions.listenbrainz

import it.fast4x.listenbrainz.ListenBrainzService
import it.fast4x.riplay.utils.DesktopAccountsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PORTING DESKTOP di ListenBrainzClient.kt (androidApp): client
 * ListenBrainz per la tab Account delle impostazioni e per l'invio
 * automatico degli ascolti.
 *
 * Il modulo :listenbrainz e' un modulo kotlin("jvm") puro (ktor/okhttp)
 * (validateToken / submitPlayingNow / submitListen / feedback) aggiunto
 * alle dipendenze jvmMain di composeApp: funziona su desktop senza
 * modifiche.
 *
 * Il service viene ricreato automaticamente quando l'URL del server cambia
 * (es. quando l'utente imposta o rimuove un server self-hosted), come su
 * Android.
 */
object DesktopListenBrainzClient {

    @Volatile
    private var currentServerUrl: String = ListenBrainzService.DEFAULT_SERVER_URL

    @Volatile
    private var serviceInstance: ListenBrainzService = ListenBrainzService()

    /**
     * Restituisce il service puntando al server indicato.
     *
     * @param serverUrl indirizzo del server ListenBrainz; vuoto = server ufficiale
     */
    @Synchronized
    fun service(serverUrl: String = ListenBrainzService.DEFAULT_SERVER_URL): ListenBrainzService {
        val normalized = ListenBrainzService.normalizeServerUrl(serverUrl)
        if (normalized != currentServerUrl) {
            currentServerUrl = normalized
            serviceInstance = ListenBrainzService(normalized)
            println("[RiPlay-ListenBrainz] service ricostruito, server = $normalized")
        }
        return serviceInstance
    }

    fun currentServer(): String =
        ListenBrainzService.normalizeServerUrl(DesktopAccountsStore.listenBrainzServerUrl)
}

// ── INVIO LISTEN (fire-and-forget, come gli helper Android) ─────────────────

/** Pubblica il brano come "in ascolto ora" (listen_type=playing_now). */
fun sendListenBrainzNowPlaying(artist: String, track: String, album: String) {
    val token = DesktopAccountsStore.listenBrainzTokenOrNull() ?: return
    CoroutineScope(Dispatchers.IO).launch {
        val result = DesktopListenBrainzClient
            .service(DesktopListenBrainzClient.currentServer())
            .submitPlayingNow(
                artist = artist,
                track = track,
                album = album,
                token = token
            )
        if (result.isSuccess) {
            println("[RiPlay-ListenBrainz] now playing aggiornato")
        } else {
            println("[RiPlay-ListenBrainz] errore now playing: ${result.exceptionOrNull()?.message}")
        }
    }
}

/** Registra l'ascolto (listen_type=single, listenedAt = adesso). */
fun sendListenBrainzScrobble(artist: String, track: String, album: String) {
    val token = DesktopAccountsStore.listenBrainzTokenOrNull() ?: return
    CoroutineScope(Dispatchers.IO).launch {
        val timestamp = System.currentTimeMillis() / 1000
        val result = DesktopListenBrainzClient
            .service(DesktopListenBrainzClient.currentServer())
            .submitListen(
                artist = artist,
                track = track,
                listenedAt = timestamp,
                album = album,
                token = token
            )
        if (result.isSuccess) {
            println("[RiPlay-ListenBrainz] listen accettato: ${result.getOrNull()?.status}")
        } else {
            println("[RiPlay-ListenBrainz] errore listen: ${result.exceptionOrNull()?.message}")
        }
    }
}
