package it.fast4x.riplay.extensions.discord

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.ui.components.RiPlayAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefJSDialogCallback
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefJSDialogHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef

/**
 * PORTING DESKTOP di DiscordLoginAndGetToken.kt (androidApp): login Discord
 * che recupera il token utente per la Rich Presence.
 *
 * MECCANISMO (stesso dell'originale Android, WebView -> KCEF):
 *  1. il browser integrato apre https://discord.com/login;
 *  2. quando l'URL finisce con "/app" (login riuscito, app Discord caricata),
 *     viene iniettato lo STESSO snippet JavaScript dell'app Android che legge
 *     localStorage.token dentro un iframe e lo esporta con alert();
 *  3. il dialog JavaScript (alert) viene intercettato con CefJSDialogHandler
 *     (equivalente di WebChromeClient.onJsAlert): il messaggio E' il token;
 *  4. fetchDiscordUser(token) -> username/avatar -> onGetToken(...).
 *
 * In piu' rispetto ad Android: campo di inserimento MANUALE del token
 * (il token da localStorage non e' piu' disponibile su alcuni account /
 * versioni di Discord: la via manuale garantisce che la connessione resti
 * possibile; il token si copia dal client Discord: View > Toggle Developer
 * Tools -> Application > Local Storage > localStorage.token).
 */
private const val DISCORD_LOGIN_URL = "https://discord.com/login"

/** Stesso snippet JavaScript dell'Android (Discord.kt JS_SNIPPET). */
private const val DISCORD_JS_SNIPPET =
    "javascript:(function(){var i=document.createElement('iframe');document.body.appendChild(i);alert(i.contentWindow.localStorage.token.slice(1,-1))})()"

/**
 * Recupera username e avatar del token Discord (come fetchDiscordUser Android).
 * @return (username, avatarUrl) o null
 */
suspend fun fetchDiscordUser(token: String): Pair<String, String>? =
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://discord.com/api/v9/users/@me")
            .header("Authorization", token)
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("[RiPlay-Discord] fetchDiscordUser http ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val json = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                val username = json["username"]?.jsonPrimitive?.content ?: return@withContext null
                val id = json["id"]?.jsonPrimitive?.content ?: return@withContext null
                val avatar = json["avatar"]?.jsonPrimitive?.content ?: ""
                val avatarUrl = if (avatar.isNotEmpty())
                    "https://cdn.discordapp.com/avatars/$id/$avatar.png"
                else
                    "https://cdn.discordapp.com/embed/avatars/${(id.toLongOrNull() ?: 0L) % 5}.png"
                Pair(username, avatarUrl)
            }
        }.getOrElse { e ->
            println("[RiPlay-Discord] errore fetchDiscordUser: ${e.message}")
            null
        }
    }

@Composable
fun DesktopDiscordLoginPanel(
    onGetToken: (token: String, username: String, avatarUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var client by remember { mutableStateOf<CefClient?>(null) }
    var browser by remember { mutableStateOf<CefBrowser?>(null) }
    var browserUrl by remember { mutableStateOf(DISCORD_LOGIN_URL) }
    // Fallback manuale: il token incollato a mano
    var manualToken by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { KcefEngine.start() }

    fun deliver(token: String) {
        coroutineScope.launch {
            val user = fetchDiscordUser(token)
            if (user != null) {
                onGetToken(token, user.first, user.second)
            } else {
                // Token per gli endpoint API rifiutato o rete offline: si
                // consegna comunque (Android fa lo stesso con onGetToken(token,"",""))
                onGetToken(token, "", "")
            }
            onDismiss()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text(
            text = ds("settings.discord.login_instructions"),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        // ── BROWSER INTEGRATO con iniezione JS (meccanismo Android) ────────
        DisposableEffect(Unit) {
            val c: CefClient? = runCatching { CefApp.getInstance().createClient() }
                .onFailure { println("[RiPlay-Discord] client CEF fallito: ${it.message}") }
                .getOrNull()
            c?.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, statusCode: Int) {
                    val url = frame?.url ?: browser.url ?: return
                    browserUrl = url
                    // Stesso scatto di Android: url che finisce con "/app"
                    // (discord.com/app dopo il login) -> iniezione snippet
                    if (url.endsWith("/app")) {
                        browser.executeJavaScript(DISCORD_JS_SNIPPET, url, 0)
                        println("[RiPlay-Discord] URL /app raggiunto: snippet localStorage.token iniettato")
                    }
                }
            })
            // L'alert() di export del token arriva qui (equivalente di
            // WebChromeClient.onJsAlert Android): CEF espone alert via
            // onJSDialog con dialogType ALERT. true = gestito dall'app,
            // il dialog non si apre all'utente.
            c?.addJSDialogHandler(object : CefJSDialogHandlerAdapter() {
                override fun onJSDialog(
                    browser: CefBrowser,
                    originUrl: String,
                    dialogType: CefJSDialogHandler.JSDialogType,
                    messageText: String,
                    defaultPromptText: String,
                    callback: CefJSDialogCallback,
                    suppressMessage: BoolRef
                ): Boolean {
                    if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT &&
                        messageText.isNotBlank() && messageText != "undefined"
                    ) {
                        println("[RiPlay-Discord] token catturato (${messageText.length} caratteri)")
                        deliver(messageText)
                    }
                    callback.Continue(true, "")
                    return true
                }
            })
            client = c
            onDispose {
                runCatching { browser?.stopLoad() }
                runCatching { browser?.setCloseAllowed() }
                runCatching { browser?.close(true) }
                browser = null
                println("[RiPlay-Discord] browser login chiuso")
            }
        }

        client?.let { cl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                androidx.compose.ui.awt.SwingPanel(
                    factory = {
                        var b = browser
                        if (b == null) {
                            b = cl.createBrowser(DISCORD_LOGIN_URL, false, false)
                            browser = b
                            b.createImmediately()
                            println("[RiPlay-Discord] browser login aperto su $DISCORD_LOGIN_URL")
                        }
                        b.uiComponent
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: Text(
            text = ds("settings.accounts.kcef_not_ready", "state" to ds("kcef.preparing_short")),
            color = Color(0xFFFFC107),
            fontSize = 12.sp
        )

        // ── FALLBACK: TOKEN MANUALE ─────────────────────────────────────────
        Text(
            text = ds("settings.discord.manual_token_title"),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = ds("settings.discord.manual_token_instructions"),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = manualToken,
                onValueChange = { manualToken = it; manualError = null },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            )
            Text(
                text = ds("settings.discord.manual_token_connect"),
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RiPlayAccent)
                    .clickable {
                        val t = manualToken.trim()
                        if (t.isBlank()) {
                            manualError = ds("settings.discord.manual_token_empty")
                        } else {
                            deliver(t)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
        manualError?.let {
            Text(text = it, color = Color(0xFFFF7A7A), fontSize = 11.sp)
        }

        Text(
            text = ds("settings.accounts.waiting_login"),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
