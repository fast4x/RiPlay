package it.fast4x.riplay.ui.screens.player.unified

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.services.playback.PlayerState
import it.fast4x.riplay.ui.components.themed.PlaybackParamsDialog
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedControlsEssential
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedControlsModern
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun UnifiedGetControls(
    likedAt: Long?,
    onBlurScaleChange: (Float) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffleMode: () -> Unit,
    playerState: PlayerState,
) {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val playerControlsType = appearanceSettings.playerControlsType
    val playerPlayButtonType = appearanceSettings.playerPlayButtonType

    val playbackSpeed = appSettings.playbackSpeed
    val scope = rememberCoroutineScope()

    var showSpeedPlayerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSpeedPlayerDialog) {
        PlaybackParamsDialog(
            onDismiss = { showSpeedPlayerDialog = false },
            speedValue = {
                scope.launch {
                    appSettingsManager.updateSettings(
                        appSettings.copy(
                            playbackSpeed = it
                        )
                    )
                }
            },
            pitchValue = {},
            durationValue = {},
            scaleValue = onBlurScaleChange
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        if (playerControlsType == PlayerControlsType.Essential)
            UnifiedControlsEssential(
                playbackSpeed = playbackSpeed,
                likedAt = likedAt,
                playerPlayButtonType = playerPlayButtonType,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true },
                onPlay = onPlay,
                onPause = onPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffleMode = onToggleShuffleMode,
                playerState = playerState
            )

        if (playerControlsType == PlayerControlsType.Modern)
            UnifiedControlsModern(
                playbackSpeed = playbackSpeed,
                playerPlayButtonType = playerPlayButtonType,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true },
                onPlay = onPlay,
                onPause = onPause,
                onNext = onNext,
                onPrevious = onPrevious,
                playerState = playerState
            )
    }
}