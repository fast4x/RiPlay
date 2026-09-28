package it.fast4x.lastfm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import it.fast4x.lastfm.models.ArtistInfo
import it.fast4x.lastfm.models.LastFmResponse
import it.fast4x.lastfm.models.NowPlayingResponse
import it.fast4x.lastfm.models.ScrobbleResponse
import it.fast4x.lastfm.models.SessionKey
import it.fast4x.lastfm.utils.LastFmAuthUtils
import it.fast4x.lastfm.utils.ProxyPreferences
import it.fast4x.lastfm.utils.getProxy
import kotlinx.serialization.json.Json

class LastFmService(
    val apiKey: String,
    private val apiSecret: String
) {

    private val baseUrl = "https://ws.audioscrobbler.com/2.0/"

    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                    isLenient = true
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
                url(baseUrl)
            }
        }
    }

    suspend fun getMobileSession(username: String, password: String): Result<SessionKey> {
        val params = mutableMapOf(
            "method" to "auth.getMobileSession",
            "username" to username,
            "password" to password,
            "api_key" to apiKey
        )

        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)
        params["api_sig"] = apiSig

        return try {

            val response: LastFmResponse<SessionKey> = client.post() {
                params.forEach { (k, v) -> parameter(k, v) }
                parameter("format", "json")
            }.body()

            if (response.error != null) {
                Result.failure(Exception("LastFmService Error Mobile Session (${response.error}): ${response.message}"))
            } else {
                response.session?.let { Result.success(it) }
                    ?: Result.failure(Exception("LastFmService Response Mobile Session empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArtistInfo(artistName: String): Result<ArtistInfo> {
        return try {
            val response: LastFmResponse<ArtistInfo> = client.get() {
                parameter("method", "artist.getinfo")
                parameter("artist", artistName)
                parameter("api_key", apiKey)
                parameter("format", "json")
            }.body()

            if (response.error != null) {
                Result.failure(Exception(response.message))
            } else {
                response.artist?.let { Result.success(it) }
                    ?: Result.failure(Exception("LastFmService Artist not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        album: String? = null,
        sessionKey: String
    ): Result<NowPlayingResponse> {

        val params = mutableMapOf(
            "method" to "track.updateNowPlaying",
            "artist" to artist,
            "track" to track,
            "api_key" to apiKey,
            "sk" to sessionKey,
            "format" to "json"
        )
        album?.let { params["album"] = it }

        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)
        params["api_sig"] = apiSig

        return try {

            val response: LastFmResponse<NowPlayingResponse> = client.submitForm(
                //url = baseUrl,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                }
            ).body()

            if (response.error != null) {
                Result.failure(Exception("LastFmService Error Now Playing: ${response.error}: ${response.message}"))
            } else {
                response.nowPlaying?.let { Result.success(it) }
                    ?: Result.failure(Exception("LastFmService Response Now Playing empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scrobble(
        artist: String,
        track: String,
        timestamp: Long,
        album: String? = null,
        sessionKey: String
    ): Result<ScrobbleResponse> {

        val params = mutableMapOf(
            "method" to "track.scrobble",
            "artist[0]" to artist,
            "track[0]" to track,
            "timestamp[0]" to timestamp.toString(),
            "api_key" to apiKey,
            "sk" to sessionKey,
            "format" to "json"
        )
        album?.let { params["album[0]"] = it }

        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)
        params["api_sig"] = apiSig

        return try {
            val response: LastFmResponse<ScrobbleResponse> = client.submitForm(
                //url = baseUrl,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }

                }
            ).body()

            if (response.error != null) {
                Result.failure(Exception("LastFmService Error Scrobble: ${response.message}"))
            } else {
                response.scrobbles?.let { Result.success(it) }
                    ?: Result.failure(Exception("LastFmService Response Scrobble empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthToken(): Result<String> {
        val params = mapOf(
            "method" to "auth.gettoken",
            "api_key" to apiKey
        )
        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)

        return try {
            val response = client.get() {
                params.forEach { (k, v) -> parameter(k, v) }
                parameter("api_sig", apiSig)
                parameter("format", "json")
            }

            val map: Map<String, String> = response.body()
            map["token"]?.let { Result.success(it) } ?: Result.failure(Exception("LastFmService Token not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(token: String): Result<SessionKey> {
        val params = mutableMapOf(
            "method" to "auth.getSession",
            "token" to token,
            "api_key" to apiKey
        )

        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)
        params["api_sig"] = apiSig

        return try {
            val response: LastFmResponse<SessionKey> = client.get(baseUrl) {
                params.forEach { (k, v) -> parameter(k, v) }
                parameter("format", "json")
            }.body()

            if (response.error != null) {
                Result.failure(Exception("LastFmService Auth Error: ${response.message}"))
            } else {
                response.session?.let { Result.success(it) }
                    ?: Result.failure(Exception("LastFmService Session not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loveTrack(artist: String, track: String, sessionKey: String): Result<String> {
        return performLoveAction(artist, track, sessionKey, "track.love")
    }

    suspend fun unloveTrack(artist: String, track: String, sessionKey: String): Result<String> {
        return performLoveAction(artist, track, sessionKey, "track.unlove")
    }

    private suspend fun performLoveAction(
        artist: String,
        track: String,
        sessionKey: String,
        method: String
    ): Result<String> {
        val params = mutableMapOf(
            "method" to method,
            "artist" to artist,
            "track" to track,
            "api_key" to apiKey,
            "sk" to sessionKey,
            "format" to "json"
        )

        val apiSig = LastFmAuthUtils.generateSignature(params, apiSecret)
        params["api_sig"] = apiSig

        return try {
            val response: LastFmResponse<Any> = client.submitForm(
                //url = baseUrl,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                }
            ).body()

            if (response.error != null) {
                Result.failure(Exception("LastFmService Error API: ${response.message}"))
            } else {
                Result.success("LastFmService loved/unloved Success")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
