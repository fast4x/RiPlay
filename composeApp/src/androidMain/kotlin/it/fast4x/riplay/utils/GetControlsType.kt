package it.fast4x.riplay.utils

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.extensions.preferences.playbackDurationKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerControlsTypeKey
import it.fast4x.riplay.extensions.preferences.playerPlayButtonTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.ui.components.themed.PlaybackParamsDialog
import it.fast4x.riplay.ui.screens.player.local.components.controls.ControlsEssential
import it.fast4x.riplay.ui.screens.player.local.components.controls.ControlsModern
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun GetControls(
    binder: LocalPlayerService.Binder,
    position: Long,
    shouldBePlaying: Boolean,
    likedAt: Long?,
    mediaId: String,
    onBlurScaleChange: (Float) -> Unit
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
    var playbackDuration by rememberPreference(playbackDurationKey, 0f)
    var setPlaybackDuration by remember { mutableStateOf(false) }

    var showSpeedPlayerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSpeedPlayerDialog) {
        PlaybackParamsDialog(
            onDismiss = { showSpeedPlayerDialog = false },
            speedValue = { playbackSpeed = it },
            pitchValue = {},
            durationValue = {
                playbackDuration = it
                setPlaybackDuration = true
            },
            scaleValue = onBlurScaleChange
        )
    }


        MedleyMode(
            binder = binder,
            seconds = if (playbackDuration < 1f) 0 else playbackDuration.roundToInt()
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        if (playerControlsType == PlayerControlsType.Essential)
            ControlsEssential(
                binder = binder,
                position = position,
                playbackSpeed = playbackSpeed,
                shouldBePlaying = shouldBePlaying,
                likedAt = likedAt,
                mediaId = mediaId,
                playerPlayButtonType = playerPlayButtonType,
                isGradientBackgroundEnabled = isGradientBackgroundEnabled,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true }
            )

        if (playerControlsType == PlayerControlsType.Modern)
            ControlsModern(
                binder = binder,
                position = position,
                playbackSpeed = playbackSpeed,
                shouldBePlaying = shouldBePlaying,
                playerPlayButtonType = playerPlayButtonType,
                isGradientBackgroundEnabled = isGradientBackgroundEnabled,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true }
            )
    }
}