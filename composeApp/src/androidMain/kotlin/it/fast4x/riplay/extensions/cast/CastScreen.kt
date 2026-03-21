package it.fast4x.riplay.extensions.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CastScreen(
    onCastVideo: () -> Unit,
    onCastYouTube: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Controlli Miracast")
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onCastVideo) {
            Text("Casta Video MP4")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { onCastYouTube("dQw4w9WgXcQ") }) { // ID video YouTube di esempio
            Text("Casta YouTube (WebView)")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onDisconnect) {
            Text("Disconnetti")
        }
    }
}