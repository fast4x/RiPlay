package it.fast4x.riplay.player.kcef

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.cef.CefClient
import org.cef.browser.CefBrowser
import player.PlayerController
import player.PlayerState

@Serializable
private data class KcefJsState(
    val s: Int = -99,
    val t: Float = 0f,
    val d: Float = 0f,
    val v: Int = 100,
    val m: Boolean = false,
    // Ultimo codice onError del player YT (0 = nessun errore)
    val err: Int = 0
)

/**
 * Motore YouTube desktop: browser CEF (KCEF) che carica la pagina
 * IFrame Player API ([PlayerHtmlServer]) e la pilota via JavaScript.
 *
 * Replica del meccanismo Android (WebViewYouTubePlayer.initWebView,
 * fork AYP) con contesto embed ACCETTATO da YouTube: l'HTML del player e'
 * generato QUI con i parametri interni SOSTITUITI PRIMA del caricamento
 * (<<injectedVideoId>> con il videoId in attesa, <<injectedPlayerVars>> con
 * i playerVars autolock AYP e origin dinamico) e viene SERVITO dal server
 * loopback LOCALE caricandolo come http://localhost:PORT/player.html (il
 * NOME localhost e' accettato dalla politica embed di YouTube: l'IP
 * 127.0.0.1 e www.youtube.com producono errore 152 su tutti i video,
 * verifica empirica). Sopra le richieste /embed/ KcefEngine impone
 * il Referer apex youtube.com (contesto Android loadDataWithBaseURL).
 * Mentre la pagina non e' ancora ATTIVA il videoId richiesto resta in
 * attesa ([pendingVideoId]) e viene:
 *  - sostituito DENTRO l'HTML se la pagina non e' ancora stata servita
 *    (parte da solo via autoplay=1, esattamente come Android);
 *  - inviato via riplayLoad/riplayCue alla conferma della pagina
 *    ([PlayerHtmlServer.onPageConfirmed]).
 *
 * Implementa sia il [PlayerController] desktop (comunicazione con le
 * barre di controllo esistenti) sia [YouTubeControl] (specchio Android).
 */
class KcefYouTubePlayer(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : PlayerController, YouTubeControl {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    /**
     * Ultimo codice errore segnalato dal player YouTube (0 = nessuno).
     * Esposto alla UI ([KcefVideoSurface] mostra codice + significato) e al log
     * con il significato: e' la chiave per diagnosticare "An error occurred".
     */
    private val _ytError = MutableStateFlow(0)
    val ytError: StateFlow<Int> = _ytError.asStateFlow()

    @Volatile private var lastLoggedYtErr = 0

    /**
     * v47: TRUE = il browser e' REALMENTE creato e attaccato al pannello
     * ospite (componente AWT presente nella gerarchia). FALSE = client CEF
     * non ancora pronto, creazione non ancora avvenuta, browser in
     * ricreazione o rilasciato (dispose/switch motore). Stato osservabile in
     * Compose: la UI (YoutubeKcefPane) lo usa per mostrare copertina/
     * segnaposto INVECE del foro nero quando la superficie del browser non
     * esiste ancora (sintomo "pannello player tutto nero" su Wayland).
     */
    private val _browserAttached = MutableStateFlow(false)
    val browserAttached: StateFlow<Boolean> = _browserAttached.asStateFlow()

    @Volatile private var browser: CefBrowser? = null
    @Volatile private var hostPanel: KcefPlayerHostPanel? = null
    @Volatile private var pendingVideoId: String? = null
    @Volatile private var pendingStart: Float = 0f
    @Volatile private var pendingIsCue: Boolean = false
    @Volatile private var muted = false
    private var pollJob: Job? = null

    // NESSUN errore visibile. Da qui:
    //  - lastVideoId: l'ultimo id richiesto, base di ogni recupero;
    //  - watchdog: se il video non parte entro 12 s (niente play, niente
    //    durata, niente errore) re-invia riplayLoad (pagina viva) oppure
    //    RICREA il browser ri-cuocendo il videoId nell'HTML (pagina morta:
    //    e' il meccanismo autoplay, il piu' affidabile validato);
    //  - play(): se non c'e' nessun video attivo (durata 0) ARMA il recupero
    //    invece di restare muto: il tasto play ha SEMPRE un effetto.
    @Volatile private var lastVideoId: String? = null
    @Volatile private var lastPollOkAt: Long = 0L
    private var watchdogJob: Job? = null
    @Volatile private var recreatingBrowser = false
    private val browserLock = Any()
    @Volatile private var lastRawYtState: Int = -99
    private var kickJob: Job? = null

    init {
        // v46 (jcefmaven): collegamento al bridge di stato standard CEF
        // (CefMessageRouter installato sul client da KcefEngine.installStateBridge):
        // la pagina pusha lo stato con window.cefQuery, Kotlin lo riceve qui.
        KcefEngine.onPlayerStateMessage = ::handleStatePush
    }

    /**
     * Callback di FINE BRANO naturale (stato YT 0 = ENDED). E' il
     * gancio dell'AVANZAMENTO AUTOMATICO della coda di riproduzione: quando
     * il video finisce da solo (NON con stop/pausa/nuovo load) il player
     * notifica l'app, che avvia il brano successivo della coda.
     * Gara e duplicati gestiti con endedFiredForVideo: un solo evento per
     * videoId, azzerato al load successivo.
     */
    var onEnded: (() -> Unit)? = null
    @Volatile private var endedFiredForVideo: String? = null

    @Volatile private var stateForVideoId: String? = null
    @Volatile private var consecutiveTimeReached = 0
    private var hiddenEndJob: Job? = null

    /**
     * SINGOLO punto di emissione della FINE BRANO NATURALE.
     * Dedup per videoId (una sola emissione per brano, gara tra i percorsi
     * risolta dal primo che arriva) e log con il motivo: yt-state-0 (stato
     * YT 0 = ENDED, il percorso storico), tempo-raggiunto (poll vivo ma
     * stato page frozen: clock media a fine durata),
     * watchdog-finestra-nascosta (poll morto: stima a tempo reale).
     */
    private fun fireNaturalEnd(reason: String) {
        val current = lastVideoId
        if (current == null || current.isBlank()) return
        synchronized(this) {
            if (endedFiredForVideo == current) return
            endedFiredForVideo = current
        }
        KcefEngine.logKcef("fine brano naturale ($reason): videoId='$current' -> avanza la coda")
        onEnded?.invoke()
    }

    /**
     * Crea (una volta sola) il browser CEF e restituisce il componente
     * AWT da incorporare. Chiamare dal thread EDT (SwingPanel factory).
     */
    fun attachBrowser(): java.awt.Component {
        val host = hostPanel ?: KcefPlayerHostPanel().also { hostPanel = it }
        ensureBrowser()
        return host
    }

    /**
     * Cea il browser SE MANCA (idempotente, thread-safe). Puo'
     * essere richiamato piu' volte: dalla factory del SwingPanel, dal
     * watchdog e dal recupero del play. Se il client non e' ancora pronto
     * esce SENZA mettere nulla in cache: il watchdog riprovera'.
     */
    private fun ensureBrowser() {
        synchronized(browserLock) {
            if (browser != null) return
            val client: CefClient = KcefEngine.clientOrNull() ?: run {
                KcefEngine.logKcef(
                    "attachBrowser: client CEF non ancora pronto (il watchdog riprovera')"
                )
                return
            }
            PlayerHtmlServer.resetPageSession()
            PlayerHtmlServer.pageHtmlProvider = { buildPlayerPageHtml() }
            PlayerHtmlServer.onPageConfirmed = { confirmPending() }
            val playerUrl = PlayerHtmlServer.ensureLocalServer()
            KcefEngine.logKcef("attachBrowser: pagina player su $playerUrl (origine onesta localhost)")
            // v46 (jcefmaven): createBrowser(String, boolean osr, boolean transparent)
            // = API org.cef standard; osr=false = rendering windowed (CefRendering
            // DEFAULT dell'era KCEF).
            val b: CefBrowser = client.createBrowser(
                playerUrl,
                false,
                false
            )
            browser = b
            hostPanel?.setBrowser(b.uiComponent)
            // createImmediately crea finestre native: DEVE girare sull'EDT
            if (java.awt.EventQueue.isDispatchThread()) b.createImmediately()
            else java.awt.EventQueue.invokeAndWait { runCatching { b.createImmediately() } }
            startPolling()
            // v47: il browser e' ora REALMENTE attaccato al pannello
            // ospite: la UI puo' togliere il segnaposto/cover e mostrare
            // il browser (aggiorna browserAttached osservato in Compose)
            _browserAttached.value = true
            KcefEngine.logKcef("browser attaccato al pannello ospite (v47 browserAttached=true)")
            // NB: nessuna evaluate qui: il videoId in attesa viene SOSTITUITO
            // dentro l'HTML della pagina (meccanismo Android <<injectedVideoId>>)
            // oppure inviato alla conferma della pagina (onPageConfirmed).
        }
    }

    /**
     * Invio del videoId in attesa alla conferma della pagina,
     * seguito dal kick autoplay
     */
    private fun confirmPending() {
        val id = pendingVideoId
        if (id != null) {
            pendingVideoId = null
            val start = pendingStart
            if (pendingIsCue) evaluate("window.riplayCue && riplayCue('$id', $start)")
            else evaluate("window.riplayLoad && riplayLoad('$id', $start)")
            scheduleAutoplayKick(id)
        }
    }

    /**
     * KICK AUTOPLAY. Un loadVideoById su pagina attiva parte da
     * SOLO se la policy lo consente
     */
    private fun scheduleAutoplayKick(videoId: String) {
        kickJob?.cancel()
        kickJob = scope.launch {
            delay(3_000L)
            val raw = lastRawYtState
            if (raw == 1 || raw == 3 || _ytError.value != 0 || lastVideoId != videoId) return@launch
            KcefEngine.logKcef(
                "autoplay kick: '$videoId' non e' partito da solo (stato YT=$raw): " +
                        "click di attivazione + play"
            )
            injectUserActivation("autoplay kick $videoId")
            scope.launch {
                delay(250L)
                evaluate("window.riplayPlay && riplayPlay()")
            }
        }
    }

    /**
     * Iniezione di USER ACTIVATION nel browser. YouTube accetta
     * playVideo() (e l'autoplay) solo se la pagina/iframe ha avuto un GESTO
     * UTENTE reale (user activation policy di Chromium;
     */
    private fun injectUserActivation(reason: String) {
        val b = browser ?: return
        java.awt.EventQueue.invokeLater {
            runCatching {
                val raw = lastRawYtState
                if (raw == 1 || raw == 3) {
                    KcefEngine.logKcef(
                        "activation: NON inviato (il video e' gia' attivo, stato YT=$raw): $reason"
                    )
                    return@invokeLater
                }
                val comp = b.uiComponent as? java.awt.Component
                val w = (comp?.width ?: 0).coerceAtLeast(64)
                val h = (comp?.height ?: 0).coerceAtLeast(36)
                val x = 2
                val y = 2
                val now = System.currentTimeMillis()
                val moved = java.awt.event.MouseEvent(
                    comp, java.awt.event.MouseEvent.MOUSE_MOVED, now, 0, x, y, 0, false
                )
                val pressed = java.awt.event.MouseEvent(
                    comp, java.awt.event.MouseEvent.MOUSE_PRESSED, now,
                    java.awt.event.InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                    java.awt.event.MouseEvent.BUTTON1
                )
                val released = java.awt.event.MouseEvent(
                    comp, java.awt.event.MouseEvent.MOUSE_RELEASED, now + 10, 0, x, y, 1, false,
                    java.awt.event.MouseEvent.BUTTON1
                )
                b.sendBrowserMouseEventCompat(moved)
                b.sendBrowserMouseEventCompat(pressed)
                b.sendBrowserMouseEventCompat(released)
                KcefEngine.logKcef(
                    "activation: input reale (mouse MOVED+PRESS+RELEASE a $x,$y) inviato a CEF " +
                            "(canvas=${w}x$h): $reason"
                )
            }.onFailure {
                KcefEngine.logKcef(
                    "activation FALLITA: ${it.javaClass.simpleName}: ${it.message}"
                )
            }
        }
    }

    /**
     * Ricostruzione del browser (STESSO pannello ospite) quando la
     * pagina risulta morta o mai attiva: il videoId viene ri-cotto nell'HTML
     */
    private fun recreateBrowser(reason: String, videoId: String, attempt: Int, allowPlayRecovery: Boolean = true) {
        if (recreatingBrowser) {
            // rientro (es. un secondo watchdog armato dal play): riprova piu'
            // tardi invece di far morire la catena in silenzio
            armWatchdog(videoId, attempt + 1, allowPlayRecovery)
            return
        }
        recreatingBrowser = true
        try {
            synchronized(browserLock) {
                if (hostPanel == null) return
                val client = KcefEngine.clientOrNull()
                if (client == null) {
                    KcefEngine.logKcef("ricrea browser: client CEF assente, riprovero' al prossimo tentativo")
                    armWatchdog(videoId, attempt + 1, allowPlayRecovery)
                    return
                }
                KcefEngine.logKcef("ricreo il browser del player: $reason")
                val old = browser
                browser = null
                _browserAttached.value = false
                pollJob?.cancel()
                pollJob = null
                pendingVideoId = videoId
                java.awt.EventQueue.invokeAndWait {
                    runCatching { old?.closeBrowserCompat() }
                    PlayerHtmlServer.resetPageSession()
                    PlayerHtmlServer.pageHtmlProvider = { buildPlayerPageHtml() }
                    PlayerHtmlServer.onPageConfirmed = { confirmPending() }
                    runCatching {
                        val playerUrl = PlayerHtmlServer.ensureLocalServer()
                        // v46 (jcefmaven): createBrowser(url, osr=false, transparent=false)
                        val b = client.createBrowser(playerUrl, false, false)
                        browser = b
                        b.createImmediately()
                        hostPanel?.setBrowser(b.uiComponent)
                    }.onFailure {
                        KcefEngine.logKcef(
                            "ricrea browser: creazione fallita: ${it.javaClass.simpleName}: ${it.message}"
                        )
                    }.onSuccess {
                        _browserAttached.value = true
                    }
                }
                startPolling()
            }
            armWatchdog(videoId, attempt + 1, allowPlayRecovery)
        } catch (t: Throwable) {
            KcefEngine.logKcef("ricrea browser FALLITA: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            recreatingBrowser = false
        }
    }

    /**
     * Vigilanza sul caricamento. Se entro 12 s il video non e' ne'
     * in riproduzione, ne' caricato (durata nota), ne' in errore, la catena
     * si e' persa da qualche parte: re-invio del load (pagina viva) oppure
     * RICREAZIONE del browser con videoId ri-cotto nell'HTML (pagina morta).
     * Massimo 3 tentativi, ognuno con log puntuale dello stato della catena.
     */
    private fun armWatchdog(videoId: String, attempt: Int = 0, allowPlayRecovery: Boolean = true) {
        watchdogJob?.cancel()
        if (attempt >= 3) {
            KcefEngine.logKcef(
                "watchdog: recupero esaurito per '$videoId' dopo 3 tentativi. Il brano non parte: " +
                        "controlla le righe [RiPlay-KCEF] e [RiPlay-YT] del log (pagina attiva? rete?)."
            )
            return
        }
        watchdogJob = scope.launch {
            delay(12_000L)
            val st = _state.value
            val err = _ytError.value
            val raw = lastRawYtState
            // in riproduzione, in errore, in pausa o in buffering: il player
            // sta funzionando, niente recupero
            if (st.isPlaying || err != 0 || raw == 2 || raw == 3) return@launch
            val pageAlive = System.currentTimeMillis() - lastPollOkAt < 8_000L
            KcefEngine.logKcef(
                "watchdog: '$videoId' non riproduce (tentativo ${attempt + 1}/3): " +
                        "paginaViva=$pageAlive pageActive=${PlayerHtmlServer.pageActive} " +
                        "browser=${browser != null} isPlaying=${st.isPlaying} statoYT=$raw " +
                        "durata=${st.duration} err=$err"
            )
            if (!pageAlive || !PlayerHtmlServer.pageActive || browser == null) {
                recreateBrowser("pagina player non risponde o assente", videoId, attempt, allowPlayRecovery)
            } else if (st.duration > 0L || raw == 5 || raw == 1) {
                if (!allowPlayRecovery) {
                    // video in CUE pronto: i metadati sono arrivati
                    // (durata nota) ma la riproduzione e' volutamente ferma
                    // in attesa del play dell'utente: NESSUN intervento
                    KcefEngine.logKcef(
                        "watchdog: video in CUE pronto e fermo (attesa del play dell'utente): nessun recupero"
                    )
                    return@launch
                }

                KcefEngine.logKcef(
                    "watchdog: video caricato ma NON partito (autoplay bloccato): " +
                            "click di attivazione + play"
                )
                injectUserActivation("watchdog autoplay bloccato $videoId")
                scope.launch {
                    delay(250L)
                    evaluate("window.riplayPlay && riplayPlay()")
                }
                armWatchdog(videoId, attempt + 1, allowPlayRecovery)
            } else if (!allowPlayRecovery) {
                // il cue non ha prodotto niente (niente durata):
                // ri-invio del CUE (NON del load, che avvierebbe il brano)
                KcefEngine.logKcef("watchdog: pagina viva, re-invio riplayCue('$videoId')")
                evaluate("window.riplayCue && riplayCue('$videoId', ${pendingStart})")
                armWatchdog(videoId, attempt + 1, allowPlayRecovery)
            } else {
                // il load non ha mai prodotto nulla: re-invio con activation
                KcefEngine.logKcef("watchdog: pagina viva, activation + re-invio riplayLoad('$videoId')")
                injectUserActivation("watchdog re-invio $videoId")
                evaluate("window.riplayLoad && riplayLoad('$videoId', ${pendingStart})")
                armWatchdog(videoId, attempt + 1, allowPlayRecovery)
            }
        }
    }

    private fun disarmWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    /**
     * HTML del player con i parametri interni SOSTITUITI (meccanismo
     * Android): il videoId in attesa viene QUOTATO dentro la pagina e
     * CONSUMATO (parte da solo con l'aplay=1 dei playerVars); se non c'e'
     * nessun video in attesa la pagina nasce con undefined, identica ad
     * AYP quando initialize riceve videoId null.
     */
    private fun buildPlayerPageHtml(): String {
        val id = pendingVideoId?.let { extractYouTubeVideoId(it) }
        if (id != null) {
            // il flag cue va letto PRIMA di consumare l'attesa
            val wasCue = pendingIsCue
            pendingVideoId = null
            pendingIsCue = false
            KcefEngine.logKcef(
                "pagina player: videoId='$id' sostituito nell'HTML (meccanismo Android" +
                        ", autoplay=" + if (wasCue) "0 = cue" else "1" + ")"
            )
            return PlayerHtmlServer.playerPageHtml("'$id'", autoplay = !wasCue)
        }
        return PlayerHtmlServer.playerPageHtml(null)
    }

    // ---- PlayerController ----

    override fun load(url: String) {
        val id = extractYouTubeVideoId(url)
            ?: url.trim().takeIf { VIDEO_ID.matches(it) }
            ?: return
        loadVideoById(id, 0f)
    }

    override fun play() {
        val raw = lastRawYtState
        if (raw == 3) {
            // buffering: la riproduzione e' GIA' partita (la policy e' stata
            // superata): il click sintetico la METTEREBBE IN PAUSA. Solo play.
            evaluate("window.riplayPlay && riplayPlay()")
            return
        }
        if (raw != 1) {
            KcefEngine.logKcef("play: activation (stato YT=$raw) poi riplayPlay")
            injectUserActivation("play richiesto dall'utente")
            scope.launch {
                delay(250L)
                evaluate("window.riplayPlay && riplayPlay()")
            }
        } else {
            evaluate("window.riplayPlay && riplayPlay()")
        }

        val st = _state.value
        val id = lastVideoId
        if (id != null && !st.isPlaying && _ytError.value == 0 && raw != 2) {
            KcefEngine.logKcef(
                "play: nessun video attivo nel player (stato YT=$raw durata=${st.duration}): " +
                        "armo il recupero per '$id'"
            )
            armWatchdog(id, 0)
        }
    }

    override fun pause() = evaluate("window.riplayPause && riplayPause()")

    override fun stop() = evaluate("window.riplayStop && riplayStop()")

    override fun dispose() {
        disarmWatchdog()
        kickJob?.cancel()
        kickJob = null
        pollJob?.cancel()
        pollJob = null
        hiddenEndJob?.cancel()
        hiddenEndJob = null
        stateForVideoId = null
        consecutiveTimeReached = 0
        val b = browser
        browser = null
        _browserAttached.value = false
        val host = hostPanel
        hostPanel = null
        runCatching { host?.removeAll() }
        pendingVideoId = null
        lastVideoId = null
        PlayerHtmlServer.onPageConfirmed = null
        PlayerHtmlServer.pageHtmlProvider = null
        runCatching { b?.closeBrowserCompat() }
    }

    override fun seekTo(timestamp: Long) =
        evaluate("window.riplaySeekTo && riplaySeekTo(${timestamp / 1000.0})")

    override fun setVolume(value: Float) {
        val v = (value * 100).toInt().coerceIn(0, 100)
        evaluate("window.riplaySetVolume && riplaySetVolume($v)")
        if (v > 0) muted = false
    }

    override fun toggleSound() {
        muted = !muted
        evaluate("window.riplaySetMuted && riplaySetMuted($muted)")
    }

    // ---- YouTubeControl ----

    override fun loadVideoById(videoId: String, startSeconds: Float) {

        val id = extractYouTubeVideoId(videoId) ?: run {
            KcefEngine.logKcef(
                "loadVideoById IGNORATO: '$videoId' non contiene un videoId YouTube valido (11 caratteri)"
            )
            return
        }
        pendingStart = startSeconds
        muted = false
        pendingIsCue = false
        resetYtError()

        KcefEngine.logKcef("loadVideoById: videoId='$id' (richiesto: '$videoId') start=$startSeconds")
        // reset tempo/durata: la nuova traccia parte da zero
        _state.value = _state.value.copy(isPlaying = false, timestamp = 0L, duration = 0L)
        // il nuovo video puo' di nuovo notificare la fine naturale
        endedFiredForVideo = null
        // la rilevazione del tempo-raggiunto riparte da zero e lo
        // state osservato non e' piu' attribuibile al nuovo videoId
        consecutiveTimeReached = 0
        stateForVideoId = null
        lastVideoId = id
        dispatchLoad(id, startSeconds, cue = false)
        // kick autoplay a 3 s (il video caricato su pagina attiva
        // parte da solo SOLO se la policy lo consente: l'esperienza dice di
        // no) + watchdog a 12 s come rete di sicurezza.
        scheduleAutoplayKick(id)
        armWatchdog(id)
    }

    override fun cueVideoById(videoId: String, startSeconds: Float) {

        val id = extractYouTubeVideoId(videoId) ?: run {
            KcefEngine.logKcef(
                "cueVideoById IGNORATO: '$videoId' non contiene un videoId YouTube valido (11 caratteri)"
            )
            return
        }
        pendingStart = startSeconds
        // flag SEMPRE aggiornato: un eventuale ricrea-browser
        // (watchdog) dovra' ricueare, non caricare
        pendingIsCue = true
        resetYtError()
        KcefEngine.logKcef("cueVideoById: videoId='$id' (richiesto: '$videoId') start=$startSeconds")
        // stesso azzeramento di loadVideoById (dedup/attribuzione)
        endedFiredForVideo = null
        consecutiveTimeReached = 0
        stateForVideoId = null
        lastVideoId = id
        dispatchLoad(id, startSeconds, cue = true)
        // anche il cue e' vigilato, ma in modalita' CUE-SAFE:
        // "caricato ma non avviato" e' lo stato ATTESO (il brano aspetta il
        // play dell'utente): il watchdog recupera solo se
        // il cue non ha prodotto niente (niente durata = metadati mancati).
        armWatchdog(id, 0, allowPlayRecovery = false)
    }

    /**
     * Instradamento del videoId verso la pagina.
     *  - browser assente oppure pagina non ancora ATTIVA (non ancora
     *    caricata/confermata): il videoId resta in attesa e viene consumato
     *    dalla SOSTITUZIONE nell'HTML (se la pagina non e' ancora stata
     *    servita) oppure dall'invio automatico alla conferma
     *    ([PlayerHtmlServer.onPageConfirmed]);
     *  - pagina attiva: invio diretto via riplayLoad/riplayCue.
     */
    private fun dispatchLoad(id: String, startSeconds: Float, cue: Boolean) {
        val b = browser
        if (b == null || !PlayerHtmlServer.pageActive) {
            pendingVideoId = id
            pendingIsCue = cue
            // log esplicito dell'attesa
            KcefEngine.logKcef(
                "load in attesa: pagina non ancora attiva (browser=${b != null} " +
                        "pageActive=${PlayerHtmlServer.pageActive}): videoId='$id' sara' inviato " +
                        "alla conferma della pagina o cotto nell'HTML"
            )
            return
        }
        if (cue) evaluate("window.riplayCue && riplayCue('$id', $startSeconds)")
        else evaluate("window.riplayLoad && riplayLoad('$id', $startSeconds)")
    }

    /** azzera l'errore corrente quando parte un nuovo video. */
    private fun resetYtError() {
        lastLoggedYtErr = 0
        _ytError.value = 0
    }

    override fun getCurrentPositionMs(): Long = _state.value.timestamp

    override fun getDurationMs(): Long = _state.value.duration

    override fun isPlaying(): Boolean = _state.value.isPlaying

    override fun getVolume(): Float = _state.value.volume

    override fun setPlaybackRate(rate: Float) =
        evaluate("window.riplaySetRate && riplaySetRate($rate)")

    // ---- Interni ----

    private fun evaluate(code: String) {
        val b = browser ?: return
        scope.launch {
            // v46 (jcefmaven): API org.cef standard (KCEF wrappava il codice con
            // la gestione errori JS: gli errori ora arrivano come console.error
            // e vengono instradati dal display handler di KcefEngine).
            runCatching { b.executeJavaScript(code, "", 0) }
        }
    }

    /**
     * v46 (jcefmaven): il poll non usa piu' evaluateJavaScriptBlocking di
     * KCEFBrowser (API KCEF). Il bridge standard CEF (CefMessageRouter,
     * installato sul client da KcefEngine.installStateBridge) inietta
     * window.cefQuery in ogni frame: il loop qui sotto CHIEDE alla pagina di
     * PUSHARE lo stato verso Kotlin, e la pagina lo invia con cefQuery
     * ("st:{json}"). Lo stato arriva in [handleStatePush] (callback di
     * KcefEngine.onPlayerStateMessage, thread UI CEF).
     */
    private val PUSH_STATE_JS =
        "(function(){try{var s=window.riplayState&&window.riplayState();" +
                "if(s!=null&&window.cefQuery){window.cefQuery({request:'st:'+JSON.stringify(s)," +
                "persistent:false,onSuccess:function(){},onFailure:function(){}});}}" +
                "catch(e){console.error('[RiPlay-YT] push stato fallito: '+e)}})()"

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(250L)
                val b = browser ?: continue
                runCatching { b.executeJavaScript(PUSH_STATE_JS, "", 0) }
            }
        }
        // PERCORSO 2 - watchdog a finestra nascosta (stesso ciclo
        // vitale del poll: parte/riparte con lui, muore col dispose)
        startHiddenEndWatch()
    }

    /**
     * Riceve lo stato pushato dalla pagina (request "st:{json}") via
     * CefMessageRouter: STESSO contenuto che prima tornava dal poll
     * evaluateJavaScriptBlocking di KCEF (stessa logica di update/errori/
     * fine brano naturale).
     */
    private fun handleStatePush(request: String) {
        if (!request.startsWith("st:")) return
        val cleaned = cleanResult(request.substring(3)) ?: return
        val s = runCatching { json.decodeFromString<KcefJsState>(cleaned) }.getOrNull() ?: return
        // STEP 2i: nuovo codice errore del player YT -> log con significato +
        // esposizione alla UI (una sola volta per codice).
        if (s.err != 0 && s.err != lastLoggedYtErr) {
            lastLoggedYtErr = s.err
            _ytError.value = s.err
            KcefEngine.logKcef(
                "YouTube player ERROR code=${s.err} (${KcefEngine.ytErrorMeaning(s.err)})"
            )
        }
        _state.value = PlayerState(
            isPlaying = s.s == 1,
            isMuted = s.m,
            volume = s.v / 100f,
            timestamp = (s.t * 1000).toLong(),
            duration = (s.d * 1000).toLong()
        )

        stateForVideoId = lastVideoId
        lastPollOkAt = System.currentTimeMillis()

        lastRawYtState = s.s
        if (s.s == 1) disarmWatchdog()
        // STEP 3i: FINE BRANO NATURALE (stato YT 0 = ENDED): una sola volta
        // per video (lo stato puo' restare 0 per minuti).
        if (s.s == 0) {
            fireNaturalEnd("yt-state-0")
        }
        // PERCORSO 1 - TEMPO RAGGIUNTO (poll vivo, stato page frozen a
        // finestra nascosta): lastState e' la cache di riplayState, ma
        // getCurrentTime() e' il clock VIVO del media element: se il brano
        // "in play" raggiunge la durata per DUE push consecutivi, e' finito
        // davvero anche se lo stato resta 1. Con la finestra visibile lo
        // stato 0 arriva al medesimo istante: gara risolta dal dedup.
        if (s.s == 1 && s.t > 0f && s.d > 0f && s.t >= s.d - 0.20f) {
            consecutiveTimeReached++
            if (consecutiveTimeReached >= 2) fireNaturalEnd("tempo-raggiunto")
        } else {
            consecutiveTimeReached = 0
        }
    }

    /**
     * PERCORSO 2 - WATCHDOG "FINE BRANO A FINESTRA NASCOSTA".
     * Caso peggiore: a finestra nascosta nel tray DEGENERA anche
     * evaluateJavaScriptBlocking (poll morto: lastPollOkAt resta indietro)
     * e NESSUN segnale arriva piu' dalla pagina. Se l'ultimo stato noto e'
     * "in play" con durata nota, la posizione e' STIMATA col tempo reale
     * trascorso dall'ultimo poll (il clock dell'audio prosegue anche con la
     * pagina degradata: e' il sintomo osservato): raggiunta la fine
     * nominale si emette la fine naturale lo stesso, la coda avanza e il
     * brano successivo viene caricato (se anche l'invio e' perso, il
     * watchdog ricrea il browser con il videoId inserito nell'HTML).
     * Guard: lo staoe osservato deve appartenere al video CORRENTE
     * (stateForVideoId) e il poll deve essere fermo da >3 s (con la pagina
     * viva si impegnano i percorsi normali, senza stime).
     */
    private fun startHiddenEndWatch() {
        hiddenEndJob?.cancel()
        hiddenEndJob = scope.launch {
            while (isActive) {
                delay(1_000L)
                val current = lastVideoId
                if (current == null || current.isBlank()) continue
                if (stateForVideoId != current) continue
                val st = _state.value
                if (!st.isPlaying || st.duration <= 0L || st.timestamp <= 0L) continue
                val now = System.currentTimeMillis()
                if (now - lastPollOkAt < 3_000L) continue
                val estimated = st.timestamp + (now - lastPollOkAt)
                if (estimated >= st.duration - 350L) {
                    fireNaturalEnd("watchdog-finestra-nascosta")
                }
            }
        }
    }

    /**
     * Il risultato dell'evaluate arriva come valore JS stringa: normalizza
     * virgolette/escape eventuali prima del parse JSON.
     */
    private fun cleanResult(raw: String?): String? {
        var s = raw?.trim() ?: return null
        if (s.isEmpty() || s == "null" || s == "undefined") return null
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return s.takeIf { it.isNotEmpty() && it != "null" }
    }

    companion object {
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    }
}

/**
 * v46 (jcefmaven): CefBrowser.sendMouseEvent era PUBLIC nell'org.cef del jar
 * KCEF (fork JetBrains); nell'org.cef di jcef-api (upstream java-cef) e'
 * PROTECTED FINAL su CefBrowser_N. L'iniezione di USER ACTIVATION passa da
 * reflection: stesso comportamento a runtime (il metodo esiste ed e' quello
 * nativo), nessuna API aggiuntiva.
 */
private fun CefBrowser.sendBrowserMouseEventCompat(e: java.awt.event.MouseEvent) {
    var cls: Class<*>? = javaClass
    while (cls != null) {
        val m = runCatching {
            cls.getDeclaredMethod("sendMouseEvent", java.awt.event.MouseEvent::class.java)
        }.getOrNull()
        if (m != null) {
            runCatching {
                m.isAccessible = true
                m.invoke(this, e)
            }.onFailure {
                KcefEngine.logKcef(
                    "activation: sendMouseEvent invocazione fallita: ${it.javaClass.simpleName}: ${it.message}"
                )
            }
            return
        }
        cls = cls.superclass
    }
    KcefEngine.logKcef(
        "activation: sendMouseEvent non trovato nell'org.cef corrente (attivazione utente saltata)"
    )
}

/**
 * v46: replica di KCEFBrowser.dispose() (stopLoad + setCloseAllowed +
 * close(true)) con le API org.cef standard (dispose() non esiste su
 * CefBrowser in jcef-api).
 */
private fun CefBrowser.closeBrowserCompat() {
    runCatching { stopLoad() }
    runCatching { setCloseAllowed() }
    runCatching { close(true) }
}
