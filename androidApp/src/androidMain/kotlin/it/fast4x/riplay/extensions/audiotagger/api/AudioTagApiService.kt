package it.fast4x.riplay.extensions.audiotagger.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import it.fast4x.riplay.extensions.audiotagger.models.ApiInfoResponse
import it.fast4x.riplay.extensions.audiotagger.models.ApiStatsResponse
import it.fast4x.riplay.extensions.audiotagger.models.GetOfflineStreamResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.GetResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyOfflineStreamResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyResponse
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class AudioTagApiService {
    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = true

//        install(ContentNegotiation) {
//            json(Json {
//                ignoreUnknownKeys = true
//                explicitNulls = false
//                encodeDefaults = true
//                isLenient = true
//            })
//        }
//
//        install(io.ktor.client.plugins.compression.ContentEncoding) {
//            gzip(0.9F)
//            deflate(0.8F)
//        }
    }

    private val baseUrl = "https://audiotag.info/api"

    // Informazioni API
    suspend fun getApiInfo(apiKey: String): ApiInfoResponse {
        return httpClient.post(baseUrl) {
            FormDataContent(Parameters.build {
                append("apikey", apiKey)
                append("action", "info")
            })
        }.body<ApiInfoResponse>()
    }

    // Statistiche account
    suspend fun getAccountStats(apiKey: String): ApiStatsResponse {
        return httpClient.post(baseUrl) {
            FormDataContent(Parameters.build {
                append("apikey", apiKey)
                append("action", "stat")
            })
        }.body<ApiStatsResponse>()
    }

    // Identificazione file audio (upload)
    suspend fun identifyAudioFile(
        apiKey: String,
        audioFile: File,
        startTime: Int = 0,
        timeLen: Int? = null
    ): IdentifyResponse {
        val response =
         httpClient.post(baseUrl) {
            MultiPartFormDataContent(
                formData {
                    append("apikey", apiKey)
                    append("action", "identify")
                    //append("start_time", startTime.toString())
                    //timeLen?.let { append("time_len", it.toString()) }
                    append("file", audioFile.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${audioFile.name}")
                    })
                }
            )
        }
        Timber.d("AudioTagger identifyAudioFile Response: ${response.bodyAsText()}")

        return response.body<IdentifyResponse>()
    }

    // Ottenere risultati identificazione
    suspend fun getRecognitionResult(apiKey: String, token: String): GetResultResponse {
        return httpClient.post(baseUrl) {
            FormDataContent(Parameters.build {
                append("apikey", apiKey)
                append("action", "get_result")
                append("token", token)
            })
        }.body<GetResultResponse>()
    }

    // Identificazione file audio remoto (URL)
    suspend fun identifyRemoteAudioFile(
        apiKey: String,
        url: String,
        baseTime: String = "00:00:00",
        overwrite: Int = 0
    ): IdentifyOfflineStreamResponse {
        return httpClient.post(baseUrl) {
             FormDataContent(Parameters.build {
                append("apikey", apiKey)
                append("action", "identify_offline_stream")
                append("url", url)
                append("base_time", baseTime)
                append("overwrite", overwrite.toString())
            })
        }.body<IdentifyOfflineStreamResponse>()
    }

    // Ottenere risultati identificazione file remoto
    suspend fun getRemoteRecognitionResult(apiKey: String, token: String): GetOfflineStreamResultResponse {
        return httpClient.post(baseUrl) {
            FormDataContent(Parameters.build {
                append("apikey", apiKey)
                append("action", "get_result_offline_stream")
                append("token", token)
            })
        }.body<GetOfflineStreamResultResponse>()
    }
}