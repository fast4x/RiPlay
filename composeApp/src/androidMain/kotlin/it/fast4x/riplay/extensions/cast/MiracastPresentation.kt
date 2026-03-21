package it.fast4x.riplay.extensions.cast

import android.app.Presentation
import android.content.Context
import android.media.MediaRouter
import android.os.Bundle
import android.view.Display
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import it.fast4x.riplay.extensions.cast.models.CastContent

class MiracastPresentation(
    context: Context,
    display: Display,
    private val content: CastContent
) : Presentation(context, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usiamo ComposeView come contenuto della Presentation
        setContentView(ComposeView(context).apply {
            setContent {
                CastScreen(content = content)
            }
        })
    }
}

@Composable
fun CastScreen(content: CastContent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Sfondo nero standard per TV
    ) {
        when (content) {
            is CastContent.Video -> {
                ExoPlayerView(url = content.url)
            }
            is CastContent.GenericView -> {
                AndroidViewFactory(factory = content.viewFactory)
            }
        }
    }
}

// Composable per ExoPlayer
@Composable
fun ExoPlayerView(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true // Mostra i controlli sulla TV
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

// Composable generico per qualunque View (WebView, Mappe, etc.)
@Composable
fun AndroidViewFactory(factory: (Context) -> View) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        factory = { factory(context) },
        modifier = Modifier.fillMaxSize()
    )
}