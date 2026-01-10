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
import it.fast4x.riplay.extensions.ritune.improved.models.ConnectionStatus
import it.fast4x.riplay.extensions.ritune.improved.models.PlayerState
import it.fast4x.riplay.extensions.ritune.improved.models.RemoteCommand
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

class ConnectedRiTuneClient {
    private val json = Json { ignoreUnknownKeys = true }
    private var session: DefaultClientWebSocketSession? = null

    private val _state = MutableStateFlow<PlayerState?>(null)
    val state: StateFlow<PlayerState?> = _state.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val commandChannel = Channel<RemoteCommand>()

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
                Timber.w("Connessione persa, riconnessione tra 5 secondi...")
                delay(5000)
            }
        }
    }

    suspend fun startConnection(ip: String, port: Int = 18443) {
        _connectionStatus.value = ConnectionStatus.Connecting
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
                _connectionStatus.value = ConnectionStatus.Connected

                val senderJob = launch {
                    for (cmd in commandChannel) {
                        try {
                            send(json.encodeToString(cmd))
                        } catch (e: Exception) {
                            Timber.d("RiLink Client Errore invio: ${e.message}")
                            break
                        }
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val newState = json.decodeFromString<PlayerState>(text)
                            _state.value = newState
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("RiLink Client Errore ricezione: ${e.message}")
                } finally {
                    senderJob.cancel()
                    _connectionStatus.value = ConnectionStatus.Disconnected
                    session = null
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Impossibile connettersi")
            session = null
        }
    }

    suspend fun sendCommand(cmd: RemoteCommand) {
        commandChannel.send(cmd)
    }

    suspend fun disconnect() {
        session?.close()
    }

}