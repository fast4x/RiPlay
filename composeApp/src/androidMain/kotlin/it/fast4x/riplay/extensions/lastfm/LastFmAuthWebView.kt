package it.fast4x.riplay.extensions.lastfm

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun LastFmAuthWebView(
    authUrl: String,
    scope: CoroutineScope,
    onAuthApproved: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        val title = view?.title ?: ""

                        Timber.d("LastFmAuthWebView: onPageFinished title: $title")

                        if (title.contains("authenticated", ignoreCase = true)
                            && title.contains("Last.fm", ignoreCase = true)) {

                            scope.launch {
                                onAuthApproved()
                            }
                        }
                    }

                }
            }
        },
        update = { webView ->
            webView.loadUrl(authUrl)
        }
    )
}