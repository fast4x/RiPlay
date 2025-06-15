package it.fast4x.rimusic.ui.screens.player.online

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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.rimusic.enums.PlayerBackgroundColors
import it.fast4x.rimusic.enums.PlayerControlsType
import it.fast4x.rimusic.enums.PlayerPlayButtonType
import it.fast4x.rimusic.ui.components.themed.PlaybackParamsDialog
import it.fast4x.rimusic.ui.screens.player.online.components.controls.ControlsEssential
import it.fast4x.rimusic.ui.screens.player.online.components.controls.ControlsModern
import it.fast4x.rimusic.utils.MedleyMode
import it.fast4x.rimusic.utils.playbackDurationKey
import it.fast4x.rimusic.utils.playbackSpeedKey
import it.fast4x.rimusic.utils.playerBackgroundColorsKey
import it.fast4x.rimusic.utils.playerControlsTypeKey
import it.fast4x.rimusic.utils.playerPlayButtonTypeKey
import it.fast4x.rimusic.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun GetControls(
    position: Long,
    shouldBePlaying: Boolean,
    likedAt: Long?,
    mediaItem: MediaItem,
    onBlurScaleChange: (Float) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffleMode: () -> Unit,
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

//TODO CHECK MEDLEY MODE
//    MedleyMode(
//        binder = binder,
//        seconds = if (playbackDuration < 1f) 0 else playbackDuration.roundToInt()
//    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        println("Controls type sono qui playerControlsType = $playerControlsType ")

        if (playerControlsType == PlayerControlsType.Essential)
            ControlsEssential(
                position = position,
                playbackSpeed = playbackSpeed,
                shouldBePlaying = shouldBePlaying,
                likedAt = likedAt,
                mediaItem = mediaItem,
                playerPlayButtonType = playerPlayButtonType,
                isGradientBackgroundEnabled = isGradientBackgroundEnabled,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true },
                onPlay = onPlay,
                onPause = onPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleRepeatMode = onToggleRepeatMode,
                onToggleShuffleMode = onToggleShuffleMode,
            )

        if (playerControlsType == PlayerControlsType.Modern)
            ControlsModern(
                position = position,
                playbackSpeed = playbackSpeed,
                shouldBePlaying = shouldBePlaying,
                playerPlayButtonType = playerPlayButtonType,
                isGradientBackgroundEnabled = isGradientBackgroundEnabled,
                onShowSpeedPlayerDialog = { showSpeedPlayerDialog = true },
                onPlay = onPlay,
                onPause = onPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleRepeatMode = onToggleRepeatMode,
                onToggleShuffleMode = onToggleShuffleMode,
            )
    }
}