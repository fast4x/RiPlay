package it.fast4x.simpmusiclyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.simpmusiclyrics.utils.ProxyPreferences
import it.fast4x.simpmusiclyrics.utils.getProxy
import it.fast4x.simpmusiclyrics.utils.runCatchingCancellable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object SimpMusicClient {
    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }

                json(feature)
                //json(feature, ContentType.Text.Html)
                //json(feature, ContentType.Text.Plain)
            }

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            ProxyPreferences.preference?.let {
                engine {
                    proxy = getProxy(it)
                }
            }

            defaultRequest {
                url("https://api-lyrics.simpmusic.org/v1/")
            }
        }
    }


    private suspend fun queryLyrics(mediaId: String): LyricsResponse {
        val response = client.get(mediaId).body<LyricsResponse>()
        println("SimpMusicClient queryLyrics response $response")
        return response
    }

    suspend fun lyrics(mediaId: String) = runCatchingCancellable {
        val lyrics = queryLyrics(mediaId)
        println("SimpMusicClient lyrics mediaId $mediaId lyrics $lyrics")
        lyrics
    }

    @JvmInline
    value class Lyrics(val text: String) {

        val sentences: List<Pair<Long, String>>
            get() = mutableListOf(0L to "").apply {
                for (line in text.trim().lines()) {
                    try {
                        val position = line.take(10).run {
                            get(8).digitToInt() * 10L +
                                    get(7).digitToInt() * 100 +
                                    get(5).digitToInt() * 1000 +
                                    get(4).digitToInt() * 10000 +
                                    get(2).digitToInt() * 60 * 1000 +
                                    get(1).digitToInt() * 600 * 1000
                        }

                        add(position to line.substring(10))
                    } catch (_: Throwable) {
                    }
                }
            }

    }

}
