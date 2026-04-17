package it.fast4x.riplay.extensions.experimental.cast.miracast

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import it.fast4x.riplay.utils.colorPalette

@Composable
fun MiracastScreen() {
    // 1. Inizializziamo il ViewModel direttamente qui.
    // Jetpack Compose assocerà automaticamente questo ViewModel al ciclo di vita dell'Activity.
    val viewModel: MiracastViewModel = viewModel()

    val context = LocalContext.current
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val colorPalette = colorPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(colorPalette.background4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Miracast Compose Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Pulsante Cast Standard
        AndroidView(
            factory = { ctx ->
                MediaRouteButton(ctx).apply {
                    // Accediamo alla proprietà del ViewModel locale
                    routeSelector = viewModel.routeSelector
                    //setRemoteIndicatorDrawable(null)
                }
            },
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stato della connessione
        if (selectedRoute != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Connesso a:\n${selectedRoute?.name}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Text(
                text = "Nessun dispositivo selezionato.\nPremi il pulsante Cast sopra.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}