package it.fast4x.riplay.extensions.audiotagger.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.extensions.audiotagger.ApiState
import it.fast4x.riplay.extensions.audiotagger.AudioTaggerViewModel
import it.fast4x.riplay.extensions.audiotagger.models.GetResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyResponse
import java.io.File

@Composable
fun IdentifyFileScreen(apiKey: String, viewModel: AudioTaggerViewModel = viewModel()) {
    val context = LocalContext.current
    var audioFile by remember { mutableStateOf<File?>(null) }
    var startTime by remember { mutableStateOf("0") }
    var timeLen by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }

    val identifyState by viewModel.identifyState.collectAsState()
    val resultState by viewModel.resultState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val file = File(context.cacheDir, "temp_audio_file")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            audioFile = file
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
            //.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Identifica File Audio",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = { filePickerLauncher.launch("audio/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(audioFile?.name ?: "Seleziona File Audio")
        }

        OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Tempo di inizio (secondi)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = timeLen,
            onValueChange = { timeLen = it },
            label = { Text("Durata da analizzare (secondi, vuoto per fine file)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (audioFile != null) {
            Button(
                onClick = {
                    val start = startTime.toIntOrNull() ?: 0
                    val len = timeLen.toIntOrNull()
                    viewModel.identifyAudioFile(apiKey, audioFile!!, start, len)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Avvia Identificazione")
            }
        }

        when (identifyState) {
            is ApiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ApiState.Success -> {
                val response = (identifyState as ApiState.Success<IdentifyResponse>).data
                token = response.token ?: ""
                Text("Token: $token")
                Text("Stato: ${response.job_status}")

                Button(
                    onClick = {
                        viewModel.getRecognitionResult(apiKey, token)
                        showResults = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ottieni Risultati")
                }
            }
            is ApiState.Error -> {
                Text(
                    text = "Errore: ${(identifyState as ApiState.Error).message}",
                    color = MaterialTheme.colors.error
                )
            }
            else -> {}
        }

        if (showResults) {
            when (resultState) {
                is ApiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ApiState.Success -> {
                    val response = (resultState as ApiState.Success<GetResultResponse>).data
                    when (response.result) {
                        "wait" -> {
                            Text("Elaborazione in corso...")
                            Button(
                                onClick = { viewModel.getRecognitionResult(apiKey, token) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aggiorna")
                            }
                        }
                        "found" -> {
                            Text("Risultati trovati:")
                            response.data?.forEach { result ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Confidenza: ${result.confidence}%")
                                        Text("Intervallo di tempo: ${result.time}")
                                        Text("Tracce:")
                                        result.tracks.forEach { track ->
                                            Text("- ${track.track_name} - ${track.artist_name}")
                                            Text("  Album: ${track.album_name} (${track.album_year})")
                                        }
                                    }
                                }
                            }
                        }
                        "not found" -> {
                            Text("Nessun risultato trovato")
                        }
                    }
                }
                is ApiState.Error -> {
                    Text(
                        text = "Errore: ${(resultState as ApiState.Error).message}",
                        color = MaterialTheme.colors.error
                    )
                }
                else -> {}
            }
        }
    }
}