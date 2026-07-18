package it.fast4x.riplay.extensions.accountlogin

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.environment.Environment
import it.fast4x.environment.models.responses.CachedAccountProfile
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.CachedAccountsSelectorDialog
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONTokener
import timber.log.Timber

private const val VISITOR_DATA_SCRIPT =
    "(function() { return window.yt && window.yt.config_ ? window.yt.config_.VISITOR_DATA : null; })()"
private const val DATA_SYNC_ID_SCRIPT =
    "(function() { return window.yt && window.yt.config_ ? window.yt.config_.DATASYNC_ID : null; })()"

private fun String?.fromJavascriptString(): String? {
    val value = this?.takeIf {
        it.isNotBlank() && it != "null" && it != "undefined"
    } ?: return null

    val parsedValue = runCatching {
        JSONTokener(value).nextValue() as? String
    }.getOrNull() ?: value

    return parsedValue.takeIf {
        it.isNotBlank() && it != "null" && it != "undefined"
    }
}

@Composable
fun AccountLogin(
    onLogin: (String) -> Unit
) {

    val scope = rememberCoroutineScope()
    var webView: WebView? = null
    //var showConfirmButton by remember { mutableStateOf(false) }
    //var restartAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loadSessionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val localContext = LocalContext.current

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    var signinUrl by remember { mutableStateOf("") }
    var showSelectorDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    //val jsonCachedAccounts by rememberPreference(PreferenceKey.YT_CACHED_ACCOUNTS.key, "")
    val jsonCachedAccounts = appSettings.ytCachedAccounts
    Timber.d("AccountLogin INITIAL CachedAccountProfile jsonString $jsonCachedAccounts ")
    val cachedAccounts = remember(jsonCachedAccounts) {
        try {
            Json.decodeFromString<List<CachedAccountProfile>>(jsonCachedAccounts)
        } catch (e: Exception) {
            Timber.e(e, "Errore nel parsing della cache account")
            emptyList()
        }
    }
    Timber.d("AccountLogin INITIAL CachedAccountProfile cachedAccounts $cachedAccounts ")


    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current))
    {
        if (loading)
            DefaultDialog(onDismiss = {}) {
                LoaderScreen()
            }


        if (cachedAccounts.isNotEmpty() && showSelectorDialog) {
            CachedAccountsSelectorDialog(
                onDismiss = { showSelectorDialog = false },
                title = stringResource(R.string.login_select_account),
                cachedAccounts = cachedAccounts,
                onValueSelected = { account ->
                    Timber.d("AccountLogin selected account $account")
                    scope.launch {
                        delay(200)

                        Timber.d("AccountLogin: save login preferences")
                        appSettingsManager.updateSettings(
                            appSettings.copy(
                                ytPageId = account.pageId.toString(),
                                ytAuthUser = account.authUser.toString(),
                                ytAccountName = account.name.toString(),
                                ytAccountEmail = account.email.toString(),
                                ytAccountChannelHandle = account.channelHandle.toString(),
                                ytAccountThumbnail = account.thumbnailUrl.toString()
                            )
                        )
//                        localContext.preferences.edit {putString(PreferenceKey.YT_PAGEID.key, account.pageId)}
//                        localContext.preferences.edit {putString(PreferenceKey.YT_AUTHUSER.key, account.authUser)}
//                        localContext.preferences.edit {putString(PreferenceKey.YT_ACCOUNT_NAME.key, account.name)}
//                        localContext.preferences.edit {putString(PreferenceKey.YT_ACCOUNT_EMAIL.key, account.email)}
//                        localContext.preferences.edit {putString(PreferenceKey.YT_ACCOUNT_CHANNEL_HANDLE.key, account.channelHandle)}
//                        localContext.preferences.edit {putString(PreferenceKey.YT_ACCOUNT_THUMBNAIL.key, account.thumbnailUrl)}
                        delay(200)

                        signinUrl = "https://music.youtube.com${account.signinUrl.toString()}"

                        Environment.pageId = account.pageId
                        Environment.authUser = account.authUser

                        showSelectorDialog = false
                        onLogin("")
                        //restartAction?.invoke()
                    }
                }
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                var cookie = ""
                var dataSyncId = ""
                var visitorData = ""

                WebView(context).apply {
                    fun refreshYouTubeConfig(onComplete: ((String, String) -> Unit)? = null) {

                        var refreshedVisitorData = visitorData
                        var refreshedDataSyncId = dataSyncId
                        var pendingCallbacks = 2

                        fun completeRefresh() {
                            pendingCallbacks -= 1
                            if (pendingCallbacks == 0) {
                                onComplete?.invoke(refreshedVisitorData, refreshedDataSyncId)
                            }
                        }

                        evaluateJavascript(VISITOR_DATA_SCRIPT) { result ->
                            result.fromJavascriptString()?.let {
                                visitorData = it
                                refreshedVisitorData = it
                            }
                            completeRefresh()
                        }
                        evaluateJavascript(DATA_SYNC_ID_SCRIPT) { result ->
                            result.fromJavascriptString()?.substringBefore("||")?.takeIf { it.isNotBlank() }?.let {
                                dataSyncId = it
                                refreshedDataSyncId = it
                            }
                            completeRefresh()
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            loading = true
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            refreshYouTubeConfig()

                            val destinationLoaded = url?.startsWith("https://music.youtube.com") == true
                            if (destinationLoaded)
                                loadSessionAction?.invoke()

                            showSelectorDialog = true

//                            if (signinUrl.isNotEmpty()) // account switched, restart
//                                restartAction?.invoke()

                            //showConfirmButton = url?.startsWith("https://music.youtube.com") == true
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView,
                            url: String?,
                            isReload: Boolean
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            refreshYouTubeConfig()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loading = newProgress < 100
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

                    webView = this

                    val url = if (cookie.isNotEmpty()) {
                        "https://music.youtube.com"
                    } else {
                        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
                    }

                    loadUrl(url)

                    loadSessionAction = {
                        val currentUrl = this.url
                        val freshCookie = CookieManager.getInstance().getCookie(currentUrl)

                        Timber.d("AccountLogin: User confirmed login.")

                        refreshYouTubeConfig { refreshedVisitorData, refreshedDataSyncId ->
                            scope.launch {
                                delay(200)

                                Timber.d("AccountLogin: save login preferences")
                                appSettingsManager.updateSettings(
                                    appSettings.copy(
                                        ytCookie = freshCookie,
                                        ytVisitorData = refreshedVisitorData,
                                        ytDataSyncId = refreshedDataSyncId
                                    )
                                )
//                                context.preferences.edit { putString(YT_VISITOR_DATA.key, refreshedVisitorData) }
//                                context.preferences.edit { putString(YT_DATA_SYNC_ID.key, refreshedDataSyncId) }
//                                context.preferences.edit { putString(YT_COOKIE.key, freshCookie) }
                                delay(200)

                                Timber.d("AccountLogin: Initialize Environment")
                                Timber.d("AccountLogin: freshCookie $freshCookie")

                                Environment.cookie = freshCookie
                                Environment.dataSyncId = refreshedDataSyncId
                                Environment.visitorData = refreshedVisitorData

                                Timber.d("AccountLogin: Initialized, get account info")

                                Environment.accountInfo().onSuccess {
                                    appSettingsManager.updateSettings(
                                        appSettings.copy(
                                            ytAccountName = it?.name.orEmpty(),
                                            ytAccountEmail = it?.email.orEmpty(),
                                            ytAccountChannelHandle = it?.channelHandle.orEmpty(),
                                            ytAccountThumbnail = it?.thumbnailUrl.orEmpty()
                                        )
                                    )
//                                    context.preferences.edit { putString(YT_ACCOUNT_NAME.key, it?.name.orEmpty()) }
//                                    context.preferences.edit { putString(YT_ACCOUNT_EMAIL.key, it?.email.orEmpty()) }
//                                    context.preferences.edit { putString(YT_ACCOUNT_CHANNEL_HANDLE.key, it?.channelHandle.orEmpty()) }
//                                    context.preferences.edit { putString(YT_ACCOUNT_THUMBNAIL.key, it?.thumbnailUrl.orEmpty()) }
                                    delay(200)

                                    Timber.d("AccountLogin: Logged in as ${it?.name}, restarting app...")

                                }.onFailure {
                                    Timber.e(it, "AccountLogin: Authentication error")
                                }

                                /*
                                Environment.getRawAccountListWithPageId().onSuccess {
                                    saveFileToInternalStorage(context, "AccountSwitcherResponse", it)
                                }.onFailure {
                                    Timber.e(it, "AccountLogin: getRawAccountListWithPageId error ${it.message}")
                                }
                                 */
                                Environment.getAccountsList().onSuccess {
                                    Timber.d("AccountLogin: getAccountsList $it")
                                    val jsonString = Json.encodeToString(it)
                                    Timber.d("AccountLogin: getAccountsList salva jsonString $jsonString")
                                    appSettingsManager.updateSettings(
                                        appSettings.copy(ytCachedAccounts = jsonString)
                                    )
                                    //context.preferences.edit { putString(PreferenceKey.YT_CACHED_ACCOUNTS.key, jsonString) }
                                    delay(200)
                                }.onFailure {
                                    Timber.e(it, "AccountLogin: getAccountsList error ${it.message}")
                                }

                            }
                        }
                    }

                    /*
                    restartAction = {
                        webView.apply {
                            stopLoading()
                            clearHistory()
                            clearCache(true)
                            clearFormData()
                        }

                        Timber.d("AccountLogin: Restart app")
                        restartApp(context)
                    }
                     */

                }
            },
            update = { webView ->
                if (signinUrl.isNotEmpty()) {
                    loading = true
                    Timber.d("AccountLogin carico signinUrl nella webview per lo switch -> $signinUrl")
                    webView.loadUrl(signinUrl)

                    signinUrl = ""
                }
            }
        )

        /*
        if (showConfirmButton && restartAction != null) {
            Button(
                shape = getRoundnessShape(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().accent,
                    contentColor = colorPalette().onAccent
                ),
                onClick = { restartAction?.invoke() },
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

         */
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
