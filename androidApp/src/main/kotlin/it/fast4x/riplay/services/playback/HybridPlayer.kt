package it.fast4x.riplay.services.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.MediaSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
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
    private val exoPlayer: ExoPlayer,
    private val youtubeControl: YouTubeControl
) : ForwardingPlayer(exoPlayer) {

    // Variabili per la normalizzazione YouTube
    private var userVolume: Float = 1.0f
    private var ytLoudnessDb: Float = 0f
    // Variabile per gestire il fade separatamente
    private var fadeMultiplier: Float = 1.0f

    // ── FIX "torna all'inizio" ─────────────────────────────────────────────────
    // Traccia l'ultimo mediaId effettivamente caricato nel player YouTube.
    // Serve a capire se il video è DAVVERO caricato, invece di basarsi su
    // playerService._currentDuration.value == 0f (che è 0 anche a metà canzone
    // in momenti del tutto normali: callback della durata non ancora arrivata,
    // pause/resume rapido, WebView ricaricata, audio focus riacquistato...).
    private var lastLoadedYtMediaId: String? = null

    // Ultima posizione nota vista dal tick dei 200ms: permette di emettere
    // EVENT_POSITION_DISCONTINUITY solo per salti REALI (seek, ad, reset),
    // non 5 volte al secondo come faceva la versione precedente.
    private var lastTickYtPositionMs: Long = C.TIME_UNSET

    // Sopra questa soglia, il cambio di posizione tra un tick e l'altro è un vero salto
    private val positionJumpThresholdMs: Long = 1500L

    var onRefreshCustomLayoutListener: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    var youtubePlayWhenReady = false

    var activeEngine: ActiveEngine = ActiveEngine.EXOPLAYER
        private set

    private val hybridListeners = mutableListOf<Player.Listener>()

    private val playbackStatsListener =
        PlaybackStatsListener(false, playerService)
    private var analyticsAttached = false

    // NOTA: il filtro eventi del segnaposto (YtPlaceholderEventFilter) è stato RIMOSSO.
    // Intercettava STATE_ENDED e playWhenReady(false, END_OF_MEDIA_ITEM) generati dal
    // "file vuoto" di ExoPlayer in modalità YouTube, ma senza conoscere il flusso
    // interno di PlayerService rischiava di nascondere eventi di cui il Service ha
    // bisogno all'avvio del brano (regressione: il brano non partiva più).
    // Il bug del riavvio è risolto da executeActualPlay() e non richiede questo filtro.
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

        exoPlayer.volume = userVolume

        // Registriamo il listener delle statistiche solo quando yt è il motore attivo
        // Lo inseriamo in un try catch perchè potrebbe creare un crash
        if (!analyticsAttached) {
            try {
                exoPlayer.addAnalyticsListener(playbackStatsListener)
            } catch (e: Exception) { Timber.e("HybridPlayer PlaybackStatsListener failed to attach to ExoPlayer: ${e.message}")}

            analyticsAttached = true
        }

        // Lo inseriamo in un try catch perchè potrebbe creare un crash di ritorno
        try {
            hybridListeners.forEach { listener ->
                listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            }
        } catch (e: Exception) { Timber.e("HybridPlayer listener failed to update timeline: ${e.message}")}

    }

    fun switchToYoutube() {
        activeEngine = ActiveEngine.YOUTUBE

        // FIX: nuova sessione YouTube → riparte il tracciamento della posizione del tick,
        // così il primo tick non genera un falso "salto" rispetto alla posizione di ExoPlayer.
        lastTickYtPositionMs = C.TIME_UNSET

        // Rimuoviamo il listener delle statistiche non ci serve per l'online
        // Lo inseriamo in un try catch perchè potrebbe creare un crash
        if (analyticsAttached) {
            try {
                exoPlayer.removeAnalyticsListener(playbackStatsListener)
            } catch (e: Exception) { Timber.e("HybridPlayer PlaybackStatsListener failed to detach from ExoPlayer: ${e.message}")}

            analyticsAttached = false
        }

        youtubePlayWhenReady = youtubeControl.isPlaying()

        invalidateYouTubeTrackChanged()

    }

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
    //
    // FIX "torna all'inizio": la versione precedente emetteva
    // EVENT_POSITION_DISCONTINUITY con DISCONTINUITY_REASON_SKIP ad OGNI tick,
    // anche quando la posizione avanzava normalmente (5 eventi finti al secondo).
    // Qualsiasi listener del Service che reagisce alle discontinuità (crossfade,
    // salvataggio della posizione, logica "la posizione è saltata → risincronizza")
    // riceveva eventi finti continui e poteva innescare un riavvio del brano.
    // Ora l'evento esce SOLO per salti reali. Il parametro force (default false)
    // mantiene la compatibilità con le chiamate esistenti del Service.
    fun invalidateYouTubePositionOnly(force: Boolean = false) {
        val currentPos = getCurrentPosition()
        val previousPos = lastTickYtPositionMs
        lastTickYtPositionMs = currentPos

        if (!force) {
            if (previousPos == C.TIME_UNSET) return
            val delta = currentPos - previousPos
            if (abs(delta) <= positionJumpThresholdMs) return
        }

        emitYtPositionDiscontinuity(currentPos)
    }

    // Emette UNA discontinuità di posizione reale verso i listener (notifica, Android Auto...)
    private fun emitYtPositionDiscontinuity(positionMs: Long) {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_POSITION_DISCONTINUITY)
                .build()
        )

        val positionInfo = Player.PositionInfo(
            null, currentMediaItemIndex, currentMediaItem, null, 0,
            positionMs, positionMs, C.INDEX_UNSET, C.INDEX_UNSET
        )

        hybridListeners.toList().forEach { listener ->
            listener.onPositionDiscontinuity(positionInfo, positionInfo, Player.DISCONTINUITY_REASON_SEEK)
            listener.onEvents(this, events)
        }
    }

    /**
     * FIX "torna all'inizio": chiamare dal PlayerService nel momento ESATTO in cui
     * il player YouTube conferma di aver caricato un video (es. callback onVideoLoaded /
     * onStateChange PLAYING / dopo il loadVideo fatto dal Service stesso).
     * In questo modo executeActualPlay() sa che il video è già dentro il player
     * e NON lo ricarica da capo al successivo play().
     */
    fun markYouTubeVideoLoaded(mediaId: String?) {
        lastLoadedYtMediaId = mediaId
        // Nuovo video: riparte il tracciamento della posizione del tick,
        // così il primo tick non genera un falso "salto" rispetto al brano precedente.
        lastTickYtPositionMs = C.TIME_UNSET
    }


    // Metodo privato per sparare gli eventi a MediaSession, Android Auto e Notifiche
    fun forwardEventsToSession(events: Player.Events) {
        for (listener in hybridListeners.toList()) {
            listener.onEvents(this, events)
        }
    }

    // --- OVERRIDE CRITICI PER NEL DIALOGO CON MEDIA3 E GOOGLE ASSISTANT ---

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        return if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.setPlaybackRate(playbackParameters.speed)
        }
        else super.setPlaybackParameters(playbackParameters)

    }

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
        val currentPosition = if (activeEngine == ActiveEngine.YOUTUBE) {
            val v = (playerService._currentSecond.value * 1000L).toLong()
            //Timber.d("TICK-POS-YT $v")
            v
        } else {
            val v = super.getCurrentPosition()
            //Timber.d("TICK-POS-EXO $v (idx=$currentMediaItemIndex)")
            v
        }
        return currentPosition
    }

    override fun getDuration(): Long {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val alternativeDuration = (playerService._currentDuration.value * 1000L).toLong()
            return if (alternativeDuration > 0) {
                alternativeDuration
            } else {
                C.TIME_UNSET
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
        val result = if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubePlayWhenReady
        } else {
            exoPlayer.playWhenReady
        }
        Timber.d("HybridPlayer getPlayWhenReady() called: activeEngine = $activeEngine result = $result")

        val azione = if (result) "PLAY" else "PAUSA"
        tracciaSorgenteComando("HybridPlayer getPlayWhenReady -> $azione")

        return result
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        val azione = if (playWhenReady) "PLAY" else "PAUSA"
        tracciaSorgenteComando("HybridPlayer setPlayWhenReady <- $azione")

        when (activeEngine) {
            ActiveEngine.YOUTUBE -> {
                if (playWhenReady) youtubeControl.play() else youtubeControl.pause()
            }
            ActiveEngine.EXOPLAYER -> super.setPlayWhenReady(playWhenReady)
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

    // ── INTERCETTATORI PUBBLICI (Chiamati da UI, Cuffie, Android Auto) ───────
    override fun play() {
        // Invece di far esplodere l'audio, deleghiamo al Service per la risalita morbida
        playerService.requestSmoothPlay()
    }

    override fun pause() {
        // Invece di tagliare l'audio di colpo, deleghiamo al Service per la discesa morbida
        playerService.requestSmoothPause()
    }

    // ── L'ESECUZIONE REALE DEL COMANDO IN ARRIVO DA PLAYERSERVICE EVITA IL LOOP  ────────
    fun executeActualPlay() {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            //Timber.d("HybridPlayer executeActualPlay() called currentDuration is ${playerService._currentDuration.value}")

            // ── FIX "torna all'inizio" (versione sicura per l'avvio) ──
            // Il bug originale: con _currentDuration == 0f QUALSIASI play() ricaricava
            // il brano con loadVideo(mediaId, 0f), facendolo ripartire da capo.
            // Regole attuali:
            //  - NUOVO mediaId (traccia diversa o mai caricata) → loadVideo(id, 0f):
            //    IDENTICO al comportamento originale per le canzoni nuove;
            //  - STESSO mediaId + durata nota → semplice play() (nessun restart);
            //  - STESSO mediaId + durata a 0 (WebView scaricata / resync) → loadVideo
            //    dall'ULTIMA POSIZIONE NOTA invece che dall'inizio.
            // NB: qui si legge SOLO lo StateFlow _currentDuration del Service, NESSUNA
            // chiamata alla WebView (youtubeControl.getDurationMs), che nella versione
            // precedente poteva bloccare/rompere l'avvio del brano se chiamata troppo presto.
            val mediaId = playerService.exoPlayer.currentMediaItem?.mediaId
            val durationLoaded = playerService._currentDuration.value > 0f

            if (mediaId != null && mediaId != lastLoadedYtMediaId) {
                // Nuovo brano (o primo avvio): carica da 0, come il codice originale
                Timber.d("HybridPlayer executeActualPlay() mediaId=$mediaId is new, loading from 0")
                playerService._internalYouTubePlayer.value?.loadVideo(mediaId, 0f)
                lastLoadedYtMediaId = mediaId
                // Il video riparte da 0: nuova baseline del tick, niente falsi salti
                lastTickYtPositionMs = 0L
            } else if (mediaId != null && !durationLoaded) {
                // Stesso video ma stato "non caricato" (WebView scaricata, resync...):
                // riparte dall'ultima posizione nota invece che dall'inizio — qui sta la fix
                val resumePositionMs = getCurrentPosition().coerceAtLeast(0L)
                Timber.d("HybridPlayer executeActualPlay() mediaId=$mediaId unloaded, reloading from ${resumePositionMs}ms")
                playerService._internalYouTubePlayer.value?.loadVideo(mediaId, resumePositionMs / 1000f)
                lastTickYtPositionMs = resumePositionMs
            } else {
                Timber.d("HybridPlayer executeActualPlay() mediaId=$mediaId already loaded, just play")
                youtubeControl.play()
            }
            youtubePlayWhenReady = true
            invalidateYouTubePlayPause()

            mainHandler.postDelayed({
                onRefreshCustomLayoutListener?.invoke()
            }, 50)
        } else {
            // super.play() originale per ExoPlayer
            super.play()
        }
    }

    fun executeActualPause() {
        //Timber.d("EXECUTE_ACTUAL_PAUSE stack:\n${Exception().stackTraceToString()}")
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.pause()
            youtubePlayWhenReady = false

            invalidateYouTubePlayPause()

            mainHandler.postDelayed({
                onRefreshCustomLayoutListener?.invoke()
            }, 50)
        } else {
            // super.pause() originale per ExoPlayer
            super.pause() // Chiama il metodo nativo di ForwardingPlayer/ExoPlayer
        }
    }


    override fun seekTo(positionMs: Long) {
        Timber.d("HybridPlayer seekTo() called: positionMs = $positionMs")
        if (activeEngine == ActiveEngine.YOUTUBE) {
            youtubeControl.seekTo(positionMs)
            // FIX "torna all'inizio": il seek diventa la nuova baseline del tick
            // (così il prossimo tick non lo scambia per un salto spurio) e viene
            // emesso UN solo evento di discontinuità reale con la posizione target.
            lastTickYtPositionMs = positionMs
            emitYtPositionDiscontinuity(positionMs)
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
        tracciaSorgenteComando("HybridPlayer seekToNextMediaItem()")
        playerService.handlePlayNextRequestedByUser("HybridPlayer.seekToNextMediaItem")
    }

    // Questo intercetta chiunque chiami player.seekToPreviousMediaItem()
    override fun seekToPreviousMediaItem() {
        tracciaSorgenteComando("HybridPlayer seekToPreviousMediaItem()")
        playerService.handlePlayPreviousRequestedByUser("HybridPlayer.seekToPreviousMediaItem")
    }

    // Per sicurezza intercettiamo anche i vecchi metodi generici di Media3
    override fun seekToNext() {
        tracciaSorgenteComando("HybridPlayer seekToNext()")
        playerService.handlePlayNextRequestedByUser("HybridPlayer.seekToNext")
    }

    override fun seekToPrevious() {
        tracciaSorgenteComando("HybridPlayer seekToPrevious()")
        playerService.handlePlayPreviousRequestedByUser("HybridPlayer.seekToPrevious")
    }


    // Questa la uso nel Service per fare il Crossfade o il fade!
    fun setFadeVolume(fadeStep: Float) {
        fadeMultiplier = fadeStep
        applyCurrentVolume()
    }

    fun updateCurrentMediaItemDuration(durationMs: Long) {
        if (activeEngine == ActiveEngine.YOUTUBE) {
            val currentItem = currentMediaItem ?: return

            // FIX "torna all'inizio": se la durata è già quella, NON rifare replaceMediaItem.
            // Ogni replace sul brano corrente riavvia il file vuoto segnaposto di ExoPlayer,
            // che genera un SECONDO STATE_ENDED a metà canzone: il gestore di "canzone
            // finita" del Service poteva prenderlo per una fine vera e far ripartire
            // (o avanzare) il brano. Deduplicando si evita anche il churn di timeline.
            val existingDurationMs = currentItem.mediaMetadata.durationMs
            if (existingDurationMs != null && abs(existingDurationMs - durationMs) < 1000L) {
                return
            }

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


    // 1. Definizione del tracker ThreadLocal
    object CommandTracker {
        private val currentController = ThreadLocal<MediaSession.ControllerInfo>()

        fun set(controllerInfo: MediaSession.ControllerInfo) {
            currentController.set(controllerInfo)
        }

        fun get(): MediaSession.ControllerInfo? = currentController.get()

        fun clear() {
            currentController.remove()
        }
    }

    fun tracciaSorgenteComando(metodo: String) {
        var controllerInfo = CommandTracker.get()

        // Se è null, facciamo un fallback sul metodo nativo di Media3 (valido per chiamate sincrone alla sessione)
        if (controllerInfo == null) {
            controllerInfo = playerService.mediaLibrarySession?.controllerForCurrentRequest
        }

        //Timber.d("HybridPlayer RiPlay_Media3_Source_Trace: controllerForCurrentRequest = ${playerService.mediaLibrarySession?.controllerForCurrentRequest}")

        val pacchetto = controllerInfo?.packageName
        val uid = controllerInfo?.uid


        val sorgente = when {
            pacchetto != null && pacchetto == "com.google.android.projection.gearhead" -> "Android Auto (Gearhead)"
            pacchetto != null && pacchetto == playerService.packageName -> "Interfaccia UI Interna (RiPlay)"
            pacchetto != null && pacchetto.contains("bluetooth") -> "Tasti Bluetooth / Volante"
            pacchetto != null && pacchetto == "com.android.systemui" -> "Notifica di Sistema / Lockscreen"
            pacchetto != null && pacchetto.contains("com.google.android.googlequicksearchbox") -> "Google Assistant"
            pacchetto != null -> "Altro Controller Esterno ($pacchetto)"
            else -> "Metodo nativo di Media3"
        }

        Timber.d("HybridPlayer RiPlay_Media3_Source_Trace: [$sorgente] -> Esecuzione metodo: $metodo")
    }



}