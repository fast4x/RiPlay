package it.fast4x.riplay.ui.screens.player.online.components.core

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.extensions.preferences.isKeepScreenOnEnabledKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo

@Composable
fun OnlinePlayerView(
    onlinePlayerView: YouTubePlayerView? = null,
    mediaItem: MediaItem,
    actAsMini: Boolean = false,
){
    if (mediaItem.isLocal) return

    val enableKeepScreenOn by rememberPreference(isKeepScreenOnEnabledKey, false)
    val isLandscape = isLandscape
    val playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )

    if (mediaItem.isVideo) {
        AndroidView(
            factory = { onlinePlayerView as View },
            update = {
                it.keepScreenOn = enableKeepScreenOn

                when (actAsMini) {
                    true -> {
                        it.layoutParams = ViewGroup.LayoutParams(
                            100,
                            100
                        )
                    }

                    false -> {
                        it.layoutParams = if (!isLandscape) {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                if (playerThumbnailSize == PlayerThumbnailSize.Expanded)
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                else playerThumbnailSize.height
                            )
                        } else {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                    }
                }

            }
        )
    } else {
        LocalView.current.keepScreenOn = enableKeepScreenOn
        onlinePlayerView?.keepScreenOn = enableKeepScreenOn
    }
}