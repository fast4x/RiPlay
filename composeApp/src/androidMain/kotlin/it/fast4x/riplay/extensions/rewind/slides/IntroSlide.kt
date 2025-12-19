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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.visualbitmap.VisualBitmapCreator
import it.fast4x.riplay.utils.colorPalette

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AnimatedContent(isVisible = isContentVisible, delay = 0) {
                        Icon(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = "RiPlay",
                            tint = colorPalette().accent,
                            modifier = Modifier
                                .size(40.dp)
                                .scale(scale)
                        )
                    }
                    AnimatedContent(isVisible = isContentVisible, delay = 200) {
                        Text(
                            text = "RiPlay",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 60.sp
                        )
                    }
                }




                AnimatedContent(isVisible = isContentVisible, delay = 400) {
                    Text(
                        text = "Rewind",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 60.sp
                    )
                }


                AnimatedContent(isVisible = isContentVisible, delay = 500) {
                    Text(
                        text = "Your ${slide.year} in music",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1000) {
                    Text(
                        text = "Your listening!\nYour discoveries!\nYour obsessions!\nGet ready to relive your most unforgettable musical moments.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                AnimatedContent(isVisible = isContentVisible, delay = 1200) {
                    Text(
                        text = "Remember, your privacy is respected!\nAll data used is only in your device and managed by you.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1500) {
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
