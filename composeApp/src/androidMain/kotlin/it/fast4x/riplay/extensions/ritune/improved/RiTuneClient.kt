package it.fast4x.riplay.extensions.ritune.improved

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneConnectionStatus
import it.fast4x.riplay.extensions.ritune.improved.models.RiTunePlayerState
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneRemoteCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RiTuneClient {
    private val json = Json { ignoreUnknownKeys = true }
    private var session: DefaultClientWebSocketSession? = null

    private val _state = MutableStateFlow<RiTunePlayerState?>(null)
    val state: StateFlow<RiTunePlayerState?> = _state.asStateFlow()

    private val _connectionStatus = MutableStateFlow<RiTuneConnectionStatus>(RiTuneConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<RiTuneConnectionStatus> = _connectionStatus.asStateFlow()

    private val commandChannel = Channel<RiTuneRemoteCommand>()

    private val client = HttpClient(OkHttp) {
        install(WebSockets)

        engine {
            config {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)

                hostnameVerifier { _, _ -> true }
            }
        }
    }
//    private val client = HttpClient(CIO) {
//        engine {
//            https {
//                trustManager = object : X509TrustManager {
//                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
//                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
//                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//                }
//            }
//        }
//        install(WebSockets)
//    }

    suspend fun startAutoConnect(ip: String, port: Int = 18443) {
        while (true) {
            try {
                startConnection(ip, port)
                break
            } catch (e: Exception) {
                Timber.w("Connection lost, try after 5 seconds...")
                delay(5000)
            }
        }
    }

    suspend fun startConnection(ip: String, port: Int = 18443) {
        _connectionStatus.value = RiTuneConnectionStatus.Connecting
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = ip,
                port = port,
                path = "/ws",
                request = {
                    url.protocol = URLProtocol.WSS
                }
            ) {
                session = this@webSocket
                _connectionStatus.value = RiTuneConnectionStatus.Connected

                val senderJob = launch {
                    for (cmd in commandChannel) {
                        try {
                            send(json.encodeToString(cmd))
                        } catch (e: Exception) {
                            Timber.d("RiTune Client Error sent: ${e.message}")
                            break
                        }
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val newState = json.decodeFromString<RiTunePlayerState>(text)
                            _state.value = newState
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("RiTune Client Errore received: ${e.message}")
                } finally {
                    senderJob.cancel()
                    _connectionStatus.value = RiTuneConnectionStatus.Disconnected
                    session = null
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = RiTuneConnectionStatus.Error(e.message ?: "Connection not possible")
            session = null
        }
    }

    suspend fun sendCommand(cmd: RiTuneRemoteCommand) {
        commandChannel.send(cmd)
    }

    suspend fun disconnect() {
        session?.close()
    }

}