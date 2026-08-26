package it.fast4x.riplay.services.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import kotlin.math.pow

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

    // Variabili per la normalizzazione YouTube
    private var userVolume: Float = 1.0f
    private var ytLoudnessDb: Float = 0f

    var onRefreshCustomLayoutListener: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var youtubePlayWhenReady = false

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
                        Player.DISCONTINUITY_REASON_SKIP
                    )
                }

                // Intervallo di polling stabile per Android Auto e notifica (200ms)
                mainHandler.postDelayed(this, 200)
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

        // Impostiamo il volume scelto dall'utente per ExoPlayer
        exoPlayer.volume = userVolume

        hybridListeners.forEach { listener ->
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
    }

    fun switchToYoutube() {
        activeEngine = ActiveEngine.YOUTUBE

        youtubePlayWhenReady = youtubeControl.isPlaying()

        mainHandler.removeCallbacks(positionUpdater)
        invalidateYouTubeTrackChanged()

//        // Facciamo ripartire il monitoraggio della posizione solo dopo mezzo secondo
        mainHandler.postDelayed({
            if (activeEngine == ActiveEngine.YOUTUBE) {
                mainHandler.post(positionUpdater)
            }
        }, 200)
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
        //Timber.d("HybridPlayer isPlaying() called: activeEngine = $activeEngine isPlaying = ${super.isPlaying} youtubeControl.isPlaying() = ${youtubeControl.isPlaying()}")
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            playbackState == Player.STATE_READY && youtubePlayWhenReady // youtubeControl.isPlaying()
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
        //Timber.d("HybridPlayer activeEngine = $activeEngine getCurrentPosition = $currentPosition")
        return currentPosition
    }

    override fun getDuration(): Long {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val alternativeDuration = (playerService._currentDuration.value * 1000L).toLong()
            return if (alternativeDuration > 0) {
                alternativeDuration
            } else {
                C.TIME_UNSET

                // FALLBACK: Se l'hybridPlayer è ancora in avvio, leggiamo la durata
                // salvata negli extras del MediaItem che ExoPlayer ha già caricato!
//                val currentItem = currentMediaItem // Riferimento al MediaItem corrente del ForwardingPlayer
//                val durationText = currentItem?.mediaMetadata?.extras?.getString("durationText") ?: "04:00"
//                durationTextToMillis(durationText)
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
            //youtubeControl.isPlaying()
            youtubePlayWhenReady
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
        return commands
//        return if (activeEngine == ActiveEngine.YOUTUBE) {
//            // Garantisce che Android Auto veda SEMPRE i tasti Play/Pausa e Seek come attivi e cliccabili
//            commands.buildUpon()
//                .add(Player.COMMAND_SET_MEDIA_ITEM)
//                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
//                .add(Player.COMMAND_PLAY_PAUSE)
//                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
//                .add(Player.COMMAND_SEEK_BACK)
//                .add(Player.COMMAND_SEEK_FORWARD)
//                .build()
//        } else {
//            commands
//        }
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
            // Aggiorniamo lo stato locale immediatamente
            youtubePlayWhenReady = true

            // Riavvia il polling più velocemente (200ms invece di 500ms)
            mainHandler.removeCallbacks(positionUpdater)
            mainHandler.postDelayed(positionUpdater, 200)

            // Notifichiamo subito la sessione
            invalidateYouTubePlayPause()

            // Aggiorniamo il custom layout (ritardato come avevi tu)
            mainHandler.postDelayed({
                onRefreshCustomLayoutListener?.invoke()
            }, 50)


            // Facciamo partire ExoPlayer in background a volume zero per garantire il player su aa
//            super.play()
//            mainHandler.removeCallbacks(positionUpdater)
//            mainHandler.post(positionUpdater)
//            // Notifichiamo la MediaSession del cambio di stato (da pausa a play)
//            invalidateYouTubePlayPause()
        }
        else super.play()
    }

    override fun pause() {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.pause()

            // Aggiorniamo lo stato locale immediatamente
            youtubePlayWhenReady = false

            // Fermiamo il polling della posizione
            mainHandler.removeCallbacks(positionUpdater)

            // Notifichiamo subito la sessione
            invalidateYouTubePlayPause()

            // Aggiorniamo il custom layout (ritardato come avevi tu)
            mainHandler.postDelayed({
                onRefreshCustomLayoutListener?.invoke()
            }, 50)

//            super.pause()
//            mainHandler.removeCallbacks(positionUpdater)
//            // Notifichiamo la MediaSession del cambio di stato (da play a pausa)
//            invalidateYouTubePlayPause()
//
//            // Ritardiamo di 50ms per dare tempo a Media3 di metabolizzare la pausa
//            mainHandler.postDelayed({
//                // Aggiorniamo il custom layout
//                onRefreshCustomLayoutListener?.invoke()
//            }, 50)
        }
        else super.pause()

    }

    override fun seekTo(positionMs: Long) {
        if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.seekTo(positionMs) else exoPlayer.seekTo(positionMs)
        if (activeEngine == ActiveEngine.YOUTUBE) invalidateYouTubePositionOnly()
    }

    override fun getVolume(): Float {
        return if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.getVolume() else userVolume
    }

    override fun setVolume(volume: Float) {
        // Salviamo sempre il volume scelto dall'utente
        userVolume = volume
        // Applichiamo la logica
        if (activeEngine == ActiveEngine.YOUTUBE) {
            applyVolumeNormalization()
        } else {
            exoPlayer.volume = userVolume
        }
    }

    fun updateCurrentMediaItemDuration(durationMs: Long) {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val currentItem = currentMediaItem ?: return
            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(
                    currentItem.mediaMetadata.buildUpon()
                        .setDurationMs(durationMs) // Media3 nativo per la durata!
                        .build()
                )
                .build()

            // Aggiorna la playlist di ExoPlayer silenziosamente
            val currentIndex = currentMediaItemIndex
            exoPlayer.replaceMediaItem(currentIndex, updatedItem)

            // Forza il refresh della timeline su MediaSession
            hybridListeners.forEach {
                it.onTimelineChanged(exoPlayer.currentTimeline, Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE)
            }
        }
    }

    /**
     * Chiamato dal PlayerService quando recupera il loudnessDb dal DB.
     */
    fun setYtLoudnessDb(loudnessDb: Float) {
        ytLoudnessDb = loudnessDb
        if (activeEngine == ActiveEngine.YOUTUBE) {
            applyVolumeNormalization()
        }
    }

    /**
     * Applica l'attenuazione se siamo su YouTube e la normalizzazione audio è attiva
     */
    fun applyVolumeNormalization() {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val volumeNormalizationEnabled = playerService.appSettings.volumeNormalizationEnabled

            // 1. Calcoliamo il fattore solo se NON dobbiamo escludere il loudness E se il loudness non è zero
            val finalVolume = if (volumeNormalizationEnabled && ytLoudnessDb != 0f) {
                val normalizationFactor = 10.0.pow((-ytLoudnessDb / 20.0)).toFloat()

                // Applichiamo la normalizzazione ma imponiamo un limite inferiore (es. 0.01f)
                // per evitare che brani con loudness anomalo azzerino l'audio se l'utente ha il volume alto.
                if (userVolume > 0f) {
                    maxOf(0.01f, minOf(1.0f, userVolume * normalizationFactor))
                } else {
                    0f // Se l'utente ha messo muto, resta a 0
                }
            } else {
                // Se la normalizzazione è disabilitata e ytLoudnessDb è zero, usiamo il puro volume utente
                userVolume
            }

            youtubeControl.setVolume(finalVolume)
            //Timber.d("HybridPlayer applyVolumeNormalization YT: finalVol=$finalVolume")
        } else {
            exoPlayer.volume = userVolume
        }
    }


}