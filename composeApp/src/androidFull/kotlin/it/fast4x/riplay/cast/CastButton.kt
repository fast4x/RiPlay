package it.fast4x.riplay.cast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.SecureConfig
import it.fast4x.riplay.utils.appContext

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val youTubeReceiverAppId = SecureConfig.getApiKey(appContext().resources.getString(R.string.RiPlay_CHROMECAST_APPLICATION_ID))
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MediaRouteButton(context).apply {
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(youTubeReceiverAppId)
                    .build()
                routeSelector = selector

            }
        }
    )
}