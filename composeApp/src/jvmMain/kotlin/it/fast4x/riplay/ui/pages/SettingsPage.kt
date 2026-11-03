package it.fast4x.riplay.ui.pages

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.datlag.kcef.KCEFClient
import it.fast4x.environment.models.responses.CachedAccountProfile
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.player.kcef.KcefState
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.utils.DesktopAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cef.browser.CefRendering
import dev.datlag.kcef.KCEFBrowser

/**
 * STEP 3ae: PRIMO LIVELLO di IMPOSTAZIONI del desktop, con le tab
 * organizzate come la parte Android (ui/screens/settings/SettingsScreen.kt):
 *
 *  - tab DATI (come DataSettings Android): esporta/importa un BACKUP del
 *    database della libreria (zip del db Room, struttura identica tra
 *    desktop e Android: stessi modelli e migrazioni). Vedi DesktopBackup.
 *  - tab ACCOUNT (come AccountsSettings Android): login YouTube Music con
 *    il browser integrato KCEF, replica di AccountLogin Android
 *    (WebView -> cookie -> Environment.cookie + accountInfo). Vedi
 *    DesktopAccountManager.
 *
 * La struttura e' pensata per ACCOGLIERE NUOVE TAB in futuro (aspetto,
 * player, ecc.) seguendo l'evoluzione delle impostazioni Android.
 */
@Composable
fun SettingsPage() {
    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(SettingsTab.DATA) }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // ── Titolo pagina ───────────────────────────────────────────────
        Text(
            text = ds("nav.settings"),
            style = typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ── Tab (stile SearchPage: chip accent) ─────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            SettingsTab.values().forEach { tab ->
                val active = tab == activeTab
                Text(
                    text = ds(tab.labelKey),
                    color = if (active) Color.Black else Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) RiPlayAccent else Color.White.copy(alpha = 0.08f))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        when (activeTab) {
            SettingsTab.DATA -> DataTab(coroutineScope)
            SettingsTab.ACCOUNTS -> AccountsTab()
            SettingsTab.ABOUT -> AboutTab()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB DATI: backup del database
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DataTab(coroutineScope: kotlinx.coroutines.CoroutineScope) {
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text(
            text = ds("settings.data.info"),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Text(
            text = ds("settings.data.db_path", "path" to DesktopBackup.dbPathLabel()),
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            // ESPORTA
            Text(
                text = ds("settings.data.export_backup"),
                color = if (busy) Color.White.copy(alpha = 0.4f) else Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (busy) RiPlayAccent.copy(alpha = 0.3f) else RiPlayAccent)
                    .clickable(enabled = !busy) {
                        coroutineScope.launch {
                            busy = true
                            status = null
                            try {
                                val path = DesktopBackup.exportBackup()
                                status = path?.let { ds("settings.data.export_done", "path" to it) }
                                statusOk = path != null
                            } catch (e: Exception) {
                                status = ds("settings.data.export_failed", "error" to (e.message ?: ""))
                                statusOk = false
                            }
                            busy = false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )

            // IMPORTA
            Text(
                text = ds("settings.data.import_backup"),
                color = if (busy) Color.White.copy(alpha = 0.4f) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (busy) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f))
                    .clickable(enabled = !busy) {
                        coroutineScope.launch {
                            busy = true
                            status = null
                            when (val result = DesktopBackup.importBackup()) {
                                is DesktopBackup.ImportResult.Scheduled -> {
                                    status = ds("settings.data.import_done")
                                    statusOk = true
                                }
                                is DesktopBackup.ImportResult.Invalid -> {
                                    status = result.reason
                                    statusOk = false
                                }
                                is DesktopBackup.ImportResult.Failed -> {
                                    status = ds("settings.data.import_failed", "error" to result.error)
                                    statusOk = false
                                }
                                DesktopBackup.ImportResult.Cancelled -> {}
                            }
                            busy = false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        status?.let {
            Text(
                text = it,
                color = if (statusOk) RiPlayAccent else Color(0xFFFFC107),
                fontSize = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB ACCOUNT: login YouTube Music con il browser integrato
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountsTab() {
    val coroutineScope = rememberCoroutineScope()

    // account corrente (dalla persistenza ~/.riplay/account.properties)
    var account by remember {
        mutableStateOf<DesktopAccountManager.StoredAccount?>(DesktopAccountManager.load())
    }
    var verifying by remember { mutableStateOf(false) }
    var loginActive by remember { mutableStateOf(false) }
    var loginBrowser by remember { mutableStateOf<KCEFBrowser?>(null) }
    // STEP 3aj: client KCEF DEDICATO al login (con cattura header Cookie).
    // Null finche' non e' pronto: l'area mostra il messaggio "in preparazione"
    // e POI crea il browser su questo client. Se la creazione fallisse, la
    // factory ricade sul client del motore (resta l'enumerazione nativa).
    var loginClientReady by remember { mutableStateOf<KCEFClient?>(null) }
    // FIX step3af: esito della verifica dell'account (null = nessun
    // messaggio, false = cookie raccolto ma verifica non riuscita)
    var loginStatus by remember { mutableStateOf<Boolean?>(null) }
    val kcefState by KcefEngine.state.collectAsState()

    // STEP 3ah: URL corrente del browser di login (loginUrl iniziale o
    // signinUrl del profilo scelto). key(loginUrl) ricrea il browser KCEF
    // quando cambia (stesso effetto del webView.loadUrl(signinUrl) Android,
    // usando SOLO le API gia' usate qui: createBrowser + dispose).
    var loginUrl by remember { mutableStateOf(DesktopAccountManager.loginUrl()) }
    // STEP 3ah: cache profili per il selettore (come ytCachedAccounts Android)
    var cachedAccounts by remember { mutableStateOf<List<CachedAccountProfile>>(emptyList()) }
    var showSelector by remember { mutableStateOf(false) }
    // true dopo la PRIMA scelta di profilo in questa sessione di login:
    // evita di riproporre il selettore a ogni raccolta cookie
    var switchedOnce by remember { mutableStateOf(false) }
    // STEP 3ah: dopo lo switch aspetta che il cookie raccolto sia DIVERSO
    // dal precedente (la navigazione su signinUrl richiede qualche secondo;
    // il primo harvest puo' ancora vedere il vecchio profilo)
    var pendingSwitchWait by remember { mutableStateOf(false) }
    var lastHarvestedCookie by remember { mutableStateOf("") }

    // STEP 3ai: diagnostica di polling VISIBILE in UI. Prima i log finivano
    // solo in console: un harvest che falliva in silenzio lasciava la tab su
    // "In attesa del completamento dell'accesso..." per sempre, senza alcun
    // indizio su cosa stesse succedendo.
    var pollCount by remember { mutableStateOf(0) }
    var pollDiag by remember { mutableStateOf<DesktopAccountManager.HarvestDiag?>(null) }
    // guardia anti-doppia-esecuzione: polling e bottone "Verifica adesso"
    // passano tutti da processHarvest, mai due volte in parallelo
    var processing by remember { mutableStateOf(false) }
    var switchWaitCycles by remember { mutableStateOf(0) }
    // ultimo cookie GIA' elaborato in questa sessione: con il selettore
    // profili aperto il polling non deve ri-verificare lo stesso cookie
    // ogni 2 s (churn di rete inutile: salva/verifica/lista profili)
    var lastProcessedCookie by remember { mutableStateOf<String?>(null) }

    // STEP 3ai: elaborazione di UN ciclo raccolta+verifica, condivisa dal
    // polling (ogni 2 s) e dal bottone "Verifica adesso" (forzata). Prima
    // la logica viveva SOLO dentro LaunchedEffect: un'eccezione non catturata
    // uccideva la coroutine e la tab restava "In attesa..." senza feedback.
    // runCatching nel loop + try/finally qui = nessuna via d'uscita silenziosa.
    suspend fun processHarvest(force: Boolean) {
        if (processing) return
        processing = true
        try {
            val result = DesktopAccountManager.harvestWithDiag()
            pollCount++
            pollDiag = result.diag
            val cookie = result.cookie
            if (cookie == null) {
                println("[RiPlay-Account] ciclo polling: ${result.diag} (visitate ${result.visitedCookies}, youtube ${result.ytCookies})")
                return
            }
            // STEP 3ah: dopo lo switch di profilo, ignora i cicli che vedono
            // ancora il VECCHIO cookie (la navigazione su signinUrl sta girando)
            // e accetta comunque dopo ~30 cicli (60 s) per non bloccare l'area
            if (pendingSwitchWait) {
                if (cookie == lastHarvestedCookie && switchWaitCycles < 30) {
                    switchWaitCycles++
                    return
                }
                pendingSwitchWait = false
                switchWaitCycles = 0
            }
            // STEP 3ai: cookie gia' elaborato in questa sessione (es. selettore
            // profili aperto): nessuna ri-verifica finche' non arriva un NUOVO
            // cookie (o il bottone forza una ri-verifica)
            if (!force && cookie == lastProcessedCookie) return
            lastProcessedCookie = cookie
            lastHarvestedCookie = cookie
            println("[RiPlay-Account] cookie di login raccolto (${cookie.length} caratteri)")
            // FIX step3af: SALVA SUBITO il cookie e imposta Environment PRIMA
            // della verifica, cosi' accountInfo e tutte le chiamate girano da
            // utente autenticato (stesso ordine di Android) e, anche se la
            // verifica fallisse, il cookie resta su disco e viene ripristinato
            // al prossimo avvio
            // STEP 3ah: il profilo scelto in precedenza (pageId/authUser) viene
            // portato AVANTI (come Android: il nuovo login NON cancella
            // ytPageId/ytAuthUser finche' il selettore non sceglie un profilo)
            val prev = account
            val provisional = DesktopAccountManager.StoredAccount(
                cookie = cookie,
                pageId = prev?.pageId,
                authUser = prev?.authUser
            )
            DesktopAccountManager.save(provisional)
            DesktopAccountManager.applyToEnvironment(provisional)
            val visitor = DesktopAccountManager.fetchVisitorData()
            val info = DesktopAccountManager.fetchAccountInfo()
            // STEP 3ah: profili dell'utente (getAccountsList, come Android) ->
            // in cache: alimenta il selettore in questa e nelle prossime sessioni
            val accounts = DesktopAccountManager.fetchAccountsList()
            if (!accounts.isNullOrEmpty())
                cachedAccounts = accounts
            val completeBase = provisional.copy(
                visitorData = visitor,
                name = info?.name ?: prev?.name,
                email = info?.email ?: prev?.email,
                channelHandle = info?.channelHandle ?: prev?.channelHandle,
                thumbnailUrl = info?.thumbnailUrl ?: prev?.thumbnailUrl,
                cachedAccounts = accounts ?: prev?.cachedAccounts
            )
            // STEP 3am (home ancora anonima nonostante il login): se il
            // profilo non e' mai stato scelto (account singolo: il selettore
            // appare solo con piu' profili) auto-seleziona il profilo di
            // default ESATTAMENTE come fa Android (dove il selettore gira
            // sempre e ytPageId/ytAuthUser sono sempre impostati): senza,
            // le browse autenticate uscivano senza X-Goog-PageId/
            // X-Goog-Authuser, divergenza reale dalla home Android.
            val complete = DesktopAccountManager.withDefaultProfile(completeBase)
            DesktopAccountManager.save(complete)
            // STEP 3am: ri-applica ALL'Environment l'account COMPLETO
            // (pageId/authUser/visitorData inclusi: prima veniva applicato
            // solo il provisional col solo cookie) - e aggiorna lo stato
            // login osservabile per la home online.
            DesktopAccountManager.applyToEnvironment(complete)
            account = complete
            loginStatus = info?.name != null
            // STEP 3ah: se ci sono piu' profili e non ne e' stato scelto
            // nessuno in questa sessione, resta aperto e mostra il selettore
            // (come CachedAccountsSelectorDialog Android); alla scelta il
            // browser naviga su signinUrl e il polling raccoglie il NUOVO
            // cookie del profilo scelto, poi l'area si chiude.
            // FIX step3ai: mentre il selettore e' aperto l'area di login NON
            // disegna piu' il browser (vedi sotto): il pannello di scelta
            // prende il suo posto invece di restare schiacciato vicino al
            // browser da 420 dp.
            if (!switchedOnce && cachedAccounts.size > 1) {
                showSelector = true
            } else {
                loginActive = false
            }
        } finally {
            processing = false
        }
    }

    // alla prima apertura della tab con un account salvato: verifica che il
    // cookie sia ancora valido (accountInfo, come fa Android all'avvio)
    LaunchedEffect(activeTabKey(account, loginActive)) {
        val stored = account ?: return@LaunchedEffect
        if (verifying || loginActive) return@LaunchedEffect
        verifying = true
        val info = DesktopAccountManager.fetchAccountInfo()
        if (info != null && info.name != null) {
            val refreshed = stored.copy(
                name = info.name ?: stored.name,
                email = info.email ?: stored.email,
                channelHandle = info.channelHandle ?: stored.channelHandle,
                thumbnailUrl = info.thumbnailUrl ?: stored.thumbnailUrl
            )
            account = refreshed
            DesktopAccountManager.save(refreshed)
            println("[RiPlay-Account] verifica OK: ${refreshed.name}")
        } else {
            println("[RiPlay-Account] verifica fallita: il cookie potrebbe essere scaduto")
            loginStatus = false
        }
        verifying = false
    }

    // POLLING di raccolta cookie durante il login (ogni 2 s), come la
    // WebViewClient Android che raccoglie i cookie all'arrivo su music.youtube.com
    // STEP 3aj: all'apertura dell'area attiva ANCHE la cattura header Cookie e
    // crea il client KCEF dedicato al login (chiave d'effetto anche su
    // kcefState: il client viene creato solo con CEF pronto).
    LaunchedEffect(loginActive, kcefState) {
        if (!loginActive) return@LaunchedEffect
        println("[RiPlay-Account] attesa login nel browser integrato...")
        DesktopAccountManager.beginLoginCapture()
        if (loginClientReady == null && kcefState is KcefState.Ready) {
            loginClientReady = withContext(Dispatchers.IO) {
                DesktopAccountManager.provideLoginClient()
            }
        }
        while (isActive && loginActive) {
            delay(2000)
            // STEP 3ai: runCatching per ciclo - NESSUNA eccezione puo' piu'
            // uccidere il loop lasciando la UI in attesa senza spiegazioni
            runCatching { processHarvest(force = false) }
                .onFailure { println("[RiPlay-Account] ciclo polling errore: ${it.message}") }
        }
    }

    // Dismetti il browser quando l'area di login si chiude (login riuscito,
    // logout, cambio tab): il player KCEF resta intoccato (browser separato)
    if (loginActive) {
        DisposableEffect(Unit) {
            onDispose {
                loginBrowser?.let { b -> runCatching { b.dispose() } }
                loginBrowser = null
                DesktopAccountManager.endLoginCapture()
                println("[RiPlay-Account] browser login chiuso")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text(
            text = ds("settings.accounts.info"),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        val logged = account != null
        Text(
            text = if (logged)
                ds("settings.accounts.logged_as", "name" to (account?.name ?: "YouTube Music"))
            else
                ds("settings.accounts.not_logged"),
            color = if (logged) RiPlayAccent else Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        // CARD account (avatar + nome + email)
        if (logged) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(12.dp)
            ) {
                account?.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
                    )
                }
                Column {
                    Text(
                        text = account?.name ?: "YouTube Music",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    account?.email?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    if (verifying)
                        Text(
                            text = ds("settings.accounts.verifying"),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            if (!logged && !loginActive) {
                // ACCEDI
                Text(
                    text = ds("settings.accounts.login"),
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(RiPlayAccent)
                        .clickable {
                            KcefEngine.start() // idempotente: il motore serve al browser di login
                            loginStatus = null
                            // STEP 3ai: azzera anche la diagnostica di polling
                            pollCount = 0
                            pollDiag = null
                            lastProcessedCookie = null
                            loginActive = true
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (logged) {
                // ESCI
                Text(
                    text = ds("settings.accounts.logout"),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            DesktopAccountManager.logout()
                            account = null
                            // STEP 3ai: chiude anche selettore/browser/diagnostica
                            showSelector = false
                            loginActive = false
                            pollCount = 0
                            pollDiag = null
                            lastProcessedCookie = null
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // FIX step3af: feedback VISIBILE quando il cookie e' stato raccolto
        // ma la verifica dell'account non e' riuscita (prima l'utente non
        // riceveva nessun segnale e sembrava che il login fosse ignorato)
        if (loginStatus == false) {
            Text(
                text = ds("settings.accounts.verify_failed"),
                color = Color(0xFFFFC107),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        // STEP 3ah: SCELTA DEL PROFILO (come CachedAccountsSelectorDialog
        // Android, resa come pannello inline della tab). Appare quando la
        // cache dei profili (getAccountsList) contiene piu' di un account.
        // Alla scelta: pageId/authUser + info profilo persistiti, Environment
        // impostato, il browser naviga su signinUrl e il NUOVO cookie viene
        // raccolto e persistito dal polling (stesso flusso Android).
        if (showSelector && cachedAccounts.isNotEmpty()) {
            Text(
                text = ds("settings.accounts.select_account"),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                cachedAccounts.forEach { profile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                showSelector = false
                                switchedOnce = true
                                coroutineScope.launch {
                                    DesktopAccountManager.selectProfile(profile, account)
                                    account = DesktopAccountManager.load()
                                    // naviga il browser integrato su signinUrl:
                                    // il vecchio browser viene smontato e ne nasce
                                    // uno nuovo sull'URL del profilo (key(loginUrl))
                                    loginBrowser?.let { b -> runCatching { b.dispose() } }
                                    loginBrowser = null
                                    loginUrl = DesktopAccountManager.switchUrl(profile)
                                        ?: DesktopAccountManager.loginUrl()
                                    // aspetta il NUOVO cookie del profilo (diverso
                                    // dall'attuale) prima di chiudere l'area
                                    lastHarvestedCookie = account?.cookie ?: ""
                                    pendingSwitchWait = true
                                    println("[RiPlay-Account] switch profilo: browser su $loginUrl")
                                }
                            }
                            .padding(10.dp)
                    ) {
                        profile.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp))
                            )
                        }
                        Column {
                            Text(
                                text = profile.name ?: profile.channelHandle ?: "YouTube Music",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            profile.email?.takeIf { it.isNotBlank() }?.let {
                                Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                        if (profile.isSelected == true ||
                            (profile.pageId != null && profile.pageId == account?.pageId)) {
                            Text(
                                text = "\u2713",
                                color = RiPlayAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // mantiene l'account corrente (profilo di default) e chiude
                Text(
                    text = ds("settings.accounts.keep_account"),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable {
                            showSelector = false
                            loginActive = false
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // ── AREA DI LOGIN (browser KCEF incorporato) ────────────────────
        // FIX step3ai: mentre il selettore di profili e' aperto il browser
        // NON viene disegnato: il pannello di scelta prende il suo posto
        // (come il CachedAccountsSelectorDialog Android) invece di restare
        // accanto/sotto la finestra da 420 dp, con la dicitura "In attesa..."
        // che sembrava non finire mai.
        if (loginActive && !showSelector) {
            Text(
                text = ds("settings.accounts.login_instructions"),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            when (val s = kcefState) {
                is KcefState.Ready -> key(loginUrl) {
                    // STEP 3aj: il browser di login vive su un client KCEF
                    // DEDICATO (slot handler propri: la cattura header Cookie
                    // non tocca il client del player). Finche' non e' pronto
                    // si mostra "in preparazione"; se la creazione fallisse
                    // la factory ricade sul client del motore.
                    val dedicatedClient: KCEFClient? = loginClientReady
                    if (dedicatedClient == null) {
                        Text(
                            text = ds("settings.accounts.kcef_not_ready", "state" to ds("kcef.preparing_short")),
                            color = Color(0xFFFFC107),
                            fontSize = 12.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            androidx.compose.ui.awt.SwingPanel(
                                factory = {
                                    var b = loginBrowser
                                    if (b == null) {
                                        val client: KCEFClient = dedicatedClient
                                            ?: KcefEngine.clientOrNull()
                                            ?: error("client KCEF assente in stato Ready")
                                        b = client.createBrowser(
                                            loginUrl,
                                            CefRendering.DEFAULT,
                                            false
                                        )
                                        loginBrowser = b
                                        b.createImmediately() // la factory gira sull'EDT
                                        println("[RiPlay-Account] browser login aperto su $loginUrl (client dedicato)")
                                    }
                                    b.uiComponent
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                else -> Text(
                    text = ds("settings.accounts.kcef_not_ready", "state" to kcefStateLabel(s)),
                    color = Color(0xFFFFC107),
                    fontSize = 12.sp
                )
            }

            Text(
                text = ds("settings.accounts.waiting_login"),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // STEP 3ai: diagnostica LIVE del polling (prima solo in console).
            // Se la riga resta ferma su "nessun cookie trovato" mentre nel
            // browser YouTube Music e' gia' aperto con l'account, il problema
            // e' nella lettura cookie di JCEF e si manda il log console; se
            // dice "cookie presenti ma senza SAPISID" l'accesso nel browser
            // non e' ancora andato a buon fine.
            if (pollCount > 0) {
                val detail = when (pollDiag) {
                    DesktopAccountManager.HarvestDiag.NO_MANAGER -> ds("settings.accounts.poll_nomanager")
                    DesktopAccountManager.HarvestDiag.NO_COOKIES -> ds("settings.accounts.poll_none")
                    DesktopAccountManager.HarvestDiag.NO_YT_COOKIES -> ds("settings.accounts.poll_noyt")
                    DesktopAccountManager.HarvestDiag.NO_SAPISID -> ds("settings.accounts.poll_noauth")
                    DesktopAccountManager.HarvestDiag.VISIT_REFUSED -> ds("settings.accounts.poll_visitrefused")
                    DesktopAccountManager.HarvestDiag.OK -> ds("settings.accounts.poll_found")
                    null -> ""
                }
                Text(
                    text = ds("settings.accounts.poll_attempts", "n" to pollCount, "detail" to detail),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // STEP 3ai: verifica manuale immediata (il polling ogni 2 s resta
            // attivo): forza un ciclo di raccolta+verifica e aggiorna anche la
            // diagnostica, utile se l'accesso nel browser e' gia' completato.
            Text(
                text = ds("settings.accounts.verify_now"),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable {
                        if (!processing) coroutineScope.launch { processHarvest(force = true) }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/** chiave per ri-verificare l'account salvato a ogni ingresso nella tab */
private fun activeTabKey(account: DesktopAccountManager.StoredAccount?, loginActive: Boolean): Any =
    "${account?.cookie?.length ?: 0}/$loginActive"

private fun kcefStateLabel(s: KcefState): String = when (s) {
    KcefState.NotStarted, KcefState.Locating -> ds("kcef.preparing")
    is KcefState.Downloading -> ds("kcef.downloading")
    KcefState.Extracting -> ds("kcef.extracting")
    KcefState.Installing -> ds("kcef.installing")
    KcefState.Initializing -> ds("kcef.starting")
    else -> ds("kcef.preparing_short")
}

/** tab delle Impostazioni (stessa partizione di Android: Dati, Account, Info) */
enum class SettingsTab(val labelKey: String) {
    DATA("settings.tab.data"),
    ACCOUNTS("settings.tab.accounts"),
    ABOUT("settings.tab.about")
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB ABOUT (STEP 4b, richiesta utente n.2): replica della schermata About
// di Android (ui/screens/settings/About.kt): versione, social, pagina del
// progetto, troubleshooting. I link aprono il browser di sistema.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Riga di presentazione (Android: "RiPlay <versione> by fast4x")
        Text(
            text = ds("settings.about.tagline", "version" to DesktopAppInfo.VERSION),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp
        )

        AboutSectionTitle(ds("settings.about.social"))
        AboutEntry(
            title = ds("settings.about.telegram"),
            subtitle = ds("settings.about.telegram_info"),
            url = "https://t.me/riplay_app"
        )
        AboutEntry(
            title = ds("settings.about.reddit"),
            subtitle = ds("settings.about.reddit_info"),
            url = "https://www.reddit.com/r/RiMusicApp/"
        )

        AboutSectionTitle(ds("settings.about.project"))
        AboutEntry(
            title = ds("settings.about.github"),
            subtitle = ds("settings.about.github_info"),
            url = "https://github.com/fast4x/RiPlay"
        )
        AboutEntry(
            title = ds("settings.about.releases"),
            subtitle = ds("settings.about.releases_info"),
            url = "https://github.com/fast4x/RiPlay/releases/latest"
        )

        AboutSectionTitle(ds("settings.about.troubleshooting"))
        AboutEntry(
            title = ds("settings.about.report_issue"),
            subtitle = ds("settings.about.github_redirect"),
            url = "https://github.com/fast4x/RiPlay/issues/new?assignees=&labels=bug&template=bug_report.yaml"
        )
        AboutEntry(
            title = ds("settings.about.request_feature"),
            subtitle = ds("settings.about.github_redirect"),
            url = "https://github.com/fast4x/RiPlay/issues/new?assignees=&labels=enhancement&template=feature_request.yaml"
        )
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        color = RiPlayAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/** voce cliccabile della tab About (equivalente desktop di SettingsEntry Android) */
@Composable
private fun AboutEntry(title: String, subtitle: String, url: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable { openInBrowser(url) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp)
        if (subtitle.isNotBlank())
            Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

/**
 * Apre l'URL nel browser predefinito del sistema (l'equivalente desktop di
 * LocalUriHandler.openUri su Android). Best-effort: in mancanza di un
 * browser configurabile l'errore va in console e non blocca la UI.
 */
private fun openInBrowser(url: String) {
    runCatching {
        val desktop = java.awt.Desktop.getDesktop()
        if (desktop.isSupported(java.awt.Desktop.Action.BROWSE))
            desktop.browse(java.net.URI(url))
    }.onFailure { it.printStackTrace() }
}
