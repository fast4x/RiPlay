package it.fast4x.riplay.extensions.experimental.cast.miracast

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView

class CastPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creiamo un FrameLayout come contenitore root
        val root = FrameLayout(context)

        // Creiamo un ComposeView e lo aggiungiamo al layout
        val composeView = ComposeView(context).apply {
            setContent {
                // QUI PUOI SCRIVERE L'INTERFACCIA COMPOSE PER LA TV
                TvContent()
            }
        }

        root.addView(composeView)
        setContentView(root)
    }
}

// Il contenuto Compose che verrà mostrato sulla TV
@Composable
fun TvContent() {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "Streaming in corso sulla TV...",
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            // Qui metteresti il tuo VideoPlayer o ExoPlayer Composable
        }
    }
}