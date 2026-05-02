package it.fast4x.riplay.extensions.audiotagger.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.audiotagger.AudioTaggerViewModel

@Composable
fun AudioTaggerMainScreen(navController: NavController, viewModel: AudioTaggerViewModel = viewModel()) {
    var apiKey by remember { mutableStateOf("ffb71eba7797972ef813261c7b4d1ca0") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AudioTag API",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { navController.navigate("api_info/$apiKey") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Informazioni API")
        }

        Button(
            onClick = { navController.navigate("account_stats/$apiKey") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Statistiche Account")
        }

        Button(
            onClick = { navController.navigate("identify_file/$apiKey") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Identifica File Audio")
        }

        Button(
            onClick = { navController.navigate("identify_remote/$apiKey") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Identifica File Remoto")
        }
    }
}