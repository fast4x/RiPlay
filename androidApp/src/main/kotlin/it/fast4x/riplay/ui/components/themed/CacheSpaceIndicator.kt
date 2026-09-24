package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.enums.CacheType
import it.fast4x.riplay.enums.ExoPlayerDiskCacheMaxSize


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalCoilApi::class)
@Composable
fun CacheSpaceIndicator(
    cacheType: CacheType = CacheType.Images,
    circularIndicator: Boolean = false,
    horizontalPadding: Dp = 12.dp,
) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val coilDiskCacheMaxSize = appSettings.coilDiskCacheMaxSize
    val localPlayerDiskCacheMaxSize = appSettings.exoPlayerDiskCacheMaxSize

    when (cacheType) {
        CacheType.Images -> {}
        CacheType.CachedSongs -> {
            if (localPlayerDiskCacheMaxSize == ExoPlayerDiskCacheMaxSize.Unlimited) return
        }
    }

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    val imageDiskCacheSize = remember {
        try {
            Coil.imageLoader(context).diskCache?.size
        } catch (e: Exception) {
            0L
        }
    }

    val cachedSongsDiskCacheSize = remember {
        try {
            binder?.cache?.cacheSpace
        } catch (e: Exception) {
            0L
        }
    }

    val progressValue = remember { mutableStateOf(0f) }

    LaunchedEffect (Unit, cacheType) {
        progressValue.value =
        when (cacheType) {
            CacheType.Images -> imageDiskCacheSize?.toFloat()
                ?.div(coilDiskCacheMaxSize.bytes.coerceAtLeast(1)) ?: 0.0f
            CacheType.CachedSongs -> cachedSongsDiskCacheSize?.toFloat()
                ?.div(localPlayerDiskCacheMaxSize.bytes.coerceAtLeast(1)) ?: 0.0f
        }
    }

    if (!circularIndicator)
        ProgressIndicator(
            progress = progressValue.value,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(horizontal = horizontalPadding)
        )
    else
        ProgressIndicatorCircular(
            progress = progressValue.value,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(horizontal = horizontalPadding)
        )
}