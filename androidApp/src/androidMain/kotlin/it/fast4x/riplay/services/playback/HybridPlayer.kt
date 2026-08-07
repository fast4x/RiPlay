package it.fast4x.riplay.services.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.commonutils.durationTextToMillis
import timber.log.Timber

// Interfaccia che il tuo wrapper YouTube DEVE implementare per parlarti
interface YouTubeControl {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun getCurrentPositionMs(): Long
    fun getDurationMs(): Long
    fun isPlaying(): Boolean
    fun getVolume(): Float
    fun setVolume(volume: Float)
    fun setPlaybackRate(rate: Float) // Mappa dal tuo enum ai float di YT
}

enum class ActiveEngine { EXOPLAYER, YOUTUBE }

@UnstableApi
class HybridPlayer (
    private val playerService: PlayerService,
    private val exoPlayer: Player,
    private val youtubeControl: YouTubeControl
) : ForwardingPlayer(exoPlayer) {

    var onRefreshCustomLayoutListener: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    var activeEngine: ActiveEngine = ActiveEngine.EXOPLAYER
        private set

    private val hybridListeners = mutableListOf<Player.Listener>()

    private val positionUpdater = object : Runnable {
        override fun run() {
            if (activeEngine == ActiveEngine.YOUTUBE) {
                val currentPos = getCurrentPosition()
                val timeline = currentTimeline

                // Inizializziamo i parametri con i fallback di base
                var windowUid: Any? = null
                var periodUid: Any? = null
                val mediaItem = currentMediaItem
                val itemIndex = currentMediaItemIndex

                // Se la timeline è pronta, estraiamo gli UID reali richiesti da Media3
                if (!timeline.isEmpty && itemIndex < timeline.windowCount) {
                    try {
                        val window = androidx.media3.common.Timeline.Window()
                        timeline.getWindow(itemIndex, window)
                        windowUid = window.uid

                        val period = androidx.media3.common.Timeline.Period()
                        timeline.getPeriod(0, period)
                        periodUid = period.uid
                    } catch (e: Exception) {
                        // Fallback silenzioso in caso di indici non ancora sincronizzati
                    }
                }

                // Creiamo l'oggetto PositionInfo con TUTTI i dati strutturali validi
                val positionInfo = Player.PositionInfo(
                    windowUid,          // 1. UID della finestra reale
                    itemIndex,          // 2. Indice del media item
                    mediaItem,          // 3. Oggetto MediaItem corrente
                    periodUid,          // 4. UID del periodo reale
                    0,                  // 5. Indice del periodo
                    currentPos,         // 6. Posizione corrente del player alternativo
                    currentPos,         // 7. contentPositionMs (uguale alla posizione corrente se non ci sono ad)
                    C.INDEX_UNSET,      // 8. adGroupIndex (INDEX_UNSET indica che NON è una pubblicità)
                    C.INDEX_UNSET       // 9. adIndexInAdGroup (INDEX_UNSET indica che NON è una pubblicità)
                )

                // Notifichiamo i listener senza mandare in crash la MediaSession
                hybridListeners.forEach { listener ->
                    listener.onPositionDiscontinuity(
                        positionInfo,
                        positionInfo,
                        Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                    )
                }

                // Intervallo di polling stabile per Android Auto e notifica (500ms)
                mainHandler.postDelayed(this, 500)
            }
        }
    }

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        hybridListeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        hybridListeners.remove(listener)
    }

    fun switchToExo() {
        activeEngine = ActiveEngine.EXOPLAYER
        mainHandler.removeCallbacks(positionUpdater)
        hybridListeners.forEach { listener ->
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
    }

    fun switchToYoutube() {
        activeEngine = ActiveEngine.YOUTUBE

        // Fermiamo temporaneamente il loop per evitare lag durante il caricamento
        mainHandler.removeCallbacks(positionUpdater)
        invalidateYouTubeTrackChanged()

        // Facciamo ripartire il monitoraggio della posizione solo dopo mezzo secondo
        mainHandler.postDelayed({
            if (activeEngine == ActiveEngine.YOUTUBE) {
                mainHandler.post(positionUpdater)
            }
        }, 500)
    }

    // Chiamare SOLO nel momento esatto in cui comincia una nuova canzone su YouTube
    fun invalidateYouTubeTrackChanged() {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_TIMELINE_CHANGED)
                .add(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)
                .add(Player.EVENT_PLAYBACK_STATE_CHANGED)
                .build()
        )
        forwardEventsToSession(events)
    }

    // Chiamare SOLO dentro Play(), Pause() o quando lo stato play/pausa cambia realmente
    fun invalidateYouTubePlayPause() {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_IS_PLAYING_CHANGED)
                .add(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                .add(Player.EVENT_PLAYBACK_STATE_CHANGED)
                .build()
        )
        forwardEventsToSession(events)
    }

    // Chiamare dentro il tuo Runnable/Loop continuo ogni 500ms
    fun invalidateYouTubePositionOnly() {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_POSITION_DISCONTINUITY)
                .build()
        )
        forwardEventsToSession(events)
    }

    // Metodo privato per sparare gli eventi a MediaSession, Android Auto e Notifiche
    fun forwardEventsToSession(events: Player.Events) {
        for (listener in hybridListeners.toList()) {
            listener.onEvents(this, events)
        }
    }

    // --- OVERRIDE CRITICI PER INGANNARE MEDIA3 E GOOGLE ASSISTANT ---

    override fun isPlaying(): Boolean {
        Timber.d("HybridPlayer isPlaying() called: activeEngine = $activeEngine isPlaying = ${super.isPlaying} youtubeControl.isPlaying() = ${youtubeControl.isPlaying()}")
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            playbackState == Player.STATE_READY && youtubeControl.isPlaying()
        }
        else super.isPlaying
    }

    override fun getPlaybackState(): Int {
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            Player.STATE_READY // Forza lo stato attivo anche se ExoPlayer ha già terminato il file vuoto
        } else {
            super.playbackState
        }
    }

    override fun getCurrentPosition(): Long {
        val currentPosition = if (activeEngine == ActiveEngine.YOUTUBE) (playerService._currentSecond.value * 1000L).toLong()
        else super.getCurrentPosition()
        Timber.d("HybridPlayer activeEngine = $activeEngine getCurrentPosition = $currentPosition")
        return currentPosition
    }

    override fun getDuration(): Long {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val alternativeDuration = (playerService._currentDuration.value * 1000L).toLong()
            return if (alternativeDuration > 0) {
                alternativeDuration
            } else {
                // FALLBACK: Se l'hybridPlayer è ancora in avvio, leggiamo la durata
                // salvata negli extras del MediaItem che ExoPlayer ha già caricato!
                val currentItem = currentMediaItem // Riferimento al MediaItem corrente del ForwardingPlayer
                val durationText = currentItem?.mediaMetadata?.extras?.getString("durationText") ?: "04:00"
                durationTextToMillis(durationText)
            }
        }

        return super.getDuration()
    }

    override fun getContentBufferedPosition(): Long {
        return if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.getCurrentPositionMs()
        else super.contentBufferedPosition
    }

    // Quando suona YouTube, ExoPlayer sta restituendo un errore perché l'URI è vuoto.
    // Auto vede l'errore e si blocca. Dobbiamo restituire NULL per dire "Tutto ok!"
    override fun getPlayerError(): PlaybackException? {
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            null // "Nessun errore, stiamo gestendo tutto noi con la WebView"
        } else {
            super.playerError
        }
    }



    override fun getPlayWhenReady(): Boolean {
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.isPlaying()
        } else {
            exoPlayer.playWhenReady
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (activeEngine == ActiveEngine.YOUTUBE) {

            // 2. Controlliamo l'audio dell'hybridPlayer in base al comando di Android Auto
            if (playWhenReady) {
                youtubeControl.play()
                mainHandler.removeCallbacks(positionUpdater)
                mainHandler.post(positionUpdater)
            } else {
                youtubeControl.pause()
                mainHandler.removeCallbacks(positionUpdater) // Blocca la barra
            }

            // Inoltriamo il comando a ExoPlayer in background per tenere allineata la sessione
            super.setPlayWhenReady(playWhenReady)

            // Spariamo la notifica in batch che ha reso reattiva l'applicazione
            invalidateYouTubePlayPause()

            // Ritardiamo di 50ms per dare tempo a Media3 di metabolizzare la pausa
            mainHandler.postDelayed({
                // Aggiorniamo il custom layout
                onRefreshCustomLayoutListener?.invoke()
            }, 50)
        } else {
            super.setPlayWhenReady(playWhenReady)
        }
    }


    override fun getAvailableCommands(): Player.Commands {
        val commands = super.getAvailableCommands()
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            // Garantisce che Android Auto veda SEMPRE i tasti Play/Pausa e Seek come attivi e cliccabili
            commands.buildUpon()
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .build()
        } else {
            commands
        }
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            if (command == Player.COMMAND_PLAY_PAUSE || command == Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
                true
            } else {
                super.isCommandAvailable(command)
            }
        } else {
            super.isCommandAvailable(command)
        }
    }


    // --- OVERRIDE DEI COMANDI ---

    override fun play() {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.play()
            // Facciamo partire ExoPlayer in background a volume zero per garantire il player su aa
            super.play()
            mainHandler.removeCallbacks(positionUpdater)
            mainHandler.post(positionUpdater)
            // Notifichiamo la MediaSession del cambio di stato (da pausa a play)
            invalidateYouTubePlayPause()
        }
        else super.play()
    }

    override fun pause() {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.pause()
            super.pause()
            mainHandler.removeCallbacks(positionUpdater)
            // Notifichiamo la MediaSession del cambio di stato (da play a pausa)
            invalidateYouTubePlayPause()

            // Ritardiamo di 50ms per dare tempo a Media3 di metabolizzare la pausa
            mainHandler.postDelayed({
                // Aggiorniamo il custom layout
                onRefreshCustomLayoutListener?.invoke()
            }, 50)
        }
        else super.pause()

    }

    override fun seekTo(positionMs: Long) {
        if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.seekTo(positionMs) else exoPlayer.seekTo(positionMs)
        if (activeEngine == ActiveEngine.YOUTUBE) invalidateYouTubePositionOnly()
    }

    override fun getVolume(): Float {
        return if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.getVolume() else exoPlayer.volume
    }

    override fun setVolume(volume: Float) {
        if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.setVolume(volume) else exoPlayer.volume =
            volume
    }

}