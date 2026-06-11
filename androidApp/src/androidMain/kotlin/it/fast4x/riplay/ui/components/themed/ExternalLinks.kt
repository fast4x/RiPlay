package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.ExternalLink
import it.fast4x.riplay.utils.colorPalette

@Composable
fun ExternalLinksSection(links: List<ExternalLink>?) {
    if (links.isNullOrEmpty()) return

    val uriHanfler = LocalUriHandler.current

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        links.forEach { link ->
            if (link.platform == "") return@forEach

            IconButton(
                modifier = Modifier.size(20.dp),
                onClick = { uriHanfler.openUri(link.url) },
                icon = when (link.platform) {
                    "home" -> R.drawable.home
                    "youtube" -> R.drawable.logo_yt
                    "instagram" -> R.drawable.logo_instagram
                    "facebook" -> R.drawable.logo_facebook
                    "twitter" -> R.drawable.logo_twitter
                    "spotify" -> R.drawable.logo_spotify
                    "applemusic" -> R.drawable.logo_apple
                    "deezer" -> R.drawable.logo_deezer
                    "soundcloud" -> R.drawable.logo_soundcloud
                    "discogs" -> R.drawable.logo_tidal
                    "rateyourmusic" -> R.drawable.logo_rate_your_music
                    "lastfm" -> R.drawable.logo_lastfm
                    else -> R.drawable.internet
                },
                color = colorPalette().text
            )
        }
    }
}