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
import it.fast4x.riplay.extensions.audiotagger.models.ApiStatsResponse

@Composable
fun AccountStatsScreen(apiKey: String, viewModel: AudioTaggerViewModel = viewModel()) {
    val accountStatsState = viewModel.accountStatsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAccountStats(apiKey)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Statistiche Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        when (accountStatsState.value) {
            is ApiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ApiState.Success<*> -> {
                val stats = (accountStatsState.value as ApiState.Success<ApiStatsResponse>).data
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Data di scadenza: ${stats.expiration_date}")
                        Text("Numero di query: ${stats.queries_count}")
                        Text("Durata totale upload (sec): ${stats.uploaded_duration_sec}")
                        Text("Dimensione totale upload (bytes): ${stats.uploaded_size_bytes}")
                        Text("Crediti spesi: ${stats.credits_spent}")
                        Text("Saldo crediti attuale: ${stats.current_credit_balance}")
                        Text("Secondi gratuiti rimanenti: ${stats.identification_free_sec_remainder}")
                    }
                }
            }
            is ApiState.Error -> {
                Text(
                    text = "Errore: ${(accountStatsState as ApiState.Error).message}",
                    color = Color.Red
                )
            }
            else -> {}
        }
    }
}