package it.fast4x.riplay.extensions.youtubelogin

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import it.fast4x.environment.Environment
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytAccountNameKey
import it.fast4x.riplay.extensions.preferences.ytAccountEmailKey
import it.fast4x.riplay.extensions.preferences.ytAccountChannelHandleKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.ytAccountThumbnailKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.utils.restartApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    onLogin: (String) -> Unit
) {

    val scope = rememberCoroutineScope()
    var webView: WebView? = null


    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            var cookie = ""
            var dataSyncId = ""
            var visitorData = ""
            var onPageFinished = false

            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                        loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                        if (url?.startsWith("https://music.youtube.com") == true && !onPageFinished) {
                            cookie = CookieManager.getInstance().getCookie(url)
                            onPageFinished = true

                            scope.launch {
                                delay(200)

                                Timber.d("YouTubeLogin: save login preferences")
                                context.preferences.edit(commit = true) { putString(ytVisitorDataKey, visitorData) }
                                context.preferences.edit(commit = true) { putString(ytDataSyncIdKey, dataSyncId) }
                                context.preferences.edit(commit = true) { putString(ytCookieKey, cookie) }
                                delay(200)

                                Timber.d("YouTubeLogin: Initialize Environment")

                                Environment.cookie = cookie
                                Environment.dataSyncId = dataSyncId
                                Environment.visitorData = visitorData

                                Timber.d("YouTubeLogin: Initialized, get account info")

                                Environment.accountInfo().onSuccess {
                                    context.preferences.edit(commit = true) { putString(ytAccountNameKey, it?.name.orEmpty()) }
                                    context.preferences.edit(commit = true) { putString(ytAccountEmailKey, it?.email.orEmpty()) }
                                    context.preferences.edit(commit = true) { putString(ytAccountChannelHandleKey, it?.channelHandle.orEmpty()) }
                                    context.preferences.edit(commit = true) { putString(ytAccountThumbnailKey, it?.thumbnailUrl.orEmpty()) }
                                    delay(200)

                                    Timber.d("YouTubeLogin: Logged in as ${it?.name}, restarting app...")

                                    webView?.apply {
                                        stopLoading()
                                        clearHistory()
                                        clearCache(true)
                                        clearFormData()
                                    }

                                    restartApp(context)

                                }.onFailure {
                                    Timber.e(it, "YouTubeLogin: Authentication error")
                                    onPageFinished = false
                                }
                            }
                        }
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

            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

}

