package it.fast4x.riplay.extensions.qrcodeanalyzer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ScanQrScreen(
    onQrCodeScanned: (String) -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    // 1. Launcher per richiedere il permesso alla fotocamera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
        } else {
            showRationale = true
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            QrCodeScanner(
                modifier = Modifier.fillMaxSize(),
                onQrCodeScanned = { qrText ->
                    onQrCodeScanned(qrText)
                },
                onError = { exception ->
                    exception.printStackTrace()
                }
            )

            ScannerOverlay(modifier = Modifier.matchParentSize())

        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showRationale) {
                    Text(
                        text = "Permesso della fotocamera negato.\nPer scansionare il QR Code, devi abilitare la fotocamera dalle impostazioni del telefono.",
                        color = Color.Red
                    )
                } else {
                    Text("Richiedendo accesso alla fotocamera...")
                }
            }
        }
    }
}