package it.fast4x.riplay.ui.screens.player.unified

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerControlsTypeKey
import it.fast4x.riplay.extensions.preferences.playerPlayButtonTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.services.playback.PlayerState
import it.fast4x.riplay.ui.components.themed.PlaybackParamsDialog
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedControlsEssential
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedControlsModern

@OptIn(UnstableApi::class)
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
    val playerControlsType by rememberPreference(
        playerControlsTypeKey,
        PlayerControlsType.Essential
    )
    val playerPlayButtonType by rememberPreference(
        playerPlayButtonTypeKey,
        PlayerPlayButtonType.Disabled
    )
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )
    val playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )

    val isGradientBackgroundEnabled = playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient ||
            playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient

    var playbackSpeed by rememberPreference(playbackSpeedKey, 1f)

    var showSpeedPlayerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSpeedPlayerDialog) {
        PlaybackParamsDialog(
            onDismiss = { showSpeedPlayerDialog = false },
            speedValue = { playbackSpeed = it },
            pitchValue = {},
            durationValue = {
//                playbackDuration = it
//                setPlaybackDuration = true
            },
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