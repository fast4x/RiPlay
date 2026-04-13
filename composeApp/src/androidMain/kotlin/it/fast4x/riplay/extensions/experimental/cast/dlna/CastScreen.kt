package it.fast4x.riplay.extensions.experimental.cast.dlna

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.findActivity

@Composable
fun CastScreen() {

    val viewModel: CastViewModel = viewModel()

    val context = LocalContext.current
    val activity = context.findActivity()

    // Registra il launcher per la MediaProjection
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onProjectionGranted(context, result.resultCode, result.data!!)
        } else {
            viewModel.clearError()
        }
    }

    // Osserva l'evento che chiede di aprire il dialog
    LaunchedEffect(Unit) {
        viewModel.requestProjectionEvent.collect {
            val mediaProjectionManager =
                activity.getSystemService(MediaProjectionManager::class.java)
            projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val devices by viewModel.devices.collectAsState()
    val isCasting by viewModel.isCasting.collectAsState()
    val castingTo by viewModel.castingTo.collectAsState()
    val error by viewModel.error.collectAsState()
    var manualIp by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            SmartMessage(it, context = context)
            viewModel.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Controlli cast
        Column(modifier = Modifier.padding(16.dp)) {

            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("IP dispositivo (es. 192.168.1.50)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.addRendererManually(manualIp) },
                enabled = manualIp.isNotBlank()
            ) {
                Text("Aggiungi manualmente")
            }

            Text(
                "Invia audio a:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            devices.forEach { device ->
                ListItem(
                    headlineContent = { Text(device.details.friendlyName) },
                    trailingContent = {
                        if (isCasting) {
                            FilledTonalButton(onClick = { viewModel.stopCast() }) {
                                Text("Stop")
                            }
                        } else {
                            Button(onClick = { viewModel.onCastRequested(device) }) {
                                Text("Cast")
                            }
                        }
                    }
                )
            }

            if (devices.isEmpty()) {
                Text(
                    "Ricerca dispositivi DLNA...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}