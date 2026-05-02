package it.fast4x.riplay.ui.components.themed


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.services.playback.PlayerState
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.doubleShadowDrop
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AudioCassette(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    painter: Painter,
    withCover: Boolean = true,
    playerState: PlayerState
) {

    val leftSpoolRotation = remember { Animatable(0f) }
    val rightSpoolRotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {

                val targetLeft = leftSpoolRotation.value + 360f
                val targetRight = rightSpoolRotation.value - 360f

                coroutineScope {
                    launch {
                        leftSpoolRotation.animateTo(
                            targetValue = targetLeft,
                            animationSpec = tween(
                                durationMillis = 2000,
                                easing = LinearEasing
                            )
                        )
                    }

                    launch {
                        rightSpoolRotation.animateTo(
                            targetValue = targetRight,
                            animationSpec = tween(
                                durationMillis = 2000,
                                easing = LinearEasing
                            )
                        )
                    }
                }
            }
        }
    }

    Box(modifier = modifier.background(Color.Transparent)) {
        Cassette(
            modifier = Modifier
                .size(width = 300.dp, height = 180.dp)
                .align(Alignment.Center)
                .border(3.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                .doubleShadowDrop(RoundedCornerShape(6.dp)),
            leftRotation = leftSpoolRotation.value,
            rightRotation = rightSpoolRotation.value,
            painter = painter,
            withCover = withCover,
            text1 = playerState.mediaInfo?.mediaItem?.mediaMetadata?.artist.toString(),
            text2 = if (playerState.mediaInfo?.mediaItem?.mediaMetadata?.albumTitle != null)
                playerState.mediaInfo.mediaItem.mediaMetadata.albumTitle.toString() else "RiPlay",
            title = playerState.mediaInfo?.mediaItem?.mediaMetadata?.title.toString()

        )
    }


}

@Composable
fun Cassette(
    modifier: Modifier = Modifier,
    leftRotation: Float,
    rightRotation: Float,
    painter: Painter,
    withCover: Boolean = true,
    text1: String = "Mix 90",
    text2: String = "90m",
    title: String = "Mix title"
) {
    val colorPalette = colorPalette()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111111))
    ) {
        // Label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colorPalette.background0)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // Text 1
            Text(
                text = text1,
                color = colorPalette.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp)
                    .padding(top = 4.dp)
            )

            // Title
            Text(
                text = title,
                color = colorPalette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)

            )

            // Testo 2
            Text(
                text = text2,
                color = colorPalette.textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 4.dp)
            )

            // Dashed line
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = colorPalette.text,
                    start = Offset(20f, size.height - 10f),
                    end = Offset(size.width - 20f, size.height - 10f),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )
            }
        }

        // Body
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 55.dp)
        ) {
            drawRoundRect(
                color = colorPalette.background1,  //Color(0xFF2a2a2a),
                topLeft = Offset(20f, 10f),
                size = Size(size.width - 40f, size.height - 110f),
                cornerRadius = CornerRadius(15f)
            )
        }

        if (withCover) {
            Image(
                painter = painter,
                contentDescription = "cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = 10.dp, y = 60.dp)
                    .width(280.dp)
                    .height(80.dp)
                    .alpha(0.3f)
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(x = 10.dp, y = 60.dp)
                    .width(170.dp)
                    .height(80.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                colorPalette.accent.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Spools
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(y = 75.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Spool(rotation = leftRotation, color = colorPalette.accent)
            Spool(rotation = rightRotation, color = colorPalette.accent)
        }


        // Tape
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerXLeft = size.width * 0.30f
            val centerXRight = size.width * 0.70f

            val tapeY = size.height / 2 + 36.dp.toPx()

            val tapeThickness = 6f

            val tapePath = Path().apply {
                moveTo(centerXLeft, tapeY - tapeThickness / 2)
                lineTo(centerXRight, tapeY - tapeThickness / 2)
                lineTo(centerXRight, tapeY + tapeThickness / 2)
                lineTo(centerXLeft, tapeY + tapeThickness / 2)
                close()
            }

            drawPath(tapePath, color = colorPalette.accent)

            drawLine(
                color = Color(0xFF5D5045).copy(alpha = 0.6f),
                start = Offset(centerXLeft, tapeY - 1.5f),
                end = Offset(centerXRight, tapeY - 1.5f),
                strokeWidth = 1f
            )
        }



        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-10).dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(12.dp)
                    .background(
                        Color(0xFF050505),
                        RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(12.dp)
                    .background(
                        Color(0xFF050505),
                        RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }

        // Screws
        val screwOffset = 8.dp
        val screwSize = 6.dp

        listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomStart, Alignment.BottomEnd
        ).forEach { alignment ->
            Box(
                modifier = Modifier
                    .align(alignment)
                    .offset(
                        x = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) screwOffset else -screwOffset,
                        y = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) screwOffset else -screwOffset
                    )
                    .size(screwSize)
                    .background(Color(0xFF444444), CircleShape)
                    .drawBehind {
                        // Screw cross
                        drawLine(
                            Color(0xFF222222),
                            Offset(0f, center.y),
                            Offset(size.width, center.y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            Color(0xFF222222),
                            Offset(center.x, 0f),
                            Offset(center.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            )
        }

        // Text STEREO
        Text(
            text = "STEREO",
            color = Color(0xFFAAAAAA),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-5).dp)
        )
    }
}

@Composable
fun Spool(rotation: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // Border external
            drawCircle(
                color = color,
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )

            // Border internal
            drawCircle(
                color = color,
                radius = radius * 0.7f,
                style = Stroke(width = 2.dp.toPx())
            )

            // Gears
            val teethCount = 6
            for (i in 0 until teethCount) {
                val angle = (360f / teethCount) * i
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.rotate(angle, center.x, center.y)
                    // Gear
                    drawRect(
                        color = color,
                        topLeft = Offset(center.x - 3.dp.toPx(), center.y - radius + 2.dp.toPx()),
                        size = Size(6.dp.toPx(), radius * 0.4f)
                    )
                    canvas.restore()
                }
            }

            val armLength = radius * 0.4f

            drawLine(
                color = Color.Gray,
                start = Offset(center.x - armLength, center.y),
                end = Offset(center.x + armLength, center.y),
                strokeWidth = 3.dp.toPx()
            )

            rotate(60f) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(center.x - armLength, center.y),
                    end = Offset(center.x + armLength, center.y),
                    strokeWidth = 3.dp.toPx()
                )
            }

            rotate(-60f) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(center.x - armLength, center.y),
                    end = Offset(center.x + armLength, center.y),
                    strokeWidth = 3.dp.toPx()
                )
            }

        }
    }
}

fun Modifier.drawBehind(block: DrawScope.() -> Unit): Modifier {
    return this.then(
        Modifier.drawWithContent {
            drawContent()
            block()
        }
    )
}