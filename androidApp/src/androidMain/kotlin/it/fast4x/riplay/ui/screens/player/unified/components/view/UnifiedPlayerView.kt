package it.fast4x.riplay.ui.screens.player.unified.components.view

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import it.fast4x.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.utils.VideoParkingLot
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isVideo
import timber.log.Timber

@Composable
fun UnifiedPlayerView(
    videoPlayerView: YouTubePlayerView? = null,
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

    // viene eseguito durante la COMPOSIZIONE del nuovo ramo
    val myClaim = remember { VideoParkingLot.newClaim() }

    if (mediaItem.isVideo) {
        DisposableEffect(Unit) {
            onDispose {
                val v = videoPlayerView ?: return@onDispose
                Timber.d("VIDEO ATTACH ONDISPOSE gen=${VideoParkingLot.generation}")
                if (!VideoParkingLot.isMine(myClaim)) return@onDispose  // ← il guard decisivo
                val parking = VideoParkingLot.host ?: return@onDispose
                if (v.parent === parking) return@onDispose
                if (v.parent != null) (v.parent as ViewGroup).removeView(v)
                parking.addView(v, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }
        }

        AndroidView(
            factory = {
                Timber.d("VIDEO ATTACH gen=${VideoParkingLot.generation}")
                val v = videoPlayerView as View
                if (v.parent != null) (v.parent as ViewGroup).removeView(v)
                v.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                v
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                it.keepScreenOn = enableKeepScreenOn
                /*
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
                */

            }
        )
    } else {
        LocalView.current.keepScreenOn = enableKeepScreenOn
        videoPlayerView?.keepScreenOn = enableKeepScreenOn
    }
}