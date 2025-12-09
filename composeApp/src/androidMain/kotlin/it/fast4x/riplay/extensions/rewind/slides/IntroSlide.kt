package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.rewind.data.AnimatedItem
import it.fast4x.riplay.extensions.rewind.data.RewindSlide

@Composable
fun IntroSlide(slide: RewindSlide.IntroSlide, isPageActive: Boolean) {

    var isContentVisible by remember { mutableStateOf(false) }


    LaunchedEffect(isPageActive) {
        if (isPageActive) {
            isContentVisible = true
        } else {
            isContentVisible = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(slide.backgroundBrush)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            AnimatedItem(isVisible = isContentVisible, delay = 0) {
                Text(
                    text = "RiPlay Rewind",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 60.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 500) {
                Text(
                    text = "Your year in music",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 1000) {
                Text(
                    text = "Your listening, your discoveries and your obsessions. Get ready to relive your most unforgettable musical moments.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(64.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 1500) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Swipe to get started",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_forward),
                        contentDescription = "Scroll",
                        tint = Color.White,
                        modifier = Modifier
                            .size(56.dp)
                            .scale(scale)
                    )
                }
            }
        }
    }
}