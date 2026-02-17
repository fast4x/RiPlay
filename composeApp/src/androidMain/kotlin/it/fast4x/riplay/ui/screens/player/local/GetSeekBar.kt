package it.fast4x.riplay.ui.screens.player.local

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.ui.components.SeekBar
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.pauseBetweenSongsKey
import it.fast4x.riplay.extensions.preferences.playerTimelineTypeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.seekWithTapKey
import it.fast4x.riplay.extensions.preferences.showRemainingSongTimeKey
import it.fast4x.riplay.extensions.preferences.textoutlineKey
import it.fast4x.riplay.extensions.preferences.transparentbarKey
import it.fast4x.riplay.ui.components.ModernSeekbar
import it.fast4x.riplay.ui.components.SeekBarAudioForms
import it.fast4x.riplay.ui.components.SeekBarSinusoidalWave
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.isCompositionLaunched
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.PlayerViewModel
import it.fast4x.riplay.utils.PlayerViewModelFactory
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.play

@OptIn(UnstableApi::class)
@Composable
fun GetSeekBar(
    position: Long,
    duration: Long,
    mediaId: String,
    ) {

    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    val playerTimelineType by rememberObservedPreference(
        playerTimelineTypeKey,
        PlayerTimelineType.FakeAudioBar
    )
    var scrubbingPosition by remember(mediaId) {
        mutableStateOf<Long?>(null)
    }
    var transparentbar by rememberPreference(transparentbarKey, true)
    val animatedPosition = remember { Animatable(position.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    val pauseBetweenSongs by rememberPreference(pauseBetweenSongsKey, PauseBetweenSongs.`0`)
    val seekWithTap by rememberObservedPreference(seekWithTapKey, true)

    val compositionLaunched = isCompositionLaunched()
    LaunchedEffect(mediaId) {
        if (compositionLaunched) animatedPosition.animateTo(0f)
    }
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    LaunchedEffect(position) {
        if (!isSeeking && !animatedPosition.isRunning)
            animatedPosition.animateTo(
                position.toFloat(), tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
    }
    val textoutline by rememberPreference(textoutlineKey, false)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {

//        if (duration == C.TIME_UNSET)
//            LinearProgressIndicator(
//                modifier = Modifier.fillMaxWidth(),
//                color = colorPalette().collapsedPlayerProgressBar
//            )

        if (playerTimelineType != PlayerTimelineType.Default
            && playerTimelineType != PlayerTimelineType.Wavy
            && playerTimelineType != PlayerTimelineType.FakeAudioBar
            && playerTimelineType != PlayerTimelineType.ThinBar
            && playerTimelineType != PlayerTimelineType.ColoredBar
            )
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                barHeight = 14.dp
            )


        if (playerTimelineType == PlayerTimelineType.Default)
            /*
            ModernSeekbar(
                value = (scrubbingPosition ?: position) / duration.toFloat(),
                onValueChange = {
                    val newPosition = (it * duration).toLong()
                    newPosition.let(binder.player::seekTo)
                },
                onDragEnd = {
                    scrubbingPosition = null
                },
                totalDurationMs = duration,
//                trackHeight = 8.dp,
//                thumbRadius = 10.dp,
//                thumbRadiusExpanded = 12.dp,
//                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
            )
             */
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
            )

        if (playerTimelineType == PlayerTimelineType.ThinBar)
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                barHeight = 1.dp,
                scrubberRadius = 4.dp
            )


        if (playerTimelineType == PlayerTimelineType.Wavy) {
            SeekBarSinusoidalWave(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textDisabled,
                shape = RoundedCornerShape(8.dp),
            )

        }

        if (playerTimelineType == PlayerTimelineType.FakeAudioBar) {
            SeekBarAudioForms(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
            )

        }


        if (playerTimelineType == PlayerTimelineType.ColoredBar)
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                barHeight = 1.dp,
                scrubberRadius = 4.dp
            )

    }

    Spacer(
        modifier = Modifier
            .height(8.dp)
    )


    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = if (playerTimelineType != PlayerTimelineType.FakeAudioBar) 20.dp else 15.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = seekWithTap,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(false),
                    onClick = {binder.player.seekTo(position - 5000)}
                )
        ){
            if (seekWithTap)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ){
                    Icon(
                        painter =  painterResource(Res.drawable.play),
                        contentDescription = "",
                        tint = colorPalette().text,
                        modifier = Modifier
                            .size(10.dp)
                            .rotate(180f)
                            .offset((5).dp, 0.dp)
                    )
                    Icon(
                        painter =  painterResource(Res.drawable.play),
                        contentDescription = "",
                        tint = colorPalette().text,
                        modifier = Modifier
                            .size(10.dp)
                            .rotate(180f)
                    )
                }
            Box{
                BasicText(
                    text = formatAsDuration(scrubbingPosition ?: position),
                    style = typography().xxs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = formatAsDuration(scrubbingPosition ?: position),
                    style = typography().xxs.semiBold.merge(TextStyle(
                        drawStyle = Stroke(width = 1.0f, join = StrokeJoin.Round),
                        color = if (!textoutline) Color.Transparent else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(0.5f)
                        else Color.Black)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }


        if (duration != C.TIME_UNSET) {
            val factory = remember(binder) {
                PlayerViewModelFactory(binder)
            }
            val playerViewModel: PlayerViewModel = viewModel(factory = factory)
            val positionAndDuration by playerViewModel.positionAndDuration.collectAsStateWithLifecycle()

            var timeRemaining by remember { mutableIntStateOf( 0 ) }
            timeRemaining = positionAndDuration.second.toInt() - positionAndDuration.first.toInt()
            var paused by remember { mutableStateOf(false) }

            if (pauseBetweenSongs != PauseBetweenSongs.`0`)
                LaunchedEffect(timeRemaining) {
                    if (
                    //formatAsDuration(timeRemaining.toLong()) == "0:00"
                        timeRemaining.toLong() < 500
                    ) {
                        paused = true
                        binder.player.pause()
                        delay(pauseBetweenSongs.number)
                        //binder.player.seekTo(position+2000)
                        binder.player.play()
                        paused = false
                    }
                }

            if (!paused) {

                if (showRemainingSongTime)
                    Box(

                    ){
                    BasicText(
                        text = "-${formatAsDuration(timeRemaining.toLong())}",
                        style = typography().xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                    )
                    BasicText(
                        text = "-${formatAsDuration(timeRemaining.toLong())}",
                        style = typography().xxs.semiBold.merge(TextStyle(
                            drawStyle = Stroke(width = 1.0f, join = StrokeJoin.Round),
                            color = if (!textoutline) Color.Transparent else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(0.5f)
                            else Color.Black)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                    )

            } else {
               /* Image(
                    painter = painterResource(R.drawable.pause),
                    colorFilter = ColorFilter.tint(colorPalette().accent),
                    modifier = Modifier
                        .size(20.dp),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                ) */
            }

            /*
            BasicText(
                text = "-${formatAsDuration(timeRemaining.toLong())} / ${formatAsDuration(duration)}",
                style = typography().xxs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
             */
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = seekWithTap,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(false),
                        onClick = {binder.player.seekTo(position + 5000)}
                    )
            ){
                Box{
                    BasicText(
                        text = formatAsDuration(duration),
                        style = typography().xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = formatAsDuration(duration),
                        style = typography().xxs.semiBold.merge(
                            TextStyle(
                                drawStyle = Stroke(width = 1.0f, join = StrokeJoin.Round),
                                color = if (!textoutline) Color.Transparent else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(
                                    0.5f
                                )
                                else Color.Black
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (seekWithTap)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    ){
                        Icon(
                            painter =  painterResource(Res.drawable.play),
                            contentDescription = "",
                            tint = colorPalette().text,
                            modifier = Modifier
                                .size(10.dp)
                                .offset((5).dp, 0.dp)
                        )
                        Icon(
                            painter =  painterResource(Res.drawable.play),
                            contentDescription = "",
                            tint = colorPalette().text,
                            modifier = Modifier
                                .size(10.dp)
                        )
                    }
            }

          }

        }
    }


}