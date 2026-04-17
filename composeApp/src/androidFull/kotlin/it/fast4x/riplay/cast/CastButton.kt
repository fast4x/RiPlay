package it.fast4x.riplay.cast

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.SecureConfig
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val colorPalette = colorPalette()
    AndroidView(
        modifier = modifier.background(colorPalette.accent.copy(alpha = 0.5f), shape = getRoundnessShape()),
        factory = { context ->
            MediaRouteButton(context).apply {
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(
                        androidx.mediarouter.media.MediaControlIntent.CATEGORY_REMOTE_PLAYBACK
                    )
                    .build()
                routeSelector = selector

            }
        }
    )
}