package it.fast4x.audiotaginfo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.streams.asInput
import it.fast4x.audiotaginfo.models.GetResultResponse
import it.fast4x.audiotaginfo.models.IdentifyResponse
import it.fast4x.audiotaginfo.models.InfoResponse
import it.fast4x.audiotaginfo.models.StatResponse
import it.fast4x.audiotaginfo.utils.ProxyPreferences
import it.fast4x.audiotaginfo.utils.getProxy
import it.fast4x.audiotaginfo.utils.runCatchingCancellable
import kotlinx.coroutines.delay
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

object AudioTagInfo {
    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    //explicitNulls = false
                    //encodeDefaults = true
                    isLenient = true
                }

                json(feature)
                json(feature, ContentType.Text.Html)
//                json(feature, ContentType.Text.Plain)
            }

            engine {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }

//            install(ContentEncoding) {
//                gzip()
//                deflate()
//            }

            ProxyPreferences.preference?.let {
                engine {
                    proxy = getProxy(it)
                }
            }

            defaultRequest {
                url("https://audiotag.info/api")
            }
        }
    }

    suspend fun identifyAudioFile(apiKey: String,  audioData: ByteArray) =
        runCatchingCancellable {

            val audioFile = File.createTempFile("audio", ".wav")
            audioFile.writeBytes(audioData)
            audioFile.deleteOnExit()

            val response = client.post {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("apikey", apiKey)
                            append("action", "identify")
                            append(
                                "file",
                                InputProvider { audioFile.inputStream().asInput().buffered() },
                                Headers.build {
                                    append(HttpHeaders.ContentType, "audio/wav")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                                }
                            )
                        },
                        boundary = "AudioTagInfo"
                    )
                )
                onUpload { bytesSentTotal, contentLength ->
                    println("AudioTagInfo identifyAudioFile uploading Sent $bytesSentTotal bytes from $contentLength")
                }

            }

            println("AudioTagInfo identifyAudioFile response: ${response.bodyAsText()}")

            val responseIdentify = response.body<IdentifyResponse>()
            if (responseIdentify.success == true && responseIdentify.token != null) {
               getResult(apiKey, responseIdentify.token)?.getOrNull() as? GetResultResponse
            } else {
                println("AudioTagInfo identifyAudioFile error: ${responseIdentify.error}")
            }

        }

    suspend fun info(apiKey: String) =
        runCatchingCancellable {
            val response = client.submitFormWithBinaryData(
                formData = formData {
                    append("apikey", apiKey)
                    append("action", "info")
                }
            )
            println("AudioTagInfo info response: ${response.bodyAsText()}")

            response.body<InfoResponse>()
        }

    suspend fun getResult(apiKey: String, token: String) =
        runCatchingCancellable {

            var numRequests = 1

            while (numRequests < 100) {
                delay(800)
                val response = client.submitFormWithBinaryData(
                    formData = formData {
                        append("apikey", apiKey)
                        append("action", "get_result")
                        append("token", token)
                    }
                )
                println("AudioTagInfo getResult Numrequests $numRequests response: ${response.bodyAsText()}")

                val result = response.body<GetResultResponse>()
                println("AudioTagInfo getResult GetResultResponse jobStatus: ${result.jobStatus}")
                if (result.jobStatus != "wait") {
                    println("AudioTagInfo getResult response: $")
                    return@runCatchingCancellable result
                } else {
                    numRequests++
                }
            }


        }

    suspend fun stat(apiKey: String) =
        runCatchingCancellable {
            val response = client.submitFormWithBinaryData(
                formData = formData {
                    append("apikey", apiKey)
                    append("action", "stat")
                }
            )
            println("AudioTagInfo stat response: ${response.bodyAsText()}")

            response.body<StatResponse>()
        }


}