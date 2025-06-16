package it.fast4x.riplay.extensions.discord

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.IconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginAndGetToken(
    navController: NavController,
    onGetToken: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var webView: WebView? = null

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
                    override fun shouldOverrideUrlLoading(
                        webView: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        stopLoading()
                        if (request.url.toString().endsWith("/app")) {
                            loadUrl("javascript:Android.onRetrieveToken((webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==void 0).exports.default.getToken());")
                        }
                        return false
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()

                WebStorage.getInstance().deleteAllData()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveToken(token: String) {
                        scope.launch(Dispatchers.Main) {
                            onGetToken(token)
                        }
                    }
                }, "Android")

                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text("Login to Discord") },
        navigationIcon = {
            IconButton(
                icon = R.drawable.chevron_back,
                onClick = navController::navigateUp,
                color = Color.White
            )
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

@UnstableApi
fun sendDiscordPresence(
    token: String,
    mediaItem: MediaItem,
    timeStart: Long,
    timeEnd: Long
) {
    if (token.isEmpty()) return

    val rpc = KizzyRPC(token)
    rpc.setActivity(
        activity = Activity(
            applicationId = "1281989764358082570",
            name = "RiMusic",
            details = mediaItem.mediaMetadata.title.toString(),
            state = mediaItem.mediaMetadata.artist.toString(),
            type = TypeDiscordActivity.LISTENING.value,
            timestamps = Timestamps(
                start = timeStart,
                end = timeEnd
            ),
            assets = Assets(
                largeImage = "https://i.ytimg.com/vi/${mediaItem.mediaId}/maxresdefault.jpg",
                smallImage = "mp:{icona_rimusic}",
                //largeText = mediaItem.mediaMetadata.title.toString(),
                //smallText = mediaItem.mediaMetadata.artist.toString(),
            ),
            buttons = listOf("Get RiMusic", "Listen to YTMusic"),
            metadata = com.my.kizzyrpc.model.Metadata(
                listOf(
                    "https://rimusic.xyz/",
                    "https://music.youtube.com/watch?v=${mediaItem.mediaId}",
                )
            )
        ),
        status = "online",
        since = System.currentTimeMillis()
    )
}

enum class TypeDiscordActivity (val value: Int) {
    PLAYING(0),
    STREAMING(1),
    LISTENING(2),
    WATCHING(3),
    COMPETING(5)
}