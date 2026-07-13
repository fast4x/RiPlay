package it.fast4x.riplay.ui.screens.player.unified.components.core

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import it.fast4x.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.LocalAppearanceSettings
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
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

    val appearanceSettingsVieModel = LocalAppearanceSettings.current
    val appearanceSettings = appearanceSettingsVieModel.activeSettings.collectAsState().value

    val enableKeepScreenOn by rememberPreference(PreferenceKey.IS_KEEP_SCREEN_ON_ENABLED.key, false)
    //val enableKeepScreenOn = appearanceSettings.enableKeepScreenOn
    val isLandscape = isLandscape
//    val playerThumbnailSize by rememberPreference(
//        PLAYER_THUMBNAIL_SIZE.key,
//        PlayerThumbnailSize.Biggest
//    )
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