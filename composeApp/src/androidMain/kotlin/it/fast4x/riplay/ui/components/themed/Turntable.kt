package it.fast4x.riplay.ui.components.themed

import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import it.fast4x.riplay.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.doubleShadowDrop
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Turntable(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    painter: Painter
) {

    val rotationState = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotationState.animateTo(
                    targetValue = rotationState.value + 360f,
                    animationSpec = tween(2000, easing = LinearEasing)
                )
            }
        }
    }

    val targetArmAngle = if (isPlaying) -35f else -55f

    val armAngle by animateFloatAsState(
        targetValue = targetArmAngle,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "armAngle"
    )

    Box(modifier = modifier.background(Color.Transparent)) {

        Box(
            modifier = Modifier
                .fillMaxSize(.85f).align(Alignment.Center),
        ) {

            RotateThumbnailCoverAnimation(
                isSongPlaying = isPlaying,
                painter = painter
            )

            RealisticToneArm(
                angle = armAngle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (5).dp, y = (-45).dp)
            )
        }

    }
}

@Composable
fun RealisticToneArm(
    angle: Float,
    modifier: Modifier = Modifier
) {
    val colorPalette = colorPalette()
    Box(
        modifier = modifier
            .width(240.dp)
            .height(120.dp)

            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .drawBehind {

                val strokeWidth = 6.dp.toPx()
                val neckLength = 60.dp.toPx()
                val armLength = 100.dp.toPx()

                val pivot = Offset(size.width, size.height / 2)

                val neckAngleDeg = 170f
                val armAngleDeg = 200f

                val neckRad = Math.toRadians(neckAngleDeg.toDouble()).toFloat()
                val armRad = Math.toRadians(armAngleDeg.toDouble()).toFloat()

                val joint = Offset(
                    x = pivot.x + neckLength * cos(neckRad),
                    y = pivot.y + neckLength * sin(neckRad)
                )

                val tip = Offset(
                    x = joint.x + armLength * cos(armRad),
                    y = joint.y + armLength * sin(armRad)
                )

                drawCircle(
                    color = colorPalette.accent,
                    radius = 12.dp.toPx(),
                    center = pivot
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(pivot.x - 3.dp.toPx(), pivot.y - 3.dp.toPx())
                )

                drawLine(
                    color = Color(0xFFAAAAAA),
                    start = pivot,
                    end = joint,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFAAAAAA), Color(0xFF555555), Color(0xFFAAAAAA))
                    ),
                    start = joint,
                    end = tip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                val rotationToAlign = (armAngleDeg - 180)

                rotate(degrees = rotationToAlign, pivot = tip) {
                    val headW = 25.dp.toPx()
                    val headH = 18.dp.toPx()

                    drawRoundRect(
                        color = colorPalette.accent,
                        topLeft = Offset(tip.x, tip.y - headH / 2),
                        size = androidx.compose.ui.geometry.Size(headW, headH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )


                    drawLine(
                        color = Color.White,
                        start = Offset(tip.x, tip.y + headH / 2),
                        end = Offset(tip.x, tip.y + headH / 2 + 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    )
}

val Color.Companion.Silver get() = Color(0xFFC0C0C0)
val Color.Companion.Circular get() = Color(0xFF444444)