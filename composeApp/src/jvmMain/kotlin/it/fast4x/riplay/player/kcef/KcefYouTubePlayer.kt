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
import javax.swing.JPanel

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
    @Volatile private var pendingVideoId: String? = null
    @Volatile private var pendingStart: Float = 0f
    @Volatile private var pendingIsCue: Boolean = false
    @Volatile private var muted = false
    private var pollJob: Job? = null

    /**
     * Crea (una volta sola) il browser CEF e restituisce il componente
     * AWT da incorporare. Chiamare dal thread EDT (SwingPanel factory).
     *
     * STEP 2r: il browser parte DIRETTAMENTE sulla pagina player del
     * server locale raggiunta come http://localhost ([PlayerHtmlServer.
     * ensureLocalServer], origine onesta accettata dalla politica embed
     * YouTube); il provider fornisce l'HTML del player con i parametri
     * interni sostituiti (meccanismo Android).
     */
    fun attachBrowser(): java.awt.Component {
        browser?.let { return it.uiComponent }
        val client: KCEFClient = KcefEngine.clientOrNull() ?: return JPanel()
        PlayerHtmlServer.resetPageSession()
        PlayerHtmlServer.pageHtmlProvider = { buildPlayerPageHtml() }
        PlayerHtmlServer.onPageConfirmed = {
            val id = pendingVideoId
            if (id != null) {
                pendingVideoId = null
                val start = pendingStart
                if (pendingIsCue) evaluate("window.riplayCue && riplayCue('$id', $start)")
                else evaluate("window.riplayLoad && riplayLoad('$id', $start)")
            }
        }
        val playerUrl = PlayerHtmlServer.ensureLocalServer()
        KcefEngine.logKcef("attachBrowser: pagina player su $playerUrl (origine onesta localhost)")
        val b: KCEFBrowser = client.createBrowser(
            playerUrl,
            CefRendering.DEFAULT,
            false
        )
        browser = b
        b.createImmediately()
        startPolling()
        // NB: nessuna evaluate qui: il videoId in attesa viene SOSTITUITO
        // dentro l'HTML della pagina (meccanismo Android <<injectedVideoId>>)
        // oppure inviato alla conferma della pagina (onPageConfirmed).
        return b.uiComponent
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

    override fun play() = evaluate("window.riplayPlay && riplayPlay()")

    override fun pause() = evaluate("window.riplayPause && riplayPause()")

    override fun stop() = evaluate("window.riplayStop && riplayStop()")

    override fun dispose() {
        pollJob?.cancel()
        pollJob = null
        val b = browser
        browser = null
        pendingVideoId = null
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
        dispatchLoad(id, startSeconds, cue = false)
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
        dispatchLoad(id, startSeconds, cue = true)
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
