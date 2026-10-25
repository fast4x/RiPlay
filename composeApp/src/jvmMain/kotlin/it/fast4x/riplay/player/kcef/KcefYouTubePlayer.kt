package it.fast4x.riplay.player.kcef

import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
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
import org.cef.browser.CefRendering
import player.PlayerController
import player.PlayerState

@Serializable
private data class KcefJsState(
    val s: Int = -99,
    val t: Float = 0f,
    val d: Float = 0f,
    val v: Int = 100,
    val m: Boolean = false,
    // STEP 2i: ultimo codice onError del player YT (0 = nessun errore)
    val err: Int = 0
)

/**
 * Motore YouTube desktop: browser CEF (KCEF) che carica la pagina
 * IFrame Player API ([PlayerHtmlServer]) e la pilota via JavaScript.
 *
 * STEP 2r: replica del meccanismo Android (WebViewYouTubePlayer.initWebView,
 * fork AYP) con contesto embed ACCETTATO da YouTube: l'HTML del player e'
 * generato QUI con i parametri interni SOSTITUITI PRIMA del caricamento
 * (<<injectedVideoId>> con il videoId in attesa, <<injectedPlayerVars>> con
 * i playerVars autolock AYP e origin dinamico) e viene SERVITO dal server
 * loopback LOCALE caricandolo come http://localhost:PORT/player.html (il
 * NOME localhost e' accettato dalla politica embed di YouTube: l'IP
 * 127.0.0.1 e www.youtube.com producono errore 152 su tutti i video,
 * verifica empirica step2r). Sopra le richieste /embed/ KcefEngine impone
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
     * STEP 2i: ultimo codice errore segnalato dal player YouTube (0 = nessuno).
     * Esposto alla UI ([KcefVideoSurface] mostra codice + significato) e al log
     * con il significato: e' la chiave per diagnosticare "An error occurred".
     */
    private val _ytError = MutableStateFlow(0)
    val ytError: StateFlow<Int> = _ytError.asStateFlow()

    @Volatile private var lastLoggedYtErr = 0

    @Volatile private var browser: KCEFBrowser? = null
    // STEP 3d: pannello ospite Swing (browser SOTTO + cover/segnaposto SOPRA).
    // Nasconderlo non serve piu' per la cover (vedi KcefPlayerHostPanel); viene
    // nascosto SOLO quando l'overlay espanso e' aperto, con l'audio gia' avviato.
    @Volatile private var hostPanel: KcefPlayerHostPanel? = null
    @Volatile private var pendingVideoId: String? = null
    @Volatile private var pendingStart: Float = 0f
    @Volatile private var pendingIsCue: Boolean = false
    @Volatile private var muted = false
    private var pollJob: Job? = null

    // STEP 3e (dal test v22: "il tasto play non ha alcun effetto"): robustezza
    // della catena di caricamento. Questa app ha una STORIA documentata di
    // chiamate JS perse (commento STEP 2g di KcefEngine) e le versioni 3c/3d
    // hanno manipolato finestre native attorno al browser (hide AWT, canvas di
    // copertura): qualunque perdita silenziosa (riplayLoad non consegnato,
    // pagina persa/crash del renderer, client assente alla factory del
    // SwingPanel) lasciava il player MUTO e il tasto play inerte, senza
    // NESSUN errore visibile. Da qui:
    //  - lastVideoId: l'ultimo id richiesto, base di ogni recupero;
    //  - watchdog: se il video non parte entro 12 s (niente play, niente
    //    durata, niente errore) re-invia riplayLoad (pagina viva) oppure
    //    RICREA il browser ri-cuocendo il videoId nell'HTML (pagina morta:
    //    e' il meccanismo autoplay step2r, il piu' affidabile validato);
    //  - play(): se non c'e' nessun video attivo (durata 0) ARMA il recupero
    //    invece di restare muto: il tasto play ha SEMPRE un effetto.
    @Volatile private var lastVideoId: String? = null
    @Volatile private var lastPollOkAt: Long = 0L
    private var watchdogJob: Job? = null
    @Volatile private var recreatingBrowser = false
    private val browserLock = Any()

    // STEP 3f (dal test v23: log utente "stato: 3 -> -1"): AUTOPOLICY YT. Il
    // log del test mostra la sequenza -1 -> 3 (buffering) -> -1 (unstarted):
    // Chromium ANNULLA la riproduzione avviata senza gesto utente (autoplay
    // policy / user activation). Prima di v21 il browser era esposto e
    // l'utente cliccava il play DI YOUTUBE dentro il browser (gesto REALE);
    // con la cover Swing sopra il browser quel gesto non esiste piu' e le
    // chiamate JS da Kotlin (evaluateJavaScript) NON sono un gesto: YouTube
    // le ignora. Il flag --autoplay-policy=no-user-gesture-required, previsto
    // da step2i, NON basta (verifica empirica: la riproduzione non e' MAI
    // partita da loadVideoById in nessuna versione, sempre col click manuale
    // nel browser). Contromisure (a strati, indipendenti dal flag):
    //  - lastRawYtState: l'ultimo stato YT GREZZO (-1/0/1/2/3/5): serve a non
    //    mandare il click sintetico quando il video STA gia' suonando o sta
    //    bufferizzando (un click lo metterebbe in pausa);
    //  - injectUserActivation: CLICK REALE via sendMouseEvent (input nativo
    //    CEF, identico all'input hardware): genera USER ACTIVATION sticky
    //    sull'iframe YouTube e, sul video unstarted, e' lui stesso il
    //    toggle-play: e' lo STESSO gesto che prima faceva partire il video;
    //  - autoplay kick (3 s dal load): se il video non e' partito da solo,
    //    click + riplayPlay PRIMA del watchdog (12 s);
    //  - watchdog 3f: riconosce l'AUTOPLAY BLOCCATO (durata nota MA stato
    //    unstarted: i metadati arrivano col buffering anche quando poi la
    //    partenza viene annullata) e ci risponde con activation+play invece
    //    di uscire (bug v23: usciva su duration>0 e non interveniva MAI).
    @Volatile private var lastRawYtState: Int = -99
    private var kickJob: Job? = null

    /**
     * STEP 3i: callback di FINE BRANO naturale (stato YT 0 = ENDED). E' il
     * gancio dell'AVANZAMENTO AUTOMATICO della coda di riproduzione: quando
     * il video finisce da solo (NON con stop/pausa/nuovo load) il player
     * notifica l'app, che avvia il brano successivo della coda.
     * Gara e duplicati gestiti con endedFiredForVideo: un solo evento per
     * videoId, azzerato al load successivo.
     */
    var onEnded: (() -> Unit)? = null
    @Volatile private var endedFiredForVideo: String? = null

    /**
     * Crea (una volta sola) il browser CEF e restituisce il componente
     * AWT da incorporare. Chiamare dal thread EDT (SwingPanel factory).
     *
     * STEP 2r: il browser parte DIRETTAMENTE sulla pagina player del
     * server locale raggiunta come http://localhost ([PlayerHtmlServer.
     * ensureLocalServer], origine onesta accettata dalla politica embed
     * YouTube); il provider fornisce l'HTML del player con i parametri
     * interni sostituiti (meccanismo Android).
     *
     * STEP 3d: restituisce il PANNELLO OSPITE (browser SOTTO + cover/
     * segnaposto SOPRA, vedi KcefPlayerHostPanel) e non piu' il browser
     * nudo. Finche' il client non e' pronto il pannello mostra solo lo
     * sfondo NERO (prima tornava un JPanel bianco).
     *
     * STEP 3e: NON cachea piu' uno stato "vuoto". La 3d creava il pannello
     * e, se il client KCEF non era ancora pronto, lo restituiva SENZA
     * browser e lo CACHEDAVA: ogni chiamata successiva riusava il pannello
     * vuoto PER SEMPRE (player muto, play inerte, nessun errore: esattamente
     * il sintomo visto in prova). Ora la creazione del browser e' separata
     * ([ensureBrowser]) e RIPETIBILE: la factory, il watchdog e il recupero
     * del play vi convergono e un client arrivato tardi viene colto al volo.
     */
    fun attachBrowser(): java.awt.Component {
        val host = hostPanel ?: KcefPlayerHostPanel().also { hostPanel = it }
        ensureBrowser()
        return host
    }

    /**
     * STEP 3e: crea il browser SE MANCA (idempotente, thread-safe). Puo'
     * essere richiamato piu' volte: dalla factory del SwingPanel, dal
     * watchdog e dal recupero del play. Se il client non e' ancora pronto
     * esce SENZA mettere nulla in cache: il watchdog riprovera'.
     */
    private fun ensureBrowser() {
        synchronized(browserLock) {
            if (browser != null) return
            val client: KCEFClient = KcefEngine.clientOrNull() ?: run {
                KcefEngine.logKcef(
                    "attachBrowser: client KCEF non ancora pronto (il watchdog riprovera')"
                )
                return
            }
            PlayerHtmlServer.resetPageSession()
            PlayerHtmlServer.pageHtmlProvider = { buildPlayerPageHtml() }
            PlayerHtmlServer.onPageConfirmed = { confirmPending() }
            val playerUrl = PlayerHtmlServer.ensureLocalServer()
            KcefEngine.logKcef("attachBrowser: pagina player su $playerUrl (origine onesta localhost)")
            val b: KCEFBrowser = client.createBrowser(
                playerUrl,
                CefRendering.DEFAULT,
                false
            )
            browser = b
            hostPanel?.setBrowser(b.uiComponent)
            // createImmediately crea finestre native: DEVE girare sull'EDT
            if (java.awt.EventQueue.isDispatchThread()) b.createImmediately()
            else java.awt.EventQueue.invokeAndWait { runCatching { b.createImmediately() } }
            startPolling()
            // NB: nessuna evaluate qui: il videoId in attesa viene SOSTITUITO
            // dentro l'HTML della pagina (meccanismo Android <<injectedVideoId>>)
            // oppure inviato alla conferma della pagina (onPageConfirmed).
        }
    }

    /**
     * STEP 3e/3f: invio del videoId in attesa alla conferma della pagina,
     * seguito dal kick autoplay (il video caricato in questo momento e'
     * soggetto alla policy: se non parte da solo, dopo 3 s arriva il click
     * di attivazione). NB: la pagina e' APPENA attiva: l'iframe YT nasce
     * quando l'IFrame API e' arrivata: il kick gestisce entrambe le corsa.
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
     * STEP 3f: KICK AUTOPLAY. Un loadVideoById su pagina attiva parte da
     * solo SOLO se la policy lo consente (flag efficace + activation):
     * l'esperienza dimostra che non e' così (stato -1 -> 3 -> -1 nel log:
     * buffering annullato). Dopo 3 s dal load, se il video non e' NE' in
     * play NE' in buffering NE' in errore, arrivano il CLICK di attivazione
     * (user activation reale sull'iframe) + riplayPlay. Un solo kick per
     * load: il watchdog resta la rete di sicurezza a 12 s.
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
     * STEP 3f: iniezione di USER ACTIVATION nel browser. YouTube accetta
     * playVideo() (e l'autoplay) solo se la pagina/iframe ha avuto un GESTO
     * UTENTE reale (user activation policy di Chromium; il flag
     * no-user-gesture-required previsto da step2i si e' rivelato
     * INEFFICACE: la riproduzione non e' mai partita da JS, vedi v23).
     * Il click e' inviato come INPUT NATIVO CEF (sendMouseEvent, identico
     * all'input hardware: genera activation TRUSTED), NON come evento AWT
     * sintetico sul canvas (in rendering windowed gli eventi AWT non
     * attraversano: la finestra nativa del browser riceve l'input da CEF).
     * Coordinate: angolo alto-sinistra (2,2) del canvas = dentro l'IFRAME
     * YT, area non interattiva (il grande pulsante play e' al centro):
     * YouTube lo interpreta come toggle play/pause: su un video unstarted
     * E' LUI STESSO ad avviare la riproduzione (il gesto che prima faceva
     * partire il video col browser esposto). Guard: niente click se il
     * video STA gia' suonando (1) o sta bufferizzando (3): un click lo
     * metterebbe in pausa.
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
                b.sendMouseEvent(moved)
                b.sendMouseEvent(pressed)
                b.sendMouseEvent(released)
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
     * STEP 3e: ricostruzione del browser (STESSO pannello ospite) quando la
     * pagina risulta morta o mai attiva: il videoId viene ri-cotto nell'HTML
     * (meccanismo autoplay step2r, il piu' affidabile validato) e la pagina
     * ricomincia da zero. EDT-safe: le operazioni native girano sull'EDT.
     */
    private fun recreateBrowser(reason: String, videoId: String, attempt: Int) {
        if (recreatingBrowser) {
            // rientro (es. un secondo watchdog armato dal play): riprova piu'
            // tardi invece di far morire la catena in silenzio
            armWatchdog(videoId, attempt + 1)
            return
        }
        recreatingBrowser = true
        try {
            synchronized(browserLock) {
                if (hostPanel == null) return
                val client = KcefEngine.clientOrNull()
                if (client == null) {
                    KcefEngine.logKcef("ricrea browser: client KCEF assente, riprovero' al prossimo tentativo")
                    armWatchdog(videoId, attempt + 1)
                    return
                }
                KcefEngine.logKcef("ricreo il browser del player: $reason")
                val old = browser
                browser = null
                pollJob?.cancel()
                pollJob = null
                pendingVideoId = videoId
                pendingIsCue = false
                java.awt.EventQueue.invokeAndWait {
                    runCatching { old?.dispose() }
                    PlayerHtmlServer.resetPageSession()
                    PlayerHtmlServer.pageHtmlProvider = { buildPlayerPageHtml() }
                    PlayerHtmlServer.onPageConfirmed = { confirmPending() }
                    runCatching {
                        val playerUrl = PlayerHtmlServer.ensureLocalServer()
                        val b = client.createBrowser(playerUrl, CefRendering.DEFAULT, false)
                        browser = b
                        b.createImmediately()
                        hostPanel?.setBrowser(b.uiComponent)
                    }.onFailure {
                        KcefEngine.logKcef(
                            "ricrea browser: creazione fallita: ${it.javaClass.simpleName}: ${it.message}"
                        )
                    }
                }
                startPolling()
            }
            armWatchdog(videoId, attempt + 1)
        } catch (t: Throwable) {
            KcefEngine.logKcef("ricrea browser FALLITA: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            recreatingBrowser = false
        }
    }

    /**
     * STEP 3e: vigilanza sul caricamento. Se entro 12 s il video non e' ne'
     * in riproduzione, ne' caricato (durata nota), ne' in errore, la catena
     * si e' persa da qualche parte: re-invio del load (pagina viva) oppure
     * RICREAZIONE del browser con videoId ri-cotto nell'HTML (pagina morta).
     * Massimo 3 tentativi, ognuno con log puntuale dello stato della catena.
     */
    private fun armWatchdog(videoId: String, attempt: Int = 0) {
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
                recreateBrowser("pagina player non risponde o assente", videoId, attempt)
            } else if (st.duration > 0L || raw == 5 || raw == 1) {
                // STEP 3f (bug v23): durata > 0 MA stato unstarted = AUTOPLAY
                // BLOCCATO (i metadati arrivano col buffering anche quando
                // Chromium annulla poi la partenza: stato 3 -> -1 nel log del
                // test v23, con il watchdog che usciva su duration>0 senza
                // intervenire). Il video e' GIA' caricato: niente ri-load che
                // lo farebbe ripartire da zero: CLICK di attivazione + play.
                KcefEngine.logKcef(
                    "watchdog: video caricato ma NON partito (autoplay bloccato): " +
                            "click di attivazione + play"
                )
                injectUserActivation("watchdog autoplay bloccato $videoId")
                scope.launch {
                    delay(250L)
                    evaluate("window.riplayPlay && riplayPlay()")
                }
                armWatchdog(videoId, attempt + 1)
            } else {
                // il load non ha mai prodotto nulla: re-invio con activation
                KcefEngine.logKcef("watchdog: pagina viva, activation + re-invio riplayLoad('$videoId')")
                injectUserActivation("watchdog re-invio $videoId")
                evaluate("window.riplayLoad && riplayLoad('$videoId', ${pendingStart})")
                armWatchdog(videoId, attempt + 1)
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
            pendingVideoId = null
            pendingIsCue = false
            KcefEngine.logKcef("pagina player: videoId='$id' sostituito nell'HTML (meccanismo Android)")
        }
        return PlayerHtmlServer.playerPageHtml(if (id != null) "'$id'" else null)
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
            // STEP 3f: activation PRIMA del play: YouTube accetta playVideo
            // solo con un gesto reale (user activation). Il click arriva
            // (EDT), poi il play: su un video unstarted il click da solo e'
            // gia' il toggle-play di YT; riplayPlay e' la conferma.
            KcefEngine.logKcef("play: activation (stato YT=$raw) poi riplayPlay")
            injectUserActivation("play richiesto dall'utente")
            scope.launch {
                delay(250L)
                evaluate("window.riplayPlay && riplayPlay()")
            }
        } else {
            evaluate("window.riplayPlay && riplayPlay()")
        }
        // STEP 3e/3f: il tasto play deve SEMPRE avere un effetto. Se non c'e'
        // nessun video attivo (mai caricato, non in pausa/buffering, nessun
        // errore) il click arma il recupero: il watchdog re-invia il load o
        // ricrea il browser, e il brano parte anche quando il load originale
        // si e' perso.
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
        val b = browser
        browser = null
        // STEP 3d: il pannello ospite muore con la composizione (al ritorno
        // su KCEF attachBrowser lo ricrea insieme al browser)
        val host = hostPanel
        hostPanel = null
        runCatching { host?.removeAll() }
        pendingVideoId = null
        lastVideoId = null
        PlayerHtmlServer.onPageConfirmed = null
        PlayerHtmlServer.pageHtmlProvider = null
        runCatching { b?.dispose() }
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
        // STEP 2k: sanitizza l'id con YouTubeUtils: accetta l'id nudo o lo
        // estrae da un URL. Un id sporco/assente fa rispondere YouTube con
        // l'errore code=2 "parametro non valido": qui lo blocchiamo PRIMA,
        // con log chiaro del valore rifiutato.
        val id = extractYouTubeVideoId(videoId) ?: run {
            KcefEngine.logKcef(
                "loadVideoById IGNORATO: '$videoId' non contiene un videoId YouTube valido (11 caratteri)"
            )
            return
        }
        pendingStart = startSeconds
        muted = false
        resetYtError()
        // STEP 2i/2k: log dell'id ESATTO che arriva al player (differenziato
        // dal valore richiesto, per diagnosticare id corrotti o URL interi)
        KcefEngine.logKcef("loadVideoById: videoId='$id' (richiesto: '$videoId') start=$startSeconds")
        // reset tempo/durata: la nuova traccia parte da zero
        _state.value = _state.value.copy(isPlaying = false, timestamp = 0L, duration = 0L)
        // STEP 3i: il nuovo video puo' di nuovo notificare la fine naturale
        endedFiredForVideo = null
        lastVideoId = id
        dispatchLoad(id, startSeconds, cue = false)
        // STEP 3f: kick autoplay a 3 s (il video caricato su pagina attiva
        // parte da solo SOLO se la policy lo consente: l'esperienza dice di
        // no) + watchdog a 12 s come rete di sicurezza.
        scheduleAutoplayKick(id)
        armWatchdog(id)
    }

    override fun cueVideoById(videoId: String, startSeconds: Float) {
        // STEP 2k: stessa sanitizzazione di loadVideoById
        val id = extractYouTubeVideoId(videoId) ?: run {
            KcefEngine.logKcef(
                "cueVideoById IGNORATO: '$videoId' non contiene un videoId YouTube valido (11 caratteri)"
            )
            return
        }
        pendingStart = startSeconds
        resetYtError()
        KcefEngine.logKcef("cueVideoById: videoId='$id' (richiesto: '$videoId') start=$startSeconds")
        lastVideoId = id
        dispatchLoad(id, startSeconds, cue = true)
        // STEP 3e: anche il cue e' vigilato (la durata arriva con i metadati
        // e disarmia il watchdog: un cue riuscito non viene toccato)
        armWatchdog(id)
    }

    /**
     * STEP 2r: instradamento del videoId verso la pagina.
     *  - browser assente oppure pagina non ancora ATTIVA (non ancora
     *    caricata/confermata): il videoId resta in attesa e viene consumato
     *    dalla SOSTITUZIONE nell'HTML (se la pagina non e' ancora stata
     *    servita) oppure dall'invio automatico alla conferma
     *    ([PlayerHtmlServer.onPageConfirmed]);
     *  - pagina attiva: invio diretto via riplayLoad/riplayCue.
     * Niente invii ottimistici che si perdono (prima del fix un riplayLoad
     * valutato su una pagina non pronta veniva silenziosamente scartato).
     */
    private fun dispatchLoad(id: String, startSeconds: Float, cue: Boolean) {
        val b = browser
        if (b == null || !PlayerHtmlServer.pageActive) {
            pendingVideoId = id
            pendingIsCue = cue
            // STEP 3e: log esplicito dell'attesa (prima era silenziosa: nel
            // log non si capiva se il load era stato inviato o no)
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

    /** STEP 2i: azzera l'errore YT corrente quando parte un nuovo video. */
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
            runCatching { b.evaluateJavaScript(code) }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(250L)
                val b = browser ?: continue
                val raw = runCatching {
                    b.evaluateJavaScriptBlocking("window.riplayState ? window.riplayState() : null")
                }.getOrNull()
                val cleaned = cleanResult(raw) ?: continue
                val s = runCatching { json.decodeFromString<KcefJsState>(cleaned) }
                    .getOrNull() ?: continue
                // STEP 2i: nuovo codice errore del player YT -> log con
                // significato + esposizione alla UI (una sola volta per codice).
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
                // STEP 3e: la pagina risponde (polling OK) + il play effettivo
                // disarma il watchdog (il recupero serve solo quando NULLA parte)
                lastPollOkAt = System.currentTimeMillis()
                // STEP 3f: stato YT GREZZO per i guard dell'activation
                lastRawYtState = s.s
                if (s.s == 1) disarmWatchdog()
                // STEP 3i: FINE BRANO NATURALE (stato YT 0 = ENDED): una sola
                // volta per video (lo stato puo' restare 0 per minuti). Un
                // videoId in riproduzione e' richiesto: senza, e' il player
                // vuoto a pagina appena pronta (non e' una fine reale).
                if (s.s == 0) {
                    val current = lastVideoId
                    if (current != null && current.isNotBlank() &&
                        endedFiredForVideo != current
                    ) {
                        endedFiredForVideo = current
                        KcefEngine.logKcef("fine brano naturale (YT state=0): videoId='$current' -> avanza la coda")
                        onEnded?.invoke()
                    }
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
