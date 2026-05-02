package it.fast4x.riplay.extensions.audiotagger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.extensions.audiotagger.ApiState
import it.fast4x.riplay.extensions.audiotagger.AudioTaggerViewModel
import it.fast4x.riplay.extensions.audiotagger.models.GetOfflineStreamResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyOfflineStreamResponse

@Composable
fun IdentifyRemoteScreen(apiKey: String, viewModel: AudioTaggerViewModel = viewModel()) {
    var url by remember { mutableStateOf("") }
    var baseTime by remember { mutableStateOf("00:00:00") }
    var overwrite by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }

    val identifyRemoteState by viewModel.identifyRemoteState.collectAsState()
    val remoteResultState by viewModel.remoteResultState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Identifica File Remoto",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL del file audio") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = baseTime,
            onValueChange = { baseTime = it },
            label = { Text("Tempo di base (HH:MM:SS)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = overwrite,
                onCheckedChange = { overwrite = it }
            )
            Text("Sovrascrivi risultati esistenti")
        }

        if (url.isNotEmpty()) {
            Button(
                onClick = {
                    val over = if (overwrite) 1 else 0
                    viewModel.identifyRemoteAudioFile(apiKey, url, baseTime, over)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Avvia Identificazione")
            }
        }

        when (identifyRemoteState) {
            is ApiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ApiState.Success -> {
                val response = (identifyRemoteState as ApiState.Success<IdentifyOfflineStreamResponse>).data
                token = response.token ?: ""
                Text("Token: $token")
                Text("Stato: ${response.status}")

                Button(
                    onClick = {
                        viewModel.getRemoteRecognitionResult(apiKey, token)
                        showResults = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ottieni Risultati")
                }
            }
            is ApiState.Error -> {
                Text(
                    text = "Errore: ${(identifyRemoteState as ApiState.Error).message}",
                    color = Color.Red
                )
            }
            else -> {}
        }

        if (showResults) {
            when (remoteResultState) {
                is ApiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ApiState.Success -> {
                    val response = (remoteResultState as ApiState.Success<GetOfflineStreamResultResponse>).data
                    when (response.status) {
                        "wait" -> {
                            Text("Elaborazione in corso... ${response.progress}")
                            Button(
                                onClick = { viewModel.getRemoteRecognitionResult(apiKey, token) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aggiorna")
                            }
                        }
                        "done" -> {
                            Text("Risultati trovati:")
                            response.result?.let { result ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Durata audio: ${result["audio_duration"]}")
                                        Text("Dimensione file: ${result["filesize"]} bytes")
                                        Text("Tempo elaborazione: ${result["time_consumed"]} sec")
                                        Text("Formato audio: ${result["audio_format"]}")

                                        // Visualizzazione dei dati JSON delle tracce
                                        // Nota: qui è necessario un parsing aggiuntivo per convertire i dati JSON in oggetti
                                        // Questo è un esempio semplificato
                                        Text("Tracce riconosciute:")
                                        // In un'implementazione reale, qui si farebbe il parsing del campo json_data
                                    }
                                }
                            }
                        }
                    }
                }
                is ApiState.Error -> {
                    Text(
                        text = "Errore: ${(remoteResultState as ApiState.Error).message}",
                        color = Color.Red
                    )
                }
                else -> {}
            }
        }
    }
}