package it.fast4x.riplay.extensions.link.improved

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.link.improved.models.ConnectionStatus
import it.fast4x.riplay.extensions.link.improved.models.PlayerState
import it.fast4x.riplay.extensions.link.improved.models.RemoteCommand
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager



class ConnectedRiLinkClient {
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

@Composable
fun RiLinkControllerScreen() {
    val coroutineScope = rememberCoroutineScope()
    val client = remember { ConnectedRiLinkClient() }
    val connectionStatus by client.connectionStatus.collectAsState()
    val playerState by client.state.collectAsState()

    var ipAddress by remember { mutableStateOf("192.168.68.102") }
    var videoId by remember { mutableStateOf("") }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(playerState?.currentTime) {
        if (!isDragging) {
            playerState?.let {
                sliderPosition = it.currentTime
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "RiLink Remote", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        if (connectionStatus is ConnectionStatus.Disconnected || connectionStatus is ConnectionStatus.Error) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("Indirizzo IP TV") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,

                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    client.startAutoConnect(ipAddress)
                }
            }) {
                Text("Connetti")
            }
            if (connectionStatus is ConnectionStatus.Error) {
                Text("Errore: ${(connectionStatus as ConnectionStatus.Error).message}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text("Stato: ${if (connectionStatus == ConnectionStatus.Connected) "Connesso" else "Connessione..."}", color = MaterialTheme.colorScheme.primary)
            Button(onClick = {
                coroutineScope.launch {
                    client.disconnect()
                }
            }) {
                Text("Disconnetti")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (connectionStatus == ConnectionStatus.Connected && playerState != null) {

            playerState?.title?.let { title ->
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            }
            playerState?.mediaId?.let { id ->
                Text("ID: $id", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val duration = playerState?.duration ?: 0f
            if (duration > 0) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        isDragging = true
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        coroutineScope.launch { client.sendCommand(RemoteCommand("seek", position = sliderPosition)) }
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(sliderPosition))
                    Text(formatTime(duration))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPlaying = playerState?.isPlaying == true

                IconButton(
                    onClick = {
                        coroutineScope.launch { client.sendCommand(RemoteCommand(if(isPlaying) "pause" else "play")) }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = "Play/Pause",
                        colorFilter = ColorFilter.tint( colorPalette().text),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Divider()
            Spacer(modifier = Modifier.height(10.dp))
            Text("Carica Video", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = videoId,
                    onValueChange = { videoId = it },
                    label = { Text("YouTube ID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,

                        )
                )
                Button(
                    onClick = {
                        if (videoId.isNotBlank()) {
                            coroutineScope.launch {
                                client.sendCommand(RemoteCommand("load", mediaId = videoId, position = 0f))
                            }
                        }
                    }
                ) {
                    Text("Load")
                }
            }

        } else if (connectionStatus == ConnectionStatus.Connected) {
            CircularProgressIndicator()
            Text("In attesa dello stato player...")
        }
    }
}

fun formatTime(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%02d:%02d", mins, secs)
}

