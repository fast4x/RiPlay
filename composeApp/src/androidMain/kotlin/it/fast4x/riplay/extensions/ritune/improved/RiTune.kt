package it.fast4x.riplay.extensions.ritune.improved

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneConnectionStatus
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneRemoteCommand
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.launch


@Composable
fun RiTuneControllerScreen() {
    val coroutineScope = rememberCoroutineScope()
    val client = remember { RiTuneClient() }
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

        if (connectionStatus is RiTuneConnectionStatus.Disconnected || connectionStatus is RiTuneConnectionStatus.Error) {
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
            if (connectionStatus is RiTuneConnectionStatus.Error) {
                Text("Errore: ${(connectionStatus as RiTuneConnectionStatus.Error).message}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text("Stato: ${if (connectionStatus == RiTuneConnectionStatus.Connected) "Connesso" else "Connessione..."}", color = MaterialTheme.colorScheme.primary)
            Button(onClick = {
                coroutineScope.launch {
                    client.disconnect()
                }
            }) {
                Text("Disconnetti")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (connectionStatus == RiTuneConnectionStatus.Connected && playerState != null) {

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
                        coroutineScope.launch { client.sendCommand(RiTuneRemoteCommand("seek", position = sliderPosition)) }
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
                        coroutineScope.launch { client.sendCommand(RiTuneRemoteCommand(if(isPlaying) "pause" else "play")) }
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
                                client.sendCommand(RiTuneRemoteCommand("load", mediaId = videoId, position = 0f))
                            }
                        }
                    }
                ) {
                    Text("Load")
                }
            }

        } else if (connectionStatus == RiTuneConnectionStatus.Connected) {
            CircularProgressIndicator()
            Text("In attesa dello stato player...")
        }
    }
}

