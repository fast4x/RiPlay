package it.fast4x.riplay.extensions.audiotagger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.extensions.audiotagger.ApiState
import it.fast4x.riplay.extensions.audiotagger.AudioTaggerViewModel
import it.fast4x.riplay.extensions.audiotagger.models.ApiInfoResponse

@Composable
fun ApiInfoScreen(apiKey: String, viewModel: AudioTaggerViewModel = viewModel()) {
    val apiInfoState = viewModel.apiInfoState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getApiInfo(apiKey)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Informazioni API",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        when (apiInfoState) {
            is ApiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ApiState.Success<*> -> {
                val info = (apiInfoState as ApiState.Success<ApiInfoResponse>).data
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Stato: ${info.status}")
                        Text("Versione API: ${info.api_ver}")
                        Text("Azione: ${info.action}")
                    }
                }
            }
            is ApiState.Error -> {
                Text(
                    text = "Errore: ${(apiInfoState as ApiState.Error).message}",
                    color = Color.Red
                )
            }
            else -> {}
        }
    }
}