package it.fast4x.riplay.cast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import it.fast4x.riplay.R

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MediaRouteButton(context).apply {
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory("CC1AD845")
                    .build()
                routeSelector = selector

            }
        }
    )
}