package it.fast4x.riplay.utils

import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.discord.DesktopDiscordPresenceManager
import it.fast4x.riplay.extensions.lastfm.sendLastFmNowPlaying
import it.fast4x.riplay.extensions.lastfm.sendLastFmScrobble
import it.fast4x.riplay.extensions.listenbrainz.sendListenBrainzNowPlaying
import it.fast4x.riplay.extensions.listenbrainz.sendListenBrainzScrobble
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * STEP 3ao: INTEGRAZIONI RIPRODUZIONE sul DESKTOP (scrobbling Last.fm /
 * ListenBrainz + Rich Presence Discord), il sostituto desktop della parte
 * di PlayerService Android che reagisce a onMediaItemTransition e ai
 * cambi di stato play/pausa.
 *
 * Sull'app desktop il player (DesktopPlayerManager: KCEF YouTube + VLCJ
 * locale) vive dentro la composizione; il "cambio brano" arriva dal
 * watcher snapshotFlow di ThreeColumnsApp (funziona anche a finestra
 * nascosta nel tray) e i cambi play/pausa sono letti con un polling di
 * 1 s sul PlayerState del controller attivo (engin-agnostico: i due
 * motori espongono StateFlow diversi).
 *
 * Logica scrobble replicata da PlayerService.onMediaItemTransition
 * (righe 3103-3143):
 *  - Last.fm:      Simple -> sendScrobble(timestamp=adesso)
 *                  NowPlaying -> sendNowPlaying
 *  - ListenBrainz: Simple -> sendListenBrainzScrobble(listenedAt=adesso)
 *                  NowPlaying -> sendListenBrainzNowPlaying
 * Le azioni scattano SOLO se il rispettivo account e' attivo e connesso
 * (DesktopAccountsStore, le chiavi delle preferenze Android).
 *
 * Discord e' pilotato da DesktopDiscordPresenceManager: nuovo brano ->
 * presence con timer vivo; pausa -> "Paused" con timer congelato; il
 * manager tiene da solo il refresh 15 s e la cancellazione dopo 5 min.
 */
object DesktopMediaIntegrations {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("RiPlay-MediaIntegrations"))

    private val presenceManager by lazy { DesktopDiscordPresenceManager() }

    @Volatile
    private var pollerStarted = false

    @Volatile
    private var lastPlayingState: Boolean? = null

    @Volatile
    private var lastSongIdForPresence: String? = null

    /**
     * CHIAMARE UNA VOLTA all'avvio (ThreeColumnsApp): avvia il polling
     * play/pausa per la presence Discord.
     */
    fun start() {
        if (pollerStarted) return
        pollerStarted = true
        scope.launch {
            while (isActive) {
                delay(1_000L)
                try {
                    val controller = DesktopPlaybackHub.activeController ?: continue
                    val state = controller.state.value
                    val playing = state.isPlaying
                    // Rileva i TRANSITORI di play/pausa (il refresh 15 s della
                    // presence gira nel manager, qui si segnalano solo i cambi)
                    if (playing != lastPlayingState) {
                        lastPlayingState = playing
                        if (DesktopAccountsStore.discordEnabled && DesktopAccountsStore.discordToken.isNotBlank()) {
                            presenceManager.onPlayingStateChanged(
                                isPlaying = playing,
                                positionMs = state.timestamp,
                                durationMs = state.duration
                            )
                        }
                    }
                } catch (t: Throwable) {
                    // mai far morire il poller per un errore di integrazione
                }
            }
        }
        println("[RiPlay-MediaIntegrations] poller avviato")
    }

    /**
     * CHIAMARE A OGNI CAMBIO BRANO (watcher ThreeColumnsApp): scrobble +
     * presence per il brano che INIZIA. `song` puo' essere null nei primi
     * istanti (il db si aggiorna dopo il click): nel caso la presence parte
     * con i soli id noti.
     */
    fun onTrackChanged(song: Song?, positionMs: Long = 0L, durationMs: Long = 0L) {
        // ── LAST.FM ─────────────────────────────────────────────────────────
        val lastFmKey = DesktopAccountsStore.lastFmSessionKeyOrNull()
        if (lastFmKey != null && song != null) {
            val artist = song.artistsText?.takeIf { it.isNotBlank() } ?: ""
            val title = cleanPrefix(song.title).takeIf { it.isNotBlank() } ?: song.title
            // Android usa mediaMetadata.albumTitle: nel modello Song desktop non
            // c'e' il titolo dell'album (solo albumId) -> campo vuoto (facoltativo)
            when (DesktopAccountsStore.lastFmScrobbleType) {
                DesktopScrobbleType.Simple -> sendLastFmScrobble(artist, title, "")
                DesktopScrobbleType.NowPlaying -> sendLastFmNowPlaying(artist, title, "")
            }
            println("[RiPlay-MediaIntegrations] last.fm (${DesktopAccountsStore.lastFmScrobbleType}) '$title' - '$artist'")
        }

        // ── LISTENBRAINZ ────────────────────────────────────────────────────
        val lbToken = DesktopAccountsStore.listenBrainzTokenOrNull()
        if (lbToken != null && song != null) {
            val artist = song.artistsText?.takeIf { it.isNotBlank() } ?: ""
            val title = cleanPrefix(song.title).takeIf { it.isNotBlank() } ?: song.title
            when (DesktopAccountsStore.listenBrainzScrobbleType) {
                DesktopScrobbleType.Simple -> sendListenBrainzScrobble(artist, title, "")
                DesktopScrobbleType.NowPlaying -> sendListenBrainzNowPlaying(artist, title, "")
            }
            println("[RiPlay-MediaIntegrations] listenbrainz (${DesktopAccountsStore.listenBrainzScrobbleType}) '$title' - '$artist'")
        }

        // ── DISCORD PRESENCE ────────────────────────────────────────────────
        val discordToken = DesktopAccountsStore.discordTokenOrNull()
        if (discordToken != null) {
            val songId = song?.id
            lastSongIdForPresence = songId
            presenceManager.onTrackChanged(
                song = song,
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = DesktopPlaybackHub.activeController?.state?.value?.isPlaying ?: false
            )
        }
    }
}
