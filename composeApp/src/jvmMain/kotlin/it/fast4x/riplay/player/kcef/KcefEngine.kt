package it.fast4x.riplay.player.kcef

import it.fast4x.riplay.i18n.ds

import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFClient
import it.fast4x.riplay.data.DesktopDbRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.ArrayDeque

/**
 * Stato del motore KCEF (download bundle CEF + inizializzazione).
 */
sealed interface KcefState {
    data object NotStarted : KcefState
    data object Locating : KcefState
    data class Downloading(val percent: Float) : KcefState
    data object Extracting : KcefState
    data object Installing : KcefState
    data object Initializing : KcefState
    data object Ready : KcefState
    data class Failed(val message: String) : KcefState
}

/**
 * Motore KCEF (Chromium Embedded Framework per JVM) per la riproduzione
 * YouTube su desktop con la stessa IFrame Player API usata su Android.
 *
 * Il bundle CEF (~120-200 MB) viene scaricato AL PRIMO AVVIO nella
 * directory dati utente dell'app:
 *  - Windows: %APPDATA%\RiPlay\kcef
 *  - Linux:   ~/.local/share/RiPlay/kcef
 *  - macOS:   ~/Library/Application Support/RiPlay/kcef
 *
 * NOTE CRITICHE sull'inizializzazione (v. crash "GPU process isn't usable"):
 *  1. KCEF.init prova PRIMA il percorso "runtime" (KCEFBuilder.initFromRuntime):
 *     carica "libcef"/"jcef" con System.loadLibrary, che su Windows trova le
 *     DLL in java.home\bin. Se il JDK contiene un JCEF integrato (Zulu/JBR),
 *     quelle natives (versione diversa dal bundle) si accoppiano alle classi
 *     org.cef del jar datlag -> subprocess incoerenti -> Chromium FATAL.
 *     FIX: pre-carichiamo le librerie del BUNDLE con System.load (path
 *     assoluto) PRIMA di KCEF.initBlocking: su Windows un modulo gia' mappato
 *     (stesso basename) viene riusato da qualunque LoadLibrary successivo,
 *     quindi anche il percorso "runtime" finisce sulle natives del bundle.
 *     STEP 2e: il preload e' ora VERIFICATO - chrome_elf/libcef/jcef devono
 *     essere presenti nel bundle e caricare senza errori, altrimenti l'init
 *     NON parte e compare un messaggio esplicito (niente piu' FATAL oscuro).
 *  2. Le settings CEF default derivano da JCefAppConfig (java.home): se i
 *     campi restano popolati col JDK, browser_subprocess_path punta al
 *     jcef_helper del JDK (versione diversa) e i subprocess vanno in crash.
 *     STEP 2e: quando il bundle NON e' ancora stato estratto impostiamo i
 *     path a STRINGA VUOTA invece di lasciare il default del JDK: KCEF
 *     (CefInitializer) autofilla i campi vuoti verso installDir del bundle.
 *  3. Usiamo il rendering WINDOWED (CefRendering.DEFAULT) in SwingPanel:
 *     windowlessRenderingEnabled va quindi impostata a false.
 *  4. La catena di caricamento nativa di CefApp su Windows e'
 *     jawt -> chrome_elf -> libcef -> jcef: il preload del solo libcef NON
 *     basta. Pre-carichiamo TUTTE le librerie native del bundle (System.load
 *     assoluto, doppia passata per l'ordine delle dipendenze).
 *  5. Argomenti CEF --disable-gpu / --disable-gpu-compositing: su molte
 *     configurazioni Windows (GPU integrata/driver datati/VM/Remote Desktop)
 *     il processo GPU non parte ("GPU process launch failed error_code=63")
 *     e Chromium esce con FATAL "GPU process isn't usable. Goodbye.".
 *     Con disable-gpu il rendering e' software e il processo GPU non serve.
 *     KCEF inoltre, riconoscendo --disable-gpu, salta il caricamento delle
 *     librerie GPU (EGL/GLESv2/vk_swiftshader) nel percorso runtime.
 *  6. DIAGNOSTICA (STEP 2e): ogni avvio stampa su console righe
 *     "[RiPlay-KCEF] ..." con java.home, presenza di JCEF nel JDK, sorgente
 *     delle classi org.cef, directory bundle, esito del preload e valori
 *     finali di settings/args. Se il crash persiste, il log identifica
 *     immediatamente la causa (build non aggiornata, preload fallito,
 *     bundle assente o settings rimaste sul JDK).
 *  7. STEP 2f: il preload caricava jcef.dll SENZA aver prima inizializzato
 *     AWT (jawt.dll): il System.load falliva con "dependent libraries not
 *     found" e il check rigoroso di step2e bloccava l'init -> avviso
 *     "KCEF non disponibile". Ora AWT/jawt vengono caricati prima del
 *     preload, le librerie GPU sono OPZIONALI (con --disable-gpu il
 *     rendering e' software e non servono) e jcef e' obbligatorio solo
 *     su JDK con JCEF integrato. La UI mostra il motivo del fallimento
 *     con pulsante "Riprova".
 *  8. STEP 2g (dal log utente v6): (a) browser_subprocess_path puntava a
 *     jcef_helper.DLL invece di jcef_helper.EXE (.dll viene prima in ordine
 *     alfabetico) -> OGNI processo figlio (GPU, network service) falliva la
 *     partenza -> "GPU process launch failed: error_code=63" + FATAL; ora
 *     l'helper .exe ha SEMPRE priorita'; (b) su Windows CefApp.startup
 *     e' asincrona: initBlocking tornava con stato INITIALIZING e client/
 *     browser nascevano prima del contesto nativo ("can't invoke native
 *     method before native context initialized", chiamate JS perse); ora si
 *     attende lo stato CefAppState.INITIALIZED prima di creare il client.
 *  9. STEP 2h (dal test v7 utente): il browser ora parte (logo YT visibile)
 *     ma il bundle veniva RI-SCARICATO a ogni riavvio. Diagnosi definitiva:
 *     (a) il bundle KCEF 2025.03.23 e' un pacchetto JetBrainsRuntime
 *     "jbr_jcef" (verificato scaricando l'archivio REALE che KCEF seleziona
 *     da github.com/JetBrains/JetBrainsRuntime, es.
 *     jbr_jcef-25.0.4.1-windows-x64-b508.40.tar.gz): contiene LEGITTIMAMENTE
 *     un intero runtime Java (bin/jvm.dll, java.dll, awt.dll, jdwp.dll,
 *     jli.dll, extnet, websocket...) + le natives CEF (libcef, chrome_elf,
 *     jcef) + ENTRAMBI jcef_helper.dll e jcef_helper.exe. Il check
 *     "contaminazione JDK" di step2g era quindi SEMPRE vero sui bundle
 *     validi -> deleteRecursively() a ogni avvio -> ri-download infinito.
 *     (b) KCEFBuilder.install (bytecode verificato) cancella l'INTERA
 *     installDir e ri-scarica il bundle se manca install.lock:
 *     existsSafely(lock) -> !exists -> deleteDir(installDir) ->
 *     downloadPackage -> TarGzExtractor -> createSafely(install.lock).
 *     FIX: check contaminazione RIMOSSO; install.lock PRE-CREATO quando il
 *     bundle e' completo (libcef + helper eseguibile) cosi' KCEF non
 *     cancella/ri-scarica MAI un bundle valido; preload ridotto a
 *     WHITELIST (chrome_elf, libcef, jcef): le decine di DLL del runtime
 *     Java del bundle NON vanno caricate nel processo host (che ha gia' il
 *     suo JVM; su Linux dlopen caricherebbe doppioni reali).
 * 10. STEP 2i (dal test v8 utente): browser e bundle ora funzionano, ma
 *     cliccando una canzone il player YouTube mostra "An error occurred"
 *     (Playback ID) e non riproduce. La pagina embed SI carica (logo
 *     visibile), quindi il problema e' nella FASE DI RIPRODUZIONE. Cause
 *     plausibili: autoplay programmatico bloccato dalla policy Chromium
 *     (l'app chiama loadVideoById senza gesto utente nel frame), QUIC/
 *     UDP-443 bloccato da reti aziendali (youtube.com su TCP funziona,
 *     gli stream di googlevideo.com su QUIC falliscono), codec assenti
 *     dal build CEF. FIX/DIAGNOSTICA: (a) flag --autoplay-policy=
 *     no-user-gesture-required e --disable-quic; (b) handler
 *     CefDisplayHandler.onConsoleMessage che instrada su log le righe
 *     ERROR/WARNING e tutti i messaggi della pagina player marcati
 *     [RiPlay-YT] (errori player YT, probe dei codec h264/aac/vp9/opus/
 *     av1, errori JS, caricamento iframe_api); (c) CefLoadHandler.
 *     onLoadError per gli errori di rete net::ERR; (d) il codice errore
 *     del player YT (2/5/100/101/150) arriva nello stato JSON (err) e
 *     viene mostrato in UI insieme al significato.
 * 11. STEP 2j (dal log v9 utente): ERRORE PLAYER YT code=2 = "invalid
 *     parameter" MA il test in browser reale con la STESSA pagina mostra
 *     che un videoId valido funziona (durata caricata) e un videoId
 *     invalido NON produce code=2: nel player IFrame API il codice 2
 *     corrisponde in pratica a LOGIN_REQUIRED - video con restrizione
 *     di eta' oppure (caso tipico) muro consenso/anti-bot di YouTube
 *     con profilo cookie VUOTO (Italia = EEA). FIX: cookie di consenso
 *     pre-seminati via CefCookieManager PRIMA che il browser li usi:
 *     SOCS=CAE (consenso EU "rifiuta non essenziali", rimuove il muro)
 *     su .youtube.com e .youtube-nocookie.com + legacy CONSENT=YES.
 *     Il videoId ora viene loggato lato Kotlin (loadVideoById) per
 *     confermare esattamente cosa arriva al player.
 * 12. STEP 2k (dal log v10 utente): code=2 PERSISTE e l'utente nota che
 *     l'errore arriva SENZA aver scelto nessuna canzone: "l'errore sembra
 *     dovuto al mancato id del video da riprodurre". CONFERMATO: (a) la
 *     pagina creava YT.Player SENZA videoId (embed vuoto: oggi YouTube
 *     risponde code=2 "parametro non valido" quando l'embed non ha ID) e
 *     (b) l'app NON passava MAI l'id della canzone cliccata al pannello
 *     KCEF (DesktopVideoSource aveva solo l'input manuale). FIX: (a)
 *     creazione RITARDATA del player nella pagina: YT.Player nasce SOLO
 *     quando arriva un videoId (riplayLoad/riplayCue) e nasce GIA' con
 *     quel video (autoplay per load); niente piu' embed vuoto -> niente
 *     piu' code=2 all'avvio; (b) ThreeColumnsApp passa videoId (Song.id)
 *     a DesktopVideoSource -> YouTubeKcefPane che chiama loadVideoById;
 *     (c) loadVideoById/cueVideoById sanitizzano l'id con
 *     YouTubeUtils.extractVideoId e IGNORANO con log chiaro gli id non
 *     validi (protezione da code=2 per id sporco o URL interi).
 * 13. STEP 2l (dal test v11 utente: code=150 "incorporamento vietato" su
 *     video che su Android si riproducono): ANALISI della preparazione del
 *     player lato Android (fork AYP) e REPLICA esatta su desktop:
 *     (a) UA FORZATA desktop Chrome/134 (WebView.userAgentString AYP) ->
 *     CefSettings.userAgent; (b) loadDataWithBaseURL("https://youtube.com")
 *     rende la pagina player FIRST-PARTY youtube.com -> su desktop non
 *     esiste loadDataWithBaseURL: la pagina viene servita da
 *     CefApp.registerSchemeHandlerFactory("https", "youtube.com") che
 *     dirotta SOLO https://youtube.com/riplay/Player.html (le altre URL
 *     inclusa www.youtube.com passano sulla rete reale); (c) playerVars
 *     IDENTICI all'autolock IFramePlayerOptions (autoplay=1, mute=0,
 *     controls=0, enablejsapi=1, fs=0, rel=0, iv_load_policy=3,
 *     cc_load_policy=0, origin=https://youtube.com); (d) IFrame API da
 *     https://youtube.com/iframe_api con defer (no www, come AYP).
 *     Fallback: se il factory non si registra, server locale 127.0.0.1
 *     come step2k ([PlayerHtmlServer.playerUrl] sceglie da solo).
 * 14. STEP 2m (dal test v12 utente: "non viene caricata la pagina che crea
 *     il player ma la pagina di youtube"): CAUSA RADICE TROVATA nella
 *     registrazione del factory step2l - avveniva sul thread kcef-init,
 *     MA registerSchemeHandlerFactory deve girare sul THREAD UI CEF.
 *     Bytecode JCEF 147 verificato: CefApp.registerSchemeHandlerFactory
 *     RITORNA SEMPRE true (boolean senza valore), la registrazione vera
 *     e' DIFERITA e la chiamata nativa fallisce in silenzio se non e' sul
 *     thread UI -> il factory non esisteva -> navigazione reale verso
 *     youtube.com -> YouTube mostrava la sua pagina. PROVA DEFINITIVA:
 *     la sorgente KCEF (KCEF.kt, AppHandler) registra il SUO factory
 *     file:// dentro AppHandler.onContextInitialized() - il callback che
 *     CEF invoca sul thread UI. FIX: passiamo a KCEF.initBlocking un
 *     AppHandler custom che in onContextInitialized (thread UI) registra
 *     ANCHE il nostro factory https://youtube.com - identico al pattern
 *     ufficiale di KCEF. Di nuovo: caricare l'HTML in questo modo produce
 *     lo stesso risultato di loadDataWithBaseURL("https://youtube.com")
 *     di Android (PlayerService.kt/AYP): pagina LOCALE non scaricata
 *     dalla rete, con documento/origine youtube.com. Sicurezza aggiunta:
 *     se a runtime il load end del main frame mostra che e' finita su una
 *     pagina YouTube REALE (o status != 2xx), fallback AUTOMATICO al
 *     server locale 127.0.0.1 (comportamento step2k noto funzionante).
 *
 * 15. STEP 2n (dal test v13 utente: postMessage "The target origin provided
 *     ('https://www.youtube.com') does not match the recipient window's
 *     origin ('http://127.0.0.1:57335')"): il log dimostra che la pagina
 *     finiva sul SERVER LOCALE (fallback step2k) e con playerVars.origin
 *     hardcoded 'https://youtube.com' (step2l) TUTTI i messaggi iframe->
 *     pagina venivano scartati -> niente si caricava piu'. Tre FIX:
 *     (a) ORIGIN DINAMICO: playerVars.origin torna location.origin
 *     (comportamento step2k validato): l'analisi del vero www-widgetapi.js
 *     mostra che il widget api SOVRASCRIVE comunque l'origin con l'origine
 *     della pagina, quindi il valore dinamico e' l'unico coerente e i
 *     postMessage tornano a matchare in ogni modalita'; (b) CANALE
 *     DETERMINISTICO per l'origine youtube.com (equivalente
 *     loadDataWithBaseURL Android): CefRequestHandler.getResourceRequest
 *     Handler - intercettazione per-request a livello browser che CEF
 *     consulta per OGNI richiesta (https standard incluse) - installato
 *     con KCEFClient.addRequestHandler subito dopo newClientBlocking,
 *     PRIMA di qualsiasi browser (bytecode JCEF 147 verificato: dispatch
 *     sul campo requestHandler_, slot che KCEF non usa); risponde SOLO a
 *     https://youtube.com/riplay/..., tutto il resto passa alla rete;
 *     (c) il factory scheme handler (step2m) resta come secondo canale.
 *     Nota sul warning dell'utente: e' la direzione pagina->iframe (il
 *     widget api posta all'iframe con l'origine del suo src www.youtube.com)
 *     e si verifica mentre l'iframe embed non ha ancora contenuto YT:
 *     transitorio e non blocca l'handshake quando l'origine pagina e' giusta.
 *
 * 16. STEP 2o (dal test v14 utente: "Exception in thread Thread-1267
 *     java.lang.NoSuchMethodError: open" e "Thread-1271 ... read" al click
 *     su una canzone, pagina player non caricata): CAUSA RADICE nel glue
 *     nativo JCEF (resource_handler.cpp): il wrapper C++ di CefResource
 *     Handler chiama via JNI PRIMA il metodo NUOVO API "open" e poi "read";
 *     il jar jcef di KCEF 2025.03.23 contiene l'interfaccia VECCHIA a 4
 *     metodi (pool CefResourceHandler.class verificata) mentre le NATIVE
 *     scaricate da KCEF arrivano dalle release JetBrainsRuntime LATEST
 *     (glue recente): GetMethodID("open") fallisce -> NoSuchMethodError
 *     descritto/cancellato dalla macro JNI_CALL_METHOD (messaggio SOLO con
 *     il nome, senza stack: e' il nativo a lanciarlo) e l'esito della
 *     richiesta dipende dal valore iniziale di BoolRef -> pagina non
 *     servita in modo affidabile. FIX DOPPIO:
 *     (a) PlayerPageResourceHandler dichiara open() con la firma ESATTA
 *     cercata dal nativo, rifiutando la modalita' nuova (handleRequest=
 *     false + return false, identico al default CefResourceHandlerAdapter
 *     nuovo): CEF cade sull'API vecchia processRequest/readResponse gia'
 *     implementata (dettagli in PlayerHtmlServer.kt);
 *     (b) handler GLOBALE degli uncaught exception installato in start():
 *     qualunque eccezione non gestita (JNI, librerie, KCEF) viene loggata
 *     con classe+messaggio+stack COMPLETO nel canale [RiPlay-KCEF], cosi'
 *     i prossimi log utente mostrano l'origine esatta invece del solo nome
 *     del metodo.
 *
 * 17. STEP 2p (dal punto situazione utente: player non appare, browser
 *     punta a un Player.html inesistente caricato da canali di inter-
 *     cettazione/server, origin passato come se fosse un parametro URL,
 *     pagina servita "come se l'host fosse youtube" = meccanismo errato).
 *     ABOLITI tutti i canali di intercettazione (factory scheme handler
 *     step2m, request handler step2n, stub open() step2o, URL fittizia
 *     https://youtube.com/riplay/Player.html): erano la causa dei NoSuch
 *     MethodError open/read (glue nativo JCEF recente contro classi jar
 *     vecchie sui CefResourceHandler custom). NUOVO MECCANISMO - replica
 *     ESATTA di Android (WebViewYouTubePlayer.initWebView, fork AYP):
 *     (a) l'HTML del player e' LOCALE con i parametri interni SOSTITUITI
 *     PRIMA del caricamento ("<<injectedVideoId>>" / "<<injectedPlayer
 *     Vars>>" come ayp_youtube_player.html); (b) l'origine first-party
 *     youtube.com (equivalente del baseUrl di loadDataWithBaseURL) viene
 *     fissata caricando una pagina host youtube.com minuscola e stabile
 *     (robots.txt) e SOVRASCRIVENDO il documento con
 *     document.write(HTML) via CefBrowser.executeJavaScript RAW (il
 *     wrapper KCEF manomette il codice mono-riga): la pagina player non
 *     e' MAI scaricata dalla rete ne' servita da un server, e l'origin
 *     resta una variabile interna del player (dentro i playerVars, mai
 *     sull'URL del browser); (c) RIPRISTINO automatico: se la pagina host
 *     fallisce (onLoadError) o il documento locale non si conferma entro
 *     6 s (marker console "pagina player avviata" assente), si ricarica
 *     dal server locale 127.0.0.1 con LO STESSO HTML sostituito
 *     (comportamento step2k noto). Coordinamento in PlayerHtmlServer
 *     (onMainFrameLoadEnd/onHijackLoadError/tryMarkWriteConfirmed).
 *
 * 18. STEP 2q (dal test v16 utente: errore YT 152 su TUTTI i video, nessun
 *     riproducibile mentre su Android tutti funzionano): la famiglia 152/
 *     152-4/152-13/153 e' la VALIDAZIONE DEL CONTESTO EMBED di YouTube
 *     (origine/referrer della pagina incorporante; uMatrix "Spoof referrer"
 *     produce 152-4, Invidious documenta il 153 per referrer mancante).
 *     VERIFICA PARAMETRI COMPLETA Android vs desktop su sorgente:
 *     (a) UA identica byte per byte (Chrome/134 Win64, AYP WebView.user
 *     AgentString = CefSettings.userAgent); (b) playerVars identici (JSON
 *     autolock IFramePlayerOptions); (c) template HTML, segnaposto e
 *     iframe_api identici; (d) videoId sostituito nell'HTML identico.
 *     DELTA TROVATO nel sorgente REALE del widget ufficiale (www-widgetapi.js
 *     versione cbcfbf2f, funzione Xa che costruisce l'URL dell'iframe embed):
 *     1. playerVars.origin viene SOVRASCRITTO dal widget con l'origine reale
 *     della pagina -> inerte su entrambe le piattaforme; 2. il widget aggiunge
 *     "forgin = window.location.href" COMPLETA e il Referer HTTP della
 *     richiesta iframe e' l'URL della pagina host: su Android
 *     loadDataWithBaseURL mantiene la pagina a "https://youtube.com/"
 *     (homepage, contesto valido) mentre su desktop la pagina host era
 *     "https://www.youtube.com/robots.txt" -> contesto embed anomalo ->
 *     YouTube rifiuta TUTTI i video con 152. FIX (PlayerHtmlServer): subito
 *     dopo document.write il documento esegue history.replaceState(null,
 *     '','/') -> location.href diventa "https://www.youtube.com/" SENZA
 *     ricaricare nulla: forgin e Referer dell'iframe diventano la homepage
 *     first-party, equivalente desktop del baseUrl Android; la pagina logga
 *     "[RiPlay-YT] contesto embed: href=... origin=..." come prova. La mappa
 *     dei significati errori ora copre 152 e 153.
 *
 * 19. STEP 2r (dal test v17 utente: 152 su TUTTI i video NONOSTANTE la
 *     homepage youtube.com del fix 2q): ESPERIMENTI EMPIRICI diretti sulla
 *     pagina embed di YouTube (la playability della pagina embed e' nel
 *     campo embedded_player_response/previewPlayabilityStatus del HTML,
 *     estratto e confrontato cambiando SOLO il Referer della richiesta):
 *     - Referer assente -> ERROR "Errore di configurazione del video
 *       player"; Referer www.youtube.com -> ERROR "Questo video non e'
 *       disponibile" (IL CASO v16/v17 = 152 su tutti); Referer 127.0.0.1
 *       -> UNPLAYABLE stesso motivo (IL CASO step2k); Referer apex
 *       youtube.com (IL CASO ANDROID loadDataWithBaseURL) -> OK;
 *       Referer http://localhost (NOME, non IP) -> OK; domini terzi -> OK.
 *     POLITICA STRING-BASED: YouTube decide solo dal valore del Referer.
 *     FIX DOPPIO: (a) la pagina player gira su http://localhost:PORT
 *     (server loopback raggiunto col NOME: origine ONESTA che YouTube
 *     accetta; il player html locale con i parametri sostituiti prima del
 *     caricamento resta identico, aboliti robots.txt/document.write/
 *     replaceState/fallback); (b) KcefEngine.installEmbedRefererPolicy:
 *     sulle richieste https://www.youtube.com/embed/ il Referer viene
 *     impostato ad apex https://youtube.com/ (stesso contesto che Android
 *     produce con loadDataWithBaseURL; combinazione Referer apex + parametri
 *     widget localhost verificata accettata). getResourceHandler resta
 *     SEMPRE null: nessun CefResourceHandler custom = nessun rischio
 *     NoSuchMethodError open/read del glue nativo JCEF (lezione step2o/2p).
 *     playerVars.origin torna DINAMICO (location.origin, validazione
 *     step2k/2n: con valore hardcoded i postMessage vengono scartati).
 *     VALIDATO UTENTE: "tutti i vari errori 152 sono spariti e viene
 *     riprodotto tutto quello che clicco".
 *
 * 20. STEP 3 (refactor player desktop, niente tab di debug):
 *     DesktopPlayerManager (player/player/DesktopVideoSource.kt) e' l'UNICO
 *     gestore motore dell'app: selezione AUTOMATICA in stile Android
 *     (PlayerService: if (currentSong.isLocal) switchToExo() else
 *     switchToYoutube()) -> brano/URL YouTube -> KCEF, brano locale
 *     (Song.id con prefisso local:), file o stream -> VLCJ. Al cambio
 *     motore l'altro motore va in pausa; al ritorno su KCEF il browser si
 *     ricrea e il videoId in attesa viene iniettato nell'HTML (meccanismo
 *     step2r). MiniPlayer e barre di controllo sono agganciate al motore
 *     ATTIVO (manager.active); rimossi i tab VLCJ/KCEF, l'URL di test
 *     BigBuckBunny e il wiring FramePlayer; il file picker e' one-shot
 *     (pickLocalMediaPath) e VlcjController.load rilascia il media player
 *     precedente. Questo motore (KCEF) NON cambia comportamento.
 *
 *     20) v20 (step3b): navigazione desktop stile Android (barra in basso
 *     Home/Songs/Artists/Albums/Playlists, pagina destinazione espansa a
 *     destra, dettagli con Back). Solo UI: questo motore (KCEF) NON
 *     cambia comportamento.
 *
 *     21) v22 (step3d): la COVER del brano e il segnaposto "nessun brano"
 *     sono disegnati a livello SWING (KcefPlayerHostPanel: Canvas AWT sopra
 *     il browser dentro lo stesso pannello ospite) INVECE CHE nascondere il
 *     componente AWT del browser (v21): nasconderlo lasciava scoperto il
 *     contenitore Swing di interop (pannello bianco) e bloccava l'avvio
 *     della riproduzione (tasto play inerte). Il browser resta vivo e
 *     visibile sotto la cover: pagina player, autoplay e contesto embed
 *     step2r INTATTI. Banner errori YT spostato sulla canvas. Solo UI:
 *     questo motore (KCEF) NON cambia comportamento.
 *
 *     22) v23 (step3e, dal test v22 "il tasto play non ha alcun effetto"):
 *     ROBUSTEZZA della catena di caricamento (la pagina player e il
 *     contesto embed step2r restano IDENTICI). KcefYouTubePlayer:
 *     attachBrowser non cachea piu' un pannello SENZA browser quando il
 *     client non e' pronto (la 3d lo cachedava: player muto per sempre,
 *     nessun errore); ensureBrowser idempotente richiamabile da factory,
 *     watchdog e recupero del play; WATCHDOG di caricamento (12 s, max 3
 *     tentativi): il video che non parte e senza ne' errore ne' durata
 *     viene re-inviato (pagina viva) oppure il browser viene RICREATO con
 *     il videoId ri-cotto nell'HTML (pagina morta, meccanismo autoplay
 *     step2r); play() arma il recupero quando non c'e' nessun video
 *     attivo (il tasto play ha SEMPRE un effetto); log puntuale di ogni
 *     passaggio (load in attesa, re-invio, ricrea browser, watchdog).
 *     PlayerHtmlServer (solo JS interno della pagina, contesto embed
 *     invariato): ultimo video richiesto memorizzato; riplayPlay con
 *     RECUPERO (player assente -> ricarica l'ultimo video invece di
 *     restare muto); secondo tentativo iframe_api via www.youtube.com
 *     dopo 4 s; log "load in attesa della API"/"load eseguito".
 *
 *     23) v24 (step3f, dal log del test v23: "stato: -1 -> 3 -> -1", il
 *     tasto play senza effetto): AUTOPOLICY / USER ACTIVATION. La
 *     sequenza -1 -> 3 (buffering) -> -1 (unstarted) e' Chromium che
 *     ANNULLA la riproduzione avviata senza gesto utente: prima di v21
 *     l'utente cliccava il play DI YOUTUBE nel browser esposto (gesto
 *     reale); con la cover Swing sopra il browser quel gesto non esiste
 *     piu' e le chiamate JS da Kotlin NON sono gesti. Il flag
 *     --autoplay-policy=no-user-gesture-required (step2i) NON basta.
 *     KcefYouTubePlayer: (a) injectUserActivation: CLICK REALE via
 *     sendMouseEvent (input nativo CEF = input hardware: user activation
 *     TRUSTED sull'iframe YT; sul video unstarted e' lui stesso il
 *     toggle-play; guard anti-pausa su playing/buffering); (b) KICK
 *     AUTOPLAY 3 s dopo ogni load (click+play se non partito da solo);
 *     (c) WATCHDOG riscritto: durata>0 MA stato unstarted = autoplay
 *     bloccato (i metadati arrivano col buffering anche quando la
 *     partenza viene annullata: era il bug che impediva al soccorso di
 *     intervenire) -> activation+play invece di uscire; (d) play():
 *     activation prima del riplayPlay; (e) stato YT GREZZO (raw) per i
 *     guard. PlayerHtmlServer (solo diagnostica, contesto embed
 *     invariato): log navigator.userActivation in onReady e in
 *     riplayPlay (prova della catena activation).
 *     DesktopVideoSource: la chiave di deduplica del routing include il
 *     percorso locale SOLO per i brani locali (prima i brani online
 *     cambiavano chiave tra le due passate e loadVideoById partiva DUE
 *     volte: brano che ripartiva da zero poco dopo l'avvio).
 *
 *     24) v25 (step3g, dal test v24: "con il player aperto si vede un
 *     pannello bianco posto esattamente sopra il browser" + richiesta
 *     "aggiungere uno chevron down in alto nel player full"): SOLO UI,
 *     questo motore (KCEF) NON cambia comportamento. KcefVideoSurface:
 *     il "pannello bianco" era il WRAPPER di interop che SwingPanel
 *     (SwingInteropViewGroup, JPanel) dipinge quando il pannello ospite
 *     e' nascosto e che Compose RI-MOSTRA a ogni layout
 *     (setVisible(!clip.isEmpty)); Compose inoltre cancella i propri
 *     pixel nella regione interop (BlendMode.Clear), quindi l'overlay non
 *     puo' apparire li'. FIX: con l'overlay aperto il nodo SwingPanel si
 *     riduce a zero (Modifier.size(0.dp)): Compose nasconde da solo il
 *     wrapper e l'overlay si vede in tutta la finestra; il browser resta
 *     nella gerarchia AWT (nessun re-parenting) e la finestra nativa e'
 *     solo nascosta (stato gia' provato: audio che prosegue). Sfondo del
 *     wrapper forzato a NERO (nessun bianco nemmeno per un frame).
 *     DesktopPlayerOverlay: chevron di chiusura piu' grande e visibile
 *     (48dp/22% invece di 40dp/14%: coperto in parte dal bianco, sembrava
 *     assente) + suggerimento di chiusura aggiornato.
 *
 *     25) v26 (step3h, dalla conferma utente v25 "Funziona!!!" e dalla
 *     richiesta "migliorie visive" + "slider orizzontali della timeline
 *     e del volume piu' premium"): SOLO UI, il motore KCEF NON cambia
 *     comportamento. PremiumSlider (nuovo componente): track che si
 *     ingrandisce al passaggio, gradiente, thumb con alone, bolla del
 *     tempo, drag locale con UN solo seek al rilascio. Overlay: entrata
 *     slide+fade+scale, cover con scala a molla/angoli/ombra, play in
 *     cerchio d'accento, sfondo sfumato. Title bar custom (la finestra
 *     undecorated prima non aveva min/max/chiudi ne' trascinamento) +
 *     dimensione finestra ricordata in ~/.riplay/window.properties.
 *     Barra navigazione con pill attiva. Accento unificato RiPlayAccent.
 *
 *     26) v27 (step3i, richiesta utente "prima dello step 4 aggiungere
 *     pezzi mancanti"): (1) ARTISTI SEGUITI - l'artista visitato viene
 *     salvato nel db (stesso schema AlbumScreen), pulsante SEGUI con
 *     bookmark (bookmarkedAt, come gli album preferiti), filtro
 *     "Seguiti" + stella nella pagina Artists; (2) STORICO BRANI - a
 *     ogni ascolto: insert Event + totalPlayTimeMs (+1 ms subito,
 *     poi +5 s a blocco mentre suona): la pagina Songs ordina ora per
 *     data di riproduzione (DatePlayed DESC) e mostra TUTTI i brani
 *     ascoltati; (3) CODA DI RIPRODUZIONE - bottone + nelle liste
 *     (Songs/album/artista/playlist/For You), pannello Coda nel player
 *     full (play subito/X rimuovi/cestino svuota), pulsante AVANTI che
 *     passa alla coda a cronologia esaurita e AVANZAMENTO AUTOMATICO
 *     a fine brano naturale (KCEF: stato YT 0 = ENDED, una sola volta
 *     per video; VLCJ: evento finished di libVLC). L'unica toccata al
 *     player e' il nuovo hook onEnded (lettura dello stato gia'
 *     esistente): NESSUNA modifica a embed/activation/referer/play.
 *
 *     27) v28 (step3j, dalla conferma utente v27 "Funziona" e dalla
 *     richiesta di 4 migliorie prima dello STEP 4): SOLO UI/app, il
 *     motore KCEF NON cambia comportamento. (1) CLICK SU UN BRANO DI UN
 *     ALBUM => TUTTO l'album va in coda e parte il brano cliccato (stessa
 *     cosa per le playlist YouTube: onPlayCollection, come il
 *     forcePlayFromBeginning Android); (2) RIORDINO DELLA CODA col
 *     TRASCINAMENTO nel pannello Coda (sh.calvin.reorderable, la STESSA
 *     libreria della coda Android; ogni brano in coda ha un uid stabile
 *     per chiavi uniche anche con duplicati); (3) RICERCA YouTube Music
 *     (nuova sezione Search nella barra in basso: campo con INVIO,
 *     suggerimenti live, cronologia db, filtri Canzoni/Video/Album/
 *     Artisti/Playlist, "Carica altri"; Environment.searchPage con le
 *     stesse SearchFilter di Android); (4) RADIO: pulsante nel player
 *     full che aggiunge in coda i BRANI SIMILI al brano in riproduzione
 *     (Environment.nextPage = la funzione radio di Android, con
 *     deduplica e feedback). Play/coda v27 intatti.
 *
 * STEP 4 (packaging) potra' pre-seminare questa directory nel DEB/MSI
 * per evitare il download iniziale.
 *
 *     28) v29 (step3k, 8 sistemazioni richieste dall'utente dopo la
 *     conferma v28): TUTTE UI/app tranne una. (1) SWITCH AUDIO/VIDEO
 *     anche nella barra del player di sinistra; (2) pulsante ESPANDI
 *     nella stessa barra: apre il player full screen anche quando la
 *     cover e' coperta da un video in riproduzione; (3) toggle
 *     audio/video RIMOSSO dal player full (per guardare il video si
 *     torna alla home); (4) pulsante RADIO anche nel player di sinistra;
 *     (5) pannello Coda nel full screen ORA CHIUDIBILE (X propria + il
 *     pannello inizia sotto la barra superiore, prima la copriva);
 *     (6) volume su RIGA PROPRIA in fondo nella barra del player di
 *     sinistra (prima compresso nella riga della timeline); (7) NELLA
 *     PAGINA PLAYER: due livelli trasparenti bloccano i click sul
 *     TITOLO del video e sul LOGO YouTube dell'embed (parita' Android:
 *     la WebView Android e' coperta dall'UI dell'app); SOLO blocco
 *     click, NESSUNA modifica a playerVars/bridge/embed; (8) logo e
 *     titolo RiPlay rimossi dall'header della colonna centrale in home.
 *
 *     29) v30 (step3l, richiesta utente "espandere il video a tutta
 *     larghezza della finestra dell'app come fosse un fullscreen, con
 *     ritorno alla larghezza iniziale"): SOLO UI, il motore KCEF NON
 *     cambia comportamento. Nuova modalita' TEATRO nel player di
 *     sinistra: il pulsante fullscreen nella barra (accanto a Radio ed
 *     ESPANDI, solo con un video in visualizzazione) espande la colonna
 *     player su TUTTA la larghezza della finestra (colonna centrale
 *     composta ma a larghezza zero: nessuna perdita di stato delle
 *     pagine); il pulsante diventa fullscreen-exit e riporta alla
 *     larghezza iniziale. Il browser NON viene ricreato ne' riappeso:
 *     cambia solo il layout attorno allo stesso SwingPanel (come un
 *     ridimensionamento della finestra), audio/video intatti; a video
 *     espanso l'input manuale e' nascosto.
 *
 *     30) v31 (step3m, richieste utente "coda accessibile senza il player
 *     full / caricamento dei file locali / home come Android"):
 *     SOLO VERSIONE (marker build): il motore NON cambia. Le tre novita'
 *     vivono TUTTE fuori da KCEF: (1) colonna coda a destra della
 *     finestra principale (Compose puro a destra della colonna player,
 *     che resta al 23% fisso: il browser non viene ridimensionato ne'
 *     toccato); (2) pagina Locali (importazione file del pc -> db ->
 *     motore VLCJ esistente); (3) home desktop da EnvironmentExt
 *     .getHomePage (stessa API della home Android). Catena di play
 *     invariata al 100%.
 *
 *     31) v44 (step3z, FIX CRASH macOS): il percorso bundle/preload/settings
 *     era scritto SOLO per win/linux. Su macOS: (a) NON esiste nessun file
 *     "libcef.dylib" (il libcef reale e' il Mach-O dentro "Chromium Embedded
 *     Framework.framework") -> bundleInstalled() sempre falso -> pre-seed DMG
 *     inutile, install.lock mai creato, KCEF ri-scaricava ~200MB, difese
 *     spente; (b) "chrome_elf" era richiesta SEMPRE ma esiste solo su
 *     Windows (su mac/linux l'init abortiva con "assenti: chrome_elf");
 *     (c) resourcesDirPath puntava alla cartella dell'helper (su mac i .pak
 *     stanno nelle Resources del framework) -> subprocess senza risorse ->
 *     FATAL; (d) il tar di fallback Kotlin non preserva il bit +x -> helper
 *     non eseguibile; (e) nessuna verifica di architettura (DMG arm64 da
 *     macos-14 su un Mac Intel = crash nativo muto); (f) stderr invisibile
 *     dentro il .app = diagnostica impossibile. FIX: scan framework-aware +
 *     bundleInstalled() mac-correct, whitelist preload mac dedicata, settings
 *     mac (subprocess dentro .app, resources dal framework), sweep permessi
 *     +x, xattr quarantine best-effort, guard architettura con messaggio
 *     chiaro, LOG SU FILE <dati>/kcef-boot.log (rotazione 1MB, hint nei
 *     messaggi d'errore) + log nativo Chromium su cef-debug.log (reflection
 *     best-effort). Windows/Linux: comportamento precedente INTATTO (tutte
 *     le diramazioni nuove sono gate isMacOS); unica eccezione cross-OS:
 *     chrome_elf ora e' richiesta solo su Windows (fix anche per Linux).
 */
object KcefEngine {

    /**
     * Marker di versione del bootstrap: SE NEI LOG NON APPARE QUESTA RIGA,
     * il processo sta eseguendo una build NON aggiornata.
     */
    const val BOOTSTRAP_VERSION = "v44 (step3z)"

    private const val TAG = "[RiPlay-KCEF]"

    private fun log(message: String) {
        // stderr: non bufferizzato dal daemon Gradle, visibile subito nel run
        System.err.println("$TAG $message")
        appendLogLine(message)
    }

    /** Log condiviso con lo stesso tag [TAG] (usato anche dal player YT). */
    internal fun logKcef(message: String) {
        System.err.println("$TAG $message")
        appendLogLine(message)
    }

    // -------------------------------------------------------------------------
    // STEP 3z: log su FILE oltre a stderr. Nell'app pacchettizzata (.app/DMG)
    // lo stderr non e' visibile da nessuna parte: senza un file su disco ogni
    // crash CEF su macOS restava indescrivibile. Il file vive accanto al
    // bundle: <installDir>/kcef-boot.log (rotazione ~1 MB su kcef-boot.log.1,
    // mai un'eccezione dal logging: qualunque fallimento disabilita il file).
    // -------------------------------------------------------------------------
    private val logLock = Any()

    @Volatile
    private var logFileResolved: File? = null

    @Volatile
    private var logFileDisabled = false

    private fun bootLogFile(): File? {
        logFileResolved?.let { return it }
        if (logFileDisabled) return null
        synchronized(logLock) {
            logFileResolved?.let { return it }
            val dir = runCatching { installDir }.getOrNull()
            if (dir == null) {
                logFileDisabled = true
                return null
            }
            val f = File(dir, "kcef-boot.log")
            logFileResolved = f
            return f
        }
    }

    private fun appendLogLine(message: String) {
        val f = bootLogFile() ?: return
        runCatching {
            synchronized(logLock) {
                if (f.length() > 1_000_000L) {
                    val old = File(f.parentFile, "kcef-boot.log.1")
                    if (old.isFile) old.delete()
                    f.renameTo(old)
                }
                f.appendText("$TAG $message\n")
            }
        }.onFailure { logFileDisabled = true }
    }

    /** STEP 3z: hint del file di log da allegare nei messaggi di errore. */
    private fun failedLogHint(): String =
        "log: ${bootLogFile()?.absolutePath ?: "kcef-boot.log nella directory dati di RiPlay"}"

    /** STEP 3z: su macOS i messaggi d'errore dicono anche DOVE sta il log. */
    private fun withLogHint(msg: String): String =
        if (isMacOS) "$msg — dettagli completi nel file ${failedLogHint()}" else msg

    private val _state = MutableStateFlow<KcefState>(KcefState.NotStarted)
    val state: StateFlow<KcefState> = _state.asStateFlow()

    @Volatile
    private var client: KCEFClient? = null

    /** Client KCEF, disponibile solo quando [state] e' [KcefState.Ready]. */
    fun clientOrNull(): KCEFClient? = client

    /** Directory di installazione del bundle CEF, accanto al database. */
    val installDir: File
        get() {
            val parent = runCatching { DesktopDbRegistry.databaseFilePath().parentFile }
                .getOrNull() ?: File(System.getProperty("user.home"), ".riplay")
            return File(parent, "kcef").apply { mkdirs() }
        }

    /**
     * STEP 2l: User-Agent IDENTICO al player Android (fork AYP,
     * WebViewYouTubePlayer.initWebView): WebView Android forza una UA desktop
     * Chrome 134 anche su telefono, cosi' il build CEF (Chromium piu' recente)
     * si presenta a YouTube con la STESSA identita' verificata che funziona
     * su Android (incluso il caso 150).
     */
    const val ANDROID_AYP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    /** Cache CEF (cookie, web storage, cache HTTP dell'iframe player). */
    private val cacheDir: File
        get() = File(installDir, "cache").apply { mkdirs() }

    @Volatile
    private var started = false

    /** Helper eseguibile del bundle (jcef_helper.exe su Windows). */
    @Volatile
    private var bundleHelperFile: File? = null

    /** libcef del bundle (dll/so/dylib). */
    @Volatile
    private var bundleLibcefFile: File? = null

    // STEP 3z (macOS): binario principale del framework CEF (l'equivalente
    // di libcef su macOS) e directory Resources del framework (.pak/locales).
    @Volatile
    private var bundleCefFrameworkFile: File? = null

    @Volatile
    private var bundleCefResourcesDir: File? = null

    @Volatile
    private var bundleScanned = false

    /** Tutte le librerie native del bundle (*.dll / *.so / *.dylib). */
    @Volatile
    private var bundleNativeFiles: List<File> = emptyList()

    /** Rapporto del preload delle natives del bundle. */
    private class PreloadReport {
        val loaded = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val missing = mutableListOf<String>()

        fun isClean(): Boolean = failed.isEmpty() && missing.isEmpty()
    }

    /**
     * Priorita' di caricamento: dipendenze foglia prima dei dipendenti
     * (chrome_elf e librerie GPU prima di libcef, libcef prima di jcef).
     */
    private fun nativePriority(name: String): Int {
        val n = name.lowercase()
        return when {
            n.startsWith("jawt") -> -1
            n.startsWith("chrome_elf") -> 0
            n.startsWith("d3dcompiler") -> 1
            n.contains("egl") -> 2
            n.contains("glesv2") -> 3
            n.contains("swiftshader") -> 4
            n.startsWith("chromium embedded framework") -> 5
            n.startsWith("libcef") -> 5
            n.startsWith("jcef") || n.startsWith("libjcef") -> 6
            else -> 7
        }
    }

    /**
     * Librerie legate alla GPU: con --disable-gpu il rendering e' software e
     * NON servono all'avvio di CEF (KCEF stesso le salta nel percorso runtime
     * quando riconosce --disable-gpu). Un loro eventuale fallimento di
     * caricamento NON deve quindi bloccare l'init.
     */
    private fun isOptionalGpuLibrary(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("egl") || n.contains("glesv2") ||
                n.contains("swiftshader") || n.startsWith("d3dcompiler") ||
                n.contains("vulkan")
    }

    /**
     * Scansione del bundle (BFS): raccoglie TUTTE le librerie native (per il
     * preload completo) e individua helper + libcef per le settings CEF.
     */
    private fun scanBundle() {
        if (bundleScanned) return
        synchronized(this) {
            if (bundleScanned) return
            val natives = mutableListOf<File>()
            val queue = ArrayDeque<File>().apply { add(installDir) }
            while (queue.isNotEmpty()) {
                val dir = queue.removeFirst()
                val files = runCatching { dir.listFiles() }.getOrNull() ?: continue
                for (f in files) {
                    if (f.isDirectory) {
                        queue.add(f)
                        continue
                    }
                    val n = f.name
                    val lower = n.lowercase()
                    if (lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib")) {
                        natives += f
                    }
                    // STEP 3z (macOS): binario principale del framework CEF e
                    // directory risorse (.pak). Su macOS NON esiste nessun file
                    // "libcef.dylib": il libcef reale e' il Mach-O interno a
                    // "Chromium Embedded Framework.framework".
                    if (lower == "chromium embedded framework" && hasFrameworkAncestor(f)) {
                        if (bundleCefFrameworkFile == null) bundleCefFrameworkFile = f
                        natives += f
                    }
                    if (lower.endsWith(".pak") && bundleCefResourcesDir == null) {
                        bundleCefResourcesDir = f.parentFile
                    }
                    if (n.startsWith("jcef_helper")) {
                        // STEP 2g: il subprocess di CEF e' un ESEGUIBILE
                        // (jcef_helper.exe su Windows): l'.exe batte la .dll
                        // omonima ("jcef_helper.dll" ordina prima) altrimenti
                        // ogni processo figlio falliva (error_code=63 FATAL).
                        // STEP 3z (macOS): vince anche l'eseguibile DENTRO
                        // jcef_helper.app rispetto a un file generico.
                        val p = helperPriority(f)
                        val current = bundleHelperFile
                        if (current == null || p > helperPriority(current)) {
                            bundleHelperFile = f
                        }
                    }
                    if (bundleLibcefFile == null &&
                        (lower == "libcef.dll" || lower == "libcef.so" || lower == "libcef.dylib")
                    ) {
                        bundleLibcefFile = f
                    }
                }
            }
            bundleNativeFiles = natives.sortedWith(
                compareBy({ nativePriority(it.name) }, { it.name })
            )
            bundleScanned = true
        }
    }

    /** Il JDK in uso contiene un JCEF integrato? Rilevazione SENZA caricare librerie. */
    fun runtimeHasJcef(): Boolean {
        val javaHome = File(System.getProperty("java.home"))
        return sequenceOf(
            File(javaHome, "bin/libcef.dll"),
            File(javaHome, "bin/jcef.dll"),
            File(javaHome, "bin/jcef_helper.exe"),
            File(javaHome, "bin/libcef.so"),
            File(javaHome, "lib/libjcef.so"),
            File(javaHome, "lib/libcef.so"),
            File(javaHome, "lib/libjcef.dylib"),
            File(javaHome, "lib/libcef.dylib")
        ).any { it.isFile }
    }

    /** Il bundle KCEF e' gia' scaricato/estratto in [installDir]? */
    fun bundleInstalled(): Boolean {
        scanBundle()
        return effectiveLibcefFile() != null
    }

    /**
     * STEP 3z: su macOS il libcef del bundle e' il binario del framework
     * "Chromium Embedded Framework.framework" (NON esiste "libcef.dylib"):
     * il vecchio check rendeva bundleInstalled() sempre falso su mac ->
     * pre-seed inutile, install.lock mai creato, KCEF ri-scaricava il
     * bundle e tutte le difese di preload restavano spente.
     */
    private fun effectiveLibcefFile(): File? =
        bundleLibcefFile ?: bundleCefFrameworkFile

    /**
     * STEP 2h: il bundle KCEF e' un pacchetto JetBrainsRuntime "jbr_jcef":
     * contiene LEGITTIMAMENTE jvm.dll/java.dll/awt.dll/jdwp.dll/jli.dll e
     * jcef_helper.dll+exe. IL CHECK "CONTAMINAZIONE JDK" DI STEP2G E' STATO
     * RIMOSSO: era SEMPRE vero sui bundle validi e causava
     * deleteRecursively() + ri-download a ogni avvio.
     */

    /**
     * Se il JDK ha un JCEF integrato e il bundle non e' ancora presente,
     * il PRIMO avvio verra' interrotto da Chromium al termine del download
     * (subprocess di versione diversa). Al riavvio il bundle sara' gia'
     * installato e l'init partira' con le natives corrette.
     */
    val needsRestartAfterFirstDownload: Boolean
        get() = runtimeHasJcef() && !bundleInstalled()

    /**
     * Pre-carica le librerie native del BUNDLE con System.load (path
     * assoluto). Su Windows il modulo mappato viene poi riusato da
     * System.loadLibrary (matching per basename), cosi' l'intera catena
     * jawt -> chrome_elf -> libcef -> jcef richiesta da KCEF/CefApp finisce
     * sulle natives del bundle, coerenti con le classi org.cef del classpath
     * (nessun mismatch JNI).
     *
     * STEP 2h: WHITELIST. Il bundle KCEF e' un pacchetto JetBrainsRuntime
     * "jbr_jcef" che contiene un intero runtime Java (jvm/java/awt/jdwp/jli/
     * extnet/websocket...): quelle librerie NON vanno caricate nel processo
     * host (che ha gia' il suo JVM; su Linux dlopen creerebbe doppioni
     * reali). La catena richiesta da CEF/init e' SOLO jawt (del JDK,
     * caricata sotto) -> chrome_elf -> libcef -> jcef. Le librerie GPU sono
     * saltate da KCEF stesso quando riconosce --disable-gpu.
     */
    private val PRELOAD_WHITELIST = listOf("chrome_elf", "libcef", "jcef")

    /**
     * STEP 3z (macOS): nel bundle mac "chrome_elf" non esiste e il libcef
     * e' il binario del framework; il ponte JNI e' "libjcef.dylib". Come
     * su Windows (step2h) le dylib del runtime Java del bundle NON vanno
     * caricate nel processo host.
     */
    private val PRELOAD_WHITELIST_MAC = listOf(
        "chromium embedded framework",
        "libjcef"
    )

    private fun preloadBundleNativeLibraries(): PreloadReport {
        // STEP 2f: jcef.dll dipende da jawt.dll (AWT). Su un thread background
        // all'avvio dell'app l'AWT puo' non essere ancora inizializzato e il
        // System.load di jcef fallirebbe con "dependent libraries not found",
        // facendo abortire l'init. Toolkit.getDefaultToolkit() carica jawt +
        // natives AWT; il load esplicito di jawt e' la cintura di sicurezza
        // (jawt e' del JDK, non del bundle: nessun rischio di mismatch).
        runCatching { java.awt.Toolkit.getDefaultToolkit() }
        runCatching {
            val javaHome = File(System.getProperty("java.home"))
            sequenceOf(
                File(javaHome, "bin/jawt.dll"),
                File(javaHome, "lib/libjawt.so"),
                File(javaHome, "lib/libjawt.dylib")
            ).firstOrNull { it.isFile }?.let { System.load(it.absolutePath) }
        }

        val report = PreloadReport()
        scanBundle()
        // STEP 2h: whitelist (vedi KDoc sopra): nessun load del runtime Java
        // del bundle; jcef_helper (dll/exe) e' il SUBPROCESS, non una libreria
        // da pre-caricare nel processo host.
        // STEP 3z: su macOS whitelist dedicata (framework CEF + libjcef):
        // il bundle mac contiene un intero runtime Java le cui dylib NON
        // vanno caricate nel processo host (equivalente step2h su Windows).
        val whitelist = if (isMacOS) PRELOAD_WHITELIST_MAC else PRELOAD_WHITELIST
        val natives = bundleNativeFiles.filter { f ->
            val n = f.name.lowercase()
            !n.startsWith("jcef_helper") && whitelist.any { n.startsWith(it) }
        }
        if (natives.isEmpty()) {
            report.missing += "<bundle assente: nessuna libreria nativa trovata>"
            return report
        }
        val failed = mutableListOf<File>()
        for (f in natives) {
            val ok = runCatching { System.load(f.absolutePath) }.isSuccess
            if (ok) report.loaded += f.name else failed += f
        }
        // Seconda passata: le dipendenze mappate in prima passata possono
        // sbloccare i caricamenti falliti (ordini di dipendenza residui).
        for (f in failed) {
            if (runCatching { System.load(f.absolutePath) }.isSuccess) {
                report.loaded += f.name
            } else if (isOptionalGpuLibrary(f.name)) {
                log("preload: ${f.name} non caricata (GPU opzionale, non bloccante con --disable-gpu)")
            } else {
                log("preload: ${f.name} non caricata (non bloccante)")
            }
        }
        // Verifica delle librerie OBBLIGATORIE (prefisso basename).
        // STEP 3z: "chrome_elf" esiste SOLO su Windows (prima era richiesta
        // anche su mac/linux e l'init abortiva sempre); "libcef" su macOS e'
        // il binario del framework (nome diverso: check via effectiveLibcef).
        if (isWindowsOS) {
            val present = natives.any { it.name.lowercase().startsWith("chrome_elf") }
            val loadedOk = report.loaded.any { it.lowercase().startsWith("chrome_elf") }
            if (!present) report.missing += "chrome_elf (assente dal bundle)"
            else if (!loadedOk) report.failed += "chrome_elf (caricamento non riuscito)"
        }
        if (effectiveLibcefFile() == null) {
            report.missing += "libcef (assente dal bundle)"
        } else {
            val libcefLoadedOk = report.loaded.any {
                it.lowercase().startsWith("libcef") ||
                        it.lowercase().startsWith("chromium embedded framework")
            }
            // STEP 3z: su macOS il caricamento e' best-effort (dyld risolve
            // @rpath a modo suo e comunque KCEF fa SystemBootstrap da
            // installDir): non blocchiamo l'init se il preload non va a segno.
            if (!libcefLoadedOk && !isMacOS) {
                report.failed += "libcef (caricamento non riuscito)"
            }
        }
        // Il ponte JNI deve coincidere con le classi org.cef: su un JDK
        // con JCEF integrato DEVE arrivare dal bundle (mismatch altrimenti).
        // Su JDK pulito un eventuale fallimento di jcef lascia procedere
        // il loader dedicato di KCEF (SystemBootstrap da installDir).
        if (runtimeHasJcef()) {
            val present = natives.any {
                val n = it.name.lowercase()
                n.startsWith("jcef") || n.startsWith("libjcef")
            }
            val loadedOk = report.loaded.any {
                val n = it.lowercase()
                n.startsWith("jcef") || n.startsWith("libjcef")
            }
            if (!present) report.missing += "jcef (assente dal bundle)"
            else if (!loadedOk) report.failed += "jcef (caricamento non riuscito)"
        }
        return report
    }

    /** Sorgente delle classi org.cef effettivamente sul classpath. */
    private fun orgCefSource(): String = runCatching {
        org.cef.CefApp::class.java.protectionDomain.codeSource.location.toString()
    }.getOrDefault("<sconosciuta>")

    private fun logEnvironment() {
        log("bootstrap $BOOTSTRAP_VERSION - build corrente se compare QUESTA riga")
        log("os=${System.getProperty("os.name")} ${System.getProperty("os.arch")} " +
                "java=${System.getProperty("java.version")} " +
                "java.home=${System.getProperty("java.home")}")
        log("JDK contiene JCEF integrato: ${runtimeHasJcef()}")
        log("classi org.cef caricate da: ${orgCefSource()}")
        log("directory bundle KCEF: ${installDir.absolutePath}")
        scanBundle()
        log("bundle: libcef=${bundleLibcefFile?.path ?: "ASSENTE"} " +
                "helper=${bundleHelperFile?.path ?: "ASSENTE"} " +
                "natives=${bundleNativeFiles.size}")
        if (isMacOS) {
            log("bundle mac: framework=${bundleCefFrameworkFile?.path ?: "ASSENTE"} " +
                    "arch=${bundleCefFrameworkFile?.let { machoArch(it) } ?: "n/d"} " +
                    "resources=${bundleCefResourcesDir?.path ?: "ASSENTE"}")
        }
        log("install.lock: ${File(installDir, "install.lock").isFile}")
    }

    /**
     * STEP 2o: handler GLOBALE degli uncaught exception. Il log utente v14
     * mostrava solo "Exception in thread Thread-1267 java.lang.NoSuchMethod
     * Error: open" senza stack (il default handler stampa lo stack, ma un
     * errore lanciato dal NATIVO via JNI non ha frame Java): installando un
     * handler proprio logghiamo classe, messaggio e stack COMPLETO nel
     * canale [RiPlay-KCEF] (stesso flusso del resto della diagnostica), poi
     * deleghiamo all'eventuale handler pre-esistente. Cosi' ogni futuro
     * problema mostrera' nel log utente l'origine esatta (classe+riga).
     * Installato UNA volta sola (flag volatile, idempotente).
     */
    @Volatile
    private var uncaughtHandlerInstalled = false

    private fun installGlobalUncaughtExceptionHandler() {
        if (uncaughtHandlerInstalled) return
        synchronized(this) {
            if (uncaughtHandlerInstalled) return
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching {
                    log("UNCAUGHT su thread '${thread.name}': " +
                            "${throwable.javaClass.name}: ${throwable.message ?: "<nessun messaggio>"}")
                    val stack = throwable.stackTrace
                    if (stack.isEmpty()) {
                        log("UNCAUGHT (stack Java vuoto: eccezione lanciata dal codice nativo/JNI)")
                    } else {
                        stack.take(20).forEach { log("UNCAUGHT   at $it") }
                        if (stack.size > 20) log("UNCAUGHT   ... altri ${stack.size - 20} frame")
                    }
                    throwable.cause?.let { cause ->
                        log("UNCAUGHT causata da: ${cause.javaClass.name}: ${cause.message ?: "?"}")
                    }
                }
                previous?.uncaughtException(thread, throwable)
            }
            uncaughtHandlerInstalled = true
            log("handler globale uncaught exception installato (stack completi nel log)")
        }
    }

    /**
     * Avvia installazione + inizializzazione in background (idempotente,
     * non bloccante: sicuro chiamarlo all'avvio dell'app).
     */
    // ---------------------------------------------------------------------------
    // STEP 3q/3s: CEF PRE-SEMINATO DALL'INSTALLER (deb/msi/dmg).
    //
    // La build desktop puo' includere l'archivio jbr_jcef (lo STESSO che
    // KCEF scaricherebbe: scripts/fetch-kcef-bundle.sh/.ps1 usa la SELEZIONE
    // IDENTICA a KCEFBuilder.GitHub) dentro l'applicazione via
    // appResourcesRootDir: finisce in <app>/resources/kcef/ e a runtime e'
    // raggiungibile con la proprieta' di sistema
    // "compose.application.resources.dir" impostata dal launcher Compose.
    //
    // Comportamento:
    //  - archivio assente -> nessuna azione (come prima: KCEF scarichera');
    //  - bundle installato + preseed.stamp UGUALE -> salto immediato;
    //  - bundle installato SENZA stamp (scaricato da KCEF) -> MAI toccato;
    //  - stamp DIVERSO -> AGGIORNAMENTO (installDir cancellata e ri-estratta);
    //  - estrazione: 'tar' di sistema (Win10+ include bsdtar) con fallback a
    //    un lettore tar minimale in Kotlin (ustar 512, GNU 'L', pax 'x',
    //    symlink best-effort, guard path traversal);
    //  - layout finale REPLICA di TarGzExtractor+move di KCEF (win: lib+bin
    //    -> root; linux: lib -> root; mac: Contents/Home/lib -> root +
    //    Contents/Frameworks -> root/Frameworks);
    //  - preseed.stamp = nome|size|lastModified dell'archivio;
    //  - QUALUNQUE errore = ripristino e fallback COMPLETO al download KCEF.
    // ---------------------------------------------------------------------------
    private const val PRESEED_STAMP_FILE = "preseed.stamp"

    /** Archivio CEF incluso nel pacchetto (resources: kcef con archivio .tar.gz), se presente. */
    private fun packagedKcefArchive(): File? = runCatching {
        val resDir = System.getProperty("compose.application.resources.dir") ?: return null
        val dir = File(resDir, "kcef")
        dir.listFiles { f -> f.isFile && f.name.startsWith("jbr_jcef") && f.name.endsWith(".tar.gz") }
            ?.maxByOrNull { it.lastModified() }
    }.getOrNull()?.takeIf { it.isFile && it.length() > 0L }

    private fun preseedStampFile(): File = File(installDir, PRESEED_STAMP_FILE)

    private fun stampOf(archive: File): String =
        "${archive.name}|${archive.length()}|${archive.lastModified()}"

    private fun preSeedPackagedBundle() {
        val archive = packagedKcefArchive() ?: run {
            log("pre-seed: nessun archivio CEF nel pacchetto (il download normale resta disponibile)")
            return
        }
        val want = stampOf(archive)
        val stampFile = preseedStampFile()
        if (bundleInstalled()) {
            val have = runCatching { stampFile.readText().trim() }.getOrDefault("")
            when {
                have == want -> {
                    log("pre-seed: bundle gia' estratto dall'installer (stamp corrisponde): salto")
                    return
                }
                have.isNotEmpty() -> log("pre-seed: bundle da archivio DIVERSO ($have -> $want): AGGIORNAMENTO")
                else -> {
                    log("pre-seed: bundle esistente scaricato da KCEF (senza stamp): MAI toccato")
                    return
                }
            }
            // aggiornamento: estrazione pulita dall'archivio nuovo
            runCatching { installDir.deleteRecursively() }
        }
        _state.value = KcefState.Installing
        log("pre-seed: estrazione di ${archive.name} (${archive.length()} byte) in ${installDir.absolutePath}")
        try {
            installDir.mkdirs()
            extractTarGzWithSystemTar(archive, installDir)
            applyPreseedLayout()
            stampFile.writeText(want)
            log("pre-seed: OK (stamp scritto: $want)")
        } catch (t: Throwable) {
            log("pre-seed FALLITA (${t.javaClass.simpleName}: ${t.message}): fallback COMPLETO al download KCEF")
            runCatching { installDir.deleteRecursively() }
            runCatching { stampFile.delete() }
        }
    }

    /** Estrazione con il 'tar' di sistema (Win10+ include bsdtar); fallback Kotlin. */
    private fun extractTarGzWithSystemTar(archive: File, dest: File) {
        val proc = ProcessBuilder(
            listOf("tar", "-xzf", archive.absolutePath, "-C", dest.absolutePath)
        ).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        val code = proc.waitFor()
        if (code == 0) return
        log("pre-seed: tar di sistema fallita (code=$code): ${out.take(200)} -> lettore tar Kotlin")
        extractTarGzKotlin(archive, dest)
    }

    /** Replica del layout TarGzExtractor+move di KCEF (dipendente dal sistema). */
    private fun applyPreseedLayout() {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> {
                File(installDir, "lib").takeIf { it.isDirectory }?.let { moveDirInto(it, installDir) }
                File(installDir, "bin").takeIf { it.isDirectory }?.let { moveDirInto(it, installDir) }
            }
            os.contains("mac") || os.contains("darwin") -> {
                val homeLib = File(installDir, "Contents/Home/lib")
                if (homeLib.isDirectory) moveDirInto(homeLib, installDir)
                val frameworks = File(installDir, "Contents/Frameworks")
                if (frameworks.isDirectory) {
                    val target = File(installDir, "Frameworks")
                    target.mkdirs()
                    moveDirInto(frameworks, target)
                }
            }
            else -> {
                File(installDir, "lib").takeIf { it.isDirectory }?.let { moveDirInto(it, installDir) }
            }
        }
    }

    /** Sposta il CONTENUTO di [src] dentro [targetDir] (rename, fallback copy). */
    private fun moveDirInto(src: File, targetDir: File) {
        for (c in src.listFiles().orEmpty()) {
            val dst = File(targetDir, c.name)
            if (dst.exists()) {
                // sovrascrittura deterministica (aggiornamento bundle)
                if (dst.isDirectory) dst.deleteRecursively() else dst.delete()
            }
            if (!c.renameTo(dst)) {
                if (c.isDirectory) {
                    dst.mkdirs()
                    moveDirInto(c, dst)
                    c.deleteRecursively()
                } else {
                    c.copyTo(dst, overwrite = true)
                    c.delete()
                }
            }
        }
        src.deleteRecursively()
    }

    // --------------------------- lettore tar minimale (fallback Kotlin) -----
    private fun readFully(input: java.io.InputStream, buffer: ByteArray): Int {
        var off = 0
        while (off < buffer.size) {
            val n = input.read(buffer, off, buffer.size - off)
            if (n < 0) return if (off == 0) -1 else off
            off += n
        }
        return off
    }

    private fun tarName(header: ByteArray): String {
        var end = header.size
        for (i in header.indices) {
            if (header[i] == 0.toByte()) { end = i; break }
        }
        return String(header, 0, end, Charsets.UTF_8).trimEnd(' ')
    }

    private fun tarOctal(header: ByteArray, off: Int, len: Int): Long {
        var s = String(header, off, len, Charsets.US_ASCII).trim(' ', 0.toChar())
        if (s.isEmpty()) return 0L
        if (s.last() == '+') s = s.dropLast(1)
        return s.toLongOrNull(8) ?: 0L
    }

    /** Salta in avanti [size] byte di payload tar (con padding a 512). */
    private fun skipTarPayload(gz: java.io.InputStream, size: Long) {
        var left = size
        val buf = ByteArray(4096)
        while (left > 0) {
            val take = minOf(left, buf.size.toLong()).toInt()
            val got = readFully(gz, buf.copyOfRange(0, take))
            if (got <= 0) return
            left -= got
        }
        val pad = ((512 - (size % 512)) % 512).toInt()
        if (pad > 0) {
            val padBuf = ByteArray(pad)
            readFully(gz, padBuf)
        }
    }

    private fun extractTarGzKotlin(archive: File, dest: File) {
        val canonDest = dest.canonicalFile
        java.util.zip.GZIPInputStream(archive.inputStream().buffered()).use { gz ->
            val header = ByteArray(512)
            var longName: String? = null
            while (true) {
                val n = readFully(gz, header)
                if (n != 512) break
                if (header.all { it == 0.toByte() }) continue
                val rawName = tarName(header.copyOfRange(0, 100))
                val type = header[156].toInt().toChar()
                val size = tarOctal(header, 124, 12)
                val prefix = tarName(header.copyOfRange(345, 500))
                var name = longName ?: rawName
                longName = null
                if (prefix.isNotEmpty() && !name.contains("/")) name = "$prefix/$name"

                when (type) {
                    'L' -> {
                        // GNU longname: il PROSSIMO header ha nome vuoto
                        val buf = ByteArray(size.toInt())
                        if (readFully(gz, buf) != size.toInt()) break
                        longName = buf.toString(Charsets.UTF_8).trimEnd(0.toChar())
                        val pad = ((512 - (size % 512)) % 512).toInt()
                        if (pad > 0) { val p = ByteArray(pad); readFully(gz, p) }
                        continue
                    }
                    'x', 'g' -> {
                        // pax header: estrai il campo path (se presente)
                        val buf = ByteArray(size.toInt())
                        if (readFully(gz, buf) != size.toInt()) break
                        if (type == 'x') {
                            val text = buf.toString(Charsets.UTF_8)
                            val m = Regex("(?m)^\\d+ path=(.+)").find(text)
                            if (m != null) longName = m.groupValues[1].trimEnd()
                        }
                        val pad = ((512 - (size % 512)) % 512).toInt()
                        if (pad > 0) { val p = ByteArray(pad); readFully(gz, p) }
                        continue
                    }
                }

                val clean = name.removePrefix("./").trimStart('/')
                if (clean.isEmpty()) {
                    if (size > 0) skipTarPayload(gz, size)
                    continue
                }
                val target = File(canonDest, clean).canonicalFile
                if (!target.path.startsWith(canonDest.path)) {
                    log("pre-seed: percorso scartato (path traversal): $clean")
                    if (size > 0) skipTarPayload(gz, size)
                    continue
                }
                when (type) {
                    '5' -> target.mkdirs()
                    '2' -> {
                        // symlink: best-effort (i link del bundle seguono il
                        // layout originale; se falliscono l'installazione resta
                        // valida perche' i file reali sono stati gia' estratti)
                        val linkName = tarName(header.copyOfRange(157, 257))
                        runCatching {
                            target.parentFile?.mkdirs()
                            java.nio.file.Files.createSymbolicLink(
                                target.toPath(),
                                java.nio.file.Paths.get(linkName)
                            )
                        }
                    }
                    '0', 0.toChar() -> {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { fos ->
                            var left = size
                            val buf = ByteArray(4096)
                            while (left > 0) {
                                val take = minOf(left, buf.size.toLong()).toInt()
                                val got = readFully(gz, buf.copyOfRange(0, take))
                                if (got <= 0) break
                                fos.write(buf, 0, got)
                                left -= got
                            }
                        }
                        val pad = ((512 - (size % 512)) % 512).toInt()
                        if (pad > 0) { val p = ByteArray(pad); readFully(gz, p) }
                    }
                    else -> if (size > 0) skipTarPayload(gz, size)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // STEP 3z: supporto macOS del bundle CEF. Il bundle jbr_jcef "macos" ha
    // un layout diverso da Windows/Linux: il libcef e' il Mach-O dentro
    // "Chromium Embedded Framework.framework", i .pak/locales stanno nelle
    // Resources del framework e il subprocess e' l'eseguibile dentro
    // jcef_helper.app. Prima di questo step tutto il percorso bundle/
    // preload/settings era scritto solo per win/linux: su macOS il pre-seed
    // veniva considerato "bundle non installato", il lock non veniva creato,
    // KCEF ri-scaricava ~200MB e le difese restavano spente.
    // -------------------------------------------------------------------------
    private val isMacOS: Boolean
        get() {
            val os = System.getProperty("os.name").lowercase()
            return os.contains("mac") || os.contains("darwin")
        }

    private val isWindowsOS: Boolean
        get() = System.getProperty("os.name").lowercase().contains("win")

    /** Antenato ".framework" entro 6 livelli (binario dentro un bundle macOS). */
    private fun hasFrameworkAncestor(f: File): Boolean {
        var d: File? = f.parentFile
        var hops = 0
        while (d != null && hops < 6) {
            if (d.name.lowercase().endsWith(".framework")) return true
            d = d.parentFile
            hops++
        }
        return false
    }

    /**
     * Priorita' del subprocess CEF: 3 = jcef_helper.exe (Windows, lezione
     * step2g: l'.exe batte la .dll omonima); 2 = eseguibile dentro un .app
     * (macOS); 1 = file generico. Vince il punteggio piu' alto.
     */
    private fun helperPriority(f: File): Int {
        val lower = f.name.lowercase()
        return when {
            lower.endsWith(".exe") -> 3
            f.path.contains(".app/Contents/MacOS/") -> 2
            else -> 1
        }
    }

    /**
     * STEP 3z: architettura del Mach-O leta dall'header (magic + cputype,
     * nessun tool esterno): "arm64" | "x86_64" | "universale (fat)" | null.
     */
    // Magic Mach-O come Int SIGNED (0xFEEDFACF -> -16843705, 0xCAFEBABE ->
    // -889275714): i literal esadecimali oltre Int.MAX sarebbero Long.
    private const val MACHO_MAGIC_64 = -16843705       // 0xFEEDFACF MH_MAGIC_64
    private const val MACHO_MAGIC_FAT = -889275714     // 0xCAFEBABE FAT_MAGIC
    private const val MACHO_CPU_ARM64 = 0x0100000C     // CPU_TYPE_ARM64
    private const val MACHO_CPU_X86_64 = 0x01000007    // CPU_TYPE_X86_64

    private fun machoArch(file: File): String? = runCatching {
        file.inputStream().use { ins ->
            val h = ByteArray(8)
            var off = 0
            while (off < 8) {
                val n = ins.read(h, off, 8 - off)
                if (n < 0) return@runCatching null
                off += n
            }
            fun le(at: Int): Int =
                (h[at].toInt() and 0xFF) or ((h[at + 1].toInt() and 0xFF) shl 8) or
                        ((h[at + 2].toInt() and 0xFF) shl 16) or ((h[at + 3].toInt() and 0xFF) shl 24)
            when (le(0)) {
                MACHO_MAGIC_64 -> when (le(4)) {
                    MACHO_CPU_ARM64 -> "arm64"
                    MACHO_CPU_X86_64 -> "x86_64"
                    else -> "sconosciuta"
                }
                MACHO_MAGIC_FAT -> "universale (fat)"
                else -> null
            }
        }
    }.getOrNull()

    private fun jvmArchName(): String {
        val a = System.getProperty("os.arch").lowercase()
        return when {
            a.contains("aarch64") || a.contains("arm64") -> "arm64"
            a.contains("x86_64") || a.contains("amd64") -> "x86_64"
            else -> a
        }
    }

    /**
     * STEP 3z: il lettore tar di fallback in Kotlin NON preserva i permessi:
     * i binari estratti uscivano senza il bit +x e i subprocess CEF non
     * potevano partire. Sweep best-effort sui percorsi eseguibili noti.
     */
    private fun fixBundleExecPermissions() {
        if (isWindowsOS) return
        var fixed = 0
        runCatching {
            scanBundle()
            bundleHelperFile?.takeIf { it.isFile && !it.canExecute() }?.let {
                if (it.setExecutable(true)) fixed++
            }
            bundleCefFrameworkFile?.takeIf { it.isFile && !it.canExecute() }?.let {
                if (it.setExecutable(true)) fixed++
            }
            val queue = ArrayDeque<File>().apply { add(installDir) }
            while (queue.isNotEmpty()) {
                val dir = queue.removeFirst()
                val files = runCatching { dir.listFiles() }.getOrNull() ?: continue
                for (f in files) {
                    if (f.isDirectory) {
                        queue.add(f)
                        continue
                    }
                    if (f.isFile && !f.canExecute() &&
                        f.path.contains(".app/Contents/MacOS/")
                    ) {
                        if (f.setExecutable(true)) fixed++
                    }
                }
            }
        }
        if (fixed > 0) log("permessi esecuzione ripristinati su $fixed file del bundle")
    }

    /**
     * STEP 3z: rimozione best-effort della quarantena Gatekeeper dal bundle
     * estratto (xattr). Un SIGKILL silenzioso sui subprocess CEF per colpa
     * dell'attributo com.apple.quarantine e' indistinguibile da un crash.
     */
    private fun removeBundleQuarantine() {
        if (!isMacOS) return
        runCatching {
            val proc = ProcessBuilder(
                listOf("xattr", "-rd", "com.apple.quarantine", installDir.absolutePath)
            ).redirectErrorStream(true).start()
            proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            log("rimozione quarantena bundle (xattr): exit=$code")
        }.onFailure {
            log("xattr non disponibile: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    fun start() {
        installGlobalUncaughtExceptionHandler()
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            _state.value = KcefState.Locating
            Thread({
                try {
                    logEnvironment()

                    // STEP 3q/3s (richiesta utente): CEF PRE-SEMINATO nell'installer
                    // (deb/msi/dmg): l'archivio jbr_jcef incluso nell'app viene
                    // estratto qui nella directory dati utente PRIMA della catena
                    // KCEF -> NESSUN download ~200MB al primo avvio. Qualunque
                    // problema = fallback COMPLETO al download normale.
                    preSeedPackagedBundle()

                    // STEP 3z (macOS): permessi di esecuzione e quarantena
                    // PRIMA di qualunque uso del bundle (il tar di fallback
                    // Kotlin non preserva i bit +x; Gatekeeper ucciderebbe i
                    // subprocess con SIGKILL silenzioso).
                    if (isMacOS && bundleInstalled()) {
                        fixBundleExecPermissions()
                        removeBundleQuarantine()
                    }

                    // STEP 2h: KCEFBuilder.install cancella l'INTERA installDir
                    // e ri-scarica il bundle se manca install.lock (bytecode
                    // verificato: existsSafely -> deleteDir -> downloadPackage
                    // -> extract -> createSafely). Un bundle COMPLETO non va
                    // mai cancellato: se il lock manca (AV, interruzione,
                    // build precedenti) lo pre-creiamo qui, cosi' KCEF salta
                    // install e riutilizza il bundle gia' scaricato.
                    val lockFile = File(installDir, "install.lock")
                    if (bundleInstalled() && !lockFile.isFile) {
                        val exeHelper = bundleHelperFile?.takeIf {
                            !it.name.lowercase().endsWith(".dll")
                        }
                        if (exeHelper != null) {
                            val created = runCatching { lockFile.createNewFile() }.getOrDefault(false)
                            log("install.lock ASSENTE ma bundle completo (libcef + ${exeHelper.name}): " +
                                    if (created) "lock ricreato -> ri-download EVITATO"
                                    else "impossibile creare il lock -> KCEF re-installera' il bundle")
                        } else {
                            log("install.lock assente e helper eseguibile mancante: bundle incompleto, lascio KCEF reinstallare")
                        }
                    } else if (bundleInstalled()) {
                        log("install.lock presente: bundle gia' installato, NESSUN ri-download")
                    }

                    // FIX 1+4: natives del bundle caricate PRIMA di qualunque init
                    // (il bundle potrebbe gia' essere presente da un avvio precedente).
                    // STEP 2e: preload VERIFICATO - senza chrome_elf/libcef/jcef del
                    // bundle l'init viene bloccato con un messaggio esplicito.
                    if (bundleInstalled()) {
                        val report = preloadBundleNativeLibraries()
                        log("preload natives bundle: caricate=${report.loaded.size} " +
                                "fallite=${report.failed.size} mancanti=${report.missing.size}")
                        if (report.loaded.isNotEmpty()) {
                            log("preload OK: ${report.loaded.joinToString(", ")}")
                        }
                        if (!report.isClean()) {
                            val detail = buildString {
                                if (report.failed.isNotEmpty()) {
                                    append("caricamento non riuscito: ${report.failed.joinToString("; ")}")
                                }
                                if (report.missing.isNotEmpty()) {
                                    if (isNotEmpty()) append(" | ")
                                    append("assenti: ${report.missing.joinToString("; ")}")
                                }
                            }
                            log("ABORT init: bundle incompleto o incompatibile -> $detail")
                            _state.value = KcefState.Failed(
                                withLogHint(
                                    "Il browser integrato (KCEF) non puo' avviarsi: $detail. " +
                                            "Controlla il log per i dettagli e reinstalla/aggiorna RiPlay."
                                )
                            )
                            return@Thread
                        }
                    } else {
                        log("bundle non ancora installato: il primo avvio lo scaricherà")
                    }

                    // STEP 3z (macOS): mismatch di architettura JVM vs CEF ->
                    // messaggio esplicito invece di un crash nativo muto di
                    // dyld (un DMG costruito su runner arm64 contiene JVM+CEF
                    // arm64: su un Mac Intel non puo' funzionare, e viceversa).
                    if (isMacOS && bundleInstalled()) {
                        val bundleArch = effectiveLibcefFile()?.let { machoArch(it) }
                        val jvmArch = jvmArchName()
                        log("architetture: jvm=$jvmArch cef=${bundleArch ?: "n/d"}")
                        if ((bundleArch == "arm64" || bundleArch == "x86_64") && bundleArch != jvmArch) {
                            log("ABORT init: MISMATCH architettura CEF($bundleArch) vs JVM($jvmArch)")
                            _state.value = KcefState.Failed(
                                withLogHint(
                                    "Il browser integrato (KCEF) e' compilato per $bundleArch ma questo Mac " +
                                            "esegue Java $jvmArch: scarica il pacchetto RiPlay che corrisponde " +
                                            "all'architettura del tuo Mac."
                                )
                            )
                            return@Thread
                        }
                    }

                    KCEF.initBlocking(
                        builder = {
                            installDir(installDir)
                            // STEP 2p: nessun AppHandler custom (rimosso quello
                            // step2m per il factory https://youtube.com: canale
                            // di intercettazione abolito). KCEF usa il SUO
                            // AppHandler predefinito (factory file:// standard).
                            // FIX 5: rendering software: su molte configurazioni
                            // Windows (GPU integrata/driver datati/VM/Remote
                            // Desktop) il processo GPU non parte ("GPU process
                            // launch failed") e Chromium esce con FATAL "GPU
                            // process isn't usable". Con disable-gpu il processo
                            // GPU non viene richiesto; KCEF salta anche il
                            // preloading delle librerie GPU nel percorso runtime.
                            // STEP 2i: --autoplay-policy=no-user-gesture-required
                            // (l'app chiama loadVideoById via JS senza gesto
                            // utente nel frame: senza questo flag la policy
                            // autoplay di Chromium puo' bloccare la partenza);
                            // --disable-quic (reti aziendali bloccano UDP/443:
                            // gli stream googlevideo.com su QUIC falliscono con
                            // "An error occurred" nel player YT; su TCP funziona).
                            args(
                                "--disable-gpu",
                                "--disable-gpu-compositing",
                                "--no-sandbox",
                                "--autoplay-policy=no-user-gesture-required",
                                "--disable-quic"
                            )
                            log("builder args: --disable-gpu --disable-gpu-compositing --no-sandbox " +
                                    "--autoplay-policy=no-user-gesture-required --disable-quic")
                            // FIX 2 (STEP 2e): settings esplicite. Se il bundle e'
                            // presente -> path verso il bundle; se NON e' ancora
                            // stato estratto -> campi a STRINGA VUOTA: KCEF
                            // (CefInitializer) autofilla i campi vuoti verso
                            // installDir, invece di lasciare i default del JDK
                            // (JCefAppConfig -> java.home -> subprocess sbagliati).
                            settings {
                                cachePath = cacheDir.absolutePath
                                windowlessRenderingEnabled = false
                                noSandbox = true
                                // STEP 2l: replica l'UA del player Android (AYP)
                                userAgent = ANDROID_AYP_USER_AGENT
                                scanBundle()
                                val helper = bundleHelperFile
                                val libcef = bundleLibcefFile
                                if (isMacOS) {
                                    // STEP 3z (macOS): subprocess = eseguibile
                                    // interno a jcef_helper.app; risorse/locales
                                    // = directory Resources del framework CEF
                                    // (accanto all'helper su mac NON ci sono i pak).
                                    val fwRes = bundleCefResourcesDir
                                    browserSubProcessPath = helper?.absolutePath ?: ""
                                    resourcesDirPath = fwRes?.absolutePath ?: ""
                                    val loc = fwRes?.let { File(it, "locales") }
                                    localesDirPath = loc?.takeIf { l -> l.isDirectory }?.absolutePath ?: ""
                                    log("settings mac: subprocess=${helper?.path ?: "ASSENTE"} " +
                                            "framework=${bundleCefFrameworkFile?.path ?: "ASSENTE"} " +
                                            "resources=${fwRes?.path ?: "ASSENTE"} " +
                                            "locales=${localesDirPath?.takeIf { it.isNotEmpty() } ?: "ASSENTE"}")
                                } else if (helper != null && libcef != null) {
                                    browserSubProcessPath = helper.absolutePath
                                    helper.parentFile?.let { res ->
                                        resourcesDirPath = res.absolutePath
                                        val loc = File(res, "locales")
                                        localesDirPath =
                                            if (loc.isDirectory) loc.absolutePath else ""
                                    }
                                    log("settings: subprocess=${helper.path} resources=${helper.parent} " +
                                            "cache=${cacheDir.path} windowless=false userAgent=Chrome/134 (AYP)")
                                } else {
                                    browserSubProcessPath = ""
                                    resourcesDirPath = ""
                                    localesDirPath = ""
                                    log("settings: campi path VUOTI (bundle assente) -> autofill KCEF verso installDir " +
                                            "(userAgent=Chrome/134 AYP resta impostata)")
                                }
                                // STEP 3z: log nativo Chromium su file (i FATAL dei
                                // subprocess altrimenti sono invisibili nel .app).
                                // Best-effort via reflection: il campo log_file
                                // esiste in CefSettings ma la DSL KCEF non lo espone.
                                runCatching {
                                    val s = this
                                    val cefLog = File(cacheDir, "cef-debug.log")
                                    var cls: Class<*>? = s::class.java
                                    var done = false
                                    while (cls != null && !done) {
                                        for (fname in listOf("logFile", "log_file")) {
                                            if (done) break
                                            runCatching {
                                                val fld = cls.getDeclaredField(fname)
                                                fld.isAccessible = true
                                                fld.set(s, cefLog.absolutePath)
                                                done = true
                                            }
                                        }
                                        cls = cls.superclass
                                    }
                                    log("cef-debug.log nativo: " +
                                            if (done) "impostato -> ${cefLog.absolutePath}"
                                            else "campo logFile non disponibile nella settings KCEF (ignorato)")
                                }
                            }
                            progress {
                                onDownloading { percent ->
                                    log("download bundle: $percent%")
                                    _state.value = KcefState.Downloading(percent)
                                }
                                onExtracting {
                                    log("estrazione bundle...")
                                    _state.value = KcefState.Extracting
                                }
                                onInstall {
                                    log("installazione bundle...")
                                    _state.value = KcefState.Installing
                                }
                                onInitializing {
                                    log("inizializzazione CEF...")
                                    _state.value = KcefState.Initializing
                                }
                            }
                        },
                        onError = { t ->
                            log("onError: ${t?.message ?: t?.javaClass?.name ?: "sconosciuto"}")
                            _state.value = KcefState.Failed(
                                withLogHint(
                                    t?.message ?: t?.javaClass?.simpleName ?: "Errore inizializzazione KCEF"
                                )
                            )
                        },
                        onRestartRequired = {
                            log("onRestartRequired")
                            _state.value = KcefState.Failed(
                                withLogHint(
                                    "Riavvia RiPlay per completare l'installazione del motore di riproduzione (KCEF)."
                                )
                            )
                        }
                    )
                    log("KCEF.initBlocking completato")
                    // STEP 2g: su Windows CefApp.startup() e' ASINCRONA (thread
                    // CefStartup): initBlocking puo' tornare con lo stato ancora
                    // INITIALIZING (visto nel log v6: client e browser creati
                    // PRIMA del contesto nativo -> "can't invoke native method
                    // ... before native context initialized", chiamate JS
                    // perse). Attendiamo lo stato INITIALIZED prima di creare
                    // client e browser.
                    val deadline = System.currentTimeMillis() + 180_000L
                    while (true) {
                        val appState = runCatching { org.cef.CefApp.getState() }.getOrNull()
                        when (appState) {
                            org.cef.CefApp.CefAppState.INITIALIZED -> break
                            org.cef.CefApp.CefAppState.SHUTTING_DOWN,
                            org.cef.CefApp.CefAppState.TERMINATED -> {
                                log("init CEF interrotta: stato=$appState")
                                _state.value = KcefState.Failed(
                                    withLogHint(
                                        "L'inizializzazione del browser integrato e' stata interrotta " +
                                                "(stato: $appState). Controlla le righe [RiPlay-KCEF] nel log."
                                    )
                                )
                                return@Thread
                            }
                            else -> {
                                if (System.currentTimeMillis() > deadline) {
                                    log("timeout attesa INITIALIZED: ultimo stato=$appState")
                                    _state.value = KcefState.Failed(
                                        withLogHint(
                                            "Timeout attesa inizializzazione browser (stato: $appState). " +
                                                    "Controlla le righe [RiPlay-KCEF] nel log."
                                        )
                                    )
                                    return@Thread
                                }
                                Thread.sleep(200)
                            }
                        }
                    }
                    log("CefApp INITIALIZED confermato: creazione client")
                    // STEP 2j: consenso EU seminato PRIMA del primo uso del browser
                    seedConsentCookies()
                    // Client creato SOLO dopo init completata: la UI monta
                    // il browser solo quando lo stato e' Ready.
                    val newClient = KCEF.newClientBlocking()
                    // STEP 2r: politica del contesto embed (Referer apex sulle
                    // richieste /embed/, contesto Android). NESSUN CefResource
                    // Handler custom (getResourceHandler sempre null): zero
                    // rischio NoSuchMethodError open/read del glue nativo.
                    installEmbedRefererPolicy(newClient)
                    installDiagnosticsHandlers(newClient)
                    client = newClient
                    log("pagina player: HTML LOCALE con parametri sostituiti PRIMA del caricamento " +
                            "(replica WebViewYouTubePlayer.initWebView Android: segnaposto " +
                            "videoId/playerVars) servita dal server loopback e caricata come " +
                            "http://localhost (origine onesta ACCETTATA dalla politica embed " +
                            "YouTube: www.youtube.com e 127.0.0.1 sono rifiutati con errore 152, " +
                            "verifica empirica step2r); l'origin resta una variabile interna " +
                            "del player (mai un parametro dell'URL del browser)")
                    log("client KCEF creato: motore YouTube pronto")
                    _state.value = KcefState.Ready
                } catch (t: Throwable) {
                    log("INIT FALLITA: ${t.javaClass.name}: ${t.message}")
                    t.stackTrace.take(8).forEach { log("  at $it") }
                    _state.value = KcefState.Failed(withLogHint(t.message ?: t.javaClass.simpleName))
                }
            }, "kcef-init").apply { isDaemon = true }.start()
        }
    }

    /**
     * Significato dei codici onError del player YouTube IFrame API
     * (documentazione ufficiale YT Player API + comportamento reale).
     */
    fun ytErrorMeaning(code: Int): String = when (code) {
        2 -> ds("error.yt_2")
        5 -> ds("error.yt_5")
        100 -> ds("error.yt_100")
        101, 150 -> ds("error.yt_101_150")
        152 -> ds("error.yt_152")
        153 -> ds("error.yt_153")
        else -> ds("error.yt_unknown")
    }

    /**
     * STEP 2j: semina i cookie di CONSENSO EU per youtube.com PRIMA che il
     * browser incorporato li usi. Il profilo CEF nasce vuoto: senza decisione
     * di consenso YouTube puo' rifiutare la riproduzione dell'embed con
     * LOGIN_REQUIRED, che l'IFrame API segnala come onError code=2
     * ("An error occurred" con Playback ID). SOCS=CAE = consenso "rifiuta
     * non essenziali": stato valido che rimuove il muro senza tracciamento
     * aggiuntivo. CONSENT=YES e' il cookie legacy che alcuni endpoint
     * richiedono ancora. Chiamare DOPO lo stato INITIALIZED (il manager
     * cookie richiede il contesto nativo) e PRIMA della creazione del browser.
     */
    private fun seedConsentCookies() {
        runCatching {
            // JCEF 147: il singleton e' getGlobalManager() (stesso uso di
            // KCEFCookieManager nel sorgente KCEF), NON getInstance().
            val mgr = org.cef.network.CefCookieManager.getGlobalManager()
                ?: error("CefCookieManager non disponibile")
            val now = java.util.Date()
            val expires = java.util.Date(now.time + 365L * 24 * 60 * 60 * 1000)
            fun cookie(name: String, value: String, domain: String) =
                org.cef.network.CefCookie(name, value, domain, "/", true, false, now, now, true, expires)
            var ok = 0
            if (mgr.setCookie("https://www.youtube.com", cookie("SOCS", "CAE", ".youtube.com"))) ok++
            if (mgr.setCookie("https://www.youtube-nocookie.com", cookie("SOCS", "CAE", ".youtube-nocookie.com"))) ok++
            if (mgr.setCookie("https://www.youtube.com", cookie("CONSENT", "YES+cb.20210328-17-p0.en+FX+417", ".youtube.com"))) ok++
            log("cookie consenso seminati: $ok/3 (SOCS/CONSENT per youtube.com e youtube-nocookie.com)")
        }.onFailure {
            log("seed cookie consenso FALLITA: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    /**
     * STEP 2r: politica del CONTESTO EMBED. YouTube valida l'embed SOLO dal
     * Referer HTTP della richiesta della pagina embed (verifica empirica
     * step2r): accetta apex youtube.com (il valore che Android produce con
     * loadDataWithBaseURL) e rifiuta www.youtube.com e l'IP 127.0.0.1.
     * Su OGNI richiesta a https://www.youtube.com/embed/ impostiamo il
     * Referer ad apex via CefRequest.setReferrer (hook onBeforeResourceLoad,
     * prima dell'invio): il contesto dichiarato a YouTube diventa lo stesso
     * di Android. Combinazione Referer apex + parametri widget localhost
     * verificata ACCETTATA (previewPlayabilityStatus OK). getResource
     * Handler restituisce SEMPRE null: nessun CefResourceHandler custom,
     * quindi il glue nativo non chiama mai open/read (nessun rischio
     * NoSuchMethodError, lezione step2o/2p). Se l'hook non venisse mai
     * chiamato, il fallback e' naturale: il Referer localhost onesto della
     * pagina e' comunque accettato dalla politica embed.
     */
    private const val EMBED_URL_PREFIX = "https://www.youtube.com/embed/"

    /** Referer impostato sulle richieste embed: apex, identico ad Android. */
    private const val EMBED_REFERER = "https://youtube.com/"

    @Volatile
    private var embedRefererLogged = false

    /** Log una sola volta dell'iniezione CSS "nascondi condividi" (STEP 3o). */
    @Volatile
    private var embedShareHiddenLogged = false

    private fun installEmbedRefererPolicy(c: KCEFClient) {
        runCatching {
            val embedHandler = object : org.cef.handler.CefResourceRequestHandlerAdapter() {
                override fun onBeforeResourceLoad(
                    browser: org.cef.browser.CefBrowser,
                    frame: org.cef.browser.CefFrame,
                    request: org.cef.network.CefRequest
                ): Boolean {
                    runCatching {
                        if (request != null && !request.isReadOnly) {
                            request.setReferrer(
                                EMBED_REFERER,
                                org.cef.network.CefRequest.ReferrerPolicy.REFERRER_POLICY_NEVER_CLEAR_REFERRER
                            )
                            if (!embedRefererLogged) {
                                embedRefererLogged = true
                                log("contesto embed: Referer impostato a '$EMBED_REFERER' " +
                                        "(contesto Android loadDataWithBaseURL) per ${request.url.take(120)}")
                            }
                        }
                    }.onFailure {
                        log("patch Referer embed non applicata: ${it.javaClass.simpleName}: ${it.message}")
                    }
                    return false // false = continua la richiesta (RV_CONTINUE)
                }
            }
            c.addRequestHandler(object : org.cef.handler.CefRequestHandlerAdapter() {
                override fun getResourceRequestHandler(
                    browser: org.cef.browser.CefBrowser,
                    frame: org.cef.browser.CefFrame,
                    request: org.cef.network.CefRequest,
                    isNavigation: Boolean,
                    isDownload: Boolean,
                    requestInitiator: String,
                    disableDefaultHandling: org.cef.misc.BoolRef
                ): org.cef.handler.CefResourceRequestHandler? {
                    val url = request?.url ?: return null
                    // SOLO le richieste della pagina embed: tutto il resto
                    // torna al default (null = rete reale, zero impatto).
                    return if (url.startsWith(EMBED_URL_PREFIX)) embedHandler else null
                }
            })
            log("politica contesto embed installata: Referer '$EMBED_REFERER' sulle richieste " +
                    "$EMBED_URL_PREFIX* (accettata da YouTube; www.youtube.com e 127.0.0.1 rifiutati)")
        }.onFailure {
            log("installazione politica contesto embed FALLITA: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    /**
     * STEP 2i: diagnostica runtime del browser. Instrada su log:
     *  - i messaggi console JS della pagina player (ERROR/WARNING sempre,
     *    INFO solo se marcati [RiPlay-YT]: probe codec, stati ed errori del
     *    player YouTube, errori JS) - sono la fonte principale per capire
     *    perche' YouTube non riproduce;
     *  - gli errori di caricamento (net::ERR) con URL.
     * KCEFClient non installa handler Display/Load propri (sorgente
     * verificata: solo un MessageRouter per il bridge JS), quindi non
     * sostituiamo nulla di interno a KCEF.
     */
    private fun installDiagnosticsHandlers(c: KCEFClient) {
        runCatching {
            c.addDisplayHandler(object : org.cef.handler.CefDisplayHandlerAdapter() {
                override fun onConsoleMessage(
                    browser: org.cef.browser.CefBrowser,
                    level: org.cef.CefSettings.LogSeverity,
                    message: String,
                    source: String,
                    line: Int
                ): Boolean {
                    val interesting = level == org.cef.CefSettings.LogSeverity.LOGSEVERITY_ERROR ||
                            level == org.cef.CefSettings.LogSeverity.LOGSEVERITY_FATAL ||
                            level == org.cef.CefSettings.LogSeverity.LOGSEVERITY_WARNING ||
                            message.contains("[RiPlay-YT]")
                    if (interesting) {
                        log("JS ${level.name.removePrefix("LOGSEVERITY_")}: $message " +
                                "(${source.substringAfterLast('/').take(60)}:$line)")
                    }
                    // STEP 2p/2r: il marker console della pagina player e'
                    // la conferma secondaria dell'attivazione (il load end e'
                    // quella primaria).
                    PlayerHtmlServer.tryMarkActiveFromConsole(message)
                    return false
                }
            })
            log("diagnostica: display handler console installato")
        }.onFailure { log("addDisplayHandler fallita: ${it.javaClass.simpleName}: ${it.message}") }
        runCatching {
            c.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
                override fun onLoadError(
                    browser: org.cef.browser.CefBrowser,
                    frame: org.cef.browser.CefFrame,
                    errorCode: org.cef.handler.CefLoadHandler.ErrorCode,
                    errorText: String,
                    failedUrl: String
                ) {
                    if (errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_NONE) return
                    // STEP 2r: nessun ripristino da attivare (il server locale
                    // e' l'unico canale). ERR_ABORTED su frame non principali
                    // e' fisiologico (annullamenti di iframe): non loggato.
                    if (errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_ABORTED &&
                        frame?.isMain != true
                    ) return
                    log("LOAD ERROR ${errorCode}: ${errorText.take(160)} url=${failedUrl.take(200)} " +
                            "main=${frame?.isMain}")
                }

                override fun onLoadEnd(
                    browser: org.cef.browser.CefBrowser,
                    frame: org.cef.browser.CefFrame,
                    httpStatusCode: Int
                ) {
                    if (frame?.isMain == true) {
                        log("load end: http=$httpStatusCode url=${frame.url.take(200)}")
                        // STEP 2r: conferma attivazione pagina player (load end
                        // della pagina locale http://localhost).
                        PlayerHtmlServer.onMainFrameLoadEnd(browser, frame.url, httpStatusCode)
                    } else {
                        // STEP 3o (richiesta utente n.6 "nel player all'interno
                        // del browser risulta cliccabile il pulsante di
                        // condivisione, disattivalo come fatto con gli altri
                        // click"): nei FRAME EMBED di YouTube (l'iframe che
                        // contiene il player con i suoi controlli) inietta un
                        // foglio di stile che NASCONDE il pulsante CONDIVIDI
                        // (.ytp-share-button) e il pannello che apre. La regola
                        // vale anche per il DOM creato DOPO il load end: un
                        // <style> persistente e' piu' affidabile della posizione
                        // in pixel. SOLO COSMETICO: tocca il DOM dell'embed,
                        // non il bridge riplay*/la catena di riproduzione; il
                        // fallback a livello pagina resta il guard trasparente
                        // #guardCondividi nella pagina player locale.
                        val fUrl = frame?.url.orEmpty()
                        if (fUrl.contains("youtube.com/embed")) {
                            runCatching {
                                frame.executeJavaScript(
                                    "(function(){try{if(document.getElementById('riplay-no-share'))return;"
                                            + "var s=document.createElement('style');s.id='riplay-no-share';"
                                            + "s.textContent='.ytp-share-button{display:none!important}'"
                                            + "+'.ytp-share-panel{display:none!important}'"
                                            + "+'.ytp-title-share-button{display:none!important}';"
                                            + "(document.head||document.documentElement).appendChild(s);}catch(e){}})()",
                                    fUrl,
                                    1
                                )
                                if (!embedShareHiddenLogged) {
                                    embedShareHiddenLogged = true
                                    log("CSS 'nascondi condividi' iniettato nel frame embed (ytp-share-button)")
                                }
                            }.onFailure {
                                log("iniezione CSS condividi fallita: ${it.javaClass.simpleName}: ${it.message}")
                            }
                        }
                    }
                }
            })
            log("diagnostica: load handler errori di rete installato")
        }.onFailure { log("addLoadHandler fallita: ${it.javaClass.simpleName}: ${it.message}") }
    }

    /**
     * Ripete l'avvio dopo uno [KcefState.Failed] (pulsante "Riprova" nella
     * UI): resetta le cache di scansione del bundle e rilancia [start].
     * Se l'init CEF era gia' partito e fallisce di nuovo, l'errore viene
     * semplicemente mostrato di nuovo con i suoi dettagli.
     */
    fun retry() {
        log("retry richiesto dalla UI")
        synchronized(this) {
            started = false
            bundleScanned = false
            bundleHelperFile = null
            bundleLibcefFile = null
            bundleCefFrameworkFile = null
            bundleCefResourcesDir = null
            bundleNativeFiles = emptyList()
        }
        start()
    }

    fun dispose() {
        runCatching { KCEF.disposeBlocking() }
        client = null
        started = false
        _state.value = KcefState.NotStarted
    }
}
