package it.fast4x.riplay.extensions.lyricshelper.providers

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.util.toUpperCasePreservingASCIIRules
import it.fast4x.riplay.extensions.lyricshelper.models.SyncLRCLyrics
import it.fast4x.riplay.extensions.lyricshelper.models.SyncLRCResponse
import it.fast4x.riplay.extensions.lyricshelper.models.SyncLRCType
import it.fast4x.riplay.utils.decodeHtmlAndUnicode
import it.fast4x.riplay.utils.runCatchingCancellable
import it.fast4x.riplay.utils.htmlToJson
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URLEncoder

suspend fun syncLRCfetchLyrics(
    context: Context,
    onSuccess: (SyncLRCLyrics) -> Unit,
    onError: (String) -> Unit,
    artist: String,
    title: String
) {
    runCatchingCancellable {
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val type = "karaoke"
        val url = "https://synclrc.tharuk.pro/lyrics?track=$encodedTitle&artist=$encodedArtist&type=$type"


        // Use webview approach to avoid Vercel Security then block simple api call, no block via browser
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {

                    val jsCode = "(function() { return document.body.innerText; })();"

                    evaluateJavascript(jsCode) { htmlResultString ->
                            try {
                                val cleanText = htmlResultString
                                    ?.decodeHtmlAndUnicode()
                                    ?.htmlToJson() ?: ""

                                val json = Json {
                                    ignoreUnknownKeys = true
                                    explicitNulls = false
                                    encodeDefaults = true
                                    isLenient = true
                                }

                                //Timber.d("fetchLyricsViaWebView Lyrics: cleantext $cleanText")

                                val response = json.decodeFromString<SyncLRCResponse>(cleanText)

                                Timber.d("fetchLyricsViaWebView Lyrics: response $response")

                                val syncLrcLyrics = when {
                                    response.error?.isEmpty() == true -> {
                                        SyncLRCLyrics(
                                            type = SyncLRCType.NONE,
                                            lyrics = null,
                                            error = response.error
                                        )

                                    }
                                    else -> {
                                        SyncLRCLyrics(
                                            type = SyncLRCType.valueOf(response.type?.toUpperCasePreservingASCIIRules() ?: "NONE"),
                                            lyrics = response.lyrics
                                        )
                                    }
                                }

                                onSuccess(syncLrcLyrics)
                            } catch (e: Exception) {
                                Timber.e("fetchLyricsViaWebView Lyrics: error ${e.stackTraceToString()}")
                                onError(e.message ?: "Unknown error")
                            }

                    }
                }
            }
            loadUrl(url)
        }
    }?.onFailure { error -> onError(error.message ?: "Unknown error") }
}

