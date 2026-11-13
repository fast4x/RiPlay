package it.fast4x.listenbrainz

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import it.fast4x.listenbrainz.models.AdditionalInfo
import it.fast4x.listenbrainz.models.ListenPayload
import it.fast4x.listenbrainz.models.RecordingFeedbackRequest
import it.fast4x.listenbrainz.models.StatusResponse
import it.fast4x.listenbrainz.models.SubmitListensRequest
import it.fast4x.listenbrainz.models.TrackMetadata
import it.fast4x.listenbrainz.models.ValidateTokenResponse
import it.fast4x.listenbrainz.utils.ProxyPreferences
import it.fast4x.listenbrainz.utils.getProxy
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Client per l'API nativa JSON di ListenBrainz.
 *
 * L'autenticazione usa il token utente personale nell'header:
 *   Authorization: Token <user token>
 * (token disponibile su https://listenbrainz.org/settings/)
 *
 * Non servono API key / apiSecret come per last.fm.
 *
 * Il server e' configurabile: oltre a quello ufficiale (https://api.listenbrainz.org)
 * si puo' puntare a un'istanza ListenBrainz self-hosted, dato che ListenBrainz e'
 * software open source installabile su un proprio server (es. in rete locale).
 */
class ListenBrainzService(serverUrl: String = ListenBrainzService.DEFAULT_SERVER_URL) {

    /**
     * Base URL dell'API, es. "https://api.listenbrainz.org/1/" oppure
     * "http://192.168.1.10:8100/1/" per un'istanza self-hosted.
     */
    val baseUrl: String = ListenBrainzService.apiBaseUrl(serverUrl)

    companion object {

        /** Server ufficiale ListenBrainz (MetaBrainz). */
        const val DEFAULT_SERVER_URL = "https://api.listenbrainz.org"

        /**
         * Normalizza l'indirizzo del server:
         * - vuoto/blank -> server ufficiale;
         * - aggiunge "https://" se manca lo schema;
         * - rimuove lo slash finale e l'eventuale suffisso "/1" (versione dell'API);
         * restituisce la radice del server, es. "https://api.listenbrainz.org"
         * oppure "http://192.168.1.10:8100".
         */
        fun normalizeServerUrl(url: String?): String {
            val trimmed = url?.trim().orEmpty()
            if (trimmed.isEmpty()) return DEFAULT_SERVER_URL

            val withScheme =
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
                else "https://$trimmed"

            val noSlash = withScheme.trimEnd('/')
            val root = if (noSlash.endsWith("/1")) noSlash.removeSuffix("/1") else noSlash

            return root.ifEmpty { DEFAULT_SERVER_URL }
        }

        /**
         * Base URL completo dell'API per il server indicato.
         * L'API ListenBrainz vive sotto "/1/" (v1).
         */
        fun apiBaseUrl(serverUrl: String?): String = "${normalizeServerUrl(serverUrl)}/1/"
    }

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

    // MSID (recording_msid) generato alla submission di ogni ascolto.
    // Serve per il feedback love/unlove che nell'API ListenBrainz richiede un recording_msid.
    private val msidByTrack = ConcurrentHashMap<String, String>()

    private fun recordingMsid(artist: String, track: String): String =
        msidByTrack.getOrPut("$artist|$track") { UUID.randomUUID().toString() }

    /**
     * GET /1/validate-token — verifica il token e restituisce il nome utente associato.
     */
    suspend fun validateToken(token: String): Result<ValidateTokenResponse> {
        return try {
            val response: ValidateTokenResponse = client.get(baseUrl + "validate-token") {
                header("Authorization", "Token $token")
            }.body()

            if (response.valid)
                Result.success(response)
            else
                Result.failure(Exception("ListenBrainzService Token invalid: ${response.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /1/submit-listens con listen_type "playing_now"
     */
    suspend fun submitPlayingNow(
        artist: String,
        track: String,
        album: String? = null,
        token: String
    ): Result<StatusResponse> {
        return submitListen(
            listenType = "playing_now",
            listenedAt = null,
            artist = artist,
            track = track,
            album = album,
            token = token
        )
    }

    /**
     * POST /1/submit-listens con listen_type "single" (ascolto completato)
     */
    suspend fun submitListen(
        artist: String,
        track: String,
        listenedAt: Long,
        album: String? = null,
        token: String
    ): Result<StatusResponse> {
        return submitListen(
            listenType = "single",
            listenedAt = listenedAt,
            artist = artist,
            track = track,
            album = album,
            token = token
        )
    }

    /**
     * POST /1/feedback/recording-feedback — segna il brano come loved (score 1)
     */
    suspend fun loveTrack(artist: String, track: String, token: String): Result<String> {
        return performFeedback(artist, track, score = 1, token = token)
    }

    /**
     * POST /1/feedback/recording-feedback — rimuove il feedback (score 0)
     */
    suspend fun unloveTrack(artist: String, track: String, token: String): Result<String> {
        return performFeedback(artist, track, score = 0, token = token)
    }

    private suspend fun submitListen(
        listenType: String,
        listenedAt: Long?,
        artist: String,
        track: String,
        album: String?,
        token: String
    ): Result<StatusResponse> {
        val request = SubmitListensRequest(
            listenType = listenType,
            payload = listOf(
                ListenPayload(
                    listenedAt = listenedAt,
                    trackMetadata = TrackMetadata(
                        artistName = artist,
                        trackName = track,
                        releaseName = album?.takeIf { it.isNotBlank() },
                        additionalInfo = AdditionalInfo(
                            mediaPlayer = "RiPlay",
                            submissionClient = "RiPlay",
                            recordingMsid = recordingMsid(artist, track)
                        )
                    )
                )
            )
        )

        return try {
            val response: StatusResponse = client.post(baseUrl + "submit-listens") {
                header("Authorization", "Token $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun performFeedback(
        artist: String,
        track: String,
        score: Int,
        token: String
    ): Result<String> {
        val msid = msidByTrack["$artist|$track"]
            ?: return Result.failure(
                Exception("ListenBrainzService Feedback: no listen submitted yet for this track")
            )

        return try {
            val response: StatusResponse = client.post(baseUrl + "feedback/recording-feedback") {
                header("Authorization", "Token $token")
                contentType(ContentType.Application.Json)
                setBody(RecordingFeedbackRequest(recordingMsid = msid, score = score))
            }.body()

            Result.success("ListenBrainzService feedback submitted (${response.status})")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
