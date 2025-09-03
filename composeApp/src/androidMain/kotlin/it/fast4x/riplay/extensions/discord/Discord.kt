package it.fast4x.riplay.extensions.discord

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.context
import it.fast4x.riplay.ui.components.themed.Title
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.withContext
import timber.log.Timber

    /**
     * Get the discord user
     */
private const val JS_SNIPPET = "javascript:(function(){var i=document.createElement('iframe');document.body.appendChild(i);alert(i.contentWindow.localStorage.token.slice(1,-1))})()"
private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

    /**
     * Get the discord user info
     */

suspend fun fetchDiscordUser(token: String): Pair<String, String>? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://discord.com/api/v9/users/@me")
        .header("Authorization", token)
        .get()
        .build()
    runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag("DiscordPresence").e("Failed to fetch Discord user: ${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val username = json.getString("username")
            val id = json.getString("id")
            val avatar = json.optString("avatar", "")
            val avatarUrl = if (avatar.isNotEmpty())
                "https://cdn.discordapp.com/avatars/$id/$avatar.png"
            else
                "https://cdn.discordapp.com/embed/avatars/${id.toLong() % 5}.png"
            Pair(username, avatarUrl)
        }
    }.getOrElse { exception ->
        // Handle rate limiting silently to avoid disturbing the user
        if (exception.message?.contains("429") == true || exception.message?.contains("Too Many Requests") == true) {
            Timber.tag("DiscordPresence").d("Rate limited by Discord API while fetching user info")
        } else {
            Timber.tag("DiscordPresence").e(exception, "Error fetching Discord user: ${exception.message}")
        }
        null
    }
}


    /**
     * Login to discord and get the token
     */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginAndGetToken(
    navController: NavController,
    onGetToken: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var webView: WebView? = null

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title(
            context().resources.getString(R.string.discord_connect),
            icon = R.drawable.chevron_down,
            onClick = { navController.navigateUp() }
        )

        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
                            webView.stopLoading()
                            if (url.endsWith("/app")) {
                                webView.loadUrl(JS_SNIPPET)
                                webView.visibility = View.GONE
                            }
                            return false
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            if (url.contains("/app")) {
                                view.loadUrl(JS_SNIPPET)
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onJsAlert(
                            view: WebView,
                            url: String,
                            message: String,
                            result: JsResult,
                        ): Boolean {
                            scope.launch(Dispatchers.Main) {
                                val token = message
                                val user = fetchDiscordUser(token)
                                if (user != null) {
                                    onGetToken(token, user.first, user.second)
                                } else {
                                    onGetToken(token, "", "")
                                }
                                navController.navigateUp()
                            }
                            this@apply.visibility = View.GONE
                            result.confirm()
                            return true
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    if (Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                        settings.userAgentString = SAMSUNG_USER_AGENT
                    }
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()
                    WebStorage.getInstance().deleteAllData()
                    webView = this
                    loadUrl("https://discord.com/login")
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
}