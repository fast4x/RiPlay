package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch

@Composable
fun GenerateQrScreen(value: String) {
    var inputText by remember { mutableStateOf(value) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // QR per lo schermo (piccolo e veloce)
    val screenQrBitmap = remember(inputText) {
        generateQrCode(inputText, 512)
    }

    // QR per l'esportazione/stampa (Alta risoluzione)
    val exportQrBitmap = remember(inputText) {
        generateQrCode(inputText, 2048)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Mostra il QR a schermo
        Card(
            modifier = Modifier.padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            screenQrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Pulsante Condividi / Esporta
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = colorPalette().accent, colorPalette().text),
            onClick = {
                exportQrBitmap?.let { bitmap ->
                    scope.launch {
                        QrCodeExporter.shareQrCode(context, bitmap, "MyQrCode.png")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.qrcode_export_share_print), fontStyle = typography().s.fontStyle)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.qr_code_as_png_format_2048x2048px_ideal_for_printing),
            style = typography().xxs,
            color = colorPalette().text
        )
    }
}