package it.fast4x.riplay.extensions.lastfm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.player.kcef.KcefState
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.utils.DesktopAccountsStore

/**
 * PORTING DESKTOP di LastFmAuthScreen.kt (androidApp): flusso di
 * autenticazione Last.fm per la tab Account.
 *
 * Stesso flusso di LastFmAuthViewModel Android:
 *  1. richiesta del token (auth.gettoken, firmato con l'API secret);
 *  2. pagina di autorizzazione https://www.last.fm/api/auth/?api_key=...&token=...
 *     aperta nel browser integrato (KCEF al posto della WebView Android);
 *  3. rilevamento dell'approvazione: Android guarda il TITOLO pagina
 *     ("authenticated"), qui facciamo lo stesso con CefDisplayHandler
 *     onTitleChange;
 *  4. getSession(token) -> chiave di sessione salvata in
 *     DesktopAccountsStore.lastFmSessionKey (~/.riplay/accounts.properties).
 *
 * In piu' rispetto ad Android c'e' un bottone "Confermo manualmente":
 * getSession e' idempotente e funziona appena l'utente ha approvato, anche
 * se il rilevamento del titolo non ha combaciato (titolo in altra lingua).
 */
private sealed interface LastFmAuthState {
    data object LoadingToken : LastFmAuthState
    data class BrowserReady(val authUrl: String, val token: String) : LastFmAuthState
    data object FetchingSession : LastFmAuthState
    data class Error(val message: String) : LastFmAuthState
}

@Composable
fun DesktopLastFmAuthPanel(
    onAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LastFmAuthState>(LastFmAuthState.LoadingToken) }
    var connectedUser by remember { mutableStateOf<String?>(null) }
    val kcefState by KcefEngine.state.collectAsState()

    // Il browser integrato serve anche qui (idempotente)
    LaunchedEffect(Unit) { KcefEngine.start() }

    // 1. richiede il token e apre la pagina di autorizzazione
    LaunchedEffect(Unit) {
        state = LastFmAuthState.LoadingToken
        runCatching {
            withContext(Dispatchers.IO) {
                DesktopLastFmClient.service.getAuthToken().getOrThrow()
            }
        }.onSuccess { token ->
            state = LastFmAuthState.BrowserReady(DesktopLastFmClient.authUrl(token), token)
        }.onFailure { e ->
            println("[RiPlay-LastFM] errore getAuthToken: ${e.message}")
            state = LastFmAuthState.Error(e.message ?: "token error")
        }
    }

    // 4. getSession(token) + salvataggio + callback
    suspend fun fetchSessionAndFinish(token: String) {
        state = LastFmAuthState.FetchingSession
        runCatching {
            withContext(Dispatchers.IO) {
                DesktopLastFmClient.service.getSession(token).getOrThrow()
            }
        }.onSuccess { session ->
            DesktopAccountsStore.updateLastFmSessionKey(session.key)
            connectedUser = session.name
            println("[RiPlay-LastFM] autenticato come ${session.name}")
            onAuthSuccess()
        }.onFailure { e ->
            println("[RiPlay-LastFM] errore getSession: ${e.message}")
            state = LastFmAuthState.Error(e.message ?: "session error")
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        when (val s = state) {
            LastFmAuthState.LoadingToken -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = ds("settings.lastfm.generating_token"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            is LastFmAuthState.BrowserReady -> {
                Text(
                    text = ds("settings.lastfm.auth_instructions"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                when (val k = kcefState) {
                    is KcefState.Ready -> LastFmAuthBrowser(
                        authUrl = s.authUrl,
                        onApproved = { coroutineScope.launch { fetchSessionAndFinish(s.token) } }
                    )

                    else -> Text(
                        text = ds("settings.accounts.kcef_not_ready", "state" to kcefStateLabel(k)),
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = ds("settings.lastfm.verify_now"),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            if (state is LastFmAuthState.BrowserReady)
                                coroutineScope.launch { fetchSessionAndFinish(s.token) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            LastFmAuthState.FetchingSession -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = ds("settings.lastfm.fetching_session"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            is LastFmAuthState.Error -> Text(
                text = ds("settings.lastfm.auth_error", "error" to s.message),
                color = Color(0xFFFF7A7A),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        connectedUser?.let {
            Text(
                text = ds("settings.lastfm.authenticated_as", "user" to it),
                color = RiPlayAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Browser KCEF dedicato alla pagina di autorizzazione Last.fm: un client
 * CEF nuovo (slot handler propri, come il login YouTube Music) con un
 * CefDisplayHandler che controlla il TITOLO della pagina: quando contiene
 * "authenticated" (pagina Last.fm dopo l'approvazione, stesso rilevamento
 * della WebViewClient Android) si rilancia getSession.
 */
@Composable
private fun LastFmAuthBrowser(
    authUrl: String,
    onApproved: () -> Unit
) {
    var client by remember { mutableStateOf<CefClient?>(null) }
    var browser by remember { mutableStateOf<CefBrowser?>(null) }
    val approvedRef = remember { mutableStateOf(false) }

    DisposableEffect(authUrl) {
        val c: CefClient? = runCatching { CefApp.getInstance().createClient() }
            .onFailure { println("[RiPlay-LastFM] client CEF fallito: ${it.message}") }
            .getOrNull()
        c?.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onTitleChange(browser: CefBrowser, title: String?) {
                // Stesso rilevamento di LastFmAuthWebView Android
                val t = title?.lowercase().orEmpty()
                if (t.contains("authenticated") || t.contains("authorized")) {
                    if (!approvedRef.value) {
                        approvedRef.value = true
                        println("[RiPlay-LastFM] approvazione rilevata dal titolo pagina: '$title'")
                        onApproved()
                    }
                }
            }
        })
        client = c
        onDispose {
            runCatching { browser?.stopLoad() }
            runCatching { browser?.setCloseAllowed() }
            runCatching { browser?.close(true) }
            browser = null
            println("[RiPlay-LastFM] browser autorizzazione chiuso")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        client?.let { cl ->
            key(authUrl) {
                androidx.compose.ui.awt.SwingPanel(
                    factory = {
                        var b = browser
                        if (b == null) {
                            b = cl.createBrowser(authUrl, false, false)
                            browser = b
                            b.createImmediately()
                            println("[RiPlay-LastFM] browser autorizzazione aperto su $authUrl")
                        }
                        b.uiComponent
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: Text(
            text = ds("settings.accounts.kcef_not_ready", "state" to ds("kcef.preparing_short")),
            color = Color(0xFFFFC107),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center).padding(12.dp)
        )
    }
}

private fun kcefStateLabel(s: KcefState): String = when (s) {
    KcefState.NotStarted, KcefState.Locating -> ds("kcef.preparing")
    is KcefState.Downloading -> ds("kcef.downloading")
    KcefState.Extracting -> ds("kcef.extracting")
    KcefState.Installing -> ds("kcef.installing")
    KcefState.Initializing -> ds("kcef.starting")
    else -> ds("kcef.preparing_short")
}
