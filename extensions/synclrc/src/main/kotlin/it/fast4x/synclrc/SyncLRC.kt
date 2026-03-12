package it.fast4x.synclrc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.toUpperCasePreservingASCIIRules
import it.fast4x.synclrc.models.SyncLRCLyrics
import it.fast4x.synclrc.models.SyncLRCResponse
import it.fast4x.synclrc.models.SyncLRCSearchResponse
import it.fast4x.synclrc.models.SyncLRCType
import it.fast4x.synclrc.utils.ProxyPreferences
import it.fast4x.synclrc.utils.getProxy
import it.fast4x.synclrc.utils.runCatchingCancellable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SyncLRC {
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
            }

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            engine {
                config {
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(10, TimeUnit.SECONDS)
                    writeTimeout(10, TimeUnit.SECONDS)

                    // Optional
                    //pingInterval(30, TimeUnit.SECONDS)
                }
                // ---------------------------------------

                ProxyPreferences.preference?.let {
                    proxy = getProxy(it)
                }
            }

            defaultRequest {
                url("https://synclrc.tharuk.pro")
            }
        }
    }

    suspend fun fetchLyrics(
        artist: String,
        title: String,
        type: SyncLRCType = SyncLRCType.KARAOKE,
    ): SyncLRCLyrics? {

        return try {
            val response = client.get("/lyrics") {
                parameter("track", URLEncoder.encode(title, "UTF-8"))
                parameter("artist", URLEncoder.encode(artist, "UTF-8"))
                parameter("type", type.type)
            }.body<SyncLRCResponse>()

            SyncLRCLyrics(
                type = SyncLRCType.valueOf(response.type.toUpperCasePreservingASCIIRules()),
                lyrics = response.lyrics
            )

        } catch (e: Exception) {
            //println("SyncLRC fetchLyrics error: ${e.message}")
            null
        }

    }

    suspend fun searchLyrics(
        query: String,
    ): SyncLRCSearchResponse? {
        return try {
            client.get("/search") {
                parameter("query", query)
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}