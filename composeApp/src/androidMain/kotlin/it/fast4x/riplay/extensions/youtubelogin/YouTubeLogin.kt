package it.fast4x.riplay.extensions.youtubelogin

import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import it.fast4x.environment.Environment
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.ytAccountChannelHandleKey
import it.fast4x.riplay.extensions.preferences.ytAccountEmailKey
import it.fast4x.riplay.extensions.preferences.ytAccountNameKey
import it.fast4x.riplay.extensions.preferences.ytAccountThumbnailKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.restartApp
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun YouTubeLogin(
    onLogin: (String) -> Unit
) {

    val scope = rememberCoroutineScope()
    var webView: WebView? = null

    var showConfirmButton by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                var cookie = ""
                var dataSyncId = ""
                var visitorData = ""

                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            showConfirmButton = url?.startsWith("https://music.youtube.com") == true
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        val userAgent = settings.userAgentString
                        settings.userAgentString = userAgent.replace("; wv", "")
                    }
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorData = newVisitorData
                            }
                        }
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null) {
                                dataSyncId = newDataSyncId.substringBefore("||")
                            }
                        }
                    }, "Android")

                    webView = this

                    val url = if (cookie.isNotEmpty()) {
                        "https://music.youtube.com"
                    } else {
                        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
                    }

                    loadUrl(url)

                    confirmAction = {
                        val currentUrl = this.url
                        val freshCookie = CookieManager.getInstance().getCookie(currentUrl)

                        Timber.d("YouTubeLogin: User confirmed login.")

                        scope.launch {
                            delay(200)

                            Timber.d("YouTubeLogin: save login preferences")
                            context.preferences.edit { putString(ytVisitorDataKey, visitorData) }
                            context.preferences.edit { putString(ytDataSyncIdKey, dataSyncId) }
                            context.preferences.edit { putString(ytCookieKey, freshCookie) }
                            delay(200)

                            Timber.d("YouTubeLogin: Initialize Environment")
                            Timber.d("YouTubeLogin: freshCookie $freshCookie")

                            Environment.cookie = freshCookie
                            Environment.dataSyncId = dataSyncId
                            Environment.visitorData = visitorData

                            Timber.d("YouTubeLogin: Initialized, get account info")

                            Environment.accountInfo().onSuccess {
                                context.preferences.edit { putString(ytAccountNameKey, it?.name.orEmpty()) }
                                context.preferences.edit { putString(ytAccountEmailKey, it?.email.orEmpty()) }
                                context.preferences.edit { putString(ytAccountChannelHandleKey, it?.channelHandle.orEmpty()) }
                                context.preferences.edit { putString(ytAccountThumbnailKey, it?.thumbnailUrl.orEmpty()) }
                                delay(200)

                                Timber.d("YouTubeLogin: Logged in as ${it?.name}, restarting app...")

                            }.onFailure {
                                Timber.e(it, "YouTubeLogin: Authentication error")
                            }

                            webView.apply {
                                stopLoading()
                                clearHistory()
                                clearCache(true)
                                clearFormData()
                            }

                            Timber.d("YouTubeLogin: Restart app")
                            restartApp(context)

                        }
                    }
                }
            }
        )

        if (showConfirmButton && confirmAction != null) {
            Button(
                shape = getRoundnessShape(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().accent,
                    contentColor = colorPalette().onAccent
                ),
                onClick = { confirmAction?.invoke() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text(
                    stringResource(R.string.login_select_your_preferred_account_or_profile_and_click_here_to_confirm_access),
                    fontSize = typography().l.fontSize
                )
            }
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
