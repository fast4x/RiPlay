package it.fast4x.riplay.ui.screens.player.unified.components.core

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import it.fast4x.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo

@Composable
fun UnifiedPlayerView(
    onlinePlayerView: YouTubePlayerView? = null,
    mediaItem: MediaItem,
    actAsMini: Boolean = false,
){
    if (mediaItem.isLocal) return

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val enableKeepScreenOn = appSettings.keepScreenEnabled

    val isLandscape = isLandscape

    val playerThumbnailSize = appearanceSettings.playerThumbnailSize

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