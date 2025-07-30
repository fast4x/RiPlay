package it.fast4x.riplay.ui.screens.player.online

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
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.models.ui.UiMedia
import it.fast4x.riplay.ui.components.ProgressPercentage
import it.fast4x.riplay.ui.components.SeekBar
import it.fast4x.riplay.ui.components.SeekBarAudioWaves
import it.fast4x.riplay.ui.components.SeekBarColored
import it.fast4x.riplay.ui.components.SeekBarCustom
import it.fast4x.riplay.ui.components.SeekBarThin
import it.fast4x.riplay.ui.components.SeekBarWaved
import it.fast4x.riplay.ui.styling.collapsedPlayerProgressBar
import kotlinx.coroutines.launch
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.getPlayerTimelineType
import it.fast4x.riplay.typography
import it.fast4x.riplay.utils.colorPaletteModeKey
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.isCompositionLaunched
import it.fast4x.riplay.utils.pauseBetweenSongsKey
import it.fast4x.riplay.utils.playerTimelineTypeKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.semiBold
import it.fast4x.riplay.utils.showRemainingSongTimeKey
import it.fast4x.riplay.utils.textoutlineKey
import it.fast4x.riplay.utils.transparentbarKey
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.play

@OptIn(UnstableApi::class)
@Composable
fun GetSeekBar(
    position: Long,
    duration: Long,
    mediaId: String,
    media: UiMedia,
    onSeekTo: (Float) -> Unit = {},
    onPause: () -> Unit = {},
    onPlay: () -> Unit = {},
) {

//    val playerTimelineType by rememberPreference(
//        playerTimelineTypeKey,
//        PlayerTimelineType.FakeAudioBar
//    )
    val playerTimelineType = getPlayerTimelineType()
    var scrubbingPosition by remember(mediaId) {
        mutableStateOf<Long?>(null)
    }
    LaunchedEffect(scrubbingPosition) {
        if (scrubbingPosition != null) {
            onSeekTo(scrubbingPosition!!.toFloat())
        }
    }

    var transparentbar by rememberPreference(transparentbarKey, true)
    val scope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(position.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    val pauseBetweenSongs by rememberPreference(pauseBetweenSongsKey, PauseBetweenSongs.`0`)

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

        if (duration == 0L)
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                color = colorPalette().collapsedPlayerProgressBar
            )

        if (playerTimelineType != PlayerTimelineType.Default
            && playerTimelineType != PlayerTimelineType.Wavy
            && playerTimelineType != PlayerTimelineType.FakeAudioBar
            && playerTimelineType != PlayerTimelineType.ThinBar
            && playerTimelineType != PlayerTimelineType.ColoredBar
            )
            SeekBarCustom(
                type = playerTimelineType,
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
                    //scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

        if (playerTimelineType == PlayerTimelineType.Default)
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
                    //scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

        if (playerTimelineType == PlayerTimelineType.ThinBar)
            SeekBarThin(
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
                    //scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

        if (playerTimelineType == PlayerTimelineType.Wavy) {
            SeekBarWaved(
                position = { animatedPosition.value },
                range = 0f..media.duration.toFloat(),
                onSeekStarted = {
                    scrubbingPosition = it.toLong()

                    //isSeeking = true
                    scope.launch {
                        animatedPosition.animateTo(it)
                    }

                },
                onSeek = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0F, duration.toFloat())
                            ?.toLong()
                    } else {
                        null
                    }

                    if (media.duration != C.TIME_UNSET) {
                        //isSeeking = true
                        scope.launch {
                            animatedPosition.snapTo(
                                animatedPosition.value.plus(delta)
                                    .coerceIn(0f, media.duration.toFloat())
                            )
                        }
                    }

                },
                onSeekFinished = {
                    //scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                    /*
                isSeeking = false
                animatedPosition.let {
                    binder.player.seekTo(it.targetValue.toLong())
                }
                 */
                },
                color = colorPalette().collapsedPlayerProgressBar,
                isActive = true,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )
        }

        if (playerTimelineType == PlayerTimelineType.FakeAudioBar)
            SeekBarAudioWaves(
                progressPercentage = ProgressPercentage((position.toFloat() / duration.toFloat()).coerceIn(0f,1f)),
                playedColor = colorPalette().accent,
                notPlayedColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                waveInteraction = {
                    scrubbingPosition = (it.value * duration.toFloat()).toLong()
                    //binder.player.seekTo(scrubbingPosition!!)
                    scrubbingPosition = null
                },
                modifier = Modifier
                    .height(40.dp)
                    //.pulsatingEffect(currentValue = position.toFloat() / duration.toFloat(), isVisible = true)
            )


        if (playerTimelineType == PlayerTimelineType.ColoredBar)
            SeekBarColored(
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
                    //scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp)
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
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(false),
                    onClick = {
                        onSeekTo(( position - 5).toFloat())
                    }
                )
        ){
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
                        .offset((5).dp,0.dp)
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
                    text = formatAsDuration(position * 1000),
                    style = typography().xxs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = formatAsDuration(position * 1000),
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
            var timeRemaining by remember { mutableIntStateOf( 0 ) }
            timeRemaining = (duration.toInt() - position.toInt()) * 1000
            var paused by remember { mutableStateOf(false) }

            if (pauseBetweenSongs != PauseBetweenSongs.`0`)
                LaunchedEffect(timeRemaining) {
                    if (
                        timeRemaining.toLong() < 500
                    ) {
                        paused = true
                        onPause()
                        delay(pauseBetweenSongs.number)
                        onPlay()
                        paused = false
                    }
                }

            if (!paused) {

                if (showRemainingSongTime)
                    Box {
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
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(false),
                        onClick = {
                            onSeekTo(( position + 5).toFloat())
                        }
                    )
            ){
                Box{
                    BasicText(
                        text = formatAsDuration(duration * 1000),
                        style = typography().xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = formatAsDuration(duration * 1000),
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
                            .offset((5).dp,0.dp)
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