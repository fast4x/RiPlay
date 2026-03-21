package it.fast4x.riplay.extensions.cast.models

import android.content.Context
import android.view.View

sealed class CastContent {
    // Video URL classico
    data class Video(val url: String) : CastContent()

    // Contenuto generico che richiede una AndroidView (es. WebView per YouTube)
    // Passiamo una 'factory' per ricreare la view sul display remoto
    data class GenericView(
        val viewFactory: (Context) -> View
    ) : CastContent()
}