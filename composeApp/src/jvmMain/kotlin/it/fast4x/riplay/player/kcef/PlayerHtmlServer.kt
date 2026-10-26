package it.fast4x.riplay.player.kcef

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import org.cef.browser.CefBrowser

/**
 * Pagina del player YouTube IFrame API - REPLICA DEL MECCANISMO ANDROID
 * con contesto embed ACCETTATO da YouTube (verifica empirica ottobre 2026).
 *
 * STEP 2r (dal test v17 utente: errore YT 152 su TUTTI i video anche con
 * history.replaceState sulla homepage): ESPERIMENTI DIRETTI sulla pagina
 * embed di YouTube (GetEmbeddedPlayer, previewPlayabilityStatus nel HTML)
 * cambiando SOLO il Referer della richiesta:
 *  - Referer ASSENTE                     -> ERROR "Errore di configurazione
 *                                           del video player" (153/152);
 *  - Referer https://www.youtube.com/    -> ERROR "Questo video non e'
 *                                           disponibile"  == IL CASO v16/v17
 *                                           (152 su TUTTI i video);
 *  - Referer http://127.0.0.1:PORT/      -> UNPLAYABLE "Questo video non e'
 *                                           disponibile"  == IL CASO step2k
 *                                           (code 150/152);
 *  - Referer https://youtube.com/ (APEX) -> OK          == IL CASO ANDROID
 *                                           (loadDataWithBaseURL apex);
 *  - Referer http://localhost:PORT/      -> OK (il NOME localhost e'
 *                                           accettato, l'IP letterale no);
 *  - Referer domini terzi normali        -> OK (embed normale da sito).
 * LA POLITICA E' STRING-BASED: YouTube decide dal valore del Referer.
 *
 * FIX: la pagina player gira su http://localhost:PORT (server loopback
 * LOCALE che serve l'HTML con i parametri interni SOSTITUITI PRIMA del
 * caricamento, meccanismo Android <<injectedVideoId>>/<<injectedPlayerVars>>):
 * il browser invia spontaneamente Referer "http://localhost:PORT/" che
 * YouTube ACCETTA (verificato con i parametri widget reali: origin/forgin
 * localhost -> OK). Nessun mascheramento, nessuna intercettazione, nessun
 * documento scritto a runtime: origine ONESTA che supera la validazione.
 *
 * CANALE ANDROID (parita' massima, attivo per le richieste embed):
 * KcefEngine.installEmbedRefererPolicy() impone sulle richieste
 * https://www.youtube.com/embed/... il Referer apex "https://youtube.com/"
 * (lo stesso che Android produce con loadDataWithBaseURL): combinazione
 * Referer apex + parametri widget localhost verificata ACCETTATA (OK).
 * getResourceHandler resta SEMPRE null: nessun CefResourceHandler custom,
 * quindi nessun rischio NoSuchMethodError open/read del glue nativo.
 *
 * Bridge JS (invariato, usato da [KcefYouTubePlayer]): riplayLoad,
 * riplayCue, riplayPlay, riplayPause, riplayStop, riplaySeekTo,
 * riplaySetVolume, riplaySetMuted, riplaySetRate, riplayState (polling
 * Kotlin ogni 250 ms). Diagnostica: righe console [RiPlay-YT] instradate
 * da KcefEngine sul log applicativo.
 *
 * ATTENZIONE: nel JS qui sotto NON usare template literal: la stringa e'
 * una raw string Kotlin e il "$" verrebbe interpolato dal compilatore.
 * Non usare sequenze slash-asterisco nei commenti Kotlin (i block comment
 * Kotlin si annidano: rischio "Unclosed comment").
 */
object PlayerHtmlServer {

    /**
     * Prefisso dell'URL della pagina player: il server locale e' raggiunto
     * col NOME "localhost" (NON con l'IP letterale 127.0.0.1: la politica
     * embed di YouTube rifiuta il Referer con IP di loopback, verificato
     * empiricamente; il nome localhost e' invece ACCETTATO).
     */
    internal const val LOCAL_URL_PREFIX = "http://localhost:"

    /** Marker console emesso dalla pagina player quando e' attiva. */
    internal const val PAGE_ACTIVE_MARKER = "pagina player avviata"

    /**
     * playerVars IDENTICI all'autolock IFramePlayerOptions del fork AYP
     * (ordine di inserimento Android compreso), con UNA differenza dovuta:
     * "origin" e' l'espressione JS dinamica location.origin (validata
     * step2k/2n: con un valore hardcoded i postMessage iframe-pagina
     * vengono scartati; il widget ufficiale la sovrascrive comunque con
     * l'origine reale della pagina, quindi dinamico e' l'unico coerente).
     * L'origin resta una variabile interna del player: vive DENTRO la
     * pagina e non viene mai appesa a nessuna URL del browser.
     */
    internal const val AYP_PLAYER_VARS_JSON =
        """{"autoplay":1,"mute":0,"origin":location.origin,"controls":0,"enablejsapi":1,"fs":0,"rel":0,"iv_load_policy":3,"cc_load_policy":0}"""

    /**
     * Provider dell'HTML del player, impostato da [KcefYouTubePlayer.attach
     * Browser] prima di creare il browser: restituisce la pagina con i
     * parametri interni GIA' SOSTITUITI (meccanismo Android), consumando
     * l'eventuale videoId in attesa. Chiamato a ogni richiesta del server
     * locale (l'HTML viene rigenerato per riflettere il videoId corrente).
     */
    @Volatile
    var pageHtmlProvider: (() -> String)? = null

    /**
     * Callback invocata quando la pagina player e' ATTIVA (load end della
     * pagina locale oppure marker console): [KcefYouTubePlayer] la usa per
     * inviare un eventuale videoId arrivato mentre la pagina non era
     * ancora pronta.
     */
    @Volatile
    var onPageConfirmed: (() -> Unit)? = null

    /** true quando la pagina player e' attiva (load end o marker console). */
    @Volatile
    internal var pageActive: Boolean = false

    /**
     * Azzera lo stato della sessione pagina: chiamato da attachBrowser
     * prima della creazione di un NUOVO browser.
     */
    fun resetPageSession() {
        synchronized(this) {
            pageActive = false
        }
    }

    /**
     * HTML del player con i parametri interni SOSTITUITI (meccanismo
     * identico ad Android WebViewYouTubePlayer.initWebView):
     * "<<injectedVideoId>>" -> 'videoId' quotato oppure undefined,
     * "<<injectedPlayerVars>>" -> playerVars autolock AYP (origin dinamico).
     */
    fun playerPageHtml(injectedVideoId: String?): String =
        PLAYER_HTML
            .replace("<<injectedVideoId>>", injectedVideoId ?: "undefined")
            .replace("<<injectedPlayerVars>>", AYP_PLAYER_VARS_JSON)

    // ------------------------------------------------------------------
    // Coordinamento della pagina (chiamato da KcefEngine sui callback CEF)
    // ------------------------------------------------------------------

    /**
     * Load end del main frame: per l'URL del server locale la pagina e'
     * attiva (ponte JS pronto, conferma e sblocco del videoId in attesa).
     * Qualunque altra URL e' inattesa e viene solo loggata (nessun
     * meccanismo di ripristino: il server locale e' l'UNICO canale).
     */
    fun onMainFrameLoadEnd(browser: CefBrowser, url: String?, httpStatusCode: Int) {
        if (url == null) return
        if (url.startsWith(LOCAL_URL_PREFIX)) {
            if (!pageActive) {
                pageActive = true
                KcefEngine.logKcef(
                    "pagina player ATTIVA: http://localhost (origine onesta accettata dalla " +
                            "politica embed YouTube), ponte JS pronto"
                )
                onPageConfirmed?.invoke()
            }
            return
        }
        KcefEngine.logKcef("load end main frame inatteso (pagina player non coinvolta): $url")
    }

    /**
     * Conferma SECONDARIA dell'attivazione: il marker console
     * ([PAGE_ACTIVE_MARKER]) arriva dal JS della pagina player. Se il load
     * end non ha ancora confermato, qui si sblocca l'invio del videoId.
     */
    fun tryMarkActiveFromConsole(consoleMessage: String) {
        if (pageActive) return
        if (!consoleMessage.contains(PAGE_ACTIVE_MARKER)) return
        pageActive = true
        KcefEngine.logKcef("pagina player ATTIVA (marker console): ponte JS pronto")
        onPageConfirmed?.invoke()
    }

    // ------------------------------------------------------------------
    // Server locale (solo loopback, porta effimera, raggiunto come localhost)
    // ------------------------------------------------------------------

    @Volatile
    private var server: HttpServer? = null

    @Volatile
    private var serverV6: HttpServer? = null

    /**
     * Avvia (se serve) il server e restituisce l'URL della pagina player
     * nella forma http://localhost:PORT/player.html. Il binding e' sul
     * loopback IPv4; se disponibile viene aggiunto ANCHE il loopback IPv6
     * sulla STESSA porta (localhost puo' risolvere a ::1: Chromium prova
     * entrambi gli indirizzi, il doppio binding elimina ogni attesa).
     * Serve l'HTML del player GIA' SOSTITUITO dal provider: nessuna cache.
     */
    fun ensureLocalServer(): String = synchronized(this) {
        val s = server ?: HttpServer.create(
            InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            0
        ).also { srv ->
            srv.createContext("/player.html") { exchange ->
                try {
                    val html = pageHtmlProvider?.invoke() ?: playerPageHtml(null)
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                    exchange.responseHeaders.add("Cache-Control", "no-store")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } catch (_: Exception) {
                    // client disconnesso a meta' risposta: ignorato
                } finally {
                    runCatching { exchange.close() }
                }
            }
            srv.executor = null
            srv.start()
            server = srv
        }
        // Secondo binding IPv6 sulla stessa porta (best effort)
        if (serverV6 == null) {
            runCatching {
                val v6 = HttpServer.create(InetSocketAddress("::1", s.address.port), 0)
                v6.createContext("/player.html") { exchange ->
                    try {
                        val html = pageHtmlProvider?.invoke() ?: playerPageHtml(null)
                        val bytes = html.toByteArray(Charsets.UTF_8)
                        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                        exchange.responseHeaders.add("Cache-Control", "no-store")
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { exchange.close() }
                    }
                }
                v6.executor = null
                v6.start()
                serverV6 = v6
                KcefEngine.logKcef("server pagina player: loopback IPv4 + IPv6 sulla porta ${s.address.port}")
            }.onFailure {
                KcefEngine.logKcef("server pagina player: solo loopback IPv4 sulla porta ${s.address.port} (IPv6 non disponibile)")
            }
        }
        "http://localhost:${s.address.port}/player.html"
    }

    fun stop() {
        synchronized(this) {
            runCatching { server?.stop(0) }
            runCatching { serverV6?.stop(0) }
            server = null
            serverV6 = null
        }
    }
}

/**
 * HTML del player, struttura allineata al fork AYP Android
 * (ayp_youtube_player.html) con i SEGNAPOSTO identici:
 *  - "<<injectedVideoId>>": stringa quotata del videoId oppure undefined
 *    (sostituito PRIMA del caricamento, come Android);
 *  - "<<injectedPlayerVars>>": playerVars autolock IFramePlayerOptions
 *    (AYP) con origin dinamico location.origin;
 *  - IFrame API da https://youtube.com/iframe_api con defer (no www, AYP);
 *  - preconnect/dns-prefetch come AYP;
 *  - diagnostica [RiPlay-YT] (probe codec, stati, errori) del desktop;
 *  - bridge desktop riplay* (equivalente delle chiamate evaluateJavascript
 *    che Android fa sulle funzioni loadVideo/cueVideo/playVideo/ecc.).
 * Il player nasce in onYouTubeIframeAPIReady come su Android: se la pagina
 * contiene un videoId parte subito (autoplay=1 nei playerVars), altrimenti
 * resta pronto e i video arrivano via riplayLoad/riplayCue.
 */
private val PLAYER_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<link rel="preconnect" href="https://youtube.com">
<link rel="preconnect" href="https://www.youtube.com">
<link rel="dns-prefetch" href="https://ytimg.com">
<style>
  html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; position: relative; }
  #youTubePlayerDOM { width: 100%; height: 100%; }
  iframe { width: 100%; height: 100%; border: 0; }
  #segnaposto { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #000; color: #8a8a8a; font: 14px sans-serif; text-align: center; padding: 24px; box-sizing: border-box; }
  /* STEP 3k (richiesta utente n.7): il TITOLO del video (in alto) e il
     LOGO YouTube (watermark in basso a destra) dell'embed restano
     CLICCABILI e mandano su youtube.com. Su Android l'utente non li vede
     perche' la WebView e' coperta dall'interfaccia dell'app; qui il
     browser e' visibile, quindi due livelli TRASPARENTI intercettano i
     click su quelle zone prima che arrivino all'iframe (solo blocco
     click: NESSUNA modifica a playerVars, al bridge riplay* e alla
     catena di riproduzione verificata). */
  #guardTitolo { position: absolute; top: 0; left: 0; width: 100%; height: 70px; z-index: 3; background: transparent; cursor: default; }
  #guardLogo { position: absolute; bottom: 0; right: 0; width: 190px; height: 90px; z-index: 3; background: transparent; cursor: default; }
</style>
</head>
<body>
<div id="youTubePlayerDOM">
  <div id="segnaposto">Nessun video in riproduzione.<br>Seleziona una canzone nell'app RiPlay:<br>il player YouTube parte qui.</div>
</div>
<div id="guardTitolo"></div>
<div id="guardLogo"></div>
<script>
  console.log('[RiPlay-YT] pagina player avviata, origine=' + location.origin + ', UA=' + navigator.userAgent);
</script>
<!-- defer come AYP: lo script differito gira DOPO il parsing, quindi dopo
     la definizione di onYouTubeIframeAPIReady qui sotto (niente corsa). -->
<script src="https://youtube.com/iframe_api" defer
        onerror="console.error('[RiPlay-YT] iframe_api NON caricato (rete/DNS/proxy?)')"></script>
<script>
  // === PARAMETRI INTERNI SOSTITUITI NELLA PAGINA PRIMA DEL CARICAMENTO ===
  // (meccanismo identico ad Android: WebViewYouTubePlayer.initWebView
  // sostituisce i segnaposto nell'HTML locale e poi carica la pagina)
  var bakedVideoId = <<injectedVideoId>>;   // stringa quotata oppure undefined

  var player = null;
  var apiReady = false;
  var pendingLoad = null;
  var pendingCue = null;
  // STEP 3e: ultimo video richiesto (load o cue). Serve al recupero del
  // tasto play quando il player non esiste (load perduto/pagina ricominciata):
  // il play ricarica l'ultimo video invece di restare muto.
  var lastRequested = null;
  var lastState = -99;
  var lastErr = 0;
  var activationLogged = false;

  console.log('[RiPlay-YT] pagina inizializzata, origine=' + location.origin
    + ', videoId nel documento=' + (bakedVideoId ? bakedVideoId : 'NESSUNO'));
  // Contesto embed che il browser dichiarera' a YouTube: la pagina host e'
  // http://localhost (nome accettato dalla politica embed; l'IP 127.0.0.1 e
  // www.youtube.com sono RIFIUTATI con errore 152, verifica empirica).
  console.log('[RiPlay-YT] contesto embed: href=' + location.href
    + ' origin=' + location.origin + ' (Referer browser = origine pagina)');

  // Probe delle capacita' multimediali del build CEF: YouTube serve H.264/AAC
  // oppure VP9/Opus (o AV1). Se tutti i codec risultano '' (vuoto) il bundle
  // CEF non ha i codec e il player mostra "An error occurred".
  (function () {
    try {
      var v = document.createElement('video');
      var r = {
        mse: ('MediaSource' in window) ? 'si' : 'no',
        h264: v.canPlayType('video/mp4; codecs="avc1.42E01E"'),
        h264_hi: v.canPlayType('video/mp4; codecs="avc1.64001f"'),
        aac: v.canPlayType('audio/mp4; codecs="mp4a.40.2"'),
        vp9: v.canPlayType('video/webm; codecs="vp9"'),
        opus: v.canPlayType('audio/webm; codecs="opus"'),
        av1: v.canPlayType('video/mp4; codecs="av01.0.05M.08"')
      };
      console.log('[RiPlay-YT] codec supportati: ' + JSON.stringify(r));
    } catch (e) {
      console.error('[RiPlay-YT] probe codec fallita: ' + e);
    }
  })();

  window.addEventListener('error', function (e) {
    console.error('[RiPlay-YT] errore JS: ' + (e.message || e.type)
      + ' @' + (e.filename || '?') + ':' + (e.lineno || 0));
  });
  window.addEventListener('unhandledrejection', function (e) {
    console.error('[RiPlay-YT] promise non gestita: ' + e.reason);
  });

  // Se l'IFrame API non arriva entro 10 s MA un video e' stato richiesto,
  // il player non puo' nascere: di solito significa youtube.com non
  // raggiungibile dal browser CEF.
  setTimeout(function () {
    if (!player && (pendingLoad || pendingCue)) {
      console.error('[RiPlay-YT] IFrame API non pronta dopo 10s, video in attesa ('
        + (pendingLoad ? pendingLoad.id : pendingCue.id)
        + '): iframe_api bloccato o pagina non attiva');
    }
  }, 10000);

  // STEP 3e: secondo tentativo di caricamento dell'IFrame API. Il primo
  // script usa youtube.com (come AYP); se dopo 4 s YT non c'e' ancora
  // (rete lenta / DNS lento / avvio graduale) si riprova via www.youtube.com
  // PRIMA di dichiarare la pagina morta: un brano cliccato non deve restare
  // in coda per sempre.
  setTimeout(function () {
    if (!window.YT || !window.YT.Player) {
      console.log('[RiPlay-YT] iframe_api non ancora disponibile: secondo tentativo via www.youtube.com');
      var s2 = document.createElement('script');
      s2.src = 'https://www.youtube.com/iframe_api';
      s2.onerror = function () {
        console.error('[RiPlay-YT] secondo tentativo iframe_api fallito (rete/DNS/proxy?)');
      };
      document.head.appendChild(s2);
    }
  }, 4000);

  function onYouTubeIframeAPIReady() {
    if (apiReady) return; // guardia: evita doppia invocazione
    apiReady = true;
    console.log('[RiPlay-YT] IFrame API pronta (origine pagina=' + location.origin
      + ', videoId documento=' + (bakedVideoId ? bakedVideoId : 'NESSUNO') + ')');
    // Il video nel documento parte da solo (autoplay=1 nei playerVars):
    // non va ricaricato se un riplayLoad arrivato durante il caricamento
    // aveva lo stesso id.
    if (bakedVideoId) {
      if (pendingLoad && pendingLoad.id === bakedVideoId) pendingLoad = null;
      if (pendingCue && pendingCue.id === bakedVideoId) pendingCue = null;
    }
    createPlayer(bakedVideoId || null, !!bakedVideoId);
    if (pendingLoad) { var r = pendingLoad; pendingLoad = null; window.riplayLoad(r.id, r.start); }
    else if (pendingCue) { var c = pendingCue; pendingCue = null; window.riplayCue(c.id, c.start); }
  }

  // Corsa critica cache: se www-widgetapi.js finisce di caricarsi PRIMA che
  // questo blocco definisca il callback, YouTube non lo chiamerebbe mai:
  // YT.ready invoca subito la funzione se l'API e' gia' pronta.
  if (window.YT && YT.ready) {
    YT.ready(function () { onYouTubeIframeAPIReady(); });
  }

  // Creazione del player IDENTICA ad AYP (ayp_youtube_player.html,
  // onYouTubeIframeAPIReady): videoId del documento se presente,
  // playerVars sostituiti nella pagina (autolock IFramePlayerOptions).
  function createPlayer(id, autoplay) {
    var playerConfig = {
      height: '100%',
      width: '100%',
      playerVars: <<injectedPlayerVars>>,
      events: {
        onReady: function() {
          console.log('[RiPlay-YT] player pronto (videoId=' + (id ? id : 'NESSUNO') + ')');
          // STEP 3f: diagnostica AUTOPOLICY. hasBeenActive=false e un video
          // che resta in unstarted (stato -1 -> 3 -> -1) indicano che la
          // policy user activation di Chromium sta bloccando l'autoplay:
          // l'app inviera' un CLICK REALE via CEF (input nativo) per
          // attivare l'iframe. Questo log e' la prova della catena.
          try {
            if (navigator.userActivation) {
              console.log('[RiPlay-YT] userActivation: hasBeenActive=' + navigator.userActivation.hasBeenActive
                + ', isActive=' + navigator.userActivation.isActive);
            } else {
              console.log('[RiPlay-YT] userActivation: API non disponibile in questo Chromium');
            }
          } catch (e3f) {}
          // Parita' AYP (onReady): sblocca autoplay/encrypted-media
          // sull'iframe e unMute immediato (AYP forza anche volume=100:
          // qui no, il volume desktop e' gestito dall'app via riplaySetVolume).
          try {
            if (player.unMute) player.unMute();
            if (player.getIframe) {
              var f = player.getIframe();
              if (f) f.setAttribute('allow', 'autoplay; encrypted-media');
            }
          } catch (e2) {
            console.log('[RiPlay-YT] onReady unlock: ' + e2);
          }
          if (autoplay && player.playVideo) player.playVideo();
        },
        onStateChange: function(e) {
          if (e.data !== lastState) {
            console.log('[RiPlay-YT] stato: ' + lastState + ' -> ' + e.data
              + '  (-1 unstarted, 0 ended, 1 playing, 2 paused, 3 buffering, 5 cued)');
            lastState = e.data;
          }
        },
        onError: function(e) {
          lastErr = e.data || 0;
          console.error('[RiPlay-YT] ERRORE PLAYER YT code=' + lastErr
            + ' (2=parametro invalido, 5=errore html5/codec/rete, 100=video inesistente/privato,'
            + ' 101/150=incorporamento vietato, 152/153=contesto embed rifiutato)'
            + ' video=' + JSON.stringify(player && player.getVideoData ? player.getVideoData() : {}));
        }
      }
    };
    if (id) { playerConfig.videoId = id; }
    console.log('[RiPlay-YT] creo YT.Player videoId=' + (id ? id : 'NESSUNO (pagina senza video)')
      + ' autoplay=' + (autoplay ? 1 : 0)
      + ' playerVars=' + JSON.stringify(playerConfig.playerVars));
    player = new YT.Player('youTubePlayerDOM', playerConfig);
  }

  function riplayLoad(id, start) {
    lastErr = 0;
    lastRequested = { id: id, start: start || 0 };
    console.log('[RiPlay-YT] load videoId=' + id + ' start=' + (start || 0));
    if (!apiReady) { pendingLoad = { id: id, start: start || 0 }; console.log('[RiPlay-YT] load in attesa della API'); return; }
    if (!player) { console.log('[RiPlay-YT] load senza player: creo il player con autoplay'); createPlayer(id, true); return; }
    player.loadVideoById({ videoId: id, startSeconds: start || 0 });
    console.log('[RiPlay-YT] load eseguito sul player esistente');
  }

  function riplayCue(id, start) {
    lastRequested = { id: id, start: start || 0 };
    console.log('[RiPlay-YT] cue videoId=' + id + ' start=' + (start || 0));
    if (!apiReady) { pendingCue = { id: id, start: start || 0 }; return; }
    if (!player) { createPlayer(id, false); return; }
    player.cueVideoById({ videoId: id, startSeconds: start || 0 });
  }

  // STEP 3e: play CON recupero. Prima: se il player non esisteva il play
  // restava muto (il tasto sembrava morto). Ora: player assente ma un
  // video e' stato richiesto -> si ricarica l'ultimo video richiesto
  // (stessa via di riplayLoad, con tutti i suoi controlli); se l'API non
  // e' ancora pronta il load resta in coda e partira' con la API.
  function riplayPlay() {
    // STEP 3f: diagnostica una tantum della user activation al momento del
    // play: hasBeenActive=false qui = la policy blocchera' playVideo
    // (il click reale dell'app deve arrivare PRIMA di questa chiamata).
    if (!activationLogged) {
      activationLogged = true;
      try {
        console.log('[RiPlay-YT] riplayPlay: userActivation.hasBeenActive='
          + (navigator.userActivation ? navigator.userActivation.hasBeenActive : 'n/d'));
      } catch (e) {}
    }
    if (player && player.playVideo) { player.playVideo(); return; }
    console.log('[RiPlay-YT] play senza player attivo: recupero con il video richiesto in precedenza');
    if (lastRequested) {
      var r = lastRequested;
      pendingLoad = null;
      pendingCue = null;
      riplayLoad(r.id, r.start);
    } else {
      console.log('[RiPlay-YT] play senza player e senza video richiesto: nulla da recuperare');
    }
  }
  function riplayPause() { if (player && player.pauseVideo) player.pauseVideo(); }
  function riplayStop()  { if (player && player.stopVideo)  player.stopVideo(); }
  function riplaySeekTo(sec) { if (player && player.seekTo) player.seekTo(sec, true); }

  function riplaySetVolume(v) {
    if (player && player.setVolume) {
      if (player.unMute) player.unMute();
      player.setVolume(Math.max(0, Math.min(100, v | 0)));
    }
  }

  function riplaySetMuted(m) {
    if (!player) return;
    if (m && player.mute) player.mute();
    else if (!m && player.unMute) player.unMute();
  }

  function riplaySetRate(r) { if (player && player.setPlaybackRate) player.setPlaybackRate(r); }

  function riplayState() {
    if (!player || !player.getPlayerState) return JSON.stringify({ s: -99, t: 0, d: 0, v: 100, m: true, err: lastErr });
    var t = 0, d = 0, v = 100, m = true;
    try {
      t = player.getCurrentTime() || 0;
      d = player.getDuration() || 0;
      v = player.getVolume() || 0;
      m = !!player.isMuted();
    } catch (e) {}
    return JSON.stringify({ s: lastState, t: t, d: d, v: v, m: m, err: lastErr });
  }
</script>
</body>
</html>
"""
