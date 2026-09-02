package it.fast4x.riplay.services.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.pow

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
    // Variabile per gestire il fade separatamente
    private var fadeMultiplier: Float = 1.0f

    var onRefreshCustomLayoutListener: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var youtubePlayWhenReady = false

    var activeEngine: ActiveEngine = ActiveEngine.EXOPLAYER
        private set

    private val hybridListeners = mutableListOf<Player.Listener>()

    /* // Non è necessario lo mantengo come eventuale workaround
    private val positionUpdater = object : Runnable {
        override fun run() {
            if (activeEngine == ActiveEngine.YOUTUBE && isPlaying) { // Gira solo se YouTube sta SUONANDO davvero
                val currentPos = getCurrentPosition()

                // Costruiamo un PositionInfo minimale e leggerissimo senza calcolare la timeline ad ogni ciclo.
                // A Media3, per aggiornare la barra di scorrimento visiva, servono solo l'indice e i millisecondi!
                val positionInfo = Player.PositionInfo(
                    null,                  // windowUid (null va benissimo per il refresh visivo)
                    currentMediaItemIndex, // Indice corrente nella playlist
                    currentMediaItem,      // Oggetto MediaItem corrente
                    null,                  // periodUid
                    0,                     // periodIndex
                    currentPos,            // Posizione corrente estratta dai tuoi secondi di YouTube
                    currentPos,            // contentPositionMs
                    C.INDEX_UNSET,
                    C.INDEX_UNSET
                )

                // Creiamo l'evento nativo per la barra di scorrimento
                val events = Player.Events(
                    FlagSet.Builder()
                        .add(Player.EVENT_POSITION_DISCONTINUITY)
                        .build()
                )

                // Spariamo l'evento a Media3 per muovere la linea del tempo visiva
                hybridListeners.forEach { listener ->
                    listener.onPositionDiscontinuity(
                        positionInfo,
                        positionInfo,
                        Player.DISCONTINUITY_REASON_SKIP
                    )
                    listener.onEvents(this@HybridPlayer, events)
                }

                // Continua il monitoraggio a intervallo stabile (200ms va benissimo)
                mainHandler.postDelayed(this, 200)
            }
        }
    }
     */

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
        //mainHandler.removeCallbacks(positionUpdater)

        exoPlayer.volume = userVolume

        hybridListeners.forEach { listener ->
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
    }

    fun switchToYoutube() {
        activeEngine = ActiveEngine.YOUTUBE

        youtubePlayWhenReady = youtubeControl.isPlaying()

        //mainHandler.removeCallbacks(positionUpdater)
        invalidateYouTubeTrackChanged()

//        // Facciamo ripartire il monitoraggio della posizione solo dopo mezzo secondo
//        mainHandler.postDelayed({
//            if (activeEngine == ActiveEngine.YOUTUBE) {
//                mainHandler.post(positionUpdater)
//            }
//        }, 200)
    }

    // Dentro HybridPlayer.kt

    // Chiamare SOLO dentro Play(), Pause() o quando lo stato play/pausa cambia realmente su YouTube
    fun invalidateYouTubePlayPause() {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_IS_PLAYING_CHANGED)
                .add(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                .add(Player.EVENT_PLAYBACK_STATE_CHANGED)
                .build()
        )

        // Media3 aggiorna la notifica di sistema e Android Auto solo se
        // i listener ricevono queste chiamate esplicite!
        playerService.serviceScope.launch(Dispatchers.Main) {
            hybridListeners.toList().forEach { listener ->
                listener.onPlayWhenReadyChanged(
                    youtubePlayWhenReady,
                    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                )
                listener.onPlaybackStateChanged(STATE_READY)
                listener.onIsPlayingChanged(youtubePlayWhenReady)
                listener.onEvents(this@HybridPlayer, events)
            }
        }
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

        hybridListeners.toList().forEach { listener ->
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            listener.onPlaybackStateChanged(Player.STATE_READY)
            listener.onEvents(this, events)
        }

    }

    // Chiamare dentro il tuo Runnable/Loop continuo ogni 200ms
    fun invalidateYouTubePositionOnly() {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_POSITION_DISCONTINUITY)
                .build()
        )

        // Per la notifica e Android Auto serve notificare la discontinuità di posizione
        val currentPos = getCurrentPosition()
        val positionInfo = Player.PositionInfo(
            null, currentMediaItemIndex, currentMediaItem, null, 0,
            currentPos, currentPos, C.INDEX_UNSET, C.INDEX_UNSET
        )

        hybridListeners.toList().forEach { listener ->
            listener.onPositionDiscontinuity(positionInfo, positionInfo, Player.DISCONTINUITY_REASON_SKIP)
            listener.onEvents(this, events)
        }
    }


    // Metodo privato per sparare gli eventi a MediaSession, Android Auto e Notifiche
    fun forwardEventsToSession(events: Player.Events) {
        for (listener in hybridListeners.toList()) {
            listener.onEvents(this, events)
        }
    }

    // --- OVERRIDE CRITICI PER NEL DIALOGO CON MEDIA3 E GOOGLE ASSISTANT ---

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
//                mainHandler.removeCallbacks(positionUpdater)
//                mainHandler.post(positionUpdater)
                youtubePlayWhenReady = true
            } else {
                youtubeControl.pause()
                //mainHandler.removeCallbacks(positionUpdater) // Blocca la barra
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
//        return commands
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            // Garantisce che Android Auto veda SEMPRE i tasti Play/Pausa e Seek come attivi e cliccabili
            commands.buildUpon()
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
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
            // Aggiorniamo lo stato locale immediatamente
            youtubePlayWhenReady = true

            // Riavvia il polling più velocemente (200ms invece di 500ms)
//            mainHandler.removeCallbacks(positionUpdater)
//            mainHandler.postDelayed(positionUpdater, 200)

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
            //mainHandler.removeCallbacks(positionUpdater)

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
        Timber.d("HybridPlayer seekTo() called: positionMs = $positionMs")
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.seekTo(positionMs)
            invalidateYouTubePositionOnly()
        } else exoPlayer.seekTo(positionMs)
    }

    override fun getVolume(): Float {
        return if (activeEngine == ActiveEngine.YOUTUBE) youtubeControl.getVolume() else exoPlayer.volume
    }

    // Il setVolume standard (chiamato dall'utente o dal sistema) DEVE resettare il fade
    override fun setVolume(volume: Float) {
        userVolume = volume
        fadeMultiplier = 1.0f // Se l'utente tocca il volume, annulliamo eventuali fade residui
        applyCurrentVolume()
    }

    // Questo intercetta chiunque chiami player.seekToNextMediaItem() (es. Android Auto o notifiche)
    override fun seekToNextMediaItem() {
        playerService.handlePlayNext()
    }

    // Questo intercetta chiunque chiami player.seekToPreviousMediaItem()
    override fun seekToPreviousMediaItem() {
        playerService.handlePlayPrevious()
    }

    // Per sicurezza intercettiamo anche i vecchi metodi generici di Media3
    override fun seekToNext() {
        playerService.handlePlayNext()
    }

    override fun seekToPrevious() {
        playerService.handlePlayPrevious()
    }


    // Questa la uso nel Service per fare il Crossfade o il fade!
    fun setFadeVolume(fadeStep: Float) {
        fadeMultiplier = fadeStep
        applyCurrentVolume()
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

        // Applico la normalizzazione software basata sul Loudness DB
        // SOLO se l'engine attivo è YouTube!
        // ExoPlayer è già gestito internamente dal LoudnessEnhancer hardware del Service.
        if (activeEngine == ActiveEngine.YOUTUBE) {
            applyCurrentVolume()
        } else {
            Timber.d("HybridPlayer: ignorato setYtLoudnessDb su ExoPlayer per proteggere il Fade In")
        }
    }

    /**
     * Centralizziamo l'applicazione del volume unendo userVolume e fadeMultiplier
     */
    fun applyCurrentVolume() {
        // Il volume software si basa unicamente sul moltiplicatore del fade (da 0.0 a 1.0)
        val calculatedVolume = fadeMultiplier

        if (activeEngine == ActiveEngine.YOUTUBE) {
            val volumeNormalizationEnabled = playerService.appSettings.volumeNormalizationEnabled

            val finalVolume = if (volumeNormalizationEnabled && ytLoudnessDb != 0f) {
                // Calcolo pulito del fattore di normalizzazione acustica
                val normalizationFactor = 10.0.pow((-ytLoudnessDb / 20.0)).toFloat()

                if (calculatedVolume > 0f) {
                    // Proteggiamo l'output restando nel range nativo 0.0 - 1.0
                    maxOf(0.01f, minOf(1.0f, calculatedVolume * normalizationFactor))
                } else {
                    0f
                }
            } else {
                calculatedVolume
            }

            youtubeControl.setVolume(finalVolume)
            Timber.d("HybridPlayer Fade/Normalizzazione YT: finalVol=$finalVolume (Fade:$fadeMultiplier)")
        } else {
            // ExoPlayer riceve direttamente il moltiplicatore lineare del fade (0.0 a 1.0)
            exoPlayer.volume = calculatedVolume
            Timber.d("HybridPlayer Fade ExoPlayer: volume=$calculatedVolume (Fade:$fadeMultiplier)")
        }
    }



}