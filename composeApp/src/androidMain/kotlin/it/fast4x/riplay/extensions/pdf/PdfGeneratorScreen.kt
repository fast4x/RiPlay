package it.fast4x.riplay.extensions.pdf

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfGeneratorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var resultMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GeneratePDF with images",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Generating PDF...")
        } else {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            generatePdfWithImages(context)
                        }

                        isLoading = false
                        resultMessage = result

                        Toast.makeText(context, "PDF Salvato in: $result", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Create PDF")
            }
        }

        if (resultMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Path file:\n$resultMessage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}