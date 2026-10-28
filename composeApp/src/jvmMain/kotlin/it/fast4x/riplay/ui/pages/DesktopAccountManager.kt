package it.fast4x.riplay.ui.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.fast4x.environment.Environment
import it.fast4x.environment.models.AccountInfo
import it.fast4x.environment.models.responses.CachedAccountProfile
import it.fast4x.environment.utils.parseCookieString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * STEP 3ae: ACCOUNT YouTube Music sul DESKTOP (tab Account delle
 * Impostazioni), replica del flusso Android (extensions/accountlogin/
 * AccountLogin.kt):
 *
 *  1. l'utente accede a Google nel browser integrato (KCEF) aperto sulla
 *     pagina di login con ritorno su music.youtube.com (come la WebView
 *     Android);
 *  2. quando l'accesso e' riuscito, i cookie YouTube sono nel
 *     CefCookieManager GLOBALE (lo stesso gia' usato da KcefEngine per il
 *     seeding SOCS/CONSENT): da li' viene raccolta la stringa Cookie;
 *  3. la stringa viene caricata in Environment.cookie (il client ktor la
 *     invia con l'header SAPISIDHASH: Environment.kt:707-720), viene
 *     salvata in ~/.riplay/account.properties e verificata con
 *     Environment.accountInfo() (nome/email/avatar);
 *  4. ai prossimi avvii main.kt chiama restoreAtStartup() e l'account e'
 *     gia' attivo (come le preferenze ytCookie/ytVisitorData Android).
 *
 * STEP 3ah (replica del flusso Android COMPLETO, richiesta utente: il
 * login desktop non persisteva come su Android e mancava la scelta dei
 * profili): come AccountLogin.kt Android, dopo la verifica vengono
 * scaricati i PROFILI con Environment.getAccountsList() (stesso endpoint
 * account switcher), messi in cache (ytCachedAccounts Android) e, se ce
 * ne sono piu' di uno, l'utente sceglie quale usare con un selettore in
 * tab Account. Alla scelta (come CachedAccountsSelectorDialog Android)
 * vengono persistiti pageId/authUser e le info del profilo, e il browser
 * integrato naviga su profile.signinUrl: il polling raccoglie il NUOVO
 * cookie del profilo scelto e lo persiste. pageId/authUser viaggiano
 * nell'Environment (Environment.pageId/Environment.authUser, come
 * ytPageId/ytAuthUser Android) cosi' le chiamate autenticate colpiscono
 * il profilo giusto anche dopo un riavvio.
 *
 * Il VISITOR_DATA/DATASYNC_ID di Android (estratti via javascript dalla
 * WebView) qui al primo livello NON sono necessari: accountInfo e le
 * richieste con cookie funzionano anche senza (visitorData viene comunque
 * impostato se possibile con Environment.getInitialVisitorData(), lo
 * stesso endpoint che il client usa anonimo). Raffinamento futuro.
 *
 * STEP 3ai (segnalazione utente v52: "la login non completa l'accesso,
 * rimane 'In attesa del completamento dell'accesso...'"): la raccolta
 * cookie non aveva NESSUN feedback visibile (i log finivano solo in
 * console) e un eventuale fallimento silenzioso del visit JCEF lasciava
 * la UI in attesa per sempre. Novita':
 *  - harvestWithDiag() ritorna l'esito QUALIFICATO (HarvestDiag: nessun
 *    manager / zero cookie / zero cookie YouTube / manca SAPISID / OK) che
 *    la tab Account mostra in UI a ogni tentativo;
 *  - la visita cookie e' DOPPIA (visitAllCookies + visitUrlCookies di
 *    music.youtube.com, risultati uniti) e l'attesa ha early-exit appena
 *    e' presente una cookie SAPISID (non serve attendere l'enumerazione
 *    completa: il Proxy riceve le cookie in streaming dal thread CEF);
 *  - hygiene del Proxy dinamico: toString/hashCode/equals rispondono con
 *    tipi coerenti (il vecchio handler tornava 'true' per OGNI metodo:
 *    se il glue JCEF avesse invocato hashCode(), il proxy avrebbe lanciato
 *    ClassCastException assorbendo il Boolean su un ritorno int);
 *  - timeout di 15 s sulle chiamate di rete (accountInfo/getAccountsList/
 *    getInitialVisitorData): una richiesta appesa non blocca piu' il
 *    ciclo di polling per tempo indefinito.
 *
 * STEP 3aj (test v53: il bottone "Verifica adesso" riporta sempre
 * "nessun cookie trovato" anche con l'accesso completato nel browser):
 * TRE cause verificate SUI SORGENTI del JCEF incluso in KCEF v44 (fork
 * DATL4G/jcef, branch upstream-fix):
 *  1. CefCookieVisitor.visit() in questo fork ritorna BOOLEAN ("return
 *     false to stop visiting cookies"): il Proxy dinamico step3ai
 *     rispondeva null -> NullPointerException all'unboxing booleano a
 *     OGNI callback ricevuto, assorbita dal glue nativo;
 *  2. CefCookieManager.getGlobalManager() NON e' un singleton nel fork:
 *     crea un wrapper NUOVO a ogni chiamata e lo collega all'istanza
 *     nativa via CefApp.onInitialization; il wrapper NON collegato lancia
 *     RuntimeException("JCEF is not initialed yet") a OGNI metodo - il
 *     vecchio runCatching la inghiottiva (invoked=false -> esito
 *     NO_COOKIES senza spiegazioni in UI);
 *  3. l'enumerazione nativa era l'UNICO percorso di raccolta: ora c'e' un
 *     FALLBACK indipendente che replica ESATTAMENTE la semantica di
 *     CookieManager.getCookie(currentUrl) Android: un client KCEF
 *     DEDICATO al login (KCEF.newClient crea un CefClient nuovo - gli
 *     handler sono per-client, il client del player resta intatto) porta
 *     un CefRequestHandler che cattura l'header "Cookie" delle richieste
 *     REALI verso youtube.com: quando l'utente e' autenticato quell'
 *     header contiene il SAPISID e viene usato come cookie string.
 * Novita': manager cache-ato (un solo wrapper), chiamate di visita DIRETTE
 * (niente reflection) con esito loggato, includeHttpOnly=TRUE sulla visita
 * di music.youtube.com (SID/HSID/SAPISID sono HttpOnly e Android li
 * include perche' viaggiano nell'header), visitor REALE che risponde true
 * (= continua la visita, senza proxy), nuovo esito VISIT_REFUSED per la UI
 * quando il gestore cookie rifiuta la lettura.
 */
object DesktopAccountManager {

    /** STEP 3ai: esito qualificato della raccolta cookie (per la UI). */
    enum class HarvestDiag {
        NO_MANAGER,    // CefCookieManager non disponibile (CEF non inizializzato?)
        NO_COOKIES,    // visita riuscita ma ZERO cookie nel profilo CEF
        NO_YT_COOKIES, // cookie presenti ma nessuno del dominio youtube.com
        NO_SAPISID,    // cookie YouTube presenti ma senza SAPISID/PAPISID
        VISIT_REFUSED, // STEP 3aj: il gestore cookie ha rifiutato le visite
        OK             // cookie autenticato raccolto
    }

    /** STEP 3ai: risultato della raccolta con diagnostica per la UI. */
    data class HarvestResult(
        val cookie: String?,
        val diag: HarvestDiag,
        val visitedCookies: Int,
        val ytCookies: Int
    )

    private const val LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

    fun loginUrl(): String = LOGIN_URL

    // ── STEP 3aj: GESTORE COOKIE CACHE-ATO ─────────────────────────────────
    // Nel fork JCEF di KCEF v44 getGlobalManager() NON e' un singleton: ogni
    // chiamata crea un wrapper nuovo collegato via CefApp.onInitialization.
    // Cache-ando il wrapper evitiamo sia il churn di collegamenti ripetuti
    // (il polling gira ogni 2 s) sia il rischio di usare un wrapper non
    // ancora collegato (RuntimeException silenziosa -> "nessun cookie").
    @Volatile
    private var cachedCookieManager: org.cef.network.CefCookieManager? = null

    private fun globalCookieManager(): org.cef.network.CefCookieManager? {
        cachedCookieManager?.let { return it }
        return runCatching { org.cef.network.CefCookieManager.getGlobalManager() }
            .onFailure {
                println("[RiPlay-Account] getGlobalManager errore: ${it.javaClass.simpleName}: ${it.message}")
            }
            .getOrNull()
            ?.also { cachedCookieManager = it }
    }

    // ── STEP 3aj: CATTURA HEADER "Cookie" (fallback indipendente) ──────────
    // Stato attivo SOLO mentre l'area di login e' aperta (begin/end chiamati
    // da SettingsPage). L'header e' tenuto SOLO in memoria (mai loggato per
    // valore, mai persistito): e' la stessa stringa che Android ottiene con
    // CookieManager.getCookie(url) sulle pagine youtube.com.
    @Volatile
    private var loginCaptureActive = false

    @Volatile
    private var capturedCookieHeader: String? = null

    @Volatile
    private var capturedHeaderFrom: String? = null

    /** Attiva la cattura header Cookie (apertura area di login). */
    fun beginLoginCapture() {
        loginCaptureActive = true
        capturedCookieHeader = null
        capturedHeaderFrom = null
        println("[RiPlay-Account] cattura header Cookie attivata")
    }

    /** Disattiva la cattura header Cookie (chiusura area di login). */
    fun endLoginCapture() {
        loginCaptureActive = false
        println("[RiPlay-Account] cattura header Cookie disattivata")
    }

    /** Nomi dei cookie in una stringa header "a=1; b=2" (privacy: solo nomi). */
    fun cookieNames(header: String): Set<String> = header.split(';').mapNotNull {
        val pair = it.trim()
        val eq = pair.indexOf('=')
        if (eq <= 0) null else pair.substring(0, eq).trim()
    }.toSet()

    private fun hasAuthCookies(header: String): Boolean = cookieNames(header).any {
        it == "SAPISID" || it == "__Secure-1PAPISID" || it == "__Secure-3PAPISID"
    }

    private fun isYoutubeHostUrl(url: String): Boolean {
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: return false
        return host == "youtube.com" || host.endsWith(".youtube.com")
    }

    /** Handler di cattura: registra l'header Cookie delle richieste YouTube. */
    private val captureResourceHandler = object : org.cef.handler.CefResourceRequestHandlerAdapter() {
        override fun onBeforeResourceLoad(
            browser: org.cef.browser.CefBrowser,
            frame: org.cef.browser.CefFrame,
            request: org.cef.network.CefRequest
        ): Boolean {
            runCatching {
                if (!loginCaptureActive) return@runCatching
                val url = request.url ?: return@runCatching
                if (!isYoutubeHostUrl(url)) return@runCatching
                val cookie = request.getHeaderByName("Cookie")
                if (cookie.isNullOrBlank()) return@runCatching
                if (!hasAuthCookies(cookie)) return@runCatching
                val previous = capturedCookieHeader
                val improves = previous == null || !hasAuthCookies(previous) ||
                        cookieNames(cookie).size > cookieNames(previous).size
                if (improves) {
                    capturedCookieHeader = cookie
                    val first = previous == null || !hasAuthCookies(previous)
                    if (first) {
                        capturedHeaderFrom = url
                        println("[RiPlay-Account] cattura header: cookie autenticati presenti su ${url.take(80)} (${cookieNames(cookie).size} cookie)")
                    }
                }
            }
            return false // false = continua la richiesta (RV_CONTINUE)
        }
    }

    private fun captureRequestHandler(): org.cef.handler.CefRequestHandler =
        object : org.cef.handler.CefRequestHandlerAdapter() {
            override fun getResourceRequestHandler(
                browser: org.cef.browser.CefBrowser,
                frame: org.cef.browser.CefFrame,
                request: org.cef.network.CefRequest,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String,
                disableDefaultHandling: org.cef.misc.BoolRef
            ): org.cef.handler.CefResourceRequestHandler? {
                if (!loginCaptureActive) return null
                val url = request?.url ?: return null
                return if (isYoutubeHostUrl(url)) captureResourceHandler else null
            }
        }

    // ── STEP 3aj: CLIENT KCEF DEDICATO AL LOGIN ────────────────────────────
    // KCEF.newClient crea un CefClient NUOVO a ogni chiamata (sorgente
    // verificato): i client hanno slot handler PROPRI, quindi aggiungere qui
    // il cacciatore header NON tocca gli handler del client del player
    // (CefClient.addRequestHandler in java-cef SOSTITUISCE: sul client
    // condiviso avremmo perso la politica embed di KcefEngine). Il browser
    // creato su questo client usa comunque il contesto richiesta GLOBALE
    // (CefBrowser_N: context null -> getGlobalContext): i cookie del login
    // finiscono nello stesso store che l'enumerazione nativa legge.
    @Volatile
    private var loginClient: dev.datlag.kcef.KCEFClient? = null

    /**
     * Client KCEF per il browser di login (cache-ato, riutilizzato tra le
     * sessioni di login). Null se la creazione non e' riuscita: la UI allora
     * ricade sul client del motore (senza cattura header, l'enumerazione
     * nativa resta attiva).
     */
    fun provideLoginClient(): dev.datlag.kcef.KCEFClient? {
        loginClient?.let { return it }
        return runCatching {
            val c = dev.datlag.kcef.KCEF.newClientBlocking()
            c.addRequestHandler(captureRequestHandler())
            loginClient = c
            println("[RiPlay-Account] client KCEF dedicato al login pronto (cattura header Cookie installata)")
            c
        }.onFailure {
            println("[RiPlay-Account] creazione client KCEF login FALLITA: ${it.javaClass.simpleName}: ${it.message}")
        }.getOrNull()
    }

    data class StoredAccount(
        val cookie: String,
        val visitorData: String? = null,
        val dataSyncId: String? = null,
        // STEP 3ah: profilo selezionato (come ytPageId/ytAuthUser Android)
        val pageId: String? = null,
        val authUser: String? = null,
        val name: String? = null,
        val email: String? = null,
        val channelHandle: String? = null,
        val thumbnailUrl: String? = null,
        // STEP 3ah: cache dei profili (come ytCachedAccounts Android)
        val cachedAccounts: List<CachedAccountProfile>? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun encodeCachedAccounts(list: List<CachedAccountProfile>): String =
        runCatching { json.encodeToString(list) }
            .onFailure { println("[RiPlay-Account] encode accounts errore: ${it.message}") }
            .getOrDefault("[]")

    fun decodeCachedAccounts(value: String?): List<CachedAccountProfile> =
        runCatching {
            if (value.isNullOrBlank()) emptyList()
            else json.decodeFromString(ListSerializer(CachedAccountProfile.serializer()), value)
        }.onFailure {
            println("[RiPlay-Account] decode accounts errore: ${it.message}")
        }.getOrDefault(emptyList())

    private fun propertiesFile(): File {
        val home = System.getProperty("user.home")
        val dir = File(home, ".riplay")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "account.properties")
    }

    fun load(): StoredAccount? {
        val f = propertiesFile()
        if (!f.isFile) return null
        return runCatching {
            val p = Properties()
            FileInputStream(f).use { p.load(it) }
            val cookie = p.getProperty("cookie").orEmpty()
            if (cookie.isBlank()) null
            else StoredAccount(
                cookie = cookie,
                visitorData = p.getProperty("visitorData"),
                dataSyncId = p.getProperty("dataSyncId"),
                pageId = p.getProperty("pageId")?.takeIf { it.isNotBlank() && it != "null" },
                authUser = p.getProperty("authUser")?.takeIf { it.isNotBlank() && it != "null" },
                name = p.getProperty("name"),
                email = p.getProperty("email"),
                channelHandle = p.getProperty("channelHandle"),
                thumbnailUrl = p.getProperty("thumbnailUrl"),
                cachedAccounts = decodeCachedAccounts(p.getProperty("cachedAccounts")).takeIf { it.isNotEmpty() }
            )
        }.onFailure { println("[RiPlay-Account] load errore: ${it.message}") }.getOrNull()
    }

    fun save(account: StoredAccount) {
        runCatching {
            val p = Properties()
            p.setProperty("cookie", account.cookie)
            account.visitorData?.let { p.setProperty("visitorData", it) }
            account.dataSyncId?.let { p.setProperty("dataSyncId", it) }
            account.pageId?.let { p.setProperty("pageId", it) }
            account.authUser?.let { p.setProperty("authUser", it) }
            account.name?.let { p.setProperty("name", it) }
            account.email?.let { p.setProperty("email", it) }
            account.channelHandle?.let { p.setProperty("channelHandle", it) }
            account.thumbnailUrl?.let { p.setProperty("thumbnailUrl", it) }
            account.cachedAccounts?.takeIf { it.isNotEmpty() }?.let {
                p.setProperty("cachedAccounts", encodeCachedAccounts(it))
            }
            FileOutputStream(propertiesFile()).use { p.store(it, "RiPlay desktop - YouTube Music account") }
            println("[RiPlay-Account] account salvato (${account.name ?: "senza nome"})")
        }.onFailure { println("[RiPlay-Account] save errore: ${it.message}") }
    }

    fun clear() {
        runCatching { propertiesFile().delete() }
        Environment.cookie = null
        Environment.visitorData = ""
        Environment.dataSyncId = null
        // step3ah: azzera anche il profilo selezionato
        Environment.pageId = null
        Environment.authUser = null
        // STEP 3am: notifica alla UI (la home online osserva questa
        // revisione e ricarica con setLogin=false)
        DesktopLoginState.bump()
        println("[RiPlay-Account] account rimosso (logout)")
    }

    /**
     * STEP 3am (segnalazione utente v56: la home resta anonima nonostante il
     * login): profilo di DEFAULT quando non e' mai stato scelto. Su Android
     * il selettore profili (CachedAccountsSelectorDialog) gira SEMPRE dopo il
     * login - anche con un solo profilo (showSelectorDialog = true a ogni
     * onPageFinished) - quindi ytPageId/ytAuthUser risultano sempre impostati
     * e OGNI chiamata autenticata porta X-Goog-PageId/X-Goog-Authuser. Sul
     * desktop il selettore appare solo con piu' profili: con un account
     * singolo pageId/authUser restavano null per sempre e le richieste
     * uscivano senza quegli header (divergenza reale da Android). Qui si
     * replica lo stato risultante Android: profilo isSelected, altrimenti il
     * primo; se la cache profili non c'e' proprio, fallback authUser="0"
     * (l'identita' di default di ogni sessione YouTube web). Offline-safe:
     * usa SOLO la cache persistita, nessuna chiamata di rete.
     */
    fun withDefaultProfile(stored: StoredAccount): StoredAccount {
        if (!stored.pageId.isNullOrBlank()) return stored
        val cached = stored.cachedAccounts?.takeIf { it.isNotEmpty() }
        val chosen = cached?.let { list ->
            list.firstOrNull { it.isSelected == true } ?: list.first()
        }
        return stored.copy(
            pageId = chosen?.pageId?.toString(),
            authUser = chosen?.authUser?.toString() ?: if (chosen == null) "0" else stored.authUser,
            name = stored.name ?: chosen?.name,
            email = stored.email ?: chosen?.email,
            thumbnailUrl = stored.thumbnailUrl ?: chosen?.thumbnailUrl
        ).also {
            if (it != stored)
                println("[RiPlay-Account] profilo di default auto-selezionato: ${it.name ?: "?"} (pageId=${it.pageId}, authUser=${it.authUser})")
        }
    }

    /** Carica l'account salvato nell'Environment (chiamato da main.kt). */
    fun restoreAtStartup() {
        val stored = load() ?: run {
            println("[RiPlay-Account] nessun account salvato: client YT Music anonimo")
            return
        }
        // STEP 3am: se il login precedente non ha mai scelto un profilo
        // (account singolo, selettore mai mostrato) colma il gap dalla cache
        // profili gia' persistita e salva la versione completata.
        val fixed = withDefaultProfile(stored)
        if (fixed != stored) save(fixed)
        applyToEnvironment(fixed)
        println("[RiPlay-Account] account ripristinato all'avvio: ${fixed.name ?: "cookie presente"} (pageId=${fixed.pageId != null}, authUser=${fixed.authUser != null})")
    }

    /** Imposta le variabili globali del client (Environment.cookie ecc.). */
    fun applyToEnvironment(account: StoredAccount) {
        Environment.cookie = account.cookie
        account.visitorData?.takeIf { it.isNotBlank() }?.let { Environment.visitorData = it }
        account.dataSyncId?.takeIf { it.isNotBlank() }?.let { Environment.dataSyncId = it }
        // STEP 3ah: profilo selezionato (come Android: Environment.pageId/
        // authUser pilotano le chiamate autenticate verso il profilo giusto;
        // null = profilo di default). Sempre impostati (anche a null) per
        // non trascinare profili vecchi tra login diversi.
        Environment.pageId = account.pageId?.takeIf { it.isNotBlank() }
        Environment.authUser = account.authUser?.takeIf { it.isNotBlank() }
        // STEP 3am: notifica osservabile alla UI (la home online ricarica
        // con il nuovo stato login anche mentre e' gia' in primo piano:
        // prima isYtLoggedIn() non era uno stato Compose e il cambio restava
        // invisibile finche' non si usciva e rientrava nella pagina)
        DesktopLoginState.bump()
    }

    /**
     * STEP 3al (segnalazione utente v55: "la homepage viene ritornata come
     * se l'utente non e' autenticato" - Android mostra molte piu' sezioni):
     * equivalente desktop di isYtLoggedIn() Android (AccountsSettings.kt:
     * cookie contiene SAPISID). true se il cookie attivo nell'Environment
     * contiene SAPISID, il cookie che autorizza le richieste con header
     * SAPISIDHASH (Environment.kt setLogin -> Cookie + Authorization +
     * X-Goog-PageId/X-Goog-Authuser). La home online desktop lo usa per
     * decidere setLogin, identico ad Android
     * (getHomePage(setLogin = isYtLoggedIn())).
     * Differenza dal funzionamento Android: qui non c'e' il toggle
     * "login abilitato" (isYtLoginEnabled Android) - vale "logged in =
     * SAPISID presente": logout o nessun account -> false.
     */
    fun isYtLoggedIn(): Boolean =
        Environment.cookie
            ?.takeIf { it.isNotBlank() }
            ?.let { parseCookieString(it) }
            // STEP 3am: STESSA FAMIGLIA dell'harvest (hasSapisid): la
            // raccolta CEF da' OK anche con SOLO __Secure-1PAPISID/
            // __Secure-3PAPISID (il SAPISID plain puo' mancare nel profilo).
            // Prima qui si guardava solo "SAPISID": in quel caso la home
            // partiva con setLogin=false -> NESSUN Cookie header -> risposta
            // anonima, mentre accountInfo (setLogin=true fisso) continuava a
            // funzionare e l'utente vedeva "connesso" + home anonima.
            ?.let { names ->
                names.containsKey("SAPISID") ||
                        names.containsKey("__Secure-1PAPISID") ||
                        names.containsKey("__Secure-3PAPISID")
            } == true

    /**
     * Raccoglie dal CefCookieManager globale la stringa cookie di
     * music.youtube.com (equivalente di CookieManager.getCookie(currentUrl)
     * di Android) e verifica che contenga SAPISID (il cookie che autorizza
     * le richieste con SAPISIDHASH).
     *
     * FIX step3af (login non persistito / Environment anonimo): la prima
     * versione cercava i metodi getCookies(List) e getCookies(List,
     * BiConsumer), che NON esistono nell'API JCEF: la reflection non trovava
     * nulla, l'harvest tornava sempre null e quindi il login non veniva mai
     * salvato ne' caricato in Environment (l'utente vedeva l'accesso
     * riuscito nel browser ma l'app restava anonima). L'API REALE di JCEF
     * per LEGGERE i cookie e' la visita tramite CefCookieVisitor:
     *   - visitAllCookies(visitor)
     *   - visitUrlCookies(url, httpOnly, visitor)
     * L'interfaccia CefCookieVisitor viene risolta BY NAME (il package varia
     * tra le versioni JCEF: org.cef.network / org.cef.callback) e
     * implementata con un Proxy dinamico: zero riferimenti compile-time a
     * tipi incerti, zero rischio NoSuchMethodError (lezione step2o/2p).
     * Il callback arriva sul thread UI di CEF: si attende con un latch e
     * timeout di sicurezza. La chiamata e' sicura da qualunque thread
     * (il manager e' gia' usato dal motore dal thread kcef-init).
     *
     * STEP 3ai: il vecchio harvestCookieFromCef() ritornava SOLO la stringa
     * (o null): un null perpetuo era indistinguibile da un CEF non pronto,
     * da un profilo vuoto o da una mancanza di SAPISID. Ora il corpo vive in
     * harvestWithDiag() che ritorna anche l'esito qualificato, e la vecchia
     * firma resta come wrapper per compatibilita'.
     */
    suspend fun harvestCookieFromCef(): String? = harvestWithDiag().cookie

    /**
     * STEP 3ai: raccolta cookie con esito QUALIFICATO (HarvestDiag) da
     * mostrare in UI.
     *
     * STEP 3aj: implementazione con le API DIRETTE del fork JCEF incluso in
     * KCEF v44 (verificate sui sorgenti DATL4G/jcef upstream-fix):
     *  - visitor REALE su org.cef.callback.CefCookieVisitor (visit ritorna
     *    boolean: true = continua la visita) al posto del Proxy dinamico
     *    che rispondeva null su un metodo booleano;
     *  - chiamate dirette mgr.visitAllCookies / mgr.visitUrlCookies con esito
     *    booleano LOGGATO (il wrapper del fork lancia RuntimeException se il
     *    collegamento all'istanza nativa non e' avvenuto: prima veniva
     *    assorbita e la UI restava su "nessun cookie trovato");
     *  - includeHttpOnly=TRUE (SID/HSID/SAPISID sono HttpOnly: Android li
     *    include perche' viaggiano nell'header Cookie);
     *  - FALLBACK indipendente: header Cookie catturato dalle richieste
     *    reali del browser di login (client dedicato), stessa semantica di
     *    CookieManager.getCookie(url) Android.
     */
    suspend fun harvestWithDiag(): HarvestResult = withContext(Dispatchers.IO) {
        val mgr = globalCookieManager()
        if (mgr == null) {
            println("[RiPlay-Account] harvest: CefCookieManager non disponibile")
            return@withContext HarvestResult(null, HarvestDiag.NO_MANAGER, 0, 0)
        }

        val cookies = java.util.Collections.synchronizedList(mutableListOf<org.cef.network.CefCookie>())
        // FIX step3aj: visitor REALE (il visit del fork ritorna boolean:
        // true = "continua la visita"). Il Proxy step3ai rispondeva null per
        // visit: NullPointerException all'unboxing a ogni callback.
        val visitor = object : org.cef.callback.CefCookieVisitor {
            override fun visit(
                cookie: org.cef.network.CefCookie?,
                count: Int,
                total: Int,
                delete: org.cef.misc.BoolRef?
            ): Boolean {
                if (cookie != null) cookies.add(cookie)
                return true
            }
        }

        // FIX step3aj: chiamate DIRETTE con esito LOGGATO (niente reflection:
        // le firme sono certe nel fork). visitAllCookies copre TUTTO il
        // profilo CEF; visitUrlCookies e' la semantica CookieManager.getCookie
        // Android limitata a music.youtube.com, ora CON HttpOnly.
        var visitAllOk: Boolean? = null
        var visitUrlOk: Boolean? = null
        runCatching { visitAllOk = mgr.visitAllCookies(visitor) }
            .onFailure { println("[RiPlay-Account] harvest: visitAllCookies errore: ${it.javaClass.simpleName}: ${it.message}") }
        runCatching { visitUrlOk = mgr.visitUrlCookies("https://music.youtube.com/", true, visitor) }
            .onFailure { println("[RiPlay-Account] harvest: visitUrlCookies errore: ${it.javaClass.simpleName}: ${it.message}") }
        println("[RiPlay-Account] harvest: esito visite CEF visitAll=$visitAllOk visitUrl=$visitUrlOk")

        // Se ENTRAMBE le visite sono rifiutate (false o errore) nessun
        // callback arrivera': inutile attendere il tetto di 6 s.
        val visitRefused = (visitAllOk == false || visitAllOk == null) &&
                (visitUrlOk == false || visitUrlOk == null)

        if (!visitRefused) {
            // attesa con EARLY-EXIT: si esce appena una cookie della famiglia
            // SAPISID e' gia' arrivata (il visit alimenta la lista in
            // streaming dal thread CEF), altrimenti tetto di 6 s.
            fun hasSapisidInFlight(): Boolean = synchronized(cookies) {
                cookies.any {
                    it.name == "SAPISID" ||
                            (it.name.startsWith("__Secure-") && it.name.contains("PAPISID"))
                }
            }
            val deadline = System.nanoTime() + 6_000_000_000L
            while (System.nanoTime() < deadline) {
                if (hasSapisidInFlight()) break
                delay(250)
            }
        }

        val snapshot: List<org.cef.network.CefCookie> = synchronized(cookies) { cookies.toList() }

        // Filtro come Android (CookieManager.getCookie("https://music.youtube.com")):
        // SOLO i cookie del dominio youtube.com (esclusi google.com e
        // youtube-nocookie.com), scartati quelli scaduti, dedup per nome
        // (vince l'ultimo: la visitUrlCookies di music.youtube.com arriva
        // DOPO visitAllCookies e quindi ha la precedenza in caso di doppione).
        val now = java.util.Date()
        val byName = LinkedHashMap<String, org.cef.network.CefCookie>()
        for (c in snapshot) {
            val d = (c.domain ?: "").lowercase()
            val isYt = d == "youtube.com" || d.endsWith(".youtube.com")
            if (!isYt) continue
            if (c.hasExpires && c.expires != null && c.expires.before(now)) continue
            byName[c.name] = c
        }
        // log diagnostico SENZA valori (privacy): solo nomi dei cookie critici
        val critical = listOf("SID", "HSID", "SSID", "SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID")
        val present = byName.keys.filter { it in critical }
        println("[RiPlay-Account] harvest: visitate ${snapshot.size} cookie, ${byName.size} YouTube valide, critici presenti: [${present.joinToString(", ")}]")
        val hasSapisid = byName.keys.any { it == "SAPISID" || it == "__Secure-1PAPISID" || it == "__Secure-3PAPISID" }

        // STEP 3aj: FALLBACK indipendente - header Cookie catturato dalle
        // richieste reali del browser di login (client dedicato). Contiene il
        // SAPISID quando l'utente e' autenticato ed e' ESATTAMENTE la stringa
        // che il browser spedirebbe a YouTube (stessa cosa che Android ottiene
        // da CookieManager.getCookie).
        val headerCookie: String? = capturedCookieHeader?.takeIf { hasAuthCookies(it) }

        val nativeCookie: String? = if (hasSapisid && byName.isNotEmpty()) {
            byName.values.joinToString("; ") { "${it.name}=${it.value}" }.takeIf { it.isNotBlank() }
        } else null

        return@withContext when {
            nativeCookie != null -> {
                println("[RiPlay-Account] harvest: cookie autenticato raccolto dall'enumerazione CEF (${byName.size} cookie YouTube)")
                HarvestResult(nativeCookie, HarvestDiag.OK, snapshot.size, byName.size)
            }
            headerCookie != null -> {
                println("[RiPlay-Account] harvest: enumerazione CEF senza esito, uso l'header Cookie catturato (${cookieNames(headerCookie).size} cookie, da ${capturedHeaderFrom?.take(60)})")
                HarvestResult(headerCookie, HarvestDiag.OK, snapshot.size, 0)
            }
            visitRefused -> {
                println("[RiPlay-Account] harvest: il gestore cookie CEF ha rifiutato ENTRAMBE le visite")
                HarvestResult(null, HarvestDiag.VISIT_REFUSED, snapshot.size, byName.size)
            }
            snapshot.isEmpty() -> {
                println("[RiPlay-Account] harvest: nessun cookie nel profilo CEF (login non ancora completato?)")
                HarvestResult(null, HarvestDiag.NO_COOKIES, 0, 0)
            }
            byName.isEmpty() ->
                HarvestResult(null, HarvestDiag.NO_YT_COOKIES, snapshot.size, 0)
            else ->
                HarvestResult(null, HarvestDiag.NO_SAPISID, snapshot.size, byName.size)
        }
    }

    /**
     * Best-effort: visitorData anonimo dal client (Environment.kt:671).
     * STEP 3ai: tetto di 15 s - una richiesta appesa non deve bloccare il
     * ciclo di polling del login (il TimeoutCancellationException viene
     * catturato SOLO qui; le cancellazioni esterne propagano).
     */
    suspend fun fetchVisitorData(): String? = try {
        withTimeout(15_000) {
            runCatching { Environment.getInitialVisitorData().getOrNull() }
                .onFailure { println("[RiPlay-Account] visitorData non disponibile: ${it.message}") }
                .getOrNull()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        println("[RiPlay-Account] visitorData timeout (15 s)")
        null
    }

    /**
     * STEP 3ah: lista dei PROFILI dell'utente (come Environment.getAccountsList
     * in AccountLogin.kt Android). Serve per il selettore: ogni profilo porta
     * pageId/authUser/signinUrl per lo switch.
     */
    suspend fun fetchAccountsList(): List<CachedAccountProfile>? = try {
        withTimeout(15_000) {
            runCatching { Environment.getAccountsList().getOrNull() }
                .onFailure { println("[RiPlay-Account] getAccountsList errore: ${it.message}") }
                .getOrNull()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        println("[RiPlay-Account] getAccountsList timeout (15 s)")
        null
    }

    /**
     * STEP 3ah: selezione del profilo (come CachedAccountsSelectorDialog
     * Android): persiste pageId/authUser e le info del profilo, e imposta
     * l'Environment PRIMA che il browser navigi su signinUrl (il nuovo
     * cookie del profilo viene raccolto dal polling e persistito dopo).
     */
    fun selectProfile(profile: CachedAccountProfile, base: StoredAccount?) {
        val updated = (base ?: StoredAccount(cookie = "")).copy(
            pageId = profile.pageId?.toString(),
            authUser = profile.authUser?.toString(),
            name = profile.name ?: base?.name,
            email = profile.email ?: base?.email,
            channelHandle = profile.channelHandle ?: base?.channelHandle,
            thumbnailUrl = profile.thumbnailUrl ?: base?.thumbnailUrl
        )
        save(updated)
        applyToEnvironment(updated)
        println("[RiPlay-Account] profilo selezionato: ${updated.name} (pageId=${updated.pageId}, authUser=${updated.authUser})")
    }

    /** STEP 3ah: URL per lo switch di profilo (come signinUrl Android). */
    fun switchUrl(profile: CachedAccountProfile): String? =
        profile.signinUrl?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com$it" }

    /**
     * Verifica l'account corrente con l'endpoint accountInfo (come Android).
     * STEP 3ai: tetto di 15 s (stesso motivo di fetchVisitorData).
     */
    suspend fun fetchAccountInfo(): AccountInfo? = try {
        withTimeout(15_000) {
            runCatching { Environment.accountInfo().getOrNull() }
                .onFailure { println("[RiPlay-Account] accountInfo errore: ${it.message}") }
                .getOrNull()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        println("[RiPlay-Account] accountInfo timeout (15 s)")
        null
    }

    /** Logout: rimuove cookie salvato e variabili Environment. */
    fun logout() = clear()
}

/**
 * STEP 3am: revisione osservabile (Compose) dello stato di login desktop.
 * Environment.cookie NON e' uno stato Compose: la home online leggeva
 * isYtLoggedIn() solo alla (ri)composizione e un login/logout riuscito
 * nell'area Account non la toccava finche' non si cambiava pagina. Ogni
 * applyToEnvironment/clear aumenta la revisione: chi la legge in
 * composizione si riuscrive automaticamente.
 */
object DesktopLoginState {
    var revision: Int by mutableStateOf(0)
        private set

    fun bump() {
        revision++
    }
}
